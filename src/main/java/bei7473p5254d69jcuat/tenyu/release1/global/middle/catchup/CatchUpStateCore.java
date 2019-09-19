package bei7473p5254d69jcuat.tenyu.release1.global.middle.catchup;

import java.util.*;

import bei7473p5254d69jcuat.tenyu.release1.communication.*;
import bei7473p5254d69jcuat.tenyu.release1.communication.P2P.*;
import bei7473p5254d69jcuat.tenyu.release1.communication.request.*;
import bei7473p5254d69jcuat.tenyu.release1.communication.request.catchup.*;
import bei7473p5254d69jcuat.tenyu.release1.communication.request.catchup.GetCore.*;
import bei7473p5254d69jcuat.tenyu.release1.db.store.single.*;
import bei7473p5254d69jcuat.tenyu.release1.global.*;
import bei7473p5254d69jcuat.tenyu.release1.global.objectivity.*;
import bei7473p5254d69jcuat.tenyu.release1.global.subjectivity.*;

public class CatchUpStateCore extends AbstractCatchUpProc {
	private AsyncRequestState<GetCore> coreReq;

	@Override
	protected boolean checkCatchUp() {
		ObjectivityCore current = Glb.getObje().getCore();
		if (current == null)
			return false;
		if (!Arrays.equals(current.hash(),
				getCtx().getMajorityAtStart().getCoreHash()))
			return false;
		return true;
	}

	@Override
	protected boolean isNoRequest() {
		return coreReq == null || coreReq.getState().isDone();
	}

	@Override
	protected boolean isReset() {
		return coreReq == null;
	}

	@Override
	protected void end() {
	}

	@Override
	protected void procResponse() {
		if (coreReq == null || coreReq.getReq() == null) {
			throw new IllegalStateException();
		}

		if (coreReq.getState().isDone()) {
			if (Response.fail(coreReq.getReq().getRes())) {
				throw new IllegalStateException("Response.fail req=" + coreReq);
			}

			Message resM = coreReq.getReq().getRes();
			GetCoreResponse res = (GetCoreResponse) resM.getContent();

			if (res.getCore() != null && Arrays.equals(
					getCtx().getMajorityAtStart().getCoreHash(),
					res.getCore().hash())) {
				//多数派客観コアを採用する
				Glb.getObje().writeTryW(txn -> new ObjectivityCoreStore(txn)
						.save(res.getCore()));
			}
			coreReq = null;
		}
	}

	@Override
	protected void requestAsync() {
		GetCore req = new GetCore();
		P2PEdge e = Glb.getSubje().getNeighborList().getNeighborRandomWeight();
		Message reqM = Message.build(req).packaging(req.createPackage(e))
				.finish();
		RequestFutureP2PEdge state = Glb.getP2p().requestAsync(reqM, e);
		coreReq = new AsyncRequestState<GetCore>(state, req, e);
	}

	@Override
	protected void resetConcrete() {
		coreReq = null;
	}
}
