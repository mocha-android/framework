/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.graphics.Bitmap;
import mocha.animation.TimingFunction;
import mocha.graphics.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigationController extends ViewController {
	public enum TransitionStyle {
		ANDROID, IOS
	}

	public static final long HIDE_SHOW_BAR_DURATION = 330;

	private NavigationBar navigationBar;
	private View containerView;
	private List<ViewController> viewControllers;
	private boolean navigationBarHidden;
	private NavigationBar.Delegate navigationBarDelegate;
	private boolean transition;
	private boolean showHideNavigationBarDuringTransition;
	private TransitionStyle transitionStyle;
	private TransitionController transitionController;

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
				this.navigationBar = navigationBarClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			this.navigationBar = new NavigationBar();
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

		this.transitionStyle = TransitionStyle.IOS;

		if(rootViewController != null) {
			this.addChildViewController(rootViewController);
			this.viewControllers.add(rootViewController);
			rootViewController.didMoveToParentViewController(this);
		}
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

	protected void loadView() {
		super.loadView();

		this.transitionController = this.transitionStyle == TransitionStyle.ANDROID ? new TransitionControllerAndroid() : new TransitionControlleriOS();

		View view = this.getView();
		view.setClipsToBounds(true);

		Rect bounds = view.getBounds();

		float navBarHeight = this.navigationBar.sizeThatFits(bounds.size).height;

		if(this.navigationBarHidden) {
			this.navigationBar.setFrame(new Rect(0.0f, -navBarHeight, bounds.size.width, navBarHeight));
			navBarHeight = 0.0f;
		} else {
			this.navigationBar.setFrame(new Rect(0.0f, 0.0f, bounds.size.width, navBarHeight));
		}

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

				this.navigationBar.setDelegate(this.navigationBarDelegate);

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
				this.transitionController.transitionFromViewController(fromViewController, toViewController, push, completion);
			}
		}
	}

	public void backKeyPressed(Event event) {
		if(this.viewControllers.size() > 1) {
			this.popViewControllerAnimated(true);
		} else {
			super.backKeyPressed(event);
		}
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

		final Rect containerFrame = bounds.copy();
		containerFrame.origin.y += navBarHeight;
		containerFrame.size.height -= navBarHeight;

		if(!animated) {
			this.navigationBar.setFrame(navigationFrame);
			this.navigationBar.setHidden(this.navigationBarHidden);
			this.containerView.setFrame(containerFrame);

			if(animations != null) {
				animations.performAnimatedChanges();
			}

			if(completion != null) {
				completion.animationCompletion(true);
			}
		} else {
			this.navigationBar.setHidden(false);

			if(this.transition) {
				this.showHideNavigationBarDuringTransition = true;
				return;
			}

			View.animateWithDuration(HIDE_SHOW_BAR_DURATION, new View.Animations() {
				public void performAnimatedChanges() {
					navigationBar.setFrame(navigationFrame);
					containerView.setFrame(containerFrame);

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

	abstract private class TransitionController {
		/**
		 * Handle the animation between the from and to view controllers.
		 * Guaranteed to have both a from and to view controller, and we're
		 * always animated if we're at this point.
		 *
		 * @param fromViewController View controller transitioning from
		 * @param toViewController View controller transition to
		 * @param push Whether or not we're adding the view controller, or removing it
		 * @param completion Callback to run upon completion, may be null
		 */
		abstract void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion);
	}

	private class TransitionControlleriOS extends TransitionController {

		void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
			final Rect bounds = containerView.getBounds();

			final Rect fromToFrame = bounds.copy();
			final Rect toFrame = bounds.copy();

			fromToFrame.origin.x = bounds.size.width * (push ? 1.0f : -1.0f);
			toViewController.getView().setFrame(fromToFrame);

			final Rect viewBounds = getView().getBounds();

			transition = true;

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

					if(showHideNavigationBarDuringTransition) {
						boolean restore = View.areAnimationsEnabled();
						View.setAnimationsEnabled(false);

						Rect containerFrame = viewBounds.copy();
						Rect fromFromFrame = viewBounds.copy();
						Rect fromNavigationFrame = navigationBar.getFrame();
						Rect toNavigationFrame = navigationBar.getFrame();
						toNavigationFrame.origin.y = 0.0f;
						fromNavigationFrame.origin.y = 0.0f;

						float navigationBarHeight = fromNavigationFrame.size.height;

						if(!navigationBarHidden) {
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
						containerView.setFrame(containerFrame);
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

					NavigationController.this.transition = false;
					showHideNavigationBarDuringTransition = false;

					Application.sharedApplication().endIgnoringInteractionEvents();
				}
			};


			navigationBar.setDelegate(null);

			if(showHideNavigationBarDuringTransition) {
				if(push) {
					navigationBar.pushNavigationItem(toViewController.getNavigationItem(), false);
					navigationBar.setDelegate(navigationBarDelegate);
				}

				View.animateWithDuration(HIDE_SHOW_BAR_DURATION, new View.Animations() {
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

	private class TransitionControllerAndroid extends TransitionController {

		void transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
			performAfterDelay(0, new Runnable() {
				public void run() {
					_transitionFromViewController(fromViewController, toViewController, push, completion);
				}
			});
		}

		void _transitionFromViewController(final ViewController fromViewController, final ViewController toViewController, final boolean push, final Runnable completion) {
			final View fromView = fromViewController.getView();
			final View toView = toViewController.getView();
			final Rect bounds = containerView.getBounds();

			final View view = getView();
			Rect viewBounds = view.getBounds();

			toView.setFrame(bounds);

			fromViewController.beginAppearanceTransition(false, push);
			toViewController.beginAppearanceTransition(true, !push);

			long start = android.os.SystemClock.uptimeMillis();

			float navigationBarHeight = navigationBar.getFrame().size.height;

			if(push) {
				this.adjustNavigationBar(toViewController.getNavigationItem(), true);
			}

			// Cache from view to image
			final ImageView transitionView = new ImageView(viewBounds);
			{
				Context context = new Context(viewBounds.size, view.scale, Bitmap.Config.ARGB_8888);
				context.save();
				context.getCanvas().translate(0.0f, navigationBarHeight * view.scale);

				if(push) {
					toView.getLayer().renderInContext(context);
				} else {
					fromView.getLayer().renderInContext(context);
				}

				context.restore();

				navigationBar.getLayer().renderInContext(context);

				transitionView.setImage(context.getImage());
				view.addSubview(transitionView);
			}

			if(push) {
				this.adjustNavigationBar(fromViewController.getNavigationItem(), false);
			} else {
				this.adjustNavigationBar(toViewController.getNavigationItem(), false);
			}

			MWarn("Took %dms to build UI cache", android.os.SystemClock.uptimeMillis() - start);

			// Animate
			final AffineTransform scaled = AffineTransform.scaled(0.8f, 0.8f);

			if(push) {
				transitionView.setAlpha(0.0f);
				transitionView.setTransform(scaled);
			} else {
				fromView.removeFromSuperview();
				containerView.addSubview(toView);
				view.bringSubviewToFront(transitionView);
			}

			final boolean restore = view.isUserInteractionEnabled();
			view.setUserInteractionEnabled(false);

			View.animateWithDuration(300, 1, new View.Animations() {
				public void performAnimatedChanges() {
					View.setTimingFunction(new TimingFunction.CubicBezierCurveTimingFunction(0.215f, 0.610f, 0.355f, 1.000f));

					if(push) {
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

					if(push) {
						adjustNavigationBar(toViewController.getNavigationItem(), true);
						containerView.addSubview(toView);
						fromView.removeFromSuperview();
					}

					fromViewController.endAppearanceTransition();
					toViewController.endAppearanceTransition();

					transitionView.removeFromSuperview();

					if(completion != null) {
						completion.run();
					}

					view.setUserInteractionEnabled(restore);
				}
			});

		}

		private void adjustNavigationBar(NavigationItem navigationItem, boolean push) {
			navigationBar.setDelegate(null);

			if(push) {
				navigationBar.pushNavigationItem(navigationItem, false);
			} else {
				navigationBar.popToNavigationItemAnimated(navigationItem, false, null, null);
			}

			navigationBar.setDelegate(navigationBarDelegate);
		}

	}

}