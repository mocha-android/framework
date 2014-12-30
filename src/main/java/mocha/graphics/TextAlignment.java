/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 Mocha. All rights reserved.
 */

package mocha.graphics;

import android.text.Layout;

public enum TextAlignment {
	LEFT(Layout.Alignment.ALIGN_NORMAL),
	CENTER(Layout.Alignment.ALIGN_CENTER),
	RIGHT(Layout.Alignment.ALIGN_OPPOSITE);

	private Layout.Alignment layoutAlignemnt;
	private TextAlignment(Layout.Alignment layoutAlignemnt) {
		this.layoutAlignemnt = layoutAlignemnt;
	}

	Layout.Alignment getLayoutAlignemnt() {
		return this.layoutAlignemnt;
	}

}
