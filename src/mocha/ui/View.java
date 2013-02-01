/*
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright 2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.animation.Animator;
import mocha.animation.AnimatorSet;
import mocha.animation.TimingFunction;
import mocha.animation.ViewAnimator;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.*;

public class View extends Responder {
	public static final boolean USE_GL_LAYERS = false;
	public static boolean SLOW_ANIMATIONS = false;

	private ViewLayer layer;
	private View superview;
	private int backgroundColor;
	private int autoresizingMask;
	private ArrayList<View> subviews;
	private boolean autoresizesSubviews;
	private int tag;
	private boolean needsLayout;
	private boolean userInteractionEnabled;
	Rect frame;
	private Rect bounds;
	private ArrayList<GestureRecognizer> gestureRecognizers;
	private boolean clipsToBounds;
	public final float scale;
	private boolean onCreatedCalled;
	private ViewController _viewController;

	public enum Autoresizing {
		NONE(0),
		FLEXIBLE_LEFT_MARGIN(1 << 0),
		FLEXIBLE_WIDTH(1 << 1),
		FLEXIBLE_RIGHT_MARGIN(1 << 2),
		FLEXIBLE_TOP_MARGIN(1 << 3),
		FLEXIBLE_HEIGHT(1 << 4),
		FLEXIBLE_BOTTOM_MARGIN(1 << 5);

		private int value;
		private Autoresizing(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static final EnumSet<Autoresizing> FLEXIBLE_SIZE = EnumSet.of(Autoresizing.FLEXIBLE_WIDTH, Autoresizing.FLEXIBLE_HEIGHT);
		public static final EnumSet<Autoresizing> FLEXIBLE_MARGINS = EnumSet.of(Autoresizing.FLEXIBLE_TOP_MARGIN, Autoresizing.FLEXIBLE_LEFT_MARGIN, Autoresizing.FLEXIBLE_BOTTOM_MARGIN, Autoresizing.FLEXIBLE_RIGHT_MARGIN);
	}

	public enum AnimationCurve {
		EASE_IN_OUT,
		EASE_IN,
		EASE_OUT,
		LINEAR
	}

	public interface AnimationDidStart {
		public void animationDidStart(String animationID, Object context);
	}

	public interface AnimationDidStop {
		public void animationDidStop(String animationID, boolean finished, Object context);
	}

	public interface AnimationCompletion {
		public void animationCompletion(boolean finished);
	}

	public interface Animations {
		public void performAnimatedChanges();
	}

	public View() {
		this(Screen.mainScreen().getContext());
	}

	public View(Rect frame) {
		this(Screen.mainScreen().getContext(), frame);
	}

	View(android.content.Context context) {
		this(context, Rect.zero());
	}

	View(android.content.Context context, Rect frame) {
		this.scale = context.getResources().getDisplayMetrics().density;

		try {
			this.layer = getLayerClass().getConstructor(android.content.Context.class).newInstance(context);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.frame = new Rect(frame);
		this.bounds = new Rect(Point.zero(), frame.size);

		this.layer.setView(this);
		this.layer.setFrame(this.frame, this.bounds);
		this.subviews = new ArrayList<View>();
		this.autoresizesSubviews = true;
		this.autoresizingMask = Autoresizing.NONE.value;
		this.userInteractionEnabled = true;
		this.gestureRecognizers = new ArrayList<GestureRecognizer>();
		this.clipsToBounds = false;


		boolean supportsDrawing;
		
		try {
			supportsDrawing = this.getClass().getMethod("draw", mocha.graphics.Context.class, Rect.class).getDeclaringClass() != View.class;
		} catch (NoSuchMethodException e) {
			supportsDrawing = false;
		}

		this.layer.setSupportsDrawing(supportsDrawing);

		this.onCreate(frame);

		if(!this.onCreatedCalled) {
			throw new RuntimeException(this.getClass().getCanonicalName() + " overrides onCreate but does not call super.");
		}
	}

	protected void onCreate(Rect frame) {
		this.onCreatedCalled = true;
	}

	public boolean clipsToBounds() {
		return clipsToBounds;
	}

	public void setClipsToBounds(boolean clipsToBounds) {
		if(this.clipsToBounds != clipsToBounds) {
			this.clipsToBounds = clipsToBounds;
			this.setNeedsDisplay();
		}
	}

	public Rect getFrame() {
		return new Rect(this.frame);
	}

	public void setFrame(Rect frame) {
		if(frame == null) {
			frame = Rect.zero();
		}

		if(areAnimationsEnabled && currentViewAnimation != null) {
			currentViewAnimation.addAnimator(ViewAnimator.ofFrame(this, frame));
			return;
		}

		if(!frame.equals(this.frame)) {
			if(this.superview != null && !this.superview.getBounds().contains(this.frame)) {
				// Fixes a weird bug in Android that leaves artifacts on the screen
				// if the old frame wasn't fully contained within the bounds.
				this.superview.setNeedsDisplay(this.frame);
			}

			Rect oldBounds = this.bounds;

			this.frame = new Rect(frame);
			this.bounds = new Rect(this.bounds.origin, this.frame.size);
			this.layer.setFrame(this.frame, this.bounds);
			this.boundsDidChange(oldBounds, this.bounds);
		}
	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public void setBounds(Rect bounds) {
		if(bounds == null) {
			bounds = Rect.zero();
		}

		if(areAnimationsEnabled && currentViewAnimation != null) {
			currentViewAnimation.addAnimator(ViewAnimator.ofBounds(this, frame));
			return;
		}

		if(!bounds.equals(this.bounds)) {
			// TODO: Combine these to be smart and only invalidate if the subview isn't fully contained
			for(View subview : this.subviews) {
				this.setNeedsDisplay(subview.frame);
			}

			if(this.superview != null) {
				this.superview.setNeedsDisplay();
			}

			Rect oldBounds = this.bounds;
			this.bounds = bounds;
			this.layer.setBounds(this.bounds);
			this.boundsDidChange(oldBounds, this.layer.getBounds());
		}
	}

	public EnumSet<Autoresizing> getAutoresizing() {
		EnumSet<Autoresizing> autoresizingMask = EnumSet.noneOf(Autoresizing.class);

		for(Autoresizing option : Autoresizing.values()) {
			if(option == Autoresizing.NONE) continue;

			if((this.autoresizingMask & option.value) != 0) {
				autoresizingMask.add(option);
			}
		}

		return autoresizingMask;
	}

	public void setAutoresizing(EnumSet<Autoresizing> autoresizing) {
		this.autoresizingMask = 0;

		for(Autoresizing option : autoresizing) {
			this.autoresizingMask |= option.getValue();
		}
	}

	public void setAutoresizing(Autoresizing autoresizing) {
		this.setAutoresizing(EnumSet.of(autoresizing));
	}

	public void setAutoresizing(Autoresizing first, Autoresizing... others) {
		this.setAutoresizing(EnumSet.of(first, others));
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		if(areAnimationsEnabled && currentViewAnimation != null) {
			currentViewAnimation.addAnimator(ViewAnimator.ofBackgroundColor(this, backgroundColor));
			return;
		}

		this.backgroundColor = backgroundColor;
		this.layer.setBackgroundColor(backgroundColor);
	}

	public boolean doesAutoresizeSubviews() {
		return autoresizesSubviews;
	}

	public void setAutoresizesSubviews(boolean autoresizesSubviews) {
		this.autoresizesSubviews = autoresizesSubviews;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

	public ViewLayer getLayer() {
		return this.layer;
	}

	public boolean isHidden() {
		return this.getLayer().isHidden();
	}

	public void setHidden(boolean hidden) {
		this.getLayer().setHidden(hidden);
	}

	public void setAlpha(float alpha) {
		if(areAnimationsEnabled && currentViewAnimation != null) {
			currentViewAnimation.addAnimator(ViewAnimator.ofAlpha(this, alpha));
		} else {
			this.getLayer().setAlpha(alpha);
		}
	}

	public float getAlpha() {
		return this.getLayer().getAlpha();
	}

	public boolean isUserInteractionEnabled() {
		return userInteractionEnabled;
	}

	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		this.userInteractionEnabled = userInteractionEnabled;
	}

	// Layout

	private boolean hasAutoresizingFor(int mask) {
		return (this.autoresizingMask & mask) != 0;
	}

	private void superviewSizeDidChange(Size oldSize, Size newSize) {
		if (this.autoresizingMask != Autoresizing.NONE.value) {
			Rect frame = this.getFrame();

			if(oldSize == null) oldSize = Size.zero();
			Size delta = new Size(newSize.width-oldSize.width, newSize.height-oldSize.height);

			if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value | Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value)) {
				frame.origin.y = floorf(frame.origin.y + (frame.origin.y / oldSize.height * delta.height));
				frame.size.height = floorf(frame.size.height + (frame.size.height / oldSize.height * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				float t = frame.origin.y + frame.size.height;
				frame.origin.y = floorf(frame.origin.y + (frame.origin.y / t * delta.height));
				frame.size.height = floorf(frame.size.height + (frame.size.height / t * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				frame.size.height = floorf(frame.size.height + (frame.size.height / (oldSize.height - frame.origin.y) * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value | Autoresizing.FLEXIBLE_TOP_MARGIN.value)) {
				frame.origin.y = floorf(frame.origin.y + (delta.height / 20.f));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_HEIGHT.value)) {
				frame.size.height = floorf(frame.size.height + delta.height);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value)) {
				frame.origin.y = floorf(frame.origin.y + delta.height);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value)) {
				frame.origin.y = floorf(frame.origin.y);
			}

			if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value | Autoresizing.FLEXIBLE_RIGHT_MARGIN.value)) {
				frame.origin.x = floorf(frame.origin.x + (frame.origin.x / oldSize.width * delta.width));
				frame.size.width = floorf(frame.size.width + (frame.size.width / oldSize.width * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				float t = frame.origin.x + frame.size.width;
				frame.origin.x = floorf(frame.origin.x + (frame.origin.x / t * delta.width));
				frame.size.width = floorf(frame.size.width + (frame.size.width / t * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				frame.size.width = floorf(frame.size.width + (frame.size.width / (oldSize.width - frame.origin.x) * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value | Autoresizing.FLEXIBLE_LEFT_MARGIN.value)) {
				frame.origin.x = floorf(frame.origin.x + (delta.width / 2.0f));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_WIDTH.value)) {
				frame.size.width = floorf(frame.size.width + delta.width);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value)) {
				frame.origin.x = floorf(frame.origin.x + delta.width);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value)) {
				frame.origin.x = floorf(frame.origin.x);
			}

			this.setFrame(frame);
		}
	}

	private void boundsDidChange(Rect oldBounds, Rect newBounds) {
		if(oldBounds == null || !oldBounds.equals(newBounds)) {
			this.setNeedsLayout();

			if (oldBounds == null || !oldBounds.size.equals(newBounds.size)) {
				if (this.autoresizesSubviews) {
					for (View subview : this.subviews) {
						subview.superviewSizeDidChange(oldBounds == null ? null : oldBounds.size, newBounds.size);
					}
				}
			}
		}
	}

	// Geometry

	Point convertPointToWindow(Point point) {
		View view = this;
		Point convertedPoint = new Point(point);

		while(view != null) {
			point.x += view.getFrame().origin.x - view.getBounds().origin.x;
			point.y += view.getFrame().origin.y - view.getBounds().origin.y;
			view = view.superview;
		}

		return point;
	}

	public Point convertPointToView(Point point, View view) {
		Point fromPoint = this.convertPointToWindow(Point.zero());
		Point toPoint = view.convertPointToWindow(Point.zero());

		float deltaX = fromPoint.x - toPoint.x;
		float deltaY = fromPoint.y - toPoint.y;

		return new Point(point.x + deltaX, point.y + deltaY);
	}

	public Point convertPointFromView(Point point, View view) {
		return view.convertPointToView(point, this);
	}

	public Rect convertRectToView(Rect rect, View view) {
		return new Rect(this.convertPointToView(rect.origin, view), rect.size);
	}

	public Rect convertRectFromView(Rect rect, View view) {
		return view.convertRectToView(rect, this);
	}

	public void sizeToFit() {
		Rect frame = this.getFrame();
		frame.size = this.sizeThatFits(frame.size);
		this.setFrame(frame);
	}

	public Size sizeThatFits(Size size) {
		return new Size(Math.min(this.frame.size.width, size.width), Math.min(this.frame.size.height, size.height));
	}

	public View hitTest(Point point, Event event) {
		if(this.isHidden() || !this.isUserInteractionEnabled() || this.getAlpha() < 0.01f || !this.pointInside(point, event)) {
			return null;
		} else {
			ArrayList<View> reverseSubviews = new ArrayList<View>(this.subviews);
			Collections.reverse(reverseSubviews);

			for(View subview : reverseSubviews) {
				View hitView = subview.hitTest(subview.convertPointFromView(point, this), event);

				if(hitView != null) {
					return hitView;
				}
			}

			return this;
		}
	}

	public boolean pointInside(Point point, Event event) {
		return this.getBounds().contains(point);
	}

	// Hierarchy

	ViewController _getViewController() {
		return _viewController;
	}

	void _setViewController(ViewController viewController) {
		this._viewController = viewController;
	}

	public Responder nextResponder() {
		return this._viewController != null ? this._viewController : this.superview;
	}

	public View getSuperview() {
		return superview;
	}

	public Window getWindow() {
		return superview != null ? superview.getWindow() : null;
	}

	public List<View> getSubviews() {
		if(subviews != null) {
			return Collections.unmodifiableList(subviews);
		} else {
			return new ArrayList<View>();
		}
	}

	// @property(nonatomic,readonly) UIWindow     *window;

	public void removeFromSuperview() {
		if(this.superview != null) {
			Window oldWindow = this.getWindow();

			this.superview.willRemoveSubview(this);
			this.willMoveWindows(oldWindow, null);
			this.willMoveToSuperview(null);
			this.layer.removeFromSuperlayer();
			this.superview.subviews.remove(this);
			this.superview = null;
			this.didMoveWindows(oldWindow, null);
			this.didMoveToSuperview();
			layer.didMoveToSuperlayer();
		}
	}

	public void insertSubview(View view, int index) {
		if(view.superview != null && view.superview != this) {
			view.superview.willRemoveSubview(view);
			view.layer.removeFromSuperlayer();
			view.superview.subviews.remove(view);
		}

		Window oldWindow = view.getWindow();
		Window newWindow = this.getWindow();

		view.willMoveWindows(oldWindow, newWindow);
		view.willMoveToSuperview(this);
		this.subviews.add(index, view);
		this.layer.insertSublayerAtIndex(view.layer, index);
		view.superview = this;
		view.didMoveWindows(oldWindow, newWindow);
		view.didMoveToSuperview();
		view.layer.didMoveToSuperlayer();
		this.didAddSubview(view);
	}

	public void exchangeSubviews(int index1, int index2) {
		if(index1 > index2) {
			int t = index2;
			index2 = index1;
			index1 = t;
		}

		ViewLayer layer1 = this.subviews.get(index1).getLayer();
		ViewLayer layer2 = this.subviews.get(index2).getLayer();

		Collections.swap(this.subviews, index1, index2);

		ViewLayer layer = this.getLayer();
		layer1.removeFromSuperlayer();
		layer2.removeFromSuperlayer();
		layer.insertSublayerAtIndex(layer2, index1);
		layer.insertSublayerAtIndex(layer1, index2);
		layer1.didMoveToSuperlayer();
		layer2.didMoveToSuperlayer();
	}

	public void addSubview(View view) {
		this.insertSubview(view, this.subviews.size());
	}

	public void insertSubviewBelowSubview(View view, View belowSubview) {
		int index = this.subviews.indexOf(belowSubview);
		this.insertSubview(view, index <= 0 ? 0 : index - 1);
	}

	public void insertSubviewAboveSubview(View view, View aboveSubview) {
		int index = this.subviews.indexOf(aboveSubview);
		this.insertSubview(view, index < 0 ? 0 : index + 1);
	}

	public void bringSubviewToFront(View view) {
		view.layer.removeFromSuperlayer();
		this.layer.addSublayer(view.layer);
		this.subviews.remove(view);
		this.subviews.add(view);
	}

	public void sendSubviewToBack(View view) {
		if(view.layer.getSuperlayer() != this.layer) return;

		view.layer.removeFromSuperlayer();
		this.layer.insertSublayerAtIndex(view.layer, 0);
		this.subviews.remove(view);
		this.subviews.add(0, view);
		view.layer.didMoveToSuperlayer();
	}

	public void didAddSubview(View subview) {

	}

	public void willRemoveSubview(View subview) {

	}

	public void willMoveToSuperview(View newSuperview) {

	}

	public void didMoveToSuperview() {

	}

	public void willMoveToWindow(Window newWindow) {

	}

	public void didMoveToWindow() {

	}

	private void willMoveWindows(Window oldWindow, Window newWindow) {
		if(oldWindow != newWindow) {
			this.willMoveToWindow(newWindow);

			for(View subview : this.subviews) {
				subview.willMoveWindows(oldWindow, newWindow);
			}
		}
	}

	private void didMoveWindows(Window oldWindow, Window newWindow) {
		if(oldWindow != newWindow) {
			this.didMoveToWindow();

			for(View subview : this.subviews) {
				subview.didMoveWindows(oldWindow, newWindow);
			}
		}
	}

	// returns YES for self.
	public View viewWithTag(int tag) {
		View foundView = null;

		if (this.tag == tag) {
			foundView = this;
		} else if(this.subviews.size() > 0) {
			for (View view : this.subviews) {
				foundView = view.viewWithTag(tag);
				if (foundView != null) break;
			}
		}

		return foundView;
	}

	// recursive search. includes self
	public boolean isDescendantOfView(View view) {
		if (view != null) {
			View testView = this;

			while (testView != null) {
				if (testView == view) {
					return true;
				} else {
					testView = testView.superview;
				}
			}
		}

		return false;
	}


	// Allows you to perform layer before the drawing cycle happens. -layoutIfNeeded forces layer early
	public void setNeedsLayout() {
		this.needsLayout = true;
		this.layer.setNeedsLayout();
	}

	public void layoutIfNeeded() {
		if(this.needsLayout) {
			this.layoutSubviews();
			this.needsLayout = false;
		}
	}

	void _layoutSubviews() {
		if(this._viewController != null) {
			this._viewController.viewWillLayoutSubviews();
		}

		this.layoutSubviews();

		if(this._viewController != null) {
			this._viewController.viewDidLayoutSubviews();
		}
	}

	public void layoutSubviews() {
		this.needsLayout = false;
	}

	// Touches
	public void touchesBegan(List<Touch> touches, Event event) {
		// MLog(this.toString() + " touchesBegan");
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		// MLog(this.toString() + " touchesMoved");
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		// MLog(this.toString() + " touchesEnded");
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		// MLog(this.toString() + " touchesCancelled");
	}

	public String toString() {
		return String.format("<%s: 0x%d; frame = %s; hidden = %b; layer = %s>", this.getClass().toString(), this.hashCode(), this.getFrame().toString(), this.isHidden(), this.getLayer().toString());
	}

	// Rendering

	public void setNeedsDisplay() {
		this.layer.setNeedsDisplay();
	}

	public void setNeedsDisplay(Rect dirtyRect) {
		this.layer.setNeedsDisplay(dirtyRect);
	}

	public void draw(mocha.graphics.Context context, Rect rect) {

	}

	// Gestures

	public void addGestureRecognizer(GestureRecognizer gestureRecognizer) {
		if(!this.gestureRecognizers.contains(gestureRecognizer)) {
			this.gestureRecognizers.add(gestureRecognizer);
			gestureRecognizer.setView(this);
		}
	}

	public void removeGestureRecognizer(GestureRecognizer gestureRecognizer) {
		if(this.gestureRecognizers.contains(gestureRecognizer)) {
			gestureRecognizer.setView(null);
			this.gestureRecognizers.remove(gestureRecognizer);
		}
	}

	public List<GestureRecognizer> getGestureRecognizers() {
		return Collections.unmodifiableList(this.gestureRecognizers);
	}

	// Helpers

	public static int ceil(float f) {
		return (int)Math.ceil((double)f);
	}

	public static int floor(float f) {
		return (int)f;
	}

	public static int round(float f) {
		return (int)(f + 0.5f);
	}

	public static float ceilf(float f) {
		return (float)((int)Math.ceil((double)f));
	}

	public static float floorf(float f) {
		return (float)((int)f);
	}

	public static float roundf(float f) {
		return (float)((int)(f + 0.5f));
	}

	public static float clampf(float value, float minimum, float maximum) {
		return Math.min(Math.max(value, minimum), maximum);
	}

	public static float degreesToRadians(float degrees) {
		return (degrees / 360.0f) * ((float)Math.PI * 2.0f);
	}

	public static float radiansToDegrees(float radians) {
		return (radians / ((float)Math.PI * 2)) * 360.0f;
	}

	// Layer backing

	public Class<? extends ViewLayer> getLayerClass() {
		if(USE_GL_LAYERS) {
			return ViewLayerGL.class;
		} else {
			return ViewLayerNative.class;
		}
	}

	// Animations

	private static ArrayList<ViewAnimation> viewAnimationStack = new ArrayList<ViewAnimation>();
	static ViewAnimation currentViewAnimation;
	static boolean areAnimationsEnabled = true;

	public static void animateWithDuration(long duration, Animations animations) {
		animateWithDuration(duration, animations, null);
	}

	public static void animateWithDuration(long duration, Animations animations, final AnimationCompletion completion) {
		beginAnimations(null, null);
		setAnimationDuration(duration);

		if(completion != null) {
			setAnimationDidStopCallback(new AnimationDidStop() {
				public void animationDidStop(String animationID, boolean finished, Object context) {
					completion.animationCompletion(finished);
				}
			});
		}

		animations.performAnimatedChanges();

		commitAnimations();
	}

	public static void beginAnimations(String animationID, Object context) {
		currentViewAnimation = new ViewAnimation();
		currentViewAnimation.animationID = animationID;
		currentViewAnimation.context = context;
		viewAnimationStack.add(currentViewAnimation);
	}

	public static void commitAnimations() {
		int size = viewAnimationStack.size();
		ViewAnimation viewAnimation = viewAnimationStack.remove(size - 1);
		size--;

		if(size > 0) {
			currentViewAnimation = viewAnimationStack.get(size - 1);
		} else {
			currentViewAnimation = null;
		}

		viewAnimation.start();
	}

	public static boolean isInAnimationContext() {
		return currentViewAnimation != null;
	}

	public static void setAnimationDidStartCallback(AnimationDidStart animationDidStartCallback) {
		if(currentViewAnimation != null) {
			currentViewAnimation.didStart = animationDidStartCallback;
		}
	}

	public static void setAnimationDidStopCallback(AnimationDidStop animationDidStopCallback) {
		if(currentViewAnimation != null) {
			currentViewAnimation.didStop = animationDidStopCallback;
		}
	}

	public static void setAnimationDuration(long duration) {
		if(currentViewAnimation != null) {
			currentViewAnimation.duration = duration;
		}
	}

	public static void setAnimationDelay(long delay) {
		if(currentViewAnimation != null) {
			currentViewAnimation.delay = delay;
		}
	}

	public static void setAnimationCurve(AnimationCurve animationCurve) {
		if(currentViewAnimation != null) {
			currentViewAnimation.animationCurve = animationCurve;
		}
	}

	/**
	 * Temporarily enables/disables animations while runnable is running
	 * restores previous state upon completion.
	 *
	 * @param enabled whether or not animations should be enabled for this session
	 * @param runnable logic to run during session
	 */
	public static void setAnimationsEnabled(boolean enabled, Runnable runnable) {
		boolean wereEnabled = areAnimationsEnabled;
		areAnimationsEnabled = enabled;

		runnable.run();

		areAnimationsEnabled = wereEnabled;
	}

	/**
	 * On by default, turning off will disable any animations, including if you're in an animation block.
	 *
	 * @param enabled whether or not animations should be enabled
	 */
	public static void setAnimationsEnabled(boolean enabled) {
		areAnimationsEnabled = enabled;
	}

	public static boolean areAnimationsEnabled() {
		return areAnimationsEnabled;
	}

	static class ViewAnimation {
		// We use a hash so if the same property is changed multiple times in the animation
		// we just override the last one.
		private HashMap<String,ViewAnimator> animators;
		long duration = 200;
		long delay = 0;
		AnimationCurve animationCurve = AnimationCurve.EASE_IN_OUT;
		AnimationDidStart didStart;
		AnimationDidStop didStop;
		String animationID;
		Object context;

		void addAnimator(ViewAnimator animator) {
			if(this.animators == null) {
				this.animators = new HashMap<String, ViewAnimator>();
			}

			this.animators.put(animator.getView().hashCode() + "-" + animator.getProperty(), animator);
		}

		void start() {
			if(this.animators == null || this.animators.size() == 0) {
				if(didStart != null) didStart.animationDidStart(animationID, context);
				if(didStop != null) didStop.animationDidStop(animationID, true, context);
				return;
			}

			List<Animator> animators = new ArrayList<Animator>(this.animators.values());
			Animator animator;

			if(animators.size() == 1) {
				animator = animators.get(0);
			} else {
				animator = AnimatorSet.withAnimators(animators);
			}

			long timeModifier = SLOW_ANIMATIONS ? 10 : 1;

			animator.setDuration(duration * timeModifier);
			animator.setStartDelay(delay * timeModifier);

			if(didStart != null || didStop != null) {
				animator.addListener(new Animator.Listener() {
					public void onAnimationStart(Animator animator) {
						if (didStart != null) {
							didStart.animationDidStart(animationID, context);
						}
					}

					public void onAnimationEnd(Animator animator) {
						if (didStop != null) {
							didStop.animationDidStop(animationID, true, context);
						}
					}

					public void onAnimationCancel(Animator animator) {
						if (didStop != null) {
							didStop.animationDidStop(animationID, false, context);
						}
					}

					public void onAnimationRepeat(Animator animator) {

					}
				});
			}

			switch (animationCurve) {
				case EASE_IN:
					animator.setTimingFunction(TimingFunction.EASE_IN);
					break;
				case EASE_OUT:
					animator.setTimingFunction(TimingFunction.EASE_OUT);
					break;
				case LINEAR:
					animator.setTimingFunction(TimingFunction.LINEAR);
					break;
				case EASE_IN_OUT:
				default:
					animator.setTimingFunction(TimingFunction.EASE_IN_OUT);
					break;
			}

			animator.start();
		}

	}
}
