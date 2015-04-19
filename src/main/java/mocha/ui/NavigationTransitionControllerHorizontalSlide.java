/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

class NavigationTransitionControllerHorizontalSlide extends NavigationTransitionController {

	public NavigationTransitionControllerHorizontalSlide(NavigationController navigationController) {
		super(navigationController);
	}

	public void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
		final NavigationController navigationController = this.getNavigationController();
		final NavigationBar navigationBar = navigationController.getNavigationBar();
		final Rect bounds = navigationController.getTopView() == null ? navigationController.getContentBounds() : navigationController.getTopView().getBounds();

		final Rect fromToFrame = bounds.copy();
		final Rect toFrame = bounds.copy();

		fromToFrame.origin.x = bounds.size.width * (push ? 1.0f : -1.0f);
		toViewController.getView().setFrame(fromToFrame);

		final Rect viewBounds = navigationController.getView().getBounds();

		navigationController.setInTransition(true);

		View.setAnimationsEnabled(false, new Runnable() {
			public void run() {
				fromViewController.getView().getSuperview().addSubview(toViewController.getView());
			}
		});

		fromViewController.beginAppearanceTransition(false, true);
		toViewController.beginAppearanceTransition(true, true);

		final Runnable transition = new Runnable() {
			public void run() {
				Application.sharedApplication().beginIgnoringInteractionEvents();

				Rect fromFrame = bounds.copy();
				fromFrame.origin.x = bounds.size.width * (push ? -1.0f : 1.0f);

				if(navigationController.getShouldShowHideNavigationBarDuringTransition()) {
					boolean restore = View.areAnimationsEnabled();
					View.setAnimationsEnabled(false);

					Rect containerFrame = viewBounds.copy();
					Rect fromFromFrame = viewBounds.copy();
					Rect fromNavigationFrame = navigationBar.getFrame();
					Rect toNavigationFrame = navigationBar.getFrame();
					toNavigationFrame.origin.y = 0.0f;
					fromNavigationFrame.origin.y = 0.0f;

					float navigationBarHeight = fromNavigationFrame.size.height;

					if(!navigationController.isNavigationBarHidden()) {
						containerFrame.origin.y += navigationBarHeight;
						containerFrame.size.height -= navigationBarHeight;

						toFrame.size.height -= navigationBarHeight;

						fromFromFrame.origin.y -= navigationBarHeight;
						fromFrame.origin.y -= navigationBarHeight;

						fromNavigationFrame.origin.x = viewBounds.size.width * (push ? 1.0f : -1.0f);
						toNavigationFrame.origin.x = 0.0f;
					} else {
						fromFromFrame.origin.y += navigationBarHeight;
						fromFromFrame.size.height -= navigationBarHeight;

						fromFrame.origin.y += navigationBarHeight;

						toNavigationFrame.origin.x = viewBounds.size.width * (push ? -1.0f : 1.0f);
						fromNavigationFrame.origin.x = 0.0f;
					}

					toFrame.origin.y = 0.0f;
					toFrame.size.height = containerFrame.size.height;

					fromToFrame.origin.y = 0.0f;
					fromToFrame.size.height = containerFrame.size.height;

					navigationBar.setFrame(fromNavigationFrame);
					if(navigationController.getTopView() != null) {
						navigationController.getTopView().setFrame(containerFrame);
					}
					fromViewController.getView().setFrame(fromFromFrame);
					toViewController.getView().setFrame(fromToFrame);

					View.setAnimationsEnabled(restore);

					navigationBar.setFrame(toNavigationFrame);
				}

				fromViewController.getView().setFrame(fromFrame);
				toViewController.getView().setFrame(toFrame);
			}
		};

		final Runnable complete = new Runnable() {
			public void run() {
				toViewController.getView().setFrame(toFrame);
				if(completion != null) completion.run();

				fromViewController.getView().removeFromSuperview();
				fromViewController.endAppearanceTransition();
				toViewController.endAppearanceTransition();

				navigationController.setInTransition(false);
				navigationController.setShouldShowHideNavigationBarDuringTransition(false);

				Application.sharedApplication().endIgnoringInteractionEvents();
			}
		};


		final NavigationBar.Delegate navigationBarDelegate = navigationBar.getDelegate();
		navigationBar.setDelegate(null);

		if(navigationController.getShouldShowHideNavigationBarDuringTransition()) {
			if(push) {
				navigationBar.pushNavigationItem(toViewController.getNavigationItem(), false);
				navigationBar.setDelegate(navigationBarDelegate);
			}

			View.animateWithDuration(NavigationController.HIDE_SHOW_BAR_DURATION, new View.Animations() {
						public void performAnimatedChanges() {
							transition.run();
						}
					}, new View.AnimationCompletion() {
						public void animationCompletion(boolean finished) {
							if(!push) {
								navigationBar.popToNavigationItemAnimated(toViewController.getNavigationItem(), false, null, null);
								navigationBar.setDelegate(navigationBarDelegate);
							}

							complete.run();
						}
					});
		} else {
			if(push) {
				navigationBar.pushNavigationItem(toViewController.getNavigationItem(), true, transition, complete);
			} else {
				navigationBar.popToNavigationItemAnimated(toViewController.getNavigationItem(), true, transition, complete);
			}

			navigationBar.setDelegate(navigationBarDelegate);
		}
	}

}
