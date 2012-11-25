/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.util.DisplayMetrics;

import java.lang.ref.WeakReference;

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
	private WeakReference<Context> weakReference;

	private Screen(Activity activity) {
		this.weakReference = new WeakReference<Context>(activity);

		this.displayMetrics = activity.getResources().getDisplayMetrics();

		android.graphics.Point size = new android.graphics.Point();
		activity.getWindowManager().getDefaultDisplay().getSize(size);

		this.bounds = new mocha.graphics.Rect();
		this.bounds.size.width = size.x / getScale();
		this.bounds.size.height = size.y / getScale();
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

	Context getContext() {
		return this.weakReference.get();
	}
}
