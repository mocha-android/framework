/**
 *  @author Shaun
 *  @date 11/19/12
 *  @copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.FloatMath;
import mocha.ui.Screen;

import java.util.HashMap;
import java.util.Map;

public final class Font {
	private final Typeface typeface;
	private final float pointSize;
	private final float lineHeight;
	private final float ascender;
	private final float descender;
	private final float leading;

	private Map<Float,TextPaint> cachedPaints;

	public Font(Typeface typeface, float pointSize) {
		this.typeface = typeface;
		this.pointSize = pointSize;
		this.cachedPaints = new HashMap<Float, TextPaint>();

		float screenScale = Screen.mainScreen().getScale();

		TextPaint textPaint = this.paintForScreenScale(screenScale);
		android.graphics.Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();

		this.ascender = -(fontMetrics.ascent / screenScale);
		this.descender = -(fontMetrics.descent / screenScale);
		this.leading = fontMetrics.leading / screenScale;

		this.lineHeight = FloatMath.ceil(this.ascender - this.descender + this.leading);
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

	public float getAscender() {
		return ascender;
	}

	public float getDescender() {
		return descender;
	}

	public float getLeading() {
		return leading;
	}

	public Font getFontWithSize(float pointSize) {
		return new Font(this.typeface, pointSize);
	}

	TextPaint paintForScreenScale(float screenScale) {
		TextPaint paint = this.cachedPaints.get(screenScale);

		if(paint == null) {
			paint = new TextPaint();
			this.cachedPaints.put(screenScale, paint);
		} else {
			paint.reset();
		}

		paint.setAntiAlias(true);
		paint.setTextSize(this.pointSize * screenScale);
		paint.setTypeface(this.typeface);

		return paint;
	}

}
