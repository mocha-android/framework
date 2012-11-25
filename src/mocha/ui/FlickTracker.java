/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import mocha.graphics.Point;

/**
 * Flicks differ from swipes in that they don't require a finite start time/point
 * A flick can happen at the end of any touch as long as the velocity towards the end of
 * the pan was fast enough.  As soon as the touch pans in a different direction
 * the "flick" tracking essentially resets, unlike swipes which fail. This means
 * the a user can pan around for a while, and then flick in any direction, and it will
 * be detected, where as a swipe gesture would fail.
 */
class FlickTracker extends mocha.foundation.Object {

	private VelocityTracker velocityTracker;
	private float minimumVelocity;
	private float maximumVelocity;
	private float minimumPrimaryMovement;
	private float maximumPrimaryMovement;
	private float minimumSecondaryMovement;
	private float maximumSecondaryMovement;
	private Point startLocation;
	private Point lastLocation;
	private Point lastDelta;

	enum Direction {
		NONE,
		LEFT,
		UP,
		RIGHT,
		DOWN
	}

	FlickTracker() {
		this.minimumPrimaryMovement = 25.0f;
		this.maximumPrimaryMovement = Float.MAX_VALUE;
		this.minimumSecondaryMovement = 0.0f;
		this.maximumSecondaryMovement = 50.0f;
	}

	public void trackEvent(Event event) {
		if(event.getType() != Event.Type.TOUCHES || event.allTouches().size() == 0) return;

		Touch touch = event.allTouches().get(0);

		if(this.velocityTracker == null) {
			this.velocityTracker = VelocityTracker.obtain();

			View view = event.allTouches().get(0).getView();
			final ViewConfiguration configuration = ViewConfiguration.get(view.getLayer().getContext());
			this.minimumVelocity = configuration.getScaledMinimumFlingVelocity();
			this.maximumVelocity = configuration.getScaledMaximumFlingVelocity();
		}


		Point delta = touch.getDelta();

		if(startLocation == null) {
			this.setStartLocation(touch.location);
		} else if(delta != null && this.lastDelta != null) {
			boolean directionChangeX = (this.lastDelta.x > 0 && delta.x < 0) || (this.lastDelta.x < 0 && delta.x > 0);
			boolean directionChangeY = (this.lastDelta.y > 0 && delta.y < 0) || (this.lastDelta.y < 0 && delta.y > 0);

			if(directionChangeX || directionChangeY) {
				this.setStartLocation(startLocation);
				MLog("Flick: Reset velocity: " + delta + " diff direction than " + this.lastDelta);
			} else {
				MLog("Flick: Continued velocity");
			}
		}

		this.lastDelta = delta;
		this.lastLocation = touch.location;
		this.velocityTracker.addMovement(event.getMotionEvent());
	}

	public Direction getFlickDirection() {
		float deltaX = Math.abs(this.startLocation.x - this.lastLocation.x);
		float deltaY = Math.abs(this.startLocation.y - this.lastLocation.y);

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

		if(primaryMovement <= maximumPrimaryMovement) {
			MLog("Flick: A");

			if(secondaryMovement >= minimumSecondaryMovement && secondaryMovement <= maximumSecondaryMovement) {
				MLog("Flick: B");

				if(primaryMovement >= minimumPrimaryMovement) {
					MLog("Flick: C");

					this.velocityTracker.computeCurrentVelocity(1000, this.maximumVelocity);
					float velocity = primaryIsY ? this.velocityTracker.getYVelocity() : this.velocityTracker.getXVelocity();

					if(Math.abs(velocity) > this.minimumVelocity) {
						MLog("Flick: D");

						if(primaryIsY) {
							if(velocity > 0) {
								return Direction.DOWN;
							} else {
								return Direction.UP;
							}
						} else {
							if(velocity > 0) {
								return Direction.RIGHT;
							} else {
								return Direction.LEFT;
							}
						}
					} else {
						MLog("Flick velocity: " + Math.abs(velocity) + ", minimum: " + this.minimumVelocity);
					}
				}
			}
		} else {
			MLog("Flick start: " + this.startLocation + ", end: " + this.lastLocation);
		}

		return Direction.NONE;
	}

	private void setStartLocation(Point startLocation) {
		this.startLocation = startLocation;
		this.velocityTracker.clear();
	}

	public void reset() {
		if(this.velocityTracker != null) {
			this.velocityTracker.recycle();
			this.velocityTracker = null;
			this.startLocation = null;
			this.lastLocation = null;
			this.lastDelta = null;
		}
	}

}
