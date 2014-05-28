/**
 *  @author Shaun
 *  @date 5/26/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Offset;

public class TabBarItem extends BarItem {
	private String badgeValue;
	private Image selectedImage;
	private Offset titlePositionAdjustment;

	public TabBarItem(String title, Image image, int tag) {
		super();

		this.setTitle(title);
		this.setImage(image);
		this.setTag(tag);
	}

	public TabBarItem(String title, Image image, Image selectedImage) {
		super();

		this.setTitle(title);
		this.setImage(image);
		this.setSelectedImage(selectedImage);
	}

	public String getBadgeValue() {
		return badgeValue;
	}

	public void setBadgeValue(String badgeValue) {
		this.badgeValue = badgeValue;
	}

	public Image getSelectedImage() {
		return selectedImage;
	}

	public void setSelectedImage(Image selectedImage) {
		this.selectedImage = selectedImage;
	}

	public Offset getTitlePositionAdjustment() {
		return titlePositionAdjustment;
	}

	public void setTitlePositionAdjustment(Offset titlePositionAdjustment) {
		this.titlePositionAdjustment = titlePositionAdjustment;
	}

}
