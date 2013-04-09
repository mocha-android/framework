/*
 *  @author Shaun
 *	@date 10/30/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.util.FloatMath;

public final class Point implements mocha.foundation.Copying <Point> {
	public float x;
	public float y;

	public static Point zero() {
		return new Point();
	}

	public Point() {
		this(0.0f, 0.0f);
	}

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Point(Point point) {
		this(point.x, point.y);
	}

	public Point(android.graphics.Point point) {
		this(point.x, point.y);
	}

	public Point(android.graphics.PointF pointF) {
		this(pointF.x, pointF.y);
	}

	public float distanceBetween(Point point) {
		if(point == null) return 0.0f;

		float deltaX = point.x - this.x;
		float deltaY = point.y - this.y;
		return FloatMath.sqrt(((deltaX * deltaX) + (deltaY * deltaY)));
	}

	public Point delta(Point point) {
		if(point == null) {
			return Point.zero();
		} else {
			return new Point(this.x - point.x, this.y - point.y);
		}
	}

	public Point abs() {
		return new Point(Math.abs(this.x), Math.abs(this.y));
	}

	public boolean equals(Point point) {
		return point != null && ((this == point) || (this.x == point.x && this.y == point.y));
	}

	public android.graphics.Point toPoint() {
		return new android.graphics.Point((int)this.x, (int)this.y);
	}

	public android.graphics.PointF toPointF() {
		return new android.graphics.PointF(this.x, this.y);
	}

	public String toString() {
		return String.format("{%s, %s}", this.x, this.y);
	}

	public Point copy() {
		return new Point(this.x, this.y);
	}

	public boolean isNaN() {
		return Float.isNaN(this.x) || Float.isNaN(this.y);
	}

	public boolean isInfinity() {
		return this.x == Float.POSITIVE_INFINITY || this.y == Float.POSITIVE_INFINITY || this.x == Float.NEGATIVE_INFINITY || this.y == Float.NEGATIVE_INFINITY;
	}

}
