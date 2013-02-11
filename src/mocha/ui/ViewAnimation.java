/*
 *  @author Shaun
 *	@date 2/4/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.animation.TimingFunction;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ViewAnimation extends mocha.foundation.Object {
	private static int PROCESS_FRAME = -949484724;
	private static long DESIRED_ANIMATION_FRAME_RATE = (long)((1.0 / 60.0) * 1000.0);
	private static boolean PROFILE_FRAME_RATE = true;

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
	private long startTime;
	private boolean hasStarted;

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
		FRAME, BOUNDS, ALPHA, BACKGROUND_COLOR, CALLBACK_POINT
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
					animation.startValue = view.getBackgroundColor();
					break;
			}
		}

		animation.endValue = endValue;
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
					animation.view.setBackgroundColor(this.interpolate(time, (Integer) animation.startValue, (Integer) animation.endValue));
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

	private static class AnimationHandler extends android.os.Handler {

		public void handleMessage(android.os.Message message) {
			if(message.what == PROCESS_FRAME) {
				long currentTime = android.os.SystemClock.uptimeMillis();
				List<ViewAnimation> animations = new ArrayList<ViewAnimation>(activeAnimations.get());

				for(ViewAnimation animation : animations) {
					if(!animation.hasStarted) {
						animation.hasStarted = true;
						animation.startTime = currentTime;
						animation.onAnimationStart();
					}

					long elapsed = currentTime - animation.startTime;
					float frame = animation.duration > 0 ? (float)elapsed / animation.duration : 1.0f;
					if(frame > 1.0f) frame = 1.0f;
					animation.processFrame(frame);

					if(frame == 1.0f) {
						animation.onAnimationEnd();
						activeAnimations.get().remove(animation);
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

}
