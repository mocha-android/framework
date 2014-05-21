/*
 *  @author Shaun
 *	@date 11/20/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Point;
import mocha.graphics.Rect;

public class ImageView extends View implements Highlightable {
	private Image image;
	private Image highlightedImage;
	private boolean highlighted;

	public ImageView() { this(Rect.zero()); }

	public ImageView(Rect frame) { super(frame); }

	public ImageView(Image image) {
		this(image, null);
	}

	public ImageView(Image image, Image highlightedImage) {
		this(new Rect(Point.zero(), image.getSize()));

		this.image = image;
		this.highlightedImage = highlightedImage;
	}

	public ImageView(int imageResourceID) {
		this(Image.imageNamed(imageResourceID), null);
	}

	public ImageView(int imageResourceID, int highlightedImageResourceID) {
		this(Image.imageNamed(imageResourceID), Image.imageNamed(highlightedImageResourceID));
	}

	protected void onCreate(Rect frame) {
		super.onCreate(frame);
		this.setUserInteractionEnabled(false);
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		this.setNeedsDisplay();
	}

	public void setImage(int resourceID) {
		this.setImage(Image.imageNamed(resourceID));
	}

	public Image getHighlightedImage() {
		return highlightedImage;
	}

	public void setHighlightedImage(Image highlightedImage) {
		this.highlightedImage = highlightedImage;
	}

	public void setHighlightedImage(int resourceID) {
		this.setHighlightedImage(Image.imageNamed(resourceID));

		if(this.isHighlighted()) {
			this.setNeedsDisplay();
		}
	}

	public boolean isHighlighted() {
		return this.highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		if(this.highlighted != highlighted) {
			this.highlighted = highlighted;
			this.setNeedsDisplay();
		}
	}

	public void draw(Context context, Rect rect) {
		Image image = this.highlighted && this.highlightedImage != null ? this.highlightedImage : this.image;
		if(image == null) return;

		Rect imageRect = rect.copy();
		imageRect.size = image.getSize();

		ContentMode contentMode = this.getContentMode();

		if(contentMode == ContentMode.SCALE_TO_FILL) {
			imageRect.size = rect.size.copy();
		} else if(contentMode == ContentMode.SCALE_ASPECT_FILL || contentMode == ContentMode.SCALE_ASPECT_FIT) {
			float scaleX = rect.size.width / imageRect.size.width;
			float scaleY = rect.size.height / imageRect.size.height;
			float scale;

			if(contentMode == ContentMode.SCALE_ASPECT_FILL) {
				scale = Math.max(scaleX, scaleY);
			} else {
				scale = Math.min(scaleX, scaleY);
			}

			imageRect.size.width *= scale;
			imageRect.size.height *= scale;

			imageRect.origin.x += (rect.size.width - imageRect.size.width) / 2.0f;
			imageRect.origin.y += (rect.size.height - imageRect.size.height) / 2.0f;
		} else {
			switch (contentMode) {
				case TOP:
				case TOP_LEFT:
				case TOP_RIGHT:
					// Already aligned top
					break;

				case BOTTOM:
				case BOTTOM_LEFT:
				case BOTTOM_RIGHT:
					imageRect.origin.y += rect.size.height - imageRect.size.height;
					break;

				default:
					imageRect.origin.y += (rect.size.height - imageRect.size.height) / 2.0f;
					break;
			}

			switch (contentMode) {
				case LEFT:
				case TOP_LEFT:
				case BOTTOM_LEFT:
					// Already aligned left
					break;

				case RIGHT:
				case TOP_RIGHT:
				case BOTTOM_RIGHT:
					imageRect.origin.x += rect.size.width - imageRect.size.width;
					break;

				default:
					imageRect.origin.x += (rect.size.width - imageRect.size.width) / 2.0f;
					break;
			}
		}

		imageRect.makeIntegral();

		if(image.getRenderingMode() == Image.RenderingMode.ALWAYS_TEMPLATE) {
			image.draw(context, imageRect, this.getTintColor(), Context.BlendMode.NORMAL, 1.0f);
		} else {
			image.draw(context, imageRect);
		}
	}

	boolean getOverridesDraw() {
		return true;
	}

	public void tintColorDidChange() {
		super.tintColorDidChange();

		Image image = this.highlighted && this.highlightedImage != null ? this.highlightedImage : this.image;

		if(image != null && image.getRenderingMode() == Image.RenderingMode.ALWAYS_TEMPLATE) {
			this.setNeedsDisplay();
		}
	}

}
