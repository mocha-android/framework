/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.animation.TimingFunction;
import mocha.graphics.AffineTransform;
import mocha.graphics.Rect;

class ViewPresentationControllerAndroid extends ViewPresentationController {

	ViewPresentationControllerAndroid(ViewController viewController) {
		super(viewController);
	}

	protected void presentViewControllerAnimated(final ViewController viewController, final ViewController hideViewController, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();
		Rect startFrame = bounds.copy();
		startFrame.offset(0.0f, bounds.size.height);
		viewController.getView().setFrame(startFrame);

		viewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		viewController.setBeingPresented(true);

		viewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		this.transitionViewController(hideViewController, viewController, true, window, new Runnable() {
			public void run() {
				if(completion != null) {
					completion.run();
				}
			}
		});
	}

	protected void dismissPresentedViewControllerAnimated(final ViewController hideViewController, final ViewController revealViewController, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();

		revealViewController.getView().setFrame(bounds);

		hideViewController.setBeingDismissed(true);

		revealViewController.beginAppearanceTransition(true, true);
		hideViewController.beginAppearanceTransition(false, true);

		this.transitionViewController(hideViewController, revealViewController, false, window, new Runnable() {
			public void run() {
				if(completion != null) {
					completion.run();
				}
			}
		});
	}

	private void transitionViewController(final ViewController fromViewController, final ViewController toViewController, final boolean presenting, final Window window, final Runnable completion) {
		performAfterDelay(0, new Runnable() {
			public void run() {
				_transitionViewController(fromViewController, toViewController, presenting, window, completion);
			}
		});
	}

	private void _transitionViewController(final ViewController fromViewController, final ViewController toViewController, final boolean presenting, final Window window, final Runnable completion) {
		final View fromView = fromViewController.getView();
		final View toView = toViewController.getView();
		final Rect bounds = window.getBounds();

		final boolean restore = window.isUserInteractionEnabled();
		window.setUserInteractionEnabled(false);

		toView.setFrame(bounds);
		toView.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH, View.Autoresizing.FLEXIBLE_HEIGHT);

		// Animate
		final AffineTransform scaled = AffineTransform.scaled(0.8f, 0.8f);

		if(presenting) {
			toView.setAlpha(0.0f);
			toView.setTransform(scaled);
			window.addSubview(toView);
		} else {
			window.addSubview(toView);
			window.bringSubviewToFront(fromView);
		}

		View.animateWithDuration(300, 1, new View.Animations() {
					public void performAnimatedChanges() {
				View.setTimingFunction(new TimingFunction.CubicBezierCurveTimingFunction(0.215f, 0.610f, 0.355f, 1.000f));

				if(presenting) {
					toView.setAlpha(1.0f);
					toView.setTransform(AffineTransform.scaled(1.0f, 1.0f));
				} else {
					fromView.setAlpha(0.0f);
					fromView.setTransform(scaled);
				}
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				window.setUserInteractionEnabled(restore);

				if(completion != null) {
					completion.run();
				}
			}
		});
	}

}
