/*
 *  @author Shaun
 *	@date 1/29/13
 *	@copyright	2013 enormego All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class Application extends mocha.foundation.Object {
	public static final String APPLICATION_DID_RECEIVE_MEMORY_WARNING_NOTIFICATION = "APPLICATION_DID_RECEIVE_MEMORY_WARNING";

	public interface Delegate {

	}

	static Application application;
	private int ignoreInteractionEventsLevel = 0;
	private Delegate delegate;
	private Activity activity;

	/**
	 * Returns the singleton application instance
	 *
	 * @return The application instance is created by Activity
	 */
	public static Application sharedApplication() {
		return application;
	}

	static void setSharedApplication(Application sharedApplication) {
		application = sharedApplication;
	}

	Application(Activity activity) {
		this.activity = activity;
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	public InterfaceOrientation getStatusBarOrientation() {
		final Display display = ((WindowManager)this.activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		int width = display.getWidth();
		int height = display.getHeight();
		int rotation = display.getRotation();

		if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
			int temp = height;
			//noinspection SuspiciousNameCombination
			height = width;
			width = temp;
		}

		boolean nativeLandscape = width > height;

		switch (display.getRotation()) {
			case Surface.ROTATION_0:
				if(nativeLandscape) {
					return InterfaceOrientation.LANDSCAPE_LEFT;
				} else {
					return InterfaceOrientation.PORTRAIT;
				}
			case Surface.ROTATION_90:
				if(nativeLandscape) {
					return InterfaceOrientation.PORTRAIT;
				} else {
					return InterfaceOrientation.LANDSCAPE_RIGHT;
				}
			case Surface.ROTATION_180:
				if(nativeLandscape) {
					return InterfaceOrientation.LANDSCAPE_RIGHT;
				} else {
					return InterfaceOrientation.PORTRAIT_UPSIDE_DOWN;
				}

			case Surface.ROTATION_270:
				if(nativeLandscape) {
					return InterfaceOrientation.PORTRAIT_UPSIDE_DOWN;
				} else {
					return InterfaceOrientation.LANDSCAPE_LEFT;
				}
		}

		return InterfaceOrientation.UNKNOWN;
	}

	/**
	 * Tells the receiver to suspend the handling of touch-related events
	 *
	 * You typically call this method before starting an animation or
	 * transition. Calls are nested with the endIgnoringInteractionEvents
	 * method.
	 */
	public void beginIgnoringInteractionEvents() {
		this.ignoreInteractionEventsLevel++;
	}

	/**
	 * Tells the receiver to resume the handling of touch-related events.
	 *
	 * You typically call this method when, after calling the
	 * beginIgnoringInteractionEvents method, the animation or transition
	 * concludes. Nested calls of this method should match nested calls of
	 * the beginIgnoringInteractionEvents method.
	 */
	public void endIgnoringInteractionEvents() {
		this.ignoreInteractionEventsLevel--;

		if(this.ignoreInteractionEventsLevel < 0) {
			throw new RuntimeException("Unbalanced calls to beginIgnoringInteractionEvents and endIgnoringInteractionEvents");
		}
	}

	/**
	 * Returns whether the receiver is ignoring events initiated by touches
	 * on the screen.
	 *
	 * @return true if the receiver is ignoring interaction events; otherwise
	 * false. The method returns true if the nested beginIgnoringInteractionEvents
	 * and endIgnoringInteractionEvents calls are at least one level deep.
	 */
	public boolean isIgnoringInteractionEvents() {
		return this.ignoreInteractionEventsLevel > 0;
	}

}
