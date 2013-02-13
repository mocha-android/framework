/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

class ScrollIndicator extends ImageView {

	static final float THICKNESS = 7.0f;
	static final float END_SIZE = 3.0f;

	enum Type {
		HORIZONTAL, VERTICAL
	}

	private boolean visible;
	private ScrollView.IndicatorStyle indicatorStyle;
	private Type type;

	public ScrollIndicator(Type type) {
		this.setUserInteractionEnabled(false);
		this.type = type;
	}

	public void setVisible(boolean visible) {
		if(this.visible != visible) {
			this.setAlpha(visible ? 1.0f : 0.0f);
			this.visible = visible;
		}
	}

	public void setIndicatorStyle(ScrollView.IndicatorStyle indicatorStyle) {
		if(indicatorStyle != this.indicatorStyle) {
			this.indicatorStyle = indicatorStyle;

			int resourceId;

			switch (indicatorStyle) {
				case BLACK:
					resourceId = R.drawable.mocha_scroll_indicator_black;
					break;
				case WHITE:
					resourceId = R.drawable.mocha_scroll_indicator_white;
					break;
				case DEFAULT:
				default:
					resourceId = R.drawable.mocha_scroll_indicator_default;
					break;
			}

			this.setImage(Image.imageNamed(resourceId).stretchableImage(3, 3));
		}
	}

	public boolean isVisible() {
		return this.visible;
	}

}
