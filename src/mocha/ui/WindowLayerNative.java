/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.foundation.*;
import mocha.graphics.Rect;

public final class WindowLayerNative extends ViewLayerNative implements WindowLayer {
	private View hitView;

	public WindowLayerNative(Context context) {
		super(context);
		MObject.MLog("Created window layout class");
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

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if(changed) {
			ViewGroup view = (ViewGroup)this.getParent();
			MObject.MLog("Parent: " + view);

			if(view != null) {
				float scale = getView().scale;

				Rect frame = new Rect(0, 0, view.getWidth() / scale, view.getHeight() / scale);
				getWindow().superSetFrame(frame);
				MObject.MLog("Window Bounds: " + getView().getBounds().toString());
				MObject.MLog("Window Frame: " + frame);
				MObject.MLog("Window Raw Size: " + view.getWidth() + "x" + view.getHeight() + " - " + (view.getHeight() / scale));
			}
		}
	}

}
