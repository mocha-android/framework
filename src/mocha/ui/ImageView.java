/*
 *  @author Shaun
 *	@date 11/20/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

public class ImageView extends View {
	private Image image;

	public ImageView() {
	}

	public ImageView(Rect frame) {
		super(frame);
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}
}
