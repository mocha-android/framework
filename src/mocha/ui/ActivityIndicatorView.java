/**
 *  @author Shaun
 *  @date 3/28/13
 *  @copyright 2013 TV Guide, Inc. All rights reserved.
 */
package mocha.ui;

// TODO: Build out.

public class ActivityIndicatorView extends View {
	
	public enum Style {
		WHITE_LARGE,
		WHITE,
		GRAY
	}

	private Style style;
	private boolean hidesWhenStopped;
	private int color;

	public ActivityIndicatorView(Style style) {
		this.style = style;
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public boolean isHidesWhenStopped() {
		return hidesWhenStopped;
	}

	public void setHidesWhenStopped(boolean hidesWhenStopped) {
		this.hidesWhenStopped = hidesWhenStopped;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void startAnimating() {

	}

	public void stopAnimating() {

	}

	public boolean isAnimating() {
		return false;
	}

}
