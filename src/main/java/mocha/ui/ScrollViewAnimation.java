/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import mocha.foundation.MObject;
import mocha.graphics.Point;
import mocha.graphics.Size;

import java.util.ArrayList;
import java.util.List;

class ScrollViewAnimation extends MObject {
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
			return new ArrayList<>();
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
	private Point minPoint;
	private boolean cancelled;

	ScrollViewAnimation(ScrollView scrollView) {
		this.target = scrollView;
	}

	void startDecelerationAnimation() {
		this.pagingEnabled = this.target.isPagingEnabled();
		this.bounces = this.target.bounces();
		this.maxPoint = this.target.maxPoint.copy();
		this.minPoint = this.target.minPoint.copy();

		if (this.bounces &&
			(this.target.contentOffset.x > this.maxPoint.x || this.target.contentOffset.x < this.minPoint.x) &&
			(this.target.contentOffset.y > this.maxPoint.y || this.target.contentOffset.y < this.minPoint.y)
		) {
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

		if (!this.target.canScrollVertically) {
			this.decelerationVelocity.y = 0;
		}

		if (!this.target.canScrollHorizontally) {
			this.decelerationVelocity.x = 0;
		}

		this.minDecelerationPoint = this.minPoint.copy();
		this.maxDecelerationPoint = this.maxPoint.copy();

		if (this.pagingEnabled) {
			float pageWidth = this.target.getBoundsWidth();
			float pageHeight = this.target.getBoundsHeight();

			this.minDecelerationPoint.x = Math.max(this.minPoint.x, FloatMath.floor(this.target.contentOffset.x / pageWidth) * pageWidth);
			this.minDecelerationPoint.y = Math.max(this.minPoint.y, FloatMath.floor(this.target.contentOffset.y / pageHeight) * pageHeight);
			this.maxDecelerationPoint.x = Math.min(this.maxPoint.x, FloatMath.ceil(this.target.contentOffset.x / pageWidth) * pageWidth);
			this.maxDecelerationPoint.y = Math.min(this.maxPoint.y, FloatMath.ceil(this.target.contentOffset.y / pageHeight) * pageHeight);
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

			this.animatedContentOffset = this.target.contentOffset.copy();
			this.previousDecelerationFrame = android.os.SystemClock.uptimeMillis();

			if(this.target.listenerDecelerating != null) {
				this.target.listenerDecelerating.willBeginDecelerating(target);
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

		if (target.willEndDraggingWithVelocityAndTargetContentOffset(velocity, targetContentOffset)) {
			if (target.listenerDragging != null) {
				target.listenerDragging.willEndDraggingWithVelocityAndTargetContentOffset(target, velocity, targetContentOffset);
			}
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
			float boundX = View.clampf(offset.x, this.minPoint.x, this.maxPoint.x);
			if (boundX != offset.x) {
				offset.x = boundX;
				this.decelerationVelocity.x = 0;
			}

			float boundY = View.clampf(offset.y, this.minPoint.y, this.maxPoint.y);
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
			float pageWidth = target.getBoundsWidth();
			float pageHeight = target.getBoundsHeight();

			target.setContentOffset(new Point(View.roundf(target.contentOffset.x / pageWidth) * pageWidth, View.roundf(target.contentOffset.y / pageHeight) * pageHeight), false);
		}

		target.didEndDecelerating();
	}

	private static class AnimationHandler extends android.os.Handler {

		public void handleMessage(android.os.Message message) {
			if(message.what == PROCESS_FRAME) {
				long currentTime = android.os.SystemClock.uptimeMillis();
				List<ScrollViewAnimation> animations = new ArrayList<>(activeAnimations.get());

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
