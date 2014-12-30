/**
 *  @author Shaun
 *  @date 5/21/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.graphics;

public class TextAttachment {
	Image image;
	final Rect bounds;

	public TextAttachment() {
		this.bounds = new Rect();
	}

	public TextAttachment(Image image) {
		this.image = image;
		this.bounds = new Rect(image.getSize());
	}

	public Image getImage() {
		return this.image;
	}

	public void setImage(Image image) {
		this.image = image;

		if(this.image != null) {
			this.bounds.size.set(image.getSize());
		} else {
			this.bounds.size.set(null);
		}

		this.bounds.origin.set(null);
	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public void setBounds(Rect bounds) {
		this.bounds.set(bounds);
	}

	public void offset(Offset offset) {
		if(offset != null) {
			this.bounds.origin.x += offset.horizontal;
			this.bounds.origin.y += offset.vertical;
		}
	}
}
