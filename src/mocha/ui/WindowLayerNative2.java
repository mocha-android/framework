/**
 *  @author Shaun
 *  @date 4/20/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import mocha.foundation.NotificationCenter;
import mocha.graphics.Rect;
import mocha.ui.View;

import java.util.HashMap;
import java.util.Map;

public class WindowLayerNative2 extends ViewLayerNative2 implements WindowLayer {

	public WindowLayerNative2(Context context) {
		super(context);

		this.getLayout().setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		this.getLayout().addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener() {
			public void onLayoutChange(android.view.View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				if(oldRight != right || oldBottom != bottom) {
					getWindow().superSetFrame(new Rect(0.0f, 0.0f, (right - left) / scale, (bottom - top) / scale));
				}
			}
		});
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

	Layout createLayout(Context context) {
		return new Layout(context) {

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

			private int largestHeight;
			private int lastProposedHeight;
			private int lastKeyboardHeight;

			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				final int proposedHeight = MeasureSpec.getSize(heightMeasureSpec);
				largestHeight = Math.max(largestHeight, getHeight());

				if(this.lastProposedHeight != proposedHeight) {
					Rect bounds = getWindow().getBounds();
					float beginHeight = this.lastKeyboardHeight / scale;

					if(this.lastProposedHeight > 0.0f) {
						final String startNotification;
						final String endNotification;

						if(proposedHeight < largestHeight) {
							startNotification = Window.KEYBOARD_WILL_SHOW_NOTIFICATION;
							endNotification = Window.KEYBOARD_DID_SHOW_NOTIFICATION;

							this.lastKeyboardHeight = largestHeight - proposedHeight;
							MWarn("KEYBOARD VISIBLE WITH HEIGHT: %d", largestHeight - proposedHeight);
						} else {
							MWarn("KEYBOARD NOT VISIBLE");
							this.lastKeyboardHeight = 0;
							startNotification = Window.KEYBOARD_WILL_HIDE_NOTIFICATION;
							endNotification = Window.KEYBOARD_DID_HIDE_NOTIFICATION;
						}

						float endHeight = this.lastKeyboardHeight / scale;

						Rect beginFrame = new Rect(0.0f, bounds.size.height - beginHeight, bounds.size.width, beginHeight);
						Rect endFrame = new Rect(0.0f, bounds.size.height - endHeight, bounds.size.width, endHeight);

						final Map<String,Object> info = new HashMap<String,Object>();
						info.put(Window.KEYBOARD_FRAME_BEGIN_USER_INFO_KEY, beginFrame);
						info.put(Window.KEYBOARD_FRAME_END_USER_INFO_KEY, endFrame);
						info.put(Window.KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY, 0L);
						info.put(Window.KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY, View.AnimationCurve.LINEAR);

						performOnMainAfterDelay(0, new Runnable() {
							public void run() {
								NotificationCenter.defaultCenter().post(startNotification, getWindow(), info);
								NotificationCenter.defaultCenter().post(endNotification, getWindow(), info);
							}
						});
					}

					this.lastProposedHeight = proposedHeight;
				}

				super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(largestHeight, proposedHeight), MeasureSpec.EXACTLY));
			}
		};
	}
}
