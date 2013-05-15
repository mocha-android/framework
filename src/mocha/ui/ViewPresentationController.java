/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;

import java.util.List;

abstract class ViewPresentationController extends MObject {

	private ViewController viewController;

	public ViewPresentationController(ViewController viewController) {
		this.viewController = viewController;
	}

	abstract void presentViewController(final ViewController viewController, final ViewController hideViewController, final boolean animated, final Window window, final Runnable completion);

	protected void presentViewControllerFinish(ViewController presentedViewController, ViewController hideViewController, Window window, Runnable completion) {
		if(window.isVisible()) {
			presentedViewController.endAppearanceTransition();
			hideViewController.endAppearanceTransition();
		}

		hideViewController.getView().removeFromSuperview();

		presentedViewController.setBeingPresented(false);

		if(completion != null) {
			completion.run();
		}
	}

	abstract void dismissPresentedViewController(ViewController hideViewController, ViewController revealViewController, List<ViewController> dismissViewControllers, boolean animated, Window window, final Runnable completion);

	protected void dismissPresentedViewControllerFinish(ViewController hideViewController, ViewController revealViewController, List<ViewController> dismissViewControllers, Window window, Runnable completion) {
		hideViewController.getView().removeFromSuperview();

		for(ViewController dismissViewController : dismissViewControllers) {
			viewController.removePresentedViewController(dismissViewController, window);
		}

		hideViewController.endAppearanceTransition();
		revealViewController.endAppearanceTransition();

		if(completion != null) {
			completion.run();
		}

		hideViewController.setBeingDismissed(false);

		if(revealViewController == viewController) {
			viewController.presentingFromWindow = null;
		}
	}
}
