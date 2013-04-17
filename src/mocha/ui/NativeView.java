/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/*
 * The current implementation of this is incredibly nasty.
 * There's something wrong with ViewLayerNative that does't correctly
 * propergate Android's layout calls downward and native views with deep
 * hierarchy's don't get laid out correctly. To counter this, we're using
 * a temporary off screen stage layout that gets added to the activity's
 * content view and them removed on the next run loop, but allows the child
 * to be correctly sized.  This needs to be addressed properly and is not
 * a viable long term solution.
 */

public class NativeView <V extends android.view.View> extends View {

	private V nativeView;
	boolean trackingTouches;
	private int stageCount;
	private int lastWidth;
	private int lastHeight;

	public NativeView(V nativeView) {
		if(!ViewLayerNative.class.isAssignableFrom(VIEW_LAYER_CLASS)) {
			throw new RuntimeException("NativeView currently only works when using ViewLayerNative.");
		}

		this.setNativeView(nativeView);
		this.setUserInteractionEnabled(true);
	}

	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		super.setUserInteractionEnabled(userInteractionEnabled);

		if(this.nativeView != null) {
			this.nativeView.setEnabled(userInteractionEnabled);
			this.nativeView.setClickable(userInteractionEnabled);
			this.nativeView.setLongClickable(userInteractionEnabled);
		}
	}

	public void setNativeView(V nativeView) {
		if(this.nativeView != null) {
			this.getNativeLayer().removeView(this.nativeView);
		}

		this.nativeView = nativeView;

		if(this.nativeView != null) {
			this.nativeView.setOnTouchListener(new android.view.View.OnTouchListener() {
				public boolean onTouch(android.view.View view, MotionEvent motionEvent) {
					if(getWindow().canDeliverToNativeView(NativeView.this, motionEvent, view)) {
						view.onTouchEvent(motionEvent);
					}

					return true;
				}
			});

			this.lastWidth = 0;
			this.lastHeight = 0;
			this.getNativeLayer().addView(this.nativeView);
		}
	}

	public V getNativeView() {
		return nativeView;
	}

	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);

		if(this.nativeView != null) {
			this.nativeView.setBackgroundColor(backgroundColor);
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();
		this.updateNativeViewFrame();
	}

	public void setHidden(boolean hidden) {
		if(hidden != this.isHidden()) {
			super.setHidden(hidden);
			this.forceLayout();
		}
	}

	public void forceLayout() {
		this.updateNativeViewFrame(true);
	}

	private void updateNativeViewFrame() {
		this.updateNativeViewFrame(false);
	}

	private void updateNativeViewFrame(boolean forceStageLayout) {
		if(this.nativeView == null) return;

		android.graphics.Rect frame = this.getFrame().toSystemRect(this.scale);
		int width = frame.width();
		int height = frame.height();

		this.nativeView.setMinimumWidth(width);
		this.nativeView.setMinimumHeight(height);
		this.nativeView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
		this.nativeView.measure(
				android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.UNSPECIFIED),
				android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.UNSPECIFIED)
		);
		this.nativeView.forceLayout();
		this.nativeView.layout(0, 0, width, height);

		if(forceStageLayout) {
			this.stageLayout();
		} else {
			this.stageLayoutIfNeeded();
		}
	}

	private void stageLayoutIfNeeded() {
		int width = this.getWidth();
		int height = this.getHeight();

		if(width != this.lastWidth || height != this.lastHeight) {
			this.stageLayout();
		}
	}

	private void stageLayout() {
		if(this.isHidden()) return;

		final android.view.View nativeView = this.nativeView;
		final ViewGroup oldParent = ((ViewGroup)nativeView.getParent());

		if(oldParent != null) {
			oldParent.removeView(nativeView);
		}

		this.lastWidth = this.getWidth();
		this.lastHeight = this.getHeight();

		final LinearLayout layout = new LinearLayout(Application.sharedApplication().getContext());
		layout.addView(nativeView);
		layout.setX(this.lastWidth * -2);
		layout.setY(this.lastHeight * -2);

		final Activity activity = Application.sharedApplication().getActivity();
		activity.addContentView(layout, new ViewGroup.LayoutParams(this.lastWidth, this.lastHeight));

		final int activeStageId = ++stageCount;

		performAfterDelay(0, new Runnable() {
			public void run() {
				if(stageCount == activeStageId) {
					((ViewGroup) nativeView.getParent()).removeView(nativeView);
					getNativeLayer().addView(nativeView);
				}

				if(layout.getParent() != null) {
					((ViewGroup)layout.getParent()).removeView(layout);
				}
			}
		});
	}

	private int getWidth() {
		return this.getNativeLayer().getWidth();
	}

	private int getHeight() {
		return this.getNativeLayer().getHeight();
	}

	public void touchesBegan(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesBegan(touches, event);
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesMoved(touches, event);
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {

		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesEnded(touches, event);
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		if(this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent()))  {
			super.touchesEnded(touches, event);
		}
	}

	private ViewLayerNative getNativeLayer() {
		return (ViewLayerNative)this.getLayer();
	}
	
}