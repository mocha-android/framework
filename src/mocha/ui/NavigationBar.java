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
	private static final long ANIMATION_DURATION = 330;

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
		this.setItems(items, false);
	}

	public void setItems(List<NavigationItem> items, boolean animated) {
		if(!this.items.equals(items)) {
			this.items.clear();
			this.items.addAll(items);

			this.setViewsWithTransition(Transition.PUSH, animated);
		}
	}

	public void pushNavigationItem(NavigationItem navigationItem, boolean animated) {
		boolean shouldPush = this.delegate == null || this.delegate.shouldPushItem(this, navigationItem);

		if(shouldPush) {
			this.items.add(navigationItem);

			this.setViewsWithTransition(Transition.PUSH, animated);

			if(this.delegate != null) {
				this.delegate.didPushItem(this, navigationItem);
			}
		}
	}

	public NavigationItem popNavigationItemAnimated(boolean animated) {
		NavigationItem previousItem = this.getTopItem();

		if(previousItem == null || (this.delegate != null && !this.delegate.shouldPopItem(this, previousItem))) {
			return null;
		} else {
			this.items.remove(previousItem);
			this.setViewsWithTransition(Transition.POP, animated);

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

	private void removeViews(List<View> views) {
		for(View view : views) {
			view.removeFromSuperview();
		}
	}

	private void setViewsWithTransition(Transition transition, boolean animated) {
		final List<View> previousViews = new ArrayList<View>();

		if(this.leftView != null) {
			previousViews.add(this.leftView);
		}

		if(this.centerView != null) {
			previousViews.add(this.centerView);
		}

		if(this.rightView != null) {
			previousViews.add(this.rightView);
		}

		if (animated) {
			float moveCenterBy = this.getBounds().size.width - ((this.centerView != null) ? this.centerView.getFrame().origin.x : 0.0f);
			float moveLeftBy = this.getBounds().size.width * 0.33f;

			if (transition == Transition.PUSH) {
				moveCenterBy *= -1.f;
				moveLeftBy *= -1.f;
			}

			final float _moveLeftBy = moveLeftBy;
			final float _moveCenterBy = moveCenterBy;

			View.animateWithDuration(ANIMATION_DURATION, new Animations() {
				public void performAnimatedChanges() {
					if(leftView != null) {
						leftView.setFrame(leftView.getFrame().offset(_moveLeftBy, 0.0f));
					}

					if(centerView != null) {
						centerView.setFrame(centerView.getFrame().offset(_moveCenterBy, 0.0f));
					}
				}
			}, new AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					removeViews(previousViews);
				}
			});


			View.beginAnimations(null, null);
			View.setAnimationCurve(AnimationCurve.EASE_IN_OUT);
			View.setAnimationDuration((long)((double)ANIMATION_DURATION * 0.8));
			if(leftView != null) leftView.setAlpha(0.0f);
			if(centerView != null) centerView.setAlpha(0.0f);
			if(rightView != null) rightView.setAlpha(0.0f);
			View.commitAnimations();
		} else {
			removeViews(previousViews);
		}

		NavigationItem topItem = this.getTopItem();

		if (topItem != null) {
			NavigationItem backItem = this.getBackItem();

			Rect leftFrame;
			Rect rightFrame;

			if (backItem != null && topItem.getLeftBarButtonItem() == null && !topItem.hidesBackButton()) {
				this.leftView = getBackItemButton(backItem);
			} else {
				this.leftView = getItemView(topItem.getLeftBarButtonItem());
			}

			if (this.leftView != null) {
				leftFrame = this.leftView.getFrame();
				leftFrame.origin.x = BUTTON_EDGE_INSETS.left;
				leftFrame.origin.y = BUTTON_EDGE_INSETS.top;
				this.leftView.setFrame(leftFrame);
				this.leftView.setAlpha(0.0f);
				this.addSubview(this.leftView);
			} else {
				leftFrame = Rect.zero();
			}

			this.rightView = getItemView(topItem.getRightBarButtonItem());

			if (this.rightView != null) {
				this.rightView.setAutoresizing(Autoresizing.FLEXIBLE_LEFT_MARGIN);
				rightFrame = this.rightView.getFrame();
				rightFrame.origin.x = this.getBounds().size.width - rightFrame.size.width - BUTTON_EDGE_INSETS.right;
				rightFrame.origin.y = BUTTON_EDGE_INSETS.top;
				this.rightView.setFrame(rightFrame);
				this.rightView.setAlpha(0.0f);
				this.addSubview(this.rightView);
			} else {
				rightFrame = Rect.zero();
			}

			this.centerView = topItem.getTitleView();

			if (this.centerView == null) {
				Label titleLabel = new Label();
				titleLabel.setText(topItem.getTitle());
				titleLabel.setTextAlignment(TextAlignment.CENTER);
				titleLabel.setBackgroundColor(Color.TRANSPARENT);
				titleLabel.setTextColor(Color.WHITE);
				titleLabel.setShadowColor(Color.rgba(0.0f, 0.0f, 0.0f, 0.5f));
				titleLabel.setShadowOffset(new Size(0.0f, 1.0f));
				titleLabel.setFont(Font.getBoldSystemFontWithSize(17.0f));
				this.centerView = titleLabel;
			}

			float centerPadding = Math.max(leftFrame.size.width, rightFrame.size.width);
			this.centerView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			this.centerView.setFrame(new Rect(BUTTON_EDGE_INSETS.left+centerPadding, BUTTON_EDGE_INSETS.top, this.getBounds().size.width - BUTTON_EDGE_INSETS.right - BUTTON_EDGE_INSETS.left - centerPadding - centerPadding, MAX_BUTTON_HEIGHT));
			this.centerView.setAlpha(0.0f);
			this.addSubview(centerView);

			if (animated) {
				float moveCenterBy = this.getBounds().size.width - ((this.centerView != null) ? this.centerView.frame.origin.x : 0);
				float moveLeftBy = this.getBounds().size.width * 0.33f;

				if (transition == Transition.PUSH) {
					moveLeftBy *= -1.0f;
					moveCenterBy *= -1.0f;
				}

				final Rect destinationLeftFrame = this.leftView != null ? this.leftView.frame : null;
				final Rect destinationCenterFrame = this.centerView != null ? this.centerView.frame : null;

				if (this.leftView != null) {
					this.leftView.setFrame(this.leftView.getFrame().offset(-moveLeftBy, 0.0f));
					this.leftView.setAlpha(0.0f);
				}

				if(this.centerView != null) {
					this.centerView.setFrame(this.centerView.getFrame().offset(-moveCenterBy, 0.0f));
					this.centerView.setAlpha(0.0f);
				}

				if(this.rightView != null) {
					this.rightView.setAlpha(0.0f);
				}


				if(this.leftView != null || this.centerView != null) {
					View.animateWithDuration(ANIMATION_DURATION, new Animations() {
						public void performAnimatedChanges() {
							if(leftView != null) {
								leftView.setFrame(destinationLeftFrame);
							}

							if(centerView != null) {
								centerView.setFrame(destinationCenterFrame);
							}
						}
					});
				}

				View.beginAnimations(null, null);
				View.setAnimationCurve(AnimationCurve.EASE_IN_OUT);
				View.setAnimationDuration((long)((double)ANIMATION_DURATION * 0.8));
				if(leftView != null) leftView.setAlpha(1.0f);
				if(centerView != null) centerView.setAlpha(1.0f);
				if(rightView != null) rightView.setAlpha(1.0f);
				View.commitAnimations();
			} else {
				if(leftView != null) leftView.setAlpha(1.0f);
				if(centerView != null) centerView.setAlpha(1.0f);
				if(rightView != null) rightView.setAlpha(1.0f);
			}
		} else {
			this.leftView = null;
			this.centerView = null;
			this.rightView = null;
		}
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
			this.setViewsWithTransition(Transition.RELOAD, false);
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
