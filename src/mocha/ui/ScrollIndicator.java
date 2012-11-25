/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

class ScrollIndicator extends View {

	static final float THICKNESS = 7.0f;
	static final float END_SIZE = 3.0f;

	enum Type {
		HORIZONTAL, VERTICAL
	}

	private boolean visible;
	private ScrollView.IndicatorStyle indicatorStyle;

	public ScrollIndicator(Type type) {
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

			switch (indicatorStyle) {
				case DEFAULT:
					this.setBackgroundColor(Color.white(0.2f, 0.7f));
					break;
				case BLACK:
					this.setBackgroundColor(Color.white(0.0f, 0.75f));
					break;
				case WHITE:
					this.setBackgroundColor(Color.white(1.0f, 0.75f));
					break;
			}
		}
	}

	public boolean isVisible() {
		return this.visible;
	}

}
