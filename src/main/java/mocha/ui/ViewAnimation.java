/*
 *  @author Shaun
 *	@date 2/4/13
 *	@copyright	2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.FloatMath;
import android.view.ViewGroup;
import mocha.animation.TimingFunction;
import mocha.foundation.MObject;
import mocha.graphics.AffineTransform;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.*;
import java.util.concurrent.Semaphore;

class ViewAnimation extends MObject {
	private static final long DESIRED_ANIMATION_FRAME_RATE = 17; // 1sec / 16.667ms = 60fps, 1sec / 17 = 58.8fps
	private static final boolean PROFILE_FRAME_RATE = false;

	// We use a hash so if the same property is changed multiple times in the animation
	// we just override the last one.
	private Map<String,Animation> animations;
	private long startTimeDelayed;
	private long startTime;
	private boolean hasStarted;
	private boolean isCancelled;
	private boolean hasEnded;
	private boolean hasBeenAddedToActiveAnimations;

	// Debug
	private int frameCount;
	private long sinceLastFrameStart;
	private long sinceLastFrameEnd;

	// Animation info
	long duration = 200;
	long delay = 0;
	View.AnimationCurve animationCurve = View.AnimationCurve.MATERIAL;
	TimingFunction timingFunction;
	View.AnimationWillStart willStart;
	View.AnimationDidStop didStop;
	String animationID;
	Object context;

	// Repeat info
	boolean repeats;
	boolean reverses;
	boolean reversing;
	double repeatCount;


	enum Type {
		FRAME(0), BOUNDS(1), ALPHA(2), BACKGROUND_COLOR(3), TRANSFORM(4), CALLBACK_POINT(5), CALLBACK_EDGE_INSETS(6);

		final int value;

		private Type(int value) {
			this.value = value;
		}
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
		boolean shouldUseHardwareLayer;
		boolean shouldRestoreToOriginalLayerType;
		int originalLayerType;

		public void setEndValue(Object endValue) {
			if(this.type == Type.BACKGROUND_COLOR) {
				this.endValue = Color.components((Integer)endValue);
			} else if(type == Type.TRANSFORM) {
				this.endValue = new AffineTransformHolder((AffineTransform)endValue);
			} else {
				this.endValue = endValue;
			}
		}
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

			switch (type) {
				case FRAME:
					animation.startValue = view.getFrame();
					break;
				case BOUNDS:
					animation.startValue = view.getBounds();
					break;
				case ALPHA:
					animation.startValue = view.getAlpha();
					animation.shouldUseHardwareLayer = true;
					break;
				case BACKGROUND_COLOR:
					animation.startValue = Color.components(view.getBackgroundColor());
					break;
				case TRANSFORM:
					animation.startValue = new AffineTransformHolder(view.getTransform());
					break;
			}
		}

		animation.setEndValue(endValue);

		if(!animation.startValue.equals(animation.endValue)) {
			this.animations.put(key, animation);
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

		animation.setEndValue(endValue);
		animation.processFrameCallback = processFrameCallback;
	}

	void start() {
		if(hasBeenAddedToActiveAnimations) return;
		hasBeenAddedToActiveAnimations = true;

		if(this.animations == null || this.animations.size() == 0) {
			if(willStart != null) willStart.animationWillStart(animationID, context);
			if(didStop != null) didStop.animationDidStop(animationID, true, context);
			return;
		}

		if(this.timingFunction == null) {
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
					this.timingFunction = TimingFunction.EASE_IN_OUT;
					break;
				case MATERIAL:
				default:
					this.timingFunction = TimingFunction.MATERIAL;
					break;
			}
		}

		if(this.reverses) {
			if(this.repeatCount <= 0.0) {
				this.repeatCount = 1.0;
			}
		}

		if(this.repeatCount > 0.0) {
			this.repeats = true;
		}

		long timeModifier = View.SLOW_ANIMATIONS ? 10 : 1;

		this.duration *= timeModifier;
		this.delay *= timeModifier;


		Set<String> strings = this.animations.keySet();
		String[] keys = strings.toArray(new String[strings.size()]);
		ViewAnimationHandler.cancelAnimationsForKeys(keys, this);

		for(Animation animation : this.animations.values()) {
			animation.view.animations[animation.type.value] = this;
		}

		ViewAnimationHandler.addAnimation(this);
	}

	boolean changeEndValueForAnimationType(View view, Type type, Object endValue) {
		if(!this.isCancelled) {
			String key = view.hashCode() + "-" + type.toString();
			Animation animation = this.animations.get(key);

			if(animation != null) {
				animation.setEndValue(endValue);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
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

		boolean reverse = this.reverses && this.reversing;

		// MLog("Running for %f", time);
		for(Animation animation : this.animations.values()) {
			// MLog("Running %s on %s", animation.type, animation.view);

			final float frame = this.timingFunction.solve(time, this.duration / 1000.0f);

			if(animation.view.isHidden() && frame != 0.0f && frame !=  1.0f) {
				continue;
			}

			Object startValue;
			Object endValue;

			if(reverse) {
				startValue = animation.endValue;
				endValue = animation.startValue;
			} else {
				startValue = animation.startValue;
				endValue = animation.endValue;
			}

			animation.view.animationIsSetting = true;
			switch (animation.type) {
				case FRAME:
					animation.view.setFrame(this.interpolate(frame, (Rect)startValue, (Rect)endValue));
					break;
				case BOUNDS:
					animation.view.setBounds(this.interpolate(frame, (Rect) startValue, (Rect) endValue));
					break;
				case ALPHA:
					animation.view.setAlpha(this.interpolate(frame, (Float) startValue, (Float) endValue));
					break;
				case BACKGROUND_COLOR:
					int[] startColor = (int[])startValue;
					int[] endColor = (int[])endValue;

					int red = this.interpolate(frame, startColor[0], endColor[0]);
					int green = this.interpolate(frame, startColor[1], endColor[1]);
					int blue = this.interpolate(frame, startColor[2], endColor[2]);
					int alpha = this.interpolate(frame, startColor[3], endColor[3]);

					animation.view.setBackgroundColor(Color.rgba(red, green, blue, alpha));
					break;
				case TRANSFORM:
					AffineTransformHolder startTransform = (AffineTransformHolder)startValue;
					AffineTransformHolder endTransform = (AffineTransformHolder)endValue;
					AffineTransform transform;

					if(time <= 0.0f) {
						transform = startTransform.transform;
					} else if(time >= 1.0f) {
						transform = endTransform.transform;
					} else {
						transform = AffineTransformHelper.interpolate(this.timingFunction, frame, startTransform.decomposed, endTransform.decomposed);
					}

					animation.view.setTransform(transform);
					break;
				case CALLBACK_POINT:
					animation.processFrameCallback.processFrame(this.interpolate(frame, (Point)startValue, (Point)endValue));
					break;
				case CALLBACK_EDGE_INSETS:
					animation.processFrameCallback.processFrame(this.interpolate(frame, (EdgeInsets)startValue, (EdgeInsets)endValue));
					break;
			}
			animation.view.animationIsSetting = false;
		}

//		if(PROFILE_FRAME_RATE) {
//			this.sinceLastFrameEnd = android.os.SystemClock.uptimeMillis();
//		}
	}

	private Rect interpolate(float frame, Rect start, Rect end) {
		Rect rect = start.copy();

		if(start.origin.x != end.origin.x) {
			rect.origin.x = this.timingFunction.interpolate(frame, start.origin.x, end.origin.x);
		}

		if(start.origin.y != end.origin.y) {
			rect.origin.y = this.timingFunction.interpolate(frame, start.origin.y, end.origin.y);
		}

		if(start.size.width != end.size.width) {
			rect.size.width = this.timingFunction.interpolate(frame, start.size.width, end.size.width);
		}

		if(start.size.height != end.size.height) {
			rect.size.height = this.timingFunction.interpolate(frame, start.size.height, end.size.height);
		}

		return rect;
	}

	private Point interpolate(float frame, Point start, Point end) {
		Point point = start.copy();

		if(start.x != end.x) {
			point.x = this.timingFunction.interpolate(frame, start.x, end.x);
		}

		if(start.y != end.y) {
			point.y = this.timingFunction.interpolate(frame, start.y, end.y);
		}

		return point;
	}

	private EdgeInsets interpolate(float frame, EdgeInsets start, EdgeInsets end) {
		EdgeInsets insets = start.copy();

		if(start.top != end.top) {
			insets.top = this.timingFunction.interpolate(frame, start.top, end.top);
		}

		if(start.left != end.left) {
			insets.left = this.timingFunction.interpolate(frame, start.left, end.left);
		}

		if(start.bottom != end.bottom) {
			insets.bottom = this.timingFunction.interpolate(frame, start.bottom, end.bottom);
		}

		if(start.right != end.right) {
			insets.right = this.timingFunction.interpolate(frame, start.right, end.right);
		}

		return insets;
	}

	private float interpolate(float frame, float start, float end) {
		if(start == end) {
			return start;
		} else {
			return this.timingFunction.interpolate(frame, start, end);
		}
	}

	private int interpolate(float frame, int start, int end) {
		return (int)this.interpolate(frame, (float)start, (float)end);
	}

	/// New implementation

	private void onAnimationStart() {
		if (willStart != null) {
			willStart.animationWillStart(animationID, context);
		}

		for(Animation animation : this.animations.values()) {
			if(animation.shouldUseHardwareLayer) {
				ViewGroup viewGroup = animation.view.getLayer().getViewGroup();

				if(viewGroup != null) {
					int type = viewGroup.getLayerType();

					if(type != android.view.View.LAYER_TYPE_HARDWARE) {
						animation.originalLayerType = type;
						animation.shouldRestoreToOriginalLayerType = true;
						viewGroup.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
					}
				}
			}
		}

		frameCount = 0;
	}

	private void onAnimationEnd() {
		this.hasEnded = true;

		long endTime = android.os.SystemClock.uptimeMillis();
		long elapsed = endTime-startTime;
		double mspf = (double)elapsed / (double)frameCount;
		double fps = 1000 / mspf;

		if(PROFILE_FRAME_RATE) {
			MObject.MLog("duration: %sms | elapsed: %sms | frames: %d | fps: %s", duration, elapsed, frameCount, fps);
		}

		this.restoreLayerTypes();
		this.cleanAnimationReferences();

		if (didStop != null) {
			didStop.animationDidStop(animationID, true, context);
		}
	}

	private void onAnimationCancel() {
		this.hasEnded = true;

		this.restoreLayerTypes();
		this.cleanAnimationReferences();

		if (didStop != null) {
			didStop.animationDidStop(animationID, false, context);
		}
	}

	private void cleanAnimationReferences() {
		for(Animation animation : this.animations.values()) {
			if(animation.view.animations[animation.type.value] == this) {
				animation.view.animations[animation.type.value] = null;
			}
		}
	}

	private void restoreLayerTypes() {
		for(Animation animation : this.animations.values()) {
			if(animation.shouldRestoreToOriginalLayerType) {
				ViewGroup viewGroup = animation.view.getLayer().getViewGroup();

				if(viewGroup != null) {
					viewGroup.setLayerType(animation.originalLayerType, null);
				}
			}
		}
	}

	static void cancelAllAnimationsReferencingView(View view) {
		ViewAnimationHandler.cancelAllAnimationsReferencingView(view);
	}

	private static class ViewAnimationHandler {
		private static Semaphore lock = new Semaphore(1);
		private static Handler handler;
		private static boolean scheduled;
		private static final List<ViewAnimation> activeAnimations = new ArrayList<ViewAnimation>();
		private static boolean isProcessing;
		private static long lastFrameTime;
		private static Runnable processor = new Runnable() {
			public void run() {
				lock.acquireUninterruptibly();
				{
					isProcessing = true;
					ViewAnimationHandler.run();
					isProcessing = false;
				}
				lock.release();
			}
		};

		static Handler getHandler() {
			if(handler == null) {
				handler = new Handler(Looper.getMainLooper());
			}

			return handler;
		}

		static void addAnimation(final ViewAnimation animation) {
			if(isProcessing) {
				getHandler().postAtFrontOfQueue(new Runnable() {
					public void run() {
						_addAnimation(animation);
					}
				});
			} else {
				_addAnimation(animation);
			}
		}

		private static void _addAnimation(ViewAnimation animation) {
			lock.acquireUninterruptibly();
			{
				activeAnimations.add(animation);
			}
			lock.release();


			if(!scheduled) {
				scheduled = true;
				getHandler().postAtFrontOfQueue(processor);
			}
		}

		static synchronized void cancelAllAnimationsReferencingView(final View view) {
			if(handler == null) return; // Nothings scheduled!

			if(isProcessing) {
				handler.postAtFrontOfQueue(new Runnable() {
					public void run() {
						_cancelAllAnimationsReferencingView(view);
					}
				});
			} else {
				_cancelAllAnimationsReferencingView(view);
			}
		}

		static synchronized void _cancelAllAnimationsReferencingView(View view) {
			lock.acquireUninterruptibly();
			{
				for(ViewAnimation viewAnimation : activeAnimations) {
					if(viewAnimation.isCancelled || viewAnimation.hasEnded) continue;

					for(Animation animation : viewAnimation.animations.values()) {
						if(animation.view == view) {
							viewAnimation.isCancelled = true;
							break;
						}
					}
				}
			}
			lock.release();
		}

		static synchronized void cancelAnimationsForKeys(final String[] keys, final ViewAnimation ignoreAnimation) {
			if(handler == null) return; // Nothings scheduled!

			if(isProcessing) {
				handler.postAtFrontOfQueue(new Runnable() {
					public void run() {
						_cancelAnimationsForKeys(keys, ignoreAnimation);
					}
				});
			} else {
				_cancelAnimationsForKeys(keys, ignoreAnimation);
			}
		}

		static synchronized void _cancelAnimationsForKeys(String[] keys, ViewAnimation ignoreAnimation) {
			lock.acquireUninterruptibly();
			{
				for(ViewAnimation viewAnimation : activeAnimations) {
					if(viewAnimation.isCancelled || viewAnimation.hasEnded || viewAnimation == ignoreAnimation) continue;

					for(String key : keys) {
						if(viewAnimation.animations.containsKey(key)) {
							viewAnimation.animations.remove(key);

							if(viewAnimation.animations.size() == 0) {
								viewAnimation.isCancelled = true;
							}
						}
					}
				}
			}
			lock.release();
		}


		private static void run() {
			long startTime = android.os.SystemClock.uptimeMillis();

			Iterator<ViewAnimation> iterator = activeAnimations.iterator();
			while(iterator.hasNext()) {
				ViewAnimation animation = iterator.next();

				if(animation.isCancelled) {
					animation.onAnimationCancel();
					iterator.remove();
					continue;
				}

				if(!animation.hasStarted) {
					if(animation.delay > 0) {
						if(animation.startTimeDelayed > 0 && startTime > animation.startTimeDelayed) {
							animation.delay = 0;
						} else {
							if(animation.startTimeDelayed == 0) {
								animation.startTimeDelayed = startTime + animation.delay;
							}

							continue;
						}
					}

					animation.hasStarted = true;
					animation.startTime = startTime;
					animation.onAnimationStart();
				}


				long elapsed = startTime - animation.startTime;
				float frame = animation.duration > 0 ? (float)elapsed / animation.duration : 1.0f;
				// MWarn("Processing frame %f", frame);
				if(frame > 1.0f) {
					frame = 1.0f;
				} else if(animation.repeats && animation.repeatCount < 0.5) {
					if(frame >= animation.repeatCount * 2) {
						frame = 1.0f;
					}
				}

				animation.processFrame(frame);

				if(frame == 1.0f) {
					if(animation.repeats && animation.repeatCount > 0) {
						animation.startTime = startTime;
						animation.repeatCount -= 0.5;

						if(animation.reverses) {
							animation.reversing = !animation.reversing;
						}
					} else {
						iterator.remove();
						animation.onAnimationEnd();
					}
				}
			}

			if(activeAnimations.size() > 0) {
				long since = lastFrameTime == 0 ? startTime : lastFrameTime;
				long now = android.os.SystemClock.uptimeMillis();
				long delay = (DESIRED_ANIMATION_FRAME_RATE - (now - since));

				if(View.SHOW_DROPPED_ANIMATION_FRAMES && delay < 0) {
					MWarn("Dropping frames! Last frame took %dms, should not take more than %dms. (%dms behind)", (now - since), DESIRED_ANIMATION_FRAME_RATE, Math.abs(delay));
				}

				handler.postDelayed(processor, Math.max(0, delay));
				lastFrameTime = now;
			} else {
				scheduled = false;
				lastFrameTime = 0;
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

		public static AffineTransform interpolate(TimingFunction timingFunction, float frame, AffineTransform start, AffineTransform end) {
			Decomposed srA = new Decomposed();
			Decomposed srB = new Decomposed();

			decompose(start, srA);
			decompose(end, srB);

			return interpolate(timingFunction, frame, srA, srB);
		}

		// Modified from the WebKit version for use with our timing functions.
		public static AffineTransform interpolate(TimingFunction timingFunction, float frame, Decomposed start, Decomposed end) {
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
				start.scaleX = timingFunction.interpolate(frame, start.scaleX, end.scaleX);
			}

			if(start.scaleY != end.scaleY) {
				start.scaleY = timingFunction.interpolate(frame, start.scaleY, end.scaleY);
			}

			if(start.angle != end.angle) {
				start.angle = timingFunction.interpolate(frame, start.angle, end.angle);
			}

			if(start.remainderA != end.remainderA) {
				start.remainderA = timingFunction.interpolate(frame, start.remainderA, end.remainderA);
			}

			if(start.remainderB != end.remainderB) {
				start.remainderB = timingFunction.interpolate(frame, start.remainderB, end.remainderB);
			}

			if(start.remainderC != end.remainderC) {
				start.remainderC = timingFunction.interpolate(frame, start.remainderC, end.remainderC);
			}

			if(start.remainderC != end.remainderC) {
				start.remainderC = timingFunction.interpolate(frame, start.remainderC, end.remainderC);
			}

			if(start.translateX != end.translateX) {
				start.translateX = timingFunction.interpolate(frame, start.translateX, end.translateX);
			}

			if(start.translateY != end.translateY) {
				start.translateY = timingFunction.interpolate(frame, start.translateY, end.translateY);
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
