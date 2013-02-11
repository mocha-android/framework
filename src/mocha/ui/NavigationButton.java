/*
 *  @author Shaun
 *	@date 1/30/13
 *	@copyright	2013 enormego All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Image;
import mocha.graphics.Rect;

class NavigationButton extends Button {

	static NavigationButton backButton(NavigationItem navigationItem) {
		if(navigationItem.getBackBarButtonItem() != null) {
			return new NavigationButton(navigationItem.getBackBarButtonItem().getTitle(), null, true);
		} else {
			return new NavigationButton(navigationItem.getTitle(), null, true);
		}
	}

	static NavigationButton button(BarButtonItem barButtonItem) {
		return new NavigationButton(barButtonItem.getTitle(), barButtonItem.getImage(), false);
	}

	private NavigationButton(String title, Image image, boolean back) {
		this.getTitleLabel().setFont(Font.getBoldSystemFontWithSize(12.0f));
		this.setContentEdgeInsets(new EdgeInsets(0.0f, back ? 15.0f : 7.0f, 0.0f, 7.0f));
		this.setTitle(title, State.NORMAL);
		this.setTitleColor(Color.WHITE, State.NORMAL);
		this.setTitleShadowColor(Color.BLACK, State.NORMAL);

		if(!back) {
			this.setImage(image, State.NORMAL);

			EdgeInsets insets = new EdgeInsets(0.0f, 5.0f, 0.0f, 5.0f);
			this.setBackgroundImage(Image.imageNamed(R.drawable.mocha_navigation_bar_default_button).resizableImageWithCapInsets(insets), State.NORMAL);
			this.setBackgroundImage(Image.imageNamed(R.drawable.mocha_navigation_bar_default_button_pressed).resizableImageWithCapInsets(insets), State.NORMAL, State.HIGHLIGHTED);

			this.setContentEdgeInsets(new EdgeInsets(0.0f, 9.0f, 0.0f, 9.0f));
			this.setImageEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));
			this.setTitleEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));
		} else {
			EdgeInsets insets = new EdgeInsets(6.0f, 14.0f, 6.0f, 6.0f);
			this.setBackgroundImage(Image.imageNamed(R.drawable.mocha_navigation_bar_default_back).resizableImageWithCapInsets(insets), State.NORMAL);
			this.setBackgroundImage(Image.imageNamed(R.drawable.mocha_navigation_bar_default_back_pressed).resizableImageWithCapInsets(insets), State.NORMAL, State.HIGHLIGHTED);

			this.setContentEdgeInsets(new EdgeInsets(0.0f, 14.0f, 0.0f, 9.0f));
			this.setImageEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));
			this.setTitleEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));
		}
	}

	public Rect getTitleRectForContentRect(Rect contentRect) {
		Rect rect = super.getTitleRectForContentRect(contentRect);
		rect.origin.x -= 1.0f;
		rect.size.width += 2.0f;
		return rect;
	}

}