/**
 *  @author Shaun
 *  @date 5/21/14
 *  @copyright 2014 Mocha, Inc. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;
import android.util.FloatMath;
import mocha.ui.Screen;

public class TextAttachmentSpan extends ReplacementSpan {
	private final float scale;
	private TextAttachment textAttachment;
	private VerticalAligment verticalAligment;

	public static enum VerticalAligment {
		BASELINE,
		BOTTOM
	}

	public TextAttachmentSpan(TextAttachment textAttachment) {
		this.scale = Screen.mainScreen().getScale();
		this.textAttachment = textAttachment;
		this.verticalAligment = VerticalAligment.BASELINE;
	}

	public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		Rect bounds = this.textAttachment.bounds;

		if (fm != null) {
			fm.ascent = -(int)FloatMath.ceil(bounds.maxY() * this.scale);
			fm.descent = 0;

			fm.top = fm.ascent;
			fm.bottom = 0;
		}

		return (int)FloatMath.ceil(bounds.maxX() * this.scale);
	}

	public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
		if(this.textAttachment.image == null) return;

		Bitmap bitmap = this.textAttachment.image.getBitmap();
		if(bitmap == null) return;

		canvas.save();

		RectF rectF = this.textAttachment.bounds.toSystemRectF(this.scale);
		float transY = bottom;

		if (this.verticalAligment == VerticalAligment.BASELINE) {
			transY -= paint.getFontMetricsInt().descent;
		}

		canvas.translate(x, transY);
		canvas.drawBitmap(bitmap, new android.graphics.Rect(0, 0, (int)FloatMath.ceil(rectF.width()), (int)FloatMath.ceil(rectF.height())), rectF, paint);
		canvas.restore();

	}
}
