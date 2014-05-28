/**
 *  @author Shaun
 *  @date 4/20/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.util.FloatMath;
import android.view.*;
import mocha.foundation.NotificationCenter;
import mocha.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

public class WindowLayerNative2 extends ViewLayerNative2 implements WindowLayer {

	public WindowLayerNative2(Context context) {
		super(context);

		this.getLayout().setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	public Window getWindow() {
		return (Window)this.getView();
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

	private void recursiveLayout(ViewLayerNative2 layer) {
		layer.updateSize();
		layer.getView()._layoutSubviews();
		layer.setNeedsDisplay();

		for(ViewLayer sublayer : layer.getSublayers()) {
			this.recursiveLayout((ViewLayerNative2)sublayer);
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

		public WindowLayout(Context context) {
			super(context);
		}

		public boolean onTouchEvent(MotionEvent motionEvent) {
			Window window = getWindow();

			Event event = window.getLastEvent();

			if(event == null) {
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

			if(changed) {
				Rect oldFrame = getWindow().getFrame();
				Rect newFrame = new Rect(0.0f, 0.0f, (right - left) / scale, (bottom - top) / scale);
				newFrame.size.height = FloatMath.ceil(newFrame.size.height);
				newFrame.size.width = FloatMath.ceil(newFrame.size.width);

				getWindow().superSetFrame(newFrame);

				if(newFrame.size.width > newFrame.size.height != oldFrame.size.width > oldFrame.size.height) {
					performOnMainAfterDelay(0, new Runnable() {
						public void run() {
							recursiveLayout(WindowLayerNative2.this);
						}
					});
				}
			}
		}

		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			final int proposedWidth = MeasureSpec.getSize(widthMeasureSpec);
			final int proposedHeight = MeasureSpec.getSize(heightMeasureSpec);

			if(this.orientationChangeCallback != null) {
				if(proposedWidth == this.orientationChangeWidth) {
					super.onMeasure(widthMeasureSpec, heightMeasureSpec);
					return;
				} else {
					cancelCallbacks(this.orientationChangeCallback);
					this.orientationChangeCallback = null;
				}
			}

			boolean orientationChange = this.hasMeasured && (this.getWidth() != proposedWidth);

			if(orientationChange) {
				this.orientationChangeCallback = performAfterDelay(1000, new Runnable() {
					public void run() {
						if(keyboardOpen) {
							float keyboardHeight = (lastKeyboardHeight / scale);

							Rect beginFrame = getWindow().getBounds();
							beginFrame.origin.y = beginFrame.size.height - keyboardHeight;
							beginFrame.size.height = keyboardHeight;

							Rect endFrame = getWindow().getBounds();
							endFrame.origin.y = endFrame.size.height;
							endFrame.size.height = keyboardHeight;
							final Map<String,Object> info = new HashMap<String,Object>();
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
						recursiveLayout(WindowLayerNative2.this);
					}
				});

				this.orientationChangeWidth  = proposedWidth;
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);

				return;
			} else {
				if(!this.hasMeasured) {
					this.hasMeasured = true;
				}
			}

			this.largestHeight = Math.max(this.largestHeight, proposedHeight);

			if(this.lastProposedHeight != proposedHeight) {
				Rect bounds = getWindow().getBounds();
				float beginHeight = this.lastKeyboardHeight / scale;

				if(this.lastProposedHeight > 0 || this.keyboardOpen) {
					final String startNotification;
					final String endNotification;

					if(proposedHeight < this.largestHeight && (this.lastProposedHeight > 0.0f)) {
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

					final Map<String,Object> info = new HashMap<>();
					info.put(Window.KEYBOARD_FRAME_BEGIN_USER_INFO_KEY, beginFrame);
					info.put(Window.KEYBOARD_FRAME_END_USER_INFO_KEY, endFrame);
					info.put(Window.KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY, 0L);
					info.put(Window.KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY, View.AnimationCurve.LINEAR);

					performOnMainAfterDelay(0, new Runnable() {
						public void run() {
							NotificationCenter.defaultCenter().post(startNotification, getWindow(), info);
							NotificationCenter.defaultCenter().post(endNotification, getWindow(), info);

							performOnMainAfterDelay(100, new Runnable() {
								@Override
								public void run() {
									recursiveInvalidate(getWindow());
								}
							});
						}
					});
				}
			}

			this.lastProposedHeight = proposedHeight;

			super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(largestHeight, proposedHeight), MeasureSpec.EXACTLY));
		}
	}

	// Fixes an odd bug on certain devices/os versions
	// that causes views to randomly go blank on keyboard
	// changes.
	private void recursiveInvalidate(View view) {
		view.setNeedsDisplay();
		view.setNeedsLayout();

		for(View subview : view.getSubviews()) {
			this.recursiveInvalidate(subview);
		}
	}
}
