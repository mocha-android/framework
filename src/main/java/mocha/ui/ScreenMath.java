/**
 *  @author Shaun
 *  @date 4/18/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;

public final class ScreenMath {
	private static float scaleF = Screen.mainScreen().getScale();
	private static double scaleD = (double)Screen.mainScreen().getScale();

	public static float floor(float v) {
		return FloatMath.floor(v * scaleF) / scaleF;
	}

	public static float ceil(float v) {
		return FloatMath.ceil(v * scaleF) / scaleF;
	}

	public static float round(float v) {
		return ((float)Math.round(v * scaleF)) / scaleF;
	}

	public static double floor(double v) {
		return Math.floor(v * scaleD) / scaleD;
	}

	public static double ceil(double v) {
		return Math.ceil(v * scaleD) / scaleD;
	}

	public static double round(double v) {
		return Math.round(v * scaleD) / scaleD;
	}

}
