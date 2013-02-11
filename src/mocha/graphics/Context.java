/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import java.util.ArrayList;

public final class Context extends mocha.foundation.Object {
	private final float scale;
	private final Canvas canvas;
	private final ArrayList<Paint> paintStates;
	private final ArrayList<TextPaint> textPaintStates;
	private Paint paint;
	private TextPaint textPaint;

	public Context(Canvas canvas, float scale) {
		this.scale = scale;
		this.canvas = canvas;
		this.paint = new Paint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.DITHER_FLAG);
		this.textPaint = new TextPaint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.SUBPIXEL_TEXT_FLAG);
		this.paintStates = new ArrayList<Paint>();
		this.textPaintStates = new ArrayList<TextPaint>();
	}

	Canvas getCanvas() {
		return this.canvas;
	}

	public float getScale() {
		return scale;
	}

	TextPaint getTextPaint() {
		return this.textPaint;
	}

	public void setFillColor(int color) {
		this.paint.setColor(color);
		this.textPaint.setColor(color);
	}

	public void setShadow(Size offset, float blur, int color) {
		if(blur == 0.0f) blur = 0.001f; // 0.0f results in no shadow, 0.001f gives us a no-blur shadow.

		if(color == 0 || offset == null || (offset.width == 0.0f && offset.height == 0.0f)) {
			this.paint.clearShadowLayer();
			this.textPaint.clearShadowLayer();
		} else {
			this.paint.setShadowLayer(blur, offset.width * this.scale, offset.height * this.scale, color);
			this.textPaint.setShadowLayer(blur, offset.width * this.scale, offset.height * this.scale, color);
		}
	}

	public void fillRect(Rect rect, int color) {
		Paint paint = new Paint(this.paint);
		paint.setColor(color);
		this.fillRect(rect, paint);
	}

	public void fillRect(Rect rect) {
		this.fillRect(rect, this.paint);
	}

	private void fillRect(Rect rect, Paint paint) {
		canvas.drawRect(rect.toSystemRect(scale), paint);
	}

	public void save() {
		this.canvas.save();

		this.paintStates.add(0, this.paint);
		this.textPaintStates.add(0, this.textPaint);

		this.paint = new Paint(this.paint);
		this.textPaint = new TextPaint(this.textPaint);
	}

	public void restore() {
		this.canvas.restore();

		this.paint = this.paintStates.remove(0);
		this.textPaint = this.textPaintStates.remove(0);
	}

}
