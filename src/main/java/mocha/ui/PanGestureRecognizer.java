/**
 *	@author Shaun
 *	@date 11/23/12
 *	@copyright	2015 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.Lists;
import mocha.graphics.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PanGestureRecognizer extends GestureRecognizer {
	private static final float MINIMUM_MOVEMENT = 10.0f;
	private static final long MAXIMUM_ELAPSED_TIME_IN_TOUCH_HISTORY = 100;

	final Point movementAmount = new Point();
	final Point start = new Point();
	final Point current = new Point();
	final private List<HistoricTouch> touchHistory = new ArrayList<>();

	Touch trackingTouch = null;

	private static class HistoricTouch {
		final long timeInMillis;
		final Point point;

		private HistoricTouch(long timeInMillis, Point point) {
			this.timeInMillis = timeInMillis;
			this.point = point;
		}
	}

	public PanGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);
	}

	public Point translationInView(View view) {
		return this.current.delta(this.start);
	}

	public Point velocityInView(View view) {
		this.truncateTouchHistory(android.os.SystemClock.uptimeMillis());

		final HistoricTouch firstPoint = Lists.first(this.touchHistory);
		final HistoricTouch lastPoint = Lists.last(this.touchHistory);

		if(firstPoint == null || lastPoint == null || firstPoint == lastPoint || lastPoint.timeInMillis == firstPoint.timeInMillis) {
			return Point.zero();
		}

		final float elapsedTime = (float) (lastPoint.timeInMillis - firstPoint.timeInMillis) / 1000.0f;
		return new Point((lastPoint.point.x - firstPoint.point.x) / elapsedTime, (lastPoint.point.y - firstPoint.point.y) / elapsedTime);
	}

	private void truncateTouchHistory(long currentTimeInMillis) {
		Iterator<HistoricTouch> iterator = this.touchHistory.iterator();

		while(iterator.hasNext()) {
			if(currentTimeInMillis - iterator.next().timeInMillis > MAXIMUM_ELAPSED_TIME_IN_TOUCH_HISTORY) {
				iterator.remove();
			}
		}
	}

	Touch findTouch(Event event) {
		List<Touch> touches = event.getTouchesForGestureRecognizer(this);
		return touches != null && touches.size() > 0 ? touches.get(0) : null;
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		if (this.trackingTouch != null) return;

		this.trackingTouch = this.findTouch(event);

		if (this.trackingTouch != null) {
			this.addTouchToHistory(this.trackingTouch);
			this.start.set(this.trackingTouch.location);
		}
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if (this.trackingTouch == null || !touches.contains(this.trackingTouch)) return;

		this.current.set(this.trackingTouch.location);

		this.movementAmount.x = this.trackingTouch.location.x - this.start.x;
		this.movementAmount.y = this.trackingTouch.location.y - this.start.y;

		final State state = this.getState();

		if (state == State.BEGAN || state == State.CHANGED) {
			this.setState(State.CHANGED);
		} else {
			if (Math.abs(this.movementAmount.x) >= MINIMUM_MOVEMENT || Math.abs(this.movementAmount.y) >= MINIMUM_MOVEMENT) {
				this.start.set(this.trackingTouch.location);
				this.touchHistory.clear();
				this.movementAmount.x = 0.0f;
				this.movementAmount.y = 0.0f;

				this.setState(State.BEGAN);
			} else {
				return;
			}
		}

		this.addTouchToHistory(this.trackingTouch);
	}

	protected void touchesEnded(List<Touch> touches, Event event) {
		if (this.trackingTouch == null || !touches.contains(this.trackingTouch)) return;

		this.trackingTouch = null;
		final State state = this.getState();

		if (state.recognized && !state.finished) {
			this.setState(State.ENDED);
		}
	}

	protected void touchesCancelled(List<Touch> touches, Event event) {
		if (this.trackingTouch == null || !touches.contains(this.trackingTouch)) return;
		this.trackingTouch = null;

		final State state = this.getState();

		if (state.recognized && !state.finished) {
			this.setState(State.CANCELLED);
		}
	}

	void addTouchToHistory(Touch touch) {
		long timestamp = touch.getTimestamp();
		this.truncateTouchHistory(timestamp);
		this.touchHistory.add(new HistoricTouch(timestamp, touch.location.copy()));
	}

	protected void setState(State state) {
		super.setState(state);

		if (state == State.FAILED) {
			this.trackingTouch = null;
			this.touchHistory.clear();
		}
	}

	protected void reset() {
		super.reset();

		this.trackingTouch = null;
		this.touchHistory.clear();
	}

}
