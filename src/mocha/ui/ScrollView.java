/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.animation.ViewAnimator;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class ScrollView extends View implements GestureRecognizer.GestureHandler {

	public interface Listener {
		public void scrollViewWillBeginDragging(ScrollView scrollView);
		public void scrollViewWillBeginScrollingAnimation(ScrollView scrollView);
		public void scrollViewDidEndScrollingAnimation(ScrollView scrollView);
		public void scrollViewDidScroll(ScrollView scrollView);
		public void scrollViewWillEndDraggingWithVelocityAndTargetContentOffset(ScrollView scrollView, Point velocity, Point targetContentOffset);
		public void scrollViewDidEndDragging(ScrollView scrollView, boolean decelerating);
		public void scrollViewWillBeginDecelerating(ScrollView scrollView);
		public void scrollViewDidEndDecelerating(ScrollView scrollView);
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
	Listener delegate;
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
		this.delegate = null;
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

	public boolean isShowsHorizontalScrollIndicator() {
		return this.showsHorizontalScrollIndicator;
	}

	public void setShowsHorizontalScrollIndicator(boolean showsHorizontalScrollIndicator) {
		this.showsHorizontalScrollIndicator = showsHorizontalScrollIndicator;
	}

	public boolean isShowsVerticalScrollIndicator() {
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
		this.setContentOffset(contentOffset, false, false);
	}

	public void setContentOffset(Point contentOffset, boolean animated) {
		this.setContentOffset(contentOffset, animated, false);
	}

	void setContentOffset(Point contentOffset, boolean animated, boolean internal) {
		if (contentOffset == null || contentOffset.equals(this.contentOffset)) {
			return;
		}

		if(this.scrollViewAnimation != null && !internal && !inSimpleAnimation) {
			this.scrollViewAnimation.cancelAnimations();
			this.scrollViewAnimation = null;
		}

		if(animated) {
			View.beginAnimations(null, null);
			View.setAnimationDuration(this.pagingEnabled ? PAGING_TRANSITION_DURATION : DEFAULT_TRANSITION_DURATION);
			View.setAnimationCurve(AnimationCurve.EASE_IN);
			View.setAnimationDidStartCallback(new AnimationDidStart() {
				public void animationDidStart(String animationID, Object context) {
					MLog("did start");
					inSimpleAnimation = true;
					if(delegate != null) {
						delegate.scrollViewWillBeginScrollingAnimation(ScrollView.this);
					}
				}
			});
			View.setAnimationDidStopCallback(new AnimationDidStop() {
				public void animationDidStop(String animationID, boolean finished, Object context) {
					if (finished) {
						MLog("did end");
						inSimpleAnimation = false;
						didScroll(true);
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
				this.contentOffset.x = clampf(this.contentOffset.x, 0, this.maxPoint.x);
				this.contentOffset.y = clampf(this.contentOffset.y, 0, this.maxPoint.y);
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

		if (!animated) {
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
			this.setContentOffset(contentOffset, animated);
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
		this.horizontalScrollIndicator = new ScrollIndicator(ScrollIndicator.Type.HORIZONTAL);
		this.horizontalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
		this.addSubview(this.horizontalScrollIndicator);


		this.verticalScrollIndicator = new ScrollIndicator(ScrollIndicator.Type.VERTICAL);
		this.verticalScrollIndicator.setIndicatorStyle(this.indicatorStyle);
		this.addSubview(this.verticalScrollIndicator);
	}

	private void updateHorizontalScrollIndicator() {
		Rect bounds = this.getBounds();
		Size size = this.getBounds().size;

		float minX = this.scrollIndicatorInsets.left + 1;
		float maxX = size.width - this.scrollIndicatorInsets.right - 1;

		if (this.canScrollVertically && this.showsVerticalScrollIndicator) {
			maxX -= ScrollIndicator.THICKNESS - 1.0f;
		}

		float adjustedWidth = maxX - minX;
		float width = Math.max(MIN_INDICATOR_LENGTH, roundf((size.width / this.adjustedContentSize.width) * adjustedWidth));
		float y = size.height - ScrollIndicator.THICKNESS - this.scrollIndicatorInsets.bottom - 1;
		float x;

		if (this.contentOffset.x < 0) {
			width = roundf(Math.max(width + this.contentOffset.x, ScrollIndicator.THICKNESS));
			x = minX;
		} else {
			if (this.contentOffset.x > this.maxPoint.x) {
				width = roundf(Math.max(width + this.adjustedContentSize.width - size.width - this.contentOffset.x, ScrollIndicator.THICKNESS));
				x = maxX - width;
			} else {
				float b = (this.contentOffset.x / (this.adjustedContentSize.width - size.width));
				x = clampf(roundf(b * (adjustedWidth - width) + this.scrollIndicatorInsets.left), minX, maxX - width);
			}
		}

		this.horizontalScrollIndicator.setFrame(new Rect(bounds.origin.x + x, y, width, ScrollIndicator.THICKNESS));
	}

	private void updateVerticalScrollIndicator() {
		Rect bounds = this.getBounds();
		Size size = bounds.size;

		float minY = this.scrollIndicatorInsets.top + 1;
		float maxY = size.height - this.scrollIndicatorInsets.bottom - 1;

		if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator) {
			maxY -= ScrollIndicator.THICKNESS - 1;
		}

		float adjustedHeight = maxY - minY;
		float height = Math.max(MIN_INDICATOR_LENGTH, roundf((size.height / this.adjustedContentSize.height) * adjustedHeight));
		float x = size.width - ScrollIndicator.THICKNESS - this.scrollIndicatorInsets.right - 1;
		float y;

		if (this.contentOffset.y < 0) {
			height = roundf(Math.max(height + this.contentOffset.y, ScrollIndicator.THICKNESS));
			y = minY;
		} else {
			if (this.contentOffset.y > this.maxPoint.y) {
				height = roundf(Math.max(height + this.adjustedContentSize.height - size.height - this.contentOffset.y, ScrollIndicator.THICKNESS));
				y = maxY - height;
			} else {
				float c = (this.contentOffset.y / (this.adjustedContentSize.height - size.height));
				y = clampf(roundf(c * (adjustedHeight - height) + this.scrollIndicatorInsets.top), minY, maxY - height);
			}
		}

		this.verticalScrollIndicator.setFrame(new Rect(x, bounds.origin.y + y, ScrollIndicator.THICKNESS, height));
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

		View.animateWithDuration(100, new Animations() {
			public void performAnimatedChanges() {
				if(showHorizontal) horizontalScrollIndicator.setVisible(true);
				if(showVertical) horizontalScrollIndicator.setVisible(true);
			}
		}, new AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				View.beginAnimations(null, null);
				View.setAnimationDelay(500);
				View.setAnimationCurve(AnimationCurve.EASE_OUT);
				if(showHorizontal) horizontalScrollIndicator.setVisible(false);
				if(showVertical) horizontalScrollIndicator.setVisible(false);
				View.commitAnimations();
			}
		});
	}

	void hideScrollIndicators() {
		View.beginAnimations(null, null);
		View.setAnimationCurve(AnimationCurve.EASE_OUT);
		this.horizontalScrollIndicator.setVisible(false);
		this.verticalScrollIndicator.setVisible(false);
		View.commitAnimations();
	}

	private void showHorizontalScrollIndicator() {
		this.horizontalScrollIndicator.setVisible(true);
	}

	private void showVerticalScrollIndicator() {
		this.verticalScrollIndicator.setVisible(true);
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

		if(this.delegate != null) {
			this.delegate.scrollViewWillBeginDragging(this);
		}

		this.dragging = true;
		Size size = this.getBounds().size;

		View.beginAnimations(null, null);
		View.setAnimationDuration(100);
		if (this.canScrollHorizontally && this.showsHorizontalScrollIndicator && (this.adjustedContentSize.width > size.width)) {
			this.showHorizontalScrollIndicator();
		}
		if (this.canScrollVertically && this.showsVerticalScrollIndicator && (this.adjustedContentSize.height > size.height)) {
			this.showVerticalScrollIndicator();
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

		if(this.delegate != null) {
			this.delegate.scrollViewDidEndDragging(this, this.decelerating);
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

		if(this.delegate != null) {
			this.delegate.scrollViewDidEndDragging(this, false);
		}

		this.hideScrollIndicators();
	}

	private void didScroll(boolean fromAnimation) {
		if(fromAnimation) {
			this.contentOffset.x = roundf(this.contentOffset.x);
			this.contentOffset.y = roundf(this.contentOffset.y);
			this.updateScrollPositionWithContentOffset();

			if(this.delegate != null) {
				this.delegate.scrollViewDidEndScrollingAnimation(this);
			}
		}

		if(this.delegate != null) {
			this.delegate.scrollViewDidScroll(this);
		}
	}

}