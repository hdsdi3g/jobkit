package tv.hd3g.testtools;

import tv.hd3g.commons.codepolicyvalidation.CheckPolicy;

public class ThisCheckPolicy extends CheckPolicy {

	/**
	 * Workaround for https://github.com/hdsdi3g/codepolicyvalidation/issues/1
	 */
	@Override
	public void noPrintStackTrace() {
	}

}
