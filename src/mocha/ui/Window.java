/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.opengl.*;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import mocha.graphics.Rect;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.HashSet;
import java.util.List;

public final class Window extends View {
	private Activity activity;
	private Responder firstResponder;
	protected ViewController rootViewController;

	public Window(Activity activity) {
		super(activity, Screen.mainScreen().getBounds());
		this.getLayer().setBackgroundColor(Color.YELLOW);
		this.activity = activity;
		this.activity.addWindow(this);
	}

	public Class<? extends ViewLayer> getLayerClass() {
		if(USE_GL_LAYERS) {
			return WindowLayerGL.class;
		} else {
			return WindowLayerCanvas.class;
		}
	}

	public ViewController getRootViewController() {
		return this.rootViewController;
	}

	public void setRootViewController(ViewController rootViewController) {
		if(this.rootViewController != rootViewController) {
			ViewController oldViewController = this.rootViewController;

			if(oldViewController != null) {
				oldViewController.viewWillDisappear(false);
				oldViewController.setNextResponder(null);
			}

			while(this.getSubviews().size() > 0) {
				this.getSubviews().get(0).removeFromSuperview();
			}

			if(oldViewController != null) {
				oldViewController.viewDidDisappear(false);
			}

			if(rootViewController != null) {
				rootViewController.setNextResponder(this);

				View view = rootViewController.getView();
				view.setFrame(this.getBounds());
				view.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);

				rootViewController.viewWillAppear(true);
				this.addSubview(view);
				rootViewController.viewDidAppear(false);
			}

			this.rootViewController = rootViewController;
		}
	}

	void onPause() {
		ViewLayer layer = this.getLayer();

		if(layer instanceof WindowLayerGL) {
			// ((WindowLayerGL)layer).surfaceView.onPause();
		}
	}

	void onResume() {
		ViewLayer layer = this.getLayer();

		if(layer instanceof WindowLayerGL) {
			// ((WindowLayerGL)layer).surfaceView.onPause();
		}
	}

	public void setFrame(Rect frame) {
		MLog("Setting window frame");
	}

	private void superSetFrame(Rect frame) {
		super.setFrame(frame);
	}

	public void removeFromSuperview() {
		// Prevent this from happening.
	}

	public View getSuperview() {
		return null;
	}

	public Window getWindow() {
		return this;
	}

	public void makeKeyWindow() {
		ViewLayer layer = this.getLayer();

		if(layer instanceof ViewLayerCanvas) {
			this.activity.setContentView((ViewLayerCanvas)layer);
		} else if(layer instanceof WindowLayerGL) {
			FrameLayout frameLayout = new FrameLayout(this.activity);
			frameLayout.addView(((WindowLayerGL)layer).surfaceView);
			this.activity.setContentView(frameLayout);
		}
	}

	public void makeKeyAndVisible() {
		this.setHidden(false);
		this.makeKeyWindow();
	}


	public void sendEvent(Event event) {
		if(Application.sharedApplication().isIgnoringInteractionEvents()) return;
		
		if(event.getType() == Event.Type.TOUCHES) {
			HashSet<GestureRecognizer> gestureRecognizers = new HashSet<GestureRecognizer>();
			List<Touch> touches = event.touchesForWindow(this);

			for(Touch touch : touches) {
				gestureRecognizers.addAll(touch.getGestureRecognizers());
			}

			for(GestureRecognizer gestureRecognizer : gestureRecognizers) {
//				if(gestureRecognizer.getState() == GestureRecognizer.State.POSSIBLE) {
//					boolean prevented = false;
//					for(GestureRecognizer otherGestureRecognizer : gestureRecognizers) {
//						if(otherGestureRecognizer.getState() == GestureRecognizer.State.BEGAN || otherGestureRecognizer.getState() == GestureRecognizer.State.CHANGED) {
//							if(otherGestureRecognizer.canPreventGestureRecognizer(gestureRecognizer)) {
//								prevented = true;
//								break;
//							}
//						}
//					}
//
//					if(prevented) continue;
//				}

				gestureRecognizer.recognizeTouches(touches, event);
			}

			for(Touch touch : touches) {
				Touch.Phase phase = touch.getPhase();

				if(phase == Touch.Phase.BEGAN) {
					touch.getView().touchesBegan(touches, event);
				} else if(phase == Touch.Phase.MOVED) {
					touch.getView().touchesMoved(touches, event);
				} else if(phase == Touch.Phase.ENDED) {
					touch.getView().touchesEnded(touches, event);
				} else if(phase == Touch.Phase.CANCELLED) {
					touch.getView().touchesCancelled(touches, event);
				}
			}
		}
	}

	public void backKeyPressed(Event event) {
		this.activity.backKeyPressed(event);
	}

	Responder getFirstResponder() {
		return firstResponder;
	}

	void setFirstResponder(Responder firstResponder) {
		this.firstResponder = firstResponder;
	}

	protected static final class WindowLayerCanvas extends ViewLayerCanvas {
		private View hitView;
		private Event lastEvent;

		public WindowLayerCanvas(Context context) {
			super(context);
			MLog("Created window layout class");
		}

		private Window getWindow() {
			return (Window)this.getView();
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

		private String getActionTitle(int action) {
			if(action == MotionEvent.ACTION_MASK) return "MASK";
			if(action == MotionEvent.ACTION_DOWN) return "DOWN";
			if(action == MotionEvent.ACTION_UP) return "UP";
			if(action == MotionEvent.ACTION_MOVE) return "MOVE";
			if(action == MotionEvent.ACTION_CANCEL) return "CANCEL";
			if(action == MotionEvent.ACTION_OUTSIDE) return "OUTSIDE";
			if(action == MotionEvent.ACTION_POINTER_DOWN) return "POINTER_DOWN";
			if(action == MotionEvent.ACTION_POINTER_UP) return "POINTER_UP";
			if(action == MotionEvent.ACTION_HOVER_MOVE) return "HOVER_MOVE";
			if(action == MotionEvent.ACTION_SCROLL) return "SCROLL";
			if(action == MotionEvent.ACTION_HOVER_ENTER) return "HOVER_ENTER";
			if(action == MotionEvent.ACTION_HOVER_EXIT) return "HOVER_EXIT";
			if(action == MotionEvent.ACTION_POINTER_INDEX_MASK) return "POINTER_INDEX_MASK";
			if(action == MotionEvent.ACTION_POINTER_INDEX_SHIFT) return "POINTER_INDEX_SHIFT";
			if(action == MotionEvent.ACTION_POINTER_1_DOWN) return "POINTER_1_DOWN";
			if(action == MotionEvent.ACTION_POINTER_2_DOWN) return "POINTER_2_DOWN";
			if(action == MotionEvent.ACTION_POINTER_3_DOWN) return "POINTER_3_DOWN";
			if(action == MotionEvent.ACTION_POINTER_1_UP) return "POINTER_1_UP";
			if(action == MotionEvent.ACTION_POINTER_2_UP) return "POINTER_2_UP";
			if(action == MotionEvent.ACTION_POINTER_3_UP) return "POINTER_3_UP";
			if(action == MotionEvent.ACTION_POINTER_ID_MASK) return "POINTER_ID_MASK";
			if(action == MotionEvent.ACTION_POINTER_ID_SHIFT) return "POINTER_ID_SHIFT";
			return "UNKNOWN";
		}
	}

	public static final class WindowLayerGL extends ViewLayerGL {
		private View hitView;
		private Event lastEvent;
		private GLSurfaceView surfaceView;
		private static int LAYOUT = -598248493;
		private ThreadLocal<LayoutHandler> layoutHandler = new ThreadLocal<LayoutHandler>();

		public WindowLayerGL(android.content.Context context) {
			super(context);

			this.surfaceView = new WindowSurfaceView(context);
		}

		WindowLayerGL getWindowLayer() {
			return this;
		}

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

		class WindowSurfaceView extends GLSurfaceView {
			private WindowSurfaceRenderer renderer;

			WindowSurfaceView(Context context) {
				super(context);

				this.setEGLContextClientVersion(1);

				this.renderer = new WindowSurfaceRenderer();
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

			class WindowSurfaceRenderer implements GLSurfaceView.Renderer {
				private final boolean showFPS = true;

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
					float scaledWidth = ceilf((float)width / scale);
					float scaledHeight = ceilf((float)height / scale);

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
}
