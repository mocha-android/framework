/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

class ScrollIndicator extends View {

	private boolean visible;
	private ScrollView.IndicatorStyle indicatorStyle;
	private float thickness;

	public ScrollIndicator() {
		this.setUserInteractionEnabled(false);

		this.thickness = 4.0f;
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

			switch (indicatorStyle) {
				case WHITE:
					this.setBackgroundColor(0x84ffffff);
					break;
				case DEFAULT:
				case BLACK:
				default:
					this.setBackgroundColor(0x84000000);
					break;
			}
		}
	}

	public float getThickness() {
		return this.thickness;
	}

	public boolean isVisible() {
		return this.visible;
	}

}
