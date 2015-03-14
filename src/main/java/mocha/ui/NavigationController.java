/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.OptionalInterface;
import mocha.foundation.OptionalInterfaceHelper;
import mocha.graphics.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigationController extends ViewController {
	public enum TransitionStyle {
		ANDROID, IOS, CUSTOM
	}

	public interface Delegate extends OptionalInterface {

		@OptionalInterface.Optional
		void willShowViewController(NavigationController navigationController, ViewController viewController, boolean animated);

		@OptionalInterface.Optional
		void didShowViewController(NavigationController navigationController, ViewController viewController, boolean animated);

	}

	public static final long HIDE_SHOW_BAR_DURATION = 330;

	private NavigationBar navigationBar;
	private List<ViewController> viewControllers;
	private boolean navigationBarHidden;
	private NavigationBar.Delegate navigationBarDelegate;
	private boolean inTransition;
	private boolean showHideNavigationBarDuringTransition;
	private TransitionStyle transitionStyle;
	private NavigationTransitionController transitionController;
	private Class<? extends NavigationTransitionController> transitionControllerClass;
	private View topView;
	private Delegate delegate;
	private boolean delegateWillShow;
	private boolean delegateDidShow;

	public NavigationController(Class<? extends NavigationBar> navigationBarClass) {
		this.viewControllers = new ArrayList<ViewController>();
		this.navigationBarDelegate = new NavigationBar.Delegate() {
			public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item) {
				return true;
			}

			public void didPushItem(NavigationBar navigationBar, NavigationItem item) {

			}

			public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item) {
				popViewControllerAnimated(true);
				return false;
			}

			public void didPopItem(NavigationBar navigationBar, NavigationItem item) {

			}
		};

		if(navigationBarClass != null) {
			try {
				this.navigationBar = navigationBarClass.getConstructor(Rect.class).newInstance(NavigationBar.INITIAL_RECT);
			} catch (NoSuchMethodException e) {
				try {
					this.navigationBar = navigationBarClass.newInstance();
				} catch (Exception e2) {
					throw new RuntimeException(e);
				}
			} catch(IllegalAccessException | InstantiationException | InvocationTargetException e2) {
				throw new RuntimeException(e2);
			}
		} else {
			this.navigationBar = new NavigationBar(NavigationBar.INITIAL_RECT);
		}

		this.navigationBar.setDelegate(this.navigationBarDelegate);
	}

	public NavigationController() {
		this((Class<? extends NavigationBar>)null);
	}

	public NavigationController(ViewController rootViewController) {
		this(rootViewController, null);
	}

	public NavigationController(ViewController rootViewController, Class<? extends NavigationBar> navigationBarClass) {
		this(navigationBarClass);

		this.transitionStyle = TransitionStyle.ANDROID;

		if(rootViewController != null) {
			this.addChildViewController(rootViewController);
			this.viewControllers.add(rootViewController);
			rootViewController.didMoveToParentViewController(this);
		}
	}

	public NavigationController(Class<? extends NavigationBar> navigationBarClass, Class<? extends Toolbar> toolbarClass) {
		// Toolbar not currently supported, this constructor exists purely for compatibility with the UIKit APIs
		this(null, navigationBarClass);
	}

	public TransitionStyle getTransitionStyle() {
		return transitionStyle;
	}

	public void setTransitionStyle(TransitionStyle transitionStyle) {
		if(this.isViewLoaded()) {
			throw new RuntimeException("You can not change a NavigationController's transitionStyle after it's view has loaded.");
		}

		this.transitionStyle = transitionStyle;
	}

	public Class<? extends NavigationTransitionController> getTransitionControllerClass() {
		return transitionControllerClass;
	}

	public void setTransitionControllerClass(Class<? extends NavigationTransitionController> transitionControllerClass) {
		this.transitionControllerClass = transitionControllerClass;
	}

	@Override
	protected void loadView() {
		super.loadView();

		if(this.transitionStyle == TransitionStyle.CUSTOM && this.transitionControllerClass != null) {
			try {
				this.transitionController = this.transitionControllerClass.getConstructor(NavigationController.class).newInstance(this);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.transitionController = this.transitionStyle == TransitionStyle.ANDROID ? new NavigationTransitionControllerAndroid(this) : new NavigationTransitionControlleriOS(this);
		}

		View view = this.getView();
		view.setClipsToBounds(true);

		Rect bounds = view.getBounds();

		float navBarHeight = this.navigationBar.sizeThatFits(bounds.size).height;

		if(this.navigationBarHidden) {
			this.navigationBar.setFrame(new Rect(0.0f, -navBarHeight, bounds.size.width, navBarHeight));
		} else {
			this.navigationBar.setFrame(new Rect(0.0f, 0.0f, bounds.size.width, navBarHeight));
		}

		this.navigationBar.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);

		view.addSubview(this.navigationBar);
	}

	@Override
	protected void viewDidLoad() {
		super.viewDidLoad();

		ViewController topViewController = this.getTopViewController();

		if(topViewController != null) {
			this.topView = topViewController.getView();
			this.topView.setFrame(this.getContentBounds());
			this.topView.setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
			this.getView().insertSubview(this.topView, 0);

			for(ViewController viewController : this.viewControllers) {
				this.navigationBar.pushNavigationItem(viewController.getNavigationItem(), false);
			}
		}
	}

	@Override
	public void viewDidAppear(boolean animated) {
		super.viewDidAppear(animated);
		this.promoteDeepestDefaultFirstResponder();
	}

	@Override
	public boolean canBecomeFirstResponder() {
		return true;
	}


	public TabBarItem getTabBarItem() {
		if(this.viewControllers.size() > 0) {
			return this.viewControllers.get(0).getTabBarItem();
		} else {
			return null;
		}
	}

	public Delegate getDelegate() {
		return this.delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
		this.delegateWillShow = OptionalInterfaceHelper.hasImplemented(delegate, Delegate.class, "willShowViewController", NavigationController.class, ViewController.class, boolean.class);
		this.delegateDidShow = OptionalInterfaceHelper.hasImplemented(delegate, Delegate.class, "didShowViewController", NavigationController.class, ViewController.class, boolean.class);
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

	/**
	 * Calls {@link #setViewControllers(java.util.List, boolean)} with animated as false
	 */
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

		List<NavigationItem> navigationItems = new ArrayList<NavigationItem>();

		for(ViewController viewController : this.viewControllers) {
			if(viewController.getParentViewController() != this) {
				this.addChildViewController(viewController);
			}

			navigationItems.add(viewController.getNavigationItem());
		}

		navigationItems.remove(navigationItems.size() - 1);

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
		this.navigationBar.setItemsWithoutUpdatingView(navigationItems);
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
			return Collections.emptyList();
		}
	}

	public List<ViewController> popToViewController(ViewController toViewController, boolean animated) {
		if(this.viewControllers.size() == 0 || !this.viewControllers.contains(toViewController) || this.getTopViewController() == toViewController) {
			return Collections.emptyList();
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

	private void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, boolean animated, final boolean push, final Runnable completion1) {
		animated = animated && fromViewController != null && toViewController != null;

		if(this.delegate != null && this.delegateWillShow) {
			this.delegate.willShowViewController(this, toViewController, animated);
		}

		final boolean _animated = animated;
		final Runnable completion = new Runnable() {
			public void run() {
				promoteDeepestDefaultFirstResponder();

				if(completion1 != null) {
					completion1.run();
				}

				if(delegate != null && delegateWillShow) {
					delegate.willShowViewController(NavigationController.this, toViewController, _animated);
				}
			}
		};

		if(toViewController == fromViewController || !this.isViewLoaded()) {
			completion.run();
		} else {
			final Rect bounds = this.getContentBounds();

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

				this.navigationBar.setDelegate(this.navigationBarDelegate);

				if(fromViewController != null) {
					fromViewController.beginAppearanceTransition(false, false);
					fromViewController.getView().removeFromSuperview();
					fromViewController.endAppearanceTransition();
				}

				if(toViewController != null) {
					this.topView = toViewController.getView();

					toViewController.beginAppearanceTransition(true, false);
					this.topView.setFrame(bounds);
					toViewController.beginAppearanceTransition(true, false);
					this.getView().insertSubview(this.topView, 0);
					toViewController.endAppearanceTransition();
				} else {
					this.topView = null;
				}

				completion.run();
			} else {
				this.transitionController.transitionFromViewController(fromViewController, toViewController, push, completion);
			}
		}
	}

	Rect getContentBounds() {
		Rect bounds = this.getView().getBounds();

		if(!this.navigationBarHidden) {
			float height = this.navigationBar.getFrame().size.height;
			bounds.origin.y += height;
			bounds.size.height -= height;
		}

		return bounds;
	}

	View getTopView() {
		return topView;
	}

	void setTopView(View topView) {
		this.topView = topView;
	}

	public void setShouldShowHideNavigationBarDuringTransition(boolean showHideNavigationBarDuringTransition) {
		this.showHideNavigationBarDuringTransition = showHideNavigationBarDuringTransition;
	}

	boolean getShouldShowHideNavigationBarDuringTransition() {
		return showHideNavigationBarDuringTransition;
	}

	boolean isInTransition() {
		return inTransition;
	}

	void setInTransition(boolean inTransition) {
		this.inTransition = inTransition;
	}

	public void backKeyPressed(Event event) {
		if(this.viewControllers.size() > 1) {
			this.popViewControllerAnimated(true);
		} else {
			super.backKeyPressed(event);
		}
	}

	Responder getDefaultFirstResponder() {
		if(this.viewControllers != null && this.viewControllers.size() > 0) {
			Responder responder = this.getTopViewController().getDefaultFirstResponder();

			if(responder != null && responder.isInCompleteResponderChain()) {
				return responder;
			}
		}

		return this;
	}

	/**
	 * @return Navigation bar visiblity
	 */
	public boolean isNavigationBarHidden() {
		return navigationBarHidden;
	}

	/**
	 * Change navigation bar visibility without animation
	 *
	 * @param hidden Set to true to hide the navgation bar, or false to show it.
	 */
	public void setNavigationBarHidden(boolean hidden) {
		this.setNavigationBarHidden(hidden, false);
	}

	/**
	 * Change navigation bar visibility
	 *
	 * @param hidden Set to true to hide the navgation bar, or false to show it.
	 * @param animated Set true to animate the changes, or false to change without animations.
	 */
	public void setNavigationBarHidden(boolean hidden, boolean animated) {
		this.setNavigationBarHidden(hidden, animated, null, null);
	}

	/**
	 * Change navigation bar visibility
	 *
	 * NOTE: The animations and completion callbacks are called regardless of whether or
	 * not the animated propery is true.
	 *
	 * @param hidden Set to true to hide the navgation bar, or false to show it.
	 * @param animated Set true to animate the changes, or false to change without animations.
	 * @param animations Callback to be called from within the animation block
	 * @param completion Completion to be called after animations have ended.
	 */
	public void setNavigationBarHidden(boolean hidden, boolean animated, final View.Animations animations, final View.AnimationCompletion completion) {
		boolean wasHidden = this.navigationBarHidden;
		this.navigationBarHidden = hidden;

		if(wasHidden == this.navigationBarHidden || !this.isViewLoaded()) {
			if(animations != null) {
				animations.performAnimatedChanges();
			}

			if(completion != null) {
				completion.animationCompletion(true);
			}
			return;
		}

		if(this.isBeingPresented() || this.isMovingToParentViewController() || (this.getParentViewController() == null && this.getPresentingViewController() == null) || this.getView().getSuperview() == null) {
			animated = false;
		}

		Rect bounds = this.getView().getBounds();

		final Rect navigationFrame;
		float navBarHeight = this.navigationBar.getFrame().size.height;

		if(this.navigationBarHidden) {
			navigationFrame = new Rect(0.0f, -navBarHeight, bounds.size.width, navBarHeight);
			navBarHeight = 0.0f;
		} else {
			navigationFrame = new Rect(0.0f, 0.0f, bounds.size.width, navBarHeight);
		}

		final Rect contentFrame = bounds.copy();
		contentFrame.origin.y += navBarHeight;
		contentFrame.size.height -= navBarHeight;

		if(!animated) {
			this.navigationBar.setFrame(navigationFrame);
			this.navigationBar.setHidden(this.navigationBarHidden);

			if(this.topView != null) {
				this.topView.setFrame(contentFrame);
			}

			if(animations != null) {
				animations.performAnimatedChanges();
			}

			if(completion != null) {
				completion.animationCompletion(true);
			}
		} else {
			this.navigationBar.setHidden(false);

			if(this.inTransition) {
				this.showHideNavigationBarDuringTransition = true;
				return;
			}

			View.animateWithDuration(HIDE_SHOW_BAR_DURATION, new View.Animations() {
				public void performAnimatedChanges() {
					navigationBar.setFrame(navigationFrame);

					if(topView != null) {
						topView.setFrame(contentFrame);
					}

					if(animations != null) {
						animations.performAnimatedChanges();
					}
				}
			}, new View.AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					if(navigationBarHidden && finished) {
						navigationBar.setHidden(true);
					}

					if(completion != null) {
						completion.animationCompletion(finished);
					}
				}
			});
		}
	}

}