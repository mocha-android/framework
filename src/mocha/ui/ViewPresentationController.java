/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;
import java.util.List;
import java.util.Set;

abstract class ViewPresentationController extends MObject {

	private ViewController viewController;

	public ViewPresentationController(ViewController viewController) {
		this.viewController = viewController;
	}

	void presentViewController(final ViewController viewController, final ViewController hideViewController, boolean animated, final Window window, final Runnable completion) {
		InterfaceOrientation presentOrientation = viewController.getPreferredInterfaceOrientationForPresentation();

		if(presentOrientation == null) {
			Set<InterfaceOrientation> supportedOrientations = viewController.getSupportedInterfaceOrientations();

			if(!supportedOrientations.contains(hideViewController.getInterfaceOrientation())) {
				presentOrientation = supportedOrientations.iterator().next();
			}
		}

		if(presentOrientation != null && presentOrientation == hideViewController.getInterfaceOrientation()) {
			presentOrientation = null;
		}

		animated = animated && window.isVisible() && presentOrientation == null;

		if(animated) {
			this.presentViewControllerAnimated(viewController, hideViewController, window, new Runnable() {
				public void run() {
					presentViewControllerFinish(viewController, hideViewController, window, completion);
				}
			});
		} else {
			hideViewController.setRestoreToInterfaceOrientationOnReappear(hideViewController.getInterfaceOrientation());

			if(presentOrientation != null) {
				window.setIgnoreNextRotationEvent();
				hideViewController.getView().setAutoresizing(View.Autoresizing.NONE);
				window.setOrientation(presentOrientation);
			}

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
		InterfaceOrientation restoreOrientation = revealViewController.getRestoreToInterfaceOrientationOnReappear();
		revealViewController.setRestoreToInterfaceOrientationOnReappear(null);

		if(restoreOrientation != null && restoreOrientation == hideViewController.getInterfaceOrientation()) {
			restoreOrientation = null;
		}

		animated = animated && window.isVisible() && restoreOrientation == null;

		if(animated) {
			this.dismissPresentedViewControllerAnimated(hideViewController, revealViewController, window, new Runnable() {
				public void run() {
					dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
				}
			});
		} else {
			if(restoreOrientation != null) {
				window.setIgnoreNextRotationEvent();
				hideViewController.getView().setAutoresizing(View.Autoresizing.NONE);
				window.setOrientation(restoreOrientation);
			}

			revealViewController.getView().setFrame(window.getBounds());
			revealViewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);

			revealViewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);
			hideViewController.setBeingDismissed(true);

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
