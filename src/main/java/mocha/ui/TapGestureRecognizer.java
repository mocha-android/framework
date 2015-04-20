package mocha.ui;

import mocha.graphics.Point;

import java.lang.reflect.Method;
import java.util.List;

// TODO: Support multiple tap counts and multiple touches
public class TapGestureRecognizer extends GestureRecognizer {
	private int numberOfTapsRequired;
	private int numberOfTouchesRequired;
	private boolean tracking;
	private Point startPoint;
	private long startTime;
	private int taps;
	private Runnable failCallback;

	private float allowableMovement = 45.0f;
	private long maximumSingleTapDuration = 750;
	private long maximumIntervalBetweenSuccessiveTaps = 350;

	public TapGestureRecognizer(GestureHandler gestureHandler) {
		this(1, 1, gestureHandler);
	}

	public TapGestureRecognizer(Object target, String actionMethodName) {
		this(1, 1, target, actionMethodName);
	}

	public TapGestureRecognizer(Object target, Method actionMethod) {
		this(1, 1, target, actionMethod);
	}

	public TapGestureRecognizer(int numberOfTapsRequired, int numberOfTouchesRequired, GestureHandler gestureHandler) {
		super(gestureHandler);

		this.numberOfTapsRequired = numberOfTapsRequired;
		this.numberOfTouchesRequired = numberOfTouchesRequired;
	}

	public TapGestureRecognizer(int numberOfTapsRequired, int numberOfTouchesRequired, Object target, String actionMethodName) {
		super(target, actionMethodName);

		this.numberOfTapsRequired = numberOfTapsRequired;
		this.numberOfTouchesRequired = numberOfTouchesRequired;
	}

	public TapGestureRecognizer(int numberOfTapsRequired, int numberOfTouchesRequired, Object target, Method actionMethod) {
		super(target, actionMethod);

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

	public boolean canBePreventedByGestureRecognizer(GestureRecognizer preventingGestureRecognizer) {
		if (preventingGestureRecognizer instanceof TapGestureRecognizer) {
			return ((TapGestureRecognizer) preventingGestureRecognizer).numberOfTapsRequired > this.numberOfTapsRequired;
		} else {
			return super.canBePreventedByGestureRecognizer(preventingGestureRecognizer);
		}
	}


	public boolean canPreventGestureRecognizer(GestureRecognizer preventedGestureRecognizer) {
		if (preventedGestureRecognizer instanceof TapGestureRecognizer) {
			return ((TapGestureRecognizer) preventedGestureRecognizer).numberOfTapsRequired <= this.numberOfTapsRequired;
		} else {
			return super.canPreventGestureRecognizer(preventedGestureRecognizer);
		}
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		this.tracking = true;
		this.startPoint = touches.get(0).location;
		this.startTime = android.os.SystemClock.uptimeMillis();

		if (this.failCallback == null) {
			this.failCallback = performAfterDelay(this.maximumIntervalBetweenSuccessiveTaps, new Runnable() {
				@Override
				public void run() {
					setState(State.FAILED);
				}
			});
		}
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if (this.tracking && !this.verifyTouch(touches.get(0))) {
			this.setState(State.FAILED);
		}
	}

	protected void touchesEnded(List<Touch> touches, Event event) {
		if (this.tracking) {
			if (this.verifyTouch(touches.get(0))) {
				this.setState(State.RECOGNIZED);
			} else {
				this.setState(State.FAILED);
			}

			this.tracking = false;
		}
	}

	private boolean verifyTouch(Touch touch) {
		if (this.startTime + this.maximumSingleTapDuration < android.os.SystemClock.uptimeMillis()) {
			return false;
		} else {
			Point delta = this.startPoint.delta(touch.location).abs();
			return delta.x <= this.allowableMovement && delta.y <= this.allowableMovement;
		}
	}

	protected void touchesCancelled(List<Touch> touches, Event event) {
		this.tracking = false;
	}

	protected void reset() {
		this.tracking = false;

		if (this.failCallback != null) {
			cancelCallbacks(this.failCallback);
			this.failCallback = null;
		}

		super.reset();
	}
}
