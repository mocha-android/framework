/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;
import android.view.ViewGroup;
import java.util.List;

public class NativeView <V extends android.view.View> extends View {

	private V nativeView;
	boolean trackingTouches;

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
		super.setHidden(hidden);
	}

	private void updateNativeViewFrame() {
		if(this.nativeView == null) return;

		android.graphics.Rect frame = this.getFrame().toSystemRect(this.scale);
		int width = frame.width();
		int height = frame.height();

		this.nativeView.setMinimumWidth(width);
		this.nativeView.setMinimumHeight(height);
		this.nativeView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
		this.nativeView.measure(
				android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
				android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
		);
		this.nativeView.forceLayout();
		this.nativeView.layout(0, 0, width, height);
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