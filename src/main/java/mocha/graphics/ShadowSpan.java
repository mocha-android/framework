package mocha.graphics;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class ShadowSpan extends CharacterStyle {

	private Shadow shadow;

	public ShadowSpan(Shadow shadow) {
		if (shadow == null) {
			throw new IllegalArgumentException("Shadow can not be null");
		}

		this.shadow = shadow.copy();
	}

	public void updateDrawState(TextPaint textPaint) {
		Offset offset = this.shadow.getOffset();
		textPaint.setShadowLayer(this.shadow.getBlurRadius(), offset.horizontal, offset.vertical, this.shadow.getColor());
	}

}
