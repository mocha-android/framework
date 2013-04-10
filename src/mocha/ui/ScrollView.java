/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.animation.TimingFunction;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class ScrollView extends View implements GestureRecognizer.GestureHandler {

	public interface Listener {
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

	Point contentOffset;
	private Size contentSize;
	private Size adjustedContentSize;
	private boolean dragging;
	boolean decelerating;
	private boolean inSimpleAnimation;
	private IndicatorStyle indicatorStyle;
	private boolean showsHorizontalScrollIndicator;
	private boolean showsVerticalScrollIndicator;
	private EdgeInsets scrollIndicatorInsets;
	private boolean pagingEnabled;
	private Size pageSize;
	private boolean bounces;
	private boolean alwaysBounceHorizontal;
	private boolean alwaysBounceVertical;
	Listener listener;
	Listener.Dragging listenerDragging;
	Listener.Decelerating listenerDecelerating;
	Listener.Animations listenerAnimations;
	ScrollViewPanGestureRecognizer panGestureRecognizer;
	Point maxPoint;
	boolean canScrollVertically;
	boolean canScrollHorizontally;
	private ScrollIndicator horizontalScrollIndicator;
	private ScrollIndicator verticalScrollIndicator;
	private EdgeInsets contentInset;
	private ScrollViewAnimation scrollViewAnimation;

	public ScrollView() { }
	public ScrollView(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.contentOffset = new Point();
		this.contentSize = Size.zero();
		this.adjustedContentSize = new Size();
		this.dragging = false;
		this.decelerating = false;
		this.indicatorStyle = IndicatorStyle.DEFAULT;
		this.showsHorizontalScrollIndicator = true;
		this.showsVerticalScrollIndicator = true;
		this.scrollIndicatorInsets = EdgeInsets.zero();
		this.pagingEnabled = false;
		this.pageSize = new Size();
		this.bounces = true;
		this.alwaysBounceHorizontal = false;
		this.alwaysBounceVertical = false;
		this.listener = null;
		this.contentInset = EdgeInsets.zero();
		this.panGestureRecognizer = new ScrollViewPanGestureRecognizer(this);
		this.setUserInteractionEnabled(true);
		this.addGestureRecognizer(this.panGestureRecognizer);
		this.setClipsToBounds(true);
		this.createScrollIndicators();
		this.updateAlwaysBounce();
	}

	public void setBounces(boolean bounces) {
		if (this.bounces == bounces) {
			return;
		}

		this.bounces = bounces;
		this.updateAlwaysBounce();
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

	public EdgeInsets getContentInset() {
		return this.contentInset.copy();
	}

	public void setContentInset(EdgeInsets contentInset) {
		this.contentInset = contentInset;
	}

	public Size getPageSize() {
		return this.pageSize == null || this.pageSize.equals(Size.zero()) ? this.getBounds().size : this.pageSize.copy();
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

	public boolean bounces() {
		return this.bounces;
	}

	public boolean alwaysBounceHorizontal() {
		return this.alwaysBounceHorizontal;
	}

	public boolean alwaysBounceVertical() {
		return this.alwaysBounceVertical;
	}

	public boolean getScrollEnabled() {
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

		if(timingFunction != null) {
			View.beginAnimations(null, null);
			View.setAnimationDuration(animationDuration);
			View.setTimingFunction(timingFunction);
			View.setAnimationDidStartCallback(new AnimationDidStart() {
				public void animationDidStart(String animationID, Object context) {
					inSimpleAnimation = true;
					if(listenerAnimations != null) {
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

			currentViewAnimation.addAnimation(this, ViewAnimation.Type.CALLBACK_POINT, this.contentOffset, contentOffset, new ViewAnimation.ProcessFrameCallback() {
				public void processFrame(Object value) {
					setContentOffset((Point)value);
				}
			});

			View.commitAnimations();
		} else {
			if(internal || inSimpleAnimation) {
				this.contentOffset = contentOffset.copy();
			} else {
				this.contentOffset = new Point(roundf(contentOffset.x), roundf(contentOffset.y));
			}

			if (!internal && !this.dragging && !this.decelerating && !inSimpleAnimation) {
				this.adjustContentSize(false);
//				this.contentOffset.x = clampf(this.contentOffset.x, 0, this.maxPoint.x);
//				this.contentOffset.y = clampf(this.contentOffset.y, 0, this.maxPoint.y);
			}

			if(View.isInAnimationContext()) {
				currentViewAnimation.addAnimation(this, ViewAnimation.Type.CALLBACK_POINT, this.contentOffset, contentOffset, new ViewAnimation.ProcessFrameCallback() {
					public void processFrame(Object value) {
						setContentOffset((Point)value);
					}
				});
			} else {
				this.updateScrollPositionWithContentOffset();
				this.didScroll(false);
			}
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
		boolean commit;

		Point contentOffset = new Point();

		if (this.pagingEnabled && animated) {
			Size a = this.getPageSize();
			contentOffset.x = roundf(this.contentOffset.x / a.width) * a.width;
			contentOffset.y = roundf(this.contentOffset.y / a.height) * a.height;
			commit = true;
		} else {
			if (this.bounces) {
				contentOffset.x = clampf(this.contentOffset.x, 0, this.maxPoint.x);
				contentOffset.y = clampf(this.contentOffset.y, 0, this.maxPoint.y);
			}

			contentOffset.x = roundf(contentOffset.x);
			contentOffset.y = roundf(contentOffset.y);

			commit = (contentOffset.x != this.contentOffset.x || contentOffset.y != this.contentOffset.y);
		}

		if (commit) {
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

	public void setContentSize(Size contentSize) {
		if (contentSize == null) {
			return;
		}

		this.contentSize = contentSize;
		this.adjustContentSize(false);
		this.updateAlwaysBounce();
	}

	private void adjustContentSize(boolean a) {
		Point b = null;

		if (a) {
			b = new Point();

			if (this.adjustedContentSize.width != 0) {
				b.x = this.contentOffset.x / this.adjustedContentSize.width;
			}
			if (this.adjustedContentSize.height != 0) {
				b.y = this.contentOffset.y / this.adjustedContentSize.height;
			}
		}

		Size size = this.getBounds().size;
		this.adjustedContentSize.width = Math.max(size.width, this.contentSize.width);
		this.adjustedContentSize.height = Math.max(size.height, this.contentSize.height);
		this.maxPoint = new Point(this.adjustedContentSize.width - size.width, this.adjustedContentSize.height - size.height);

		if (a) {
			this.contentOffset = new Point(Math.min(b.x * this.adjustedContentSize.width, this.maxPoint.x), Math.min(b.y * this.adjustedContentSize.height, this.maxPoint.y));
		}

		this.canScrollHorizontally = (size.width < this.adjustedContentSize.width);
		this.canScrollVertically = (size.height < this.adjustedContentSize.height);
	}

	public void setIndicatorStyle(IndicatorStyle indicatorStyle) {
		this.indicatorStyle = indicatorStyle;
		this.horizontalScrollIndicator.setIndicatorStyle(indicatorStyle);
		this.verticalScrollIndicator.setIndicatorStyle(indicatorStyle);
	}

	public void setScrollIndicatorInsets(EdgeInsets edgeInsets) {
		this.scrollIndicatorInsets = edgeInsets;

		if (this.horizontalScrollIndicator.isVisible()) {
			this.updateHorizontalScrollIndicator();
		}

		if (this.verticalScrollIndicator.isVisible()) {
			this.updateVerticalScrollIndicator();
		}
	}

	private void createScrollIndicators() {
		this.horizontalScrollIndicator = new ScrollIndicator();
		this.horizontalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
		this.addSubview(this.horizontalScrollIndicator);


		this.verticalScrollIndicator = new ScrollIndicator();
		this.verticalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
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
		return dragging;
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

	private Point startContentOffset;

	private void panningDidStart(PanGestureRecognizer gestureRecognizer) {
		if(this.scrollViewAnimation != null) {
			this.scrollViewAnimation.cancelAnimations();
			this.scrollViewAnimation = null;
		}

		this.startContentOffset = this.contentOffset.copy();

		if(this.listenerDragging != null) {
			this.listenerDragging.willBeginDragging(this);
		}

		this.dragging = true;
		Size size = this.getBounds().size;

		this.horizontalScrollIndicator.cancelAnimations();
		this.verticalScrollIndicator.cancelAnimations();
		View.beginAnimations(null, null);
		View.setAnimationDuration(100);
		View.setAnimationCurve(AnimationCurve.LINEAR);
		if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator && (this.adjustedContentSize.width > size.width)) {
			this.horizontalScrollIndicator.setVisible(true);
		}
		if (this.canScrollVertically && this.showsVerticalScrollIndicator && (this.adjustedContentSize.height > size.height)) {
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
			x -= ((x > this.maxPoint.x) ? (x - this.maxPoint.x) : ((x < 0) ? x : 0)) / 2;
			y -= ((y > this.maxPoint.y) ? (y - this.maxPoint.y) : ((y < 0) ? y : 0)) / 2;
		} else {
			x = clampf(x, 0, this.maxPoint.x);
			y = clampf(y, 0, this.maxPoint.y);
		}

		this.setContentOffset(new Point(x, y));
	}

	private void panningDidEnd(PanGestureRecognizer gestureRecognizer) {
		if(!this.dragging) return;

		MLog("%s", MGetCurrentMethodName());

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
		MLog("%s", MGetCurrentMethodName());

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

		if(this.listener != null) {
			this.listener.didScroll(this);
		}
	}

}
