/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;
import android.view.ViewConfiguration;
import mocha.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public final class Event extends mocha.foundation.Object {
	private Type type;
	private long timestamp;
	private ArrayList<Touch> touches;
	private boolean finished;
	private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
	private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
	private MotionEvent motionEvent;

	public enum Type {
		TOUCHES,
		SYSTEM,
		UNKNOWN
	}

	static Event systemEvent(Window window) {
		Event event = new Event();
		event.timestamp = android.os.SystemClock.uptimeMillis();
		event.type = Type.SYSTEM;
		return event;
	}

	private Event() {

	}

	Event(MotionEvent motionEvent, Window window) {
		this.updateMotionEvent(motionEvent, window);
	}

	void updateMotionEvent(MotionEvent motionEvent, Window window) {
		this.type = Type.TOUCHES;
		this.timestamp = motionEvent.getEventTime();
		int action = motionEvent.getActionMasked();
		this.motionEvent = motionEvent;

		int numberOfTouches = motionEvent.getPointerCount();
		boolean multiTouch = motionEvent.getActionIndex() > 0;

		Touch.Phase phase = null;

		if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
			phase = Touch.Phase.BEGAN;
			finished = false;
		} else if(action == MotionEvent.ACTION_MOVE) {
			phase = Touch.Phase.MOVED;
			finished = false;
		} else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
			phase = Touch.Phase.ENDED;
			finished = !multiTouch;
		} else if(action == MotionEvent.ACTION_CANCEL) {
			phase = Touch.Phase.CANCELLED;
			finished = true;
		} else {
			this.type = Type.UNKNOWN;
			finished = true;
		}

		if(this.touches == null) {
			this.touches = new ArrayList<Touch>();
		}

		for(int i = 0; i < numberOfTouches; i++) {
			Touch touch;
			if(i >= this.touches.size()) {
				touch = new Touch();
				this.touches.add(touch);
			} else {
				touch = this.touches.get(i);
			}

			Point point = new Point(motionEvent.getX(i) / window.scale, motionEvent.getY(i) / window.scale);
			if(phase == Touch.Phase.BEGAN) {
				int tapCount = 1;
				if(this.timestamp - touch.getTimestamp() < DOUBLE_TAP_TIMEOUT) {
					tapCount += touch.getTapCount();
				}

				touch.setPhase(phase, point, tapCount, this.timestamp);
				touch.setTouchedView(window.hitTest(touch.location, this));
			} else {
				touch.updatePhase(phase, point, this.timestamp);
			}
		}

		if(numberOfTouches < this.touches.size()) {
			this.touches.removeAll(this.touches.subList(this.touches.size() - 1, numberOfTouches));
		}
	}

	MotionEvent getMotionEvent() {
		return this.motionEvent;
	}

	public Type getType() {
		return type;
	}

	public long getTimestamp() {
		return timestamp;
	}

	void setTouches(ArrayList<Touch> touches) {
		this.touches = touches;
	}

	public List<Touch>allTouches() {
		return this.touches;
	}

	public List<Touch>touchesForView(View view) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getView() == view) {
				touches.add(touch);
			}
		}

		return touches;
	}

	public List<Touch>touchesForWindow(Window window) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getWindow() == window) {
				touches.add(touch);
			}
		}

		return touches;
	}

	public List<Touch>touchesForGestureRecognizer(GestureRecognizer gestureRecognizer) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getGestureRecognizers().contains(gestureRecognizer)) {
				touches.add(touch);
			}
		}

		return touches;
	}

	boolean isFinished() {
		return this.finished;
	}

}
