/*
 *  @author Shaun
 *	@date 11/20/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Rect;

public class ImageView extends View {
	private Image image;

	public ImageView() { this(Rect.zero()); }

	public ImageView(Rect frame) {
		super(frame);

		this.setUserInteractionEnabled(false);
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public void draw(Context context, Rect rect) {
		if(this.image != null) {
			this.image.draw(context, rect);
		}
	}
}
