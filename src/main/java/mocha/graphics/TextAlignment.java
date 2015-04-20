package mocha.graphics;

import android.text.Layout;

public enum TextAlignment {
	LEFT(Layout.Alignment.ALIGN_NORMAL),
	CENTER(Layout.Alignment.ALIGN_CENTER),
	RIGHT(Layout.Alignment.ALIGN_OPPOSITE);

	private Layout.Alignment layoutAlignment;

	private TextAlignment(Layout.Alignment layoutAlignment) {
		this.layoutAlignment = layoutAlignment;
	}

	Layout.Alignment getLayoutAlignment() {
		return this.layoutAlignment;
	}

}
