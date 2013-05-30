/*
 *  @author Shaun
 *	@date 11/14/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;
import mocha.graphics.Point;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract public class GestureRecognizer extends MObject {
	public enum State {
		// State     Recognized		Reset		Notify		Finished
		POSSIBLE	(false,			false,		false,		false),
		BEGAN		(true,			false,		true,		false),
		CHANGED		(true,			false,		true,		false),
		ENDED		(true,			true,		true,		true),
		CANCELLED	(false,			true,		true,		true),
		FAILED		(false,			true,		false,		true),

		// Only used for discrete gestures like Tap and Swipe
		RECOGNIZED	(true,			true,		true,		true);

		boolean recognized;
		boolean needsReset;
		boolean notifyHandlers;
		boolean finished;

		State(boolean recognized, boolean needsReset, boolean notifyHandlers, boolean finished) {
			this.recognized = recognized;
			this.needsReset = needsReset;
			this.notifyHandlers = notifyHandlers;
			this.finished = finished;
		}
	}

	public interface Delegate {
		/**
		 * Called when the gesture recognizer attempts to change it's state from
		 * State.POSSIBLE to a recognized state.
		 *
		 * Returning true will allow the gesture recognizer to continue handling
		 * touches.
		 *
		 * Returning false will cause the gesture recognizer to switch to State.FAILED
		 *
		 * Default is true
		 *
		 * @param gestureRecognizer gesture recognizer checking if it should begin
		 * @return whether or not the recognizer should begin
		 */
		public boolean shouldBegin(GestureRecognizer gestureRecognizer);

		/**
		 * Called right before the gesture recognizer calls #touchesBegan()
		 *
		 * Returning true will allow the gesture recognizer to handle the touch
		 *
		 * Returning false will stop the gesture recognizer from ever seeing the touch
		 *
		 * Default is true
		 *
		 * @param gestureRecognizer gesture recognizer checking to see if it can recieve
		 *                          the touch
		 * @param touch Touch to be received by the gesture recognizer
		 * @return whether or not a gesture recognizer can receive the touch
		 */
		public boolean shouldReceiveTouch(GestureRecognizer gestureRecognizer, Touch touch);

		/**
		 * Called when one of the gesture recognizers attempts to recognize it's gesture
		 * that would prevent the other from recognizing it's own gesture.
		 *
		 * Returning true will guarantee both gesture recognizers can recognizer their
		 * gestures simultaneously.
		 *
		 * Returning false does not guarantee both gesture recognizers won't recognize
		 * their gestures simultaneously, as the other gesture recognizer's delegate
		 * call return true and allow it.
		 *
		 * Default is false, no two gestures can be recognized at the same time.
		 *
		 * @param gestureRecognizer gesture recognizer who's delegate is being called
		 * @param otherGestureRecognizer other gesture recognizer
		 * @return whether or not the gesture recognizers can recognize their gestures
		 * simultaneously.
		 */
		public boolean shouldRecognizeSimultaneously(GestureRecognizer gestureRecognizer, GestureRecognizer otherGestureRecognizer);
	}

	public interface GestureHandler {
		public void handleGesture(GestureRecognizer gestureRecognizer);
	}

	private Delegate delegate;
	private boolean delaysTouchesBegan;
	private boolean delaysTouchesEnded;
	private boolean cancelsTouchesInView;
	private boolean enabled;
	private State state;
	private View view;
	private List<GestureHandler> registeredGestureHandlers;
	private List<Touch> trackingTouches;
	private List<Touch> ignoredTouches;
	private Event lastEvent;
	private boolean didSetState;
	private Set<WeakReference<GestureRecognizer>> failureRequirements;
	private Set<WeakReference<GestureRecognizer>> failureDependents;
	private boolean failureRequirementsSatisfied;
	private boolean passedPreventionTests;
	private Runnable resetCallback;
	private boolean shouldPerformHapticFeedbackOnTransitionToRecognizedState;
	private boolean shouldPlayClickSoundOnTransitionToRecognizedState;

	public GestureRecognizer() {
		this.state = State.POSSIBLE;
		this.cancelsTouchesInView = true;
		this.delaysTouchesBegan = false;
		this.delaysTouchesEnded = true;
		this.enabled = true;

		this.registeredGestureHandlers = new ArrayList<GestureHandler>();
		this.trackingTouches = new ArrayList<Touch>();
		this.ignoredTouches = new ArrayList<Touch>();

		this.failureRequirements = new HashSet<WeakReference<GestureRecognizer>>();
		this.failureDependents = new HashSet<WeakReference<GestureRecognizer>>();
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
		this.failureRequirements.add(new WeakReference<GestureRecognizer>(gestureRecognizer));
		gestureRecognizer.failureDependents.add(new WeakReference<GestureRecognizer>(this));
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

	public boolean getShouldPerformHapticFeedbackOnTransitionToRecognizedState() {
		return this.shouldPerformHapticFeedbackOnTransitionToRecognizedState;
	}

	public void setShouldPerformHapticFeedbackOnTransitionToRecognizedState(boolean shouldPerformHapticFeedbackOnTransitionToRecognizedState) {
		this.shouldPerformHapticFeedbackOnTransitionToRecognizedState = shouldPerformHapticFeedbackOnTransitionToRecognizedState;
	}

	public boolean getShouldPlayClickSoundOnTransitionToRecognizedState() {
		return this.shouldPlayClickSoundOnTransitionToRecognizedState;
	}

	public void setShouldPlayClickSoundOnTransitionToRecognizedState(boolean shouldPlayClickSoundOnTransitionToRecognizedState) {
		this.shouldPlayClickSoundOnTransitionToRecognizedState = shouldPlayClickSoundOnTransitionToRecognizedState;
	}

	public State getState() {
		return state;
	}

	protected void setState(State state) {
		if(state == null) {
			throw new RuntimeException("You can not set state to null.");
		}

		// Not sure if iOS enforces this, but it makes sense to.
		if(this.state.finished && state != State.POSSIBLE) {
			MWarn("The gesture %s has already finished and can only be set to a POSSIBLE state, setting to %s is not allowed.", this, state);
			return;
		}

		// In testing, iOS doesn't allow you to skip from POSSIBLE to CHANGED
		// when you set state to CHANGED from a POSSIBLE state, it ignores
		// you and sets it to BEGAN anyway.
		if(this.state == State.POSSIBLE && state == State.CHANGED) {
			state = State.BEGAN;
		}

		// iOS doesn't allow you to go backwards, so if your state was changed
		// and you switch to BEGAN, it ignores you and treats it as if you set
		// state to CHANGED
		else if(this.state == State.CHANGED && state == State.BEGAN) {
			state = State.CHANGED;
		}

		// You can only fail from a POSSIBLE state, once you recognize the
		// gesture, iOS treats setting a FAILED state as a CANCELLED state
		else if(state == State.FAILED && this.state != State.POSSIBLE) {
			state = State.CANCELLED;
		}

		// We need to make sure we can transition to a recognized state first
		if(this.state == State.POSSIBLE && state.recognized) {
			// Then check with the delegate, if we have one
			boolean prevented = this.delegate != null && !this.delegate.shouldBegin(this);

			if(!prevented) {
				// If the delegate hasn't prevented us, let's hand it over to
				// the prevention test
				prevented = !this.didPassPreventionTest(this.lastEvent);
			}

			// If we've been prevented, fail and then reset
			if(prevented) {
				this.state = State.FAILED;
				this.notifyDependentsOfFailure();
				this.scheduleReset();
				return;
			}
		}

		State lastState = this.state;
		this.didSetState = true;
		this.state = state;

		boolean shouldNotify = this.shouldNotifyHandlersForState(this.state);

		// Notify handlers if the state wants it AND we're allowed to.
		if (shouldNotify) {
			if(lastState == State.POSSIBLE && this.state.recognized) {
				this.notifyDependentsOfRecognition();
			}

			if(this.state.recognized) {
				this.cancelTouches();
			}

			if(this.state == State.BEGAN || this.state == State.RECOGNIZED) {
				if(this.shouldPerformHapticFeedbackOnTransitionToRecognizedState) {
					this.view.performHapticFeedback();
				}

				if(this.shouldPlayClickSoundOnTransitionToRecognizedState) {
					this.view.playClickSound();
				}
			}

			for(GestureHandler gestureHandler : this.registeredGestureHandlers) {
				gestureHandler.handleGesture(GestureRecognizer.this);
			}
		}

		boolean needsReset = state.needsReset;

		if(needsReset && (state == State.ENDED || state == State.RECOGNIZED)) {
			// We don't want to reset until we can notify, if we can't notify
			// it's because we still have a failure dependency we're waiting on
			needsReset = shouldNotify;
		}

		// Notify failure dependents if we've failed/cancelled
		if(this.state == State.FAILED || this.state == State.CANCELLED) {
			this.notifyDependentsOfFailure();
		}

		if(needsReset) {
			this.scheduleReset();
		}
	}

	public View getView() {
		return this.view;
	}

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

		if(this.resetCallback != null) {
			this.reset();
		}

		this.lastEvent = event;
		this.didSetState = false;

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
		if(!this.didSetState && this.getState() == State.CHANGED && this.shouldNotifyHandlersForState(State.CHANGED)) {
			for(GestureHandler gestureHandler : this.registeredGestureHandlers) {
				gestureHandler.handleGesture(GestureRecognizer.this);
			}
		}
	}

	Event lastEvent() {
		return this.lastEvent;
	}

	private void scheduleReset() {
		this.resetCallback = performOnMainAfterDelay(0, new Runnable() {
			public void run() {
				resetCallback = null;
				reset();
			}
		});
	}

	protected void reset() {
		if(this.lastEvent != null) {
			// Remove from last touches

			for(Touch touch : this.lastEvent.allTouches()) {
				touch.removeGestureRecognizer(this);
			}
		}

		this.state = State.POSSIBLE;
		this.trackingTouches.clear();
		this.ignoredTouches.clear();
		this.failureRequirementsSatisfied = false;
		this.passedPreventionTests = false;

		if(this.resetCallback != null) {
			cancelCallbacks(this.resetCallback);
			this.resetCallback = null;
		}
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

	private boolean shouldNotifyHandlersForState(State state) {
		// iOS always notifies for CANCELLED, regardless of failure requirements
		return state == State.CANCELLED || state.notifyHandlers && this.areFailureRequirementsSatisfied();
	}

	private boolean areFailureRequirementsSatisfied() {
		if(this.failureRequirementsSatisfied) {
			return this.failureRequirementsSatisfied;
		}

		if(this.failureRequirements.size() == 0) {
			return true;
		}

		for(WeakReference<GestureRecognizer> reference : this.failureRequirements) {
			GestureRecognizer gestureRecognizer = reference.get();
			if(gestureRecognizer == null || gestureRecognizer.getView() == null) continue;

			if(gestureRecognizer.state != State.FAILED && gestureRecognizer.state != State.CANCELLED) {
				return false;
			}
		}

		this.failureRequirementsSatisfied = true;
		return true;
	}

	private void failureRequirementDidFail(GestureRecognizer gestureRecognizer, Event event) {
		if(!this.state.recognized || !this.state.notifyHandlers) return;

		if(this.didPassPreventionTest(event)) {
			if(this.state == State.CHANGED || this.state == State.BEGAN) {
				this.notifyHandlersWithTemporaryState(State.BEGAN);
			} else {
				this.notifyHandlersWithTemporaryState(this.state);
			}

			this.notifyDependentsOfRecognition();

			if(this.state.recognized) {
				this.cancelTouches();
			}
		}
	}

	private void failureRequirementWasRecognized(GestureRecognizer gestureRecognizer) {
		this.setState(State.FAILED);
	}

	private void notifyHandlersWithTemporaryState(State state) {
		State restoreState = this.state;
		this.state = state;

		for(GestureHandler gestureHandler : this.registeredGestureHandlers) {
			gestureHandler.handleGesture(GestureRecognizer.this);
		}

		this.state = restoreState;
	}

	private void notifyDependentsOfFailure() {
		for(WeakReference<GestureRecognizer> reference : this.failureDependents) {
			GestureRecognizer gestureRecognizer = reference.get();

			if(gestureRecognizer != null) {
				gestureRecognizer.failureRequirementDidFail(this, this.lastEvent);
			}
		}
	}

	private void notifyDependentsOfRecognition() {
		for(WeakReference<GestureRecognizer> reference : this.failureDependents) {
			GestureRecognizer gestureRecognizer = reference.get();

			if(gestureRecognizer != null) {
				gestureRecognizer.failureRequirementWasRecognized(this);
			}
		}
	}

	private boolean shouldAttemptToRecognize() {
		return (this.enabled && this.state != State.FAILED && this.state != State.CANCELLED && this.state != State.ENDED);
	}

	private boolean didPassPreventionTest(Event event) {
		if(!this.areFailureRequirementsSatisfied()) {
			return true;
		}

		if(this.passedPreventionTests) {
			return true;
		}

		List<Touch> touches = event.getTouchesForGestureRecognizer(this);
		touches.removeAll(this.ignoredTouches);

		List<GestureRecognizer> gestureRecognizers = new ArrayList<GestureRecognizer>();

		for(Touch touch : touches) {
			gestureRecognizers.addAll(touch.getGestureRecognizers());
		}

		gestureRecognizers.remove(this);

		for(GestureRecognizer otherGestureRecognizer : gestureRecognizers) {
			if(!otherGestureRecognizer.state.recognized) continue;

			if(otherGestureRecognizer.canPreventGestureRecognizer(this) && this.canBePreventedByGestureRecognizer(otherGestureRecognizer)) {
				boolean should = this.delegate != null && this.delegate.shouldRecognizeSimultaneously(this, otherGestureRecognizer);
				boolean otherShould = otherGestureRecognizer.delegate != null && otherGestureRecognizer.delegate.shouldRecognizeSimultaneously(otherGestureRecognizer, this);

				if(!should && !otherShould) {
					return false;
				}
			}
		}

		this.passedPreventionTests = true;
		return true;
	}

	private void cancelTouches() {
		if(!this.cancelsTouchesInView) return;

		for(Touch touch : this.trackingTouches) {
			List<Touch> touches = new ArrayList<Touch>();
			touches.add(touch);

			View view = touch.getView();
			if(view.isMultipleTouchEnabled() || view.trackingSingleTouch == touch) {
				view.touchesCancelled(touches, this.lastEvent);

				if(view.trackingSingleTouch == touch) {
					view.trackingSingleTouch = null;
				}
			}
		}
	}

	public String toString() {
		return String.format("<%s: 0x%d; state = %s; view = %s>", this.getClass(), this.hashCode(), this.state, this.view.toString());
	}

}
