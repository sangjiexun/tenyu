package bei7473p5254d69jcuat.tenyu.release1.global.subjectivity;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import bei7473p5254d69jcuat.tenyu.release1.communication.request.subjectivity.*;
import bei7473p5254d69jcuat.tenyu.release1.communication.request.useredge.*;
import bei7473p5254d69jcuat.tenyu.release1.db.store.single.*;
import bei7473p5254d69jcuat.tenyu.release1.global.*;
import bei7473p5254d69jcuat.tenyu.release1.global.Glb.*;
import bei7473p5254d69jcuat.tenyu.release1.global.middle.*;
import bei7473p5254d69jcuat.tenyu.release1.global.objectivity.*;
import bei7473p5254d69jcuat.tenyu.release1.util.Util.*;
import jetbrains.exodus.env.*;

/**
 * 主観。非統一値、つまり自分の元でしか意味を持たない値を管理。
 * 留意する問題が独特で難しいがおおよそ以下である。
 * ・値が古く無効になっている可能性。近傍のIPアドレスが変わり通信不能に
 * なる可能性がある。通信できても他ノードに入れ替わっている可能性がある。
 * ノードの接続可能フラグは予測不可能なタイミングで接続可能/不可が変わる。
 * フラグはupdateNeighbors()のたびに更新される。
 * このような値はある程度の確率で正しい。
 * ・値の量やバランスや質が悪化している可能性。
 * 近傍の中に１つでも通信可能なノードが無ければネットワークから分断される。
 * ファイルの分担において全ファイルを近傍から取得可能な状態を保つ必要がある。
 * できるだけ低レイテンシなノードを揃える必要がある。
 * 主観の状態が悪化しても理想的状態へと回復できる必要がある。
 * 並列なアクセスが多発するクラスである。
 * シングルトン。
 * 留意する問題が難しいとは、P2PEdgeやP2PNodeの状態について、
 * どのような順序で設定されるかが不明、古かったり未設定だったりするからである。
 *
 * TODO:定期処理系のスレッドについて一本化すべきか？
 * 1スレッドにまとめて前回実行日時からの経過時間に応じて呼び出すという事もできる。
 * しかし、そうするとスレッドの待機時間を短く設定するしかないだろう。
 * スレッドインスタンスの数は減るがコンテキストスイッチの回数は増加するだろう。
 *
 * @author exceptiontenyu@gmail.com
 *
 */
public class Subjectivity extends IdObject
		implements ChainVersionup, GlbMemberDynamicState {
	public static final int neighborMax = 2000;

	/**
	 * DBから既存の主観を読み込むか新規作成して返す
	 */
	public static Subjectivity loadOrCreate() {
		return Glb.getDb(Glb.getFile().getSubjectivityDBPath())
				.computeInTransaction((txn) -> {
					Subjectivity r = null;
					try {
						r = new SubjectivityStore(txn).get();
					} catch (Exception e) {
						Glb.getLogger().error("", e);
					}
					//ロード成功したらロードしたものを、失敗したら新しいのをrに設定
					r = r == null ? new Subjectivity() : r;
					return r;
				});
	}

	private transient long commonKeyExchangeInterval = 1000L * 60 * 60;

	public void setCommonKeyExchangeInterval(long commonKeyExchangeInterval) {
		this.commonKeyExchangeInterval = commonKeyExchangeInterval;
	}

	private transient long getAddressesInterval = 1000L * 60;

	/**
	 * Subjeの中にP2Pクラスを通じて通信する部分があるが、
	 * SubjeはP2Pより先にstartされるので、初回だけ少し待機してから
	 * 各スレッドが動作する必要がある。
	 */
	private long initialSleep = 1000L * 4;

	/**
	 * P2Pネットワーク上の自分
	 */
	protected P2PNode me = new P2PNode();

	/**
	 * 更新可能近傍リスト。
	 * サブネットワークはここから更新不可の近傍リストを作成して使用する。
	 * UpdatableNeighborListのインスタンスはこの１つのみ存在する。
	 */
	private UpdatableNeighborList neighborList = new UpdatableNeighborList(1000,
			neighborMax);

	/**
	 * 近傍の更新間隔
	 */
	protected long notificationInterval = 1000L * 60 * 20;

	/**
	 * 定期的にゴミデータを削除する
	 */
	private transient ScheduledFuture<?> periodicClear = null;
	/**
	 * 定期的な共通鍵の交換
	 */
	private transient ScheduledFuture<?> periodicCommonKeyExchange = null;

	/**
	 * 近傍の削除と増加処理を行う
	 */
	private transient ScheduledFuture<?> periodicGetAddresses = null;

	/**
	 * 定期的に近傍に自分の状態を伝える
	 */
	private transient ScheduledFuture<?> periodicNotification = null;

	/**
	 * 自分が設定した各全体運営者の権限
	 */
	private Map<Long, Double> powers = new ConcurrentHashMap<>();

	/**
	 * メッセージに割り当てられる自分の手元で一意な番号。
	 * この番号は連番でありある程度予測されてしまうことに注意が必要。
	 */
	private AtomicLong subjeMessageId = new AtomicLong();

	private transient long unsecureClearInterval = 1000L * 60 * 10;

	/**
	 * 仮の近傍リスト。これの必要性は、公開鍵が既にneighborsにある状態で
	 * 認識プロセスで同じ公開鍵を扱わなければならない場合があるから。
	 */
	private transient UpdatableNeighborListThreadVisible unsecureNeighborList = new UpdatableNeighborListThreadVisible(
			1000, neighborMax);

	protected Subjectivity() {
	}

	public long getCommonKeyExchangeIntervalAfter() {
		return commonKeyExchangeInterval;
	}

	public long getGetAddressesInterval() {
		return getAddressesInterval;
	}

	public long getInitialSleep() {
		return initialSleep;
	}

	public AddrInfo getMyAddrInfo() {
		return Glb.getConf().getAddrInfo(Glb.getSubje().getMe().getAddr());
	}

	/**
	 * @return	自分の情報を可能な限り最新にして返す
	 */
	public synchronized P2PNode getMe() {
		byte[] oldAddr = me.getAddr();
		byte[] newAddr = neighborList.getMyGlobalAddr();
		if (newAddr == null)
			newAddr = oldAddr;
		AddrInfo addr = Glb.getConf().getAddrInfo(newAddr);
		me.setAddrInfo(addr);
		me.setVeteran(Glb.getMiddle().getObjeCatchUp().imVeteran());

		if (!Arrays.equals(oldAddr, newAddr)) {
			//グローバルアドレスが変化していたらアドレス解決サーバに通知
			//非同期。同期だとgetMe()の処理時間が状態に依存して大きく変動してしまう。
			//重複して通知しないようにするためにsynchronizedしている
			Glb.getExecutor().execute(() -> UserEdgeGreeting.send());
		}

		return me;
	}

	public UpdatableNeighborList getNeighborList() {
		return neighborList;
	}

	public long getNextSubjeMessageId() {
		return subjeMessageId.incrementAndGet();
	}

	public long getNotificationInterval() {
		return notificationInterval;
	}

	public Map<Long, Double> getPowers() {
		return powers;
	}

	public long getUnsecureClearInterval() {
		return unsecureClearInterval;
	}

	public UpdatableNeighborListThreadVisible getUnsecureNeighborList() {
		return unsecureNeighborList;
	}

	/**
	 * 自分のP2PNodeを設定する。
	 * このメソッドを呼んでもIPアドレスはセットされない。
	 */
	public void initMe() {
		//アプリ起動時のme設定
		Conf c = Glb.getConf();
		AddrInfo addr = c.getAddrInfo(null);
		me.setAddrInfo(addr);
		me.setPubKey(
				new ByteArrayWrapper(c.getMyStandardPublicKey().getEncoded()));
		me.setType(c.getMyStandardKeyType());
		me.setNodeNumber(Glb.getConf().getNodeNumber());
	}

	/**
	 * DBに記録する。
	 * スケジューラ、P2P、ローカルIPC等を停止してから呼ぶ。
	 */
	public boolean save() {
		return Glb.getDb(Glb.getFile().getSubjectivityDBPath())
				.computeInTransaction((txn) -> {
					try {
						return new SubjectivityStore(txn).save(this);
					} catch (NoSuchAlgorithmException e) {
						Glb.getLogger().error("", e);
						return false;
					}
				});
	}

	public void setCommonKeyExchangeIntervalAfter(
			long commonKeyExchangeIntervalAfter) {
		this.commonKeyExchangeInterval = commonKeyExchangeIntervalAfter;
	}

	public void setGetAddressesInterval(long getAddressesInterval) {
		this.getAddressesInterval = getAddressesInterval;
	}

	public void setNotificationInterval(long notificationInterval) {
		this.notificationInterval = notificationInterval;
	}

	public void setPowers(Map<Long, Double> powers) {
		this.powers = powers;
	}

	public void setUnsecureClearInterval(long unsecureClearInterval) {
		this.unsecureClearInterval = unsecureClearInterval;
	}

	/**
	 * 各定期処理共通の初期待機時間
	 */
	public long initialWait = 1000L * 5;

	public void setInitialWait(long initialWait) {
		this.initialWait = initialWait;
	}

	/**
	 * 呼び出し直後から定期的に処理する。
	 */
	public void start() {
		Glb.debug(() -> "Subjectivity started.");

		//起動のたびにconfの内容をmeに設定
		initMe();

		//通知
		long initialWaitNotification = initialWait;
		if (periodicNotification != null) {
			periodicNotification.cancel(false);
			initialWaitNotification += notificationInterval;
		}
		periodicNotification = Glb.getExecutorPeriodic()
				.scheduleAtFixedRate(() -> {
					try {
						PeriodicNotification.send(neighborList);
					} catch (Exception e) {
						Glb.getLogger().error("", e);
					}
				}, initialWaitNotification, notificationInterval,
						TimeUnit.MILLISECONDS);

		//アドレス取得
		long initialWaitGetAddress = initialWait;
		if (periodicGetAddresses != null) {
			periodicGetAddresses.cancel(false);
			initialWaitGetAddress += getAddressesInterval;
		}
		periodicGetAddresses = Glb.getExecutorPeriodic()
				.scheduleAtFixedRate(() -> {
					try {
						neighborList.getAddresses();
					} catch (Exception e) {
						Glb.getLogger().error("", e);
					}
				}, initialWaitGetAddress, getAddressesInterval,
						TimeUnit.MILLISECONDS);

		//仮リストクリア
		long initialWaitClear = initialWait;
		if (periodicClear != null) {
			periodicClear.cancel(false);
			initialWaitClear += unsecureClearInterval;
		}
		periodicClear = Glb.getExecutorPeriodic().scheduleAtFixedRate(() -> {
			try {
				for (P2PEdge e : unsecureNeighborList.getNeighborsUnsafe()) {
					//もし古ければ
					if ((System.currentTimeMillis() - e.getCreateDate()) > 1000L
							* 60 * 10) {
						Glb.debug("unsecureClear Task remove P2PEdge:" + e);
						unsecureNeighborList
								.removeNeighbor(e.getNode().getP2PNodeId());
					}
				}
			} catch (Exception e) {
				Glb.getLogger().error("", e);
			}
		}, initialWaitClear, unsecureClearInterval, TimeUnit.MILLISECONDS);

		//定期共通鍵交換
		long initialWaitCKE = initialWait;
		if (periodicCommonKeyExchange != null) {
			periodicCommonKeyExchange.cancel(false);
			initialWaitCKE += commonKeyExchangeInterval;
		}
		//もし初期待機時間が長すぎると
		//短時間だけアプリを起動するような使い方の場合に
		//処理が一切発生しない可能性がある。周期が長いので。それはまずい
		periodicCommonKeyExchange = Glb.getExecutorPeriodic()
				.scheduleAtFixedRate(() -> {
					Glb.getLogger().info("periodic commonkey exchange called");
					try {
						for (P2PEdge n : neighborList.getNeighborsUnsafe()) {
							try {
								//1日以内に共通鍵交換をしていなければ再交換
								if (!n.isCommonKeyExchangeIn1Day()) {
									//TODO:共通鍵交換をしたいだけだが認識からやり直している
									//一連の処理が認識段階からやる事を前提にしているせい。
									//エッジ情報をunsecureの方に入れてから共通鍵交換メソッドを呼べば動くだろうが、
									//エッジIDを使いまわせるだけだ。エッジIDも定期更新がかかった方が良いだろう。
									//認識からやり直してもプロセッサ証明スコア等は流用される
									Recognition.send(n.getNode().getAddr(),
											n.getNode().getP2pPort());
								}
							} catch (Exception e) {
								Glb.getLogger().error("定期的共通鍵交換で例外", e);
								continue;
							}
						}
					} catch (Exception e) {
						Glb.getLogger().error("", e);
					}
				}, initialWaitCKE, commonKeyExchangeInterval,
						TimeUnit.MILLISECONDS);
	}

	public void stop() {
		boolean cancel = false;
		if (periodicNotification != null) {
			periodicNotification.cancel(false);
			cancel = true;
		}
		if (periodicGetAddresses != null) {
			periodicGetAddresses.cancel(false);
			cancel = true;
		}
		if (periodicClear != null) {
			periodicClear.cancel(false);
			cancel = true;
		}
		if (periodicCommonKeyExchange != null) {
			periodicCommonKeyExchange.cancel(false);
			cancel = true;
		}

		long wait = 1500;
		if (cancel) {
			wait += 1000 * 20;
		}
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		save();
	}

	@Override
	protected final boolean validateAtCreateIdObjectConcrete(
			ValidationResult r) {
		//TODO 検証処理を書くべきか迷った。falseになるとセーブされなくなる
		//そもそも検証処理の必要性が弱い
		return true;
	}

	@Override
	protected boolean validateAtUpdateChangeIdObjectConcrete(ValidationResult r,
			Object old) {
		return true;
	}

	@Override
	protected final boolean validateAtUpdateIdObjectConcrete(
			ValidationResult r) {
		return true;
	}

	@Override
	public boolean validateReferenceIdObjectConcrete(ValidationResult r,
			Transaction txn) throws Exception {
		return true;
	}
}
