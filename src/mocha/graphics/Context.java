/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.*;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import mocha.ui.*;
import mocha.ui.Color;

import java.util.ArrayList;

public final class Context extends mocha.foundation.Object {
	private final float scale;
	private final ArrayList<Paint> paintStates;
	private final ArrayList<Paint> strokePaintStates;
	private final ArrayList<TextPaint> textPaintStates;
	private final ArrayList<Path> clipPaths;
	private Canvas canvas;
	private Paint paint;
	private Paint strokePaint;
	private TextPaint textPaint;
	private Bitmap bitmap;
	private Path clipPath;

	public enum BlendMode {
		NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN,
		CLEAR, SOURCE_IN, SOURCE_OUT, SOURCE_ATOP, DESTINATION_OVER, DESTINATION_IN,
		DESTINATION_OUT, DESTINATION_ATOP, XOR
	}

	public interface DrawClipped {
		public void drawClipped(Context context);
	}

	interface DrawClippedToPath {
		public void drawClippedToPath(Canvas canvas, RectF rect, Paint paint);
	}

	private Context(float scale) {
		this.scale = scale;
		this.paint = new Paint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.DITHER_FLAG);
		this.strokePaint = new Paint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.DITHER_FLAG);
		this.textPaint = new TextPaint(/*Paint.ANTI_ALIAS_FLAG |*/ Paint.SUBPIXEL_TEXT_FLAG);
		this.paintStates = new ArrayList<Paint>();
		this.strokePaintStates = new ArrayList<Paint>();
		this.textPaintStates = new ArrayList<TextPaint>();
		this.clipPaths = new ArrayList<Path>();

		this.strokePaint.setStyle(Paint.Style.STROKE);
		this.strokePaint.setStrokeJoin(Paint.Join.ROUND);
		this.strokePaint.setStrokeCap(Paint.Cap.ROUND);
	}

	private Context(Context context, Canvas canvas) {
		this(canvas, context.scale);

		this.paint = new Paint(context.paint);
		this.strokePaint = new Paint(context.strokePaint);
		this.textPaint = new TextPaint(context.textPaint);
	}

	public Context(Canvas canvas, float scale) {
		this(scale);
		this.canvas = canvas;

		if(!canvas.isHardwareAccelerated()) {
			this.paint.setAntiAlias(true);
			this.strokePaint.setAntiAlias(true);
			this.textPaint.setAntiAlias(true);
		}
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

	public Rect getClipBoundingBox() {
		if(this.clipPath != null) {
			return this.clipPath.getBounds();
		} else {
			android.graphics.Rect clipBounds = this.canvas.getClipBounds();
			Rect bounds = new Rect();
			bounds.origin.x = (float)clipBounds.left / this.scale;
			bounds.origin.y = (float)clipBounds.top / this.scale;
			bounds.size.width = (float)clipBounds.width() / this.scale;
			bounds.size.height = (float)clipBounds.height() / this.scale;
			return bounds;
		}
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

	Paint getStrokePaint() {
		return strokePaint;
	}

	Paint getPaint() {
		return paint;
	}

	public void setFillColor(float red, float green, float blue, float alpha) {
		this.setFillColor(mocha.ui.Color.rgba(red, green, blue, alpha));
	}

	public void setFillColor(int color) {
		this.paint.setColor(color);
		this.textPaint.setColor(color);
	}

	public void setStrokeColor(int color) {
		this.strokePaint.setColor(color);
	}

	static android.graphics.Xfermode getXferMode(BlendMode blendMode) {
		if(blendMode == null || blendMode == BlendMode.NORMAL) {
			return null;
		}

		switch (blendMode) {
			case MULTIPLY:
				return new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
			case SCREEN:
				return new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
			case OVERLAY:
				return new PorterDuffXfermode(PorterDuff.Mode.OVERLAY);
			case DARKEN:
				return new PorterDuffXfermode(PorterDuff.Mode.DARKEN);
			case LIGHTEN:
				return new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN);
			case CLEAR:
				return new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
			case SOURCE_IN:
				return new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
			case SOURCE_OUT:
				return new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
			case SOURCE_ATOP:
				return new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
			case DESTINATION_OVER:
				return new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
			case DESTINATION_IN:
				return new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
			case DESTINATION_OUT:
				return new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
			case DESTINATION_ATOP:
				return new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
			case XOR:
				return new PorterDuffXfermode(PorterDuff.Mode.XOR);
			case NORMAL:
			default:
				return null;
		}
	}

	public void setBlendMode(BlendMode blendMode) {
		android.graphics.Xfermode xfermode = getXferMode(blendMode);
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

	public void clearShadow() {
		this.paint.clearShadowLayer();
		this.textPaint.clearShadowLayer();
		this.strokePaint.clearShadowLayer();
	}

	public void drawClippedToPath(Path path, DrawClipped drawClipped) {
		Rect bounds = this.getClipBoundingBox();

		// Create source bitmap/canvas
		Bitmap bitmap1 = Bitmap.createBitmap((int)FloatMath.ceil(bounds.width()), (int)FloatMath.ceil(bounds.height()), Bitmap.Config.ARGB_8888);
		bitmap1.setDensity(this.canvas.getDensity());

		Canvas canvas = new Canvas(bitmap1);
		canvas.drawARGB(0, 0, 0, 0);

		// Setup paint
		Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint1.setColor(Color.BLACK);
		paint1.setXfermode(null);

		// Draw mask
		android.graphics.Path nativepath = new android.graphics.Path(path.getScaledNativePath(this.scale));
		canvas.drawPath(nativepath, paint1);

		// Perform draw operation
		paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		drawClipped.drawClipped(new Context(this, canvas));

		// Draw bitmap
		paint1 = new Paint(this.paint);
		paint1.setAlpha(255);
		paint1.clearShadowLayer();
		this.canvas.drawBitmap(bitmap1, bounds.origin.x * scale, bounds.origin.y * scale, paint1);
	}

	void drawClippedToPath(Rect rect, DrawClippedToPath drawClippedToPath) {
		android.graphics.RectF rect1 = rect.toSystemRectF(scale);

		// Create source bitmap/canvas
		Bitmap bitmap1 = Bitmap.createBitmap((int)FloatMath.ceil(rect1.width()), (int)FloatMath.ceil(rect1.height()), Bitmap.Config.ARGB_8888);
		bitmap1.setDensity(this.canvas.getDensity());

		Canvas canvas = new Canvas(bitmap1);
		canvas.drawARGB(0, 0, 0, 0);

		// Setup paint
		Paint paint1 = new Paint(this.paint);
		paint1.setAntiAlias(true);
		paint1.setColor(Color.BLACK);
		paint1.setAlpha(255);
		paint1.setXfermode(null);

		// Draw mask
		android.graphics.Path path = new android.graphics.Path(this.clipPath.getScaledNativePath(this.scale));
		path.offset(-rect1.left, -rect1.top);
		canvas.drawPath(path, paint1);

		// Perform draw operation
		rect1.offsetTo(0, 0);
		paint1.setColor(this.paint.getColor());
		paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		drawClippedToPath.drawClippedToPath(canvas, rect1, paint1);

		// Draw bitmap
		paint1 = new Paint(this.paint);
		paint1.clearShadowLayer();
		this.canvas.drawBitmap(bitmap1, rect.origin.x * scale, rect.origin.y * scale, null);
	}

	public void fillRect(Rect rect, int color) {
		Paint paint1 = this.paint;
		this.paint = new Paint(this.paint);
		this.paint.setColor(color);
		this.fillRect(rect);
		this.paint = paint1;
	}

	public void fillRect(Rect rect) {
		if(this.clipPath != null) {
			Rect bounds = this.clipPath.getBounds();
			/*if(rect.contains(bounds)) {
				this.clipPath.fill(this);
			} else {*/
				this.drawClippedToPath(rect, new DrawClippedToPath() {
				public void drawClippedToPath(Canvas canvas, RectF rect, Paint paint) {
					canvas.drawRect(rect, paint);
				}
			});
			//}
		} else {
			this.canvas.drawRect(rect.toSystemRect(scale), this.paint);
		}
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
		this.clipPath = null;
		this.canvas.clipRect(rect.toSystemRect(this.scale));
	}

	public void fillEllipseInRect(Rect rect) {
		boolean isAntiAlias = this.paint.isAntiAlias();
		if(!isAntiAlias) this.paint.setAntiAlias(true);

		if(this.clipPath != null) {
			this.drawClippedToPath(rect, new DrawClippedToPath() {
				public void drawClippedToPath(Canvas canvas, RectF rect, Paint paint) {
					canvas.drawOval(rect, paint);
				}
			});
		} else {
			this.canvas.drawOval(rect.toSystemRectF(this.scale), this.paint);
		}

		if(!isAntiAlias) this.paint.setAntiAlias(false);
	}

	public void strokeEllipseInRect(Rect rect) {
		boolean isAntiAlias = this.strokePaint.isAntiAlias();
		if(!isAntiAlias) this.strokePaint.setAntiAlias(true);
		this.canvas.drawOval(rect.toSystemRectF(this.scale), this.strokePaint);
		if(!isAntiAlias) this.strokePaint.setAntiAlias(false);
	}

	// TODO: Add support for Gradient.DrawingOptions
	public void drawLinearGradient(Gradient gradient, Point startPoint, Point endPoint, Gradient.DrawingOptions... options) {
		if(gradient == null || gradient.colors == null || gradient.colors.length == 0) return;

		LinearGradient linearGradient = new LinearGradient(startPoint.x * this.scale, startPoint.y * this.scale, endPoint.x * this.scale, endPoint.y * this.scale, gradient.colors, gradient.locations, Shader.TileMode.CLAMP);
		Paint paint1 = this.paint;
		this.paint = new Paint(paint1);
		this.paint.setColor(gradient.colors[0]);
		this.paint.setShader(linearGradient);

		if(this.clipPath != null) {
			this.clipPath.fill(this);
		} else {
			this.canvas.drawPaint(this.paint);
		}

		this.paint = paint1;
	}

	// TODO: Add support for Gradient.DrawingOptions
	public void drawRadialGradient(Gradient gradient, Point radiusCenter, float radius, Gradient.DrawingOptions... options) {
		if(gradient == null || gradient.colors == null || gradient.colors.length == 0) return;

		RadialGradient radialGradient = new RadialGradient(radiusCenter.x * this.scale, radiusCenter.y * this.scale, radius, gradient.colors, gradient.locations, Shader.TileMode.CLAMP);
		Paint paint1 = this.paint;
		this.paint = new Paint(paint1);
		this.paint.setColor(gradient.colors[0]);
		this.paint.setShader(radialGradient);

		if(this.clipPath != null) {
			this.clipPath.fill(this);
		} else {
			this.canvas.drawPaint(this.paint);
		}

		this.paint = paint1;
	}

	public void setClipPath(Path path) {
		if(path == null) {
			this.clipPath = null;
		} else {
			this.clipPath = path.copy();
		}
	}

	Path getClipPath() {
		return this.clipPath;
	}

	public void draw(android.view.View view) {
		view.draw(this.canvas);
	}

	public void save() {
		this.canvas.save();

		this.paintStates.add(0, this.paint);
		this.textPaintStates.add(0, this.textPaint);
		this.strokePaintStates.add(0, this.strokePaint);
		this.clipPaths.add(0, this.clipPath);

		this.paint = new Paint(this.paint);
		this.textPaint = new TextPaint(this.textPaint);
		this.strokePaint = new Paint(this.strokePaint);
	}

	public void restore() {
		this.canvas.restore();

		this.paint = this.paintStates.remove(0);
		this.textPaint = this.textPaintStates.remove(0);
		this.strokePaint = this.strokePaintStates.remove(0);
		this.clipPath = this.clipPaths.remove(0);
	}

}
