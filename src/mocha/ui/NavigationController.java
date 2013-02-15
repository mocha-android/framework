/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Rect;
import mocha.graphics.TextAlignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigationController extends ViewController implements NavigationBar.Delegate {
	private NavigationBar navigationBar;
	private View containerView;
	private List<ViewController> viewControllers;
	private boolean navigationBarHidden;

	public NavigationController(ViewController rootViewController) {
		this();

		if(rootViewController != null) {
			this.addChildViewController(rootViewController);
			this.viewControllers.add(rootViewController);
			rootViewController.didMoveToParentViewController(this);
		}
	}

	public NavigationController() {
		this.viewControllers = new ArrayList<ViewController>();

		this.navigationBar = new NavigationBar();
		this.navigationBar.setDelegate(this);
	}

	protected void loadView() {
		super.loadView();

		View view = this.getView();
		view.setClipsToBounds(true);

		Rect bounds = view.getBounds();

		float navBarHeight = 44.0f;
		this.navigationBar.setFrame(new Rect(0.0f, 0.0f, bounds.size.width, navBarHeight));
		this.navigationBar.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);

		Rect frame = bounds.copy();
		frame.origin.y += navBarHeight;
		frame.size.height -= navBarHeight;

		this.containerView = new View(frame);
		this.containerView.setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		this.containerView.setBackgroundColor(Color.BLACK);
		view.addSubview(this.containerView);

		view.addSubview(this.navigationBar);
	}

	protected void viewDidLoad() {
		super.viewDidLoad();

		ViewController topViewController = this.getTopViewController();

		if(topViewController != null) {
			View view = topViewController.getView();
			view.setFrame(this.containerView.getBounds());
			view.setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
			this.containerView.addSubview(view);

			for(ViewController viewController : this.viewControllers) {
				this.navigationBar.pushNavigationItem(viewController.getNavigationItem(), false);
			}
		}
	}

	public boolean canBecomeFirstResponder() {
		return true;
	}

	public NavigationBar getNavigationBar() {
		return navigationBar;
	}

	public ViewController getTopViewController() {
		int size;
		return (size = this.viewControllers.size()) > 0 ? this.viewControllers.get(size - 1) : null;
	}

	public List<ViewController> getViewControllers() {
		return Collections.unmodifiableList(viewControllers);
	}

	public void setViewControllers(List<ViewController> viewControllers) {
		this.setViewControllers(viewControllers, false);
	}

	public void setViewControllers(List<ViewController> viewControllers, boolean animated) {
		ViewController previousTopViewController = this.getTopViewController();

		final List<ViewController> previousViewControllers = new ArrayList<ViewController>(this.viewControllers);
		previousViewControllers.removeAll(viewControllers);

		this.viewControllers.clear();

		if(viewControllers != null) {
			this.viewControllers.addAll(viewControllers);
		}

		for(ViewController viewController : previousViewControllers) {
			viewController.willMoveToParentViewController(null);
		}

		for(ViewController viewController : this.viewControllers) {
			if(viewController.getParentViewController() != this) {
				this.addChildViewController(viewController);
			}
		}

		Runnable finish = new Runnable() {
			public void run() {
				for(ViewController viewController : previousViewControllers) {
					viewController.removeFromParentViewController();
				}

				for(ViewController viewController : NavigationController.this.viewControllers) {
					viewController.didMoveToParentViewController(NavigationController.this);
				}
			}
		};

		ViewController newTopViewController = this.getTopViewController();
		this.transitionFromViewController(previousTopViewController, newTopViewController, animated, true, finish);
	}

	public void pushViewController(final ViewController viewController, boolean animated) {
		if(this.viewControllers.contains(viewController)) return;

		animated = animated && this.viewControllers.size() > 0 && this.getView().getWindow() != null;

		ViewController topViewController = this.getTopViewController();
		this.viewControllers.add(viewController);

		this.addChildViewController(viewController);
		this.transitionFromViewController(topViewController, viewController, animated, true, new Runnable() {
			public void run() {
				viewController.didMoveToParentViewController(NavigationController.this);
			}
		});
	}

	public void popViewControllerAnimated(boolean animated) {
		int size;
		if((size = this.viewControllers.size()) < 2) return;

		final ViewController poppedViewController = this.viewControllers.get(size - 1);
		ViewController toViewController = this.viewControllers.get(size - 2);

		animated = animated && this.getView().getWindow() != null;

		poppedViewController.willMoveToParentViewController(null);
		this.viewControllers.remove(size - 1);

		this.transitionFromViewController(poppedViewController, toViewController, animated, false, new Runnable() {
			public void run() {
				poppedViewController.removeFromParentViewController();
			}
		});
	}

	public List<ViewController> popToRootViewControllerAnimated(boolean animated) {
		if(this.viewControllers.size() > 0) {
			return this.popToViewController(this.viewControllers.get(0), animated);
		} else {
			return new ArrayList<ViewController>();
		}
	}

	public List<ViewController> popToViewController(ViewController toViewController, boolean animated) {
		if(this.viewControllers.size() == 0 || !this.viewControllers.contains(toViewController) || this.getTopViewController() == toViewController) {
			return new ArrayList<ViewController>();
		}

		final List<ViewController> poppingViewControllers = new ArrayList<ViewController>(this.viewControllers.subList(this.viewControllers.indexOf(toViewController) + 1, this.viewControllers.size()));
		ViewController fromViewController = this.getTopViewController();
		this.viewControllers.removeAll(poppingViewControllers);

		this.transitionFromViewController(fromViewController, toViewController, animated, false, new Runnable() {
			public void run() {
				for(ViewController viewController : poppingViewControllers) {
					viewController.removeFromParentViewController();
				}
			}
		});

		return poppingViewControllers;
	}

	private void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, boolean animated, final boolean push, final Runnable completion) {
		animated = animated && fromViewController != null && toViewController != null;

		if(toViewController == fromViewController || !this.isViewLoaded()) {
			if(completion != null) completion.run();
		} else {
			final Rect bounds = this.containerView.getBounds();

			if(toViewController != null) {
				toViewController.getView().setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
			}

			if(!animated) {
				this.navigationBar.setDelegate(null);

				if(push) {
					if(toViewController != null) {
						this.navigationBar.pushNavigationItem(toViewController.getNavigationItem(), false);
					}
				} else {
					if(toViewController != null) {
						this.navigationBar.popToNavigationItemAnimated(toViewController.getNavigationItem(), false, null, null);
					}
				}

				this.navigationBar.setDelegate(this);

				if(fromViewController != null) {
					fromViewController.beginAppearanceTransition(false, false);
					fromViewController.getView().removeFromSuperview();
					fromViewController.endAppearanceTransition();
				}

				if(toViewController != null) {
					toViewController.beginAppearanceTransition(true, false);
					toViewController.getView().setFrame(bounds);
					toViewController.beginAppearanceTransition(true, false);
					this.containerView.addSubview(toViewController.getView());
					toViewController.endAppearanceTransition();

				}

				if(completion != null) completion.run();
			} else {
				// At this point, we're guaranteed to have a from and to view controller.
				Rect frame = bounds.copy();
				frame.origin.x = bounds.size.width * (push ? 1.0f : -1.0f);
				toViewController.getView().setFrame(frame);

				Runnable transition = new Runnable() {
					public void run() {
						Application.sharedApplication().beginIgnoringInteractionEvents();

						View.setAnimationsEnabled(false, new Runnable() {
							public void run() {
								fromViewController.getView().getSuperview().addSubview(toViewController.getView());
							}
						});

						fromViewController.beginAppearanceTransition(false, true);
						toViewController.beginAppearanceTransition(true, true);

						Rect frame = bounds.copy();
						frame.origin.x = bounds.size.width * (push ? -1.0f : 1.0f);
						fromViewController.getView().setFrame(frame);

						toViewController.getView().setFrame(bounds);
					}
				};

				Runnable complete = new Runnable() {
					public void run() {
						toViewController.getView().setFrame(bounds);
						if(completion != null) completion.run();

						fromViewController.getView().removeFromSuperview();
						fromViewController.endAppearanceTransition();
						toViewController.endAppearanceTransition();

						Application.sharedApplication().endIgnoringInteractionEvents();
					}
				};


				this.navigationBar.setDelegate(null);

				if(push) {
					this.navigationBar.pushNavigationItem(toViewController.getNavigationItem(), true, transition, complete);
				} else {
					this.navigationBar.popToNavigationItemAnimated(toViewController.getNavigationItem(), true, transition, complete);
				}

				this.navigationBar.setDelegate(this);
			}
		}
	}

	public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item) {
		return true;
	}

	public void didPushItem(NavigationBar navigationBar, NavigationItem item) {

	}

	public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item) {
		this.popViewControllerAnimated(true);
		return false;
	}

	public void didPopItem(NavigationBar navigationBar, NavigationItem item) {

	}

	public void backKeyPressed(Event event) {
		if(this.viewControllers.size() > 1) {
			this.popViewControllerAnimated(true);
		} else {
			super.backKeyPressed(event);
		}
	}
}
