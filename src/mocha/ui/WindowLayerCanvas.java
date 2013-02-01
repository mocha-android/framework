/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 TV Guide, Inc. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.graphics.Rect;

public class WindowLayerCanvas extends ViewLayerCanvas implements WindowLayer {
	private View hitView;
	private Event lastEvent;
	private static int LAYOUT = -598248493;
	private static int RENDER = -493857374;
	private ThreadLocal<LayoutHandler> layoutHandler = new ThreadLocal<LayoutHandler>();
	private WindowView windowView;

	public WindowLayerCanvas(android.content.Context context) {
		super(context);

		this.windowView = new WindowView(context);
	}

	public android.view.View getNativeView() {
		return this.windowView;
	}

	WindowLayerCanvas getWindowLayer() {
		return this;
	}

	public void onWindowPause() { }

	public void onWindowResume() { }

	void scheduleLayout() {
		LayoutHandler handler = this.layoutHandler.get();

		if(handler == null) {
			handler = new LayoutHandler();
			this.layoutHandler.set(handler);
		}

		if(!handler.layoutScheduled) {
			handler.sendEmptyMessage(LAYOUT);
		}
	}

	private Window getWindow() {
		return (Window)this.getView();
	}

	class WindowView extends android.view.View {
		private final boolean showFPS = true;

		//  The number of frames
		int frameCount = 0;

		//  Number of frames per second
		float fps = 0;

		long currentTime = 0, previousTime = 0;

		WindowView(Context context) {
			super(context);
			this.setWillNotDraw(false);
		}

		public void forceLayout() {
			super.forceLayout();

			ViewGroup view = (ViewGroup)this.getParent();
			if(view != null) {
				float scale = getView().scale;

				Rect frame = new Rect(0, 0, view.getWidth() / scale, view.getHeight() / scale);
				getWindow().superSetFrame(frame);
				MLog("Window Bounds: " + getView().getBounds().toString());
				MLog("Window Frame: " + frame);
				MLog("Window Raw Size: " + view.getWidth() + "x" + view.getHeight() + " - " + (view.getHeight() / scale));
			}
		}

		public boolean onTouchEvent(MotionEvent motionEvent) {
			if(lastEvent == null) {
				lastEvent = new Event(motionEvent, getWindow());
			} else {
				lastEvent.updateMotionEvent(motionEvent, getWindow());
			}

			getWindow().sendEvent(lastEvent);

			return true;
		}

		protected void onDraw(Canvas canvas) {
			WindowLayerCanvas.this.draw(canvas);

			if(showFPS) {
				calculateFPS();
			}
		}



		void calculateFPS() {
			//  Increase frame count
			frameCount++;

			//  Get the number of milliseconds since glutInit called
			//  (or first call to glutGet(CanvasUT ELAPSED TIME)).
			currentTime = android.os.SystemClock.uptimeMillis();

			//  Calculate time passed
			long timeInterval = currentTime - previousTime;

			if(timeInterval > 1000) {
				//  calculate the number of frames per second
				fps = frameCount / (timeInterval / 1000.0f);

				//  Set time
				previousTime = currentTime;

				//  Reset frame count
				frameCount = 0;

				MLog("FPS: %s", fps);
			}
		}
	}

	class LayoutHandler extends android.os.Handler {
		public boolean layoutScheduled;

		public void handleMessage(android.os.Message message) {
			this.layoutScheduled = false;

			if(message.what == LAYOUT) {
				layout(WindowLayerCanvas.this);
				windowView.invalidate();
			}
		}

		private void layout(ViewLayerCanvas layer) {
			layer.layoutSublayersIfNeeded();

			for(ViewLayerCanvas sublayer : layer.getSublayersCanvas()) {
				layout(sublayer);
			}
		}
	}
}
