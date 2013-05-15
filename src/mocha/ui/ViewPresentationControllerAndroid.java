/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.animation.TimingFunction;
import mocha.graphics.AffineTransform;
import mocha.graphics.Rect;

import java.util.List;

class ViewPresentationControllerAndroid extends ViewPresentationController {

	ViewPresentationControllerAndroid(ViewController viewController) {
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

		viewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		viewController.setBeingPresented(true);

		if(window.isVisible()) {
			viewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);
		}

		if(animated) {
			this.transitionViewController(hideViewController, viewController, true, window, new Runnable() {
				public void run() {
					presentViewControllerFinish(viewController, hideViewController, window, completion);

					if(completion != null) {
						completion.run();
					}
				}
			});
		} else {
			window.addSubview(viewController.getView());
			this.presentViewControllerFinish(viewController, hideViewController, window, completion);
		}
	}

	void transitionViewController(final ViewController fromViewController, final ViewController toViewController, final boolean presenting, final Window window, final Runnable completion) {
		performAfterDelay(0, new Runnable() {
			public void run() {
				_transitionViewController(fromViewController, toViewController, presenting, window, completion);
			}
		});
	}

	void _transitionViewController(final ViewController fromViewController, final ViewController toViewController, final boolean presenting, final Window window, final Runnable completion) {
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

	void dismissPresentedViewController(final ViewController hideViewController, final ViewController revealViewController, final List<ViewController> dismissViewControllers, boolean animated, final Window window, final Runnable completion) {
		final Rect bounds = window.getBounds();

		revealViewController.getView().setFrame(bounds);

		hideViewController.setBeingDismissed(true);

		revealViewController.beginAppearanceTransition(true, animated);
		hideViewController.beginAppearanceTransition(false, animated);

		if(animated) {
			this.transitionViewController(hideViewController, revealViewController, false, window, new Runnable() {
				public void run() {
					dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
				}
			});
		} else {
			window.addSubview(revealViewController.getView());
			this.dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
		}
	}

}
