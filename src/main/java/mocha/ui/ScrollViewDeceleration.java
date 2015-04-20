package mocha.ui;

import android.util.FloatMath;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import mocha.foundation.MObject;
import mocha.graphics.Point;

class ScrollViewDeceleration extends MObject {
	private static float MINIMUM_VELOCITY = 10.0f;

	// Used to get more granular values from Rebound
	private static float SPRING_END_VALUE = 25.0f;

	private final SpringSystem springSystem = SpringSystem.create();
	private final Spring spring;
	private boolean cancelled;
	private ScrollView target;
	private Point startContentOffset;
	private Point targetContentOffset;
	private final Point reusablePoint = Point.zero();

	ScrollViewDeceleration(ScrollView scrollView) {
		this.target = scrollView;

		this.spring = this.springSystem.createSpring().addListener(new SimpleSpringListener() {
			@Override
			public void onSpringUpdate(Spring spring) {
				setScrollProgress((float) spring.getCurrentValue());
			}

			@Override
			public void onSpringAtRest(Spring spring) {
				if (!cancelled && target.decelerating) {
					target.didEndDecelerating();
				}
			}
		});
	}

	boolean begin() {
		final boolean bounces = this.target.bounces();
		final Point maxPoint = this.target.maxPoint.copy();
		final Point minPoint = this.target.minPoint.copy();
		this.startContentOffset = this.target.getContentOffset();

		if ((this.startContentOffset.x > maxPoint.x || this.startContentOffset.x < minPoint.x) && (this.startContentOffset.y > maxPoint.y || this.startContentOffset.y < minPoint.y)) {
			return false;
		}

		Point velocity = this.target.panGestureRecognizer.velocityInView(this.target);

		if (velocity.isNaN() || velocity.isInfinity()) {
			return false;
		}

		if (!this.target.canScrollVertically) {
			velocity.y = 0;
		}

		if (!this.target.canScrollHorizontally) {
			velocity.x = 0;
		}

		Point velocityAbs = velocity.abs();

		if (velocityAbs.x < MINIMUM_VELOCITY && velocityAbs.y < MINIMUM_VELOCITY) {
			return false;
		}

		final boolean primaryMovementIsY = velocityAbs.y > velocityAbs.x;
		this.spring.setVelocity((primaryMovementIsY ? velocity.y : velocity.x) / 1000.0f);

		velocity.x /= -1000.0f;
		velocity.y /= -1000.0f;


		if (this.target.isPagingEnabled()) {
			final float width = this.target.getBoundsWidth();
			final float height = this.target.getBoundsHeight();

			this.targetContentOffset = new Point();

			if (velocity.x > 0) {
				this.targetContentOffset.x = Math.min(maxPoint.x, FloatMath.ceil(this.startContentOffset.x / width) * width);
			} else {
				this.targetContentOffset.x = Math.max(minPoint.x, FloatMath.floor(this.startContentOffset.x / width) * width);
			}

			if (velocity.y > 0) {
				this.targetContentOffset.y = Math.min(maxPoint.y, FloatMath.ceil(this.target.contentOffset.y / height) * height);
			} else {
				this.targetContentOffset.y = Math.max(minPoint.y, FloatMath.floor(this.target.contentOffset.y / height) * height);
			}

			this.spring.setSpringConfig(SpringConfig.fromBouncinessAndSpeed(5.0, 5.0));
		} else {
			Point adjustment = new Point(MINIMUM_VELOCITY, MINIMUM_VELOCITY);
			adjustment.x /= velocity.x < 0.0f ? -1000.0f : 1000.0f;
			adjustment.y /= velocity.y < 0.0f ? -1000.0f : 1000.0f;

			final float friction = (float) Math.log(this.target.getEffectiveDecelerationRate());
			this.targetContentOffset = new Point(this.target.contentOffset.x - ((velocity.x - adjustment.x) / friction), this.target.contentOffset.y - ((velocity.y - adjustment.y) / friction));

			final boolean overscroll;

			if (bounces) {
				if (primaryMovementIsY) {
					overscroll = this.targetContentOffset.y < minPoint.y || this.targetContentOffset.y > maxPoint.y;
				} else {
					overscroll = this.targetContentOffset.x < minPoint.x || this.targetContentOffset.x > maxPoint.x;
				}
			} else {
				overscroll = false;
			}

			if (overscroll) {
				this.spring.setSpringConfig(new SpringConfig(60, 12.5));
			} else {
				this.spring.setSpringConfig(new SpringConfig(60, 30));
			}
		}

		this.targetContentOffset.x = View.clampf(this.targetContentOffset.x, this.target.minPoint.x, this.target.maxPoint.x);
		this.targetContentOffset.y = View.clampf(this.targetContentOffset.y, this.target.minPoint.y, this.target.maxPoint.y);

		if (this.target.listenerDragging != null) {
			this.target.listenerDragging.willEndDraggingWithVelocityAndTargetContentOffset(this.target, velocity, this.targetContentOffset);
		}

		this.targetContentOffset.x = ScreenMath.round(this.targetContentOffset.x);
		this.targetContentOffset.y = ScreenMath.round(this.targetContentOffset.y);

		if (!this.targetContentOffset.equals(this.startContentOffset)) {
			this.target.decelerating = true;

			if (this.target.listenerDecelerating != null) {
				this.target.listenerDecelerating.willBeginDecelerating(this.target);
			}

			this.spring.setEndValue(SPRING_END_VALUE);

			return true;
		} else {
			return false;
		}
	}

	void cancel() {
		this.cancelled = true;
		this.target.decelerating = false;
		this.spring.destroy();
	}

	void setScrollProgress(float value) {
		if (this.cancelled) return;

		value /= SPRING_END_VALUE;

		this.reusablePoint.x = ScreenMath.round(this.startContentOffset.x + ((this.targetContentOffset.x - this.startContentOffset.x) * value));
		this.reusablePoint.y = ScreenMath.round(this.startContentOffset.y + ((this.targetContentOffset.y - this.startContentOffset.y) * value));

		this.target.setContentOffset(this.reusablePoint, null, 0, true);
	}

}
