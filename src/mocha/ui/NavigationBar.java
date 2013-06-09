/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class NavigationBar extends View {
	private static final EdgeInsets DEFAULT_ITEM_EDGE_INSETS = new EdgeInsets(7.0f, 5.0f, 7.0f, 5.0f);
	private static final float MIN_BUTTON_WIDTH = 33.0f;
	private static final float MAX_BUTTON_WIDTH = 200.0f;
	private static final float MAX_TITLE_HEIGHT_DEFAULT = 30.0f;
	private static final float MAX_TITLE_HEIGHT_LANDSCAPE_PHONE = 24.0f;
	static final long ANIMATION_DURATION = 330;
	static final AnimationCurve ANIMATION_CURVE = AnimationCurve.EASE_IN_OUT;

	public interface Delegate {
		/**
		 * Called before a navigation item is pushed onto the stack
		 *
		 * @param navigationBar Navigation bar the item is trying to be pushed onto
		 * @param item Navigation item being pushed
		 * @return If true, the navigation item will be pushed onto the stack, if false it will be prevented
		 */
		public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item);

		/**
		 * Called after the navigation item has been pushed onto the stack
		 *
		 * @param navigationBar Navigation bar the item was pushed onto
		 * @param item Navigation item just pushed onto the stack
		 */
		public void didPushItem(NavigationBar navigationBar, NavigationItem item);

		/**
		 * Called before a navigation item is popped from the stack
		 *
		 * @param navigationBar Navigation bar trying to pop the item
		 * @param item Navigation item being popped
		 * @return If true, the navigation item will be popped from the stack, if false it will be prevented
		 */
		public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item);

		/**
		 * Called after a navigation item has been popped from the stack
		 *
		 * @param navigationBar Navigation bar the item was popped from
		 * @param item Navigation item just popped from the stack
		 */
		public void didPopItem(NavigationBar navigationBar, NavigationItem item);
	}

	public enum TitleAlignment {
		/**
		 * Horizontally align title to the left
		 */
		LEFT,

		/**
		 * Center title horizontally
		 */
		CENTER,

		/**
		 * Horizontally align title to the right
		 */
		RIGHT
	}

	private enum Transition {
		PUSH, POP, RELOAD
	}

	private BarStyle barStyle;
	private int tintColor;
	private List<NavigationItem> items;
	private Delegate delegate;
	private boolean needsReload;
	private EdgeInsets itemEdgeInsets;
	private TitleAlignment titleAlignment;

	private View leftView;
	private View centerView;
	private View rightView;
	private Image backgroundImage;
	private boolean leftIsBackButton;

	private TextAttributes titleTextAttributes;
	private BarMetricsStorage<Float> titleVerticalPositionAdjustment;
	private BarMetricsStorage<Image> backgroundImages;
	private Image shadowImage;
	private ImageView shadowImageView;

	private NavigationItemDelegate navigationItemDelegate = new NavigationItemDelegate();

	private static mocha.ui.Appearance.Storage<NavigationBar, Appearance> appearanceStorage;

	/**
	 * Get the appearance for a specific NavigationBar subclass
	 *
	 * @param cls Subclass
	 * @param <E> Subclass
	 * @return Appearance
	 */
	public static <E extends NavigationBar> Appearance appearance(Class<E> cls) {
		if(appearanceStorage == null) {
			appearanceStorage = new mocha.ui.Appearance.Storage<NavigationBar, Appearance>(NavigationBar.class, Appearance.class);
		}

		return appearanceStorage.appearance(cls);
	}

	/**
	 * Get the appearance for all NavigationBar classes
	 *
	 * @return Appearance
	 */
	public static Appearance appearance() {
		return appearance(NavigationBar.class);
	}

	public NavigationBar() { this(new Rect(0.0f, 0.0f, 320.0f, 44.0f)); }
	public NavigationBar(Rect frame) { super(frame); }

	@Override
	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.setTintColor(Color.rgba(0.529f, 0.616f, 0.722f, 1.0f));
		this.backgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_background);
		this.items = new ArrayList<NavigationItem>();
		this.itemEdgeInsets = DEFAULT_ITEM_EDGE_INSETS;

		this.titleVerticalPositionAdjustment = new BarMetricsStorage<Float>();
		this.backgroundImages = new BarMetricsStorage<Image>();
		this.titleAlignment = TitleAlignment.CENTER;

		if(appearanceStorage != null) {
			appearanceStorage.apply(this);
		}
	}

	@Override
	public Size sizeThatFits(Size size) {
		return new Size(size.width, Math.min(size.height, 44.0f));
	}

	/**
	 * Get the bar style
	 * @return Bar style
	 */
	public BarStyle getBarStyle() {
		return barStyle;
	}

	/**
	 * Set the bar style
	 * @param barStyle Bar style
	 */
	public void setBarStyle(BarStyle barStyle) {
		this.barStyle = barStyle;
	}

	/**
	 * Get the tint color of the bar
	 * @return Tint color
	 */
	public int getTintColor() {
		return tintColor;
	}

	/**
	 * Set the tint color of the bar
	 * @param tintColor
	 */
	public void setTintColor(int tintColor) {
		this.tintColor = tintColor;
		this.setBackgroundColor(tintColor);
	}

	/**
	 * Get the edge insets for bar button items
	 * @return Edge insets
	 */
	public EdgeInsets getItemEdgeInsets() {
		if(this.itemEdgeInsets == DEFAULT_ITEM_EDGE_INSETS) {
			return null;
		} else {
			return this.itemEdgeInsets.copy();
		}
	}

	/**
	 * Set the edge insets for bar button items
	 *
	 * @param itemEdgeInsets Edge insets
	 */
	public void setItemEdgeInsets(EdgeInsets itemEdgeInsets) {
		if(itemEdgeInsets == null) {
			this.itemEdgeInsets = DEFAULT_ITEM_EDGE_INSETS;
		} else {
			this.itemEdgeInsets = itemEdgeInsets;
		}
	}

	/**
	 * Get the horizontal title aligment
	 * @return Title alignment
	 */
	public TitleAlignment getTitleAlignment() {
		return this.titleAlignment;
	}

	/**
	 * Set the horizontal title alignment
	 * @param titleAlignment Title alignment
	 */
	public void setTitleAlignment(TitleAlignment titleAlignment) {
		if(titleAlignment == null) {
			this.titleAlignment = TitleAlignment.CENTER;
		} else {
			this.titleAlignment = titleAlignment;
		}
	}

	/**
	 * Get the top navigation item in the stack
	 * @return Top navigation item or null
	 */
	public NavigationItem getTopItem() {
		return this.items.size() > 0 ? this.items.get(this.items.size() - 1) : null;
	}

	/**
	 * Get the previous navigation item in the stack
	 * @return Previous navigation item or null
	 */
	public NavigationItem getBackItem() {
		return this.items.size() > 1 ? this.items.get(this.items.size() - 2) : null;
	}

	/**
	 * Get all navigation item's in the stack
	 * @return All navigation item's in the stack
	 */
	public List<NavigationItem> getItems() {
		return items;
	}

	/**
	 * Set the navigation item stack, without any animation
	 *
	 * @param items Navigation item stack
	 */
	public void setItems(List<NavigationItem> items) {
		this.setItems(items, false, null, null);
	}

	/**
	 * Set the navigaiton item stack, optionally animated
	 *
	 * @param items Navigation item stack
	 * @param animated If true, the transition will be animated, if false no animation will occur
	 */
	public void setItems(List<NavigationItem> items, boolean animated) {
		this.setItems(items, animated, null, null);
	}

	void setItemsWithoutUpdatingView(List<NavigationItem> items) {
		this.items.clear();
		this.items.addAll(items);

		for(NavigationItem item : items) {
			item.setDelegate(this.navigationItemDelegate);
		}
	}

	/**
	 * Set the navigaiton item stack, optionally animated
	 *
	 * @param items Navigation item stack
	 * @param animated If true, the transition will be animated, if false no animation will occur
	 * @param additionalTransitions Additional transitions/animations that need to be performed in the same animation context
	 * @param transitionCompleteCallback Callback for with the transition has finished
	 */
	public void setItems(List<NavigationItem> items, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		if(!this.items.equals(items)) {
			this.items.clear();
			this.items.addAll(items);

			for(NavigationItem item : items) {
				item.setDelegate(this.navigationItemDelegate);
			}

			this.updateItemsWithTransition(Transition.PUSH, animated, additionalTransitions, transitionCompleteCallback);
		}
	}

	void replaceNavigationItem(NavigationItem oldItem, NavigationItem newItem) {
		if(this.items.contains(oldItem)) {
			int index = this.items.indexOf(oldItem);

			if(oldItem.getDelegate() == this.navigationItemDelegate) {
				oldItem.setDelegate(null);
			}

			newItem.setDelegate(this.navigationItemDelegate);
			this.items.set(index, newItem);
			this.needsReload = true;
			this.layoutSubviews();
		}
	}

	/**
	 * Push a navigation item onto the stack
	 *
	 * @param navigationItem Navigation item to add to the top of the stack
	 * @param animated If true, the transition will be animated, if false no animation will occur
	 */
	public void pushNavigationItem(NavigationItem navigationItem, boolean animated) {
		this.pushNavigationItem(navigationItem, animated, null, null);
	}

	void pushNavigationItem(NavigationItem navigationItem, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		boolean shouldPush = this.delegate == null || this.delegate.shouldPushItem(this, navigationItem);

		if(shouldPush) {
			this.items.add(navigationItem);
			navigationItem.setDelegate(this.navigationItemDelegate);

			this.updateItemsWithTransition(Transition.PUSH, animated, additionalTransitions, transitionCompleteCallback);

			if(this.delegate != null) {
				this.delegate.didPushItem(this, navigationItem);
			}
		}
	}

	/**
	 * Remove the top most navigation item from the stack
	 * @param animated If true, the transition will be animated, if false no animation will occur
	 * @return The navigation item being removed from the stack
	 */
	public NavigationItem popNavigationItemAnimated(boolean animated) {
		return this.popNavigationItemAnimated(animated, null, null);
	}

	void popToNavigationItemAnimated(NavigationItem navigationItem, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		if(!this.items.contains(navigationItem)) return;

		NavigationItem topItem = this.getTopItem();
		if(topItem == navigationItem) return;

		List<NavigationItem> items = new ArrayList<NavigationItem>(this.items);
		boolean shouldRemove = false;

		for(NavigationItem item : items) {
			if(shouldRemove) {
				if(item != topItem) {
					item.setDelegate(null);
					this.items.remove(item);
				}
			} else if(item == navigationItem) {
				shouldRemove = true;
			}
		}

		this.popNavigationItemAnimated(animated, additionalTransitions, transitionCompleteCallback);
	}

	NavigationItem popNavigationItemAnimated(boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		NavigationItem previousItem = this.getTopItem();

		if(previousItem == null || (this.delegate != null && !this.delegate.shouldPopItem(this, previousItem))) {
			return null;
		} else {
			previousItem.setDelegate(null);
			this.items.remove(previousItem);
			this.updateItemsWithTransition(Transition.POP, animated, additionalTransitions, transitionCompleteCallback);

			if(this.delegate != null) {
				this.delegate.didPopItem(this, previousItem);
			}

			return previousItem;
		}
	}

	void clearNavigationItems() {
		for(NavigationItem item : this.items) {
			item.setDelegate(null);
		}

		this.items.clear();

		if(this.leftView != null) {
			this.leftView.removeFromSuperview();
			this.leftView = null;
		}

		if(this.centerView != null) {
			this.centerView.removeFromSuperview();
			this.centerView = null;
		}

		if(this.rightView != null) {
			this.rightView.removeFromSuperview();
			this.rightView = null;
		}
	}

	/**
	 * Get the navigation bar delegate
	 * @return Navigation bar delegate
	 */
	public Delegate getDelegate() {
		return delegate;
	}

	/**
	 * Set the navigation bar delegate
	 * @param delegate Navigation bar delegate
	 */
	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	/**
	 * Get shadow image
	 *
	 * @return Shadow image, may be null
	 */
	public Image getShadowImage() {
		return shadowImage;
	}

	/**
	 * Set the shadow image
	 * @param shadowImage Shadow image or null to use the default
	 */
	public void setShadowImage(Image shadowImage) {
		this.shadowImage = shadowImage;
	}

	/**
	 * Get a background image for the specified bar metrics
	 *
	 * @param barMetrics Bar metrics
	 * @return Background image, may be null
	 */
	public Image getBackgroundImage(BarMetrics barMetrics) {
		return this.backgroundImages.get(barMetrics);
	}

	/**
	 * Set a background image for the specified bar metrics
	 *
	 * @param backgroundImage Background image
	 * @param barMetrics Bar metrics
	 */
	public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics) {
		this.backgroundImages.set(barMetrics, backgroundImage);
	}

	/**
	 * Get the vertical title offset for the specified bar metrics
	 *
	 * @param barMetrics Bar metrics
	 * @return Vertical title offset in points
	 */
	public float getTitleVerticalPositionAdjustment(BarMetrics barMetrics) {
		return this.titleVerticalPositionAdjustment.get(barMetrics);
	}

	/**
	 * Set the vertical title offset for the specified bar metrics
	 * @param adjustment Vertical title offset in points
	 * @param barMetrics Bar metrics
	 */
	public void setTitleVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
		this.titleVerticalPositionAdjustment.set(barMetrics, adjustment);
	}

	/**
	 * Get the text attributes for the title label
	 * @return Text attributes
	 */
	public TextAttributes getTitleTextAttributes() {
		return this.titleTextAttributes;
	}

	/**
	 * Set the text attributes for the title label
	 * @param titleTextAttributes Text attributes
	 */
	public void setTitleTextAttributes(TextAttributes titleTextAttributes) {
		this.titleTextAttributes = titleTextAttributes;
	}

	private void updateItemsWithTransition(final Transition transition, boolean animated, final Runnable additionalTransitions, final Runnable transitionCompleteCallback) {
		View previousLeftView = this.leftView;
		View previousRightView = this.rightView;
		final View previousCenterView = this.centerView;
		final boolean previousLeftIsBackButton = this.leftIsBackButton;
		final Rect bounds = this.getBounds();

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
				frame.origin.x = this.itemEdgeInsets.left;
				frame.origin.y = this.itemEdgeInsets.top;
				this.leftView.setFrame(frame);
			}

			if(this.leftView == previousLeftView) {
				previousLeftView = null;
			}

			this.rightView = getItemView(topItem.getRightBarButtonItem());

			if (this.rightView != null) {
				this.rightView.setAutoresizing(Autoresizing.FLEXIBLE_LEFT_MARGIN);
				Rect frame = this.rightView.getFrame();
				frame.origin.x = bounds.size.width - frame.size.width - this.itemEdgeInsets.right;
				frame.origin.y = this.itemEdgeInsets.top;
				this.rightView.setFrame(frame);
			}

			if(this.rightView == previousRightView) {
				previousRightView = null;
			}

			float titleMinX = this.itemEdgeInsets.left;
			float titleMaxX = bounds.size.width - this.itemEdgeInsets.right;


			if(this.leftView != null) {
				titleMinX += this.leftView.getFrame().size.width + this.itemEdgeInsets.right;
			}

			if(this.rightView != null) {
				titleMaxX -= this.rightView.getFrame().size.width + this.itemEdgeInsets.left;
			}

			this.centerView = topItem.getTitleView();

			Rect titleFrame = new Rect();
			titleFrame.size.width = titleMaxX - titleMinX;
			titleFrame.size.height = bounds.size.height > 30.0f ? MAX_TITLE_HEIGHT_DEFAULT : MAX_TITLE_HEIGHT_LANDSCAPE_PHONE;

			if (this.centerView == null) {
				Float adjustment = this.titleVerticalPositionAdjustment.get(BarMetrics.DEFAULT);
				if(adjustment == null) adjustment = 0.0f;

				this.centerView = new NavigationItemTitleView(titleFrame, topItem, this.titleTextAttributes, adjustment);
			}

			titleFrame.size = this.centerView.sizeThatFits(titleFrame.size);
			titleFrame.origin.y = floorf((bounds.size.height - titleFrame.size.height) / 2.0f);
			titleFrame.origin.x = floorf((bounds.size.width - titleFrame.size.width) / 2.0f);

			if(titleFrame.origin.x < titleMinX) {
				titleFrame.origin.x = titleMinX;
			}

			if(titleFrame.maxX() > titleMaxX) {
				float maxDiff = titleFrame.maxX() - titleMaxX;

				if(titleFrame.origin.x > titleMinX) {
					float minDiff = titleFrame.origin.x - titleMinX;

					if(minDiff >= maxDiff) {
						titleFrame.origin.x -= maxDiff;
						maxDiff = 0;
					} else {
						titleFrame.origin.y -= maxDiff - minDiff;
						maxDiff -= minDiff;
					}
				}

				if(maxDiff > 0) {
					titleFrame.size.width -= maxDiff;
				}
			}

			switch (this.titleAlignment) {
				case LEFT:
					titleFrame.origin.x = titleMinX;
					break;
				case CENTER:
					// Already centered
					break;
				case RIGHT:
					titleFrame.origin.x = titleMaxX - titleFrame.size.width;
					break;
			}

			this.centerView.setAutoresizing(Autoresizing.FLEXIBLE_MARGINS);
			this.centerView.setFrame(titleFrame);
			this.centerView.setAlpha(0.0f);

			if(this.centerView instanceof NavigationItemTitleView) {
				((NavigationItemTitleView)this.centerView).setTitleAlignment(this.titleAlignment);
				((NavigationItemTitleView)this.centerView).setDestinationFrame(titleFrame, bounds);
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

			final View finalPreviousRightView = previousRightView;
			final View finalPreviousLeftView = previousLeftView;
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

					if(finalPreviousRightView != null) {
						finalPreviousRightView.setAlpha(0.0f);
					}

					if(previousCenterView != null) {
						previousCenterView.setAlpha(0.0f);
						previousCenterView.setFrame(getAnimateFromRectForTitleView(previousCenterView, transition, true));
					}

					if(finalPreviousLeftView != null) {
						finalPreviousLeftView.setAlpha(0.0f);

						if(previousLeftIsBackButton) {
							finalPreviousLeftView.setFrame(getAnimateFromRectForBackButton(finalPreviousLeftView, transition, true));
						}
					}

					if(additionalTransitions != null) {
						additionalTransitions.run();
					}
				}
			}, new AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					if(finalPreviousRightView != null) finalPreviousRightView.removeFromSuperview();
					if(finalPreviousLeftView != null) finalPreviousLeftView.removeFromSuperview();
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

			if(this.centerView != null) {
				this.centerView.setAlpha(1.0f);
				this.addSubview(this.centerView);
			}

			if(this.leftView != null && this.leftView.getSuperview() == null) {
				this.leftView.setAlpha(1.0f);
				this.addSubview(this.leftView);
			}

			if(this.rightView != null && this.rightView.getSuperview() == null) {
				this.rightView.setAlpha(1.0f);
				this.addSubview(this.rightView);
			}

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
			frame.origin.x = -frame.size.width - this.itemEdgeInsets.left;
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
		this.layoutSubviews();
	}

	@Override
	public void setFrame(Rect frame) {
		if(!this.getFrame().size.equals(frame.size)) {
			this.needsReload = true;
		}

		super.setFrame(frame);
	}

	@Override
	public void layoutSubviews() {
		super.layoutSubviews();

		if(this.needsReload) {
			this.needsReload = false;
			this.updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}

		Size size;
		if(this.shadowImage != null && (size = this.getShadowImage().getSize()).width > 0 && size.height > 0) {
			if(this.shadowImageView == null) {
				this.shadowImageView = new ImageView();
				this.addSubview(this.shadowImageView);
			}

			if(this.shadowImageView.getImage() != this.shadowImage) {
				this.shadowImageView.setImage(this.shadowImage);
			}

			Rect bounds = this.getBounds();
			this.shadowImageView.setFrame(new Rect(0.0f, bounds.size.height, bounds.size.width, size.height));
		} else if(this.shadowImageView != null) {
			this.shadowImageView.removeFromSuperview();
			this.shadowImageView = null;
		}
	}

	@Override
	public void draw(Context context, Rect rect) {
		Image image = this.getBackgroundImage(BarMetrics.DEFAULT);

		if(image == null) {
			image = this.backgroundImage;
		}

		image.draw(context, rect);
	}

	private void setBarButtonSize(View view, boolean bordered) {
		Rect bounds = this.getBounds();
		Rect frame = view.getFrame();
		frame.size = view.sizeThatFits(new Size(bounds.size.width, bounds.size.height - this.itemEdgeInsets.top - this.itemEdgeInsets.bottom));

		if(bordered) {
			frame.size.width = Math.max(frame.size.width, MIN_BUTTON_WIDTH);
		}

		view.setFrame(frame);
	}

	private Button getBackItemButton(NavigationItem navigationItem) {
		if(navigationItem == null) return null;

		Button button = navigationItem.getBackBarButton();

		if(button == null) {
			button = BarButton.backButton(navigationItem);

			button.addActionTarget(new Control.ActionTarget() {
				public void onControlEvent(Control control, Control.ControlEvent controlEvent, Event event) {
					NavigationBar.this.popNavigationItemAnimated(true);
				}
			}, Control.ControlEvent.TOUCH_UP_INSIDE);
		} else {
			// TODO: Should replace old target action
		}

		this.setBarButtonSize(button, button instanceof BarButton && ((BarButton) button).isBordered());

		return button;
	}

	private View getItemView(final BarButtonItem barButtonItem) {
		if(barButtonItem == null) return null;

		if(barButtonItem.getCustomView() != null) {
			return barButtonItem.getCustomView();
		} else if(barButtonItem.getView() != null) {
			return barButtonItem.getView();
		} else {
			BarButton button = BarButton.button(barButtonItem);
			this.setBarButtonSize(button, button.isBordered());
			barButtonItem.setView(button);

			return button;
		}
	}

	class NavigationItemDelegate implements NavigationItem.Delegate {
		public void titleChanged(NavigationItem navigationItem) {
			updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}

		public void titleViewChanged(NavigationItem navigationItem) {
			updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}

		public void leftBarButtonItemChanged(NavigationItem navigationItem, boolean animated) {
			updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}

		public void rightBarButtonItemChanged(NavigationItem navigationItem, boolean animated) {
			updateItemsWithTransition(Transition.RELOAD, false, null, null);
		}
	}

	public static class Appearance extends mocha.ui.Appearance <NavigationBar> {
		private Method setShadowImage;
		private Method setBackgroundImage;
		private Method setTitleVerticalPositionAdjustment;
		private Method setTitleTextAttributes;
		private Method setTitleAlignment;

		public Appearance() {
			try {
				this.setShadowImage = NavigationBar.class.getMethod("setShadowImage", Image.class);
				this.setBackgroundImage = NavigationBar.class.getMethod("setBackgroundImage", Image.class, BarMetrics.class);
				this.setTitleTextAttributes = NavigationBar.class.getMethod("setTitleTextAttributes", TextAttributes.class);
				this.setTitleVerticalPositionAdjustment = NavigationBar.class.getMethod("setTitleVerticalPositionAdjustment", Float.TYPE, BarMetrics.class);
				this.setTitleAlignment = NavigationBar.class.getMethod("setTitleAlignment", TitleAlignment.class);
			} catch (NoSuchMethodException ignored) { }
		}

		/**
		 * @see NavigationBar#setShadowImage(mocha.graphics.Image)
		 * @param shadowImage Shadow image
		 */
		public void setShadowImage(Image shadowImage) {
			this.store(this.setShadowImage, shadowImage);
		}

		/**
		 * @see NavigationBar#setBackgroundImage(mocha.graphics.Image, BarMetrics)
		 * @param backgroundImage Background image
		 * @param barMetrics Bar metrics
		 */
		public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics) {
			this.store(this.setBackgroundImage, backgroundImage, barMetrics);
		}

		/**
		 * @see NavigationBar#setTitleVerticalPositionAdjustment(float, BarMetrics)
		 * @param adjustment Adjustment
		 * @param barMetrics Bar metrics
		 */
		public void setTitleVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
			this.store(this.setTitleVerticalPositionAdjustment, adjustment, barMetrics);
		}

		/**
		 * @see NavigationBar#setTitleTextAttributes(TextAttributes)
		 * @param titleTextAttributes Text attributes
		 */
		public void setTitleTextAttributes(TextAttributes titleTextAttributes) {
			this.store(this.setTitleTextAttributes, titleTextAttributes);
		}

		/**
		 * @see NavigationBar#setTitleAlignment(mocha.ui.NavigationBar.TitleAlignment)
		 * @param titleAlignment Title alignment
		 */
		public void setTitleAlignment(TitleAlignment titleAlignment) {
			this.store(this.setTitleAlignment, titleAlignment);
		}

	}

}
