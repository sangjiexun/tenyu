package bei7473p5254d69jcuat.tenyu.ui.creator.game;

import bei7473p5254d69jcuat.tenyu.db.store.administrated.individuality.game.item.*;
import bei7473p5254d69jcuat.tenyu.model.promise.objectivity.administrated.individuality.game.item.*;
import bei7473p5254d69jcuat.tenyu.model.release1.objectivity.administrated.individuality.game.item.*;
import bei7473p5254d69jcuat.tenyu.ui.common.*;
import bei7473p5254d69jcuat.tenyu.ui.creator.game.RatingGameEquipmentClassGui.*;
import glb.*;
import jetbrains.exodus.env.*;

public class RatingGameEquipmentClassGui extends
		IndividualityObjectGui<RatingGameEquipmentClassI,
				RatingGameEquipmentClass,
				RatingGameEquipmentClass,
				RatingGameEquipmentClassStore,
				RatingGameEquipmentClassGui,
				RatingGameEquipmentClassTableItem> {
	public RatingGameEquipmentClassGui(String name, String id) {
		super(name, id);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public static class RatingGameEquipmentClassTableItem
			extends IndividualityObjectTableItem<RatingGameEquipmentClassI,
					RatingGameEquipmentClass> {

		public RatingGameEquipmentClassTableItem(RatingGameEquipmentClass src) {
			super(src);
			// TODO 自動生成されたコンストラクター・スタブ
		}

	}

	@Override
	protected RatingGameEquipmentClassTableItem createTableItem(
			RatingGameEquipmentClass o) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Lang getClassNameLang() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected RatingGameEquipmentClassGui createDetailGui() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public RatingGameEquipmentClassStore getStore(Transaction txn) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
