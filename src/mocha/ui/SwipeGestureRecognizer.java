/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import mocha.graphics.Point;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SwipeGestureRecognizer extends GestureRecognizer {
	private EnumSet<Direction> directions;
//	private int numberOfTouchesRequired;
	private VelocityTracker velocityTracker;

	private long startTimestamp;
	private Point startLocation;
	private boolean shouldTrack;

	final static long DEFAULT_MAXIMUM_DURATION = 500;
	final static float DEFAULT_MINIMUM_PRIMARY_MOVEMENT = 50.0f;
	final static float DEFAULT_MAXIMUM_PRIMARY_MOVEMENT = Float.MAX_VALUE;
	final static float DEFAULT_MINIMUM_SECONDARY_MOVEMENT = 0.0f;
	final static float DEFAULT_MAXIMUM_SECONDARY_MOVEMENT = 5.0f;

	private long maximumDuration;
	private float minimumPrimaryMovement;
	private float maximumPrimaryMovement;
	private float minimumSecondaryMovement;
	private float maximumSecondaryMovement;
	private float minimumVelocity;
	private float maximumVelocity;

	public enum Direction {
		RIGHT,
		LEFT,
		UP,
		DOWN
	}

	public SwipeGestureRecognizer(GestureHandler gestureHandler) {
		this(Direction.RIGHT, gestureHandler);
	}


	public SwipeGestureRecognizer(Direction direction, GestureHandler gestureHandler) {
		super(gestureHandler);

		this.setDirection(direction);

		this.maximumDuration = DEFAULT_MAXIMUM_DURATION;
		this.minimumPrimaryMovement = DEFAULT_MINIMUM_PRIMARY_MOVEMENT;
		this.maximumPrimaryMovement = DEFAULT_MAXIMUM_PRIMARY_MOVEMENT;
		this.minimumSecondaryMovement = DEFAULT_MINIMUM_SECONDARY_MOVEMENT;
		this.maximumSecondaryMovement = DEFAULT_MAXIMUM_SECONDARY_MOVEMENT;
	}

	void setView(View view) {
		super.setView(view);

		final ViewConfiguration configuration = ViewConfiguration.get(view.getLayer().getContext());
		this.minimumVelocity = configuration.getScaledMinimumFlingVelocity();
		this.maximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	protected void reset() {
		super.reset();

		if(this.velocityTracker != null) {
			this.velocityTracker.recycle();
			this.velocityTracker = null;
		}
	}

	protected void setState(State state) {
		super.setState(state);

		if(state == State.CANCELLED || state == State.ENDED || state == State.FAILED) {
			this.shouldTrack = false;
		}
	}

	// Properties

	public Direction getDirection() {
		return this.directions.iterator().next();
	}

	public Set<Direction> getDirections() {
		return new HashSet<Direction>(this.directions);
	}

	public void setDirection(Direction direction) {
		this.directions = EnumSet.of(direction);
	}

	public void setDirection(Direction direction, Direction... others) {
		this.directions = EnumSet.of(direction, others);
	}

	// Touches

	private void trackVelocity(Event event) {
		if(this.velocityTracker == null) {
			this.velocityTracker = VelocityTracker.obtain();
		}

		this.velocityTracker.addMovement(event.getMotionEvent());
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		if(this.getState() == State.POSSIBLE) {
			this.shouldTrack = true;
			this.startTimestamp = event.getTimestamp();
			this.startLocation = touches.get(0).location;
			this.trackVelocity(event);
		}
	}

	protected void touchesMoved(List<Touch> touches, Event event) {
		if(!this.shouldTrack) return;

		this.trackVelocity(event);
		this.trackMovement(touches.get(0).location, event.getTimestamp());
	}

	protected void touchesEnded(List<Touch> touches, Event event) {
		if(!this.shouldTrack) return;

		this.trackVelocity(event);
		if(!this.trackMovement(touches.get(0).location, event.getTimestamp())) {
			this.setState(State.FAILED);
		}
	}

	protected void touchesCancelled(List<Touch> touches, Event event) {
		if(!this.shouldTrack) return;
		this.setState(State.CANCELLED);
	}

	// Return value: whether or not a movement has finished and a cancelled/ended state was set
	protected boolean trackMovement(Point location, long timestamp) {
		if(timestamp - this.startTimestamp > maximumDuration) {
			this.setState(State.FAILED);
			return true;
		}

		float deltaX = Math.abs(this.startLocation.x - location.x);
		float deltaY = Math.abs(this.startLocation.y - location.y);

		float primaryMovement;
		float secondaryMovement;
		boolean primaryIsY = deltaY >= deltaX;

		if(primaryIsY) {
			primaryMovement = deltaY;
			secondaryMovement = deltaX;
		} else {
			primaryMovement = deltaX;
			secondaryMovement = deltaY;
		}

		if(primaryMovement > maximumPrimaryMovement) {
			this.setState(State.FAILED);
			return true;
		}

		if(secondaryMovement < minimumSecondaryMovement || secondaryMovement > maximumSecondaryMovement) {
			this.setState(State.FAILED);
			return true;
		}

		if(primaryMovement >= minimumPrimaryMovement) {
			this.velocityTracker.computeCurrentVelocity(1000, this.maximumVelocity);
			float velocity = primaryIsY ? this.velocityTracker.getYVelocity() : this.velocityTracker.getXVelocity();

			if(Math.abs(velocity) > this.minimumVelocity) {
				Direction direction = this.findDirection(primaryIsY, velocity > 0);

				if(this.directions.contains(direction)) {
					this.setState(State.RECOGNIZED);
				} else {
					this.setState(State.FAILED);
				}

				return true;
			}
		}

		return false;
	}

	private Direction findDirection(boolean primaryIsY, boolean positive) {
		if(primaryIsY) {
			if(positive) {
				return Direction.DOWN;
			} else {
				return Direction.UP;
			}
		} else {
			if(positive) {
				return Direction.RIGHT;
			} else {
				return Direction.LEFT;
			}
		}
	}

	@Override
	public String toStringExtra() {
		return "direction = " + directions + "; " + super.toStringExtra();
	}

}
