/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;
import mocha.graphics.Rect;

import java.util.List;

abstract class ViewPresentationController extends MObject {

	private ViewController viewController;

	public ViewPresentationController(ViewController viewController) {
		this.viewController = viewController;
	}

	void presentViewController(final ViewController viewController, final ViewController hideViewController, boolean animated, final Window window, final Runnable completion) {
		animated = animated && window.isVisible();

		if(animated) {
			this.presentViewControllerAnimated(viewController, hideViewController, window, new Runnable() {
				public void run() {
					presentViewControllerFinish(viewController, hideViewController, window, completion);
				}
			});
		} else {
			viewController.getView().setFrame(window.getBounds());
			viewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
			viewController.setBeingPresented(true);

			if(window.isVisible()) {
				viewController.beginAppearanceTransition(true, animated);
				hideViewController.beginAppearanceTransition(false, animated);
			}

			window.addSubview(viewController.getView());
			this.presentViewControllerFinish(viewController, hideViewController, window, completion);
		}
	}

	abstract protected void presentViewControllerAnimated(ViewController viewController, ViewController hideViewController, Window window, Runnable completion);

	private void presentViewControllerFinish(ViewController presentedViewController, ViewController hideViewController, Window window, Runnable completion) {
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

	void dismissPresentedViewController(final ViewController hideViewController, final ViewController revealViewController, final List<ViewController> dismissViewControllers, boolean animated, final Window window, final Runnable completion) {
		animated = animated && window.isVisible();

		if(animated) {
			this.dismissPresentedViewControllerAnimated(hideViewController, revealViewController, window, new Runnable() {
				public void run() {
					dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
				}
			});
		} else {
			revealViewController.getView().setFrame(window.getBounds());

			hideViewController.setBeingDismissed(true);

			revealViewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);

			window.addSubview(revealViewController.getView());
			this.dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
		}
	}

	abstract protected void dismissPresentedViewControllerAnimated(ViewController hideViewController, ViewController revealViewController, Window window, Runnable completion);

	private void dismissPresentedViewControllerFinish(ViewController hideViewController, ViewController revealViewController, List<ViewController> dismissViewControllers, Window window, Runnable completion) {
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
