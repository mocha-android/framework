/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

public enum InterfaceOrientation {
	PORTRAIT,
	PORTRAIT_UPSIDE_DOWN,
	LANDSCAPE_LEFT,
	LANDSCAPE_RIGHT,
	UNKNOWN;

	public boolean isLandscape() {
		return this == LANDSCAPE_LEFT || this == LANDSCAPE_RIGHT;
	}

	public boolean isPortrait() {
		return this == PORTRAIT || this == PORTRAIT_UPSIDE_DOWN;
	}

}
