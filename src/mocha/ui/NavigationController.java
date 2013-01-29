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
import java.util.List;

public class NavigationController extends ViewController implements NavigationBar.Delegate {
	private NavigationBar navigationBar;
	private View containerView;
	private List<ViewController> viewControllers;
	private boolean navigationBarHidden;
	private Label tmpPopLabel;

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

		this.tmpPopLabel = new Label(new Rect(6.0f, 6.0f, 38.0f, 28.0f));
		Rect bounds = this.tmpPopLabel.getBounds();
		bounds.inset(-10.0f, -10.0f);
		this.tmpPopLabel.setBounds(bounds);
		this.tmpPopLabel.setText("Pop");
		this.tmpPopLabel.setBackgroundColor(Color.GRAY);
		this.tmpPopLabel.setFont(Font.getBoldSystemFontWithSize(10.0f));
		this.tmpPopLabel.setTextAlignment(TextAlignment.CENTER);
		this.tmpPopLabel.setUserInteractionEnabled(true);
		this.tmpPopLabel.addGestureRecognizer(new TapGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				popViewControllerAnimated(true);
			}
		}));
		this.navigationBar.addSubview(tmpPopLabel);
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
			MLog("HERE");
		} else {
			MLog("NOT HERE?");
		}
	}

	public ViewController getTopViewController() {
		return this.viewControllers.size() > 0 ? this.viewControllers.get(0) : null;
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
		this.viewControllers.add(viewController);

		ViewController topViewController = this.getTopViewController();
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

		poppedViewController.willMoveToParentViewController(null);
		this.viewControllers.remove(size - 1);

		this.transitionFromViewController(poppedViewController, toViewController, animated, false, new Runnable() {
			public void run() {
				poppedViewController.removeFromParentViewController();
			}
		});
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

				this.transitionFromViewController(fromViewController, toViewController, 330, new View.Animations() {
					public void performAnimatedChanges() {
						Rect frame = bounds.copy();
						frame.origin.x = bounds.size.width * (push ? -1.0f : 1.0f);
						fromViewController.getView().setFrame(frame);

						toViewController.getView().setFrame(bounds);
					}
				}, new View.AnimationCompletion() {
					public void animationCompletion(boolean finished) {
						toViewController.getView().setFrame(bounds);
						if(completion != null) completion.run();
					}
				});
			}
		}
	}

	public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item) {
		return true;
	}

	public void didPushItem(NavigationBar navigationBar, NavigationItem item) {

	}

	public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item) {
		return true;
	}

	public void didPopItem(NavigationBar navigationBar, NavigationItem item) {

	}
}
