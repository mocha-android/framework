/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

public class Color extends mocha.foundation.Object {
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

	public static int rgba(float red, float green, float blue, float alpha) {
		return android.graphics.Color.argb((int)Math.round(alpha * 255.0f), (int)Math.round(red * 255.0f), (int)Math.round(green * 255.0f), (int)Math.round(blue * 255.0f));
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

}
