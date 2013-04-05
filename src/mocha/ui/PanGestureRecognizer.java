/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public class PanGestureRecognizer extends GestureRecognizer {
	Point translation;
	boolean tracking;
	boolean panning;
	private List<TrackingDataPoint> trackingDataPoints;
	Point startTouchPosition;
	Point lastTouchPosition;

	private static final float MINIMUM_TRACKING_FOR_PAN = 10.0f;
	private static final long MAX_TIME_FOR_TRACKING_DATA_POINTS = 100;

	private static class TrackingDataPoint {
		long time;
		Point point;

		private TrackingDataPoint(long time, Point point) {
			this.time = time;
			this.point = point;
		}
	}

	public PanGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);

		this.translation = new Point();
		this.tracking = false;
		this.panning = false;
		this.trackingDataPoints = new ArrayList<TrackingDataPoint>();
	}

	public Point translationInView(View view) {
		return this.lastTouchPosition.delta(this.startTouchPosition);
	}

	public Point velocityInView(View view) {
		this.purgeTrackingDataPointsWithTime(android.os.SystemClock.uptimeMillis());

		if (this.trackingDataPoints.size() < 2) {
			return Point.zero();
		}

		TrackingDataPoint firstPoint = this.trackingDataPoints.get(0);
		TrackingDataPoint lastPoint = this.trackingDataPoints.get(this.trackingDataPoints.size() - 1);
		float timeDelta = (float)(lastPoint.time - firstPoint.time) / 1000.0f;

		if(timeDelta == 0) {
			return Point.zero();
		} else {
			return new Point((lastPoint.point.x - firstPoint.point.x) / timeDelta, (lastPoint.point.y - firstPoint.point.y) / timeDelta);
		}
	}

	Touch findTouch(Event event) {
		List<Touch> touches = event.touchesForGestureRecognizer(this);
		return touches != null && touches.size() > 0 ? touches.get(0) : null;
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		if(this.tracking) return;

		Touch touch = this.findTouch(event);

		if(touch != null) {
			this.startTouchPosition = touch.location.copy();
			this.tracking = true;
			this.addTrackingDataPoint(this.startTouchPosition, event);
		}
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if(!this.tracking) return;

		Touch touch = this.findTouch(event);

		if(touch != null) {
			this.lastTouchPosition = touch.location.copy();

			Point location = touch.location;
			this.translation = new Point(location.x - this.startTouchPosition.x, location.y - this.startTouchPosition.y);

			if (this.panning) {
				this.setState(State.CHANGED);
			} else {
				if (Math.abs(this.translation.x) >= MINIMUM_TRACKING_FOR_PAN || Math.abs(this.translation.y) >= MINIMUM_TRACKING_FOR_PAN) {
					this.startTouchPosition = touch.location.copy();
					this.trackingDataPoints.clear();
					this.translation.x = 0.0f;
					this.translation.y = 0.0f;

					this.panning = true;
					this.setState(State.BEGAN);
				} else {
					return;
				}
			}

			this.addTrackingDataPoint(this.lastTouchPosition, event);
		} else if(this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.CANCELLED);
			this.tracking = false;
		}
	}

	protected void touchesEnded(List<Touch> touches, Event event) {
		if(!this.tracking) return;
		this.tracking = false;

		if(this.panning) {
			this.setState(State.ENDED);
		} else if(this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.CANCELLED);
		}
	}

	protected void touchesCancelled(List<Touch> touches, Event event) {
		if(!this.tracking) return;
		this.tracking = false;

		if (this.panning) {
			this.setState(State.CANCELLED);
		}
	}

	private void purgeTrackingDataPointsWithTime(long timestamp) {
		while (this.trackingDataPoints.size() > 0) {
			if (timestamp - this.trackingDataPoints.get(0).time <= MAX_TIME_FOR_TRACKING_DATA_POINTS) {
				break;
			}

			this.trackingDataPoints.remove(0);
		}
	}

	void addTrackingDataPoint(Point point, Event event) {
		long timestamp = event.getTimestamp();
		this.purgeTrackingDataPointsWithTime(timestamp);
		this.trackingDataPoints.add(new TrackingDataPoint(timestamp, point.copy()));
	}

	protected void reset() {
		super.reset();

		this.tracking = false;
		this.panning = false;
		this.trackingDataPoints.clear();
		this.startTouchPosition = null;
		this.lastTouchPosition = null;
	}

}
