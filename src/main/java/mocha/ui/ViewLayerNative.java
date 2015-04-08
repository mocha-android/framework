/**
 *  @author Shaun
 *  @date 4/20/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import mocha.foundation.MObject;
import mocha.graphics.AffineTransform;
import mocha.graphics.Path;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.ArrayList;
import java.util.List;

public class ViewLayerNative extends MObject implements ViewLayer {

	private FrameLayout.LayoutParams layoutParams;
	private FrameLayout layout;
	private Context context;
	private mocha.graphics.Context drawContext;

	private final Rect frame;
	private final Rect bounds;
	private final android.graphics.Rect reuseableRect;
	private final android.graphics.RectF reuseableRectF;

	private boolean supportsDrawing;
	private boolean clipsToBounds;
	private int backgroundColor;
	private final AffineTransform transform;
	private int shadowColor;
	private float shadowOpacity;
	private Size shadowOffset;
	private float shadowRadius;
	private Path shadowPath;

	private Path cornerPath;
	private android.graphics.Path cornerNativePath;
	private float cornerRadius;
	private Paint backgroundPaint;

	private int borderColor;
	private float borderWidth;
	private Path borderPath;
	private android.graphics.Path borderNativePath;

	private List<ViewLayerNative> sublayers;
	private ViewLayerNative superlayer;

	private View view;
	private Runnable scheduledLayoutCallback;
	private Runnable layoutCallback;

	private View clipToView;
	private Matrix matrix;
	private float tx;
	private float ty;

	final float scale;

	public ViewLayerNative(Context context) {
		this.context = context;

		this.layout = this.createLayout(context);
		this.layout.setClipToPadding(false);
		this.layout.setClipChildren(false);
		this.layout.setWillNotDraw(false);

		this.sublayers = new ArrayList<>();

		this.frame = Rect.zero();
		this.bounds = Rect.zero();
		this.reuseableRect = new android.graphics.Rect();
		this.reuseableRectF = new android.graphics.RectF();
		this.transform = AffineTransform.identity();
		this.clipsToBounds = false;

		this.scale = Screen.mainScreen().getScale();
	}

	public Context getContext() {
		return this.context;
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}

	public boolean getSupportsDrawing() {
		return supportsDrawing;
	}

	public void setSupportsDrawing(boolean supportsDrawing) {
		this.supportsDrawing = supportsDrawing;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;

		if(this.useLayoutBackgroundColor()) {
			this.layout.setBackgroundColor(backgroundColor);
		}
	}

	protected boolean useLayoutBackgroundColor() {
		return this.cornerRadius <= 0.0f;
	}

	protected void drawBackground(Canvas canvas, RectF rect, Paint borderPaint, boolean alwaysDraw) {
		if(this.cornerRadius > 0.0f) {
			if(this.backgroundColor != Color.TRANSPARENT) {
				canvas.drawPath(this.getCornerNativePath(), this.getBackgroundPaint());
			}
		} else if(alwaysDraw && this.backgroundColor != Color.TRANSPARENT) {
			canvas.drawRect(rect, this.getBackgroundPaint());
		}

		if(borderPaint != null) {
			canvas.drawPath(this.getBorderNativePath(), borderPaint);
		}
	}

	private Paint getBackgroundPaint() {
		if(this.backgroundPaint == null) {
			this.backgroundPaint = new Paint();
			this.backgroundPaint.setAntiAlias(true);
			this.backgroundPaint.setDither(true);
		}

		this.backgroundPaint.setColor(this.backgroundColor);
		return this.backgroundPaint;
	}

	public boolean clipsToBounds() {
		return this.clipsToBounds;
	}

	public void setClipsToBounds(boolean clipsToBounds) {
		if(this.clipsToBounds != clipsToBounds) {
			this.clipsToBounds = clipsToBounds;
			this.updateHierarchyClips();
		}
	}

	private void updateHierarchyClips() {
		for(ViewLayerNative sublayer : this.sublayers) {
			if(this.clipsToBounds) {
				sublayer.setClipToView(this.getView());
			} else {
				sublayer.setClipToView(this.clipToView);
			}
		}
	}

	private void setClipToView(View clipToView) {
		this.clipToView = clipToView;
		this.updateHierarchyClips();
	}

	public void setFrame(Rect frame, Rect bounds) {
		frame = frame == null ? Rect.zero() : frame;
		boolean originChanged = !frame.origin.equals(this.frame.origin);
		boolean sizeChanged = !frame.size.equals(this.frame.size);

		this.frame.set(frame);


		if(originChanged) {
			this.updatePosition();
		}

		if(sizeChanged) {
			this.updateSize();
			this.setNeedsLayout();
		}

		this.setBounds(bounds);
	}

	public void setBounds(Rect bounds) {
		if(!this.bounds.equals(bounds)) {
			this.bounds.set(bounds);

			for(ViewLayerNative sublayer : this.sublayers) {
				sublayer.updatePosition();
			}
		}
	}

	public Rect getBounds() {
		return this.bounds;
	}

	private void updateLayout() {
		this.updateSize();
		this.updatePosition();
	}

	void updateSize() {
		this.frame.toSystemRect(this.reuseableRect, this.scale);
		float width = this.reuseableRect.width();
		float height = this.reuseableRect.height();

		if(this.layoutParams == null) {
			this.layoutParams = new FrameLayout.LayoutParams(this.reuseableRect.width(), this.reuseableRect.height());
		} else {
			this.layoutParams.width = this.reuseableRect.width();
			this.layoutParams.height = this.reuseableRect.height();
		}

		this.layout.setLayoutParams(this.layoutParams);
		this.onUpdateSize(width, height);

		this.cornerPath = null;
		this.cornerNativePath = null;

		this.borderPath = null;
		this.borderNativePath = null;
	}

	void updatePosition() {
		Rect frame = this.frame.getScaledRect(scale);

		float offsetX;
		float offsetY;

		if(this.superlayer != null) {
			Rect bounds = this.superlayer.bounds.getScaledRect(scale);
			offsetX = bounds.origin.x;
			offsetY = bounds.origin.y;
		} else {
			offsetX = 0;
			offsetY = 0;
		}
		this.layout.setTranslationX((int)(frame.origin.x - offsetX + this.tx));
		this.layout.setTranslationY((int)(frame.origin.y - offsetY + this.ty));
	}

	protected void onUpdateSize(float nativeWidth, float nativeHeight) {

	}

	public boolean isHidden() {
		return this.layout.getVisibility() == android.view.View.GONE;
	}

	public void setHidden(boolean hidden) {
		this.layout.setVisibility(hidden ? android.view.View.GONE : android.view.View.VISIBLE);
	}

	public float getAlpha() {
		return this.layout.getAlpha();
	}

	public void setAlpha(float alpha) {
		this.layout.setAlpha(alpha);
	}

	public void setNeedsLayout() {
		if(this.scheduledLayoutCallback != null) {
			return;
		}

		if(this.layoutCallback == null) {
			this.layoutCallback = new LayoutCallback();
		}

		this.scheduledLayoutCallback = performOnMainAfterDelay(0, this.layoutCallback);
		this.layout.requestLayout();
	}

	public void setNeedsDisplay() {
		this.layout.invalidate();
	}

	public void setNeedsDisplay(Rect dirtyRect) {
		this.layout.invalidate();
	}

	public AffineTransform getTransform() {
		return transform;
	}

	public void setTransform(AffineTransform transform) {
		if(this.transform.equals(transform)) return;
		this.transform.set(transform);

		if(this.transform.isIdentity()) {
			this.matrix = null;

			if(this.layout.getScaleX() != 1.0f) {
				this.layout.setScaleX(1.0f);
			}

			if(this.layout.getScaleY() != 1.0f) {
				this.layout.setScaleY(1.0f);
			}

			this.resetTranslation();
		} else {
			float a = this.transform.getA();
			float b = this.transform.getB();
			float c = this.transform.getC();
			float d = this.transform.getD();

			float tx = this.transform.getTx() * this.scale;
			float ty = this.transform.getTy() * this.scale;

			// Check for a simple scale/translate animation
			if(b == 0.0f && c == 0.0f) {
				if(this.matrix != null) {
					this.matrix = null;
					this.setNeedsDisplay();
				}

				this.layout.setScaleX(a);
				this.layout.setScaleY(d);

				if(tx != this.tx || ty != this.ty) {
					this.tx = tx;
					this.ty = ty;

					this.updatePosition();
				}
			}

			// Use a full matrix transform, which is still buggy.
			else {
				float[] values = new float[] {
						a, b,
						c, d,

						tx,
						ty
				};

				if(this.matrix == null) {
					this.matrix = new Matrix();
				}

				this.matrix.setValues(new float[]{
						values[0], values[2], values[4],
						values[1], values[3], values[5],
						0.0f, 0.0f, 1.0f
				});

				this.resetTranslation();
			}

			this.setNeedsDisplay();

			if(this.getSuperlayer() != null) {
				this.getSuperlayer().setNeedsDisplay();
			}
		}
	}

	private void resetTranslation() {
		if(this.tx != 0.0f || this.ty != 0.0f) {
			this.tx = 0.0f;
			this.ty = 0.0f;

			this.updatePosition();
		}
	}

	public void addSublayer(ViewLayer layer) {
		this.insertSublayerAtIndex(layer, Integer.MAX_VALUE);
	}

	public void insertSublayerAtIndex(ViewLayer layer, int index) {
		ViewLayerNative layer1 = this.assertLayerType(layer);
		ViewLayer superlayer = layer1.getSuperlayer();

		if(superlayer != null && superlayer != this) {
			layer1.removeFromSuperlayer();
		}

		int size = this.sublayers.size();

		if(superlayer == this) {
			this.layout.removeView(layer1.layout);
		}

		layer1.setClipToView(this.clipsToBounds ? this.getView() : this.clipToView);

		if(index >= size) {
			this.layout.addView(layer1.layout);
			this.sublayers.add(layer1);
		} else {
			if(index < 0) {
				index = 0;
			}

			this.layout.addView(layer1.layout, index);
			this.sublayers.add(index, layer1);
		}

		if(superlayer != this) {
			layer1.superlayer = this;
			layer1.didMoveToSuperlayer();
		}

		layer1.updateLayout();
	}

	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling) {
		ViewLayerNative sibling1 = this.assertLayerType(sibling);
		int index = this.sublayers.indexOf(sibling1);
		this.insertSublayerAtIndex(layer, index > 0 ? index - 1 : 0);
	}

	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling) {
		ViewLayerNative sibling1 = this.assertLayerType(sibling);
		int index = this.sublayers.indexOf(sibling1);
		this.insertSublayerAtIndex(layer, index == -1 ? Integer.MAX_VALUE : index + 1);
	}

	public void didMoveToSuperlayer() {

	}

	public List<ViewLayer> getSublayers() {
		return new ArrayList<ViewLayer>(this.sublayers);
	}

	public ViewLayer getSuperlayer() {
		return this.superlayer;
	}

	public void removeFromSuperlayer() {
		if(this.superlayer != null) {
			this.superlayer.sublayers.remove(this);
			this.superlayer.layout.removeView(this.layout);
			this.superlayer = null;

			this.didMoveToSuperlayer();
		}
	}

	private ViewLayerNative assertLayerType(ViewLayer layer) {
		if(layer instanceof ViewLayerNative) {
			return (ViewLayerNative) layer;
		} else {
			throw new InvalidSubLayerClassException(this, layer);
		}
	}

	public int getShadowColor() {
		return shadowColor;
	}

	public void setShadowColor(int shadowColor) {
		this.shadowColor = shadowColor;
	}

	public float getShadowOpacity() {
		return shadowOpacity;
	}

	public void setShadowOpacity(float shadowOpacity) {
		this.shadowOpacity = shadowOpacity;
	}

	public Size getShadowOffset() {
		return shadowOffset;
	}

	public void setShadowOffset(Size shadowOffset) {
		this.shadowOffset = shadowOffset;
	}

	public float getShadowRadius() {
		return shadowRadius;
	}

	public void setShadowRadius(float shadowRadius) {
		this.shadowRadius = shadowRadius;
	}

	public Path getShadowPath() {
		return shadowPath;
	}

	public void setShadowPath(Path shadowPath) {
		this.shadowPath = shadowPath;
	}

	public float getCornerRadius() {
		return this.cornerRadius;
	}

	protected Path getCornerPath() {
		if(this.cornerPath == null) {
			float cornerRadius = Math.min(Math.min(this.cornerRadius, this.bounds.size.width / 2.0f), this.bounds.size.height / 2.0f);
			this.cornerPath = Path.withRoundedRect(new Rect(this.bounds.size), cornerRadius);
		}

		return this.cornerPath;
	}

	protected android.graphics.Path getCornerNativePath() {
		if(this.cornerNativePath == null) {
			this.cornerNativePath = this.getCornerPath().getScaledNativePath(this.scale);
		}

		return this.cornerNativePath;
	}

	public void setCornerRadius(float cornerRadius) {
		this.cornerRadius = cornerRadius;

		if(this.cornerRadius > 0.0f) {
			this.setSupportsDrawing(true);
			this.layout.setBackgroundColor(Color.TRANSPARENT);
		} else {
			this.layout.setBackgroundColor(this.backgroundColor);
		}

		this.cornerPath = null;
		this.cornerNativePath = null;
	}

	public int getBorderColor() {
		return this.borderColor;
	}

	public void setBorderColor(int borderColor) {
		this.borderColor = borderColor;
		this.setNeedsDisplay();
	}

	public float getBorderWidth() {
		return this.borderWidth;
	}

	public void setBorderWidth(float borderWidth) {
		this.borderWidth = borderWidth;
		this.setNeedsDisplay();
	}

	protected Path getBorderPath() {
		if(this.borderPath == null) {
			Rect rect = new Rect(this.bounds.size);
			rect.inset(borderWidth / 2.0f, borderWidth / 2.0f);

			float cornerRadius = Math.min(Math.min(this.cornerRadius, this.bounds.size.width / 2.0f), this.bounds.size.height / 2.0f);
			this.borderPath = Path.withRoundedRect(rect, cornerRadius);
		}

		return this.borderPath;
	}

	protected android.graphics.Path getBorderNativePath() {
		if(this.borderNativePath == null) {
			this.borderNativePath = this.getBorderPath().getScaledNativePath(this.scale);
		}

		return this.borderNativePath;
	}


	public void renderInContext(mocha.graphics.Context context) {
		Rect bounds = this.view.getBounds();
		Rect rect = new Rect(0.0f, 0.0f, bounds.size.width, bounds.size.height);

		rect.toSystemRectF(this.reuseableRectF, this.scale);
		drawBackground(context.getCanvas(), this.reuseableRectF, null, true); // TODO: Support borders here

		if(this.supportsDrawing) {
			context.clipToRect(rect);
			this.getView().draw(context, rect);
		}

		int count = this.layout.getChildCount();
		for(int i = 0; i < count; i++) {
			android.view.View child = this.layout.getChildAt(i);
			if(child.getVisibility() != android.view.View.VISIBLE || child.getAlpha() < 0.01f) continue;

			if(child instanceof Layout) {
				Rect frame = ((Layout) child).getLayer().view.getFrame();

				if(!this.clipsToBounds || bounds.intersects(frame)) {
					context.save();
					context.translate(frame.origin.x - bounds.origin.x, frame.origin.y - bounds.origin.y);
					((Layout) child).getLayer().renderInContext(context);
					context.restore();
				}
			} else {
				this.renderSystemView(context, child);
			}
		}
	}

	public float getZPosition() {
		return 0;
	}

	public void setZPosition(float zPosition) {

	}

	private void renderSystemView(mocha.graphics.Context context, android.view.View systemView) {
		systemView.draw(context.getCanvas());
	}

	FrameLayout getLayout() {
		return this.layout;
	}

	public ViewGroup getViewGroup() {
		return this.layout;
	}

	Layout createLayout(Context context) {
		return new Layout(context);
	}

	class LayoutCallback implements Runnable {
		public void run() {
			scheduledLayoutCallback = null;
			view._layoutSubviews();
		}
	}

	class Layout extends FrameLayout {
		private Paint borderPaint;

		Layout(Context context) {
			super(context);
		}

		private ViewLayerNative getLayer() {
			return ViewLayerNative.this;
		}

		private boolean updateClippingRect(Canvas canvas) {
			RectF clippingRect = null;
			if(clipsToBounds) {
				clippingRect = new RectF(0.0f, 0.0f, bounds.size.width * scale, bounds.size.height * scale);
			} else if(clipToView != null) {
				// TODO: Properly handle this scenario
//				Rect bounds = this.clipToView.convertRectToView(this.clipToView.getBounds(), this.view);
//
//				if(!bounds.contains(this.view.frame)) {
//					mocha.foundation.MObject.MLog("SELF: %s %s | CANVAS: %s | CLIPPING TO: %s %s | adjusted: %s", this.view.getClass().getName(), this.frame, canvas.getClipBounds(), this.clipToView.getClass().getName(), this.clipToView.getBounds(), bounds);
//					// clippingRect = bounds.toSystemRectF(this.scale);
//				}
			}

			if(clippingRect != null) {
				canvas.save(Canvas.CLIP_SAVE_FLAG);
				canvas.clipRect(clippingRect);
				return true;
			} else {
				return false;
			}
		}

		public void draw(android.graphics.Canvas canvas) {
			boolean restore = this.updateClippingRect(canvas);

			if(matrix == null) {
				super.draw(canvas);
			} else {
				canvas.save(Canvas.MATRIX_SAVE_FLAG);

				float centerX = (bounds.size.width * scale) / 2.0f;
				float centerY = (bounds.size.height * scale) / 2.0f;

				canvas.translate(centerX, centerY); // Push center
				canvas.concat(matrix);
				canvas.translate(-centerX, -centerY); // Pop center

				super.draw(canvas);

				canvas.restore();
			}

			if(restore) {
				canvas.restore();
			}
		}

		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			if(borderWidth > 0.0f && borderColor != 0) {
				if(this.borderPaint == null) {
					this.borderPaint = new Paint();
					this.borderPaint.setAntiAlias(true);
					this.borderPaint.setDither(true);
					this.borderPaint.setStyle(Paint.Style.STROKE);
					this.borderPaint.setStrokeJoin(Paint.Join.MITER);
					this.borderPaint.setStrokeCap(Paint.Cap.ROUND);
				}

				this.borderPaint.setStrokeWidth(borderWidth * scale);
				this.borderPaint.setColor(borderColor);
			} else {
				this.borderPaint = null;
			}

			bounds.toSystemRectF(reuseableRectF, scale);
			reuseableRectF.right -= reuseableRectF.left;
			reuseableRectF.left = 0;

			reuseableRectF.bottom -= reuseableRectF.top;
			reuseableRectF.top = 0;

			drawBackground(canvas, reuseableRectF, this.borderPaint, false);

			if(supportsDrawing) {
				if(drawContext != null) {
					drawContext.reset(canvas);
				} else {
					drawContext = new mocha.graphics.Context(canvas, scale);
				}

				view.draw(drawContext, view.getBounds());

				drawContext.reset(null);
			}
		}

		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			super.onLayout(changed, l, t, r, b);
			// view._layoutSubviews();
		}

		public boolean onTouchEvent(MotionEvent motionEvent) {
			return false;
		}

	}

}
