/*
 *  @author Shaun
 *	@date 1/29/13
 *	@copyright	2013 TV Guide, Inc. All rights reserved.
 */
package mocha.ui;

public class Application {

	static Application application;
	private int ignoreInteractionEventsLevel = 0;

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
