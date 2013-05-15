/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.pm.ActivityInfo;

import java.util.EnumSet;
import java.util.Set;

public enum InterfaceOrientation {
	PORTRAIT,
	PORTRAIT_UPSIDE_DOWN,
	LANDSCAPE_LEFT,
	LANDSCAPE_RIGHT,
	UNKNOWN;

	public static final Set<InterfaceOrientation> SET_ALL = toSet(PORTRAIT, PORTRAIT_UPSIDE_DOWN, LANDSCAPE_LEFT, LANDSCAPE_RIGHT);
	public static final Set<InterfaceOrientation> SET_ALL_BUT_UPSIDE_DOWN = toSet(PORTRAIT, LANDSCAPE_LEFT, LANDSCAPE_RIGHT);
	public static final Set<InterfaceOrientation> SET_LANDSCAPE = toSet(LANDSCAPE_LEFT, LANDSCAPE_RIGHT);
	public static final Set<InterfaceOrientation> SET_PORTRAIT = toSet(PORTRAIT);

	public boolean isLandscape() {
		return this == LANDSCAPE_LEFT || this == LANDSCAPE_RIGHT;
	}

	public boolean isPortrait() {
		return this == PORTRAIT || this == PORTRAIT_UPSIDE_DOWN;
	}

	public static Set<InterfaceOrientation> toSet(InterfaceOrientation... interfaceOrientations) {
		if(interfaceOrientations != null && interfaceOrientations.length == 1) {
			return EnumSet.of(interfaceOrientations[0]);
		} else if(interfaceOrientations != null && interfaceOrientations.length > 0) {
			return EnumSet.of(interfaceOrientations[0], interfaceOrientations);
		} else {
			return EnumSet.noneOf(InterfaceOrientation.class);
		}
	}

	int toNativeOrientation(boolean nativeLandscape) {
		switch (this) {
			case PORTRAIT:
				return !nativeLandscape ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			case PORTRAIT_UPSIDE_DOWN:
				return !nativeLandscape ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			case LANDSCAPE_LEFT:
				return !nativeLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			case LANDSCAPE_RIGHT:
				return !nativeLandscape ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			case UNKNOWN:
			default:
				return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		}
	}
}
