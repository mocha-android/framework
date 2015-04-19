/**
 *	@author Shaun
 *	@date 4/19/15
 *	@copyright 2015 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.AffineTransform;
import mocha.graphics.Rect;

class ViewPresentationControllerMaterial extends ViewPresentationController {

	ViewPresentationControllerMaterial(ViewController viewController) {
		super(viewController);
	}

	protected void presentViewControllerAnimated(ViewController viewController, ViewController hideViewController, Window window, final Runnable completion) {
		final View view = viewController.getView();
		view.setTransform(AffineTransform.translation(0.0f, view.getBoundsHeight() * 0.08f));
		view.setAlpha(0.0f);
		view.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH, View.Autoresizing.FLEXIBLE_HEIGHT);

		viewController.setBeingPresented(true);

		viewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		window.addSubview(view);

		View.animateWithDuration(300, new View.Animations() {
			public void performAnimatedChanges() {
				view.setTransform(AffineTransform.identity());
				view.setAlpha(1.0f);
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				if(completion != null) {
					completion.run();
				}
			}
		});
	}

	protected void dismissPresentedViewControllerAnimated(ViewController hideViewController, ViewController revealViewController, Window window, final Runnable completion) {
		revealViewController.getView().setFrame(window.getBounds());
		revealViewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		window.insertSubviewBelowSubview(revealViewController.getView(), hideViewController.getView());

		hideViewController.setBeingDismissed(true);

		revealViewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		final View view = hideViewController.getView();

		View.animateWithDuration(300, new View.Animations() {
					public void performAnimatedChanges() {
				view.setTransform(AffineTransform.translation(0.0f, view.getBoundsHeight() * 0.08f));
				view.setAlpha(0.0f);
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				completion.run();
			}
		});
	}
}
