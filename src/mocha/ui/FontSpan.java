/**
 *  @author Shaun
 *  @date 5/22/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import mocha.graphics.Font;

/**
 * FontSpan is a convenience class to assist with using Mocha's {@link Font} class with Android spannables.
 *
 * <p>Since {@link Font} already provides both the typeface and size, FontSpan replaces the need
 * to use {@link android.text.style.TypefaceSpan} and {@link android.text.style.RelativeSizeSpan}.</p>
 */

public class FontSpan extends MetricAffectingSpan {

	private Font font;
	private float scale;

	/**
	 * Initialize FontSpan with a font
	 *
	 * @param font Font for text to be drawn with
	 */
	public FontSpan(Font font) {
		this.font = font.copy();
		this.scale = Screen.mainScreen().getScale();
	}

	@Override
	public void updateMeasureState(TextPaint paint) {
		this.apply(paint);
	}

	@Override
	public void updateDrawState(TextPaint paint) {
		this.apply(paint);
	}

	private void apply(TextPaint paint) {
		paint.setTypeface(font.getTypeface());
		paint.setTextSize(font.getPointSize() * this.scale);
	}

}
