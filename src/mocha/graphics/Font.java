/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Typeface;
import android.text.TextPaint;
import mocha.ui.Screen;

public final class Font {
	private final Typeface typeface;
	private final float pointSize;
	private final float lineHeight;

	public Font(Typeface typeface, float pointSize) {
		this.typeface = typeface;
		this.pointSize = pointSize;

		float screenScale = Screen.mainScreen().getScale();

		TextPaint textPaint = TextDrawing.createPaintForFont(this, screenScale);
		android.graphics.Rect textBounds = new android.graphics.Rect();
		textPaint.getTextBounds("Py", 0, 2, textBounds);
		this.lineHeight = (float)textBounds.height() / screenScale;
	}

	public static Font getSystemFontWithSize(float pointSize) {
		return new Font(Typeface.DEFAULT, pointSize);
	}

	public static Font getBoldSystemFontWithSize(float pointSize) {
		return new Font(Typeface.DEFAULT_BOLD, pointSize);
	}

	public Typeface getTypeface() {
		return typeface;
	}

	public float getPointSize() {
		return pointSize;
	}

	public float getLineHeight() {
		return lineHeight;
	}

	public Font getFontWithSize(float pointSize) {
		return new Font(this.typeface, pointSize);
	}

}
