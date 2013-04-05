/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;
import mocha.graphics.Size;

import java.util.ArrayList;
import java.util.List;

class ScrollViewAnimation extends mocha.foundation.Object {
	private static float DECELERATION_FRICTION_FACTOR = 0.998f;
	private static long DESIRED_ANIMATION_FRAME_RATE = (long)((1.0 / 60.0) * 1000.0);
	private static float MINIMUM_VELOCITY = 10.0f;
	private static float PENETRATION_DECELERATION = 5.0f;
	private static float PENETRATION_ACCELERATION = 8.0f;
	private static float MIN_VELOCITY_FOR_DECELERATION = 250;
	private static float MIN_VELOCITY_FOR_DECELERATION_WITH_PAGING = 300;
	private static float PAGING_ACCELERATION = 0.00036f;
	private static float PAGING_DECELERATION = 0.9668f;
	private static int PROCESS_FRAME = -249848493;

	private static ThreadLocal<AnimationHandler> animationHandler = new ThreadLocal<AnimationHandler>();
	private static final ThreadLocal<List<ScrollViewAnimation>> activeAnimations = new ThreadLocal<List<ScrollViewAnimation>>() {
		protected List<ScrollViewAnimation> initialValue() {
			return new ArrayList<ScrollViewAnimation>();
		}
	};

	private ScrollView target;
	
	private Point decelerationVelocity;
	private Point minDecelerationPoint;
	private Point maxDecelerationPoint;
	private Point nextPageContentOffset;
	private Point animatedContentOffset;
	private Size adjustedDecelerationFactor;
	private long previousDecelerationFrame;

	// Copied from ScrollView on start for performance
	private boolean pagingEnabled;
	private boolean bounces;
	private Point maxPoint;
	private boolean cancelled;

	ScrollViewAnimation(ScrollView scrollView) {
		this.target = scrollView;
	}

	void startDecelerationAnimation() {
		this.pagingEnabled = target.isPagingEnabled();
		this.bounces = target.bounces();
		this.maxPoint = target.maxPoint.copy();

		if (this.bounces && (target.contentOffset.x > this.maxPoint.x || target.contentOffset.x < 0) && (target.contentOffset.y > this.maxPoint.y || target.contentOffset.y < 0)) {
			return;
		}

		this.decelerationVelocity = target.panGestureRecognizer.velocityInView(target).copy();
		this.decelerationVelocity.x = -this.decelerationVelocity.x;
		this.decelerationVelocity.y = -this.decelerationVelocity.y;
		this.adjustedDecelerationFactor = new Size(DECELERATION_FRICTION_FACTOR, DECELERATION_FRICTION_FACTOR);

		if(this.decelerationVelocity.isNaN() || this.decelerationVelocity.isInfinity()) {
			MLog("Did not detect a valid velocity: %s", this.decelerationVelocity);
			return;
		} else {
			MLog("Detected a valid velocity: %s", this.decelerationVelocity);
		}

		if (!target.canScrollVertically) {
			this.decelerationVelocity.y = 0;
		}

		if (!target.canScrollHorizontally) {
			this.decelerationVelocity.x = 0;
		}

		this.minDecelerationPoint = new Point(0, 0);
		this.maxDecelerationPoint = this.maxPoint.copy();

		if (this.pagingEnabled) {
			Size pageSize = target.getPageSize();
			this.minDecelerationPoint.x = Math.max(0, View.floorf(target.contentOffset.x / pageSize.width) * pageSize.width);
			this.minDecelerationPoint.y = Math.max(0, View.floorf(target.contentOffset.y / pageSize.height) * pageSize.height);
			this.maxDecelerationPoint.x = Math.min(this.maxPoint.x, View.ceilf(target.contentOffset.x / pageSize.width) * pageSize.width);
			this.maxDecelerationPoint.y = Math.min(this.maxPoint.y, View.ceilf(target.contentOffset.y / pageSize.height) * pageSize.height);
		}

		float minimumVelocity = this.pagingEnabled ? MIN_VELOCITY_FOR_DECELERATION_WITH_PAGING : MIN_VELOCITY_FOR_DECELERATION;

		if (Math.abs(this.decelerationVelocity.x) > minimumVelocity || Math.abs(this.decelerationVelocity.y) > minimumVelocity) {
			target.decelerating = true;
			if (this.pagingEnabled) {
				this.nextPageContentOffset = new Point((this.decelerationVelocity.x > 0) ? this.maxDecelerationPoint.x : this.minDecelerationPoint.x, (this.decelerationVelocity.y > 0) ? this.maxDecelerationPoint.y : this.minDecelerationPoint.y);
			} else {
				if(target.listenerDragging != null) {
					this.adjustedDecelerationParameters();
				}
			}

			this.animatedContentOffset = target.contentOffset.copy();
			this.previousDecelerationFrame = android.os.SystemClock.uptimeMillis();

			if(target.listenerDecelerating != null) {
				target.listenerDecelerating.willBeginDecelerating(target);
			}

			for(ScrollViewAnimation animation : activeAnimations.get()) {
				if(animation.target == this.target) {
					throw new RuntimeException("Scroll view already has a deceleration animation");
				}
			}

			activeAnimations.get().add(this);

			AnimationHandler handler = animationHandler.get();

			if(handler == null) {
				handler = new AnimationHandler();
				animationHandler.set(handler);
			}

			handler.sendEmptyMessage(PROCESS_FRAME);
		} else {
			MLog("Not enough velocity: %s (minimum: %f)", this.decelerationVelocity, minimumVelocity);
		}
	}

	void cancelAnimations() {
		this.cancelled = true;
		this.stopDecelerationAnimation();
	}

	private void adjustedDecelerationParameters() {
		Point velocity = new Point(this.decelerationVelocity.x / 1000, this.decelerationVelocity.y / 1000);
		Point a = new Point((this.decelerationVelocity.x < 0 ? -MINIMUM_VELOCITY : MINIMUM_VELOCITY) / 1000, (this.decelerationVelocity.y < 0 ? -MINIMUM_VELOCITY : MINIMUM_VELOCITY) / 1000);
		float d = (float)Math.log(DECELERATION_FRICTION_FACTOR);
		Point targetContentOffset = new Point(target.contentOffset.x - (velocity.x - a.x) / d, target.contentOffset.y - (velocity.y - a.y) / d);
		targetContentOffset.x = View.clampf(targetContentOffset.x, this.minDecelerationPoint.x, this.maxDecelerationPoint.x);
		targetContentOffset.y = View.clampf(targetContentOffset.y, this.minDecelerationPoint.y, this.maxDecelerationPoint.y);

		Point originalTargetContentOffset = targetContentOffset.copy();

		if(target.listenerDragging != null) {
			target.listenerDragging.willEndDraggingWithVelocityAndTargetContentOffset(target, velocity, targetContentOffset);
		}

		if (originalTargetContentOffset.equals(targetContentOffset)) {
			return;
		}

		Point e = new Point(targetContentOffset.x - target.contentOffset.x, targetContentOffset.y - target.contentOffset.y);

		if ((velocity.x <= 0 && targetContentOffset.x < originalTargetContentOffset.x) || (velocity.x >= 0 && targetContentOffset.x > originalTargetContentOffset.x)) {
			this.decelerationVelocity.x = (a.x - d * e.x) * 1000;
		} else {
			this.adjustedDecelerationFactor.width = Math.min(MIN_VELOCITY_FOR_DECELERATION, (float)Math.exp(-(velocity.x - a.x) / e.x));
		}

		if ((velocity.y <= 0 && targetContentOffset.y < originalTargetContentOffset.y) || (velocity.y >= 0 && targetContentOffset.y > originalTargetContentOffset.y)) {
			this.decelerationVelocity.y = (a.y - d * e.y) * 1000;
		} else {
			this.adjustedDecelerationFactor.height = Math.min(MIN_VELOCITY_FOR_DECELERATION, (float)Math.exp(-(velocity.y - a.y) / e.y));
		}
	}

	private void stopDecelerationAnimation() {
		target.decelerating = false;
		activeAnimations.get().remove(this);
	}

	private void stepThroughDecelerationAnimation() {
		if(cancelled) return;

		if (!target.decelerating) {
			activeAnimations.get().remove(this);
			return;
		}

		long currentTime = android.os.SystemClock.uptimeMillis();
		long frameDelta = currentTime - this.previousDecelerationFrame;

		Point offset = this.animatedContentOffset.copy();

		if (this.pagingEnabled) {
			for (long frame = 0; frame < frameDelta; frame++) {
				this.decelerationVelocity.x += 1000 * (PAGING_ACCELERATION * (this.nextPageContentOffset.x - offset.x));
				this.decelerationVelocity.x *= PAGING_DECELERATION;
				offset.x = offset.x + (this.decelerationVelocity.x / 1000);

				this.decelerationVelocity.y += 1000 * (PAGING_ACCELERATION * (this.nextPageContentOffset.y - offset.y));
				this.decelerationVelocity.y *= PAGING_DECELERATION;
				offset.y = offset.y + (this.decelerationVelocity.y / 1000);
			}
		} else {
			Size m = this.adjustedDecelerationFactor;
			Size n = new Size((float)Math.exp(Math.log(m.width) * frameDelta), (float)Math.exp(Math.log(m.height) * frameDelta));

			Size l = new Size(m.width * ((1 - n.width) / (1 - m.width)), m.height * ((1 - n.height) / (1 - m.height)));
			offset.x += (this.decelerationVelocity.x / 1000) * l.width;
			offset.y += (this.decelerationVelocity.y / 1000) * l.height;
			this.decelerationVelocity.x *= n.width;
			this.decelerationVelocity.y *= n.height;
		}

		if (!this.bounces) {
			float boundX = View.clampf(offset.x, 0, this.maxPoint.x);
			if (boundX != offset.x) {
				offset.x = boundX;
				this.decelerationVelocity.x = 0;
			}

			float boundY = View.clampf(offset.y, 0, this.maxPoint.y);
			if (boundY != offset.y) {
				offset.y = boundY;
				this.decelerationVelocity.y = 0;
			}
		}

		this.animatedContentOffset = offset;

		if (target.contentOffset.x != View.roundf(offset.x) || target.contentOffset.y != View.roundf(offset.y)) {
			target.setContentOffset(offset, null, 0, true);
		}

		float absVelocityX = Math.abs(this.decelerationVelocity.x);
		float absVelocityY = Math.abs(this.decelerationVelocity.y);
		boolean a = (absVelocityX <= MINIMUM_VELOCITY && absVelocityY <= MINIMUM_VELOCITY);
		boolean o = (!this.pagingEnabled && a);
		boolean j = this.pagingEnabled && a && (Math.abs(this.nextPageContentOffset.x - offset.x) <= 1) && (Math.abs(this.nextPageContentOffset.y - offset.y) <= 1);

		if (o || j) {
			this.decelerationAnimationCompleted();
			return;
		}

		if (!this.pagingEnabled && this.bounces) {
			Point p = new Point(0, 0);

			if (offset.x < this.minDecelerationPoint.x) {
				p.x = this.minDecelerationPoint.x - offset.x;
			} else {
				if (offset.x > this.maxDecelerationPoint.x) {
					p.x = this.maxDecelerationPoint.x - offset.x;
				}
			}
			if (offset.y < this.minDecelerationPoint.y) {
				p.y = this.minDecelerationPoint.y - offset.y;
			} else {
				if (offset.y > this.maxDecelerationPoint.y) {
					p.y = this.maxDecelerationPoint.y - offset.y;
				}
			}

			if (p.x != 0) {
				if (p.x * this.decelerationVelocity.x <= 0) {
					this.decelerationVelocity.x += p.x * PENETRATION_DECELERATION;
				} else {
					this.decelerationVelocity.x = p.x * PENETRATION_ACCELERATION;
				}
			}
			if (p.y != 0) {
				if (p.y * this.decelerationVelocity.y <= 0) {
					this.decelerationVelocity.y += p.y * PENETRATION_DECELERATION;
				} else {
					this.decelerationVelocity.y = p.y * PENETRATION_ACCELERATION;
				}
			}

			if(this.decelerationVelocity.isNaN()) {
				this.stopDecelerationAnimation();
				return;
			}
		}

		this.previousDecelerationFrame = currentTime;
	}

	private void decelerationAnimationCompleted() {
		this.stopDecelerationAnimation();
		target.hideScrollIndicators();

		if (this.pagingEnabled) {
			Size a = target.getPageSize();
			target.setContentOffset(new Point(View.roundf(target.contentOffset.x / a.width) * a.width, View.roundf(target.contentOffset.y / a.height) * a.height), false);
		}

		target.snapContentOffsetToBounds(false);

		if(target.listenerDecelerating != null) {
			target.listenerDecelerating.didEndDecelerating(target);
		}
	}

	private static class AnimationHandler extends android.os.Handler {

		public void handleMessage(android.os.Message message) {
			if(message.what == PROCESS_FRAME) {
				long currentTime = android.os.SystemClock.uptimeMillis();
				ArrayList<ScrollViewAnimation> animations = new ArrayList<ScrollViewAnimation>(activeAnimations.get());

				for(ScrollViewAnimation animation : animations) {
					animation.stepThroughDecelerationAnimation();
				}

				if(activeAnimations.get().size() > 0) {
					this.sendEmptyMessageDelayed(PROCESS_FRAME, Math.max(0, DESIRED_ANIMATION_FRAME_RATE - android.os.SystemClock.uptimeMillis() - currentTime));
				} else {
					MLog("Appears to be out of active animations??");
				}
			}
		}

	}
}
