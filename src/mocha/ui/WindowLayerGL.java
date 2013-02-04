/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.graphics.Rect;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class WindowLayerGL extends ViewLayerGL implements WindowLayer {
	private View hitView;
	private Event lastEvent;
	private GLSurfaceView surfaceView;
	private static int LAYOUT = -598248493;
	private ThreadLocal<LayoutHandler> layoutHandler = new ThreadLocal<LayoutHandler>();

	public WindowLayerGL(android.content.Context context) {
		super(context);

		this.surfaceView = new WindowSurfaceView(context);
	}

	public android.view.View getNativeView() {
		return this.surfaceView;
	}

	WindowLayerGL getWindowLayer() {
		return this;
	}

	public void onWindowPause() {
		// this.surfaceView.onPause();
	}

	public void onWindowResume() {
		// this.surfaceView.onResume();
	}

	void scheduleLayout() {
		LayoutHandler handler = this.layoutHandler.get();

		if(handler == null) {
			handler = new LayoutHandler();
			this.layoutHandler.set(handler);
		}

		if(!handler.layoutScheduled) {
			handler.sendEmptyMessage(LAYOUT);
			handler.layoutScheduled = true;
		}
	}

	private Window getWindow() {
		return (Window)this.getView();
	}

	class WindowSurfaceView extends GLSurfaceView {
		private WindowSurfaceView.WindowSurfaceRenderer renderer;

		WindowSurfaceView(Context context) {
			super(context);

			this.setEGLContextClientVersion(1);

			this.renderer = new WindowSurfaceView.WindowSurfaceRenderer();
			this.setRenderer(this.renderer);

			this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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

		class WindowSurfaceRenderer implements Renderer {
			private final boolean showFPS = false;

			//  The number of frames
			int frameCount = 0;

			//  Number of frames per second
			float fps = 0;

			long currentTime = 0, previousTime = 0;

			public WindowSurfaceRenderer() {

			}

			public void onSurfaceCreated(GL10 gl, EGLConfig config) {
				gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
				gl.glShadeModel(GL10.GL_SMOOTH);
				gl.glClearDepthf(1.0f);
				gl.glDisable(GL10.GL_DEPTH_TEST);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				gl.glEnable(GL10.GL_BLEND);
			}

			public void onDrawFrame(GL10 gl) {
				gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
				gl.glLoadIdentity();
				WindowLayerGL.this.draw(gl);

				if(showFPS) {
					calculateFPS(gl);
				}
			}

			private void drawFPS(GL10 gl) {
				gl.glLoadIdentity();
			}

			void calculateFPS(GL10 gl) {
				//  Increase frame count
				frameCount++;

				//  Get the number of milliseconds since glutInit called
				//  (or first call to glutGet(GLUT ELAPSED TIME)).
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

			public void onSurfaceChanged(GL10 gl, int width, int height) {
				float scaledWidth = View.ceilf((float) width / scale);
				float scaledHeight = View.ceilf((float) height / scale);

				gl.glViewport(0, 0, width, height);
				gl.glMatrixMode(GL10.GL_PROJECTION);
				gl.glLoadIdentity();
				GLU.gluOrtho2D(gl, 0.0f, scaledWidth, scaledHeight, 0.0f);

				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity();
			}
		}
	}


	class LayoutHandler extends android.os.Handler {
		public boolean layoutScheduled;

		public void handleMessage(android.os.Message message) {
			this.layoutScheduled = false;

			if(message.what == LAYOUT) {
				layout(WindowLayerGL.this);
				surfaceView.requestRender();
			}
		}

		private void layout(ViewLayerGL layer) {
			layer.layoutSublayersIfNeeded();

			for(ViewLayerGL sublayer : layer.getSublayersGL()) {
				layout(sublayer);
			}
		}
	}

}
