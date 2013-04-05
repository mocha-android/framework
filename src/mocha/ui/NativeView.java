/**
 *  @author Shaun
 *  @date 3/1/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;

import java.util.List;

public class NativeView <V extends android.view.View> extends View {

	private V nativeView;

	public NativeView(V nativeView) {
		this.nativeView = nativeView;

		if(!(this.getLayer() instanceof ViewLayerNative)) {
			throw new RuntimeException("NativeView currently only works when using ViewLayerNative.");
		}

		if(this.nativeView != null) {
			this.getNativeLayer().addView(this.nativeView);
		}

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
		this.getNativeLayer().addView(this.nativeView);
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

		if(this.nativeView != null) {
			android.graphics.Rect frame = this.getFrame().toSystemRect(this.scale);
			this.nativeView.layout(0, 0, frame.width(), frame.height());
		}
	}

	public void touchesBegan(List<Touch> touches, Event event) {
		super.touchesBegan(touches, event);

		if(this.nativeView != null) {
			this.nativeView.onTouchEvent(event.getMotionEvent());
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		super.touchesMoved(touches, event);

		if(this.nativeView != null) {
			this.nativeView.onTouchEvent(event.getMotionEvent());
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		super.touchesEnded(touches, event);

		if(this.nativeView != null) {
			this.nativeView.onTouchEvent(event.getMotionEvent());
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		super.touchesCancelled(touches, event);

		if(this.nativeView != null) {
			this.nativeView.onTouchEvent(event.getMotionEvent());
		}
	}

	private ViewLayerNative getNativeLayer() {
		return (ViewLayerNative)this.getLayer();
	}

}
