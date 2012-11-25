/*
 *  @author Shaun
 *	@date 10/30/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

public final class Size {
	public float width;
	public float height;

	public static Size zero() {
		return new Size();
	}

	public Size() {
		this(0.0f, 0.0f);
	}

	public Size(float width, float height) {
		this.width = width;
		this.height = height;
	}

	public Size(Size size) {
		this(size.width, size.height);
	}

	public boolean equals(Size size) {
		return size != null && ((this == size) || (this.width == size.width && this.height == size.height));
	}

	public String toString() {
		return String.format("[%sx%s]", ((Float)this.width), ((Float)this.height));
	}

	public void set(Size size) {
		this.width = size.width;
		this.height = size.height;
	}

	public Size copy() {
		return new Size(this.width, this.height);
	}

}
