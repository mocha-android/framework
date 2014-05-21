/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import mocha.foundation.MObject;

public class Color extends MObject {
	public static final int BLACK = android.graphics.Color.BLACK;
	public static final int DKGRAY = android.graphics.Color.DKGRAY;
	public static final int GRAY = android.graphics.Color.GRAY;
	public static final int LTGRAY = android.graphics.Color.LTGRAY;
	public static final int WHITE = android.graphics.Color.WHITE;
	public static final int RED = android.graphics.Color.RED;
	public static final int GREEN = android.graphics.Color.GREEN;
	public static final int BLUE = android.graphics.Color.BLUE;
	public static final int YELLOW = android.graphics.Color.YELLOW;
	public static final int CYAN = android.graphics.Color.CYAN;
	public static final int MAGENTA = android.graphics.Color.MAGENTA;
	public static final int TRANSPARENT = android.graphics.Color.TRANSPARENT;

	public static int rgb(float red, float green, float blue) {
		return android.graphics.Color.rgb((int)Math.round(red * 255.0f), (int)Math.round(green * 255.0f), (int)Math.round(blue * 255.0f));
	}


	public static int hsb(float hue, float saturation, float brightness) {
		hue = (hue % 1 + 1) % 1;

		int i = (int)Math.floor(hue * 6);
		float f = hue * 6 - i;
		float p = brightness * (1 - saturation);
		float q = brightness * (1 - saturation * f);
		float t = brightness * (1 - saturation * (1 - f));

		float r, g, b;

		switch (i) {
			case 0:
				r = brightness; g = t; b = p;
				break;
			case 1:
				r = q; g = brightness; b = p;
				break;
			case 2:
				r = p; g = brightness; b = t;
				break;
			case 3:
				r = p; g = q; b = brightness;
				break;
			case 4:
				r = t; g = p; b = brightness;
				break;
			case 5:
				r = brightness; g = p; b = q;
				break;
			default:
				r = 0; g = 0; b = 0;
		}

		return rgb(r, g, b);
	}

	public static int hsba(float hue, float sat, float brightness, float alpha) {
		return colorWithAlpha(hsb(hue, sat, brightness), alpha);
	}

	public static int rgba(float red, float green, float blue, float alpha) {
		return android.graphics.Color.argb((int)Math.round(alpha * 255.0f), (int)Math.round(red * 255.0f), (int)Math.round(green * 255.0f), (int)Math.round(blue * 255.0f));
	}

	public static int rgb(int red, int green, int blue) {
		return android.graphics.Color.rgb(red, green, blue);
	}

	public static int rgba(int red, int green, int blue, int alpha) {
		return android.graphics.Color.argb(alpha, red, green, blue);
	}

	public static int white(float white) {
		return rgb(white, white, white);
	}
	
	public static int white(float white, float alpha) {
		return rgba(white, white, white, alpha);
	}

	public static int red(int color) {
		return android.graphics.Color.red(color);
	}

	public static int green(int color) {
		return android.graphics.Color.green(color);
	}

	public static int blue(int color) {
		return android.graphics.Color.blue(color);
	}

	public static int alpha(int color) {
		return android.graphics.Color.alpha(color);
	}

	public static float redf(int color) {
		return (float)android.graphics.Color.red(color) / 255.0f;
	}

	public static float greenf(int color) {
		return (float)android.graphics.Color.green(color) / 255.0f;
	}

	public static float bluef(int color) {
		return (float)android.graphics.Color.blue(color) / 255.0f;
	}

	public static float alphaf(int color) {
		return (float)android.graphics.Color.alpha(color) / 255.0f;
	}

	public static int colorWithAlpha(int color, float alpha) {
		return colorWithAlpha(color, Math.round(alpha * 255.0f));
	}

	public static int colorWithAlpha(int color, int alpha) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		return android.graphics.Color.argb(alpha, red, green, blue);
	}

	public static int[] components(int color) {
		return new int[] { Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color) };
	}

	public static float[] componentsf(int color) {
		return new float[] { Color.redf(color), Color.greenf(color), Color.bluef(color), Color.alphaf(color) };
	}

	public static String toString(int color) {
		float red = Color.redf(color);
		float green = Color.greenf(color);
		float blue = Color.bluef(color);
		float alpha = Color.alphaf(color);
		return String.format("<Color red = %.03f, green = %.03f, blue = %.03f, alpha = %.03f>", red, green, blue, alpha);
	}

}
