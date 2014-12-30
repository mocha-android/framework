/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.List;

public class LongPressGestureRecognizer extends GestureRecognizer {
	private float allowableMovement = 10.0f;
	private long minimumPressDuration = 500;
	private int numberOfTapsRequired = 1;
	private Point beginLocation;
	private boolean waiting;
	private Runnable beginGestureRunnable;

	public LongPressGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);

		this.beginGestureRunnable = new Runnable() {
			public void run() {
				LongPressGestureRecognizer.this.beginGesture();
			}
		};

		this.setShouldPlayClickSoundOnTransitionToRecognizedState(true);
		this.setShouldPerformHapticFeedbackOnTransitionToRecognizedState(true);
	}

	public int getNumberOfTapsRequired() {
		return numberOfTapsRequired;
	}

	public void setNumberOfTapsRequired(int numberOfTapsRequired) {
		this.numberOfTapsRequired = numberOfTapsRequired;
	}

	public float getAllowableMovement() {
		return allowableMovement;
	}

	public void setAllowableMovement(float allowableMovement) {
		this.allowableMovement = allowableMovement;
	}

	public long getMinimumPressDuration() {
		return minimumPressDuration;
	}

	public void setMinimumPressDuration(long minimumPressDuration) {
		this.minimumPressDuration = minimumPressDuration;
	}

	private void beginGesture() {
		this.waiting = false;

		if (this.getState() == State.POSSIBLE) {
			this.setState(State.BEGAN);
		}
	}

	private void cancelWaiting() {
		if (this.waiting) {
			this.waiting = false;
			cancelCallbacks(this.beginGestureRunnable);
			this.setState(State.FAILED);
		}
	}

	@Override
	protected void touchesBegan(List<Touch> touches, Event event) {
		Touch touch = event.getTouchesForGestureRecognizer(this).get(0);

		if (!this.waiting && this.getState() == State.POSSIBLE && touch.getTapCount() >= this.numberOfTapsRequired) {
			this.beginLocation = touch.location;
			this.waiting = true;
			performAfterDelay(this.minimumPressDuration, this.beginGestureRunnable);
		}
	}

	@Override
	protected void touchesMoved(List<Touch> touches, Event event) {
		if (waiting || this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			if(this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
				this.setState(State.CHANGED);
			} else if(waiting) {
				float distance = event.getTouchesForGestureRecognizer(this).get(0).location.distanceBetween(this.beginLocation);
				if (distance > this.allowableMovement) {
					this.cancelWaiting();
				} else {
					MLog("Touches moved, " + distance + " < " + this.allowableMovement);
				}
			}
		}
	}

	@Override
	protected void touchesEnded(List<Touch> touches, Event event) {
		if (this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.ENDED);
		} else {
			this.cancelWaiting();
		}
	}

	@Override
	protected void touchesCancelled(List<Touch> touches, Event event) {
		if (this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.CANCELLED);
		} else {
			this.cancelWaiting();
		}
	}

}
