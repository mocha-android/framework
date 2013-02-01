/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.widget.FrameLayout;
import mocha.graphics.Rect;

import java.util.HashSet;
import java.util.List;

public final class Window extends View {
	private Activity activity;
	private Responder firstResponder;
	private WindowLayer windowLayer;
	protected ViewController rootViewController;

	public Window(Activity activity) {
		super(activity, Screen.mainScreen().getBounds());
		this.getLayer().setBackgroundColor(Color.YELLOW);
		this.activity = activity;
		this.activity.addWindow(this);

		this.windowLayer = (WindowLayer)this.getLayer();
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
		this.windowLayer.onWindowPause();
	}

	void onResume() {
		this.windowLayer.onWindowResume();
	}

	public void setFrame(Rect frame) {
		MLog("Setting window frame");
	}

	void superSetFrame(Rect frame) {
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
		this.activity.setContentView(this.windowLayer.getNativeView());
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

}
