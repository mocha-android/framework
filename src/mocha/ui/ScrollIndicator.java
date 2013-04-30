/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

class ScrollIndicator extends ImageView {

	private boolean visible;
	private ScrollView.IndicatorStyle indicatorStyle;
	private float thickness;

	public ScrollIndicator() {
		this.setUserInteractionEnabled(false);
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
			int capSize;

			switch (indicatorStyle) {
				case BLACK:
					resourceId = R.drawable.mocha_scroll_indicator_black;
					capSize = 2;
					break;
				case WHITE:
					resourceId = R.drawable.mocha_scroll_indicator_white;
					capSize = 2;
					break;
				case DEFAULT:
				default:
					resourceId = R.drawable.mocha_scroll_indicator_default;
					capSize = 3;
					break;
			}

			this.thickness = (capSize * 2) + 1.0f;
			this.setImage(Image.imageNamed(resourceId).stretchableImage(capSize, capSize));
		}
	}

	public float getThickness() {
		return thickness;
	}

	public boolean isVisible() {
		return this.visible;
	}

}
