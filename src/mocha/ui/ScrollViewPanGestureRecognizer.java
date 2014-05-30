/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.List;

class ScrollViewPanGestureRecognizer extends PanGestureRecognizer {

	private enum ScrollDirection {
		VERTICAL, HORIZONTAL
	}

	// TODO: Add support for nested scroll views

	private boolean directionalLockEnabled;
	private ScrollDirection scrollDirection;
	private ScrollDirection lastScrollDirection;
	private final Point startLocation;
	private final Point lastLocation;

	ScrollViewPanGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);


		this.lastLocation = Point.zero();
		this.startLocation = Point.zero();
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		ScrollView scrollView;

		Touch touch = this.findTouch(event);

		// If our scroll view is decelerating, begin immediately to halt touching
		if(!this.tracking && !this.panning && (scrollView = this.getScrollView()) != null && scrollView.decelerating) {
			if(touch != null) {
				this.startTouchPosition = touch.location.copy();
				this.tracking = true;
				this.lastTouchPosition = touch.location.copy();
				this.addTrackingDataPoint(this.lastTouchPosition, event);

				Point location = touch.location;
				this.translation = new Point(location.x - this.startTouchPosition.x, location.y - this.startTouchPosition.y);
				this.panning = true;
				this.scrollDirection = this.lastScrollDirection;
				this.setState(State.BEGAN, true);

				return;
			}
		}

		if(touch != null) {
			this.startLocation.set(touch.location);
			this.lastLocation.set(this.startLocation);
		}

		super.touchesBegan(touches, event);
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if(this.directionalLockEnabled && this.scrollDirection == null) {
			Touch touch = this.findTouch(event);

			if(touch != null) {
				this.lastLocation.set(touch.location);
			}
		}

		super.touchesMoved(touches, event);
	}

	protected void setState(State state) {
		this.setState(state, false);
	}

	protected void setState(State state, boolean skipDirectionCheck) {
		if(!skipDirectionCheck && state == State.BEGAN && this.directionalLockEnabled) {
			Point abs = new Point(Math.abs(this.startLocation.x - this.lastLocation.x), Math.abs(this.startLocation.y - this.lastLocation.y));

			if(abs.x > abs.y) {
				this.scrollDirection = ScrollDirection.HORIZONTAL;
			} else {
				this.scrollDirection = ScrollDirection.VERTICAL;
			}

			ScrollView scrollView = this.getScrollView();

			if(scrollView != null) {
				if(this.scrollDirection == ScrollDirection.HORIZONTAL) {
					if(!scrollView.canScrollHorizontally && !scrollView.alwaysBounceHorizontal()) {
						state = State.FAILED;
					}
				} else if(this.scrollDirection == ScrollDirection.VERTICAL) {
					if(!scrollView.canScrollVertically && !scrollView.alwaysBounceVertical()) {
						state = State.FAILED;
					}
				}
			}
		}

		if(state == State.BEGAN && this.areAnyEnclosingScrollViewsDecelerating()) {
			state = State.FAILED;
		}

		super.setState(state);
	}

	public Point translationInView(View view) {
		Point point = super.translationInView(view);

		if(this.directionalLockEnabled) {
			if(this.scrollDirection == ScrollDirection.HORIZONTAL) {
				point.y = 0.0f;
			} else if(this.scrollDirection == ScrollDirection.VERTICAL) {
				point.x = 0.0f;
			}
		}

		return point;
	}

	public Point velocityInView(View view) {
		Point point = super.velocityInView(view);

		if(this.directionalLockEnabled) {
			if(this.scrollDirection == ScrollDirection.HORIZONTAL) {
				point.y = 0.0f;
			} else if(this.scrollDirection == ScrollDirection.VERTICAL) {
				point.x = 0.0f;
			}
		}

		return point;
	}

	protected void reset() {
		this.lastScrollDirection = this.scrollDirection;
		this.scrollDirection = null;
		super.reset();
	}

	boolean isDirectionalLockEnabled() {
		return directionalLockEnabled;
	}

	void setDirectionalLockEnabled(boolean directionalLockEnabled) {
		this.directionalLockEnabled = directionalLockEnabled;
	}

	private ScrollView getScrollView() {
		if(this.getView() instanceof ScrollView) {
			return (ScrollView) this.getView();
		} else {
			return null;
		}
	}

	private ScrollView getEnclosingScrollView() {
		ScrollView scrollView = this.getScrollView();
		if(scrollView == null) return null;

		View superview = scrollView;

		while((superview = superview.getSuperview()) != null) {
			if(superview instanceof ScrollView) {
				return (ScrollView)superview;
			}
		}

		return null;
	}

	private boolean areAnyEnclosingScrollViewsScrolling() {
		ScrollView enclosingScrollView = this.getEnclosingScrollView();

		if(enclosingScrollView == null) {
			return false;
		}

		do {
			ScrollViewPanGestureRecognizer recognizer = enclosingScrollView.panGestureRecognizer;
			if(recognizer.panning) return true;
			ScrollView scrollView = recognizer.getScrollView();
			if(scrollView == null) return false;
			if(scrollView.decelerating) return true;
			enclosingScrollView = recognizer.getEnclosingScrollView();
		} while(enclosingScrollView != null);

		return false;
	}

	private boolean areAnyEnclosingScrollViewsDecelerating() {
		ScrollView scrollView = this.getScrollView();
		if(scrollView == null) return false;

		View superview = scrollView;

		while((superview = superview.getSuperview()) != null) {
			if(superview instanceof ScrollView) {
				if(((ScrollView) superview).decelerating) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean shouldRequireFailureOfGestureRecognizer(GestureRecognizer otherGestureRecognizer) {
		if(otherGestureRecognizer instanceof PanGestureRecognizer) {
			if (this.isDescedent((PanGestureRecognizer) otherGestureRecognizer)) {
				return true;
			}
		}

		return super.shouldRequireFailureOfGestureRecognizer(otherGestureRecognizer);
	}

	protected boolean isDescedent(PanGestureRecognizer otherGestureRecognizer) {
		View view = this.getView();
		View otherView = otherGestureRecognizer.getView();

		while (otherView != null) {
			if(view == otherView) {
				return true;
			} else {
				otherView = otherView.getSuperview();
			}
		}

		return false;
	}

}