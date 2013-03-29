/**
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright 2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import mocha.graphics.AffineTransform;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.*;

public class View extends Responder implements Accessibility {
	static final Class<? extends ViewLayer> VIEW_LAYER_CLASS = ViewLayerNative.class;
	static final Class<? extends WindowLayer> WINDOW_LAYER_CLASS = WindowLayerNative.class;

	public static boolean SLOW_ANIMATIONS = false;
	public static boolean SHOW_DROPPED_ANIMATION_FRAMES = false;

	private static final int AUTORESIZING_NONE = 0;
	private static final int AUTORESIZING_FLEXIBLE_LEFT_MARGIN = 1 << 0;
	private static final int AUTORESIZING_FLEXIBLE_WIDTH = 1 << 1;
	private static final int AUTORESIZING_FLEXIBLE_RIGHT_MARGIN = 1 << 2;
	private static final int AUTORESIZING_FLEXIBLE_TOP_MARGIN = 1 << 3;
	private static final int AUTORESIZING_FLEXIBLE_HEIGHT = 1 << 4;
	private static final int AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN = 1 << 5;

	public enum Autoresizing {
		NONE(AUTORESIZING_NONE),
		FLEXIBLE_LEFT_MARGIN(AUTORESIZING_FLEXIBLE_LEFT_MARGIN),
		FLEXIBLE_WIDTH(AUTORESIZING_FLEXIBLE_WIDTH),
		FLEXIBLE_RIGHT_MARGIN(AUTORESIZING_FLEXIBLE_RIGHT_MARGIN),
		FLEXIBLE_TOP_MARGIN(AUTORESIZING_FLEXIBLE_TOP_MARGIN),
		FLEXIBLE_HEIGHT(AUTORESIZING_FLEXIBLE_HEIGHT),
		FLEXIBLE_BOTTOM_MARGIN(AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN);

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

	public enum ContentMode {
		/**
		 * Scales content to fill the entire bounds
		 * Aspect ratio will be changed if necessary
		 */
		SCALE_TO_FILL,

		/**
		 * Contents scaled to fit with fixed aspect.
		 * Remainder is transparent.
		 */
		SCALE_ASPECT_FIT,

		/**
		 * Contents scaled to fill with fixed aspect ratio.
		 * Some portion of content may be clipped
		 */
		SCALE_ASPECT_FILL,

		/**
		 * Calls setNeedsDisplay on bounds change
		 */
		REDRAW,

		/**
		 * Aligned to the center vertically and horizontally
		 * Content size will not be changed
		 */
		CENTER,

		/**
		 * Aligned to the top vertically, aligned to the center horizontally
		 * Content size will not be changed
		 */
		TOP,

		/**
		 * Aligned to the bottom vertically, aligned to the center horizontally
		 * Content size will not be changed
		 */
		BOTTOM,

		/**
		 * Aligned to the center vertically, aligned to the left horizontally
		 * Content size will not be changed
		 */
		LEFT,

		/**
		 * Aligned to the center vertically, aligned to the right horizontally
		 * Content size will not be changed
		 */
		RIGHT,

		/**
		 * Aligned to the top vertically, aligned to the left horizontally
		 * Content size will not be changed
		 */
		TOP_LEFT,

		/**
		 * Aligned to the top vertically, aligned to the right horizontally
		 * Content size will not be changed
		 */
		TOP_RIGHT,

		/**
		 * Aligned to the bottom vertically, aligned to the left horizontally
		 * Content size will not be changed
		 */
		BOTTOM_LEFT,

		/**
		 * Aligned to the bottom vertically, aligned to the right horizontally
		 * Content size will not be changed
		 */
		BOTTOM_RIGHT,
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
	private ContentMode contentMode;
	private boolean multipleTouchEnabled;
	Touch trackingSingleTouch;

	private ViewController _viewController;

	// Accesibility
	private boolean isAccessibilityElement;
	private String accessibilityLabel;
	private String accessibilityHint;
	private String accessibilityValue;
	private Trait[] accessibilityTraits;
	private Rect accessibilityFrame;
	private Point accessibilityActivationPoint;
	private boolean accessibilityElementsHidden;
	private boolean accessibilityViewIsModal;
	private boolean shouldGroupAccessibilityChildren;

	public View() {
		this(Application.sharedApplication().getContext());
	}

	public View(Rect frame) {
		this(Application.sharedApplication().getContext(), frame);
	}

	View(android.content.Context context) {
		this(context, Rect.zero());
	}

	View(android.content.Context context, Rect frame) {
		this.scale = Screen.mainScreen().getScale();

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
		this.contentMode = ContentMode.SCALE_TO_FILL;
		this.multipleTouchEnabled = false;

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
			this.layer.setClipsToBounds(clipsToBounds);
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

		if(!frame.equals(this.frame)) {
			if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
				currentViewAnimation.addAnimation(this, ViewAnimation.Type.FRAME, frame.copy());
				this.frame = frame.copy();
				return;
			}

			if(this.superview != null && !this.superview.getBounds().contains(this.frame)) {
				// Fixes a weird bug in Android that leaves artifacts on the screen
				// if the old frame wasn't fully contained within the bounds.
				this.superview.setNeedsDisplay(this.frame);
			}

			Rect oldBounds = this.bounds;

			this.frame = frame.copy();
			boolean boundsChanged;

			if(!this.bounds.size.equals(this.frame.size)) {
				this.bounds = new Rect(this.bounds.origin, this.frame.size);
				boundsChanged = true;
			} else {
				boundsChanged = false;
			}

			this.layer.setFrame(this.frame, this.bounds);

			if(boundsChanged) {
				this.boundsDidChange(oldBounds, this.bounds);
			}
		}
	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public void setBounds(Rect bounds) {
		if(bounds == null) {
			bounds = Rect.zero();
		}

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, ViewAnimation.Type.BOUNDS, bounds);
			this.bounds = bounds.copy();
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
			this.boundsDidChange(oldBounds, this.bounds);
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
		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, ViewAnimation.Type.BACKGROUND_COLOR, backgroundColor);
			this.backgroundColor = backgroundColor;
			return;
		}

		this.backgroundColor = backgroundColor;
		this.layer.setBackgroundColor(backgroundColor);
	}

	public void setTransform(AffineTransform transform) {
		if(transform == null) {
			transform = AffineTransform.identity();
		} else {
			transform = transform.copy();
		}

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, ViewAnimation.Type.TRANSFORM, transform);
		} else {
			this.layer.setTransform(transform);
		}
	}

	public AffineTransform getTransform() {
		return this.layer.getTransform();
	}

	public boolean doesAutoresizeSubviews() {
		return autoresizesSubviews;
	}

	public void setAutoresizesSubviews(boolean autoresizesSubviews) {
		this.autoresizesSubviews = autoresizesSubviews;
	}

	public ContentMode getContentMode() {
		return contentMode;
	}

	public void setContentMode(ContentMode contentMode) {
		if(contentMode == null) {
			contentMode = ContentMode.SCALE_TO_FILL;
		}

		this.contentMode = contentMode;
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
		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, ViewAnimation.Type.ALPHA, alpha);
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

	public boolean isMultipleTouchEnabled() {
		return multipleTouchEnabled;
	}

	public void setMultipleTouchEnabled(boolean multipleTouchEnabled) {
		this.multipleTouchEnabled = multipleTouchEnabled;
	}

	/**
	 * Change the backing layer type, if we're using ViewLayerNative.
	 * Changing this shouldn't be necessary unless you're using something like
	 * blending modes in your view's draw() method that isn't supported by
	 * hardware layers yet.
	 *
	 * @param hardwareAccelerationEnabled Whether or not hardware acceleration is enabled
	 */
	public void setHardwareAccelerationEnabled(boolean hardwareAccelerationEnabled) {
		ViewLayer layer = this.getLayer();

		if(layer instanceof ViewLayerNative) {
			if(hardwareAccelerationEnabled) {
				((ViewLayerNative) layer).setLayerType(android.view.View.LAYER_TYPE_NONE, null);
			} else {
				((ViewLayerNative) layer).setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
			}
		}
	}

	/**
	 * @return Whether or not the view is hardware accelerated
	 */
	public boolean isHardwareAccelerationEnabled() {
		ViewLayer layer = this.getLayer();
		return layer instanceof ViewLayerNative && ((ViewLayerNative) layer).isHardwareAccelerated();
	}

	// Layout

	private boolean hasAutoresizingFor(int mask) {
		return (this.autoresizingMask & mask) != 0;
	}

	private void superviewSizeDidChange_(Size oldSize, Size newSize) {
		// NOTE: This is a port from Chameleon and is slightly faster than the version below however the
		// logic is buggy and doesn't always scale correctly (specifically width+height only scaling, scales margins anyway)

		if (this.autoresizingMask != Autoresizing.NONE.value) {
			Rect frame = this.getFrame();

			if(oldSize == null) oldSize = Size.zero();
			Size delta = new Size(newSize.width-oldSize.width, newSize.height-oldSize.height);

			if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value | Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value)) {
				frame.origin.y = roundf(frame.origin.y + (frame.origin.y / oldSize.height * delta.height));
				frame.size.height = roundf(frame.size.height + (frame.size.height / oldSize.height * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				float t = frame.origin.y + frame.size.height;
				frame.origin.y = roundf(frame.origin.y + (frame.origin.y / t * delta.height));
				frame.size.height = roundf(frame.size.height + (frame.size.height / t * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				frame.size.height = roundf(frame.size.height + (frame.size.height / (oldSize.height - frame.origin.y) * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value | Autoresizing.FLEXIBLE_TOP_MARGIN.value)) {
				frame.origin.y = roundf(frame.origin.y + (delta.height / 20.f));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_HEIGHT.value)) {
				frame.size.height = roundf(frame.size.height + delta.height);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value)) {
				frame.origin.y = roundf(frame.origin.y + delta.height);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value)) {
				frame.origin.y = roundf(frame.origin.y);
			}

			if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value | Autoresizing.FLEXIBLE_RIGHT_MARGIN.value)) {
				frame.origin.x = roundf(frame.origin.x + (frame.origin.x / oldSize.width * delta.width));
				frame.size.width = roundf(frame.size.width + (frame.size.width / oldSize.width * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				float t = frame.origin.x + frame.size.width;
				frame.origin.x = roundf(frame.origin.x + (frame.origin.x / t * delta.width));
				frame.size.width = roundf(frame.size.width + (frame.size.width / t * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				frame.size.width = roundf(frame.size.width + (frame.size.width / (oldSize.width - frame.origin.x) * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value | Autoresizing.FLEXIBLE_LEFT_MARGIN.value)) {
				frame.origin.x = roundf(frame.origin.x + (delta.width / 2.0f));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_WIDTH.value)) {
				frame.size.width = roundf(frame.size.width + delta.width);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value)) {
				frame.origin.x = roundf(frame.origin.x + delta.width);
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value)) {
				frame.origin.x = roundf(frame.origin.x);
			}

			this.setFrame(frame);
		}
	}

	private void superviewSizeDidChange(Size oldSuperviewSize, Size newSuperviewSize) {
		int mask = this.autoresizingMask;
		if(mask == AUTORESIZING_NONE) return;

		Point origin = this.frame.origin.copy();
		Size size = this.frame.size.copy();
		float delta;

		if (oldSuperviewSize.width != 0.0f || size.width == 0.0f) {
			int horizontalMask = (mask & AUTORESIZING_FLEXIBLE_LEFT_MARGIN) + (mask & AUTORESIZING_FLEXIBLE_WIDTH) + (mask & AUTORESIZING_FLEXIBLE_RIGHT_MARGIN);

			if(horizontalMask != AUTORESIZING_NONE) {
				if(horizontalMask == AUTORESIZING_FLEXIBLE_LEFT_MARGIN) {
					origin.x += newSuperviewSize.width - oldSuperviewSize.width;
				} else if(horizontalMask == AUTORESIZING_FLEXIBLE_WIDTH) {
					size.width = newSuperviewSize.width - (oldSuperviewSize.width - this.frame.size.width);
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH)) {
					delta = (oldSuperviewSize.width - this.frame.size.width - this.frame.origin.x);
					origin.x = (this.frame.origin.x / (oldSuperviewSize.width - delta)) * (newSuperviewSize.width - delta);
					size.width = newSuperviewSize.width - origin.x - delta;
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_RIGHT_MARGIN)) {
					delta = (oldSuperviewSize.width - this.frame.size.width - this.frame.origin.x);
					origin.x += (newSuperviewSize.width - oldSuperviewSize.width) * (this.frame.origin.x / (this.frame.origin.x + delta));
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_RIGHT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH)) {
					delta = (oldSuperviewSize.width - this.frame.size.width - this.frame.origin.x);
					float scaledRightMargin = (delta / (oldSuperviewSize.width - this.frame.origin.x)) * (newSuperviewSize.width - this.frame.origin.x);
					size.width = newSuperviewSize.width - origin.x - scaledRightMargin;
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH | AUTORESIZING_FLEXIBLE_RIGHT_MARGIN)) {
					origin.x = (this.frame.origin.x / oldSuperviewSize.width) * newSuperviewSize.width;
					size.width = (this.frame.size.width / oldSuperviewSize.width) * newSuperviewSize.width;
				}
			}
		}

		if (oldSuperviewSize.height != 0 || size.height == 0) {
			int verticalMask = (mask & AUTORESIZING_FLEXIBLE_TOP_MARGIN) + (mask & AUTORESIZING_FLEXIBLE_HEIGHT) + (mask & AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN);

			if(verticalMask != AUTORESIZING_NONE) {
				if(verticalMask == AUTORESIZING_FLEXIBLE_TOP_MARGIN) {
					origin.y += newSuperviewSize.height - oldSuperviewSize.height;
				} else if(verticalMask == AUTORESIZING_FLEXIBLE_HEIGHT) {
					size.height = newSuperviewSize.height - (oldSuperviewSize.height - this.frame.size.height);
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT)) {
					delta = (oldSuperviewSize.height - this.frame.size.height - this.frame.origin.y);
					origin.y = (this.frame.origin.y / (oldSuperviewSize.height - delta)) * (newSuperviewSize.height - delta);
					size.height = newSuperviewSize.height - origin.y - delta;
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN)) {
					delta = (oldSuperviewSize.height - this.frame.size.height - this.frame.origin.y);
					origin.y += (newSuperviewSize.height - oldSuperviewSize.height) * (this.frame.origin.y / (this.frame.origin.y + delta));
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT)) {
					delta = (oldSuperviewSize.height - this.frame.size.height - this.frame.origin.y);
					float scaledBottomMargin = (delta / (oldSuperviewSize.height - this.frame.origin.y)) * (newSuperviewSize.height - this.frame.origin.y);
					size.height = newSuperviewSize.height - origin.y - scaledBottomMargin;
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT | AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN)) {
					origin.y = (this.frame.origin.y / oldSuperviewSize.height) * newSuperviewSize.height;
					size.height = (this.frame.size.height / oldSuperviewSize.height) * newSuperviewSize.height;
				}
			}
		}

		if(!this.frame.origin.equals(origin) || !this.frame.size.equals(size)) {
			this.setFrame(new Rect(origin, size));
		}
	}

	void boundsDidChange(Rect oldBounds, Rect newBounds) {
		if(oldBounds == null || !oldBounds.equals(newBounds)) {
			this.setNeedsLayout();

			if (oldBounds == null || !oldBounds.size.equals(newBounds.size)) {
				if (this.autoresizesSubviews) {
					for (View subview : this.subviews) {
						subview.superviewSizeDidChange(oldBounds == null ? null : oldBounds.size, newBounds.size);
					}
				}
			}

			// TODO: Fix this. This shouldn't be needed, there's a bug with ViewLayerNative
			// related to requestLayout, it doesn't get processed in certain scenarios.
			this.layoutIfNeeded();
		}
	}

	// Geometry

	Point convertPointToWindow(Point point) {
		View view = this;
		Point convertedPoint = point.copy();

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
		return this.getTransform().apply(this.getBounds()).contains(point);
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
		if(view.superview != this) return;

		view.layer.removeFromSuperlayer();
		this.layer.addSublayer(view.layer);
		this.subviews.remove(view);
		this.subviews.add(view);
	}

	public void sendSubviewToBack(View view) {
		if(view.superview != this) return;

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

			if(newWindow == null && this.isFirstResponder()) {
				this.resignFirstResponder();
			}

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
		if(this.superview == null) return;

		if(this.needsLayout) {
			if(this._viewController != null) {
				this._viewController.viewWillLayoutSubviews();
			}

			this.layoutSubviews();
			this.needsLayout = false;

			if(this._viewController != null) {
				this._viewController.viewDidLayoutSubviews();
			}

		}
	}

	public void layoutSubviews() {
		this.needsLayout = false;
	}

	public String toString() {
		return String.format("<%s: 0x%d; frame = %s; hidden = %b%s>", this.getClass().toString(), this.hashCode(), this.getFrame().toString(), this.isHidden(), (this.tag != 0 ? "; tag = "+this.tag : ""));
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
		return (int)FloatMath.ceil(f);
	}

	public static int ceil(double d) {
		return (int)Math.ceil(d);
	}

	public static int floor(float f) {
		return (int)FloatMath.floor(f);
	}

	public static int floor(double d) {
		return (int)Math.floor(d);
	}

	public static int round(float f) {
		return (int)FloatMath.floor(f + 0.5f);
	}

	public static int round(double d) {
		return (int)Math.round(d);
	}

	public static float ceilf(float f) {
		return FloatMath.ceil(f);
	}

	public static float floorf(float f) {
		return FloatMath.floor(f);
	}

	public static float roundf(float f) {
		return FloatMath.floor(f + 0.5f);
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
		return VIEW_LAYER_CLASS;
	}

	// Accessibility

	public boolean isAccessibilityElement() {
		return isAccessibilityElement;
	}

	public void setIsAccessibilityElement(boolean accessibilityElement) {
		isAccessibilityElement = accessibilityElement;
	}

	public String getAccessibilityLabel() {
		return accessibilityLabel;
	}

	public void setAccessibilityLabel(String accessibilityLabel) {
		this.accessibilityLabel = accessibilityLabel;
	}

	public String getAccessibilityHint() {
		return accessibilityHint;
	}

	public void setAccessibilityHint(String accessibilityHint) {
		this.accessibilityHint = accessibilityHint;
	}

	public String getAccessibilityValue() {
		return accessibilityValue;
	}

	public void setAccessibilityValue(String accessibilityValue) {
		this.accessibilityValue = accessibilityValue;
	}

	public Trait[] getAccessibilityTraits() {
		return accessibilityTraits;
	}

	public void setAccessibilityTraits(Trait... accessibilityTraits) {
		this.accessibilityTraits = accessibilityTraits;
	}

	public Rect getAccessibilityFrame() {
		return accessibilityFrame;
	}

	public void setAccessibilityFrame(Rect accessibilityFrame) {
		this.accessibilityFrame = accessibilityFrame;
	}

	public Point getAccessibilityActivationPoint() {
		return accessibilityActivationPoint;
	}

	public void setAccessibilityActivationPoint(Point accessibilityActivationPoint) {
		this.accessibilityActivationPoint = accessibilityActivationPoint;
	}

	public boolean getAccessibilityElementsHidden() {
		return accessibilityElementsHidden;
	}

	public void setAccessibilityElementsHidden(boolean accessibilityElementsHidden) {
		this.accessibilityElementsHidden = accessibilityElementsHidden;
	}

	public boolean getAccessibilityViewIsModal() {
		return accessibilityViewIsModal;
	}

	public void setAccessibilityViewIsModal(boolean accessibilityViewIsModal) {
		this.accessibilityViewIsModal = accessibilityViewIsModal;
	}

	public boolean shouldGroupAccessibilityChildren() {
		return shouldGroupAccessibilityChildren;
	}

	public void setShouldGroupAccessibilityChildren(boolean shouldGroupAccessibilityChildren) {
		this.shouldGroupAccessibilityChildren = shouldGroupAccessibilityChildren;
	}

	// Animations

	public enum AnimationOption {
		LAYOUT_SUBVIEWS,
		ALLOW_USER_INTERACTION,
		/*BEGIN_FROM_CURRENT_STATE,
		REPEAT,
		AUTOREVERSE,
		OVERRIDE_INHERITED_DURATION,
		OVERRIDE_INHERITED_CURVE,
		ALLOW_ANIMATED_CONTENT,*/
		SHOW_HIDE_TRANSITION_VIEWS,

		CURVE_EASE_IN_OUT,
		CURVE_EASE_IN,
		CURVE_EASE_OUT,
		CURVE_LINEAR,

		TRANSITION_NONE,
		/*TRANSITION_FLIP_FROM_LEFT,
		TRANSITION_FLIP_FROM_RIGHT,
		TRANSITION_CURL_UP,
		TRANSITION_CURL_DOWN,*/
		TRANSITION_CROSS_DISSOLVE,
		/*TRANSITION_FLIP_FROM_TOP,
		TRANSITION_FLIP_FROM_BOTTOM*/
	}

	/**
	 * Cancels any animation blocks referencing this view. This will apply to
	 * ALL view's in an animation block and not just this view. The animating
	 * properties will be left the state they were upon cancellation. Meaning,
	 * if alpha was animating from 0.0f to 1.0f and the animation is cancelled
	 * half way through, the final alpha value will be 0.5f.
	 *
	 * NOTE: Calling this in an animation block will have no affect and be ignored.
	 */
	public void cancelAnimations() {
		if(currentViewAnimation != null) return;
		ViewAnimation.cancelAllAnimationsReferencingView(this);
	}

	private static ArrayList<ViewAnimation> viewAnimationStack = new ArrayList<ViewAnimation>();
	static ViewAnimation currentViewAnimation;
	static boolean areAnimationsEnabled = true;

	public static void animateWithDuration(long duration, Animations animations) {
		animateWithDuration(duration, animations, null);
	}

	public static void animateWithDuration(long duration, Animations animations, final AnimationCompletion completion) {
		animateWithDuration(duration, 0, animations, completion);
	}


	public static void animateWithDuration(long duration, long delay, Animations animations, final AnimationCompletion completion) {
		beginAnimations(null, null);
		setAnimationDuration(duration);
		setAnimationDelay(delay);

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

	public static void beginAnimations() {
		beginAnimations(null, null);
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

	public static void transition(final View fromView, final View toView, long duration, final AnimationCompletion completion, AnimationOption... options) {
		final EnumSet<AnimationOption> _options;

		if(options == null || options.length == 0) {
			_options = EnumSet.noneOf(AnimationOption.class);
		} else if(options.length == 1) {
			_options = EnumSet.of(options[0]);
		} else {
			_options = EnumSet.of(options[0], options);
		}

		View superview = fromView.getSuperview();
		final boolean showHide = _options.contains(AnimationOption.SHOW_HIDE_TRANSITION_VIEWS);
		final float toAlpha = toView.getAlpha();
		final float fromAlpha = fromView.getAlpha();

		boolean restore = areAnimationsEnabled();
		setAnimationsEnabled(false);
		toView.setAlpha(0.0f);

		if(showHide) {
			toView.setHidden(false);
		} else {
			superview.insertSubviewBelowSubview(toView, fromView);
		}

		if(duration <= 0) {
			toView.setAlpha(toAlpha);

			if(showHide) {
				fromView.setHidden(true);
			} else {
				fromView.removeFromSuperview();
			}

			setAnimationsEnabled(restore);

			if(completion != null) {
				performAfterDelay(0, new Runnable() {
					public void run() {
						completion.animationCompletion(true);
					}
				});
			}
		} else {
			setAnimationsEnabled(restore);

			final boolean fromInteractionEnabled = fromView.isUserInteractionEnabled();
			final boolean toInteractionEnabled = toView.isUserInteractionEnabled();

			if(!_options.contains(AnimationOption.ALLOW_USER_INTERACTION)) {
				fromView.setUserInteractionEnabled(false);
				toView.setUserInteractionEnabled(false);
			}

			animateWithDuration(duration, new Animations() {
				public void performAnimatedChanges() {
					applyCurveOptions(_options);

					toView.setAlpha(toAlpha);
					fromView.setAlpha(0.0f);

					if(_options.contains(AnimationOption.LAYOUT_SUBVIEWS)) {
						toView.layoutIfNeeded();
						fromView.layoutIfNeeded();
					}
				}
			}, new AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					if(showHide) {
						fromView.setHidden(true);
					} else {
						fromView.removeFromSuperview();
					}

					fromView.setAlpha(fromAlpha);

					fromView.setUserInteractionEnabled(fromInteractionEnabled);
					toView.setUserInteractionEnabled(toInteractionEnabled);

					if(completion != null) {
						completion.animationCompletion(finished);
					}
				}
			});
		}

	}

	private static void applyCurveOptions(EnumSet<AnimationOption> options) {
		if(options.contains(AnimationOption.CURVE_EASE_IN)) {
			View.setAnimationCurve(AnimationCurve.EASE_IN);
		} else if(options.contains(AnimationOption.CURVE_EASE_OUT)) {
			View.setAnimationCurve(AnimationCurve.EASE_OUT);
		} else if(options.contains(AnimationOption.CURVE_LINEAR)) {
			View.setAnimationCurve(AnimationCurve.LINEAR);
		} else {
			View.setAnimationCurve(AnimationCurve.EASE_IN_OUT);
		}
	}

}
