package mocha.graphics;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.FloatMath;
import mocha.foundation.Copying;
import mocha.ui.Application;
import mocha.ui.Screen;

import java.util.HashMap;
import java.util.Map;

public final class Font implements Copying<Font> {
	private final Typeface typeface;
	private final float pointSize;
	private final float lineHeight;
	private final float ascender;
	private final float descender;
	private final float leading;

	private Map<Float, TextPaint> cachedPaints;

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

	public Font(String assetName, float pointSize) {
		this(Typeface.createFromAsset(Application.sharedApplication().getContext().getAssets(), assetName), 12.0f);
	}

	public static Font withAssetName(String assetName, float pointSize) {
		if (!assetName.contains(".")) {
			assetName += ".ttf";
		}

		return new Font(assetName, pointSize);
	}

	private Font(Font font) {
		this.typeface = font.typeface;
		this.pointSize = font.pointSize;
		this.ascender = font.ascender;
		this.descender = font.descender;
		this.leading = font.leading;
		this.lineHeight = font.lineHeight;
		this.cachedPaints = new HashMap<Float, TextPaint>();
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

		if (paint == null) {
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

	public Font copy() {
		return new Font(this);
	}
}
