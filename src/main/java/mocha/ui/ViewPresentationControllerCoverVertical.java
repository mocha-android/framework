/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

class ViewPresentationControllerCoverVertical extends ViewPresentationController {

	ViewPresentationControllerCoverVertical(ViewController viewController) {
		super(viewController);
	}

	protected void presentViewControllerAnimated(final ViewController viewController, final ViewController hideViewController, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();
		Rect startFrame = bounds.copy();
		startFrame.offset(0.0f, bounds.size.height);
		viewController.getView().setFrame(startFrame);

		viewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH, View.Autoresizing.FLEXIBLE_HEIGHT);
		viewController.setBeingPresented(true);

		viewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		window.addSubview(viewController.getView());

		View.animateWithDuration(330, new View.Animations() {
			public void performAnimatedChanges() {
				viewController.getView().setFrame(bounds);
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				if(completion != null) {
					completion.run();
				}
			}
		});
	}

	protected void dismissPresentedViewControllerAnimated(final ViewController hideViewController, final ViewController revealViewController, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();

		revealViewController.getView().setFrame(bounds);
		revealViewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		window.insertSubviewBelowSubview(revealViewController.getView(), hideViewController.getView());

		hideViewController.setBeingDismissed(true);

		revealViewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		View.animateWithDuration(330, new View.Animations() {
					public void performAnimatedChanges() {
				Rect endFrame = bounds.copy();
				endFrame.offset(0.0f, bounds.size.height);
				hideViewController.getView().setFrame(endFrame);
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				completion.run();
			}
		});
	}
}
