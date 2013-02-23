/**
 *  @author Shaun
 *  @date 2/11/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.graphics;

import android.util.FloatMath;

public class AffineTransform extends mocha.foundation.Object {
	private static final AffineTransform IDENTITY = new AffineTransform(1, 0, 0, 1, 0, 0);

	private float a, b, c, d;
	private float tx;
	private float ty;

	public enum AngleUnit {
		DEGREES, RADIANS
	}

	public static AffineTransform identity() {
		return IDENTITY.copy();
	}

	public static AffineTransform translation(float tx, float ty) {
		return new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, tx, ty);
	}

	public static AffineTransform scaled(float sx, float sy) {
		return new AffineTransform(sx, 0.0f, 0.0f, sy, 0.0f, 0.0f);
	}

	public static AffineTransform rotation(float value, AngleUnit angleUnit) {
		if(angleUnit == AngleUnit.DEGREES) {
			return rotation((float)Math.toRadians(value));
		} else {
			return rotation(value);
		}
	}

	public static AffineTransform rotation(float radians) {
		float cos = FloatMath.cos(radians);
		float sin = FloatMath.sin(radians);
		return new AffineTransform(cos, sin, -sin, cos, 0.0f, 0.0f);
	}

	public AffineTransform(float a, float b, float c, float d, float tx, float ty) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.tx = tx;
		this.ty = ty;
	}

	public void translate(float tx, float ty) {
		this.tx = this.tx + (this.a * tx) + (this.c * ty);
		this.ty = (this.ty + this.b) * tx + (this.d * ty);
	}

	public void scale(float sx, float sy) {
		this.a *= sx;
		this.b *= sx;
		this.c *= sy;
		this.d *= sy;
	}

	public void rotate(float value, AngleUnit angleUnit) {
		if(angleUnit == AngleUnit.DEGREES) {
			this.rotate((float)Math.toRadians(value));
		} else {
			this.rotate(value);
		}
	}

	public void rotate(float radians) {
		float sin = FloatMath.sin(radians);
		float cos = FloatMath.cos(radians);

		float a = this.a;
		float c = this.c;
		float b = this.b;
		float d = this.d;

		this.a = (cos * a) + (sin * c);
		this.c = (-sin * a) + (cos * c);

		this.b = (cos * b) + (sin * d);
		this.d = (-sin * b) + (cos * d);
	}

	public void concat(AffineTransform transform) {
		float a = (this.a * transform.a) + (this.b * transform.c);
		float b = (this.a * transform.b) + (this.b * transform.d);
		float c = (this.c * transform.a) + (this.d * transform.c);
		float d = (this.c * transform.b) + (this.d * transform.d);
		float tx = (this.tx * transform.a) + (this.ty * transform.c + transform.tx);
		float ty = (this.tx * transform.b) + (this.ty * transform.d + transform.ty);

		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.tx = tx;
		this.ty = ty;
	}

	public void invert() {
		float determinant = (this.a * this.d) - (this.c * this.b);

		if(determinant == 0){
			return;
		}

		float a = this.d /determinant;
		float b = -this.b / determinant;
		float c = -this.c / determinant;
		float d = this.a / determinant;
		float tx = ((-this.d * this.tx) + (this.c * this.ty)) / determinant;
		float ty = ((this.b * this.tx) - (this.a * this.ty)) / determinant;

		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.tx = tx;
		this.ty = ty;
	}

	public AffineTransform copy() {
		return new AffineTransform(this.a, this.b, this.c, this.d, this.tx, this.ty);
	}

	public boolean isIdentity() {
		return this.equals(IDENTITY);
	}

	public boolean equals(Object object) {
		if(object == this) return true;

		if(object instanceof AffineTransform) {
			AffineTransform transform = (AffineTransform)object;
			return this.a == transform.a && this.b == transform.b && this.c == transform.c && this.d == transform.d && this.tx == transform.tx && this.ty == transform.ty;
		} else {
			return false;
		}
	}

	public float getA() {
		return a;
	}

	public float getB() {
		return b;
	}

	public float getC() {
		return c;
	}

	public float getD() {
		return d;
	}

	public float getTx() {
		return tx;
	}

	public float getTy() {
		return ty;
	}

	public Point apply(Point point) {
		Point transformed = new Point();

		transformed.x = (point.x * this.a) + (point.y * this.c) + this.tx;
		transformed.y = (point.x * this.b) + (point.y * this.d) + this.ty;

		return transformed;
	}

	public Size apply(Size size) {
		Size transformed = new Size();

		transformed.width = (size.width * this.a) + (size.height * this.c);
		transformed.height = (size.width * this.b) + (size.height * this.d);

		return transformed;
	}

	public Rect apply(Rect rect) {
		float top = rect.minY();
		float left = rect.minX();
		float right = rect.maxX();
		float bottom = rect.maxY();

		Point topLeft = this.apply(new Point(left, top));
		Point topRight = this.apply(new Point(right, top));
		Point bottomLeft = this.apply(new Point(left, bottom));
		Point bottomRight = this.apply(new Point(right, bottom));

		float minX = Math.min(Math.min(Math.min(topLeft.x, topRight.x), bottomLeft.x), bottomRight.x);
		float maxX = Math.max(Math.max(Math.max(topLeft.x, topRight.x), bottomLeft.x), bottomRight.x);
		float minY = Math.min(Math.min(Math.min(topLeft.y, topRight.y), bottomLeft.y), bottomRight.y);
		float maxY = Math.max(Math.max(Math.max(topLeft.y, topRight.y), bottomLeft.y), bottomRight.y);

		return new Rect(minX, minY, (maxX - minX), (maxY - minY));
	}

	public String toString() {
		return String.format("[%s, %s, %s, %s, %s, %s]", this.a, this.b, this.c, this.d, this.tx, this.ty);
	}

}