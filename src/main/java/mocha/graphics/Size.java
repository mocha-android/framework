package mocha.graphics;

public final class Size implements mocha.foundation.Copying<Size> {
	public float width;
	public float height;

	public static Size zero() {
		return new Size();
	}

	public Size() {
		this.width = 0.0f;
		this.height = 0.0f;
	}

	public Size(float width, float height) {
		this.width = width;
		this.height = height;
	}

	public Size(Size size) {
		this(size.width, size.height);
	}

	public void set(Size size) {
		if (this != size) {
			if (size == null) {
				this.width = 0.0f;
				this.height = 0.0f;
			} else {
				this.width = size.width;
				this.height = size.height;
			}
		}
	}

	public boolean equals(Size size) {
		return size != null && ((this == size) || (this.width == size.width && this.height == size.height));
	}

	public String toString() {
		return String.format("{%s, %s}", this.width, this.height);
	}

	public Size copy() {
		return new Size(this.width, this.height);
	}

	public static Size min(Size a, Size b) {
		return new Size(Math.min(a.width, b.width), Math.min(a.height, b.height));
	}

}
