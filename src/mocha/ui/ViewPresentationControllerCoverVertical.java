/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

import java.util.List;

class ViewPresentationControllerCoverVertical extends ViewPresentationController {

	ViewPresentationControllerCoverVertical(ViewController viewController) {
		super(viewController);
	}

	void presentViewController(final ViewController viewController, final ViewController hideViewController, boolean animated, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();
		animated = animated && window.isVisible();

		if(animated) {
			Rect startFrame = bounds.copy();
			startFrame.offset(0.0f, bounds.size.height);
			viewController.getView().setFrame(startFrame);
		} else {
			viewController.getView().setFrame(bounds);
		}

		viewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH, View.Autoresizing.FLEXIBLE_HEIGHT);
		viewController.setBeingPresented(true);

		if(window.isVisible()) {
			viewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);
		}

		window.addSubview(viewController.getView());


		if(animated) {
			final boolean restore = window.isUserInteractionEnabled();
			window.setUserInteractionEnabled(false);

			View.animateWithDuration(330, new View.Animations() {
						public void performAnimatedChanges() {
							viewController.getView().setFrame(bounds);
						}
					}, new View.AnimationCompletion() {
						public void animationCompletion(boolean finished) {
							presentViewControllerFinish(viewController, hideViewController, window, completion);
							window.setUserInteractionEnabled(restore);
						}
					});
		} else {
			this.presentViewControllerFinish(viewController, hideViewController, window, completion);
		}

	}

	void dismissPresentedViewController(final ViewController hideViewController, final ViewController revealViewController, final List<ViewController> dismissViewControllers, boolean animated, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();

		revealViewController.getView().setFrame(bounds);
		revealViewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		window.insertSubviewBelowSubview(revealViewController.getView(), hideViewController.getView());

		hideViewController.setBeingDismissed(true);

		revealViewController.beginAppearanceTransition(true, animated);
		hideViewController.beginAppearanceTransition(false, animated);

		if(animated) {
			final boolean restore = window.isUserInteractionEnabled();
			window.setUserInteractionEnabled(false);

			View.animateWithDuration(330, new View.Animations() {
						public void performAnimatedChanges() {
							Rect endFrame = bounds.copy();
							endFrame.offset(0.0f, bounds.size.height);
							hideViewController.getView().setFrame(endFrame);
						}
					}, new View.AnimationCompletion() {
						public void animationCompletion(boolean finished) {
							dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
							window.setUserInteractionEnabled(restore);
						}
					});
		} else {
			this.dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
		}
	}
}
