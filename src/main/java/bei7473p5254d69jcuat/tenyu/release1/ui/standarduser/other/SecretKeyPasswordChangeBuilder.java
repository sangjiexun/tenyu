package bei7473p5254d69jcuat.tenyu.release1.ui.standarduser.other;

import bei7473p5254d69jcuat.tenyu.release1.global.*;
import bei7473p5254d69jcuat.tenyu.release1.ui.common.*;
import bei7473p5254d69jcuat.tenyu.release1.ui.standarduser.other.SecretKeyPasswordGui.*;
import javafx.scene.*;

public class SecretKeyPasswordChangeBuilder extends GuiBuilder {
	@Override
	public Node build() {
		SecretKeyPasswordGui built = new SecretKeyPasswordGui(name(), id());
		built.build();
		built.buildExternalButton(new SubmitButtonFuncs(gui -> {
			ValidationResult vr = new ValidationResult();
			SecretKeyPassword m = built.setupModel();

			boolean r = m.validate(vr);
			if (!r) {
				gui.message(vr);
			}
			return r;
		}, gui -> Glb.getConf()
				.changeSecretKeyPassword(built.getNewPasswordInput().getText()),
				gui -> built.clear(), null
		));

		return built.getGrid();
	}

	@Override
	public String name() {
		return Lang.OTHER_SECRETKEYPASSWORD_CHANGE.toString();
	}

	@Override
	public String id() {
		return "secretKeyPasswordChange";
	}

}
