/*
 *  @author Shaun
 *	@date 2/4/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import mocha.animation.TimingFunction;
import mocha.graphics.AffineTransform;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ViewAnimation extends mocha.foundation.Object {
	private static int PROCESS_FRAME = -949484724;
	private static long DESIRED_ANIMATION_FRAME_RATE = (long)((1.0 / 60.0) * 1000.0);
	private static boolean PROFILE_FRAME_RATE = false;

	private static ThreadLocal<AnimationHandler> animationHandler = new ThreadLocal<AnimationHandler>();
	private static final ThreadLocal<ArrayList<ViewAnimation>> activeAnimations = new ThreadLocal<ArrayList<ViewAnimation>>() {
		protected ArrayList<ViewAnimation> initialValue() {
			return new ArrayList<ViewAnimation>();
		}
	};

	// We use a hash so if the same property is changed multiple times in the animation
	// we just override the last one.
	private HashMap<String,Animation> animations;
	private TimingFunction timingFunction;
	private long startTimeDelayed;
	private long startTime;
	private boolean hasStarted;
	private boolean isCancelled;

	// Debug
	private int frameCount;
	private long sinceLastFrameStart;
	private long sinceLastFrameEnd;

	// Animation info
	long duration = 200;
	long delay = 0;
	View.AnimationCurve animationCurve = View.AnimationCurve.EASE_IN_OUT;
	View.AnimationDidStart didStart;
	View.AnimationDidStop didStop;
	String animationID;
	Object context;


	enum Type {
		FRAME, BOUNDS, ALPHA, BACKGROUND_COLOR, TRANSFORM, CALLBACK_POINT
	}

	interface ProcessFrameCallback {
		void processFrame(Object value);
	}

	static class Animation {
		View view;
		Type type;
		Object startValue;
		Object endValue;
		private ProcessFrameCallback processFrameCallback;
	}

	/**
	 * Add an animation to the view
	 *
	 * @param view view the animation should be applied to
	 * @param type type of animation
	 * @param endValue end value for this animation, start value will be grabbed from current state.
	 *                 endValue type is expected to be correct, an exception will be thrown if it's wrong
	 */
	void addAnimation(View view, Type type, Object endValue) {
		if(this.animations == null) {
			this.animations = new HashMap<String, Animation>();
		}

		String key = view.hashCode() + "-" + type.toString();

		Animation animation = this.animations.get(key);

		if(animation == null) {
			animation = new Animation();
			animation.view = view;
			animation.type = type;
			this.animations.put(key, animation);

			switch (type) {
				case FRAME:
					animation.startValue = view.getFrame();
					break;
				case BOUNDS:
					animation.startValue = view.getBounds();
					break;
				case ALPHA:
					animation.startValue = view.getAlpha();
					break;
				case BACKGROUND_COLOR:
					animation.startValue = Color.components(view.getBackgroundColor());
					break;
				case TRANSFORM:
					animation.startValue = new AffineTransformHolder(view.getTransform());
					break;
			}
		}

		if(type == Type.BACKGROUND_COLOR) {
			animation.endValue = Color.components((Integer)endValue);
		} else if(type == Type.TRANSFORM) {
			animation.endValue = new AffineTransformHolder((AffineTransform)endValue);
		} else {
			animation.endValue = endValue;
		}
	}

	/**
	 * Use of this method is discouraged unless using a callback type
	 *
	 * @param view view the animation should be applied to
	 * @param type type of animation
	 * @param startValue start value for this animation
	 * @param endValue end value for this animation
	 * @param processFrameCallback If using a callback type, you must provide this
	 */
	void addAnimation(View view, Type type, Object startValue, Object endValue, ProcessFrameCallback processFrameCallback) {
		if(this.animations == null) {
			this.animations = new HashMap<String, Animation>();
		}

		String key = view.hashCode() + "-" + type.toString();

		Animation animation = this.animations.get(key);

		if(animation == null) {
			animation = new Animation();
			animation.view = view;
			animation.type = type;
			animation.startValue = startValue;
			this.animations.put(key, animation);
		}

		animation.endValue = endValue;
		animation.processFrameCallback = processFrameCallback;
	}

	void start() {
		if(this.animations == null || this.animations.size() == 0) {
			if(didStart != null) didStart.animationDidStart(animationID, context);
			if(didStop != null) didStop.animationDidStop(animationID, true, context);
			return;
		}

		switch (animationCurve) {
			case EASE_IN:
				this.timingFunction = TimingFunction.EASE_IN;
				break;
			case EASE_OUT:
				this.timingFunction = TimingFunction.EASE_OUT;
				break;
			case LINEAR:
				this.timingFunction = TimingFunction.LINEAR;
				break;
			case EASE_IN_OUT:
			default:
				this.timingFunction = TimingFunction.EASE_IN_OUT;
				break;
		}

		long timeModifier = View.SLOW_ANIMATIONS ? 10 : 1;

		this.duration *= timeModifier;
		this.delay *= timeModifier;

		activeAnimations.get().add(this);

		AnimationHandler handler = animationHandler.get();

		if(handler == null) {
			handler = new AnimationHandler();
			animationHandler.set(handler);
		}

		handler.sendEmptyMessage(PROCESS_FRAME);
	}

	private void processFrame(float time) {
		if(PROFILE_FRAME_RATE) {
//			long start = android.os.SystemClock.uptimeMillis();
//			if(this.sinceLastFrameStart > 0) {
//				MLog("time since last frame started: %s | ended: %s", start - this.sinceLastFrameStart, start-this.sinceLastFrameEnd);
//			}
//
//			this.sinceLastFrameStart = start;
			this.frameCount++;
		}

		// MLog("Running for %f", time);
		for(Animation animation : this.animations.values()) {
			// MLog("Running %s on %s", animation.type, animation.view);

			switch (animation.type) {
				case FRAME:
					animation.view.setFrame(this.interpolate(time, (Rect)animation.startValue, (Rect)animation.endValue));
					break;
				case BOUNDS:
					animation.view.setBounds(this.interpolate(time, (Rect) animation.startValue, (Rect) animation.endValue));
					break;
				case ALPHA:
					animation.view.setAlpha(this.interpolate(time, (Float) animation.startValue, (Float) animation.endValue));
					break;
				case BACKGROUND_COLOR:
					int[] startColor = (int[])animation.startValue;
					int[] endColor = (int[])animation.endValue;

					int red = this.interpolate(time, startColor[0], endColor[0]);
					int green = this.interpolate(time, startColor[1], endColor[1]);
					int blue = this.interpolate(time, startColor[2], endColor[2]);
					int alpha = this.interpolate(time, startColor[3], endColor[3]);

					animation.view.setBackgroundColor(Color.rgba(red, green, blue, alpha));
					break;
				case TRANSFORM:
					AffineTransformHolder startTransform = (AffineTransformHolder)animation.startValue;
					AffineTransformHolder endTransform = (AffineTransformHolder)animation.endValue;
					AffineTransform transform;

					if(time <= 0.0f) {
						transform = startTransform.transform;
					} else if(time >= 1.0f) {
						transform = endTransform.transform;
					} else {
						transform = AffineTransformHelper.interpolate(this.timingFunction, time, this.duration, startTransform.decomposed, endTransform.decomposed);
					}

					animation.view.setTransform(transform);
					break;
				case CALLBACK_POINT:
					animation.processFrameCallback.processFrame(this.interpolate(time, (Point)animation.startValue, (Point)animation.endValue));
					break;
			}
		}

//		if(PROFILE_FRAME_RATE) {
//			this.sinceLastFrameEnd = android.os.SystemClock.uptimeMillis();
//		}
	}

	private Rect interpolate(float time, Rect start, Rect end) {
		Rect rect = start.copy();

		if(start.origin.x != end.origin.x) {
			rect.origin.x = this.timingFunction.interpolate(time, this.duration, start.origin.x, end.origin.x);
		}

		if(start.origin.y != end.origin.y) {
			rect.origin.y = this.timingFunction.interpolate(time, this.duration, start.origin.y, end.origin.y);
		}

		if(start.size.width != end.size.width) {
			rect.size.width = this.timingFunction.interpolate(time, this.duration, start.size.width, end.size.width);
		}

		if(start.size.height != end.size.height) {
			rect.size.height = this.timingFunction.interpolate(time, this.duration, start.size.height, end.size.height);
		}

		return rect;
	}

	private Point interpolate(float time, Point start, Point end) {
		Point point = start.copy();

		if(start.x != end.x) {
			point.x = this.timingFunction.interpolate(time, this.duration, start.x, end.x);
		}

		if(start.y != end.y) {
			point.y = this.timingFunction.interpolate(time, this.duration, start.y, end.y);
		}

		return point;
	}

	private float interpolate(float time, float start, float end) {
		if(start == end) {
			return start;
		} else {
			return this.timingFunction.interpolate(time, this.duration, start, end);
		}
	}

	private int interpolate(float time, int start, int end) {
		return (int)this.interpolate(time, (float)start, (float)end);
	}

	/// New implementation

	private void onAnimationStart() {
		if (didStart != null) {
			didStart.animationDidStart(animationID, context);
		}

		frameCount = 0;
	}

	private void onAnimationEnd() {
		long endTime = android.os.SystemClock.uptimeMillis();
		long elapsed = endTime-startTime;
		double mspf = (double)elapsed / (double)frameCount;
		double fps = 1000 / mspf;

		if(PROFILE_FRAME_RATE) {
			mocha.foundation.Object.MLog("duration: %sms | elapsed: %sms | frames: %d | fps: %s", duration, elapsed, frameCount, fps);
		}

		if (didStop != null) {
			didStop.animationDidStop(animationID, true, context);
		}
	}

	private void onAnimationCancel() {
		if (didStop != null) {
			didStop.animationDidStop(animationID, false, context);
		}
	}

	static void cancelAllAnimationsReferencingView(View view) {
		AnimationHandler handler = animationHandler.get();
		if(handler == null) return;

		List<ViewAnimation> viewAnimations = new ArrayList<ViewAnimation>(activeAnimations.get());
		for(ViewAnimation viewAnimation : viewAnimations) {
			if(viewAnimation.isCancelled) continue;

			for(Animation animation : viewAnimation.animations.values()) {
				if(animation.view == view) {
					viewAnimation.isCancelled = true;
					activeAnimations.get().remove(viewAnimation);
					viewAnimation.onAnimationCancel();
					break;
				}
			}
		}
	}

	private static class AnimationHandler extends android.os.Handler {

		public void handleMessage(android.os.Message message) {
			if(message.what == PROCESS_FRAME) {
				long currentTime = android.os.SystemClock.uptimeMillis();
				List<ViewAnimation> animations = new ArrayList<ViewAnimation>(activeAnimations.get());

				for(ViewAnimation animation : animations) {
					if(animation.isCancelled) continue;

					if(!animation.hasStarted) {
						if(animation.delay > 0) {
							if(animation.startTimeDelayed > 0 && currentTime > animation.startTimeDelayed) {
								animation.delay = 0;
							} else {
								if(animation.startTimeDelayed == 0) {
									animation.startTimeDelayed = currentTime + animation.delay;
								}

								continue;
							}
						}

						animation.hasStarted = true;
						animation.startTime = currentTime;
						animation.onAnimationStart();
					}


					long elapsed = currentTime - animation.startTime;
					float frame = animation.duration > 0 ? (float)elapsed / animation.duration : 1.0f;
					if(frame > 1.0f) frame = 1.0f;
					animation.processFrame(frame);

					if(frame == 1.0f) {
						activeAnimations.get().remove(animation);
						animation.onAnimationEnd();
					}
				}

				if(activeAnimations.get().size() > 0) {
					long delay = Math.max(0, DESIRED_ANIMATION_FRAME_RATE - android.os.SystemClock.uptimeMillis() - currentTime);

					if(delay == 0) {
						this.sendEmptyMessage(PROCESS_FRAME);
					} else {
						this.sendEmptyMessageDelayed(PROCESS_FRAME, delay);
					}
				}
			}
		}

	}

	static class AffineTransformHolder {
		final AffineTransform transform;
		final AffineTransformHelper.Decomposed decomposed;

		AffineTransformHolder(AffineTransform transform) {
			this.transform = transform;
			this.decomposed = new AffineTransformHelper.Decomposed();
			AffineTransformHelper.decompose(transform, this.decomposed);
		}
	}

	static class AffineTransformHelper {
		/**
		 * This code has been ported from WebKit.
		 *
		 * The original code can be found here:
		 * http://trac.webkit.org/browser/trunk/Source/WebCore/platform/graphics/transforms/TransformationMatrix.cpp?rev=143810
		 */

		private static final float PI = (float)Math.PI;

		static class Decomposed implements mocha.foundation.Copying <Decomposed> {
			float scaleX, scaleY;
			float angle;
			float remainderA, remainderB, remainderC, remainderD;
			float translateX, translateY;

			public Decomposed copy() {
				Decomposed decomposed = new Decomposed();
				decomposed.scaleX = this.scaleX;
				decomposed.scaleY = this.scaleY;
				decomposed.angle = this.angle;
				decomposed.remainderA = this.remainderA;
				decomposed.remainderB = this.remainderB;
				decomposed.remainderC = this.remainderC;
				decomposed.remainderD = this.remainderD;
				decomposed.translateX = this.translateX;
				decomposed.translateY = this.translateY;
				return decomposed;
			}
		}

		public static AffineTransform interpolate(TimingFunction timingFunction, float time, long duration, AffineTransform start, AffineTransform end) {
			Decomposed srA = new Decomposed();
			Decomposed srB = new Decomposed();

			decompose(start, srA);
			decompose(end, srB);

			return interpolate(timingFunction, time, duration, srA, srB);
		}

		// Modified from the WebKit version for use with our timing functions.
		public static AffineTransform interpolate(TimingFunction timingFunction, float time, long duration, Decomposed start, Decomposed end) {
			start = start.copy();
			end = end.copy();

			// If x-axis of one is flipped, and y-axis of the other, convert to an unflipped rotation.
			if ((start.scaleX < 0 && end.scaleY < 0) || (start.scaleY < 0 &&  end.scaleX < 0)) {
				start.scaleX = -start.scaleX;
				start.scaleY = -start.scaleY;
				start.angle += start.angle < 0 ? PI : -PI;
			}

			// Don't rotate the long way around.
			start.angle = start.angle % (2 * PI);
			end.angle = end.angle % (2 * PI);

			if (Math.abs(start.angle - end.angle) > PI) {
				if (start.angle > end.angle)
					start.angle -= PI * 2;
				else
					end.angle -= PI * 2;
			}

			if(start.scaleX != end.scaleX) {
				start.scaleX = timingFunction.interpolate(time, duration, start.scaleX, end.scaleX);
			}

			if(start.scaleY != end.scaleY) {
				start.scaleY = timingFunction.interpolate(time, duration, start.scaleY, end.scaleY);
			}

			if(start.angle != end.angle) {
				start.angle = timingFunction.interpolate(time, duration, start.angle, end.angle);
			}

			if(start.remainderA != end.remainderA) {
				start.remainderA = timingFunction.interpolate(time, duration, start.remainderA, end.remainderA);
			}

			if(start.remainderB != end.remainderB) {
				start.remainderB = timingFunction.interpolate(time, duration, start.remainderB, end.remainderB);
			}

			if(start.remainderC != end.remainderC) {
				start.remainderC = timingFunction.interpolate(time, duration, start.remainderC, end.remainderC);
			}

			if(start.remainderC != end.remainderC) {
				start.remainderC = timingFunction.interpolate(time, duration, start.remainderC, end.remainderC);
			}

			if(start.translateX != end.translateX) {
				start.translateX = timingFunction.interpolate(time, duration, start.translateX, end.translateX);
			}

			if(start.translateY != end.translateY) {
				start.translateY = timingFunction.interpolate(time, duration, start.translateY, end.translateY);
			}

			return recompose(start);
		}

		public static boolean decompose(AffineTransform transform, Decomposed decomp) {
			transform = transform.copy();

			// Compute scaling factors
			float sx = xScale(transform);
			float sy = yScale(transform);

			// Compute cross product of transformed unit vectors. If negative,
			// one axis was flipped.
			if (transform.getA() * transform.getD() - transform.getC() * transform.getB() < 0) {
				// Flip axis with minimum unit vector dot product
				if (transform.getA() < transform.getD())
					sx = -sx;
				else
					sy = -sy;
			}

			// Remove scale from matrix
			transform.scale(1.0f / sx, 1.0f / sy);

			// Compute rotation
			float angle = (float)Math.atan2(transform.getB(), transform.getA());

			// Remove rotation from matrix
			transform.rotate(-angle);

			// Return results
			decomp.scaleX = sx;
			decomp.scaleY = sy;
			decomp.angle = angle;
			decomp.remainderA = transform.getA();
			decomp.remainderB = transform.getB();
			decomp.remainderC = transform.getC();
			decomp.remainderD = transform.getD();
			decomp.translateX = transform.getTx();
			decomp.translateY = transform.getTy();

			return true;
		}

		public static AffineTransform recompose(Decomposed decomp) {
			AffineTransform recomposed = new AffineTransform(decomp.remainderA, decomp.remainderB, decomp.remainderC, decomp.remainderD, decomp.translateX, decomp.translateY);
			recomposed.rotate(decomp.angle);
			recomposed.scale(decomp.scaleX, decomp.scaleY);
			return recomposed;
		}

		private static float xScale(AffineTransform transform) {
			return FloatMath.sqrt(transform.getA() * transform.getA() + transform.getB() * transform.getB());
		}

		private static float yScale(AffineTransform transform) {
			return FloatMath.sqrt(transform.getC() * transform.getC() + transform.getD() * transform.getD());
		}

	}


}
