package bei7473p5254d69jcuat.tenyu.ui.standarduser.gameplay;

import bei7473p5254d69jcuat.tenyu.db.store.administrated.individuality.game.statebyuser.*;
import bei7473p5254d69jcuat.tenyu.model.promise.objectivity.administrated.individuality.game.statebyuser.*;
import bei7473p5254d69jcuat.tenyu.model.release1.objectivity.administrated.individuality.game.statebyuser.*;
import bei7473p5254d69jcuat.tenyu.ui.common.*;
import bei7473p5254d69jcuat.tenyu.ui.standarduser.gameplay.StaticGameStateByUserGui.*;
import glb.*;
import jetbrains.exodus.env.*;

public class StaticGameStateByUserGui extends
		AdministratedObjectGui<StaticGameStateByUserI,
				StaticGameStateByUser,
				StaticGameStateByUser,
				StaticGameStateByUserStore,
				StaticGameStateByUserGui,
				StaticGameStateByUserTableItem> {
	public StaticGameStateByUserGui(String name, String id) {
		super(name, id);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public static class StaticGameStateByUserTableItem
			extends AdministratedObjectTableItem<StaticGameStateByUserI,
					StaticGameStateByUser> {

		public StaticGameStateByUserTableItem(StaticGameStateByUser src) {
			super(src);
			// TODO 自動生成されたコンストラクター・スタブ
		}

	}

	@Override
	protected StaticGameStateByUserTableItem createTableItem(
			StaticGameStateByUser o) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Lang getClassNameLang() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected StaticGameStateByUserGui createDetailGui() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public StaticGameStateByUserStore getStore(Transaction txn) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
