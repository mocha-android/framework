/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.graphics.Bitmap;
import mocha.animation.TimingFunction;
import mocha.foundation.NotificationCenter;
import mocha.graphics.AffineTransform;
import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Rect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewController extends Responder {
	private static Method didReceiveMemoryWarningMethod;

	public enum ModalTransitionStyle {
		COVER_VERTICAL,
		ANDROID,
		FLIP_HORIZONTAL,
		CROSS_DISOLVE,
		PARTIAL_CURL
	}

	public enum ModalPresentationStyle {
		FULL_SCREEN,
		PAGE_SHEET,
		FORM_SHEET,
		CURRENT_CONTEXT
	}

	private View view;
	private ViewController parentViewController;
	private List<ViewController> childViewControllers;
	private int appearanceTransitionIsAppearing;
	private int appearanceTransitionAnimated;
	private NavigationItem navigationItem;
	private String title;
	private Responder nextResponder;
	private boolean willAppear;
	private boolean didAppear;

	private ModalTransitionStyle modalTransitionStyle;
	private ModalPresentationStyle modalPresentationStyle;
	private List<ViewController> presentedViewControllers;
	private ViewController presentingViewController;

	private boolean isBeingPresented;
	private boolean isBeingDismissed;
	private boolean isMovingToParentViewController;
	private boolean isMovingFromParentViewController;
	private Window presentingFromWindow;

	public ViewController() {
		this.childViewControllers = new ArrayList<ViewController>();
		this.modalPresentationStyle = ModalPresentationStyle.FULL_SCREEN;
		this.modalTransitionStyle = ModalTransitionStyle.COVER_VERTICAL;

		if(didReceiveMemoryWarningMethod == null) {
			try {
				didReceiveMemoryWarningMethod = ViewController.class.getMethod("didReceiveMemoryWarning");
			} catch (NoSuchMethodException e) {
				MWarn(e, "Could not find memory warning method.");
			}
		}

		NotificationCenter.defaultCenter().addObserver(this, didReceiveMemoryWarningMethod, Application.DID_RECEIVE_MEMORY_WARNING_NOTIFICATION, null);
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
		this.willAppear = true;
		this.notifyChildrenAppearanceTransitionBegin(true, animated);
	}

	/**
	 * Called when the view has been fully transitioned onto the screen. Default does nothing
	 *
	 * @param animated Whether or not the transition was animated
	 */
	public void viewDidAppear(boolean animated) {
		this.willAppear = false;
		this.didAppear = true;
		this.notifyChildrenAppearanceTransitionEnded();
	}

	/**
	 * Called when the view is dismissed, covered or otherwise hidden. Default does nothing
	 *
	 * @param animated Whether or not the transition will be animated
	 */
	public void viewWillDisappear(boolean animated) {
		this.didAppear = false;
		this.willAppear = false;
		this.notifyChildrenAppearanceTransitionBegin(false, animated);
	}

	/**
	 * Called after the view was dismissed, covered or otherwise hidden. Default does nothing
	 *
	 * @param animated Whether or not the transition was animated
	 */
	public void viewDidDisappear(boolean animated) {
		this.didAppear = false;
		this.willAppear = false;
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

		if(this.navigationItem != null) {
			this.navigationItem.setTitle(title);
		}
	}

	public NavigationItem getNavigationItem() {
		if(this.navigationItem == null) {
			this.navigationItem = new NavigationItem();
			this.navigationItem.setTitle(this.title);
		}

		return this.navigationItem;
	}

	public void setNavigationItem(NavigationItem navigationItem) {
		NavigationController navigationController = this.getNavigationController();

		if(navigationController != null && this.navigationItem != null) {
			navigationController.getNavigationBar().replaceNavigationItem(this.navigationItem, navigationItem);
		}

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

	// Notifies when rotation begins, reaches halfway point and ends.
	protected void willRotateToInterfaceOrientation(InterfaceOrientation toInterfaceOrientation, long duration) {

	}

	protected void didRotateFromInterfaceOrientation(InterfaceOrientation fromInterfaceOrientation) {

	}

	// Called from within a rotating animation block, for additional animations during rotation.
	// A subclass may override this method, or the two-part variants below, but not both.
	protected void willAnimateRotationToInterfaceOrientation(InterfaceOrientation toInterfaceOrientation, long duration) {

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
	 * @see View#animateWithDuration(long, mocha.ui.View.Animations, mocha.ui.View.AnimationCompletion)
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

						if (animations != null) {
							animations.performAnimatedChanges();
						}
					}
				}, new View.AnimationCompletion() {
					public void animationCompletion(boolean finished) {
						if (finished) {
							fromViewController.getView().removeFromSuperview();
							fromViewController.endAppearanceTransition();
							toViewController.endAppearanceTransition();
						}

						if (completion != null) {
							completion.animationCompletion(finished);
						}
					}
				}
		);
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
		if(isAppearing && this.parentViewController != null && !this.parentViewController.willAppear && !this.parentViewController.didAppear) return;

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
		if(this.appearanceTransitionIsAppearing == 1 && this.parentViewController != null && !this.parentViewController.willAppear && !this.parentViewController.didAppear) return;

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
		if(parentViewController == null) {
			this.isMovingFromParentViewController = true;
		} else {
			this.isMovingToParentViewController = true;
		}
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
		if(parentViewController == null) {
			this.isMovingFromParentViewController = false;
		} else {
			this.isMovingToParentViewController = false;
		}
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

	public void presentViewController(ViewController viewController, boolean animated) {
		this.presentViewController(viewController, animated, null);
	}

	public void presentViewController(ViewController viewController, boolean animated, Runnable completion) {
		this.getRootParentViewController()._presentViewController(viewController, animated, completion);
	}

	private void _presentViewController(ViewController viewController, boolean animated, Runnable completion) {
		// TODO: Handle stuff like orientation changes, transition styles and presentation styles

		final Window window = this.getWindow();

		if(window == null) {
			throw new RuntimeException("You can not present a view controller from a view controller that isn't attached to a window.");
		}

		if(this.presentingFromWindow != window) {
			this.presentingFromWindow = window;
		}

		int stackSize = this.presentedViewControllers == null ? 0 : this.presentedViewControllers.size();
		ViewController hideViewController = stackSize == 0 ? this : this.presentedViewControllers.get(stackSize - 1);

		this.addPresentedViewController(viewController, window);
		this.getPresentationController(viewController.getModalTransitionStyle()).presentViewController(viewController, hideViewController, animated, window, completion);
	}

	public void dismissViewController(boolean animated) {
		this.dismissViewController(animated, null);
	}

	public void dismissViewController(boolean animated, Runnable completion) {
		ViewController root = this.getRootParentViewController();

		if(root.presentedViewControllers != null && root.presentedViewControllers.size() > 0) {
			root.dismissPresentedViewController(root.presentedViewControllers.get(root.presentedViewControllers.size() - 1), animated, completion);
		} else if(root.presentingViewController != null) {
			root.presentingViewController.dismissPresentedViewController(root, animated, completion);
		} else {
			MWarn("There is no presented view controller to dismiss.");
		}
	}

	private void dismissPresentedViewController(ViewController viewController, boolean animated, final Runnable completion) {
		int index;
		int count;

		if(this.presentedViewControllers == null || (count = this.presentedViewControllers.size()) <= 0 || (index = this.presentedViewControllers.indexOf(viewController)) == -1) {
			throw new RuntimeException(String.format("%s is not presenting %s, so it can not dismiss it.", this, viewController));
		}

		final Window window = this.getWindow(); // We should already be a root parent;

		if(window == null) {
			throw new RuntimeException("Trying to dimiss presented view controller from a presenter without a window.");
		}

		final List<ViewController> dismissViewControllers = new ArrayList<ViewController>(this.presentedViewControllers.subList(index, count));
		final ViewController hideViewController = dismissViewControllers.get(dismissViewControllers.size() - 1);
		final ViewController revealViewController;

		if(index == 0) {
			revealViewController = this;
		} else {
			revealViewController = this.presentedViewControllers.get(index - 1);
		}

		this.getPresentationController(hideViewController.getModalTransitionStyle()).dismissPresentedViewController(hideViewController, revealViewController, dismissViewControllers, animated, window, null);
	}

	private PresentationController getPresentationController(ModalTransitionStyle transitionStyle) {
		if(transitionStyle == ModalTransitionStyle.ANDROID) {
			return new PresentationControllerAndroid();
		} else {
			return new PresentationControllerCoverVertical();
		}
	}

	private ViewController getRootParentViewController() {
		if(this.parentViewController == null) {
			return this;
		} else {
			return this.parentViewController.getRootParentViewController();
		}
	}

	private Window getWindow() {
		ViewController rootParentViewController = this.getRootParentViewController();
		if(rootParentViewController.presentingFromWindow != null) {
			return rootParentViewController.presentingFromWindow;
		} else if(rootParentViewController.isViewLoaded()) {
			return rootParentViewController.getView().getWindow();
		} else {
			return null;
		}
	}

	private void addPresentedViewController(ViewController presentedViewController, Window window) {
		if(this.presentedViewControllers == null) {
			this.presentedViewControllers = new ArrayList<ViewController>();
		}

		this.presentedViewControllers.add(presentedViewController);
		presentedViewController.presentingViewController = this;
		window.addVisibleViewController(presentedViewController);
	}

	private void removePresentedViewController(ViewController presentedViewController, Window window) {
		if(this.presentedViewControllers == null) return;

		this.presentedViewControllers.remove(presentedViewController);
		presentedViewController.presentingViewController = null;
		window.removeVisibleViewController(presentedViewController);
	}


	public ViewController getPresentedViewController() {
		ViewController viewController = this;

		while(viewController != null) {
			if(viewController.presentedViewControllers != null && viewController.presentedViewControllers.size() > 0) {
				return viewController.presentedViewControllers.get(viewController.presentedViewControllers.size() - 1);
			} else {
				viewController = viewController.parentViewController;
			}
		}

		return null;
	}

	public ViewController getPresentingViewController() {
		if(this.presentingViewController != null) {
			return this.presentingViewController;
		} else {
			ViewController viewController = this;

			while(viewController.parentViewController != null) {
				viewController = viewController.parentViewController;
			}

			return viewController.presentingViewController;
		}
	}

	public boolean isBeingPresented() {
		return this.isBeingPresented;
	}

	public boolean isBeingDismissed() {
		return this.isBeingDismissed;
	}

	public boolean isMovingToParentViewController() {
		return this.isMovingToParentViewController;
	}

	public boolean isMovingFromParentViewController() {
		return this.isMovingFromParentViewController;
	}

	public ModalTransitionStyle getModalTransitionStyle() {
		return modalTransitionStyle;
	}

	public void setModalTransitionStyle(ModalTransitionStyle modalTransitionStyle) {
		this.modalTransitionStyle = modalTransitionStyle;
	}

	public ModalPresentationStyle getModalPresentationStyle() {
		return modalPresentationStyle;
	}

	public void setModalPresentationStyle(ModalPresentationStyle modalPresentationStyle) {
		this.modalPresentationStyle = modalPresentationStyle;
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

	private abstract class PresentationController {

		abstract void presentViewController(final ViewController viewController, final ViewController hideViewController, final boolean animated, final Window window, final Runnable completion);

		protected void presentViewControllerFinish(ViewController presentedViewController, ViewController hideViewController, Runnable completion) {
			presentedViewController.endAppearanceTransition();
			hideViewController.endAppearanceTransition();
			hideViewController.getView().removeFromSuperview();

			presentedViewController.isBeingPresented = false;

			if(completion != null) {
				completion.run();
			}
		}

		abstract void dismissPresentedViewController(ViewController hideViewController, ViewController revealViewController, List<ViewController> dismissViewControllers, boolean animated, Window window, final Runnable completion);

		protected void dismissPresentedViewControllerFinish(ViewController hideViewController, ViewController revealViewController, List<ViewController> dismissViewControllers, Window window, Runnable completion) {
			hideViewController.getView().removeFromSuperview();

			hideViewController.endAppearanceTransition();
			revealViewController.endAppearanceTransition();

			for(ViewController dismissViewController : dismissViewControllers) {
				removePresentedViewController(dismissViewController, window);
			}

			if(completion != null) {
				completion.run();
			}

			hideViewController.isBeingDismissed = false;

			if(revealViewController == ViewController.this) {
				presentingFromWindow = null;
			}
		}
	}

	private class PresentationControllerCoverVertical extends PresentationController {

		void presentViewController(final ViewController viewController, final ViewController hideViewController, final boolean animated, final Window window, final Runnable completion) {
			final Rect bounds = window.getBounds();

			if(animated) {
				Rect startFrame = bounds.copy();
				startFrame.offset(0.0f, bounds.size.height);
				viewController.getView().setFrame(startFrame);
			} else {
				viewController.getView().setFrame(bounds);
			}

			viewController.isBeingPresented = true;
			viewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);
			window.addSubview(viewController.getView());


			if(animated) {
				final boolean restore = window.isUserInteractionEnabled();
				window.setUserInteractionEnabled(false);

				View.animateWithDuration(330, new View.Animations() {
							public void performAnimatedChanges() {
								viewController.getView().setFrame(bounds);
							}
						}, new View.AnimationCompletion() {
							public void animationCompletion(boolean finished) {
								presentViewControllerFinish(viewController, hideViewController, completion);
								window.setUserInteractionEnabled(restore);
							}
						});
			} else {
				this.presentViewControllerFinish(viewController, hideViewController, completion);
			}

		}

		void dismissPresentedViewController(final ViewController hideViewController, final ViewController revealViewController, final List<ViewController> dismissViewControllers, boolean animated, final Window window, final Runnable completion) {
			final Rect bounds = window.getBounds();

			revealViewController.getView().setFrame(bounds);
			window.insertSubviewBelowSubview(revealViewController.getView(), hideViewController.getView());

			hideViewController.isBeingDismissed = true;

			revealViewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);

			if(animated) {
				final boolean restore = window.isUserInteractionEnabled();
				window.setUserInteractionEnabled(false);

				View.animateWithDuration(330, new View.Animations() {
							public void performAnimatedChanges() {
								Rect endFrame = bounds.copy();
								endFrame.offset(0.0f, bounds.size.height);
								hideViewController.getView().setFrame(endFrame);
							}
						}, new View.AnimationCompletion() {
							public void animationCompletion(boolean finished) {
								dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
								window.setUserInteractionEnabled(restore);
							}
						});
			} else {
				this.dismissPresentedViewControllerFinish(hideViewController, revealViewController, dismissViewControllers, window, completion);
			}
		}
	}

	private class PresentationControllerAndroid extends PresentationController {

		void presentViewController(final ViewController viewController, final ViewController hideViewController, boolean animated, Window window, final Runnable completion) {
			final Rect bounds = window.getBounds();

			if(animated) {
				Rect startFrame = bounds.copy();
				startFrame.offset(0.0f, bounds.size.height);
				viewController.getView().setFrame(startFrame);
			} else {
				viewController.getView().setFrame(bounds);
			}

			viewController.isBeingPresented = true;
			viewController.beginAppearanceTransition(true, animated);
			hideViewController.beginAppearanceTransition(false, animated);


			if(animated) {
				this.transitionViewController(hideViewController, viewController, true, window, new Runnable() {
					public void run() {
						presentViewControllerFinish(viewController, hideViewController, completion);

						if(completion != null) {
							completion.run();
						}
					}
				});
			} else {
				window.addSubview(viewController.getView());
				this.presentViewControllerFinish(viewController, hideViewController, completion);
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

			fromViewController.beginAppearanceTransition(false, presenting);
			toViewController.beginAppearanceTransition(true, !presenting);

			long start = android.os.SystemClock.uptimeMillis();

			// Cache from view to image
			final ImageView transitionView = new ImageView(bounds);
			{
				Context context = new Context(bounds.size, view.scale, Bitmap.Config.ARGB_8888);
				if(presenting) {
					toView.getLayer().renderInContext(context);
				} else {
					fromView.getLayer().renderInContext(context);
				}

				transitionView.setImage(context.getImage());
				view.addSubview(transitionView);
			}

			MWarn("Took %dms to build UI cache", android.os.SystemClock.uptimeMillis() - start);

			// Animate
			final AffineTransform scaled = AffineTransform.scaled(0.8f, 0.8f);

			if(presenting) {
				transitionView.setAlpha(0.0f);
				transitionView.setTransform(scaled);
			} else {
				fromView.removeFromSuperview();
				window.addSubview(toView);
				view.bringSubviewToFront(transitionView);
			}

			View.animateWithDuration(300, 1, new View.Animations() {
						public void performAnimatedChanges() {
					View.setTimingFunction(new TimingFunction.CubicBezierCurveTimingFunction(0.215f, 0.610f, 0.355f, 1.000f));

					if(presenting) {
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
					image.recycle();

					if(presenting) {
						window.addSubview(toView);
						fromView.removeFromSuperview();
					} else {
						window.addSubview(toViewController.getView());
					}

					transitionView.removeFromSuperview();

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

			hideViewController.isBeingDismissed = true;

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

}