package mocha.ui;

import mocha.foundation.NotificationCenter;
import mocha.foundation.Sets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ViewController extends Responder {
	private static Method didReceiveMemoryWarningMethod;

	public enum ModalTransitionStyle {
		MATERIAL,
		HOLO,
		CUSTOM
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
	private TabBarItem tabBarItem;
	private String title;
	private Responder nextResponder;
	private boolean willAppear;
	private boolean didAppear;

	private final LayoutSupport topLayoutGuide;
	private final LayoutSupport bottomLayoutGuide;

	private ModalTransitionStyle modalTransitionStyle;
	private ModalPresentationStyle modalPresentationStyle;
	private Class<? extends ViewPresentationController> customPresentationControllerClass;
	private List<ViewController> presentedViewControllers;
	private ViewController presentingViewController;

	private boolean isBeingPresented;
	private boolean isBeingDismissed;
	private boolean isMovingToParentViewController;
	private boolean isMovingFromParentViewController;
	Window presentingFromWindow;

	private boolean shouldAutorotate;
	private InterfaceOrientation preferredInterfaceOrientationForPresentation;
	private Set<InterfaceOrientation> supportedInterfaceOrientations;
	private InterfaceOrientation restoreToInterfaceOrientationOnReappear;
	private InterfaceOrientation interfaceOrientation;

	public ViewController() {
		this.childViewControllers = new ArrayList<>();
		this.modalPresentationStyle = ModalPresentationStyle.FULL_SCREEN;
		this.modalTransitionStyle = ModalTransitionStyle.MATERIAL;
		this.shouldAutorotate = this.getInitialShouldAutorotate();
		this.preferredInterfaceOrientationForPresentation = this.getInitialPreferredInterfaceOrientationForPresentation();

		this.topLayoutGuide = new LayoutSupport();
		this.bottomLayoutGuide = new LayoutSupport();

		this.supportedInterfaceOrientations = Sets.copy(this.getInitialSupportedInterfaceOrientations());

		if (didReceiveMemoryWarningMethod == null) {
			try {
				didReceiveMemoryWarningMethod = ViewController.class.getMethod("didReceiveMemoryWarning");
			} catch (NoSuchMethodException e) {
				MWarn(e, "Could not find memory warning method.");
			}
		}

		NotificationCenter.defaultCenter().addObserver(this, didReceiveMemoryWarningMethod, Application.DID_RECEIVE_MEMORY_WARNING_NOTIFICATION, null);
	}

	protected final android.content.Context getContext() {
		return Application.sharedApplication().getContext();
	}

	protected void loadView() {
		View view = new View(Screen.mainScreen().getBounds());
		view.setBackgroundColor(Color.WHITE);
		this.setView(view);
	}

	public View getView() {
		if (this.view == null) {
			this.loadView();

			if (this.view == null) {
				throw new RuntimeException(this.getClass().getCanonicalName() + " did not set a view in loadView().");
			} else {
				this.viewDidLoad();
			}
		}

		return this.view;
	}

	public void setView(View view) {
		boolean hadView = this.view != null;

		if (hadView) {
			this.view._setViewController(null);

			if (view == null) {
				this.viewWillUnload();
			}
		}

		this.view = view;

		if (this.view != null) {
			this.view._setViewController(this);
		} else if (hadView) {
			this.viewDidUnload();
		}
	}

	protected void viewDidLoad() {

	}

	public boolean isViewLoaded() {
		return this.view != null;
	}

	protected void viewWillUnload() {

	}

	protected void viewDidUnload() {

	}

	public void viewWillAppear(boolean animated) {
		this.willAppear = true;
		this.notifyChildrenAppearanceTransitionBegin(true, animated);
		this.getView().setNeedsLayout();
	}

	public void viewDidAppear(boolean animated) {
		this.willAppear = false;
		this.didAppear = true;
		this.notifyChildrenAppearanceTransitionEnded();
	}

	public void viewWillDisappear(boolean animated) {
		this.didAppear = false;
		this.willAppear = false;
		this.notifyChildrenAppearanceTransitionBegin(false, animated);
	}

	public void viewDidDisappear(boolean animated) {
		this.didAppear = false;
		this.willAppear = false;
		this.notifyChildrenAppearanceTransitionEnded();
	}

	private void notifyChildrenAppearanceTransitionBegin(boolean isAppearing, boolean animated) {
		if (this.shouldAutomaticallyForwardAppearanceMethods()) {
			for (ViewController viewController : this.childViewControllers) {
				viewController.beginAppearanceTransition(isAppearing, animated);
			}
		}
	}

	private void notifyChildrenAppearanceTransitionEnded() {
		if (this.shouldAutomaticallyForwardAppearanceMethods()) {
			for (ViewController viewController : this.childViewControllers) {
				viewController.endAppearanceTransition();
			}
		}
	}

	public void viewWillLayoutSubviews() {

	}

	public void viewDidLayoutSubviews() {

	}

	public LayoutSupport getTopLayoutGuide() {
		return this.topLayoutGuide;
	}

	public LayoutSupport getBottomLayoutGuide() {
		return this.bottomLayoutGuide;
	}

	public InterfaceOrientation getInterfaceOrientation() {
		if (this.interfaceOrientation != null) {
			return this.interfaceOrientation;
		} else {
			return Application.sharedApplication().getStatusBarOrientation();
		}
	}

	void setInterfaceOrientation(InterfaceOrientation interfaceOrientation) {
		this.interfaceOrientation = interfaceOrientation;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;

		if (this.navigationItem != null) {
			this.navigationItem.setTitle(title);
		}
	}

	public NavigationItem getNavigationItem() {
		if (this.navigationItem == null) {
			this.navigationItem = new NavigationItem();
			this.navigationItem.setTitle(this.title);
		}

		return this.navigationItem;
	}

	public void setNavigationItem(NavigationItem navigationItem) {
		if (this.navigationItem != navigationItem) {
			NavigationController navigationController = this.getNavigationController();

			if (navigationController != null && this.navigationItem != null) {
				navigationController.getNavigationBar().replaceNavigationItem(this.navigationItem, navigationItem);
			}

			this.navigationItem = navigationItem;
		}
	}

	public TabBarItem getTabBarItem() {
		if (this.tabBarItem == null) {
			this.tabBarItem = new TabBarItem(this.title, null, -1);
		}

		return tabBarItem;
	}

	public void setTabBarItem(TabBarItem tabBarItem) {
		// TODO: Build out logic to update TabBarController once that's built out.
		this.tabBarItem = tabBarItem;
	}

	protected List<ViewController> getChildViewControllers() {
		return Collections.unmodifiableList(this.childViewControllers);
	}

	public ViewController getParentViewController() {
		return this.parentViewController;
	}

	public NavigationController getNavigationController() {
		// Check parent first
		if (this.parentViewController instanceof NavigationController) {
			return (NavigationController) this.parentViewController;
		} else {
			// Use the less performant deep find.
			return this.findNearestParentViewController(NavigationController.class);
		}
	}

	public TabBarController getTabBarController() {
		// Check parent first
		if (this.parentViewController instanceof TabBarController) {
			return (TabBarController) this.parentViewController;
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
			if (parentViewControllerClass.isInstance(viewController)) {
				nearestViewController = (E) viewController;
				break;
			}

			viewController = viewController.getParentViewController();
		}

		return nearestViewController;
	}

	protected void willRotateToInterfaceOrientation(InterfaceOrientation toInterfaceOrientation) {
		if (this.shouldAutomaticallyForwardRotationMethods() && this.childViewControllers != null && this.childViewControllers.size() > 0) {
			for (ViewController viewController : this.childViewControllers) {
				if (viewController.isViewLoaded() && viewController.getView().getSuperview() != null) {
					viewController.willRotateToInterfaceOrientation(toInterfaceOrientation);
				}
			}
		}
	}

	protected void didRotateFromInterfaceOrientation(InterfaceOrientation fromInterfaceOrientation) {
		if (this.shouldAutomaticallyForwardRotationMethods() && this.childViewControllers != null && this.childViewControllers.size() > 0) {
			for (ViewController viewController : this.childViewControllers) {
				if (viewController.isViewLoaded() && viewController.getView().getSuperview() != null) {
					viewController.didRotateFromInterfaceOrientation(fromInterfaceOrientation);
				}
			}
		}
	}

	public final boolean getShouldAutorotate() {
		return this.shouldAutorotate;
	}

	public boolean getInitialShouldAutorotate() {
		return true;
	}

	public void setShouldAutorotate(boolean shouldAutorotate) {
		this.shouldAutorotate = shouldAutorotate;
		this.orientationConfigChanged();
	}

	public final InterfaceOrientation getPreferredInterfaceOrientationForPresentation() {
		return this.preferredInterfaceOrientationForPresentation;
	}

	public InterfaceOrientation getInitialPreferredInterfaceOrientationForPresentation() {
		return this.preferredInterfaceOrientationForPresentation;
	}


	public void setPreferredInterfaceOrientationForPresentation(InterfaceOrientation preferredInterfaceOrientationForPresentation) {
		this.preferredInterfaceOrientationForPresentation = preferredInterfaceOrientationForPresentation;
	}

	public final Set<InterfaceOrientation> getSupportedInterfaceOrientations() {
		return EnumSet.copyOf(this.supportedInterfaceOrientations);
	}

	public Set<InterfaceOrientation> getInitialSupportedInterfaceOrientations() {
		if (Device.get().getUserInterfaceIdiom() == Device.UserInterfaceIdiom.PHONE) {
			return InterfaceOrientation.SET_ALL_BUT_UPSIDE_DOWN;
		} else {
			return InterfaceOrientation.SET_ALL;
		}
	}

	public void setSupportedInterfaceOrientations(InterfaceOrientation... supportedInterfaceOrientations) {
		this.setSupportedInterfaceOrientations(InterfaceOrientation.toSet(supportedInterfaceOrientations));
	}

	public void setSupportedInterfaceOrientations(Set<InterfaceOrientation> supportedInterfaceOrientations) {
		this.supportedInterfaceOrientations = EnumSet.copyOf(supportedInterfaceOrientations);
		this.orientationConfigChanged();
	}

	private void orientationConfigChanged() {
		if (this.isViewLoaded()) {
			Window window = this.view.getWindow();

			if (window != null) {
				window.viewControllerOrientationConfigChanged(this);
			}
		}
	}

	InterfaceOrientation getRestoreToInterfaceOrientationOnReappear() {
		return restoreToInterfaceOrientationOnReappear;
	}

	void setRestoreToInterfaceOrientationOnReappear(InterfaceOrientation restoreToInterfaceOrientationOnReappear) {
		this.restoreToInterfaceOrientationOnReappear = restoreToInterfaceOrientationOnReappear;
	}

	public void addChildViewController(ViewController childViewController) {
		if (childViewController.parentViewController != null) {
			childViewController.willMoveToParentViewController(null);
			childViewController.removeFromParentViewController();
		}

		childViewController.willMoveToParentViewController(this);
		childViewController.parentViewController = this;
		this.childViewControllers.add(childViewController);
	}

	public void removeFromParentViewController() {
		this.parentViewController.childViewControllers.remove(this);
		this.parentViewController = null;
		this.didMoveToParentViewController(null);
	}

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

	public void beginAppearanceTransition(boolean isAppearing, boolean animated) {
		if (isAppearing && this.parentViewController != null && !this.parentViewController.willAppear && !this.parentViewController.didAppear)
			return;

		this.appearanceTransitionIsAppearing = isAppearing ? 1 : -1;
		this.appearanceTransitionAnimated = animated ? 1 : -1;

		if (!this.isViewLoaded()) {
			this.getView();
		}

		if (isAppearing) {
			this.viewWillAppear(animated);
		} else {
			this.viewWillDisappear(animated);
		}
	}

	public void endAppearanceTransition() {
		if (this.appearanceTransitionIsAppearing == 1 && this.parentViewController != null && !this.parentViewController.willAppear && !this.parentViewController.didAppear)
			return;

		if (this.appearanceTransitionIsAppearing == 1) {
			this.viewDidAppear(this.appearanceTransitionAnimated == 1);
		} else if (this.appearanceTransitionIsAppearing == -1) {
			this.viewDidDisappear(this.appearanceTransitionAnimated == 1);
		}

		this.appearanceTransitionIsAppearing = 0;
		this.appearanceTransitionAnimated = 0;
	}

	public void willMoveToParentViewController(ViewController parentViewController) {
		if (parentViewController == null) {
			this.isMovingFromParentViewController = true;
		} else {
			this.isMovingToParentViewController = true;
		}
	}

	public void didMoveToParentViewController(ViewController parentViewController) {
		if (parentViewController == null) {
			this.isMovingFromParentViewController = false;
		} else {
			this.isMovingToParentViewController = false;
		}
	}

	public boolean shouldAutomaticallyForwardAppearanceMethods() {
		return true;
	}

	public boolean shouldAutomaticallyForwardRotationMethods() {
		return true;
	}

	public void presentViewController(ViewController viewController, boolean animated) {
		this.presentViewController(viewController, animated, null);
	}

	public void presentViewController(ViewController viewController, boolean animated, Runnable completion) {
		this.getRootParentViewController()._presentViewController(viewController, animated, completion);
	}

	private void _presentViewController(final ViewController viewController, boolean animated, final Runnable completion) {
		// TODO: Handle stuff like orientation changes, transition styles and presentation styles

		final Window window = this.getWindow();

		if (window == null) {
			throw new RuntimeException("You can not present a view controller from a view controller that isn't attached to a window.");
		}

		if (this.presentingFromWindow != window) {
			this.presentingFromWindow = window;
		}

		int stackSize = this.presentedViewControllers == null ? 0 : this.presentedViewControllers.size();
		ViewController hideViewController = stackSize == 0 ? this : this.presentedViewControllers.get(stackSize - 1);

		this.addPresentedViewController(viewController, window);
		this.getPresentationController(viewController.getModalTransitionStyle(), viewController.getCustomPresentationControllerClass()).presentViewController(viewController, hideViewController, animated, window, new Runnable() {
			public void run() {
				if (completion != null) {
					completion.run();
				}

				window.viewControllerOrientationConfigChanged(viewController);
				promoteDeepestDefaultFirstResponder();
			}
		});
	}

	public void dismissViewController(boolean animated) {
		this.dismissViewController(animated, null);
	}

	public void dismissViewController(boolean animated, Runnable completion) {
		ViewController root = this.getRootParentViewController();

		if (root.presentedViewControllers != null && root.presentedViewControllers.size() > 0) {
			root.dismissPresentedViewController(root.presentedViewControllers.get(root.presentedViewControllers.size() - 1), animated, completion);
		} else if (root.presentingViewController != null) {
			root.presentingViewController.dismissPresentedViewController(root, animated, completion);
		} else {
			MWarn("There is no presented view controller to dismiss.");
		}
	}

	private void dismissPresentedViewController(ViewController viewController, boolean animated, final Runnable completion) {
		int index;
		int count;

		if (this.presentedViewControllers == null || (count = this.presentedViewControllers.size()) <= 0 || (index = this.presentedViewControllers.indexOf(viewController)) == -1) {
			throw new RuntimeException(String.format("%s is not presenting %s, so it can not dismiss it.", this, viewController));
		}

		final Window window = this.getWindow(); // We should already be a root parent;

		if (window == null) {
			throw new RuntimeException("Trying to dimiss presented view controller from a presenter without a window.");
		}

		final List<ViewController> dismissViewControllers = new ArrayList<ViewController>(this.presentedViewControllers.subList(index, count));
		final ViewController hideViewController = dismissViewControllers.get(dismissViewControllers.size() - 1);
		final ViewController revealViewController;

		if (index == 0) {
			revealViewController = this;
		} else {
			revealViewController = this.presentedViewControllers.get(index - 1);
		}

		this.getPresentationController(hideViewController.getModalTransitionStyle(), hideViewController.getCustomPresentationControllerClass()).dismissPresentedViewController(hideViewController, revealViewController, dismissViewControllers, animated, window, new Runnable() {
			public void run() {
				window.viewControllerOrientationConfigChanged(revealViewController);
			}
		});
	}

	private ViewPresentationController getPresentationController(ModalTransitionStyle transitionStyle, Class<? extends ViewPresentationController> customPresentationControllerClass) {
		if (transitionStyle == ModalTransitionStyle.CUSTOM && customPresentationControllerClass != null) {
			try {
				return customPresentationControllerClass.getConstructor(ViewController.class).newInstance(this);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		} else if (transitionStyle == ModalTransitionStyle.HOLO) {
			return new ViewPresentationControllerHolo(this);
		} else {
			return new ViewPresentationControllerMaterial(this);
		}
	}

	public Class<? extends ViewPresentationController> getCustomPresentationControllerClass() {
		return this.customPresentationControllerClass;
	}

	public void setCustomPresentationControllerClass(Class<? extends ViewPresentationController> customPresentationControllerClass) {
		this.customPresentationControllerClass = customPresentationControllerClass;
	}

	private ViewController getRootParentViewController() {
		if (this.parentViewController == null) {
			return this;
		} else {
			return this.parentViewController.getRootParentViewController();
		}
	}

	private Window getWindow() {
		ViewController rootParentViewController = this.getRootParentViewController();
		if (rootParentViewController.presentingFromWindow != null) {
			return rootParentViewController.presentingFromWindow;
		} else if (rootParentViewController.isViewLoaded()) {
			return rootParentViewController.getView().getWindow();
		} else {
			return null;
		}
	}

	private void addPresentedViewController(ViewController presentedViewController, Window window) {
		if (this.presentedViewControllers == null) {
			this.presentedViewControllers = new ArrayList<ViewController>();
		}

		this.presentedViewControllers.add(presentedViewController);
		presentedViewController.presentingViewController = this;
		window.addVisibleViewController(presentedViewController);
	}

	void removePresentedViewController(ViewController presentedViewController, Window window) {
		if (this.presentedViewControllers == null) return;

		this.presentedViewControllers.remove(presentedViewController);
		presentedViewController.presentingViewController = null;
		window.removeVisibleViewController(presentedViewController);
		this.promoteDeepestDefaultFirstResponder();
	}


	public ViewController getPresentedViewController() {
		ViewController viewController = this;

		while (viewController != null) {
			if (viewController.presentedViewControllers != null && viewController.presentedViewControllers.size() > 0) {
				return viewController.presentedViewControllers.get(viewController.presentedViewControllers.size() - 1);
			} else {
				viewController = viewController.parentViewController;
			}
		}

		return null;
	}

	public ViewController getPresentingViewController() {
		if (this.presentingViewController != null) {
			return this.presentingViewController;
		} else {
			ViewController viewController = this;

			while (viewController.parentViewController != null) {
				viewController = viewController.parentViewController;
			}

			return viewController.presentingViewController;
		}
	}

	void setBeingPresented(boolean beingPresented) {
		this.isBeingPresented = beingPresented;
	}

	public boolean isBeingPresented() {
		return this.isBeingPresented;
	}

	void setBeingDismissed(boolean beingDismissed) {
		this.isBeingDismissed = beingDismissed;
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

	public void didReceiveMemoryWarning() {
		if (this.view != null && this.view.getSuperview() == null) {
			this.setView(null);
		}
	}

	void setNextResponder(Responder nextResponder) {
		this.nextResponder = nextResponder;
	}

	public Responder nextResponder() {
		return this.view != null ? this.view.getSuperview() : this.nextResponder;
	}

	public boolean canBecomeFirstResponder() {
		return this.isInCompleteResponderChain() && this.isViewLoaded();
	}

	boolean canBecomeDefaultFirstResponder() {
		return true;
	}

	Responder getDefaultFirstResponder() {
		return this;
	}

	public void backKeyPressed(Event event) {
		if (this.parentViewController == null && this.presentingViewController != null) {
			this.dismissViewController(true);
		} else if (this.parentViewController != null) {
			this.parentViewController.backKeyPressed(event);
		} else {
			Application.sharedApplication().getActivity().backKeyPressed(event);
		}
	}

	void dispatchBackKeyPressed(Event event) {
		ViewController viewController = this;

		while (viewController.getChildViewControllerForBackKeyPressed() != null) {
			viewController = viewController.getChildViewControllerForBackKeyPressed();
		}

		viewController.backKeyPressed(event);
	}

	public ViewController getChildViewControllerForBackKeyPressed() {
		return null;
	}

}