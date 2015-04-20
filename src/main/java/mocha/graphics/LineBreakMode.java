package mocha.graphics;

import android.text.TextUtils;

public enum LineBreakMode {
	WORD_WRAPPING(null),
	CHAR_WRAPPING(null),
	CLIPPING(null),
	TRUNCATING_HEAD(TextUtils.TruncateAt.START),
	TRUNCATING_TAIL(TextUtils.TruncateAt.END),
	TRUNCATING_MIDDLE(TextUtils.TruncateAt.MIDDLE);

	private final TextUtils.TruncateAt truncateAt;

	private LineBreakMode(TextUtils.TruncateAt truncateAt) {
		this.truncateAt = truncateAt;
	}

	TextUtils.TruncateAt truncateAt() {
		return this.truncateAt;
	}

}
