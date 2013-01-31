/*
 *  @author Shaun
 *	@date 1/29/13
 *	@copyright	2013 enormego All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;

public class BarItem extends mocha.foundation.Object {
	private Image image;
	private String title;
	private boolean enabled;
	private EdgeInsets imageInsets;
	private int tag;

	public BarItem() {
		this.enabled = true;
		this.imageInsets = EdgeInsets.zero();
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EdgeInsets getImageInsets() {
		return imageInsets;
	}

	public void setImageInsets(EdgeInsets imageInsets) {
		this.imageInsets = imageInsets;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}
}
