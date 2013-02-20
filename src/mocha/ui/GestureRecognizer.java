/*
 *  @author Shaun
 *	@date 11/14/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.ArrayList;
import java.util.List;

abstract public class GestureRecognizer extends mocha.foundation.Object {
	private Delegate delegate;
	private boolean delaysTouchesBegan;
	private boolean delaysTouchesEnded;
	private boolean cancelsTouchesInView;
	private boolean enabled;
	private State state;
	private View view;
	private ArrayList<GestureHandler> registeredGestureHandlers;
	private List<Touch> trackingTouches;
	private List<Touch> ignoredTouches;
	private Event lastEvent;
	private boolean stateSet;

	public enum State {
		POSSIBLE,
		BEGAN,
		CHANGED,
		ENDED,
		CANCELLED,
		FAILED,

		// Only used for discrete gestures like Tap
		RECOGNIZED
	}

	public interface Delegate {
		public boolean shouldBegin(GestureRecognizer gestureRecognizer);
		public boolean shouldReceiveTouch(GestureRecognizer gestureRecognizer, Touch touch);
		public boolean shouldRecognizeSimultaneouslyWithGestureRecognizer(GestureRecognizer gestureRecognizer, GestureRecognizer otherGestureRecognizer);
	}

	public interface GestureHandler {
		public void handleGesture(GestureRecognizer gestureRecognizer);
	}

	public GestureRecognizer() {
		this.state = State.POSSIBLE;
		this.cancelsTouchesInView = true;
		this.delaysTouchesBegan = false;
		this.delaysTouchesEnded = true;
		this.enabled = true;

		this.registeredGestureHandlers = new ArrayList<GestureHandler>();
		this.trackingTouches = new ArrayList<Touch>();
		this.ignoredTouches = new ArrayList<Touch>();
	}

	public GestureRecognizer(GestureHandler gestureHandler) {
		this();
		this.addHandler(gestureHandler);
	}

	public void addHandler(GestureHandler gestureHandler) {
		this.registeredGestureHandlers.add(gestureHandler);
	}

	public void removeHandler(GestureHandler gestureHandler) {
		this.registeredGestureHandlers.remove(gestureHandler);
	}

	public void requireGestureRecognizerToFail(GestureRecognizer gestureRecognizer) {

	}

	List<Touch> getTrackingTouches() {
		return this.trackingTouches;
	}

	public Point locationInView(View view) {
		if(this.trackingTouches.size() > 1) {
			float x = 0.0f;
			float y = 0.0f;
			float k = 0.0f;

			for (Touch touch : this.trackingTouches) {
				Point p = touch.locationInView(this.view);
				x += p.x;
				y += p.y;
				k++;
			}

			if (k > 0) {
				return new Point(x / k, y / k);
			} else {
				return Point.zero();
			}
		} else if(this.trackingTouches.size() == 1) {
			return this.locationOfTouchInView(0, view);
		} else {
			return Point.zero();
		}
	}

	public Point locationOfTouchInView(int touchIndex, View view) {
		return this.trackingTouches.get(touchIndex).locationInView(view);
	}

	public int numberOfTouches() {
		return this.trackingTouches.size();
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	public boolean getDelaysTouchesBegan() {
		return delaysTouchesBegan;
	}

	public void setDelaysTouchesBegan(boolean delaysTouchesBegan) {
		this.delaysTouchesBegan = delaysTouchesBegan;
	}

	public boolean getDelaysTouchesEnded() {
		return delaysTouchesEnded;
	}

	public void setDelaysTouchesEnded(boolean delaysTouchesEnded) {
		this.delaysTouchesEnded = delaysTouchesEnded;
	}

	public boolean getCancelsTouchesInView() {
		return cancelsTouchesInView;
	}

	public void setCancelsTouchesInView(boolean cancelsTouchesInView) {
		this.cancelsTouchesInView = cancelsTouchesInView;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public State getState() {
		return state;
	}

	protected void setState(State state) {
		if((state == State.BEGAN || state == State.RECOGNIZED) && this.delegate != null) {
			if(!this.delegate.shouldBegin(this)) {
				this.state = State.POSSIBLE;
				performAfterDelay(0, new Runnable() {
					public void run() {
						reset();
					}
				});
				return;
			}
		}

		this.stateSet = true;
		StateTransition transition = null;

		for(StateTransition allowedTransition : allowedTransitions) {
			if (allowedTransition.fromState == this.state && allowedTransition.toState == state) {
				transition = allowedTransition;
				break;
			}
		}

		if(transition == null) {
			throw new RuntimeException("Invalid state transition from " + this.state + " to " + state + ".");
		}

		this.state = transition.toState;

		if (transition.shouldNotify && this.shouldNotifyHandlersForState(this.state)) {
			for(final GestureHandler gestureHandler : this.registeredGestureHandlers) {
				gestureHandler.handleGesture(GestureRecognizer.this);
			}
		}

		if(transition.shouldReset) {
			performAfterDelay(0, new Runnable() {
				public void run() {
					reset();
				}
			});
		}
	}

	public View getView() {
		return this.view;
	}

	/// Internal

	void setView(View view) {
		this.reset();
		this.view = view;
	}

	void recognizeTouches(List<Touch> touches, Event event) {
		if(!this.shouldAttemptToRecognize()) return;
		this.trackingTouches.clear();
		this.trackingTouches.addAll(touches);
		this.trackingTouches.removeAll(this.ignoredTouches);

		if(this.delegate != null) {
			boolean shouldRemoveAgain = false;

			for(Touch touch : this.trackingTouches) {
				if(touch.getPhase() == Touch.Phase.BEGAN) {
					if(!this.delegate.shouldReceiveTouch(this, touch)) {
						this.ignoreTouch(touch, event);
						shouldRemoveAgain = true;
					}
				}
			}

			if(shouldRemoveAgain) {
				this.trackingTouches.removeAll(this.ignoredTouches);
			}
		}

		if(this.trackingTouches.size() == 0) return;

		this.lastEvent = event;
		this.stateSet = false;

		for(Touch touch : this.trackingTouches) {
			switch (touch.getPhase()) {
				case BEGAN:
					this.touchesBegan(this.trackingTouches, event);
					break;
				case MOVED:
					this.touchesMoved(this.trackingTouches, event);
					break;
				case STATIONARY:
					break;
				case ENDED:
					this.touchesEnded(this.trackingTouches, event);
					break;
				case CANCELLED:
					this.touchesCancelled(this.trackingTouches, event);
					break;
				case GESTURE_BEGAN:
					break;
				case GESTURE_CHANGED:
					break;
				case GESTURE_ENDED:
					break;
			}
		}

		// If state is already changed, and it wasn't set in this loop, we'll notify
		// the handlers anyway.
		if(!this.stateSet && this.getState() == State.CHANGED) {
			for(GestureHandler gestureHandler : this.registeredGestureHandlers) {
				gestureHandler.handleGesture(GestureRecognizer.this);
			}
		}
	}

	Event lastEvent() {
		return this.lastEvent;
	}

	/// Subclass use

	protected void reset() {
		this.state = State.POSSIBLE;
		this.trackingTouches.clear();
		this.ignoredTouches.clear();
	}

	protected void ignoreTouch(Touch touch, Event event) {
		this.ignoredTouches.add(touch);
	}
	
	protected boolean canPreventGestureRecognizer(GestureRecognizer preventingGestureRecognizer) {
		return true;
	}

	protected boolean canBePreventedByGestureRecognizer(GestureRecognizer preventingGestureRecognizer) {
		return true;
	}

	abstract protected void touchesBegan(List<Touch> touches, Event event);
	abstract protected void touchesMoved(List<Touch> touches, Event event);
	abstract protected void touchesEnded(List<Touch> touches, Event event);
	abstract protected void touchesCancelled(List<Touch> touches, Event event);

	protected boolean shouldNotifyHandlersForState(State state) {
		return true;
	}

	////

	private boolean shouldAttemptToRecognize() {
		return (this.enabled && this.state != State.FAILED && this.state != State.CANCELLED && this.state != State.ENDED);
	}

	public String toString() {
		return String.format("<%s: 0x%d; state = %s; view = %s>", this.getClass(), this.hashCode(), this.state, this.view.toString());
	}

	private static class StateTransition {
		public State fromState;
		public State toState;
		public boolean shouldNotify;
		public boolean shouldReset;

		public StateTransition(State fromState, State toState, boolean  shouldNotify, boolean  shouldReset) {
			this.fromState = fromState;
			this.toState = toState;
			this.shouldNotify = shouldNotify;
			this.shouldReset = shouldReset;
		}
	}

	private static StateTransition[] allowedTransitions = new StateTransition[] {
		new StateTransition(State.POSSIBLE,		State.BEGAN,		true,	false),
		new StateTransition(State.POSSIBLE,		State.FAILED,		false,	true),
		new StateTransition(State.POSSIBLE,		State.ENDED,		true,	true),
		new StateTransition(State.POSSIBLE,		State.RECOGNIZED,	true,	true),
		new StateTransition(State.BEGAN,		State.CHANGED,		true,	false),
		new StateTransition(State.BEGAN,		State.CANCELLED,	true,	true),
		new StateTransition(State.BEGAN,		State.ENDED,		true,	true),
		new StateTransition(State.CHANGED,		State.CHANGED,		true,	false),
		new StateTransition(State.CHANGED,		State.CANCELLED,	true,	true),
		new StateTransition(State.CHANGED,		State.ENDED,		true,	true),
		new StateTransition(State.FAILED,		State.POSSIBLE,		false,	false),
	};

}
