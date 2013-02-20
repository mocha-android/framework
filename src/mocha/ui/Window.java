/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

import java.util.ArrayList;
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
		return WINDOW_LAYER_CLASS;
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
				gestureRecognizer.recognizeTouches(touches, event);
			}

			int numberOfTouches = touches.size();

			if(numberOfTouches == 1) {
				Touch touch = touches.get(0);
				this.sendTouches(touches, touch.getPhase(), event, touch.getView());
			} else if(numberOfTouches > 1) {
				// TODO: Come up with a more performant way to do this..

				List<Touch> undeliveredTouches = new ArrayList<Touch>(touches);
				List<Touch> touchesToDeliver = new ArrayList<Touch>();

				int remainingTouches;
				while((remainingTouches = undeliveredTouches.size()) > 0) {
					Touch touch = undeliveredTouches.get(0);
					touchesToDeliver.clear();
					touchesToDeliver.add(touch);

					// Find other touches with the same state and view
					for(int i = 1; i < remainingTouches; i++) {
						Touch nextTouch = undeliveredTouches.get(i);

						if(nextTouch.getPhase() == touch.getPhase() && nextTouch.getView() == touch.getView()) {
							touchesToDeliver.add(touch);
						}
					}

					undeliveredTouches.removeAll(touchesToDeliver);

					// Send related touches
					this.sendTouches(touchesToDeliver, touch.getPhase(), event, touch.getView());
				}
			}
		}
	}

	private void sendTouches(List<Touch> touches, Touch.Phase phase, Event event, View view) {
		switch (phase) {
			case BEGAN:
				view.touchesBegan(touches, event);
				break;
			case MOVED:
				view.touchesMoved(touches, event);
				break;
			case ENDED:
				view.touchesEnded(touches, event);
				break;
			case CANCELLED:
				view.touchesCancelled(touches, event);
				break;
			default:
				// Do nothing
				break;
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
