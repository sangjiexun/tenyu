package bei7473p5254d69jcuat.tenyu.release1.global.objectivity.naturality.agenda.content;

import bei7473p5254d69jcuat.tenyu.release1.global.*;
import bei7473p5254d69jcuat.tenyu.release1.global.objectivity.*;
import bei7473p5254d69jcuat.tenyu.release1.global.objectivity.naturality.agenda.*;
import jetbrains.exodus.env.*;

/**
 * 全体の動作に影響する各種値を設定する
 *
 * @author exceptiontenyu@gmail.com
 *
 */
public class AgendaConfig implements AgendaContentI {
	/**
	 * 客観コアの設定値を持つオブジェクト
	 */
	private ObjectivityCoreConfig config;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AgendaConfig other = (AgendaConfig) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		return true;
	}

	public ObjectivityCoreConfig getConfig() {
		return config;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		return result;
	}

	@Override
	public boolean run(Agenda a) {
		Glb.getObje().getCore().setConfig(config);
		return true;
	}

	public void setConfig(ObjectivityCoreConfig config) {
		this.config = config;
	}

	@Override
	public boolean validateAtCreate(ValidationResult r) {
		boolean b = true;
		if (config == null) {
			r.add(Lang.AGENDA_CONFIG, Lang.ERROR_EMPTY);
			b = false;
		} else {
			if (!config.validateAtCreate(r)) {
				b = false;
			}
		}
		return b;
	}

	@Override
	public boolean validateReference(ValidationResult r, Transaction txn) {
		return true;
	}

}
