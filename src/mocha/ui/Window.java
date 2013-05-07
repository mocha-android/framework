/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.view.MotionEvent;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Window extends View {
	private Activity activity;
	private Responder firstResponder;
	private WindowLayer windowLayer;
	private Event lastEvent;
	protected ViewController rootViewController;
	private List<ViewController> visibleViewControllers; // Root view controllers + visible modals

	public static final String KEYBOARD_WILL_SHOW_NOTIFICATION = "KEYBOARD_WILL_SHOW_NOTIFICATION";
	public static final String KEYBOARD_DID_SHOW_NOTIFICATION = "KEYBOARD_DID_SHOW_NOTIFICATION";
	public static final String KEYBOARD_WILL_HIDE_NOTIFICATION = "KEYBOARD_WILL_HIDE_NOTIFICATION";
	public static final String KEYBOARD_DID_HIDE_NOTIFICATION = "KEYBOARD_DID_HIDE_NOTIFICATION";


	public static final String KEYBOARD_FRAME_BEGIN_USER_INFO_KEY = "KEYBOARD_FRAME_BEGIN_USER_INFO_KEY"; // Rect
	public static final String KEYBOARD_FRAME_END_USER_INFO_KEY = "KEYBOARD_FRAME_END_USER_INFO_KEY"; // Rect
	public static final String KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY = "KEYBOARD_ANIMATION_DURATION_USER_INFO_KEY"; // Long
	public static final String KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY = "KEYBOARD_ANIMATION_CURVE_USER_INFO_KEY"; // View.AnimationCurve


	public Window(Activity activity) {
		super(activity, Screen.mainScreen().getBounds());
		this.getLayer().setBackgroundColor(Color.YELLOW);
		this.activity = activity;
		this.activity.addWindow(this);

		this.windowLayer = (WindowLayer)this.getLayer();
		this.visibleViewControllers = new ArrayList<ViewController>();
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

				if(oldViewController.isFirstResponder()) {
					oldViewController.resignFirstResponder();
				}

				this.removeVisibleViewController(oldViewController);
			}

			if(rootViewController != null) {
				rootViewController.setNextResponder(this);

				View view = rootViewController.getView();
				view.setFrame(this.getBounds());
				view.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);

				rootViewController.viewWillAppear(true);
				this.addSubview(view);
				rootViewController.viewDidAppear(false);

				this.addVisibleViewController(rootViewController);
			}

			this.rootViewController = rootViewController;

			if(this.rootViewController != null) {
				this.rootViewController.promoteDeepestDefaultFirstResponder();
			}
		}
	}

	void addVisibleViewController(ViewController viewController) {
		this.visibleViewControllers.add(viewController);
	}

	void removeVisibleViewController(ViewController viewController) {
		this.visibleViewControllers.remove(viewController);
	}

	void onPause() {
		this.windowLayer.onWindowPause();

		if(this.rootViewController != null) {
			this.rootViewController.beginAppearanceTransition(false, false);
			this.rootViewController.endAppearanceTransition();
		}

		for(ViewController viewController : this.visibleViewControllers) {
			viewController.beginAppearanceTransition(false, false);
			viewController.endAppearanceTransition();
		}
	}

	void onResume() {
		this.windowLayer.onWindowResume();

		if(this.rootViewController != null) {
			this.rootViewController.beginAppearanceTransition(true, false);
			this.rootViewController.endAppearanceTransition();
		}

		for(ViewController viewController : this.visibleViewControllers) {
			viewController.beginAppearanceTransition(true, false);
			viewController.endAppearanceTransition();
		}
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

		if(this.rootViewController != null) {
			this.promoteDeepestDefaultFirstResponder();
		}
	}

	public void makeKeyAndVisible() {
		this.setHidden(false);
		this.makeKeyWindow();
	}

	Event getLastEvent() {
		return lastEvent;
	}

	void setLastEvent(Event lastEvent) {
		this.lastEvent = lastEvent;
	}

	boolean canDeliverToNativeView(NativeView nativeView, MotionEvent motionEvent, android.view.View touchedView) {
		if(this.lastEvent == null) {
			this.lastEvent = Event.touchEvent(this);
		}

		this.lastEvent.updateMotionEvent(motionEvent, this, touchedView);

		boolean bubble = true;

		if(nativeView.trackingTouches) {
			bubble = false;
		} else {
			if(nativeView.isUserInteractionEnabled()) {
				for(Touch touch : this.lastEvent.allTouches()) {
					if(touch.getView() == nativeView) {
						bubble = false;
						nativeView.trackingTouches = true;
						break;
					}
				}
			}
		}

		int mask = motionEvent.getActionMasked();

		if(nativeView.trackingTouches && (mask == MotionEvent.ACTION_POINTER_UP || mask == MotionEvent.ACTION_UP)) {
			nativeView.trackingTouches = false;
		}

		if(bubble) {
			this.sendEvent(this.lastEvent);
			return false;
		} else {
			this.lastEvent.cleanTouches();
			return true;
		}
	}

	public void sendEvent(Event event) {
		if(Application.sharedApplication().isIgnoringInteractionEvents()) return;

		if(event.getType() == Event.Type.TOUCHES) {
			HashSet<GestureRecognizer> gestureRecognizers = new HashSet<GestureRecognizer>();
			List<Touch> touches = event.getCurrentTouches();

			for(Touch touch : touches) {
				gestureRecognizers.addAll(touch.getGestureRecognizers());
			}

			for(GestureRecognizer gestureRecognizer : gestureRecognizers) {
				gestureRecognizer.recognizeTouches(event.getTouchesForGestureRecognizer(gestureRecognizer), event);
			}

			int numberOfTouches = touches.size();

			if(numberOfTouches == 1) {
				Touch touch = touches.get(0);
				this.sendTouches(touches, touch.getPhase(), event, touch.getView());
			} else if(numberOfTouches > 1) {
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

			event.cleanTouches();
		}
	}

	private void sendTouches(List<Touch> touches, Touch.Phase phase, Event event, View view) {
		if(view == null) return;

		List<GestureRecognizer> gestureRecognizers = new ArrayList<GestureRecognizer>();
		if(touches.size() == 1) {
			gestureRecognizers.addAll(touches.get(0).getGestureRecognizers());
		} else if(touches.size() > 1) {
			for(Touch touch : touches) {
				gestureRecognizers.addAll(touch.getGestureRecognizers());
			}
		}

		if(gestureRecognizers.size() > 0) {
			for (GestureRecognizer gestureRecognizer : gestureRecognizers) {
				if(!gestureRecognizer.getCancelsTouchesInView()) {
					continue;
				}

				boolean removedTouches = false;

				switch (gestureRecognizer.getState()) {
					case BEGAN:
					case CHANGED:
					case ENDED:
					case RECOGNIZED:
						touches.removeAll(gestureRecognizer.getTrackingTouches());
						removedTouches = true;
						break;

					default:
						// don't ignore touches
				}

				if(removedTouches && touches.size() == 0) break;
			}
		}

		if(touches.size() == 0) return;

		if(!view.isMultipleTouchEnabled()) {
			boolean skipContainsCheck = false;

			if(phase == Touch.Phase.BEGAN && view.trackingSingleTouch == null) {
				view.trackingSingleTouch = touches.get(0);
				skipContainsCheck = true;
			}

			if(!skipContainsCheck && !touches.contains(view.trackingSingleTouch)) return;

			if(touches.size() > 1) {
				touches.clear();
				touches.add(view.trackingSingleTouch);
			}

			if(phase == Touch.Phase.CANCELLED || phase == Touch.Phase.ENDED) {
				view.trackingSingleTouch = null;
			}
		} else if(view.trackingSingleTouch != null) {
			view.trackingSingleTouch = null;
		}

		if(touches.size() == 0) return;

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

	public Responder nextResponder() {
		return Application.sharedApplication();
	}

	Responder getDefaultFirstResponder() {
		if(this.visibleViewControllers.size() > 0) {
			return this.visibleViewControllers.get(this.visibleViewControllers.size() - 1).getDefaultFirstResponder();
		} else if(this.rootViewController != null) {
			return this.rootViewController.getDefaultFirstResponder();
		} else {
			return this;
		}
	}

	public boolean canBecomeFirstResponder() {
		return true;
	}

	boolean canBecomeDefaultFirstResponder() {
		return true;
	}

	void willRotateToInterfaceOrientation(InterfaceOrientation toInterfaceOrientation) {
		HashSet<ViewController> viewControllers = new HashSet<ViewController>(this.visibleViewControllers);

		if(this.rootViewController != null) {
			viewControllers.add(this.rootViewController);
		}

		for(ViewController viewController : viewControllers) {
			viewController.willRotateToInterfaceOrientation(toInterfaceOrientation);
		}
	}

	void didRotateFromInterfaceOrientation(InterfaceOrientation fromInterfaceOrientation) {
		HashSet<ViewController> viewControllers = new HashSet<ViewController>(this.visibleViewControllers);

		if(this.rootViewController != null) {
			viewControllers.add(this.rootViewController);
		}

		for(ViewController viewController : viewControllers) {
			viewController.didRotateFromInterfaceOrientation(fromInterfaceOrientation);
		}
	}

}
