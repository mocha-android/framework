package mocha.ui;

import android.util.FloatMath;
import mocha.graphics.Size;

public final class ScreenMath {
	private static float scaleF = Screen.mainScreen().getScale();
	private static double scaleD = (double) Screen.mainScreen().getScale();

	public static float floor(float v) {
		return FloatMath.floor(v * scaleF) / scaleF;
	}

	public static float ceil(float v) {
		return FloatMath.ceil(v * scaleF) / scaleF;
	}

	public static float round(float v) {
		return ((float) Math.round(v * scaleF)) / scaleF;
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


	public static Size floor(Size v) {
		return new Size(floor(v.width), floor(v.height));
	}

	public static Size ceil(Size v) {
		return new Size(ceil(v.width), ceil(v.height));
	}

	public static Size round(Size v) {
		return new Size(round(v.width), round(v.height));
	}

}
