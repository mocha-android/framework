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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Event extends mocha.foundation.Object {
	private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
	private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

	/**
	 * Event type
	 */
	private Type type;

	/**
	 * Time the event occurred
	 */
	private long timestamp;

	/**
	 * Touches for the current event
	 */
	private List<Touch> touches;

	/**
	 * Touches that have occurred since the first touch went down.
	 *
	 * This is used so if you touch down with one finger, then another,
	 * then touch up with the first finger while keeping the second down,
	 * and then put the first finger down again, we're able to restore
	 * the original touch instance instead of creating a new one.
	 *
	 * After all touches have ended, this is cleared.
	 */
	private Map<Integer,Touch> lifetimeTouches;

	/**
	 * Underlying motion event
	 */
	private MotionEvent motionEvent;

	/**
	 * Reused in #updateMotionEvent() each time it's called
	 *
	 * The references is stored here strictly to avoid objection creation
	 * every time #updateMotionEvent() is called.
	 */
	private MotionEvent.PointerProperties pointerProperties;

	/**
	 * If true, we clear out lifetimeTouches on the next
	 * #updateMotionEvent() call.
	 */
	private boolean resetTouchesOnNextUpdate;

	public enum Type {
		TOUCHES,
		SYSTEM,
		UNKNOWN
	}

	/**
	 * Create a system event for a window
	 *
	 * @param window window the event will be sent to
	 * @return system even
	 */
	static Event systemEvent(Window window) {
		Event event = new Event();
		event.timestamp = android.os.SystemClock.uptimeMillis();
		event.type = Type.SYSTEM;
		return event;
	}

	private Event() { }

	/**
	 * Create a motion event
	 * @param motionEvent system motion event
	 * @param window window the event will be sent to
	 */
	Event(MotionEvent motionEvent, Window window) {
		this.lifetimeTouches = new HashMap<Integer, Touch>();
		this.touches = new ArrayList<Touch>();
		this.pointerProperties = new MotionEvent.PointerProperties();
		this.updateMotionEvent(motionEvent, window);
	}

	/**
	 * Update this event with a new motion event
	 *
	 * @param motionEvent system motion event
	 * @param window window the event will be sent to
	 */
	void updateMotionEvent(MotionEvent motionEvent, Window window) {
		this.type = Type.TOUCHES;
		this.timestamp = motionEvent.getEventTime();
		int action = motionEvent.getActionMasked();
		this.motionEvent = motionEvent;

		int numberOfTouches = motionEvent.getPointerCount();

		if(this.resetTouchesOnNextUpdate || action == MotionEvent.ACTION_DOWN) {
			this.lifetimeTouches.clear();
		}

		this.touches.clear();

		Touch.Phase phase = null;
		this.resetTouchesOnNextUpdate = false;

		if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
			phase = Touch.Phase.BEGAN;
		} else if(action == MotionEvent.ACTION_MOVE) {
			phase = Touch.Phase.MOVED;
		} else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
			phase = Touch.Phase.ENDED;
			this.resetTouchesOnNextUpdate = action == MotionEvent.ACTION_UP;
		} else if(action == MotionEvent.ACTION_CANCEL) {
			phase = Touch.Phase.CANCELLED;
		} else {
			this.type = Type.UNKNOWN;
		}

		for(int i = 0; i < numberOfTouches; i++) {
			motionEvent.getPointerProperties(i, this.pointerProperties);
			int touchId = this.pointerProperties.id;

			Touch touch = this.lifetimeTouches.get(touchId);

			if(touch == null) {
				touch = new Touch();
				this.lifetimeTouches.put(touchId, touch);
			}

			this.touches.add(touch);

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
	}

	/**
	 * Get the system motion event
	 *
	 * @return system motion event
	 */
	MotionEvent getMotionEvent() {
		return this.motionEvent;
	}

	/**
	 * Get the event type
	 *
	 * @return event type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Get the timestamp for the event
	 *
	 * @return event typestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Get all of the touches for the event
	 *
	 * @return touches for the event
	 */
	public List<Touch>allTouches() {
		return this.touches;
	}

	/**
	 * Get touches for a specific view
	 *
	 * @param view view to get touches in
	 * @return touches for specified view
	 */
	public List<Touch>touchesForView(View view) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getView() == view) {
				touches.add(touch);
			}
		}

		return touches;
	}

	/**
	 * Get touches for a specific window
	 *
	 * @param window view to get touches in
	 * @return touches for specified window
	 */
	public List<Touch>touchesForWindow(Window window) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getWindow() == window) {
				touches.add(touch);
			}
		}

		return touches;
	}

	/**
	 * Get touches for a specific gesture recognizer
	 *
	 * @param gestureRecognizer gesture recognizer to get touches for
	 * @return touches for specified gesture recognizer
	 */
	public List<Touch>touchesForGestureRecognizer(GestureRecognizer gestureRecognizer) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches()) {
			if (touch.getGestureRecognizers().contains(gestureRecognizer)) {
				touches.add(touch);
			}
		}

		return touches;
	}

}
