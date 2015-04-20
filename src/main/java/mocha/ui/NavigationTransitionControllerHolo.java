package mocha.ui;

import android.graphics.Bitmap;
import mocha.animation.TimingFunction;
import mocha.foundation.MObject;
import mocha.graphics.AffineTransform;
import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Rect;

class NavigationTransitionControllerHolo extends NavigationTransitionController {

	public NavigationTransitionControllerHolo(NavigationController navigationController) {
		super(navigationController);
	}

	public void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
		MObject.performAfterDelay(0, new Runnable() {
			public void run() {
				_transitionFromViewController(fromViewController, toViewController, push, completion);
			}
		});
	}

	void _transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
		final View fromView = fromViewController.getView();
		final View toView = toViewController.getView();

		final NavigationController navigationController = this.getNavigationController();
		final View view = navigationController.getView();
		Rect viewBounds = view.getBounds();

		fromViewController.beginAppearanceTransition(false, push);
		toViewController.beginAppearanceTransition(true, !push);

		toView.setFrame(navigationController.getContentBounds());

		long start = android.os.SystemClock.uptimeMillis();

		float navigationBarHeight = navigationController.getNavigationBar().getFrame().size.height;

		if (push) {
			this.adjustNavigationBar(toViewController.getNavigationItem(), true);
		}

		// Cache from view to image
		final ImageView transitionView = new ImageView(viewBounds);
		{
			try {
				Context context = new Context(viewBounds.size, view.scale, Bitmap.Config.ARGB_8888);
				context.save();
				context.getCanvas().translate(0.0f, navigationBarHeight * view.scale);

				if (push) {
					toView.getLayer().renderInContext(context);
				} else {
					fromView.getLayer().renderInContext(context);
				}

				context.restore();

				navigationController.getNavigationBar().getLayer().renderInContext(context);

				transitionView.setImage(context.getImage());
				view.addSubview(transitionView);
			} catch (OutOfMemoryError ignored) {

			}
		}

		if (push) {
			this.adjustNavigationBar(fromViewController.getNavigationItem(), false);
		} else {
			this.adjustNavigationBar(toViewController.getNavigationItem(), false);
		}

		MObject.MWarn("Took %dms to build UI cache", android.os.SystemClock.uptimeMillis() - start);

		// Animate
		final AffineTransform scaled = AffineTransform.scaled(0.8f, 0.8f);

		if (push) {
			transitionView.setAlpha(0.0f);
			transitionView.setTransform(scaled);
		} else {
			fromView.removeFromSuperview();
			navigationController.getView().addSubview(toView);
			navigationController.getView().bringSubviewToFront(navigationController.getNavigationBar());
			view.bringSubviewToFront(transitionView);
		}

		final boolean restore = view.isUserInteractionEnabled();
		view.setUserInteractionEnabled(false);

		View.animateWithDuration(300, 1, new View.Animations() {
			public void performAnimatedChanges() {
				View.setTimingFunction(new TimingFunction.CubicBezierCurveTimingFunction(0.215f, 0.610f, 0.355f, 1.000f));

				if (push) {
					transitionView.setAlpha(1.0f);
					transitionView.setTransform(AffineTransform.scaled(1.0f, 1.0f));
				} else {
					transitionView.setAlpha(0.0f);
					transitionView.setTransform(scaled);
				}
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				transitionView.removeFromSuperview();
				Image image = transitionView.getImage();
				transitionView.setImage(null);

				if (image != null) {
					image.recycle();
				}

				if (push) {
					adjustNavigationBar(toViewController.getNavigationItem(), true);
					navigationController.getView().addSubview(toView);
					navigationController.getView().bringSubviewToFront(navigationController.getNavigationBar());
					fromView.removeFromSuperview();
				}

				navigationController.setTopView(toView);

				fromViewController.endAppearanceTransition();
				toViewController.endAppearanceTransition();

				transitionView.removeFromSuperview();

				if (completion != null) {
					completion.run();
				}

				view.setUserInteractionEnabled(restore);
			}
		});

	}

	private void adjustNavigationBar(NavigationItem navigationItem, boolean push) {
		NavigationController navigationController = this.getNavigationController();
		NavigationBar navigationBar = navigationController.getNavigationBar();

		NavigationBar.Delegate delegate = navigationBar.getDelegate();
		navigationBar.setDelegate(null);

		if (push) {
			this.pushNavigationItem(navigationItem, false, null, null);
		} else {
			this.popToNavigationItemAnimated(navigationItem, false, null, null);
		}

		navigationBar.setDelegate(delegate);
	}

}
