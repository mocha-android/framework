/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.foundation.*;
import mocha.graphics.Rect;

public final class WindowLayerNative extends ViewLayerNative implements WindowLayer {
	private View hitView;
	private Event lastEvent;

	public WindowLayerNative(Context context) {
		super(context);
		mocha.foundation.Object.MLog("Created window layout class");
	}

	public android.view.View getNativeView() {
		return this;
	}

	private Window getWindow() {
		return (Window)this.getView();
	}

	public void onWindowPause() {

	}

	public void onWindowResume() {

	}

	public boolean onTouchEvent(MotionEvent motionEvent) {
		// 	MLog("Touch Event: " + getActionTitle(motionEvent.getAction()) +  " | Touches: " + motionEvent.getPointerCount());

		if(this.lastEvent == null) {
			this.lastEvent = new Event(motionEvent, this.getWindow());
		} else {
			this.lastEvent.updateMotionEvent(motionEvent, this.getWindow());
		}

		this.getWindow().sendEvent(this.lastEvent);

		return true;
	}

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if(changed) {
			ViewGroup view = (ViewGroup)this.getParent();
			mocha.foundation.Object.MLog("Parent: " + view);

			if(view != null) {
				float scale = getView().scale;

				Rect frame = new Rect(0, 0, view.getWidth() / scale, view.getHeight() / scale);
				getWindow().superSetFrame(frame);
				mocha.foundation.Object.MLog("Window Bounds: " + getView().getBounds().toString());
				mocha.foundation.Object.MLog("Window Frame: " + frame);
				mocha.foundation.Object.MLog("Window Raw Size: " + view.getWidth() + "x" + view.getHeight() + " - " + (view.getHeight() / scale));
			}
		}
	}

}
