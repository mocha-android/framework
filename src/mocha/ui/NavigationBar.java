/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.util.ArrayList;
import java.util.List;

public class NavigationBar extends View {
	private static final EdgeInsets BUTTON_EDGE_INSETS = new EdgeInsets(7.0f, 5.0f, 0.0f, 5.0f);
	private static final float MIN_BUTTON_WIDTH = 33.0f;
	private static final float MAX_BUTTON_WIDTH = 200.0f;
	private static final float MAX_BUTTON_HEIGHT = 30.0f;
	static final long ANIMATION_DURATION = 330;
	static final AnimationCurve ANIMATION_CURVE = AnimationCurve.EASE_IN_OUT;

	public interface Delegate {
		public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item);
		public void didPushItem(NavigationBar navigationBar, NavigationItem item);

		public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item);
		public void didPopItem(NavigationBar navigationBar, NavigationItem item);
	}

	private enum Transition {
		PUSH, POP, RELOAD
	}

	private BarStyle barStyle;
	private int tintColor;
	private List<NavigationItem> items;
	private Delegate delegate;
	private boolean needsReload;

	private View leftView;
	private View centerView;
	private View rightView;
	private Image backgroundImage;
	private boolean leftIsBackButton;

	public NavigationBar() { this(new Rect(0.0f, 0.0f, 320.0f, 44.0f)); }

	public NavigationBar(Rect frame) {
		super(frame);

		this.setTintColor(Color.rgba(0.529f, 0.616f, 0.722f, 1.0f));
		this.backgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_background);
		this.items = new ArrayList<NavigationItem>();
	}

	public BarStyle getBarStyle() {
		return barStyle;
	}

	public void setBarStyle(BarStyle barStyle) {
		this.barStyle = barStyle;
	}

	public int getTintColor() {
		return tintColor;
	}

	public void setTintColor(int tintColor) {
		this.tintColor = tintColor;
		this.setBackgroundColor(tintColor);
	}

	public NavigationItem getTopItem() {
		return this.items.size() > 0 ? this.items.get(this.items.size() - 1) : null;
	}

	public NavigationItem getBackItem() {
		return this.items.size() > 1 ? this.items.get(this.items.size() - 2) : null;
	}

	public List<NavigationItem> getItems() {
		return items;
	}

	public void setItems(List<NavigationItem> items) {
		this.setItems(items, false, null, null);
	}

	void setItems(List<NavigationItem> items, boolean animated) {
		this.setItems(items, animated, null, null);
	}

	public void setItems(List<NavigationItem> items, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		if(!this.items.equals(items)) {
			this.items.clear();
			this.items.addAll(items);

			this.updateItemsWithTransition(Transition.PUSH, animated, additionalTransitions, transitionCompleteCallback);
		}
	}

	public void pushNavigationItem(NavigationItem navigationItem, boolean animated) {
		this.pushNavigationItem(navigationItem, animated, null, null);
	}

	void pushNavigationItem(NavigationItem navigationItem, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		boolean shouldPush = this.delegate == null || this.delegate.shouldPushItem(this, navigationItem);

		if(shouldPush) {
			this.items.add(navigationItem);

			this.updateItemsWithTransition(Transition.PUSH, animated, additionalTransitions, transitionCompleteCallback);

			if(this.delegate != null) {
				this.delegate.didPushItem(this, navigationItem);
			}
		}
	}

	public NavigationItem popNavigationItemAnimated(boolean animated) {
		return this.popNavigationItemAnimated(animated, null, null);
	}

	NavigationItem popNavigationItemAnimated(boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		NavigationItem previousItem = this.getTopItem();

		if(previousItem == null || (this.delegate != null && !this.delegate.shouldPopItem(this, previousItem))) {
			return null;
		} else {
			this.items.remove(previousItem);
			this.updateItemsWithTransition(Transition.POP, animated, additionalTransitions, transitionCompleteCallback);

			if(this.delegate != null) {
				this.delegate.didPopItem(this, previousItem);
			}

			return previousItem;
		}
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	private void updateItemsWithTransition(final Transition transition, boolean animated, final Runnable additionalTransitions, final Runnable transitionCompleteCallback) {
		final View previousLeftView = this.leftView;
		final View previousRightView = this.rightView;
		final View previousCenterView = this.centerView;
		final boolean previousLeftIsBackButton = this.leftIsBackButton;

		NavigationItem topItem = this.getTopItem();

		if (topItem != null) {
			NavigationItem backItem = this.getBackItem();

			if (backItem != null && topItem.getLeftBarButtonItem() == null && !topItem.hidesBackButton()) {
				this.leftView = getBackItemButton(backItem);
				leftIsBackButton = true;
			} else {
				this.leftView = getItemView(topItem.getLeftBarButtonItem());
				leftIsBackButton = false;
			}

			if (this.leftView != null) {
				Rect frame = this.leftView.getFrame();
				frame.origin.x = BUTTON_EDGE_INSETS.left;
				frame.origin.y = BUTTON_EDGE_INSETS.top;
				this.leftView.setFrame(frame);
			}

			this.rightView = getItemView(topItem.getRightBarButtonItem());

			if (this.rightView != null) {
				this.rightView.setAutoresizing(Autoresizing.FLEXIBLE_LEFT_MARGIN);
				Rect frame = this.rightView.getFrame();
				frame.origin.x = this.getBounds().size.width - frame.size.width - BUTTON_EDGE_INSETS.right;
				frame.origin.y = BUTTON_EDGE_INSETS.top;
				this.rightView.setFrame(frame);
			}

			Rect titleFrame = new Rect();
			titleFrame.size = this.getBounds().size.copy();
			titleFrame.size.width -= BUTTON_EDGE_INSETS.left + BUTTON_EDGE_INSETS.right;

			if(this.leftView != null) {
				titleFrame.origin.x = this.leftView.getFrame().maxX() + BUTTON_EDGE_INSETS.left;
				titleFrame.size.width -= this.leftView.getFrame().size.width + BUTTON_EDGE_INSETS.left;
			} else {
				titleFrame.origin.x = BUTTON_EDGE_INSETS.left;
			}

			if(this.rightView != null) {
				titleFrame.size.width -= this.rightView.getFrame().size.width + BUTTON_EDGE_INSETS.right;
			}

			this.centerView = topItem.getTitleView();

			if (this.centerView == null) {
				this.centerView = new NavigationItemTitleView(topItem);
			}

			this.centerView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
			this.centerView.setFrame(titleFrame);
			this.centerView.setAlpha(0.0f);

			if(this.centerView instanceof NavigationItemTitleView) {
				((NavigationItemTitleView)this.centerView).setDestinationFrame(titleFrame, this.getBounds());
			}
		} else {
			this.leftView = null;
			this.centerView = null;
			this.rightView = null;
			leftIsBackButton = false;
		}

		if(animated) {
			final Rect centerFrame = this.centerView != null ? this.centerView.getFrame() : null;
			if(centerFrame != null) {
				this.centerView.setAlpha(0.0f);
				this.centerView.setFrame(this.getAnimateFromRectForTitleView(this.centerView, transition, false));
				this.addSubview(this.centerView);
			}

			if(this.rightView != null) {
				this.rightView.setAlpha(0.0f);
				this.addSubview(this.rightView);
			}

			final Rect leftFrame = this.leftView != null ? this.leftView.getFrame() : null;
			if(leftFrame != null) {
				this.leftView.setAlpha(0.0f);

				if(this.leftIsBackButton) {
					this.leftView.setFrame(this.getAnimateFromRectForBackButton(this.leftView, transition, false));
				}

				this.addSubview(this.leftView);
			}

			View.animateWithDuration(ANIMATION_DURATION, new Animations() {
				public void performAnimatedChanges() {
					View.setAnimationCurve(ANIMATION_CURVE);

					if(leftView != null) {
						leftView.setAlpha(1.0f);
						leftView.setFrame(leftFrame);
					}

					if(rightView != null) {
						rightView.setAlpha(1.0f);
					}

					if(centerView != null) {
						centerView.setAlpha(1.0f);
						centerView.setFrame(centerFrame);
					}

					if(previousRightView != null) {
						previousRightView.setAlpha(0.0f);
					}

					if(previousCenterView != null) {
						previousCenterView.setAlpha(0.0f);
						previousCenterView.setFrame(getAnimateFromRectForTitleView(previousCenterView, transition, true));
					}

					if(previousLeftView != null) {
						previousLeftView.setAlpha(0.0f);

						if(previousLeftIsBackButton) {
							previousLeftView.setFrame(getAnimateFromRectForBackButton(previousLeftView, transition, true));
						}
					}

					if(additionalTransitions != null) {
						additionalTransitions.run();
					}
				}
			}, new AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					if(previousRightView != null) previousRightView.removeFromSuperview();
					if(previousLeftView != null) previousLeftView.removeFromSuperview();
					if(previousCenterView != null) previousCenterView.removeFromSuperview();

					if(transitionCompleteCallback != null) {
						transitionCompleteCallback.run();
					}
				}
			});
		} else {
			if(previousRightView != null) previousRightView.removeFromSuperview();
			if(previousLeftView != null) previousLeftView.removeFromSuperview();
			if(previousCenterView != null) previousCenterView.removeFromSuperview();

			if(this.centerView != null) this.addSubview(this.centerView);
			if(this.leftView != null) this.addSubview(this.leftView);
			if(this.rightView != null) this.addSubview(this.rightView);

			if(additionalTransitions != null) {
				additionalTransitions.run();
			}

			if(transitionCompleteCallback != null) {
				transitionCompleteCallback.run();
			}
		}
	}


	private Rect getAnimateFromRectForBackButton(View backButton, Transition transition, boolean previous) {
		Rect frame = backButton.getFrame();

		if(transition != Transition.PUSH && transition != Transition.POP) {
			return frame;
		}

		if((transition == Transition.PUSH && previous) || (transition == Transition.POP && !previous)) {
			frame.origin.x = -frame.size.width - BUTTON_EDGE_INSETS.left;
		} else {
			frame.origin.x = floorf((this.getBounds().size.width - frame.size.width) / 2.0f);
		}

		return frame;
	}

	private Rect getAnimateFromRectForTitleView(View titleView, Transition transition, boolean previous) {
		Rect frame = titleView.getFrame();

		if(transition != Transition.PUSH && transition != Transition.POP) {
			return frame;
		}

		if(titleView instanceof NavigationItemTitleView) {
			if((transition == Transition.PUSH && previous) || (transition == Transition.POP && !previous)) {
				frame.origin.x = -((NavigationItemTitleView) titleView).getTextRect().minX();
			} else {
				frame.origin.x = this.getBounds().size.width - ((NavigationItemTitleView) titleView).getTextRect().origin.x;
			}
		} else {
			if((transition == Transition.PUSH && previous) || (transition == Transition.POP && !previous)) {
				frame.origin.x = -frame.size.width;
			} else {
				frame.origin.x = this.getBounds().size.width;
			}
		}

		return frame;
	}

	void updateNavigationItem(NavigationItem navigationItem, boolean animated) {
		if(navigationItem != this.getTopItem()) return;

		// TODO: Handle animation
		this.needsReload = true;
		this.setNeedsLayout();
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if(this.needsReload) {
			this.needsReload = false;
			this.updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}
	}

	public void draw(Context context, Rect rect) {
		this.backgroundImage.draw(context, rect);
	}

	private static void setBarButtonSize(View view) {
		Rect frame = view.getFrame();
		frame.size = view.sizeThatFits(new Size(MAX_BUTTON_WIDTH, MAX_BUTTON_HEIGHT));
		frame.size.width = Math.max(frame.size.width, MIN_BUTTON_WIDTH);
		frame.size.height = MAX_BUTTON_HEIGHT;
		view.setFrame(frame);
	}

	private Button getBackItemButton(NavigationItem navigationItem) {
		if(navigationItem == null) return null;

		NavigationButton button = NavigationButton.backButton(navigationItem);
		button.addActionTarget(new Control.ActionTarget() {
			public void onControlEvent(Control control, Control.ControlEvent controlEvent) {
				NavigationBar.this.popNavigationItemAnimated(true);
			}
		}, Control.ControlEvent.TOUCH_UP_INSIDE);

		setBarButtonSize(button);

		return button;
	}

	private View getItemView(final BarButtonItem barButtonItem) {
		if(barButtonItem == null) return null;

		if(barButtonItem.getCustomView() != null) {
			return barButtonItem.getCustomView();
		} else {
			NavigationButton button = NavigationButton.button(barButtonItem);
			button.addActionTarget(new Control.ActionTarget() {
				public void onControlEvent(Control control, Control.ControlEvent controlEvent) {
					BarButtonItem.Action action = barButtonItem.getAction();

					if(action != null) {
						action.action(barButtonItem);
					}
				}
			}, Control.ControlEvent.TOUCH_UP_INSIDE);

			setBarButtonSize(button);

			return button;
		}
	}

}
