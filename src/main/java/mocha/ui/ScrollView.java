/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import mocha.animation.TimingFunction;
import mocha.foundation.OptionalInterface;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class ScrollView extends View implements GestureRecognizer.GestureHandler {

	public interface Listener extends OptionalInterface {
		@Optional
		public void didScroll(ScrollView scrollView);

		public interface Dragging extends Listener {
			public void willBeginDragging(ScrollView scrollView);
			public void willEndDraggingWithVelocityAndTargetContentOffset(ScrollView scrollView, Point velocity, Point targetContentOffset);
			public void didEndDragging(ScrollView scrollView, boolean decelerating);
		}

		public interface Decelerating extends Listener {
			public void willBeginDecelerating(ScrollView scrollView);
			public void didEndDecelerating(ScrollView scrollView);
		}

		public interface Animations extends Listener {
			public void willBeginScrollingAnimation(ScrollView scrollView);
			public void didEndScrollingAnimation(ScrollView scrollView);
		}
	}

	private static float MIN_INDICATOR_LENGTH = 34.0f;
	private static long DEFAULT_TRANSITION_DURATION = 330;
	private static long PAGING_TRANSITION_DURATION = 250;

	public enum IndicatorStyle {
		DEFAULT, BLACK, WHITE
	}

	public enum KeyboardDismissMode {
		// Do not dismiss keyboard
		NONE,

		// Dismiss when user starts to drag
		ON_DRAG,

		// Dismisses keyboard with user touch, can be cancelled if touch moves up
		INTERACTIVE
	}

	final Point contentOffset = Point.zero();
	private final Size contentSize = Size.zero();
	private final Size adjustedContentSize = Size.zero();
	private boolean dragging;
	boolean decelerating;
	private boolean inSimpleAnimation;
	private IndicatorStyle indicatorStyle;
	private boolean showsHorizontalScrollIndicator;
	private boolean showsVerticalScrollIndicator;
	private final EdgeInsets scrollIndicatorInsets = EdgeInsets.zero();
	private boolean pagingEnabled;
	private boolean bounces;
	private boolean alwaysBounceHorizontal;
	private boolean alwaysBounceVertical;
	private boolean hasLaidOut;
	Listener listener;
	Listener.Dragging listenerDragging;
	Listener.Decelerating listenerDecelerating;
	Listener.Animations listenerAnimations;
	ScrollViewPanGestureRecognizer panGestureRecognizer;
	Point maxPoint;
	Point minPoint;
	boolean canScrollVertically;
	boolean canScrollHorizontally;
	private ScrollIndicator horizontalScrollIndicator;
	private ScrollIndicator verticalScrollIndicator;
	private final EdgeInsets contentInset = EdgeInsets.zero();
	private ScrollViewAnimation scrollViewAnimation;
	private KeyboardDismissMode keyboardDismissMode;
	private final Point reuseablePoint = new Point();

	public ScrollView() { }
	public ScrollView(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.dragging = false;
		this.decelerating = false;
		this.indicatorStyle = IndicatorStyle.DEFAULT;
		this.showsHorizontalScrollIndicator = true;
		this.showsVerticalScrollIndicator = true;
		this.pagingEnabled = false;
		this.bounces = true;
		this.alwaysBounceHorizontal = false;
		this.alwaysBounceVertical = false;
		this.listener = null;
		this.panGestureRecognizer = new ScrollViewPanGestureRecognizer(this);
		this.setUserInteractionEnabled(true);
		this.addGestureRecognizer(this.panGestureRecognizer);
		this.setClipsToBounds(true);
		this.createScrollIndicators();
		this.updateAlwaysBounce();
		this.minPoint = Point.zero();
		this.maxPoint = Point.zero();
	}

	public void layoutSubviews() {
		super.layoutSubviews();
		this.hasLaidOut = true;
	}

	public void setBounces(boolean bounces) {
		if (this.bounces == bounces) {
			return;
		}

		this.bounces = bounces;
		this.updateAlwaysBounce();
	}

	public boolean getBounces() {
		return this.bounces;
	}

	public void setAlwaysBounceVertical(boolean bounces) {
		if (this.alwaysBounceVertical == bounces) {
			return;
		}

		this.alwaysBounceVertical = bounces;
		this.updateAlwaysBounce();
	}

	public void setAlwaysBounceHorizontal(boolean bounces) {
		if (this.alwaysBounceHorizontal == bounces) {
			return;
		}

		this.alwaysBounceHorizontal = bounces;
		this.updateAlwaysBounce();
	}

	private void updateAlwaysBounce() {

	}

	public Listener getListener() {
		return listener;
	}

	public void setListener(Listener listener) {
		if(listener != null) {
			this.listener = listener;

			if(listener instanceof Listener.Dragging) {
				this.listenerDragging = (Listener.Dragging)listener;
			} else {
				this.listenerDragging = null;
			}

			if(listener instanceof Listener.Decelerating) {
				this.listenerDecelerating = (Listener.Decelerating)listener;
			} else {
				this.listenerDecelerating = null;
			}

			if(listener instanceof Listener.Animations) {
				this.listenerAnimations = (Listener.Animations)listener;
			} else {
				this.listenerAnimations = null;
			}


		} else {
			this.listener = null;
			this.listenerDragging = null;
			this.listenerDecelerating = null;
			this.listenerAnimations = null;
		}
	}

	public PanGestureRecognizer getPanGestureRecognizer() {
		return this.panGestureRecognizer;
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;
		super.setFrame(frame);

		if(!oldSize.equals(frame.size)) {
			this.adjustContentSize(true);
		}
	}

	private void bringScrollIndicatorsToFront() {
		if (this.horizontalScrollIndicator != null) {
			super.bringSubviewToFront(this.horizontalScrollIndicator);
		}

		if (this.verticalScrollIndicator != null) {
			super.bringSubviewToFront(this.verticalScrollIndicator);
		}
	}

	public void addSubview(View subview) {
		super.addSubview(subview);
		this.bringScrollIndicatorsToFront();
	}

	public void bringSubviewToFront(View subview) {
		super.bringSubviewToFront(subview);
		this.bringScrollIndicatorsToFront();
	}

	public void insertSubview(View subview, int index) {
		super.insertSubview(subview, index);
		this.bringScrollIndicatorsToFront();
	}

	public KeyboardDismissMode getKeyboardDismissMode() {
		return this.keyboardDismissMode;
	}

	public void setKeyboardDismissMode(KeyboardDismissMode keyboardDismissMode) {
		// TODO
		MWarn("KeyboardDismissMode is not currently implemented.");
		this.keyboardDismissMode = keyboardDismissMode;
	}

	public EdgeInsets getContentInset() {
		return this.contentInset.copy();
	}

	public float getContentInsetTop() {
		return this.contentInset.top;
	}

	public float getContentInsetLeft() {
		return this.contentInset.left;
	}

	public float getContentInsetBottom() {
		return this.contentInset.bottom;
	}

	public float getContentInsetRight() {
		return this.contentInset.right;
	}

	public void setContentInset(EdgeInsets contentInset) {
		if(contentInset == null) {
			contentInset = EdgeInsets.zero();
		} else {
			contentInset = contentInset.copy();
		}

		if(!this.contentInset.equals(contentInset)) {
			ViewAnimation.Type type = ViewAnimation.Type.CALLBACK_EDGE_INSETS;

			if(View.isInAnimationContext()) {
				currentViewAnimation.addAnimation(this, type, this.contentInset, contentInset, new ViewAnimation.ProcessFrameCallback() {
					public void processFrame(Object value) {
						setContentInset((EdgeInsets)value);
					}
				});

				return;
			}

			if(!this.animationIsSetting && this.animations[type.value] != null) {
				if(this.changeEndValueForAnimationType(type, contentInset.copy())) {
					return;
				}
			}

			EdgeInsets previousContentInset = this.contentInset.copy();
			this.contentInset.set(contentInset);
			this.minPoint = new Point(-contentInset.left, -contentInset.top);

			Size size = this.getBounds().size;

			this.adjustedContentSize.width -= previousContentInset.right;
			this.adjustedContentSize.width += this.contentInset.right;

			this.adjustedContentSize.height -= previousContentInset.bottom;
			this.adjustedContentSize.height += this.contentInset.bottom;
			this.maxPoint = new Point(this.adjustedContentSize.width - size.width, this.adjustedContentSize.height - size.height);

			if(!this.hasLaidOut) {
				this.setContentOffset(this.minPoint);
			} else {
				Point contentOffset = this.contentOffset.copy();

				if(this.contentInset.left < previousContentInset.left) {
					contentOffset.x += previousContentInset.left - this.contentInset.left;
				}

				if(this.contentInset.top < previousContentInset.top) {
					contentOffset.y += previousContentInset.top - this.contentInset.top;
				}

				if(!contentOffset.equals(this.contentOffset)) {
					this.setContentOffset(contentOffset);
				}
			}

		}
	}

	public boolean isPagingEnabled() {
		return this.pagingEnabled;
	}

	public void setPagingEnabled(boolean pagingEnabled) {
		this.pagingEnabled = pagingEnabled;
	}

	public boolean showsHorizontalScrollIndicator() {
		return this.showsHorizontalScrollIndicator;
	}

	public void setShowsHorizontalScrollIndicator(boolean showsHorizontalScrollIndicator) {
		this.showsHorizontalScrollIndicator = showsHorizontalScrollIndicator;
	}

	public boolean showsVerticalScrollIndicator() {
		return this.showsVerticalScrollIndicator;
	}

	public void setShowsVerticalScrollIndicator(boolean showsVerticalScrollIndicator) {
		this.showsVerticalScrollIndicator = showsVerticalScrollIndicator;
	}

	public Point getContentOffset() {
		return this.contentOffset.copy();
	}

	public float getContentOffsetX() {
		return this.contentOffset.x;
	}

	public float getContentOffsetY() {
		return this.contentOffset.y;
	}

	public boolean bounces() {
		return this.bounces;
	}

	public boolean alwaysBounceHorizontal() {
		return this.alwaysBounceHorizontal;
	}

	public boolean alwaysBounceVertical() {
		return this.alwaysBounceVertical;
	}

	public boolean isScrollEnabled() {
		return this.panGestureRecognizer.isEnabled();
	}

	public void setScrollEnabled(boolean scrollEnabled) {
		this.panGestureRecognizer.setEnabled(scrollEnabled);
	}

	public boolean isDirectionalLockEnabled() {
		return this.panGestureRecognizer.isDirectionalLockEnabled();
	}

	public void setDirectionalLockEnabled(boolean directionalLockEnabled) {
		this.panGestureRecognizer.setDirectionalLockEnabled(directionalLockEnabled);
	}

	public void scrollRectToVisible(Rect rect, boolean animated) {
		Rect contentRect = new Rect(0.0f, 0.0f, this.contentSize.width, this.contentSize.height);
		Rect visibleRect = new Rect(this.getBounds());
		Rect goalRect = rect.intersection(contentRect);

		if (goalRect != null && !visibleRect.contains(goalRect)) {
			goalRect.size.width = Math.min(goalRect.size.width, visibleRect.size.width);
			goalRect.size.height = Math.min(goalRect.size.height, visibleRect.size.height);

			Point offset = this.contentOffset.copy();

			if (goalRect.maxY() > visibleRect.maxY()) {
				offset.y += goalRect.maxY() - visibleRect.maxY();
			} else if (goalRect.minY() < visibleRect.minY()) {
				offset.y += goalRect.minY() - visibleRect.minY();
			}

			if (goalRect.maxX() > visibleRect.maxX()) {
				offset.x += goalRect.maxX() - visibleRect.maxX();
			} else if (goalRect.minX() < visibleRect.minX()) {
				offset.x += goalRect.minX() - visibleRect.minX();
			}

			this.setContentOffset(offset, animated);
		}
	}

	public void setContentOffset(Point contentOffset) {
		this.setContentOffset(contentOffset, null, 0, false);
	}

	public void setContentOffset(Point contentOffset, boolean animated) {
		this.setContentOffset(contentOffset, animated ? TimingFunction.LINEAR : null, DEFAULT_TRANSITION_DURATION, false);
	}

	void setContentOffset(Point contentOffset, TimingFunction timingFunction, long animationDuration, boolean internal) {
		if (contentOffset == null || contentOffset.equals(this.contentOffset)) {
			return;
		}

		if(this.scrollViewAnimation != null && !internal && !inSimpleAnimation) {
			this.scrollViewAnimation.cancelAnimations();
			this.scrollViewAnimation = null;
		}

		final ViewAnimation.Type type = ViewAnimation.Type.CALLBACK_POINT;

		if(!this.animationIsSetting && this.animations[type.value] != null) {
			if(this.changeEndValueForAnimationType(type, contentOffset.copy())) {
				return;
			}
		}

		if(timingFunction != null) {
			View.beginAnimations(null, null);
			View.setAnimationDuration(animationDuration);
			View.setTimingFunction(timingFunction);
			View.setAnimationWillStartCallback(new AnimationWillStart() {
				public void animationWillStart(String animationID, Object context) {
					inSimpleAnimation = true;
					if (listenerAnimations != null) {
						listenerAnimations.willBeginScrollingAnimation(ScrollView.this);
					}
				}
			});
			View.setAnimationDidStopCallback(new AnimationDidStop() {
				public void animationDidStop(String animationID, boolean finished, Object context) {
					if (finished) {
						inSimpleAnimation = false;
						didScroll(true);
						hideScrollIndicators();
					}
				}
			});

			currentViewAnimation.addAnimation(this, type, this.contentOffset.copy(), contentOffset.copy(), new ViewAnimation.ProcessFrameCallback() {
				public void processFrame(Object value) {
					setContentOffset((Point)value);
				}
			});

			View.commitAnimations();
		} else if(View.isInAnimationContext()) {
			currentViewAnimation.addAnimation(this, type, this.contentOffset.copy(), contentOffset.copy(), new ViewAnimation.ProcessFrameCallback() {
				public void processFrame(Object value) {
					setContentOffset((Point)value);
				}
			});
		} else {
			if(internal || inSimpleAnimation) {
				this.contentOffset.set(contentOffset);
			} else {
				this.contentOffset.x = roundf(contentOffset.x);
				this.contentOffset.y = roundf(contentOffset.y);
			}

			if (!internal && !this.dragging && !this.decelerating && !inSimpleAnimation) {
				this.adjustContentSize(false);
			}

			this.updateScrollPositionWithContentOffset();
			this.didScroll(false);
		}

		if (timingFunction == null) {
			if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator) {
				this.updateHorizontalScrollIndicator();
			}

			if (this.canScrollVertically && this.showsVerticalScrollIndicator) {
				this.updateVerticalScrollIndicator();
			}
		}
	}

	private void updateScrollPositionWithContentOffset() {
		Rect bounds = this.getBounds();
		bounds.origin.x = this.contentOffset.x;
		bounds.origin.y = this.contentOffset.y;
		this.setBounds(bounds);
	}

	void snapContentOffsetToBounds(boolean animated) {
		Point contentOffset = this.contentOffset.copy();

		if (this.pagingEnabled && animated) {
			float width = this.getBoundsWidth();
			float height = this.getBoundsHeight();

			contentOffset.x = roundf(contentOffset.x / width) * width;
			contentOffset.y = roundf(contentOffset.y / height) * height;
		} else {
			if (this.bounces) {
				contentOffset.x = clampf(contentOffset.x, this.minPoint.x, this.maxPoint.x);
				contentOffset.y = clampf(contentOffset.y, this.minPoint.y, this.maxPoint.y);

				Rect frame = this.getFrame();

				if(this.contentOffset.x > this.minPoint.x && (this.contentSize.width + this.contentInset.right) < frame.size.width) {
					contentOffset.x = this.minPoint.x;
				}

				if(this.contentOffset.y > this.minPoint.y && (this.contentSize.height + this.contentInset.bottom) < frame.size.height) {
					contentOffset.y = this.minPoint.y;
				}
			}

			contentOffset.x = roundf(contentOffset.x);
			contentOffset.y = roundf(contentOffset.y);
		}

		if ((contentOffset.x != this.contentOffset.x || contentOffset.y != this.contentOffset.y)) {
			if(pagingEnabled && animated) {
				this.setContentOffset(contentOffset, TimingFunction.EASE_OUT, PAGING_TRANSITION_DURATION, false);
			} else {
				this.setContentOffset(contentOffset, new TimingFunction.CubicBezierCurveTimingFunction(0.390f, 0.575f, 0.565f, 1.000f), 400, false);
			}
		}
	}

	public Size getContentSize() {
		return this.contentSize.copy();
	}

	public float getContentSizeWidth() {
		return this.contentSize.width;
	}

	public float getContentSizeHeight() {
		return this.contentSize.height;
	}

	public void setContentSize(Size contentSize) {
		this.contentSize.set(contentSize);
		this.adjustContentSize(false);
		this.updateAlwaysBounce();
	}

	private void adjustContentSize(boolean adjustOffset) {
		if (adjustOffset) {
			if (this.adjustedContentSize.width != 0) {
				this.reuseablePoint.x = this.contentOffset.x / this.adjustedContentSize.width;
			} else {
				this.reuseablePoint.x = 0.0f;
			}

			if (this.adjustedContentSize.height != 0) {
				this.reuseablePoint.y = this.contentOffset.y / this.adjustedContentSize.height;
			} else {
				this.reuseablePoint.y = 0.0f;
			}
		}

		float boundsWidth = this.getBoundsWidth();
		float boundsHeight = this.getBoundsHeight();

		this.adjustedContentSize.width = Math.max(boundsWidth, this.contentSize.width + this.contentInset.right);
		this.adjustedContentSize.height = Math.max(boundsHeight, this.contentSize.height + this.contentInset.bottom);
		this.maxPoint = new Point(this.adjustedContentSize.width - boundsWidth, this.adjustedContentSize.height - boundsHeight);

		if (adjustOffset) {
			this.contentOffset.x = Math.min(this.reuseablePoint.x * this.adjustedContentSize.width, this.maxPoint.x);
			this.contentOffset.y = Math.min(this.reuseablePoint.y * this.adjustedContentSize.height, this.maxPoint.y);
		}

		this.canScrollHorizontally = (boundsWidth < this.adjustedContentSize.width);
		this.canScrollVertically = (boundsHeight < this.adjustedContentSize.height);
	}

	public void setIndicatorStyle(IndicatorStyle indicatorStyle) {
		this.indicatorStyle = indicatorStyle;
		this.horizontalScrollIndicator.setIndicatorStyle(indicatorStyle);
		this.verticalScrollIndicator.setIndicatorStyle(indicatorStyle);
	}

	public void setScrollIndicatorInsets(EdgeInsets edgeInsets) {
		this.scrollIndicatorInsets.set(edgeInsets);

		if (this.horizontalScrollIndicator.isVisible()) {
			this.updateHorizontalScrollIndicator();
		}

		if (this.verticalScrollIndicator.isVisible()) {
			this.updateVerticalScrollIndicator();
		}
	}

	public EdgeInsets getScrollIndicatorInsets() {
		return this.scrollIndicatorInsets.copy();
	}

	private void createScrollIndicators() {
		this.horizontalScrollIndicator = new ScrollIndicator();
		this.horizontalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
		this.horizontalScrollIndicator.setVisible(false);
		this.addSubview(this.horizontalScrollIndicator);


		this.verticalScrollIndicator = new ScrollIndicator();
		this.verticalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
		this.verticalScrollIndicator.setVisible(false);
		this.addSubview(this.verticalScrollIndicator);
	}

	private void updateHorizontalScrollIndicator() {
		Rect bounds = this.getBounds();
		Size size = this.getBounds().size;

		float minX = this.scrollIndicatorInsets.left + 1;
		float maxX = size.width - this.scrollIndicatorInsets.right - 1;

		if (this.canScrollVertically && this.showsVerticalScrollIndicator) {
			maxX -= this.verticalScrollIndicator.getThickness() - 1.0f;
		}

		float adjustedWidth = maxX - minX;
		float width = Math.max(MIN_INDICATOR_LENGTH, roundf((size.width / this.adjustedContentSize.width) * adjustedWidth));
		float y = size.height - this.horizontalScrollIndicator.getThickness() - this.scrollIndicatorInsets.bottom - 1;
		float x;

		if (this.contentOffset.x < 0) {
			width = roundf(Math.max(width + this.contentOffset.x, this.horizontalScrollIndicator.getThickness()));
			x = minX;
		} else {
			if (this.contentOffset.x > this.maxPoint.x) {
				width = roundf(Math.max(width + this.adjustedContentSize.width - size.width - this.contentOffset.x, this.horizontalScrollIndicator.getThickness()));
				x = maxX - width;
			} else {
				float b = (this.contentOffset.x / (this.adjustedContentSize.width - size.width));
				x = clampf(roundf(b * (adjustedWidth - width) + this.scrollIndicatorInsets.left), minX, maxX - width);
			}
		}

		this.horizontalScrollIndicator.setFrame(new Rect(bounds.origin.x + x, y, width, this.horizontalScrollIndicator.getThickness()));
	}

	private void updateVerticalScrollIndicator() {
		Rect bounds = this.getBounds();
		Size size = bounds.size;

		float minY = this.scrollIndicatorInsets.top + 1;
		float maxY = size.height - this.scrollIndicatorInsets.bottom - 1;

		if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator) {
			maxY -= this.horizontalScrollIndicator.getThickness() - 1;
		}

		float adjustedHeight = maxY - minY;
		float height = Math.max(MIN_INDICATOR_LENGTH, roundf((size.height / this.adjustedContentSize.height) * adjustedHeight));
		float x = size.width - this.verticalScrollIndicator.getThickness() - this.scrollIndicatorInsets.right - 1;
		float y;

		if (this.contentOffset.y < 0) {
			height = roundf(Math.max(height + this.contentOffset.y, this.verticalScrollIndicator.getThickness()));
			y = minY;
		} else {
			if (this.contentOffset.y > this.maxPoint.y) {
				height = roundf(Math.max(height + this.adjustedContentSize.height - size.height - this.contentOffset.y, this.verticalScrollIndicator.getThickness()));
				y = maxY - height;
			} else {
				float c = (this.contentOffset.y / (this.adjustedContentSize.height - size.height));
				y = clampf(roundf(c * (adjustedHeight - height) + this.scrollIndicatorInsets.top), minY, maxY - height);
			}
		}

		this.verticalScrollIndicator.setFrame(new Rect(x, bounds.origin.y + y, this.verticalScrollIndicator.getThickness(), height));
	}

	public void flashScrollIndicators() {
		final boolean showHorizontal = this.canScrollHorizontally && this.showsHorizontalScrollIndicator && (this.adjustedContentSize.width > this.getBounds().size.width);
		final boolean showVertical = this.canScrollVertically && this.showsVerticalScrollIndicator && (this.adjustedContentSize.height > this.getBounds().size.height);

		if (showHorizontal) {
			this.updateHorizontalScrollIndicator();
		}

		if (showVertical) {
			this.updateVerticalScrollIndicator();
		}

		this.horizontalScrollIndicator.cancelAnimations();
		this.verticalScrollIndicator.cancelAnimations();

		View.animateWithDuration(100, 100, new Animations() {
			public void performAnimatedChanges() {
				View.setAnimationCurve(AnimationCurve.LINEAR);
				if(showHorizontal) horizontalScrollIndicator.setVisible(true);
				if(showVertical) verticalScrollIndicator.setVisible(true);
			}
		}, new AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				if(finished) {
					performAfterDelay(0, new Runnable() {
						public void run() {
							hideScrollIndicators(true);
						}
					});
				}
			}
		});
	}

	void hideScrollIndicators() {
		hideScrollIndicators(false);
	}

	void hideScrollIndicators(boolean fromFlash) {
		if(!this.horizontalScrollIndicator.isVisible() && !this.verticalScrollIndicator.isVisible()) return;

		horizontalScrollIndicator.cancelAnimations();
		verticalScrollIndicator.cancelAnimations();

		View.beginAnimations(null, null);
		View.setAnimationCurve(AnimationCurve.EASE_OUT);
		View.setAnimationDelay(fromFlash ? 600 : 300);
		this.horizontalScrollIndicator.setVisible(false);
		this.verticalScrollIndicator.setVisible(false);
		View.commitAnimations();
	}

	public boolean isDragging() {
		return this.dragging;
	}

	public boolean isDecelerating() {
		return this.decelerating;
	}

	public void handleGesture(GestureRecognizer gestureRecognizer) {
		if(gestureRecognizer != this.panGestureRecognizer) return;

		switch (gestureRecognizer.getState()) {
			case BEGAN:
				this.panningDidStart(this.panGestureRecognizer);
				break;
			case CHANGED:
				this.panningDidChange(this.panGestureRecognizer);
				break;
			case ENDED:
				this.panningDidEnd(this.panGestureRecognizer);
				break;
			case CANCELLED:
				this.panningDidCancel(this.panGestureRecognizer);
				break;
		}
	}

	private final Point startContentOffset = Point.zero();

	private void panningDidStart(PanGestureRecognizer gestureRecognizer) {
		if(this.scrollViewAnimation != null) {
			this.scrollViewAnimation.cancelAnimations();
			this.scrollViewAnimation = null;
		}

		if(this.listenerDragging != null) {
			this.listenerDragging.willBeginDragging(this);
		}

		this.startContentOffset.set(this.contentOffset);

		this.dragging = true;

		this.horizontalScrollIndicator.cancelAnimations();
		this.verticalScrollIndicator.cancelAnimations();
		View.beginAnimations(null, null);
		View.setAnimationDuration(100);
		View.setAnimationCurve(AnimationCurve.LINEAR);
		if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator && (this.adjustedContentSize.width > this.getBoundsWidth())) {
			this.horizontalScrollIndicator.setVisible(true);
		}
		if (this.canScrollVertically && this.showsVerticalScrollIndicator && (this.adjustedContentSize.height > this.getBoundsHeight())) {
			this.verticalScrollIndicator.setVisible(true);
		}
		View.commitAnimations();
	}

	private void panningDidChange(PanGestureRecognizer gestureRecognizer) {
		boolean canScrollHorizontally = (this.canScrollHorizontally || (this.bounces && this.alwaysBounceHorizontal));
		boolean canScrollVertically = (this.canScrollVertically || (this.bounces && this.alwaysBounceVertical));

		Point translation = gestureRecognizer.translationInView(this);
		float x = canScrollHorizontally ? (this.startContentOffset.x - translation.x) : this.contentOffset.x;
		float y = canScrollVertically ? (this.startContentOffset.y - translation.y) : this.contentOffset.y;

		if (this.bounces) {
			x -= ((x > this.maxPoint.x) ? (x - this.maxPoint.x) : ((x < this.minPoint.x) ? x - this.minPoint.x : 0)) / 2;
			y -= ((y > this.maxPoint.y) ? (y - this.maxPoint.y) : ((y < this.minPoint.y) ? y - this.minPoint.y : 0)) / 2;
		} else {
			x = clampf(x, this.minPoint.x, this.maxPoint.x);
			y = clampf(y, this.minPoint.y, this.maxPoint.y);
		}

		this.reuseablePoint.x = x;
		this.reuseablePoint.y = y;
		this.setContentOffset(this.reuseablePoint);
	}

	private void panningDidEnd(PanGestureRecognizer gestureRecognizer) {
		if(!this.dragging) return;

		this.dragging = false;

		if(this.scrollViewAnimation != null) {
			this.scrollViewAnimation.cancelAnimations();
		}

		this.scrollViewAnimation = new ScrollViewAnimation(this);
		this.scrollViewAnimation.startDecelerationAnimation();

		if(this.listenerDragging != null) {
			this.listenerDragging.didEndDragging(this, this.decelerating);
		}

		if (!this.decelerating) {
			this.snapContentOffsetToBounds(true);
			this.hideScrollIndicators();
		}
	}

	private void panningDidCancel(PanGestureRecognizer gestureRecognizer) {
		if (!this.dragging) {
			return;
		}

		this.dragging = false;
		this.snapContentOffsetToBounds(true);

		if(this.listenerDragging != null) {
			this.listenerDragging.didEndDragging(this, false);
		}

		this.hideScrollIndicators();
	}

	private void didScroll(boolean fromAnimation) {
		if(fromAnimation) {
			this.contentOffset.x = roundf(this.contentOffset.x);
			this.contentOffset.y = roundf(this.contentOffset.y);
			this.updateScrollPositionWithContentOffset();

			if(this.listenerAnimations != null) {
				this.listenerAnimations.didEndScrollingAnimation(this);
			}
		}

		this.didScroll();

		if(this.listener != null) {
			this.listener.didScroll(this);
		}
	}

	/**
	 * Convenience for subclasses, called before listener is notified
	 */
	protected void didScroll() {

	}

	/**
	 * Convenience for subclasses, called before listener is notified
	 *
	 * @param velocity Scroll velocity
	 * @param targetContentOffset Target content offset
	 * @return true to allow the listener to be called, false to prevent it from being called
	 */
	protected boolean willEndDraggingWithVelocityAndTargetContentOffset(Point velocity, Point targetContentOffset) {
		return true;
	}

}
