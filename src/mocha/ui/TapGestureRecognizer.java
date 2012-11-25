/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import java.util.List;

public class TapGestureRecognizer extends GestureRecognizer {
	private int numberOfTapsRequired;
	private int numberOfTouchesRequired;

	public TapGestureRecognizer(GestureHandler gestureHandler) {
		this(1, 1, gestureHandler);
	}

	public TapGestureRecognizer(int numberOfTapsRequired, int numberOfTouchesRequired, GestureHandler gestureHandler) {
		super(gestureHandler);

		this.numberOfTapsRequired = numberOfTapsRequired;
		this.numberOfTouchesRequired = numberOfTouchesRequired;
	}

	public int getNumberOfTapsRequired() {
		return numberOfTapsRequired;
	}

	public void setNumberOfTapsRequired(int numberOfTapsRequired) {
		this.numberOfTapsRequired = numberOfTapsRequired;
	}

	public int getNumberOfTouchesRequired() {
		return numberOfTouchesRequired;
	}

	public void setNumberOfTouchesRequired(int numberOfTouchesRequired) {
		this.numberOfTouchesRequired = numberOfTouchesRequired;
	}

	///

	protected boolean shouldNotifyHandlersForState(State state) {
		return state == State.BEGAN;
	}

	public boolean canBePreventedByGestureRecognizer(GestureRecognizer preventingGestureRecognizer) {
		if(preventingGestureRecognizer instanceof TapGestureRecognizer) {
			return ((TapGestureRecognizer)preventingGestureRecognizer).numberOfTapsRequired > this.numberOfTapsRequired;
		} else {
			return super.canBePreventedByGestureRecognizer(preventingGestureRecognizer);
		}
	}


	public boolean canPreventGestureRecognizer(GestureRecognizer preventedGestureRecognizer) {
		if(preventedGestureRecognizer instanceof TapGestureRecognizer) {
			return ((TapGestureRecognizer)preventedGestureRecognizer).numberOfTapsRequired <= this.numberOfTapsRequired;
		} else {
			return super.canPreventGestureRecognizer(preventedGestureRecognizer);
		}
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);

		if (touch.getTapCount() >= this.numberOfTapsRequired) {
			if (this.getState() == State.POSSIBLE) {
				this.setState(State.BEGAN);
			} else if (this.getState() == State.BEGAN) {
				this.setState(State.CHANGED);
			}
		}
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if (this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.CANCELLED);
		}
	}

	protected void touchesEnded(List<Touch> touches, Event event) {
		if (this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.ENDED);
		}
	}

	protected void touchesCancelled(List<Touch> touches, Event event) {
		if (this.getState() == State.BEGAN || this.getState() == State.CHANGED) {
			this.setState(State.CANCELLED);
		}
	}

}
