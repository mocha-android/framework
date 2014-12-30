/*
 *  @author Shaun
 *	@date 10/30/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

public final class EdgeInsets implements mocha.foundation.Copying<EdgeInsets> {
	// specify amount to inset (positive) for each of the edges. values can be negative to 'outset'
	public float top;
	public float left;
	public float bottom;
	public float right;

	public static EdgeInsets zero() {
		return new EdgeInsets();
	}

	public EdgeInsets() {
		this(0.0f, 0.0f, 0.0f, 0.0f);
	}

	public EdgeInsets(float top, float left, float bottom, float right) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	public void set(EdgeInsets edgeInsets) {
		if(this != edgeInsets) {
			if (edgeInsets == null) {
				this.top = 0.0f;
				this.left = 0.0f;
				this.bottom = 0.0f;
				this.right = 0.0f;
			} else {
				this.top = edgeInsets.top;
				this.left = edgeInsets.left;
				this.bottom = edgeInsets.bottom;
				this.right = edgeInsets.right;
			}
		}
	}

	public boolean equals(EdgeInsets insets) {
		return (this == insets) || (this.left == insets.left && this.top == insets.top && this.right == insets.right && this.bottom == insets.bottom);
	}

	public Rect inset(Rect rect) {
		Rect rect1 = new Rect(rect);

		rect1.origin.x += this.left;
		rect1.origin.y += this.top;
		rect1.size.width -= (this.left + this.right);
		rect1.size.height -= (this.top  + this.bottom);

		return rect1;
	}

	@Override
	public String toString() {
		return String.format("{%s, %s, %s, %s}", this.top, this.left, this.bottom, this.right);
	}

	public EdgeInsets copy() {
		return new EdgeInsets(this.top, this.left, this.bottom, this.right);
	}

}
