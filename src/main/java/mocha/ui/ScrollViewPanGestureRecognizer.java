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
	private final Point startLocation = new Point();
	private final Point lastLocation = new Point();

	ScrollViewPanGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		if (this.trackingTouch != null) return;

		final ScrollView scrollView = this.getScrollView();

		final Touch touch = this.findTouch(event);
		final State state = this.getState();

		// If our scroll view is decelerating, begin gesture immediately to halt deceleration
		if (!state.recognized && !state.finished && scrollView != null && scrollView.decelerating) {
			if (touch != null) {
				this.start.set(touch.location);
				this.trackingTouch = touch;
				this.current.set(touch.location);
				this.addTouchToHistory(touch);

				Point location = touch.location;
				this.movementAmount.x = location.x - this.start.x;
				this.movementAmount.y = location.y - this.start.y;
				this.scrollDirection = this.lastScrollDirection;
				this.setState(State.BEGAN, true);

				return;
			}
		}

		if (touch != null) {
			this.startLocation.set(touch.location);
			this.lastLocation.set(this.startLocation);
		}

		super.touchesBegan(touches, event);
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if (this.trackingTouch == null || !touches.contains(this.trackingTouch)) return;

		if (this.directionalLockEnabled && this.scrollDirection == null) {
			this.lastLocation.set(this.trackingTouch.location);
		}

		super.touchesMoved(touches, event);
	}

	protected void setState(State state) {
		this.setState(state, false);
	}

	protected void setState(State state, boolean skipDirectionCheck) {
		if (!skipDirectionCheck && state == State.BEGAN && this.directionalLockEnabled) {
			Point abs = new Point(Math.abs(this.startLocation.x - this.lastLocation.x), Math.abs(this.startLocation.y - this.lastLocation.y));

			if (abs.x > abs.y) {
				this.scrollDirection = ScrollDirection.HORIZONTAL;
			} else {
				this.scrollDirection = ScrollDirection.VERTICAL;
			}

			ScrollView scrollView = this.getScrollView();

			if (scrollView != null) {
				if (this.scrollDirection == ScrollDirection.HORIZONTAL) {
					if (!scrollView.canScrollHorizontally && !scrollView.alwaysBounceHorizontal()) {
						state = State.FAILED;
					}
				} else if (this.scrollDirection == ScrollDirection.VERTICAL) {
					if (!scrollView.canScrollVertically && !scrollView.alwaysBounceVertical()) {
						state = State.FAILED;
					}
				}
			}
		}

		if (state == State.BEGAN && this.areAnyEnclosingScrollViewsDecelerating()) {
			state = State.FAILED;
		}

		super.setState(state);
	}

	public Point translationInView(View view) {
		Point point = super.translationInView(view);

		if (this.directionalLockEnabled) {
			if (this.scrollDirection == ScrollDirection.HORIZONTAL) {
				point.y = 0.0f;
			} else if (this.scrollDirection == ScrollDirection.VERTICAL) {
				point.x = 0.0f;
			}
		}

		return point;
	}

	public Point velocityInView(View view) {
		Point point = super.velocityInView(view);

		if (this.directionalLockEnabled) {
			if (this.scrollDirection == ScrollDirection.HORIZONTAL) {
				point.y = 0.0f;
			} else if (this.scrollDirection == ScrollDirection.VERTICAL) {
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
		return this.directionalLockEnabled;
	}

	void setDirectionalLockEnabled(boolean directionalLockEnabled) {
		this.directionalLockEnabled = directionalLockEnabled;
	}

	private ScrollView getScrollView() {
		if (this.getView() instanceof ScrollView) {
			return (ScrollView) this.getView();
		} else {
			return null;
		}
	}

	private boolean areAnyEnclosingScrollViewsDecelerating() {
		ScrollView scrollView = this.getScrollView();
		if (scrollView == null) return false;

		View superview = scrollView;

		while ((superview = superview.getSuperview()) != null) {
			if (superview instanceof ScrollView) {
				if (((ScrollView) superview).decelerating) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean shouldRequireFailureOfGestureRecognizer(GestureRecognizer otherGestureRecognizer) {
		if (otherGestureRecognizer instanceof PanGestureRecognizer) {
			if (this.isDescendant((PanGestureRecognizer) otherGestureRecognizer)) {
				return true;
			}
		}

		return super.shouldRequireFailureOfGestureRecognizer(otherGestureRecognizer);
	}

	protected boolean isDescendant(PanGestureRecognizer otherGestureRecognizer) {
		View view = this.getView();
		View otherView = otherGestureRecognizer.getView();

		while (otherView != null) {
			if (view == otherView) {
				return true;
			} else {
				otherView = otherView.getSuperview();
			}
		}

		return false;
	}

}