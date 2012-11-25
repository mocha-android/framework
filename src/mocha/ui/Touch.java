/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.ArrayList;

public final class Touch extends mocha.foundation.Object {
	Point location;
	Point previousLocation;
	private View view;
	private long timestamp;
	private int tapCount;
	private Phase phase;
	private Gesture gesture;
	private Point delta;
	private float rotation;
	private float magnification;
	private ArrayList<GestureRecognizer> gestureRecognizers;

	public enum Phase {
		BEGAN,
		MOVED,
		STATIONARY,
		ENDED,
		CANCELLED,

		// Internal phases
		GESTURE_BEGAN,
		GESTURE_CHANGED,
		GESTURE_ENDED
	}

	enum Gesture {
		UNKNOWN,
		PAN,
		ROTATION,
		PINCH,
		SWIPE
	}

	Touch() {
		this.phase = Phase.CANCELLED;
		this.gesture = Gesture.UNKNOWN;
		this.gestureRecognizers = new ArrayList<GestureRecognizer>();
	}

	public View getView() {
		return this.view;
	}

	public Window getWindow() {
		if(this.view == null) {
			return null;
		} else {
			return this.view.getWindow();
		}
	}

	public Point locationInView(View view) {
		Window window = this.getWindow();

		if(window != null) {
			return this.getWindow().convertPointToView(this.location, view);
		} else {
			return Point.zero();
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getTapCount() {
		return tapCount;
	}

	public Phase getPhase() {
		return phase;
	}

	public ArrayList<GestureRecognizer>getGestureRecognizers() {
		return new ArrayList<GestureRecognizer>(this.gestureRecognizers);
	}

	/// Internal methods

	void setPhase(Phase phase, Point screenLocation, int tapCount, long timestamp) {
		this.phase = phase;
		this.gesture = Gesture.UNKNOWN;
		this.previousLocation = this.location = screenLocation;
		this.tapCount = tapCount;
		this.timestamp = timestamp;
		this.rotation = 0.0f;
		this.magnification = 0.0f;
	}

	void updatePhase(Phase phase, Point screenLocation, long timestamp) {
		if(!screenLocation.equals(this.location)) {
			this.previousLocation = this.location;
			this.location = screenLocation;
		}

		this.phase = phase;
		this.timestamp = timestamp;
	}

	void updateGesture(Gesture gesture, Point screenLocation, Point delta, float rotation, float magnification, long timestamp) {
		if(!screenLocation.equals(this.location)) {
			this.previousLocation = this.location;
			this.location = screenLocation;
		}

		this.phase = Phase.GESTURE_CHANGED;
		this.gesture = gesture;
		this.delta = delta;
		this.rotation = rotation;
		this.magnification = magnification;
		this.timestamp = timestamp;
	}

	// sets up the window and gesture recognizers as well
	void setTouchedView(View view) {
		if(this.view != view) {
			this.view = view;
			this.gestureRecognizers.clear();
			this.gestureRecognizers.addAll(gestureRecognizersForView(view));
		}
	}

	// sets the initial view to nil, but leaves window and gesture recognizers alone - used when a view is removed while touch is active
	void removeFromView() {
		ArrayList<GestureRecognizer> remainingRecognizers = new ArrayList<GestureRecognizer>(this.gestureRecognizers);

		for(GestureRecognizer recognizer : this.gestureRecognizers) {
			if(recognizer.getView() == this.view) {
				if (recognizer.getState() == GestureRecognizer.State.BEGAN || recognizer.getState() == GestureRecognizer.State.CHANGED) {
					recognizer.setState(GestureRecognizer.State.CANCELLED);
				}

				remainingRecognizers.remove(recognizer);
			}
		}

		this.gestureRecognizers.clear();
		this.gestureRecognizers.addAll(remainingRecognizers);
		this.view = null;
	}

	void setTouchPhaseCancelled() {
		this.phase = Phase.CANCELLED;
	}

	Point getDelta() {
		return this.delta;
	}

	float getRotation() {
		return this.rotation;
	}

	float getMagnification() {
		return this.magnification;
	}

	Gesture getGesture() {
		return this.gesture;
	}

	private static ArrayList<GestureRecognizer> gestureRecognizersForView(View view) {
		ArrayList<GestureRecognizer> gestureRecognizers = new ArrayList<GestureRecognizer>();

		while(view != null) {
			gestureRecognizers.addAll(view.getGestureRecognizers());
			view = view.getSuperview();
		}

		return gestureRecognizers;
	}

}
