package mocha.ui;

import android.content.Context;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.foundation.NotificationCenter;
import mocha.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

public class WindowLayerNative extends ViewLayerNative implements WindowLayer {

	public WindowLayerNative(Context context) {
		super(context);

		this.getLayout().setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	public Window getWindow() {
		return (Window) this.getView();
	}

	public android.view.View getNativeView() {
		return this.getLayout();
	}

	public void onWindowPause() {

	}

	public void onWindowResume() {

	}

	void updateSize() {

	}

	void updatePosition() {

	}

	private void recursiveLayout(ViewLayerNative layer) {
		layer.updateSize();
		layer.getView()._layoutSubviews();
		layer.setNeedsDisplay();

		for (ViewLayer sublayer : layer.getSublayers()) {
			this.recursiveLayout((ViewLayerNative) sublayer);
		}
	}

	Layout createLayout(Context context) {
		return new WindowLayout(context);
	}

	private class WindowLayout extends Layout {
		private int largestHeight;
		private int lastProposedHeight;
		private int lastKeyboardHeight;
		private boolean hasMeasured;
		private boolean keyboardOpen;
		private Runnable orientationChangeCallback;
		private int orientationChangeWidth;

		private int measuredWidth;
		private int measuredHeight;

		public WindowLayout(Context context) {
			super(context);
		}

		public boolean onTouchEvent(MotionEvent motionEvent) {
			Window window = getWindow();

			Event event = window.getLastEvent();

			if (event == null) {
				event = Event.touchEvent(window);
				window.setLastEvent(event);
			}

			event.updateMotionEvent(motionEvent, window, this);
			window.sendEvent(event);

			return true;
		}

		//
		// TODO: This entire system is a mess and needs to be redone
		// The goal here is to detect keyboard changes along with rotation
		// and properly layout our views, and send the keyboard notifications.
		// As far as I can tell, this is impossibly difficult to do on Android
		// and the current implementation is quite a hack. - Shaun
		//

		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);

			if (changed) {
				MLog("[WINDOW SIZE] onLayout changed, new height: %d, top: %d, left: %d | using: %dx%d", bottom - top, top, left, measuredWidth, measuredHeight);

				// Rect oldFrame = getWindow().getFrame();
				Rect newFrame = new Rect(0.0f, 0.0f, this.measuredWidth / scale, this.measuredHeight / scale);
				newFrame.size.height = FloatMath.ceil(newFrame.size.height);
				newFrame.size.width = FloatMath.ceil(newFrame.size.width);

				getWindow().superSetFrame(newFrame);

				performOnMainAfterDelay(0, new Runnable() {
					public void run() {
						recursiveLayout(WindowLayerNative.this);
					}
				});
			}
		}

		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			final int proposedWidth = MeasureSpec.getSize(widthMeasureSpec);
			final int proposedHeight = MeasureSpec.getSize(heightMeasureSpec);

			this.measuredWidth = proposedWidth;
			this.measuredHeight = proposedHeight;

			if (this.orientationChangeCallback != null) {
				if (proposedWidth == this.orientationChangeWidth) {
					super.onMeasure(widthMeasureSpec, heightMeasureSpec);
					return;
				} else {
					cancelCallbacks(this.orientationChangeCallback);
					this.orientationChangeCallback = null;
				}
			}

			boolean orientationChange = this.hasMeasured && (this.getWidth() != proposedWidth);

			if (orientationChange) {
				this.orientationChangeCallback = performAfterDelay(1000, new Runnable() {
					public void run() {
						if (keyboardOpen) {
							float keyboardHeight = (lastKeyboardHeight / scale);

							Rect beginFrame = getWindow().getBounds();
							beginFrame.origin.y = beginFrame.size.height - keyboardHeight;
							beginFrame.size.height = keyboardHeight;

							Rect endFrame = getWindow().getBounds();
							endFrame.origin.y = endFrame.size.height;
							endFrame.size.height = keyboardHeight;
							final Map<String, Object> info = new HashMap<String, Object>();
							info.put(Window.KEYBOARD_FRAME_BEGIN_USER_INFO_KEY, beginFrame);
							info.put(Window.KEYBOARD_FRAME_END_USER_INFO_KEY, endFrame);
							info.put(Window.KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY, 0L);
							info.put(Window.KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY, View.AnimationCurve.LINEAR);

							NotificationCenter.defaultCenter().post(Window.KEYBOARD_WILL_HIDE_NOTIFICATION, getWindow(), info);
							NotificationCenter.defaultCenter().post(Window.KEYBOARD_DID_HIDE_NOTIFICATION, getWindow(), info);
						}

						lastKeyboardHeight = 0;
						lastProposedHeight = 0;
						largestHeight = 0;
						orientationChangeWidth = 0;
						orientationChangeCallback = null;
						recursiveLayout(WindowLayerNative.this);
					}
				});

				this.orientationChangeWidth = proposedWidth;
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);

				return;
			} else {
				if (!this.hasMeasured) {
					this.hasMeasured = true;
				}
			}

			this.largestHeight = Math.max(this.largestHeight, proposedHeight);

			if (this.lastProposedHeight != proposedHeight) {
				Rect bounds = getWindow().getBounds();
				float beginHeight = this.lastKeyboardHeight / scale;

				if (this.lastProposedHeight > 0 || this.keyboardOpen) {
					final String startNotification;
					final String endNotification;

					if (proposedHeight < this.largestHeight && (this.lastProposedHeight > 0.0f)) {
						startNotification = Window.KEYBOARD_WILL_SHOW_NOTIFICATION;
						endNotification = Window.KEYBOARD_DID_SHOW_NOTIFICATION;

						this.lastKeyboardHeight = this.largestHeight - proposedHeight;
						this.keyboardOpen = true;
						MLog("[WINDOW SIZE] KEYBOARD VISIBLE WITH HEIGHT: %d", this.largestHeight - proposedHeight);
					} else {
						MLog("[WINDOW SIZE] KEYBOARD NOT VISIBLE");
						this.lastKeyboardHeight = 0;
						this.keyboardOpen = false;
						startNotification = Window.KEYBOARD_WILL_HIDE_NOTIFICATION;
						endNotification = Window.KEYBOARD_DID_HIDE_NOTIFICATION;
					}

					float endHeight = this.lastKeyboardHeight / scale;

					Rect beginFrame = new Rect(0.0f, bounds.size.height - beginHeight, bounds.size.width, beginHeight);
					Rect endFrame = new Rect(0.0f, bounds.size.height - endHeight, bounds.size.width, endHeight);

					final Map<String, Object> info = new HashMap<>();
					info.put(Window.KEYBOARD_FRAME_BEGIN_USER_INFO_KEY, beginFrame);
					info.put(Window.KEYBOARD_FRAME_END_USER_INFO_KEY, endFrame);
					info.put(Window.KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY, 0L);
					info.put(Window.KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY, View.AnimationCurve.LINEAR);

					performOnMainAfterDelay(0, new Runnable() {
						public void run() {
							NotificationCenter.defaultCenter().post(startNotification, getWindow(), info);
							NotificationCenter.defaultCenter().post(endNotification, getWindow(), info);

							/*performOnMainAfterDelay(100, new Runnable() {
								@Override
								public void run() {
									recursiveLayout(WindowLayerNative2.this);
								}
							});*/
						}
					});
				}
			}

			this.lastProposedHeight = proposedHeight;

			this.measuredHeight = Math.max(largestHeight, proposedHeight);
			super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(largestHeight, proposedHeight), MeasureSpec.EXACTLY));
		}
	}
}
