/**
 *  @author Shaun
 *	@date 11/13/12
 *	@copyright 2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.FloatMath;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.ViewGroup;
import mocha.animation.TimingFunction;
import mocha.graphics.AffineTransform;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class View extends Responder implements Accessibility {
	static final Class<? extends ViewLayer> VIEW_LAYER_CLASS = ViewLayerNative2.class;
	static final Class<? extends WindowLayer> WINDOW_LAYER_CLASS = WindowLayerNative2.class;

	/**
	 * Set to true to make all animations take 10x longer than normal
	 * @hide
	 */
	public static boolean SLOW_ANIMATIONS = false;

	/**
	 * Set to true to log dropped frames duration animations
	 * @hide
	 */
	public static boolean SHOW_DROPPED_ANIMATION_FRAMES = false;

	private static final int AUTORESIZING_NONE = 0;
	private static final int AUTORESIZING_FLEXIBLE_LEFT_MARGIN = 1 << 0;
	private static final int AUTORESIZING_FLEXIBLE_WIDTH = 1 << 1;
	private static final int AUTORESIZING_FLEXIBLE_RIGHT_MARGIN = 1 << 2;
	private static final int AUTORESIZING_FLEXIBLE_TOP_MARGIN = 1 << 3;
	private static final int AUTORESIZING_FLEXIBLE_HEIGHT = 1 << 4;
	private static final int AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN = 1 << 5;

	public enum Autoresizing {
		/**
		 * View does not resize
		 * <p><i>This is the default value</i></p>
		 */
		NONE(AUTORESIZING_NONE),

		/**
		 * Resizes the view by adjusting the height
		 */
		FLEXIBLE_HEIGHT(AUTORESIZING_FLEXIBLE_HEIGHT),

		/**
		 * Resizes the view by adjusting the width
		 */
		FLEXIBLE_WIDTH(AUTORESIZING_FLEXIBLE_WIDTH),

		/**
		 * Resizes the view by adjusting the left margin
		 */
		FLEXIBLE_LEFT_MARGIN(AUTORESIZING_FLEXIBLE_LEFT_MARGIN),

		/**
		 * Resizes the view by adjusting the right margin
		 */
		FLEXIBLE_RIGHT_MARGIN(AUTORESIZING_FLEXIBLE_RIGHT_MARGIN),

		/**
		 * Resizes the view by adjusting the top margin
		 */
		FLEXIBLE_TOP_MARGIN(AUTORESIZING_FLEXIBLE_TOP_MARGIN),

		/**
		 * Resizes the view by adjusting the bottom margin
		 */
		FLEXIBLE_BOTTOM_MARGIN(AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN);

		private int value;
		private Autoresizing(int value) {
			this.value = value;
		}

		int getValue() {
			return this.value;
		}

		/**
		 * Convenience Set to pass to {@link View#setAutoresizing(java.util.Set)} to resize both width and height
		 */
		public static final Set<Autoresizing> FLEXIBLE_SIZE = Collections.unmodifiableSet(EnumSet.of(Autoresizing.FLEXIBLE_WIDTH, Autoresizing.FLEXIBLE_HEIGHT));

		/**
		 * Convenience Set to pass to {@link View#setAutoresizing(java.util.Set)} to resize all margins
		 */
		public static final Set<Autoresizing> FLEXIBLE_MARGINS = Collections.unmodifiableSet(EnumSet.of(Autoresizing.FLEXIBLE_TOP_MARGIN, Autoresizing.FLEXIBLE_LEFT_MARGIN, Autoresizing.FLEXIBLE_BOTTOM_MARGIN, Autoresizing.FLEXIBLE_RIGHT_MARGIN));
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
		BOTTOM_RIGHT
	}

	public enum AnimationCurve {
		/**
		 * Animation starts and ends slow, and accelerates during the middle
		 * <p><i>This is the default value</i></p>
		 */
		EASE_IN_OUT,

		/**
		 * Animation starts slow and accelerates until it's complete
		 */
		EASE_IN,

		/**
		 * Animation starts quickly and slows down until it's complete
		 */
		EASE_OUT,

		/**
		 * Animation progresses evenly throughout it's duration
		 */
		LINEAR
	}

	public enum TintAdjustmentMode {
		/**
		 * Automatically determines the correct tint adjustment mode to use
		 */
		AUTOMATIC,

		/**
		 * Normal tinting
		 */
		NORMAL,

		/**
		 * Dimmed tinting, tintColor will become desaturated
		 */
		DIMMED
	}

	/**
	 * Callback for when animations start
	 * @see View#setAnimationWillStartCallback(mocha.ui.View.AnimationWillStart)
	 */
	public interface AnimationWillStart {
		/**
		 * Called right before the first frame of the animation is processed
		 *
		 * @param animationID Animation ID set via {@link View#beginAnimations(String, Object)}
		 * @param context Context set via {@link View#beginAnimations(String, Object)}
		 */
		public void animationWillStart(String animationID, Object context);
	}

	/**
	 * Call for when animations stop
	 * @see View#setAnimationDidStopCallback(mocha.ui.View.AnimationDidStop)
	 */
	public interface AnimationDidStop {
		/**
		 * Called after the last frame of the animation is processed or if it was cancelled
		 *
		 * @param animationID Animation ID set via {@link View#beginAnimations(String, Object)}
		 * @param finished true if the animation finished, false if it was cancelled
		 * @param context Context set via {@link View#beginAnimations(String, Object)}
		 */
		public void animationDidStop(String animationID, boolean finished, Object context);
	}

	/**
	 * Callback for when an animation completes
	 * @see View#animateWithDuration(long, mocha.ui.View.Animations, mocha.ui.View.AnimationCompletion)
	 * @see View#animateWithDuration(long, long, mocha.ui.View.Animations, mocha.ui.View.AnimationCompletion)
	 */
	public interface AnimationCompletion {
		/**
		 * Called after the last frame of the animation is processed or if it was cancelled
		 *
		 * @param finished true if the animation finished, false if it was cancelled
		 */
		public void animationCompletion(boolean finished);
	}

	/**
	 * Callback for when animated changes should be performed
	 * @see View#animateWithDuration(long, mocha.ui.View.Animations)
	 * @see View#animateWithDuration(long, mocha.ui.View.Animations, mocha.ui.View.AnimationCompletion)
	 * @see View#animateWithDuration(long, long, mocha.ui.View.Animations, mocha.ui.View.AnimationCompletion)
	 */
	public interface Animations {
		/**
		 * Called by {@link View#animateWithDuration(long, mocha.ui.View.Animations)} in an animation context
		 * Any changes to animatable properties will be animated if changed within this method.
		 */
		public void performAnimatedChanges();
	}

	private ViewLayer layer;
	private View superview;
	private int backgroundColor;
	private int autoresizingMask;
	private List<View> subviews;
	private boolean autoresizesSubviews;
	private int tag;
	private boolean needsLayout;
	private boolean userInteractionEnabled;
	final Rect frame;
	private final Rect bounds;
	private final AffineTransform transform;
	private final EdgeInsets layoutMargins;
	private boolean preservesSuperviewLayoutMargins;
	private List<GestureRecognizer> gestureRecognizers;
	private boolean clipsToBounds;
	public final float scale;
	private boolean onCreatedCalled;
	private ContentMode contentMode;
	private boolean multipleTouchEnabled;

	Touch trackingSingleTouch;

	private int tintColor;
	private int dimmedTintColor;
	private TintAdjustmentMode tintAdjustmentMode;

	private int inheritedTintColor;
	private int inheritedDimmedTintColor;
	private TintAdjustmentMode inherritedTintAdjustmentMode;
	private boolean tintInheritenceValid;

	/**
	 * Mapped to {@link ViewAnimation.Type#value}. Each property type can
	 * only have one animation, so this is 1 to 1
	 */
	ViewAnimation[] animations;

	/**
	 * Animatable properties should check this before calling {@link View#changeEndValueForAnimationType(mocha.ui.ViewAnimation.Type, Object)}
	 */
	boolean animationIsSetting;

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
			this.layer = this.createLayer(context);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.frame = frame.copy();
		this.bounds = new Rect(0.0f, 0.0f, frame.size.width, frame.size.height);
		this.transform = AffineTransform.identity();
		this.animations = new ViewAnimation[ViewAnimation.Type.values().length];
		this.layoutMargins = new EdgeInsets();

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
		this.needsLayout = true;
		this.tintColor = 0;
		this.tintAdjustmentMode = TintAdjustmentMode.AUTOMATIC;

		this.layer.setSupportsDrawing(this.getOverridesDraw());

		this.onCreate(frame.copy());

		if(!this.onCreatedCalled) {
			throw new RuntimeException(this.getClass().getCanonicalName() + " overrides onCreate but does not call super.");
		}
	}

	boolean getOverridesDraw() {
		try {
			return this.getClass().getMethod("draw", mocha.graphics.Context.class, Rect.class).getDeclaringClass() != View.class;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	/**
	 * Called at the end of the constructor.  It's recommended for view's to do any initialization work
	 * in this method and not the constructor.
	 *
	 * {@important All subclasses must call super, if they do not, a runtime exception is thrown.}
	 *
	 * @param frame Frame the constructor was called with, or {@link Rect#zero()} if there wasn't one provided
	 */
	protected void onCreate(Rect frame) {
		this.onCreatedCalled = true;
	}

	public boolean getClipsToBounds() {
		return clipsToBounds;
	}

	/**
	 * Set whether or not this view clips it's subviews to it's bounds
	 *
	 * <p>Clipping subviews to a views bounds will come with a performance hit, so it's
	 * recommend this is only used when absolutely necessary.</p>
	 *
	 * @param clipsToBounds if true, subviews (recursively) will be clipped to the bounds of this view
	 */
	public void setClipsToBounds(boolean clipsToBounds) {
		if(this.clipsToBounds != clipsToBounds) {
			this.clipsToBounds = clipsToBounds;

			this.layer.setClipsToBounds(clipsToBounds);
			this.setNeedsDisplay();
		}
	}

	public Rect getFrame() {
		return this.frame.copy();
	}

	public float getFrameX() {
		return this.frame.origin.x;
	}

	public float getFrameY() {
		return this.frame.origin.y;
	}

	public float getFrameWidth() {
		return this.frame.size.width;
	}

	public float getFrameHeight() {
		return this.frame.size.height;
	}

	/**
	 * Set the frame of the view
	 *
	 * @animatable
	 * @param frame New frame
	 */
	public void setFrame(Rect frame) {
		if(frame == null) {
			frame = Rect.zero();
		}

		if(frame.equals(this.frame)) {
			return;
		}

		ViewAnimation.Type animationType = ViewAnimation.Type.FRAME;

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, animationType, frame.copy());
			this.frame.set(frame);
			return;
		}

		if(!this.animationIsSetting && this.animations[animationType.value] != null) {
			if(this.changeEndValueForAnimationType(animationType, frame.copy())) {
				this.frame.set(frame);
				return;
			}
		}

		if(this.superview != null && !this.superview.bounds.contains(this.frame)) {
			// Fixes a weird bug in Android that leaves artifacts on the screen
			// if the old frame wasn't fully contained within the bounds.
			this.superview.setNeedsDisplay(this.frame);
		}

		this.frame.set(frame);
		boolean boundsChanged;

		float oldWidth = this.bounds.size.width;
		float oldHeight = this.bounds.size.height;
		float x = this.bounds.origin.x;
		float y = this.bounds.origin.y;

		if(oldWidth != this.frame.size.width || oldHeight != this.frame.size.height) {
			this.bounds.size.width = this.frame.size.width;
			this.bounds.size.height = this.frame.size.height;
			boundsChanged = true;
		} else {
			boundsChanged = false;
		}

		this.layer.setFrame(this.frame, this.bounds);

		if(boundsChanged) {
			this.boundsDidChange(x, y, oldWidth, oldHeight, x, y, this.bounds.size.width, this.bounds.size.height);
		}
	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public float getBoundsX() {
		return this.bounds.origin.x;
	}

	public float getBoundsY() {
		return this.bounds.origin.y;
	}

	public float getBoundsWidth() {
		return this.bounds.size.width;
	}

	public float getBoundsHeight() {
		return this.bounds.size.height;
	}

	/**
	 * Set the bounds of the view
	 *
	 * <p>All subviews are laid out relative to the origin of their superviews bounds.  By default
	 * the origin is 0,0, however if it's set to a value of 25,25, all subviews will be laid out
	 * starting 25pt from the left, and 25pt from the top.</p>
	 *
	 * @animatable
	 * @param bounds New bounds
	 */
	public void setBounds(Rect bounds) {
		if(bounds == null) {
			bounds = Rect.zero();
		}

		ViewAnimation.Type type = ViewAnimation.Type.BOUNDS;

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, type, bounds);
			this.bounds.set(bounds);
			return;
		}

		if(!this.animationIsSetting && this.animations[type.value] != null) {
			if(this.changeEndValueForAnimationType(type, bounds.copy())) {
				this.bounds.set(bounds);
				return;
			}
		}

		if(!bounds.equals(this.bounds)) {
			// TODO: Combine these to be smart and only invalidate if the subview isn't fully contained
			for(View subview : this.subviews) {
				this.setNeedsDisplay(subview.frame);
			}

			if(this.superview != null) {
				this.superview.setNeedsDisplay();
			}

			float oldX = this.bounds.origin.x;
			float oldY = this.bounds.origin.y;
			float oldWidth = this.bounds.size.width;
			float oldHeight = this.bounds.size.height;

			this.bounds.set(bounds);
			this.layer.setBounds(this.bounds);
			this.boundsDidChange(oldX, oldY, oldWidth, oldHeight, bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height);
		}
	}


	public Point getCenter() {
		return new Point(this.frame.midX(), this.frame.midY());
	}

	public float getCenterX() {
		return this.frame.midX();
	}

	public float getCenterY() {
		return this.frame.midY();
	}

	public void setCenter(Point center) {
		Rect frame = this.bounds.copy();
		frame.origin.x = center.x - (frame.size.width / 2.0f);
		frame.origin.y = center.y - (frame.size.height / 2.0f);
		this.setFrame(frame);
	}

	public void setLayoutMargins(EdgeInsets layoutMargins) {
		this.layoutMargins.set(layoutMargins);
		this.notifyLayoutMarginsChanged();
	}

	public EdgeInsets getLayoutMargins() {
		return this.layoutMargins.copy();
	}

	public boolean getPreservesSuperviewLayoutMargins() {
		return this.preservesSuperviewLayoutMargins;
	}

	public void setPreservesSuperviewLayoutMargins(boolean preservesSuperviewLayoutMargins) {
		this.preservesSuperviewLayoutMargins = preservesSuperviewLayoutMargins;
		this.notifyLayoutMarginsChanged();
	}

	public void layoutMarginsDidChange() {

	}

	private void notifyLayoutMarginsChanged() {
		this.layoutMarginsDidChange();

		for(View subview : this.getSubviews()) {
			if(subview.getPreservesSuperviewLayoutMargins()) {
				subview.notifyLayoutMarginsChanged();
			}
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

	/**
	 * Set the autoresizing rules for this view
	 * @param autoresizing Set of rules this view should autoresize by
	 */
	public void setAutoresizing(Set<Autoresizing> autoresizing) {
		this.autoresizingMask = 0;

		for(Autoresizing option : autoresizing) {
			this.autoresizingMask |= option.getValue();
		}
	}

	/**
	 * Set a single autoresizing rule for this view
	 * @param autoresizing Rule this view should autoresize by
	 */
	public void setAutoresizing(Autoresizing autoresizing) {
		this.setAutoresizing(EnumSet.of(autoresizing));
	}

	/**
	 * Set the autoresizing rules for this view
	 * @param first First autoresizing rule
	 * @param others The rest of the autoresizing rules
	 */
	public void setAutoresizing(Autoresizing first, Autoresizing... others) {
		this.setAutoresizing(EnumSet.of(first, others));
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * Set the background color of this view
	 *
	 * @animatable
	 * @param backgroundColor New background color
	 */
	public void setBackgroundColor(int backgroundColor) {
		ViewAnimation.Type type = ViewAnimation.Type.BACKGROUND_COLOR;

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, type, backgroundColor);
			this.backgroundColor = backgroundColor;
			return;
		}

		if(!this.animationIsSetting && this.animations[type.value] != null) {
			if(this.changeEndValueForAnimationType(type, backgroundColor)) {
				return;
			}
		}

		this.backgroundColor = backgroundColor;
		this.layer.setBackgroundColor(backgroundColor);
	}

	/**
	 * Set the transform of this view
	 *
	 * @animatable
	 * @param transform Transform value
	 */
	public void setTransform(AffineTransform transform) {
		if(this.transform.equals(transform)) {
			return;
		}

		if(transform == null) {
			transform = AffineTransform.identity();
		}

		ViewAnimation.Type type = ViewAnimation.Type.TRANSFORM;

		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, type, transform.copy());
		} else {
			if(!this.animationIsSetting && this.animations[type.value] != null) {
				if(this.changeEndValueForAnimationType(type, transform.copy())) {
					this.transform.set(transform);
					return;
				}
			}

			this.layer.setTransform(transform);
			this.transform.set(transform);
		}
	}

	public AffineTransform getTransform() {
		return this.transform.copy();
	}

	public boolean doesAutoresizeSubviews() {
		return autoresizesSubviews;
	}

	/**
	 * Set whether or not this view autoresizes it's subviews
	 *
	 * Defaul value is true.
	 *
	 * @param autoresizesSubviews If true, subviews will be autoresized according to their rules
	 *                               if false, no changes will occurr to subviews
	 */
	public void setAutoresizesSubviews(boolean autoresizesSubviews) {
		this.autoresizesSubviews = autoresizesSubviews;
	}

	public ContentMode getContentMode() {
		return contentMode;
	}

	/**
	 * Setting a content mode tells the view how to lay out it's
	 * content relative to the bounds of this view.
	 *
	 * This does not affect all views, only certain views that contain content
	 * such as {@link ImageView} handle this value.
	 *
	 * @param contentMode Content mode
	 */
	public void setContentMode(ContentMode contentMode) {
		if(contentMode == null) {
			contentMode = ContentMode.SCALE_TO_FILL;
		}

		this.contentMode = contentMode;
	}

	public int getTag() {
		return tag;
	}

	/**
	 * Set a tag to easily identify and look up views
	 *
	 * @param tag
	 */
	public void setTag(int tag) {
		this.tag = tag;
	}

	/**
	 * Get the layer backing this view
	 * @return Backing layer
	 * @hide
	 */
	public ViewLayer getLayer() {
		return this.layer;
	}

	public boolean isHidden() {
		return this.getLayer().isHidden();
	}

	/**
	 * Set whether or not a view is hidden.
	 *
	 * A hidden view will not receive any touches and will not be draw.
	 *
	 * @param hidden Whether or not the view is hidden
	 */
	public void setHidden(boolean hidden) {
		this.getLayer().setHidden(hidden);
	}

	/**
	 * Set the alpha value for this view
	 *
	 * <p>If the view has an alpha value less than 0.01, no touches will be delivered.</p>
	 * {@note Alpha blending is expensive, use sparingly!}
	 *
	 * @animatable
	 * @param alpha A number between 0.0 (completely transparent) and 1.0 (completely opaque)
	 */
	public void setAlpha(float alpha) {
		ViewAnimation.Type type = ViewAnimation.Type.ALPHA;
		if(areAnimationsEnabled && currentViewAnimation != null && this.superview != null) {
			currentViewAnimation.addAnimation(this, type, alpha);
		} else {
			if(!this.animationIsSetting && this.animations[type.value] != null) {
				if(this.changeEndValueForAnimationType(type, alpha)) {
					return;
				}
			}

			this.getLayer().setAlpha(alpha);
		}
	}

	public float getAlpha() {
		return this.getLayer().getAlpha();
	}

	public TintAdjustmentMode getTintAdjustmentMode() {
		if(this.tintAdjustmentMode == null) {
			if(this.getSuperview() != null) {
				return this.getSuperview().getTintAdjustmentMode();
			} else {
				return TintAdjustmentMode.AUTOMATIC;
			}
		} else {
			return this.tintAdjustmentMode;
		}
	}

	public void setTintAdjustmentMode(TintAdjustmentMode tintAdjustmentMode) {
		this.tintAdjustmentMode = tintAdjustmentMode;

		if(tintAdjustmentMode == TintAdjustmentMode.AUTOMATIC) {
			this.updateTintInheritence();
		}

		this.notifyTintChanged();
	}

	public int getTintColor() {
		if(this.getTintAdjustmentMode() == TintAdjustmentMode.DIMMED) {
			if(this.tintColor == 0) {
				return this.inheritedDimmedTintColor;
			} else {
				return this.dimmedTintColor;
			}
		} else {
			if(this.tintColor == 0) {
				return this.inheritedTintColor;
			} else {
				return this.tintColor;
			}
		}
	}

	public void setTintColor(int tintColor) {
		this.tintColor = tintColor;

		if(tintColor != 0) {
			// TODO: Desaturate instead of adjusting alpha
			this.dimmedTintColor = Color.colorWithAlpha(tintColor, 0.5f);
		} else {
			this.dimmedTintColor = 0;
			this.updateTintInheritence();
		}

		this.notifyTintChanged();
	}

	/**
	 * Updates inherited tint properties
	 */
	private void updateTintInheritence() {
		if(this.superview == null) {
			this.inheritedTintColor = Color.BLUE;
			this.inheritedDimmedTintColor = Color.GRAY;
			this.inherritedTintAdjustmentMode = TintAdjustmentMode.NORMAL;
			this.tintInheritenceValid = false;
		} else {
			if(this.superview.tintColor == 0) {
				this.inheritedTintColor = this.superview.inheritedTintColor;
				this.inheritedDimmedTintColor = this.superview.inheritedDimmedTintColor;
			} else {
				this.inheritedTintColor = this.superview.tintColor;
				this.inheritedDimmedTintColor = this.superview.dimmedTintColor;
			}

			if(this.superview.tintAdjustmentMode == TintAdjustmentMode.AUTOMATIC) {
				this.inherritedTintAdjustmentMode = this.superview.inherritedTintAdjustmentMode;
			} else {
				this.inherritedTintAdjustmentMode = this.superview.tintAdjustmentMode;
			}

			this.tintInheritenceValid = this.inheritedTintColor != 0 && this.inherritedTintAdjustmentMode != TintAdjustmentMode.AUTOMATIC;
		}
	}

	/**
	 * Notifies subviews who inherit tint that their inherited values
	 * need to be updated.
	 */
	private void notifyTintChanged() {
		this.tintColorDidChange();

		for(View subview : this.getSubviews()) {
			if(subview.doesInheritTint()) {
				subview.updateTintInheritence();
				subview.notifyTintChanged();
			}
		}
	}

	private boolean doesInheritTint() {
		return this.tintColor == 0 || this.tintAdjustmentMode == TintAdjustmentMode.AUTOMATIC;
	}

	public void tintColorDidChange() {

	}

	/**
	 * @return True if this view can receive touches, false otherwise
	 */
	public boolean isUserInteractionEnabled() {
		return this.userInteractionEnabled;
	}

	/**
	 * Set whether or not this view (and any of it's subviews) can receive touches
	 *
	 * <p><i>Default value is true.</i></p>
	 *
	 * @param userInteractionEnabled True if this view can receive touches, false otherwise.
	 */
	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		this.userInteractionEnabled = userInteractionEnabled;
	}

	public boolean isMultipleTouchEnabled() {
		return multipleTouchEnabled;
	}

	/**
	 * Set whether or not this view can recieves multiple touches
	 *
	 * <p><i>Default value is false.</i></p>
	 *
	 * @param multipleTouchEnabled True if this view supports multi touch, false otherwise.
	 */
	public void setMultipleTouchEnabled(boolean multipleTouchEnabled) {
		this.multipleTouchEnabled = multipleTouchEnabled;
	}

	/**
	 * Change the backing layer type, if we're using a view group backed layer type.
	 * Changing this shouldn't be necessary unless you're using something like
	 * blending modes in your view's draw() method that isn't supported by
	 * hardware layers yet.
	 *
	 * @param hardwareAccelerationEnabled Whether or not hardware acceleration is enabled
	 */
	public void setHardwareAccelerationEnabled(boolean hardwareAccelerationEnabled) {
		ViewGroup viewGroup = this.getLayer().getViewGroup();

		if(viewGroup != null) {
			if(hardwareAccelerationEnabled) {
				viewGroup.setLayerType(android.view.View.LAYER_TYPE_NONE, null);
			} else {
				viewGroup.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
			}
		}
	}

	/**
	 * @return Whether or not the view is hardware accelerated
	 */
	public boolean isHardwareAccelerationEnabled() {
		ViewGroup viewGroup = this.getLayer().getViewGroup();
		return viewGroup != null && viewGroup.isHardwareAccelerated();
	}

	// Layout

	private boolean hasAutoresizingFor(int mask) {
		return (this.autoresizingMask & mask) != 0;
	}

	private void superviewSizeDidChange_(float oldWidth, float oldHeight, float newWidth, float newHeight) {
		// NOTE: This is a port from Chameleon and is slightly faster than the version below however the
		// logic is buggy and doesn't always scale correctly (specifically width+height only scaling, scales margins anyway)

		if (this.autoresizingMask != Autoresizing.NONE.value) {
			Rect frame = this.getFrame();

			Size delta = new Size(newWidth-oldWidth, newHeight-oldHeight);

			if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value | Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value)) {
				frame.origin.y = roundf(frame.origin.y + (frame.origin.y / oldHeight * delta.height));
				frame.size.height = roundf(frame.size.height + (frame.size.height / oldHeight * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_TOP_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				float t = frame.origin.y + frame.size.height;
				frame.origin.y = roundf(frame.origin.y + (frame.origin.y / t * delta.height));
				frame.size.height = roundf(frame.size.height + (frame.size.height / t * delta.height));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_BOTTOM_MARGIN.value | Autoresizing.FLEXIBLE_HEIGHT.value)) {
				frame.size.height = roundf(frame.size.height + (frame.size.height / (oldHeight - frame.origin.y) * delta.height));
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
				frame.origin.x = roundf(frame.origin.x + (frame.origin.x / oldWidth * delta.width));
				frame.size.width = roundf(frame.size.width + (frame.size.width / oldWidth * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_LEFT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				float t = frame.origin.x + frame.size.width;
				frame.origin.x = roundf(frame.origin.x + (frame.origin.x / t * delta.width));
				frame.size.width = roundf(frame.size.width + (frame.size.width / t * delta.width));
			} else if (hasAutoresizingFor(Autoresizing.FLEXIBLE_RIGHT_MARGIN.value | Autoresizing.FLEXIBLE_WIDTH.value)) {
				frame.size.width = roundf(frame.size.width + (frame.size.width / (oldWidth - frame.origin.x) * delta.width));
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

	private void superviewSizeDidChange(float oldWidth, float oldHeight, float newWidth, float newHeight) {
		int mask = this.autoresizingMask;
		if(mask == AUTORESIZING_NONE) return;

		Point origin = this.frame.origin.copy();
		Size size = this.frame.size.copy();
		float delta;

		if (oldWidth != 0.0f || size.width == 0.0f) {
			int horizontalMask = (mask & AUTORESIZING_FLEXIBLE_LEFT_MARGIN) + (mask & AUTORESIZING_FLEXIBLE_WIDTH) + (mask & AUTORESIZING_FLEXIBLE_RIGHT_MARGIN);

			if(horizontalMask != AUTORESIZING_NONE) {
				if(horizontalMask == AUTORESIZING_FLEXIBLE_LEFT_MARGIN) {
					origin.x += newWidth - oldWidth;
				} else if(horizontalMask == AUTORESIZING_FLEXIBLE_WIDTH) {
					size.width = newWidth - (oldWidth - this.frame.size.width);
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH)) {
					delta = (oldWidth - this.frame.size.width - this.frame.origin.x);
					origin.x = (this.frame.origin.x / (oldWidth - delta)) * (newWidth - delta);
					size.width = newWidth - origin.x - delta;
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_RIGHT_MARGIN)) {
					delta = (oldWidth - this.frame.size.width - this.frame.origin.x);
					origin.x += (newWidth - oldWidth) * (this.frame.origin.x / (this.frame.origin.x + delta));
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_RIGHT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH)) {
					delta = (oldWidth - this.frame.size.width - this.frame.origin.x);
					float scaledRightMargin = (delta / (oldWidth - this.frame.origin.x)) * (newWidth - this.frame.origin.x);
					size.width = newWidth - origin.x - scaledRightMargin;
				} else if(horizontalMask == (AUTORESIZING_FLEXIBLE_LEFT_MARGIN | AUTORESIZING_FLEXIBLE_WIDTH | AUTORESIZING_FLEXIBLE_RIGHT_MARGIN)) {
					origin.x = (this.frame.origin.x / oldWidth) * newWidth;
					size.width = (this.frame.size.width / oldWidth) * newWidth;
				}
			}
		}

		if (oldHeight != 0 || size.height == 0) {
			int verticalMask = (mask & AUTORESIZING_FLEXIBLE_TOP_MARGIN) + (mask & AUTORESIZING_FLEXIBLE_HEIGHT) + (mask & AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN);

			if(verticalMask != AUTORESIZING_NONE) {
				if(verticalMask == AUTORESIZING_FLEXIBLE_TOP_MARGIN) {
					origin.y += newHeight - oldHeight;
				} else if(verticalMask == AUTORESIZING_FLEXIBLE_HEIGHT) {
					size.height = newHeight - (oldHeight - this.frame.size.height);
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT)) {
					delta = (oldHeight - this.frame.size.height - this.frame.origin.y);
					origin.y = (this.frame.origin.y / (oldHeight - delta)) * (newHeight - delta);
					size.height = newHeight - origin.y - delta;
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN)) {
					delta = (oldHeight - this.frame.size.height - this.frame.origin.y);
					origin.y += (newHeight - oldHeight) * (this.frame.origin.y / (this.frame.origin.y + delta));
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT)) {
					delta = (oldHeight - this.frame.size.height - this.frame.origin.y);
					float scaledBottomMargin = (delta / (oldHeight - this.frame.origin.y)) * (newHeight - this.frame.origin.y);
					size.height = newHeight - origin.y - scaledBottomMargin;
				} else if(verticalMask == (AUTORESIZING_FLEXIBLE_TOP_MARGIN | AUTORESIZING_FLEXIBLE_HEIGHT | AUTORESIZING_FLEXIBLE_BOTTOM_MARGIN)) {
					origin.y = (this.frame.origin.y / oldHeight) * newHeight;
					size.height = (this.frame.size.height / oldHeight) * newHeight;
				}
			}
		}

		if(!this.frame.origin.equals(origin) || !this.frame.size.equals(size)) {
			this.setFrame(new Rect(origin, size));
		}
	}

	void boundsDidChange(float oldX, float oldY, float oldWidth, float oldHeight, float newX, float newY, float newWidth, float newHeight) {
		boolean originChanged = oldX != newX || oldY != newY;
		boolean sizeChanged = oldWidth != newWidth || oldHeight != newHeight;

		if(originChanged || sizeChanged) {
			this.setNeedsLayout();

			if (sizeChanged) {
				if (this.autoresizesSubviews) {
					for (View subview : this.subviews) {
						subview.superviewSizeDidChange(oldWidth, oldHeight, newWidth, newHeight);
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
		Point convertedPoint = point == null ? new Point() : point.copy();

		while(view != null) {
			convertedPoint.x += view.frame.origin.x - view.bounds.origin.x;
			convertedPoint.y += view.frame.origin.y - view.bounds.origin.y;
			view = view.superview;
		}

		return convertedPoint;
	}

	/**
	 * Converts a point in this views bounds to be relative to the bounds
	 * of the provided view.
	 *
	 * @param point Point to convert
	 * @param view Target view to convert the point to
	 * @return Point relative to the target views bounds
	 */
	public Point convertPointToView(Point point, View view) {
		if(view == null) {
			view = this.getWindow();
		}

		Point fromPoint = this.convertPointToWindow(null);
		Point toPoint = view.convertPointToWindow(null);

		fromPoint.x = point.x + (fromPoint.x - toPoint.x);
		fromPoint.y = point.y + (fromPoint.y - toPoint.y);

		return fromPoint;
	}

	/**
	 * Convert point from the provided view to be relative to the bounds of this view.
	 *
	 * @param point Point to convert
	 * @param view View to convert point from
	 * @return Point relative to this views bounds
	 */
	public Point convertPointFromView(Point point, View view) {
		if(view == null) {
			view = this.getWindow();
		}

		if(view == null) {
			return point;
		}

		return view.convertPointToView(point, this);
	}

	/**
	 * Converts a rect in this views bounds to be relative to the bounds
	 * of the provided view.
	 *
	 * {@note Only the origin of the rect is affected by this, size will remain unchanged.}
	 *
	 * @param rect Rect to convert
	 * @param view Target view to convert the rect origin to
	 * @return Rect relative to the target views bounds
	 */
	public Rect convertRectToView(Rect rect, View view) {
		return new Rect(this.convertPointToView(rect.origin, view), rect.size);
	}

	/**
	 * Converts a rect in the provided views to be relative to the bounds
	 * of this view.
	 *
	 * {@note Only the origin of the rect is affected by this, size will remain unchanged.}
	 *
	 * @param rect Rect to convert
	 * @param view View to convert rect origin from
	 * @return Rect relative to this views bounds
	 */
	public Rect convertRectFromView(Rect rect, View view) {
		if(view == null) {
			view = this.getWindow();
		}

		if(view == null) {
			return rect;
		}

		return view.convertRectToView(rect, this);
	}

	/**
	 * Adjusts the frame of this view to fit.
	 *
	 * <p>The default behavior is to set this current frames size
	 * to the value of {@link View#sizeThatFits(mocha.graphics.Size)} using the current
	 * frame.size value.</p>
	 */
	public void sizeToFit() {
		Rect frame = this.getFrame();
		frame.size = this.sizeThatFits(frame.size);
		this.setFrame(frame);
	}

	/**
	 * Get a size that fits within the bounds of the provided size.
	 *
	 * <p>The default behavior is to take the small width and smallest height between
	 * the views frame size and the provided size.</p>
	 *
	 * @param size Max size
	 * @return Size that fits the provided size
	 */
	public Size sizeThatFits(Size size) {
		return new Size(Math.min(this.frame.size.width, size.width), Math.min(this.frame.size.height, size.height));
	}

	/**
	 * Recursively find the deepest view that contains this point, isn't hidden, has user interaction enabled
	 * as has an alpha value > 0.01.
	 *
	 * {@note The view returned by this method is not guaranteed to be within this views hierarchy.
	 * Subclasses can override this method to redirect touches to an entirely different view, so do not make
	 * any assumptions based on the returned view and the view hiearchy.}
	 *
	 * @param point Point relative to the bounds of this view
	 * @param event If triggered by a touch event, that event will be provided, otherwise this will be null.
	 * @return Deepest view containing the point or null if nothing matches.
	 */
	public View hitTest(Point point, Event event) {
		if(this.isHidden() || !this.isUserInteractionEnabled() || this.getAlpha() < 0.01f || !this.pointInside(point, event)) {
			return null;
		} else {
			int size = this.subviews.size();
			for(int i = size - 1; i >= 0; i--) {
				View subview = this.subviews.get(i);
				View hitView = subview.hitTest(subview.convertPointFromView(point, this), event);

				if(hitView != null) {
					return hitView;
				}
			}

			return this;
		}
	}

	/**
	 * Check whether or not a point is contained by this view.
	 *
	 * @param point Point to check
	 * @param event If triggered by a touch event, that event will be provided, otherwise this will be null.
	 * @return Whether or not this view contains the point
	 */
	public boolean pointInside(Point point, Event event) {
		if(this.transform == null || this.transform.isIdentity()) {
			return this.bounds.contains(point);
		} else {
			return this.transform.apply(this.bounds).contains(point);
		}
	}

	/**
	 * Plays the standard Android click sound.
	 *
	 * {@important This view must be attached to a window for the sound to play.}
	 */
	public void playClickSound() {
		if(this.getLayer().getViewGroup() != null) {
			this.getLayer().getViewGroup().playSoundEffect(SoundEffectConstants.CLICK);
		}
	}

	/**
	 * Performs the standard Android haptic feedback. (BZZZT!)
	 *
	 * {@important This view must be attached to a window for haptic feedback to be performed.}
	 */
	public void performHapticFeedback() {
		if(this.getLayer().getViewGroup() != null) {
			this.getLayer().getViewGroup().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		}
	}

	// Hierarchy

	ViewController _getViewController() {
		return _viewController;
	}

	void _setViewController(ViewController viewController) {
		this._viewController = viewController;
	}

	@Override
	public Responder nextResponder() {
		return this._viewController != null ? this._viewController : this.superview;
	}

	/**
	 * Get the superview the view is attached to
	 *
	 * @return Superview or null
	 */
	public View getSuperview() {
		return superview;
	}

	/**
	 * Get the window the view is attached to
	 *
	 * @return Window or null
	 */
	public Window getWindow() {
		return superview != null ? superview.getWindow() : null;
	}

	/**
	 * Get a list of the views subviews
	 *
	 * @return Subviews
	 */
	public List<View> getSubviews() {
		if(subviews != null) {
			return Collections.unmodifiableList(subviews);
		} else {
			return new ArrayList<View>();
		}
	}

	/**
	 * Removes the view from it's superview.
	 *
	 * If this view doesn't have a superview, this method does nothing.
	 */
	public void removeFromSuperview() {
		if(this.superview != null) {
			Window oldWindow = this.getWindow();

			this.superview.willRemoveSubview(this);
			this.willMoveWindows(oldWindow, null);
			this.willMoveToSuperview(null);
			this.layer.removeFromSuperlayer();
			this.superview.subviews.remove(this);
			this.superview = null;
			this.tintInheritenceValid = false;
			this.didMoveWindows(oldWindow, null);
			this.didMoveToSuperview();
			this.layer.didMoveToSuperlayer();
		}
	}

	protected void debugTint() {
		View view = this;
		String indent = "-";

		MWarn("DEBUG_TINT, TINT TRACE ON WINDOW: %s", this.getWindow());

		while(view != null) {
			MWarn("DEBUG_TINT, %s valid inheritence: %s, color: %s (%s), mode: %s, view: %s", indent, view.tintInheritenceValid, Color.toString(view.tintColor),Color.toString(view.inheritedTintColor), view.tintAdjustmentMode, view);
			view = view.superview;
			indent += "-";
		}
	}

	/**
	 * Inserts a subview at the specified index
	 *
	 * If the view already has a superview and it's not this view, the view is first
	 * removed from it's superview.
	 *
	 * @param view View to insert
	 * @param index Index to insert the view at. Must be >= 0 and <= the number of subviews
	 */
	public void insertSubview(View view, int index) {
		if(view.superview != null && view.superview != this) {
			view.superview.willRemoveSubview(view);
			view.layer.removeFromSuperlayer();
			view.superview.subviews.remove(view);
		}

		if(view.superview == this) {
			this.exchangeSubviews(this.subviews.indexOf(view), index);
		} else {
			Window oldWindow = view.getWindow();
			Window newWindow = this.getWindow();

			view.willMoveWindows(oldWindow, newWindow);
			view.willMoveToSuperview(this);
			this.subviews.add(index, view);
			this.layer.insertSublayerAtIndex(view.layer, index);
			view.superview = this;
			view.didMoveWindows(oldWindow, newWindow);
			view.didMoveToSuperview();
			this.didAddSubview(view);

			if(view.doesInheritTint() && (!this.doesInheritTint() || this.tintInheritenceValid)) {
				view.updateTintInheritence();
				view.notifyTintChanged();
			}

			if(view.getPreservesSuperviewLayoutMargins()) {
				view.setLayoutMargins(this.layoutMargins);
			}

			view._layoutSubviews();
		}
	}

	/**
	 * Exchange the order of two views
	 *
	 * @param index1 Index of view 1
	 * @param index2 Index of view 2
	 */
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

	/**
	 * Adds the subview to the front
	 *
	 * <p>If the views superview is this view, the view is brought to the front</p>
	 * <p>If the view has a superview that's not this view, it's first removed from it's superview</p>
	 *
	 * @param view View to add
	 */
	public void addSubview(View view) {
		if(view.superview == this) {
			this.bringSubviewToFront(view);
		} else {
			this.insertSubview(view, this.subviews.size());
		}
	}

	/**
	 * Insert a subview below a view already added to this view.
	 *
	 * If the view already has a superview and it's not this view, the view is first
	 * removed from it's superview.
	 *
	 * @param view View to insert
	 * @param belowSubview Subview to insert the view directly below
	 */
	public void insertSubviewBelowSubview(View view, View belowSubview) {
		int index = this.subviews.indexOf(belowSubview);
		this.insertSubview(view, index <= 0 ? 0 : index - 1);
	}

	/**
	 * Insert a subview above a view already added to this view.
	 *
	 * If the view already has a superview and it's not this view, the view is first
	 * removed from it's superview.
	 *
	 * @param view View to insert
	 * @param aboveSubview Subview to insert the view directly above
	 */
	public void insertSubviewAboveSubview(View view, View aboveSubview) {
		int index = this.subviews.indexOf(aboveSubview);
		this.insertSubview(view, index < 0 ? 0 : index + 1);
	}

	/**
	 * Brings the view to the front of the subview stack
	 *
	 * If the view's superview is not this view, this method does nothing.
	 *
	 * @param view View to bring to the front
	 */
	public void bringSubviewToFront(View view) {
		if(view.superview != this) return;

		view.layer.removeFromSuperlayer();
		this.layer.addSublayer(view.layer);
		this.subviews.remove(view);
		this.subviews.add(view);
	}

	/**
	 * Send the view to the back of the subview stack
	 *
	 * If the view's superview is not this view, this method does nothing.
	 *
	 * @param view View to send to the back
	 */
	public void sendSubviewToBack(View view) {
		if(view.superview != this) return;

		view.layer.removeFromSuperlayer();
		this.layer.insertSublayerAtIndex(view.layer, 0);
		this.subviews.remove(view);
		this.subviews.add(0, view);
		view.layer.didMoveToSuperlayer();
	}

	/**
	 * Called when a subview was added
	 *
	 * @param subview Subview that was added
	 */
	public void didAddSubview(View subview) {

	}

	/**
	 * Called right before a subview is removed
	 *
	 * @param subview Subview that will be removd
	 */
	public void willRemoveSubview(View subview) {

	}

	/**
	 * Called right before the view moves to it's new superview
	 *
	 * @param newSuperview New superview or null if being removed from a superview
	 */
	public void willMoveToSuperview(View newSuperview) {

	}

	/**
	 * Called right after a view changes superviews
	 */
	public void didMoveToSuperview() {

	}

	/**
	 * Called right before a view moves to a new window
	 *
	 * @param newWindow New window or null if being removed
	 */
	public void willMoveToWindow(Window newWindow) {

	}

	/**
	 * Called directly after a view changes windows or is removed from one
	 */
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

	/**
	 * Finds a view by a tag, including this view
	 *
	 * @param tag Tag to search for
	 * @return First view matching the tag or null if none is found
	 */
	public View getViewWithTag(int tag) {
		View foundView = null;

		if (this.tag == tag) {
			foundView = this;
		} else if(this.subviews.size() > 0) {
			for (View view : this.subviews) {
				foundView = view.getViewWithTag(tag);
				if (foundView != null) break;
			}
		}

		return foundView;
	}

	/**
	 * Determines whether or not the view descends from this view
	 *
	 * @param view Subview to check
	 * @return true if the subview is contained within this views subview hiearchy or if the view is this view, false otherwise
	 */
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

	/**
	 * Schedules this view to be laid out on the next loop
	 *
	 * Multiple calls to this method before the next layout takes place will have no affect.
	 */
	public void setNeedsLayout() {
		this.needsLayout = true;
		this.layer.setNeedsLayout();
	}

	/**
	 * Calls {@link mocha.ui.View#layoutSubviews()} if layout is needed (set by {@link mocha.ui.View#setNeedsLayout()}
	 */
	public void layoutIfNeeded() {
		if(this.needsLayout) {
			this._layoutSubviews();
		}
	}

	void _layoutSubviews() {
		if(this.superview == null) return;

		if(this.needsLayout) {
			if(this._viewController != null) {
				this._viewController.viewWillLayoutSubviews();
			}

			this.layoutSubviews();
			this.needsLayout = false; // Just in case a subclass doesn't call super

			if(this._viewController != null) {
				this._viewController.viewDidLayoutSubviews();
			}
		}
	}

	/**
	 * The default behavior of this method does nothing.  Subclasses should override this method
	 * if autoresizing rules do not provide the level of control necessary to lay out a views subviews.
	 */
	public void layoutSubviews() {
		this.needsLayout = false;
	}

	@Override
	protected String toStringExtra() {
		return String.format("frame = %s; bounds = %s; alpha = %.2f; hidden = %b%s%s", this.getFrame(), this.getBounds(), this.getAlpha(), this.isHidden(), (this.tag != 0 ? "; tag = "+this.tag : ""), (this._viewController != null ? "; viewController = " + this._viewController : ""));
	}

	// Rendering

	/**
	 * Sets the entire view as dirty and will be completely redrawn in the near future.
	 */
	public void setNeedsDisplay() {
		this.layer.setNeedsDisplay();
	}

	/**
	 * Sets a portion of this view as dirty that will be redrawn in the near future
	 * @param dirtyRect Rect of the view to set dirty
	 */
	public void setNeedsDisplay(Rect dirtyRect) {
		this.layer.setNeedsDisplay(dirtyRect);
	}

	/**
	 * Draw the views contents within the provided rect
	 *
	 * @param context Context to draw into
	 * @param rect The part of the view that is dirty or the views bounds if the entire view is dirty
	 */
	public void draw(mocha.graphics.Context context, Rect rect) {

	}

	// Gestures

	/**
	 * Add a gesture recognizer to this view
	 *
	 * Gesture recognizers and views are a 1 to 1 pairing, if the gesture recognizer is already
	 * attached to another view, it is first removed from that view before being added to this one.
	 *
	 * @param gestureRecognizer Gesture recognizer to add
	 */
	public void addGestureRecognizer(GestureRecognizer gestureRecognizer) {
		if(!this.gestureRecognizers.contains(gestureRecognizer)) {
			if(gestureRecognizer.getView() != null) {
				gestureRecognizer.getView().removeGestureRecognizer(gestureRecognizer);
			}

			this.gestureRecognizers.add(gestureRecognizer);
			gestureRecognizer.setView(this);
		}
	}

	/**
	 * Remove a gesture recognizer from this from
	 *
	 * This method will not do anything if the gesture recognizer isn't already
	 * attached to this view.
	 *
	 * @param gestureRecognizer Gesture recognizer to remove
	 */
	public void removeGestureRecognizer(GestureRecognizer gestureRecognizer) {
		if(this.gestureRecognizers.contains(gestureRecognizer)) {
			gestureRecognizer.setView(null);
			this.gestureRecognizers.remove(gestureRecognizer);
		}
	}

	/**
	 * Get all gesture recognizers attached to this view
	 * @return List of this views gesture recognizers
	 */
	public List<GestureRecognizer> getGestureRecognizers() {
		return Collections.unmodifiableList(this.gestureRecognizers);
	}

	// Helpers

	/**
	 * Convenince method to ceil a float and return an int
	 *
	 * @param f Float value to ceil
	 * @return ceiled int
	 */
	public static int ceil(float f) {
		return (int)FloatMath.ceil(f);
	}

	/**
	 * Convenince method to ceil a double and return an int
	 *
	 * @param d Double value to ceil
	 * @return ceiled int
	 */
	public static int ceil(double d) {
		return (int)Math.ceil(d);
	}

	/**
	 * Convenince method to floor a float and return an int

	 * @param f Float value to floor
	 * @return floored int
	 */
	public static int floor(float f) {
		return (int)FloatMath.floor(f);
	}

	/**
	 * Convenince method to floor a double and return an int
	 *
	 * @param d Double value to floor
	 * @return floored int
	 */
	public static int floor(double d) {
		return (int)Math.floor(d);
	}

	/**
	 * Convenince method to round a float and return an int
	 *
	 * @param f Float value to round
	 * @return rounded int
	 */
	public static int round(float f) {
		return Math.round(f + 0.5f);
	}

	/**
	 * Convenince method to round a double and return an int
	 *
	 * @param d Double value to round
	 * @return rounded int
	 */
	public static int round(double d) {
		return (int)Math.round(d);
	}

	/**
	 * Convenince method to ceil a float and return a float
	 *
	 * @param f Float value to ceil
	 * @return ceiled float
	 */
	public static float ceilf(float f) {
		// NOTE: We don't use FloatMath because Android Lint says this is faster
		return (float)Math.ceil(f);
	}

	/**
	 * Convenince method to floor a float and return a float
	 *
	 * @param f Float value to floor
	 * @return floored float
	 */
	public static float floorf(float f) {
		// NOTE: We don't use FloatMath because Android Lint says this is faster
		return FloatMath.floor(f);
	}

	/**
	 * Convenince method to round a float and return a float
	 *
	 * @param f Float value to ceil
	 * @return rounded float
	 */
	public static float roundf(float f) {
		// NOTE: We don't use FloatMath because Android Lint says this is faster
		return FloatMath.floor(f + 0.5f);
	}

	/**
	 * Clamps the value to the minimum and maximum bounds
	 *
	 * @param value Value to clamp
	 * @param minimum Minimum the value can be
	 * @param maximum Maximum the value can be
	 * @return If the value is > maximum, maximum is returned, if the value < minimum, minimum is returned, otherwise the provided value is returned.
	 */
	public static float clampf(float value, float minimum, float maximum) {
		return Math.min(Math.max(value, minimum), maximum);
	}

	/**
	 * Converts degrees to radians
	 *
	 * @param degrees Degrees to convert
	 * @return Radians
	 */
	public static float degreesToRadians(float degrees) {
		return (degrees / 360.0f) * ((float)Math.PI * 2.0f);
	}

	/**
	 * Converts radians to degrees
	 *
	 * @param radians Radians to convert
	 * @return Degrees
	 */
	public static float radiansToDegrees(float radians) {
		return (radians / ((float)Math.PI * 2)) * 360.0f;
	}

	// Layer backing

	/**
	 * Get the layer class backing this view
	 * @return View layer
	 * @hide
	 */
	public Class<? extends ViewLayer> getLayerClass() {
		return VIEW_LAYER_CLASS;
	}

	ViewLayer createLayer(android.content.Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		return getLayerClass().getConstructor(android.content.Context.class).newInstance(context);
	}

	// Accessibility

	@Override
	public boolean isAccessibilityElement() {
		return isAccessibilityElement;
	}

	@Override
	public void setIsAccessibilityElement(boolean accessibilityElement) {
		isAccessibilityElement = accessibilityElement;
	}

	@Override
	public String getAccessibilityLabel() {
		return accessibilityLabel;
	}

	@Override
	public void setAccessibilityLabel(String accessibilityLabel) {
		this.accessibilityLabel = accessibilityLabel;
	}

	@Override
	public String getAccessibilityHint() {
		return accessibilityHint;
	}

	@Override
	public void setAccessibilityHint(String accessibilityHint) {
		this.accessibilityHint = accessibilityHint;
	}

	@Override
	public String getAccessibilityValue() {
		return accessibilityValue;
	}

	@Override
	public void setAccessibilityValue(String accessibilityValue) {
		this.accessibilityValue = accessibilityValue;
	}

	@Override
	public Trait[] getAccessibilityTraits() {
		return accessibilityTraits;
	}

	@Override
	public void setAccessibilityTraits(Trait... accessibilityTraits) {
		this.accessibilityTraits = accessibilityTraits;
	}

	@Override
	public Rect getAccessibilityFrame() {
		return accessibilityFrame;
	}

	@Override
	public void setAccessibilityFrame(Rect accessibilityFrame) {
		this.accessibilityFrame = accessibilityFrame;
	}

	public Point getAccessibilityActivationPoint() {
		return accessibilityActivationPoint;
	}

	@Override
	public void setAccessibilityActivationPoint(Point accessibilityActivationPoint) {
		this.accessibilityActivationPoint = accessibilityActivationPoint;
	}

	@Override
	public boolean getAccessibilityElementsHidden() {
		return accessibilityElementsHidden;
	}

	@Override
	public void setAccessibilityElementsHidden(boolean accessibilityElementsHidden) {
		this.accessibilityElementsHidden = accessibilityElementsHidden;
	}

	@Override
	public boolean getAccessibilityViewIsModal() {
		return accessibilityViewIsModal;
	}

	@Override
	public void setAccessibilityViewIsModal(boolean accessibilityViewIsModal) {
		this.accessibilityViewIsModal = accessibilityViewIsModal;
	}

	@Override
	public boolean shouldGroupAccessibilityChildren() {
		return shouldGroupAccessibilityChildren;
	}

	@Override
	public void setShouldGroupAccessibilityChildren(boolean shouldGroupAccessibilityChildren) {
		this.shouldGroupAccessibilityChildren = shouldGroupAccessibilityChildren;
	}

	// Snapshotting

	public View snapshotView() {
		return new SnapshotView(this);
	}

	public View resizableSnapshotView(Rect rect, EdgeInsets capInsets) {
		return new SnapshotView(this);
	}

	// Animations

	/**
	 * Options to pass into {@link View#transition(View, View, long, mocha.ui.View.AnimationCompletion, mocha.ui.View.AnimationOption...)}
	 */
	public enum AnimationOption {
		/**
		 * If provided, {@link mocha.ui.View#layoutIfNeeded()} if needed will be called on the transitioning views.
		 */
		LAYOUT_SUBVIEWS,

		/**
		 * By default, transitions disable user interaction on the transitioning views during the animation.
		 * This option allows user interaction to take place
		 */
		ALLOW_USER_INTERACTION,

		/**
		 * Optionally show/hide the transition views instead of changing their
		 * superview, which is the default behavior.
		 */
		SHOW_HIDE_TRANSITION_VIEWS,

		/**
		 * @see AnimationCurve#EASE_IN_OUT
		 */
		CURVE_EASE_IN_OUT,

		/**
		 * @see AnimationCurve#EASE_IN
		 */
		CURVE_EASE_IN,

		/**
		 * @see AnimationCurve#EASE_OUT
		 */
		CURVE_EASE_OUT,

		/**
		 * @see AnimationCurve#LINEAR
		 */
		CURVE_LINEAR,

		TRANSITION_NONE,

		/**
		 * If provided, the transition will fade between the two views
		 */
		TRANSITION_CROSS_DISSOLVE,
	}

	private static List<ViewAnimation> viewAnimationStack = new ArrayList<ViewAnimation>();
	static ViewAnimation currentViewAnimation = null;
	static boolean areAnimationsEnabled = true;

	/**
	 * Check whether or not you're in an animation transaction
	 *
	 * @return If true, setting animatable properties will cause them to animate the change
	 */
	public static boolean inAnimationTransaction() {
		return currentViewAnimation != null && areAnimationsEnabled;
	}

	/**
	 * Setup an animation with the specified duration and group of animations
	 *
	 * @param duration Length of the animation in seconds
	 * @param animations Changes to be animated
	 */
	public static void animateWithDuration(double duration, Animations animations) {
		animateWithDuration((long)(duration * 1000.0), animations, null);
	}

	/**
	 * Setup an animation with the specified duration and group of animations
	 *
	 * @param duration Length of the animation in milliseconds
	 * @param animations Changes to be animated
	 */
	public static void animateWithDuration(long duration, Animations animations) {
		animateWithDuration(duration, animations, null);
	}

	/**
	 * Setup an animation with the specified duration and group of animations
	 *
	 * @param duration Length of the animation in seconds
	 * @param animations Changes to be animated
	 * @param completion Callback for when the animation ends
	 */
	public static void animateWithDuration(double duration, Animations animations, final AnimationCompletion completion) {
		animateWithDuration((long)(duration * 1000.0), 0, animations, completion);
	}

	/**
	 * Setup an animation with the specified duration and group of animations
	 *
	 * @param duration Length of the animation in milliseconds
	 * @param animations Changes to be animated
	 * @param completion Callback for when the animation ends
	 */
	public static void animateWithDuration(long duration, Animations animations, final AnimationCompletion completion) {
		animateWithDuration(duration, 0, animations, completion);
	}

	/**
	 * Setup an animation with the specied duration and group of animations

	 * @param duration Length of the animation in milliseconds
	 * @param delay Milliseconds to wait before starting the animation
	 * @param animations Changes to be animated
	 * @param completion Callback for when the animation ends
	 */
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

	/**
	 * Begin an animation context
	 */
	public static void beginAnimations() {
		beginAnimations(null, null);
	}

	/**
	 * Begin an animation context
	 *
	 * @param animationID Animation ID for this context
	 * @param context Optional context parameter to pass through to will start/did stop callbacks
	 */
	public static void beginAnimations(String animationID, Object context) {
		currentViewAnimation = new ViewAnimation();
		currentViewAnimation.animationID = animationID;
		currentViewAnimation.context = context;
		viewAnimationStack.add(currentViewAnimation);
	}

	/**
	 * Commit and start the changes made within this animation context
	 */
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

	/**
	 * Cancels any animation blocks referencing this view.
	 *
	 * This will apply to ALL view's in an animation block and not just this view.
	 * The animating properties will be left the state they were upon cancellation.
	 * Meaning, if alpha was animating from 0.0f to 1.0f and the animation is cancelled
	 * half way through, the final alpha value will be 0.5f.
	 *
	 * {@note Calling this in an animation context will not remove changes made in that context.}
	 */
	public void cancelAnimations() {
		ViewAnimation.cancelAllAnimationsReferencingView(this);
	}

	boolean changeEndValueForAnimationType(ViewAnimation.Type type, Object endValue) {
		ViewAnimation animation = this.animations[type.value];
		return animation != null && animation.changeEndValueForAnimationType(this, type, endValue);
	}

	/**
	 * Check whether or not we're in an animation context
	 *
	 * @return If true if we're in an animation context, false otherwise
	 */
	public static boolean isInAnimationContext() {
		return currentViewAnimation != null;
	}

	/**
	 * Set a callback for when the animation is about to start
	 *
	 * This method does nothing if we're not in an animation context
	 *
	 * @param animationWillStartCallback Animation will start callback
	 */
	public static void setAnimationWillStartCallback(AnimationWillStart animationWillStartCallback) {
		if(currentViewAnimation != null) {
			currentViewAnimation.willStart = animationWillStartCallback;
		}
	}

	/**
	 * Set a callback for when the animation stops
	 *
	 * This method does nothing if we're not in an animation context
	 *
	 * @param animationDidStopCallback Animation did stop callback
	 */
	public static void setAnimationDidStopCallback(AnimationDidStop animationDidStopCallback) {
		if(currentViewAnimation != null) {
			currentViewAnimation.didStop = animationDidStopCallback;
		}
	}

	/**
	 * Set the duration of the animation
	 *
	 * @param duration Duration in milliseconds
	 */
	public static void setAnimationDuration(long duration) {
		if(currentViewAnimation != null) {
			currentViewAnimation.duration = duration;
		}
	}

	/**
	 * Set how many times the animation should repeat
	 *
	 * <p>Setting to 0 will play the animation once without repeating, you may set this
	 * value to a fraction, 0.5 implies a single play through from start to end. Once the number
	 * of repeats has finished, the values are set to their end values.</p>
	 *
	 * <p>Set to {@link Double#MAX_VALUE} to repeat infinitely.</p>
	 *
	 * @param repeatCount Number of times the animation should repeat
	 */
	public static void setAnimationRepeatCount(double repeatCount) {
		if(currentViewAnimation != null) {
			currentViewAnimation.repeatCount = repeatCount;
		}
	}

	/**
	 * Set whether or not the animation reverses when it repeats
	 *
	 * Repeating and reversing animations should always end on a .5 value, not a .0 value.
	 * A .5 value indicates the animation will end on the "end" values, a .0 value indicates
	 * the animation will end on the "start" values and then snap back to the end values
	 * upon completion.
	 *
	 * @param autoreverses If true, the animation will play backwards after it gets to the end, and then forwards again until the number of repeats is complete.
	 *                     If false and the animation repeats, the animation will jump back to the start values after it ends
	 */
	public static void setAnimationRepeatAutoreverses(boolean autoreverses) {
		if(currentViewAnimation != null) {
			currentViewAnimation.reverses = autoreverses;
		}
	}

	/**
	 * Set the number of milliseconds to wait before starting the animation
	 *
	 * @param delay Milliseconds to delay the animation start
	 */
	public static void setAnimationDelay(long delay) {
		if(currentViewAnimation != null) {
			currentViewAnimation.delay = delay;
		}
	}

	/**
	 * Set the curve of the animation
	 *
	 * @param animationCurve Animation curve
	 */
	public static void setAnimationCurve(AnimationCurve animationCurve) {
		if(currentViewAnimation != null) {
			currentViewAnimation.animationCurve = animationCurve;
		}
	}

	public static void setTimingFunction(TimingFunction timingFunction) {
		if(currentViewAnimation != null) {
			currentViewAnimation.timingFunction = timingFunction;
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
	 * Performs changes with animations disabled
	 *
	 * @param runnable logic to run during session
	 */
	public static void performWithoutAnimation(Runnable runnable) {
		setAnimationsEnabled(false, runnable);
	}

	/**
	 * On by default, turning off will disable any animations, including if you're in an animation block.
	 *
	 * @param enabled whether or not animations should be enabled
	 */
	public static void setAnimationsEnabled(boolean enabled) {
		areAnimationsEnabled = enabled;
	}

	/**
	 * Check whether or not animations are enabled
	 *
	 * {@note This is true by default, true simply means animations can take place, not that they are taking place.}
	 *
	 * @return true if animations are allowed, false otherwise
	 */
	public static boolean areAnimationsEnabled() {
		return areAnimationsEnabled;
	}

	/**
	 * Transition from one view to another
	 *
	 * {@note By default, the toView is added to fromView's superview, and fromView is removed from it's
	 * superview upon completion of the transition.  You can supply the SHOW_HIDE_TRANSITION_VIEWS option to
	 * hide the fromView upon completion instead of removing it.}
	 *
	 * @param fromView View to transition from
	 * @param toView View to transition to
	 * @param duration Length of the transition animation in milliseconds
	 * @param completion Completion callback
	 * @param options Animation options
	 */
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
