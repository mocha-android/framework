/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.*;
import android.text.TextPaint;
import android.util.DisplayMetrics;

import java.util.ArrayList;

public final class Context extends mocha.foundation.Object {
	private final float scale;
	private final ArrayList<Paint> paintStates;
	private final ArrayList<Paint> strokePaintStates;
	private final ArrayList<TextPaint> textPaintStates;
	private Canvas canvas;
	private Paint paint;
	private Paint strokePaint;
	private TextPaint textPaint;
	private Bitmap bitmap;

	public enum BlendMode {
		NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN,
		CLEAR, SOURCE_IN, SOURCE_OUT, SOURCE_ATOP, DESTINATION_OVER, DESTINATION_IN,
		DESTINATION_OUT, DESTINATION_ATOP, XOR
	}

	private Context(float scale) {
		this.scale = scale;
		this.paint = new Paint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.DITHER_FLAG);
		this.strokePaint = new Paint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.DITHER_FLAG);
		this.textPaint = new TextPaint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.SUBPIXEL_TEXT_FLAG);
		this.paintStates = new ArrayList<Paint>();
		this.strokePaintStates = new ArrayList<Paint>();
		this.textPaintStates = new ArrayList<TextPaint>();

		this.strokePaint.setStyle(Paint.Style.STROKE);
		this.strokePaint.setStrokeJoin(Paint.Join.ROUND);
		this.strokePaint.setStrokeCap(Paint.Cap.ROUND);
	}

	public Context(Canvas canvas, float scale) {
		this(scale);
		this.canvas = canvas;
	}

	public Context(Size size, float scale) {
		this(size, scale, Bitmap.Config.ARGB_8888);
	}

	public Context(Size size, float scale, Bitmap.Config bitmapConfig) {
		this(scale);

		int width = (int)((size.width * scale) + 0.5f);
		int height = (int)((size.height * scale) + 0.5f);
		this.bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
		this.bitmap.setDensity(Math.round(DisplayMetrics.DENSITY_MEDIUM * scale));
		this.canvas = new Canvas(this.bitmap);
	}

	public Image getImage() {
		if(this.bitmap != null) {
			return new Image(Bitmap.createBitmap(this.bitmap));
		} else {
			// TODO: Support this for context's created with canvas
			return null;
		}
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

	public void setStrokeColor(int color) {
		this.strokePaint.setColor(color);
	}

	public void setBlendMode(BlendMode blendMode) {
		android.graphics.Xfermode xfermode;

		switch (blendMode) {
			case MULTIPLY:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
				break;
			case SCREEN:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
				break;
			case OVERLAY:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.OVERLAY);
				break;
			case DARKEN:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.DARKEN);
				break;
			case LIGHTEN:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN);
				break;
			case CLEAR:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
				break;
			case SOURCE_IN:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
				break;
			case SOURCE_OUT:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
				break;
			case SOURCE_ATOP:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
				break;
			case DESTINATION_OVER:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
				break;
			case DESTINATION_IN:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
				break;
			case DESTINATION_OUT:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
				break;
			case DESTINATION_ATOP:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
				break;
			case XOR:
				xfermode = new PorterDuffXfermode(PorterDuff.Mode.XOR);
				break;
			case NORMAL:
			default:
				xfermode = null;
				break;
		}

		this.paint.setXfermode(xfermode);
		this.textPaint.setXfermode(xfermode);
		this.strokePaint.setXfermode(xfermode);
	}

	public void setAlpha(float alpha) {
		int alphai = Math.round(alpha * 255);
		this.paint.setAlpha(alphai);
		this.textPaint.setAlpha(alphai);
		this.strokePaint.setAlpha(alphai);
	}

	public void setLineWidth(float lineWidth) {
		this.strokePaint.setStrokeWidth(lineWidth * this.scale);
	}

	public void setLineCap(Path.LineCap lineCap) {
		switch (lineCap) {
			case BUTT:
				this.strokePaint.setStrokeCap(Paint.Cap.BUTT);
				break;
			case ROUND:
				this.strokePaint.setStrokeCap(Paint.Cap.ROUND);
				break;
			case SQUARE:
				this.strokePaint.setStrokeCap(Paint.Cap.SQUARE);
				break;
		}
	}

	public void setLineJoin(Path.LineJoin lineJoin) {
		switch (lineJoin) {
			case MITER:
				this.strokePaint.setStrokeJoin(Paint.Join.MITER);
				break;
			case ROUND:
				this.strokePaint.setStrokeJoin(Paint.Join.ROUND);
				break;
			case BEVEL:
				this.strokePaint.setStrokeJoin(Paint.Join.BEVEL);
				break;
		}
	}

	public void setLineMiterLimit(float miterLimit) {
		this.strokePaint.setStrokeMiter(miterLimit * this.scale);
	}

	public void setLineDash(float phase, float[] lengths) {
		if(lengths == null || lengths.length == 0) {
			this.strokePaint.setPathEffect(null);
		} else {
			float[] scaledLengths = lengths.clone();
			int count = scaledLengths.length;

			for(int i = 0; i < count; i++) {
				scaledLengths[i] *= this.scale;
			}

			this.strokePaint.setPathEffect(new DashPathEffect(scaledLengths, phase * this.scale));
		}
	}

	public void setShadow(Offset offset, float blur, int color) {
		this.setShadow(offset.horizontal, offset.vertical, blur, color);
	}

	public void setShadow(Size offset, float blur, int color) {
		this.setShadow(offset.width, offset.height, blur, color);
	}

	public void setShadow(float horizontalOffset, float verticalOffset, float blur, int color) {
		if(blur == 0.0f) blur = 0.001f; // 0.0f results in no shadow, 0.001f gives us a no-blur shadow.

		if(color == 0 || (horizontalOffset == 0.0f && verticalOffset == 0.0f)) {
			this.paint.clearShadowLayer();
			this.textPaint.clearShadowLayer();
			this.strokePaint.clearShadowLayer();
		} else {
			this.paint.setShadowLayer(blur, horizontalOffset * this.scale, verticalOffset * this.scale, color);
			this.textPaint.setShadowLayer(blur, horizontalOffset * this.scale, verticalOffset * this.scale, color);
			this.strokePaint.setShadowLayer(blur, horizontalOffset * this.scale, verticalOffset * this.scale, color);
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

	public void strokeRect(Rect rect) {
		android.graphics.Path path = new android.graphics.Path();
		path.addRect(rect.toSystemRectF(this.scale), android.graphics.Path.Direction.CCW);
		this.canvas.drawPath(path, this.strokePaint);
	}

	public void strokeRect(Rect rect, float lineWidth) {
		float restoreLineWidth = this.strokePaint.getStrokeWidth();
		this.strokePaint.setStrokeWidth(lineWidth * this.scale);
		this.strokeRect(rect);
		this.strokePaint.setStrokeWidth(restoreLineWidth);
	}

	public void clipToRect(Rect rect) {
		this.canvas.clipRect(rect.toSystemRect(this.scale));
	}

	// TODO: Add support for Gradient.DrawingOptions
	public void drawLinearGradient(Gradient gradient, Point startPoint, Point endPoint, Gradient.DrawingOptions... options) {
		if(gradient == null || gradient.colors == null || gradient.colors.length == 0) return;

		this.save();
		LinearGradient linearGradient = new LinearGradient(startPoint.x * this.scale, startPoint.y * this.scale, endPoint.x * this.scale, endPoint.y * this.scale, gradient.colors, gradient.locations, Shader.TileMode.CLAMP);
		this.paint.setColor(gradient.colors[0]);
		this.paint.setShader(linearGradient);
		this.canvas.drawPaint(this.paint);
		this.restore();
	}

	// TODO: Add support for Gradient.DrawingOptions
	public void drawRadialGradient(Gradient gradient, Point radiusCenter, float radius, Gradient.DrawingOptions... options) {
		if(gradient == null || gradient.colors == null || gradient.colors.length == 0) return;

		this.save();
		RadialGradient radialGradient = new RadialGradient(radiusCenter.x * this.scale, radiusCenter.y * this.scale, radius, gradient.colors, gradient.locations, Shader.TileMode.CLAMP);
		this.paint.setColor(gradient.colors[0]);
		this.paint.setShader(radialGradient);
		this.canvas.drawPaint(this.paint);
		this.restore();
	}

	public void save() {
		this.canvas.save();

		this.paintStates.add(0, this.paint);
		this.textPaintStates.add(0, this.textPaint);
		this.strokePaintStates.add(0, this.strokePaint);

		this.paint = new Paint(this.paint);
		this.textPaint = new TextPaint(this.textPaint);
		this.strokePaint = new Paint(this.strokePaint);
	}

	public void restore() {
		this.canvas.restore();

		this.paint = this.paintStates.remove(0);
		this.textPaint = this.textPaintStates.remove(0);
		this.strokePaint = this.strokePaintStates.remove(0);
	}

}
