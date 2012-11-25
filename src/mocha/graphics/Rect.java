/*
 *  @author Shaun
 *	@date 10/30/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import mocha.ui.Offset;

public final class Rect {
	public Point origin;
	public Size size;

	public static Rect zero() {
		return new Rect();
	}

	public Rect() {
		this.origin = Point.zero();
		this.size = Size.zero();
	}

	public Rect(Point origin, Size size) {
		this.origin = origin.copy();
		this.size = size.copy();
	}

	public Rect(float x, float y, float width, float height) {
		this.origin = new Point(x, y);
		this.size = new Size(width, height);
	}

	public Rect(Rect rect) {
		this.origin = rect.origin.copy();
		this.size = rect.size.copy();
	}

	public Rect(android.graphics.Rect rect) {
		this(rect.left, rect.top, rect.width(), rect.height());
	}

	public Rect(android.graphics.RectF rectF) {
		this(rectF.left, rectF.top, rectF.width(), rectF.height());
	}

	public float minX() {
		return this.origin.x;
	}

	public float midX() {
		return this.origin.x + (this.size.width / 2.0f);
	}

	public float maxX() {
		return this.origin.x + this.size.width;
	}

	public float minY() {
		return this.origin.y;
	}

	public float midY() {
		return this.origin.y + (this.size.height / 2.0f);
	}

	public float maxY() {
		return this.origin.y + this.size.height;
	}

	public Point mid() {
		return new Point(this.midX(), this.midY());
	}

	public Point max() {
		return new Point(this.maxX(), this.maxY());
	}

	public float width() {
		return this.size.width;
	}

	public float height() {
		return this.size.height;
	}

	public boolean empty() {
		return this.size.width == 0.0f || this.size.height == 0.0f;
	}

	public void inset(float dx, float dy) {
		this.origin.x += dx;
		this.origin.y += dy;
		this.size.width -= (2.0f * dx);
		this.size.height -= (2.0f * dy);
	}

	public void offset(float dx, float dy) {
		this.origin.x += dx;
		this.origin.y += dy;
	}

	public void offset(Offset offset) {
		this.origin.x += offset.horizontal;
		this.origin.y += offset.vertical;
	}

	public boolean contains(Point point) {
		return point != null && (point.x >= this.origin.x  && point.y >= this.origin.y && point.x <= this.maxX() && point.y <= this.maxY());
	}

	public boolean contains(Rect rect) {
		return this.contains(rect.origin) && this.contains(rect.max());
	}

	public boolean intersects(Rect rect) {
		return this.contains(rect.origin) || rect.contains(this.origin) || this.contains(rect.max()) || rect.contains(this.max());
	}

	public Rect intersection(Rect rect) {
		Rect intersection = new Rect(Math.max(this.minX(), rect.minX()), Math.max(this.minY(), rect.minY()), 0.0f, 0.0f);
		intersection.size.width = Math.min(this.maxX(), rect.maxX()) - intersection.minX();
		intersection.size.height = Math.min(this.maxY(), rect.maxY()) - intersection.minY();
		return intersection.empty() ? null : intersection;
	}

	public boolean equals(Rect rect) {
		return rect != null && ((this == rect) || (this.origin.equals(rect.origin) && this.size.equals(rect.size)));
	}

	public android.graphics.Rect toSystemRect() {
		android.graphics.Rect rect = new android.graphics.Rect();
		rect.left = (int)this.origin.x;
		rect.right = (int)this.maxX();
		rect.top = (int)this.origin.y;
		rect.bottom = (int)this.maxY();
		return rect;
	}

	public android.graphics.Rect toSystemRect(float scale) {
		android.graphics.Rect rect = new android.graphics.Rect();
		rect.left = (int)(this.origin.x * scale);
		rect.right = (int)(this.maxX() * scale);
		rect.top = (int)(this.origin.y * scale);
		rect.bottom = (int)(this.maxY() * scale);
		return rect;
	}

	public Rect getScaledRect(float scale) {
		return new Rect(this.origin.x * scale, this.origin.y * scale, this.size.width * scale, this.size.height * scale);
	}

	public android.graphics.RectF toSystemRectF() {
		android.graphics.RectF rectF = new android.graphics.RectF();
		rectF.left = this.origin.x;
		rectF.right = this.maxX();
		rectF.top = this.origin.y;
		rectF.bottom = this.maxY();
		return rectF;
	}

	public android.graphics.RectF toSystemRectF(float scale) {
		android.graphics.RectF rectF = new android.graphics.RectF();
		rectF.left = this.origin.x * scale;
		rectF.right = this.maxX() * scale;
		rectF.top = this.origin.y  * scale;
		rectF.bottom = this.maxY() * scale;
		return rectF;
	}

	public String toString() {
		return String.format("(%s %s; %s %s)", (Float)this.origin.x, (Float)this.origin.y, (Float)this.size.width, (Float)this.size.height);
	}

	public Rect copy() {
		return new Rect(this);
	}

}
