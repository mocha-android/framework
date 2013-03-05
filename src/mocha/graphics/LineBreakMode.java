/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */

package mocha.graphics;

import android.text.TextUtils;

public enum LineBreakMode {
	WORD_WRAPPING,
	CHAR_WRAPPING,
	CLIPPING,
	TRUNCATING_HEAD,
	TRUNCATING_TAIL,
	TRUNCATING_MIDDLE;

	TextUtils.TruncateAt truncateAt() {
		switch (this) {
			case TRUNCATING_HEAD:
				return TextUtils.TruncateAt.START;
			case TRUNCATING_TAIL:
				return TextUtils.TruncateAt.END;
			case TRUNCATING_MIDDLE:
				return TextUtils.TruncateAt.MIDDLE;
			case WORD_WRAPPING:
			case CHAR_WRAPPING:
			case CLIPPING:
			default:
				return null;
		}
	}
}
