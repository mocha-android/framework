/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.graphics.Rect;

import java.util.HashSet;
import java.util.List;

public final class Window extends View {
	private Activity activity;
	private Responder firstResponder;
	protected ViewController rootViewController;
	boolean layoutOut;

	public Window(Activity activity) {
		super(activity, Screen.mainScreen().getBounds());
		this.getLayer().setBackgroundColor(Color.YELLOW);
		this.activity = activity;
	}

	private static Rect getDisplayRect(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);

		android.graphics.Rect rect = new android.graphics.Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
		android.view.View decor = activity.getWindow().getDecorView();
		android.view.View content = activity.getWindow().findViewById(android.view.Window.ID_ANDROID_CONTENT);
		MLog("Display: " + rect.width() + "x" + rect.height() + " | Content " + content.getWidth() + "x" + content.getHeight());
		return new Rect(0, 0, rect.width(), rect.height());
	}

	public Class<? extends ViewLayer> getLayerClass() {
		MLog("Getting window layout class");
		return WindowLayer.class;
	}

	public ViewController getRootViewController() {
		return this.rootViewController;
	}

	public void setRootViewController(ViewController rootViewController) {
		if(this.rootViewController != rootViewController) {
			ViewController oldViewController = this.rootViewController;

			if(oldViewController != null) {
				oldViewController.viewWillDisappear(false);
			}

			while(this.getSubviews().size() > 0) {
				this.getSubviews().get(0).removeFromSuperview();
			}

			if(oldViewController != null) {
				oldViewController.viewDidDisappear(false);
			}

			if(rootViewController != null) {
				rootViewController.getView().setFrame(this.getBounds());
				rootViewController.viewWillAppear(true);
				this.addSubview(rootViewController.getView());
				rootViewController.viewDidAppear(false);
			}

			this.rootViewController = rootViewController;
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
		this.activity.setContentView(this.getLayer());
	}

	public void makeKeyAndVisible() {
		this.setHidden(false);
		this.makeKeyWindow();
	}

	public void sendEvent(Event event) {
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

	Responder getFirstResponder() {
		return firstResponder;
	}

	void setFirstResponder(Responder firstResponder) {
		this.firstResponder = firstResponder;
	}

	protected static final class WindowLayer extends ViewLayer {
		private View hitView;
		private Event lastEvent;

		public WindowLayer(Context context) {
			super(context);
			MLog("Created window layout class");
		}

		public void forceLayout() {
			super.forceLayout();

			ViewGroup view = (ViewGroup)this.getParent();
			if(view != null) {
				Rect frame = new Rect(0, 0, view.getWidth() / scale, view.getHeight() / scale);
				this.getWindow().superSetFrame(frame);
				MLog("Window Bounds: " + this.getView().getBounds().toString());
				MLog("Window Frame: " + frame);
				MLog("Window Raw Size: " + view.getWidth() + "x" + view.getHeight() + " - " + (view.getHeight() / scale));
			}
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
}
