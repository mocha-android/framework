/**
 *  @author Shaun
 *  @date 5/22/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import mocha.graphics.Font;

public class FontSpan extends MetricAffectingSpan {

	private Font font;
	private float scale;

	public FontSpan(Font font) {
		this.font = font.copy();
		this.scale = Screen.mainScreen().getScale();
	}

	public void updateMeasureState(TextPaint paint) {
		this.apply(paint);
	}

	public void updateDrawState(TextPaint paint) {
		this.apply(paint);
	}

	private void apply(TextPaint paint) {
		paint.setTypeface(font.getTypeface());
		paint.setTextSize(font.getPointSize() * this.scale);
	}

}
