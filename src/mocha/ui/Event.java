/*
 *  @author Shaun
 *  @date 11/13/12
 *  @copyright 2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import mocha.graphics.Point;

import java.util.*;

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
	private List<Touch> currentTouches;

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
	 * Touches currently on the screen, they may not be part of the current event
	 * if they're stationary.
	 */
	private List<Touch> allTouches;

	/**
	 * Underlying motion event
	 */
	private MotionEvent motionEvent;

	/**
	 * If true, we clear out lifetimeTouches on the next
	 * #updateMotionEvent() call.
	 */
	private boolean resetTouchesOnNextClean;

	/**
	 * Event type
	 */
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

	/**
	 * Create a touch event
	 *
	 * @param window window the event will be sent to
	 * @return touch event
	 */
	static Event touchEvent(Window window) {
		Event event = new Event();
		event.lifetimeTouches = new HashMap<Integer, Touch>();
		event.currentTouches = new ArrayList<Touch>();
		event.allTouches = new ArrayList<Touch>();
		event.type = Type.TOUCHES;
		return event;
	}

	private Event() { }

	/**
	 * Update this event with a new motion event
	 *
	 * @param motionEvent system motion event
	 * @param window window the event will be sent to
	 * @param touchedView the view that was touched to cause this event
	 */
	void updateMotionEvent(MotionEvent motionEvent, Window window, android.view.View touchedView) {
		this.type = Type.TOUCHES;
		this.timestamp = motionEvent.getEventTime();
		this.motionEvent = motionEvent;
		this.currentTouches.clear();

		int action = motionEvent.getActionMasked();

		if(action == MotionEvent.ACTION_MOVE) {
			// Move events do not provide the pointer(s) responsible
			// so we need to loop through all of them and figure out
			// which ones moved and which didn't.

			int numberOfTouches = motionEvent.getPointerCount();

			for(int pointerIndex = 0; pointerIndex < numberOfTouches; pointerIndex++) {
				Touch touch = this.getTouchForPointerIndex(motionEvent, pointerIndex);
				Touch.Phase phase = touch.getPhase();

				if(phase == Touch.Phase.BEGAN || phase == Touch.Phase.MOVED || phase == Touch.Phase.STATIONARY) {
					Point point = this.getTouchLocation(motionEvent, pointerIndex, window, touchedView);

					if(!point.equals(touch.location)) {
						touch.updatePhase(Touch.Phase.MOVED, point, this.timestamp);
						this.currentTouches.add(touch);
					} else {
						touch.updatePhase(Touch.Phase.STATIONARY, point, this.timestamp);
					}
				}
			}
		} else {
			// Most likely a UP/DOWN action, which means it's only a single
			// touch and we need to use the action index

			int actionIndex = motionEvent.getActionIndex();

			// Ensure we have a valid index
			if(actionIndex < 0 || actionIndex >= motionEvent.getPointerCount()) {
				return;
			}

			// Get touch/point
			Touch touch = this.getTouchForPointerIndex(motionEvent, actionIndex);
			Point point = this.getTouchLocation(motionEvent, actionIndex, window, touchedView);
			this.currentTouches.add(touch);

			Touch.Phase phase = null;

			// Determine phase for motion event action
			switch (action) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
					phase = Touch.Phase.BEGAN;
					this.allTouches.add(touch);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					phase = Touch.Phase.ENDED;
					this.resetTouchesOnNextClean = action == MotionEvent.ACTION_UP;
					break;
				case MotionEvent.ACTION_CANCEL:
					phase = Touch.Phase.CANCELLED;
					break;
				default:
					this.type = Type.UNKNOWN;
			}

			// Update/set touch for phase
			if(phase == Touch.Phase.BEGAN) {
				touch.setPhase(phase, point, 1, this.timestamp);
				touch.setTouchedView(window.hitTest(touch.location, this));
			} else if(phase == Touch.Phase.ENDED || phase == Touch.Phase.CANCELLED) {
				touch.updatePhase(phase, point, this.timestamp);
			}

			// Set all other touches to stationary
			for(Touch otherTouch : this.allTouches) {
				if(otherTouch == touch) continue;

				Touch.Phase touchPhase = otherTouch.getPhase();

				if(touchPhase == Touch.Phase.BEGAN || touchPhase == Touch.Phase.MOVED) {
					otherTouch.setPhase(Touch.Phase.STATIONARY);
				}
			}
		}
	}

	/**
	 * Gets or creates a touch instance for a pointer index in a motion event
	 *
	 * @param motionEvent motion event for the pointer index
	 * @param pointerIndex pointer index
	 * @return touch
	 */
	private Touch getTouchForPointerIndex(MotionEvent motionEvent, int pointerIndex) {
		int touchId = motionEvent.getPointerId(pointerIndex);
		Touch touch = this.lifetimeTouches.get(touchId);

		if(touch == null) {
			touch = new Touch();
			this.lifetimeTouches.put(touchId, touch);
		}

		return touch;
	}

	/**
	 * Get's the location of the touch relative to the window
	 *
	 * @param motionEvent system motion event
	 * @param pointerIndex pointer index
	 * @param window window the event will be sent to
	 * @param touchedView the view that was touched to cause this event
	 * @return Touch location
	 */

	private Point getTouchLocation(MotionEvent motionEvent, int pointerIndex, Window window, android.view.View touchedView) {
		float x = motionEvent.getX(pointerIndex);
		float y = motionEvent.getY(pointerIndex);

		if(window.getLayer() != touchedView) {
			ViewGroup viewGroup = window.getLayer().getViewGroup();

			if(viewGroup != null) {
				int[] windowLocation = new int[2];
				viewGroup.getLocationOnScreen(windowLocation);

				int[] viewLocation = new int[2];
				touchedView.getLocationOnScreen(viewLocation);

				x += viewLocation[0] - windowLocation[0];
				y += viewLocation[1] - windowLocation[1];
			}
		}

		return new Point(x / window.scale, y / window.scale);
	}

	/**
	 * Clean up touches after the event has been processed.
	 */
	void cleanTouches() {
		if(this.resetTouchesOnNextClean) {
			this.lifetimeTouches.clear();
			this.allTouches.clear();
			this.currentTouches.clear();
			this.resetTouchesOnNextClean = false;
		} else {
			List<Touch> touches = new ArrayList<Touch>(this.allTouches);

			for(Touch touch : touches) {
				if(touch.getPhase() == Touch.Phase.ENDED || touch.getPhase() == Touch.Phase.CANCELLED) {
					this.allTouches.remove(touch);
				}
			}
		}
	}

	/**
	 * Get the current touches for when the event was last updated
	 * by a MotionEvent.
	 *
	 * @return current touches
	 */
	List<Touch> getCurrentTouches() {
		return currentTouches;
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
	 * @return event timestamp
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
		return Collections.unmodifiableList(this.allTouches);
	}

	/**
	 * Get touches for a specific view
	 *
	 * @param view view to get touches in
	 * @return touches for specified view
	 */
	public List<Touch> getTouchesForView(View view) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches) {
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
	public List<Touch> getTouchesForWindow(Window window) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches) {
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
	public List<Touch> getTouchesForGestureRecognizer(GestureRecognizer gestureRecognizer) {
		List<Touch> touches = new ArrayList<Touch>();

		for(Touch touch : this.allTouches) {
			if (touch.getGestureRecognizers().contains(gestureRecognizer)) {
				touches.add(touch);
			}
		}

		return touches;
	}

}
