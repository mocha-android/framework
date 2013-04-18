/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.util.DisplayMetrics;
import android.util.FloatMath;

public class Screen extends mocha.foundation.Object {
	private static Screen MAIN_SCREEN;

	static void setupMainScreen(Activity activity) {
		if(MAIN_SCREEN == null) {
			MAIN_SCREEN = new Screen(activity);
		}
	}

	public static Screen mainScreen() {
		if(MAIN_SCREEN == null) {
			throw new RuntimeException("Screen is unavailable until " + Activity.class.getCanonicalName() + " is instantiated.");
		}

		return MAIN_SCREEN;
	}

	private mocha.graphics.Rect bounds;
	private DisplayMetrics displayMetrics;

	@SuppressWarnings("SuspiciousNameCombination")
	private Screen(Activity activity) {
		this.displayMetrics = activity.getResources().getDisplayMetrics();

		android.graphics.Point size = new android.graphics.Point();
		activity.getWindowManager().getDefaultDisplay().getSize(size);

		float x = FloatMath.floor((size.x / getScale()) + 0.5f);
		float y = FloatMath.floor((size.y / getScale()) + 0.5f);
		this.bounds = new mocha.graphics.Rect();

		if(size.y > size.x) {
			this.bounds.size.width = x;
			this.bounds.size.height = y;
		} else {
			this.bounds.size.width = y;
			this.bounds.size.height = x;
		}
	}

	public float getScale() {
		return this.displayMetrics.density;
	}

	public int getDpi() {
		return this.displayMetrics.densityDpi;
	}

	public mocha.graphics.Rect getBounds() {
		return new mocha.graphics.Rect(this.bounds);
	}

}
