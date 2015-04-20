package mocha.ui;

import mocha.foundation.Lists;
import mocha.foundation.MObject;
import mocha.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public final class Touch extends MObject {
	final Point location = new Point();
	final Point previousLocation = new Point();
	private View view;
	private long timestamp;
	private int tapCount;
	private Phase phase = Phase.CANCELLED;
	private Gesture gesture = Gesture.UNKNOWN;
	private final Point delta = new Point();
	private float rotation;
	private float magnification;
	private final List<GestureRecognizer> gestureRecognizers = new ArrayList<>();

	public enum Phase {
		BEGAN,
		MOVED,
		STATIONARY,
		ENDED,
		CANCELLED,
	}

	enum Gesture {
		UNKNOWN,
		PAN,
		ROTATION,
		PINCH,
		SWIPE
	}

	Touch() {
	}

	public View getView() {
		return this.view;
	}

	public Window getWindow() {
		if (this.view == null) {
			return null;
		} else {
			return this.view.getWindow();
		}
	}

	public Point locationInView(View view) {
		Window window = this.getWindow();

		if (window != null) {
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

	public List<GestureRecognizer> getGestureRecognizers() {
		return Lists.copy(this.gestureRecognizers);
	}

	/// Internal methods

	void setPhase(Phase phase, Point screenLocation, int tapCount, long timestamp) {
		this.phase = phase;
		this.gesture = Gesture.UNKNOWN;
		this.previousLocation.set(screenLocation);
		this.location.set(screenLocation);
		this.tapCount = tapCount;
		this.timestamp = timestamp;
		this.rotation = 0.0f;
		this.magnification = 0.0f;
	}

	void setPhase(Phase phase) {
		this.phase = phase;
	}

	void updatePhase(Phase phase, Point screenLocation, long timestamp) {
		if (!screenLocation.equals(this.location)) {
			this.previousLocation.set(this.location);
			this.location.set(screenLocation);
		}

		this.phase = phase;
		this.timestamp = timestamp;
	}

	// sets up the window and gesture recognizers as well
	void setTouchedView(View view) {
		if (this.view != view) {
			this.view = view;
			this.gestureRecognizers.clear();
			this.gestureRecognizers.addAll(gestureRecognizersForView(view));
		}
	}

	void removeGestureRecognizer(GestureRecognizer gestureRecognizer) {
		this.gestureRecognizers.remove(gestureRecognizer);
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

	private static List<GestureRecognizer> gestureRecognizersForView(View view) {
		List<GestureRecognizer> gestureRecognizers = new ArrayList<GestureRecognizer>();

		while (view != null) {
			gestureRecognizers.addAll(view.getGestureRecognizers());
			view = view.getSuperview();
		}

		return gestureRecognizers;
	}

	protected String toStringExtra() {
		return String.format("timestamp = %d; tapCount = %d; phase = %s; view = %s", this.timestamp, this.tapCount, this.phase, this.view);
	}

}
