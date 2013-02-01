/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.view.MotionEvent;

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

}
