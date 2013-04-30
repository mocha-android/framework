/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.graphics;

import android.graphics.*;
import mocha.foundation.Copying;
import mocha.foundation.MObject;

import java.util.Collections;
import java.util.EnumSet;

public class Path extends MObject implements Copying<Path> {
	private final android.graphics.Path nativePath;
	private float lineWidth;
	private LineCap lineCapStyle;
	private LineJoin lineJoinStyle;
	private float miterLimit;
	private float flatness;
	private boolean usesEvenOddFillRule;
	private Rect cachedBounds;
	private android.graphics.Path cachedScaledPath;
	private float cachedScaledPathFactor;

	public enum LineCap {
		BUTT,
		ROUND,
		SQUARE
	}

	public enum LineJoin {
		MITER,
		ROUND,
		BEVEL
	}

	public enum Corner {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT;

		public final static Corner[] ALL = new Corner[] { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT };
	}

	public Path() {
		this(new android.graphics.Path());
	}

	private Path(android.graphics.Path nativePath) {
		this.nativePath = nativePath;
		this.lineWidth = 1.0f;
		this.lineCapStyle = LineCap.BUTT;
		this.lineJoinStyle = LineJoin.MITER;
	}

	public static Path withRect(Rect rect) {
		Path path = new Path();
		path.nativePath.addRect(rect.toSystemRectF(), android.graphics.Path.Direction.CW);
		return path;
	}

	public static Path withRoundedRect(Rect rect, float cornerRadius) {
		return withRoundedRect(rect, new Size(cornerRadius,cornerRadius), Corner.ALL);
	}

	public static Path withRoundedRect(Rect rect, Size cornerRadii, Corner... byRoundingCorners) {
		android.graphics.Path path = new android.graphics.Path();
		EnumSet<Corner> corners = EnumSet.noneOf(Corner.class);
		Collections.addAll(corners, byRoundingCorners);

		Point min = rect.origin;
		Point max = rect.max();

		if(corners.contains(Corner.TOP_LEFT)) {
			path.moveTo(min.x, min.y + cornerRadii.height);
			path.quadTo(min.x, min.y, min.x + cornerRadii.width, min.y);
		} else {
			path.moveTo(rect.origin.x, rect.origin.y);
		}

		if(corners.contains(Corner.TOP_RIGHT)) {
			path.lineTo(max.x - cornerRadii.width, min.y);
			path.quadTo(max.x, min.y, max.x, min.y + cornerRadii.height);
		} else {
			path.lineTo(rect.origin.x + rect.size.width, rect.origin.y);
		}

		if(corners.contains(Corner.BOTTOM_RIGHT)) {
			path.lineTo(max.x, max.y - cornerRadii.height);
			path.quadTo(max.x, max.y, max.x - cornerRadii.width, max.y);
		} else {
			path.lineTo(rect.origin.x + rect.size.width, rect.origin.y + rect.size.height);
		}

		if(corners.contains(Corner.BOTTOM_LEFT)) {
			path.lineTo(min.x + cornerRadii.width, max.y);
			path.quadTo(min.x, max.y, min.x, max.y - cornerRadii.height);
		} else {
			path.lineTo(rect.origin.x, rect.origin.y + rect.size.height);
		}

		path.close();

		return new Path(path);
	}

	public void moveToPoint(Point point) {
		this.moveTo(point.x, point.y);
	}

	public void moveTo(float x, float y) {
		this.nativePath.moveTo(x, y);
		this.invalidateCache();
	}

	public void addLineToPoint(Point point) {
		this.addLineTo(point.x, point.y);
	}

	public void addLineTo(float x, float y) {
		this.nativePath.lineTo(x, y);
		this.invalidateCache();
	}

	public void addCurveToPoint(Point endPoint, Point controlPoint1, Point controlPoint2) {
		this.addCurveTo(endPoint.x, endPoint.y, controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y);
	}

	public void addCurveTo(float endX, float endY, float cp1x, float cp1y, float cp2x, float cp2y) {
		this.nativePath.cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY);
		this.invalidateCache();
	}

	public void addQuadCurveToPoint(Point endPoint, Point controlPoint) {
		this.addQuadCurveTo(endPoint.x, endPoint.y, controlPoint.x, controlPoint.y);
	}

	public void addQuadCurveTo(float endX, float endY, float cpx, float cpy) {
		this.nativePath.quadTo(cpx, cpy, endX, endY);
		this.invalidateCache();
	}

//	public void addArcWithCenter(Point center, float radius, float startAngle, float endAngle, boolean clockwise) {
//
//	}

	public void closePath() {
		this.nativePath.close();
		this.invalidateCache();
	}

	public void removeAllPoints() {
		this.nativePath.reset();
		this.invalidateCache();
	}

	public void appendPath(Path path) {
		this.nativePath.addPath(path.nativePath);
		this.invalidateCache();
	}

	public Path getReversedPath() {
		return null;
	}

	public void applyTransform(AffineTransform transform) {
		Matrix matrix = new Matrix();

		if(!transform.isIdentity()) {
			float[] values = new float[] {
				transform.getA(), transform.getB(),
				transform.getC(), transform.getD(),

				transform.getTx(),
				transform.getTy()
			};

			matrix.setValues(new float[]{
					values[0], values[2], values[4],
					values[1], values[3], values[5],
					0.0f, 0.0f, 1.0f
			});
		}

		this.nativePath.transform(matrix);
		this.invalidateCache();
	}

	public boolean isEmpty() {
		return this.nativePath.isEmpty();
	}

	public Rect getBounds() {
		if(this.cachedBounds == null) {
			RectF rectF = new RectF();
			this.nativePath.computeBounds(rectF, true);
			this.cachedBounds = new Rect(rectF);
		}

		return this.cachedBounds;
	}

	public Point getCurrentPoint() {
		return null;
	}

	public boolean containsPoint(Point point) {
		return this.getBounds().contains(point);
	}

	public float getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
	}

	public LineCap getLineCapStyle() {
		return lineCapStyle;
	}

	public void setLineCapStyle(LineCap lineCapStyle) {
		this.lineCapStyle = lineCapStyle;
	}

	public LineJoin getLineJoinStyle() {
		return lineJoinStyle;
	}

	public void setLineJoinStyle(LineJoin lineJoinStyle) {
		this.lineJoinStyle = lineJoinStyle;
	}

	public float getMiterLimit() {
		return miterLimit;
	}

	public void setMiterLimit(float miterLimit) {
		this.miterLimit = miterLimit;
	}

	public float getFlatness() {
		return flatness;
	}

	public void setFlatness(float flatness) {
		this.flatness = flatness;
	}

	public boolean isUsesEvenOddFillRule() {
		return usesEvenOddFillRule;
	}

	public void setUsesEvenOddFillRule(boolean usesEvenOddFillRule) {
		this.usesEvenOddFillRule = usesEvenOddFillRule;

		if(usesEvenOddFillRule) {
			this.nativePath.setFillType(android.graphics.Path.FillType.EVEN_ODD);
		} else {
			this.nativePath.setFillType(android.graphics.Path.FillType.WINDING);
		}
	}

	public void setLineDash(float[] pattern, int count, float phase) {

	}

	public float[] getLineDash(int[] count, float[] phase) {
		return null;
	}

	public void setupStrokePropertiesInContext(Context context) {
		context.setLineWidth(this.lineWidth);
		context.setLineCap(this.lineCapStyle);
		context.setLineJoin(this.lineJoinStyle);
		// context.setFlatness(this.getFlatness());
		// context.setMiterLimit(this.getMiterLimit());
	}

	public void fill(Context context) {
		boolean antiAlias = context.getPaint().isAntiAlias();
		context.getPaint().setAntiAlias(true);

		final float scale = context.getScale();
		final android.graphics.Path path = this.getScaledNativePath(scale);

		if(context.getClipPath() != null && context.getClipPath() != this) {
			final Rect bounds = this.getBounds();

			context.drawClippedToPath(bounds, new Context.DrawClippedToPath() {
				public void drawClippedToPath(Canvas canvas, RectF rect, Paint paint) {
					android.graphics.Path path1 = new android.graphics.Path();
					path1.addPath(path);
					path1.offset(-bounds.origin.x * scale, -bounds.origin.y * scale);
					canvas.drawPath(path1, paint);
				}
			});
		} else {
			context.getCanvas().drawPath(path, context.getPaint());
		}

		context.getPaint().setAntiAlias(antiAlias);
	}

	public void stroke(Context context) {
		boolean antiAlias = context.getPaint().isAntiAlias();

		context.save();
		this.setupStrokePropertiesInContext(context);
		context.getPaint().setAntiAlias(true);
		context.getCanvas().drawPath(this.getScaledNativePath(context.getScale()), context.getStrokePaint());
		context.getPaint().setAntiAlias(antiAlias);
		context.restore();
	}

	// These methods do not affect the blend mode or alpha of the current graphics context
	public void fill(Context context, Context.BlendMode blendMode, float alpha) {
		boolean antiAlias = context.getPaint().isAntiAlias();

		context.save();
		context.setBlendMode(blendMode);
		context.setAlpha(alpha);
		context.getPaint().setAntiAlias(true);
		context.getCanvas().drawPath(this.getScaledNativePath(context.getScale()), context.getPaint());
		context.getPaint().setAntiAlias(antiAlias);
		context.restore();
	}

	public void stroke(Context context, Context.BlendMode blendMode, float alpha) {
		boolean antiAlias = context.getPaint().isAntiAlias();

		context.save();
		context.setBlendMode(blendMode);
		context.setAlpha(alpha);
		this.setupStrokePropertiesInContext(context);
		context.getStrokePaint().setAntiAlias(true);
		context.getCanvas().drawPath(this.getScaledNativePath(context.getScale()), context.getStrokePaint());
		context.getStrokePaint().setAntiAlias(antiAlias);
		context.restore();
	}

	public void addClip(Context context) {
		context.setClipPath(this);
	}

	android.graphics.Path getScaledNativePath(float scale) {
		if(this.cachedScaledPath == null || this.cachedScaledPathFactor != scale) {
			Matrix matrix = new Matrix();
			matrix.setScale(scale, scale);

			this.cachedScaledPath = new android.graphics.Path();
			this.cachedScaledPath.addPath(this.nativePath, matrix);
			this.cachedScaledPathFactor = scale;
		}

		return this.cachedScaledPath;
	}

	private void invalidateCache() {
		this.cachedBounds = null;
		this.cachedScaledPath = null;
		this.cachedScaledPathFactor = 0.0f;
	}

	public Path copy() {
		Path path = new Path();
		path.appendPath(this);
		return path;
	}
}
