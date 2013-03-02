/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.NotificationCenter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewController extends Responder {
	private View view;
	private ViewController parentViewController;
	private List<ViewController> childViewControllers;
	private int appearanceTransitionIsAppearing;
	private int appearanceTransitionAnimated;
	private NavigationItem navigationItem;
	private String title;
	private Responder nextResponder;

	private static Method didReceiveMemoryWarningMethod;

	public ViewController() {
		this.childViewControllers = new ArrayList<ViewController>();

		if(didReceiveMemoryWarningMethod == null) {
			try {
				didReceiveMemoryWarningMethod = ViewController.class.getMethod("didReceiveMemoryWarning");
			} catch (NoSuchMethodException e) {
				MWarn(e, "Could not find memory warning method.");
			}
		}

		NotificationCenter.defaultCenter().addObserver(this, didReceiveMemoryWarningMethod, Application.APPLICATION_DID_RECEIVE_MEMORY_WARNING_NOTIFICATION, null);
	}

	/**
	 * Called by getView if no view has been loaded. Should never be called directly.
	 */
	protected void loadView() {
		this.setView(new View(Screen.mainScreen().getBounds()));
	}

	/**
	 * The first time this is called, a call to loadView() will be called.
	 * Subclasses must call super.
	 *
	 * @return view
	 */
	public View getView() {
		if(this.view == null) {
			this.loadView();

			if(this.view == null) {
				throw new RuntimeException(this.getClass().getCanonicalName() + " did not set a view in loadView().");
			} else {
				this.viewDidLoad();
			}
		}

		return this.view;
	}

	public void setView(View view) {
		boolean hadView = this.view != null;

		if(hadView) {
			this.view._setViewController(null);

			if(view == null) {
				this.viewWillUnload();
			}
		}

		this.view = view;

		if(this.view != null) {
			this.view._setViewController(this);
		} else if(hadView) {
			this.viewDidUnload();
		}
	}

	/**
	 * Called directly after loadView has finished and set the view.
	 */
	protected void viewDidLoad() {

	}

	public boolean isViewLoaded() {
		return this.view != null;
	}

	protected void viewWillUnload() {

	}

	/**
	 * Called after the view controller's view has been set to null.
	 * For example, this can be called if a view controller's view is off screen
	 * and the application is running low on memory.
	 */
	protected void viewDidUnload() {

	}

	/**
	 * Called when the view is about to made visible. Default does nothing
	 *
	 * @param animated Whether or not the transition will be animated
	 */
	public void viewWillAppear(boolean animated) {
		this.notifyChildrenAppearanceTransitionBegin(true, animated);
	}

	/**
	 * Called when the view has been fully transitioned onto the screen. Default does nothing
	 *
	 * @param animated Whether or not the transition was animated
	 */
	public void viewDidAppear(boolean animated) {
		this.notifyChildrenAppearanceTransitionEnded();
	}

	/**
	 * Called when the view is dismissed, covered or otherwise hidden. Default does nothing
	 *
	 * @param animated Whether or not the transition will be animated
	 */
	public void viewWillDisappear(boolean animated) {
		this.notifyChildrenAppearanceTransitionBegin(false, animated);
	}

	/**
	 * Called after the view was dismissed, covered or otherwise hidden. Default does nothing
	 *
	 * @param animated Whether or not the transition was animated
	 */
	public void viewDidDisappear(boolean animated) {
		this.notifyChildrenAppearanceTransitionEnded();
	}

	/**
	 * Notifies all child view controllers of an appearance transition
	 *
	 * @param isAppearing true if the child view controller’s view is about to be added to the view
	 *                    hierarchy, false if it is being removed.
	 * @param animated Whether or not the transition was animated
	 */
	private void notifyChildrenAppearanceTransitionBegin(boolean isAppearing, boolean animated) {
		if(this.shouldAutomaticallyForwardAppearanceMethods()) {
			for(ViewController viewController : this.childViewControllers) {
				viewController.beginAppearanceTransition(isAppearing, animated);
			}
		}
	}

	/**
	 * Notifies all child view controllers than the previous appearance transition has ended
	 */
	private void notifyChildrenAppearanceTransitionEnded() {
		if(this.shouldAutomaticallyForwardAppearanceMethods()) {
			for(ViewController viewController : this.childViewControllers) {
				viewController.endAppearanceTransition();
			}
		}
	}

	/**
	 * Called just before the view controller's view's layoutSubviews method is invoked. Subclasses
	 * can implement as necessary. The default is a nop.
	 */
	public void viewWillLayoutSubviews() {

	}

	/**
	 * Called just after the view controller's view's layoutSubviews method is invoked. Subclasses
	 * can implement as necessary. The default is a nop.
	 */
	public void viewDidLayoutSubviews() {

	}

	/**
	 * Get the current orientation for the interface
	 * @return interface orientation
	 */
	public InterfaceOrientation getInterfaceOrientation() {
		// TODO: this needs to be smarter, shouldn't just grab the status bar orientation
		return Application.sharedApplication().getStatusBarOrientation();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public NavigationItem getNavigationItem() {
		if(this.navigationItem == null) {
			this.navigationItem = new NavigationItem();
			this.navigationItem.setTitle(this.title);
		}

		return this.navigationItem;
	}

	public void setNavigationItem(NavigationItem navigationItem) {
		this.navigationItem = navigationItem;
	}

	protected List<ViewController> getChildViewControllers() {
		return Collections.unmodifiableList(this.childViewControllers);
	}

	public ViewController getParentViewController() {
		return this.parentViewController;
	}

	/**
	 * The nearest ancestor in the view controller hierarchy that is a navigation controller.
	 *
	 * If the receiver or one of its ancestors is a child of a navigation controller, this property
	 * contains the owning navigation controller. This is null if the view controller is not
	 * embedded inside a navigation controller.
	 *
	 * @return an instance of NavigationController or null
	 */
	public NavigationController getNavigationController() {
		// Check parent first
		if(this.parentViewController instanceof NavigationController) {
			return (NavigationController)this.parentViewController;
		} else {
			// Use the less performant deep find.
			return this.findNearestParentViewController(NavigationController.class);
		}
	}

	/**
	 * The nearest ancestor in the view controller hierarchy that is a tab bar controller.
	 *
	 * If the receiver or one of its ancestors is a child of a tab bar controller, this property
	 * contains the owning tab tab controller. This is null if the view controller is not
	 * embedded inside a tab bar controller.
	 *
	 * @return an instance of TabBarController or null
	 */
	public TabBarController getTabBarController() {
		// Check parent first
		if(this.parentViewController instanceof TabBarController) {
			return (TabBarController)this.parentViewController;
		} else {
			// Use the less performant deep find.
			return this.findNearestParentViewController(TabBarController.class);
		}
	}

	@SuppressWarnings("unchecked")
	<E extends ViewController> E findNearestParentViewController(Class<E> parentViewControllerClass) {
		ViewController viewController = this;
		E nearestViewController = null;

		while (viewController != null) {
			if(parentViewControllerClass.isInstance(viewController)) {
				nearestViewController = (E)viewController;
				break;
			}

			viewController = viewController.getParentViewController();
		}

		return nearestViewController;
	}

	/**
	 * Adds the given view controller as a child.
	 *
	 * If the child controller has a different parent controller, it will first be removed from its
	 * current parent by calling removeFromParentViewController. If this method is overridden then
	 * the super implementation must be called.
	 *
	 * @param childViewController The view controller to be added as a child.
	 */
	public void addChildViewController(ViewController childViewController) {
		if(childViewController.parentViewController != null) {
			childViewController.willMoveToParentViewController(null);
			childViewController.removeFromParentViewController();
		}

		childViewController.willMoveToParentViewController(this);
		childViewController.parentViewController = this;
		this.childViewControllers.add(childViewController);
	}

	/**
	 * Removes the the receiver from its parent's children controllers array. If this method is
	 * overridden then the super implementation must be called.
	 */
	public void removeFromParentViewController() {
		this.parentViewController.childViewControllers.remove(this);
		this.parentViewController = null;
		this.didMoveToParentViewController(null);
	}

	/**
	 * This method can be used to transition between sibling child view controllers. The receiver of
	 * this method is their common parent view controller. (Use ViewController.addChildViewController()
	 * to create the parent/child relationship.) This method will add the toViewController's view to
	 * the superview of the fromViewController's view and the fromViewController's view will be removed
	 * from its superview after the transition completes. It is important to allow this method to add
	 * and remove the views. The arguments to this method are the same as those defined by View's
	 * animation API.
	 *
	 * @param fromViewController A view controller whose view is currently visible in the parent’s view
	 *                           hierarchy.
	 * @param toViewController A child view controller whose view is not currently in the view hierarchy.
	 * @param duration The total duration of the animations, in seconds. If you pass zero, the changes
	 *                 are made without animating them.
	 * @param animations Changes to commit to the views. Here you programmatically change any animatable
	 *                   properties of the views in your view hierarchy. This parameter must not be NULL.
	 * @param completion Callback when the animation completes.
	 *
	 * @see View.animateWithDuration()
	 */
	public void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final long duration, final View.Animations animations, final View.AnimationCompletion completion) {
		View.animateWithDuration(duration, new View.Animations() {
			public void performAnimatedChanges() {
				View.setAnimationsEnabled(false, new Runnable() {
					public void run() {
						fromViewController.getView().getSuperview().addSubview(toViewController.getView());
					}
				});

				fromViewController.beginAppearanceTransition(false, duration > 0);
				toViewController.beginAppearanceTransition(true, duration > 0);

				if(animations != null) {
					animations.performAnimatedChanges();
				}
			}
		}, new View.AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				if(finished) {
					fromViewController.getView().removeFromSuperview();
					fromViewController.endAppearanceTransition();
					toViewController.endAppearanceTransition();
				}

				if(completion != null) {
					completion.animationCompletion(finished);
				}
			}
		});
	}

	/**
	 * Tells a child controller its appearance is about to change.
	 * If you are implementing a custom container controller, use this method to tell the child that its
	 * views are about to appear or disappear. Do not invoke viewWillAppear(), viewWillDisappear(),
	 * viewDidAppear(), or viewDidDisappear() directly.
	 *
	 * @param isAppearing true if the child view controller’s view is about to be added to the view
	 *                    hierarchy, false if it is being removed.
	 * @param animated If true, the transition is being animated.
	 */
	public void beginAppearanceTransition(boolean isAppearing, boolean animated) {
		this.appearanceTransitionIsAppearing = isAppearing ? 1 : -1;
		this.appearanceTransitionAnimated = animated ? 1 : -1;

		if(isAppearing) {
			this.viewWillAppear(animated);
		} else {
			this.viewWillDisappear(animated);
		}
	}

	/**
	 * Tells a child controller its appearance has changed.
	 *
	 * If you are implementing a custom container controller, use this method to tell the child that
	 * the view transition is complete.
	 */
	public void endAppearanceTransition() {
		if(this.appearanceTransitionIsAppearing == 1) {
			this.viewDidAppear(this.appearanceTransitionAnimated == 1);
		} else if(this.appearanceTransitionIsAppearing == -1) {
			this.viewDidDisappear(this.appearanceTransitionAnimated == 1);
		}

		this.appearanceTransitionIsAppearing = 0;
		this.appearanceTransitionAnimated = 0;
	}

	/**
	 * Called just before the view controller is added or removed from a container view controller.
	 *
	 * Your view controller can override this method when it needs to know that it has been added to a
	 * container.
	 *
	 * If you are implementing your own container view controller, it must call the
	 * willMoveToParentViewController() method of the child view controller before calling the
	 * removeFromParentViewController() method, passing in a parent value of null.
	 *
	 * When your custom container calls the addChildViewController() method, it automatically calls the
	 * willMoveToParentViewController() method of the view controller to be added as a child before adding
	 * it.
	 *
	 * @param parentViewController The parent view controller, or null if there is no parent.
	 */
	public void willMoveToParentViewController(ViewController parentViewController) {

	}

	/**
	 * Called after the view controller is added or removed from a container view controller.
	 *
	 * Your view controller can override this method when it wants to react to being added to a container.
	 *
	 * If you are implementing your own container view controller, it must call the
	 * didMoveToParentViewController() method of the child view controller after the transition to the new
	 * controller is complete or, if there is no transition, immediately after calling the
	 * addChildViewController() method.
	 *
	 * The removeFromParentViewController() method automatically calls the didMoveToParentViewController()
	 * method of the child view controller after it removes the child.
	 *
	 * @param parentViewController The parent view controller, or null if there is no parent.
	 */
	public void didMoveToParentViewController(ViewController parentViewController) {

	}

	/**
	 * Returns a Boolean value indicating whether appearance methods are forwarded to child view controllers
	 *
	 * This method is called to determine whether to automatically forward appearance-related containment
	 * callbacks to child view controllers.
	 *
	 * The default implementation returns true. Subclasses of the ViewController class that implement
	 * containment logic may override this method to control how these methods are forwarded. If you override
	 * this method and return false, you are responsible for telling the child when its views are going to
	 * appear or disappear. You do this by calling the child view controller’s beginAppearanceTransition() and
	 * endAppearanceTransition() methods.
	 *
	 * Any child view controller attached to a superview, will be sent appearance methods if this is enabled.
	 * If the child view controller is not attached to a superview, appearance methods will not be delivered.
	 *
	 * @return true if appearance methods are forwarded or false if they are not.
	 */
	public boolean shouldAutomaticallyForwardAppearanceMethods() {
		return true;
	}

	/**
	 * Sent to the view controller when the app receives a memory warning.
	 *
	 * You should never call this method directly. Instead, this method is called when the system determines
	 * that the amount of available memory is low.
	 *
	 * You can override this method to release any additional memory used by your view controller. If you do,
	 * your implementation of this method must call the super implementation at some point.
	 */
	public void didReceiveMemoryWarning() {
		if(this.view != null && this.view.getSuperview() == null) {
			this.setView(null);
		}
	}

	void setNextResponder(Responder nextResponder) {
		this.nextResponder = nextResponder;
	}

	public Responder nextResponder() {
		return this.view != null ? this.view.getSuperview() : this.nextResponder;
	}

}
