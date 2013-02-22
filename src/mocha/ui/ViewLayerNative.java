/*
 *  @author Shaun
 *	@date 11/28/12
 *	@copyright	2012 enormego All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import mocha.graphics.AffineTransform;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class ViewLayerNative extends ViewGroup implements ViewLayer {
	private static boolean ignoreLayout;

	private Rect frame;
	private Rect bounds;
	private View view;
	private AffineTransform transform;
	private Matrix matrix;
	private boolean supportsDrawing;
	private boolean clipsToBounds;
	public final float scale;

	public ViewLayerNative(Context context) {
		super(context);
		this.setClipToPadding(false);
		this.setClipsToBounds(false);
		this.scale = context.getResources().getDisplayMetrics().density;
		this.transform = AffineTransform.identity();
		this.matrix = null;
	}

	private static boolean pushIgnoreLayout() {
		boolean old = ignoreLayout;
		ignoreLayout = true;
		return old;
	}

	private static void popIgnoreLayout(boolean oldValue) {
		ignoreLayout = oldValue;
	}

	public void setSupportsDrawing(boolean supportsDrawing) {
		this.setWillNotDraw(!supportsDrawing);
		this.supportsDrawing = supportsDrawing;

		if(supportsDrawing) {
			// this.setDrawingCacheEnabled(true);
			// this.setDrawingCacheQuality(android.view.View.DRAWING_CACHE_QUALITY_HIGH);
		}
	}

	public void setClipsToBounds(boolean clipsToBounds) {
		this.setClipChildren(clipsToBounds);
		this.clipsToBounds = clipsToBounds;
	}

	public boolean clipsToBounds() {
		return this.clipsToBounds;
	}

	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);

		if(this.supportsDrawing) {
			// this.setDrawingCacheBackgroundColor(backgroundColor);
		}
	}

	public void setView(View view) {
		this.view = view;
		this.setTag(view.getClass().toString());
	}

	public View getView() {
		return this.view;
	}

	Rect getFrame() {
		return frame;
	}

	public void setFrame(Rect frame, Rect bounds) {
		this.setFrame(frame, bounds, this.frame == null || frame == null || !this.frame.size.equals(frame.size));
	}

	void setFrame(Rect frame, Rect bounds, boolean setNeedsLayout) {
		this.frame = frame.getScaledRect(scale);
		this.bounds = bounds.getScaledRect(scale);

		View superview = this.getView().getSuperview();

		if(superview != null) {
			this.layoutRelativeToBounds(superview.getLayer().getBounds());
		}

		if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}
	}

	public void didMoveToSuperlayer() {
		View superview = this.getView().getSuperview();

		if(superview != null) {
			boolean ignoreLayout = pushIgnoreLayout();
			this.layoutRelativeToBounds(superview.getLayer().getBounds());
			this.getView()._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		}
	}

	private void layoutRelativeToBounds(Rect bounds) {
		boolean ignoreLayout = pushIgnoreLayout();
		this.layout(0, 0, ceil(this.frame.size.width), ceil(this.frame.size.height));
		this.setX((this.frame.origin.x - bounds.origin.x));
		this.setY((this.frame.origin.y - bounds.origin.y));
		popIgnoreLayout(ignoreLayout);
	}

	public Rect getBounds() {
		return bounds;
	}

	public void setBounds(Rect bounds) {
		this.setBounds(bounds, true);
	}

	void setBounds(Rect bounds, boolean setNeedsLayout) {
		Point oldPoint = this.bounds != null ? this.bounds.origin : null;
		this.bounds = bounds.getScaledRect(scale);

		if(oldPoint != null && !oldPoint.equals(this.bounds.origin)) {
			boolean ignoreLayout = pushIgnoreLayout();
			for(View subview : this.view.getSubviews()) {
				ViewLayer layer = subview.getLayer();
				if(layer instanceof ViewLayerNative) {
					((ViewLayerNative)layer).layoutRelativeToBounds(this.bounds);
				}
			}

			this.view._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		} else if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		this.setMeasuredDimension(ceil(frame.size.width), ceil(this.frame.size.height));
	}

	protected void onLayout(boolean changed, int i, int i1, int i2, int i3) {
		if(ignoreLayout) return;
		boolean ignoreLayout = pushIgnoreLayout();
		this.getView()._layoutSubviews();
		popIgnoreLayout(ignoreLayout);
	}

	public boolean onTouchEvent(MotionEvent motionEvent) {
		return false;
	}

	protected void onDraw(android.graphics.Canvas canvas) {
		super.onDraw(canvas);

		View view = this.getView();
		view.draw(new mocha.graphics.Context(canvas, this.scale), new Rect(view.getBounds()));
	}

	public void draw(android.graphics.Canvas canvas) {
		if(this.matrix == null) {
			super.draw(canvas);
		} else {
			canvas.save();

			float centerX = this.bounds.size.width / 2.0f;
			float centerY = this.bounds.size.height / 2.0f;

			canvas.translate(centerX, centerY); // Push center
			canvas.concat(this.matrix);
			canvas.translate(-centerX, -centerY); // Pop center

			super.draw(canvas);

			canvas.restore();
		}
	}

	public static int ceil(float f) {
		return (int)Math.ceil((double)f);
	}

	public static int floor(float f) {
		return (int)f;
	}

	public static int round(float f) {
		return (int)(f + 0.5f);
	}

	// ViewLayer

	public AffineTransform getTransform() {
		return this.transform;
	}

	public void setTransform(AffineTransform transform) {
		if(transform == null) transform = AffineTransform.identity();
		if(this.transform.equals(transform)) return;
		this.transform = transform.copy();

		if(this.transform.isIdentity()) {
			this.matrix = null;
		} else {
			float[] values = new float[] {
					this.transform.getA(), this.transform.getB(),
					this.transform.getC(), this.transform.getD(),

					this.transform.getTx() * this.scale,
					this.transform.getTy() * this.scale
			};

			if(this.matrix == null) {
				this.matrix = new Matrix();
			}

			this.matrix.setValues(new float[]{
					values[0], values[2], values[4],
					values[1], values[3], values[5],
					0.0f, 0.0f, 1.0f
			});
		}

		this.setNeedsDisplay();
		this.getSuperlayer().setNeedsDisplay();
	}

	public boolean isHidden() {
		return this.getVisibility() == GONE;
	}

	public void setHidden(boolean hidden) {
		this.setVisibility(hidden ? GONE : VISIBLE);
	}

	public void setNeedsLayout() {
		this.requestLayout();
	}

	public void setNeedsDisplay() {
		this.invalidate();
	}

	public void setNeedsDisplay(Rect dirtyRect) {
		this.invalidate(dirtyRect.toSystemRect(this.scale));
	}

	public void addSublayer(ViewLayer layer) {
		if(!(layer instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, layer);
		ViewLayerNative canvasLayer = (ViewLayerNative)layer;

		if(canvasLayer.getParent() == this) return;
		canvasLayer.removeFromSuperlayer();

		this.addView((ViewLayerNative)layer);
	}

	public void insertSublayerAtIndex(ViewLayer layer, int index) {
		if(!(layer instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, layer);

		ViewLayerNative canvasLayer = (ViewLayerNative)layer;

		if(canvasLayer.getParent() == this) return;
		canvasLayer.removeFromSuperlayer();

		index = Math.max(Math.min(index, this.getChildCount()), 0);
		this.addView(canvasLayer, index);
	}

	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, sibling);

		ViewLayerNative canvasLayer = (ViewLayerNative)layer;
		ViewLayerNative canvasSibling = (ViewLayerNative)sibling;
		int index = this.getIndexOf(canvasSibling);
		this.insertSublayerAtIndex(canvasLayer, index > 0 ? index - 1 : 0);
	}

	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerNative)) throw new InvalidSubLayerClassException(this, sibling);

		ViewLayerNative canvasLayer = (ViewLayerNative)layer;
		ViewLayerNative canvasSibling = (ViewLayerNative)sibling;

		int index = this.getIndexOf(canvasSibling);
		this.insertSublayerAtIndex(canvasLayer, index+1);
	}

	private int getIndexOf(ViewLayerNative layer) {
		int count = this.getChildCount();

		for(int index = 0; index < count; index++) {
			if(this.getChildAt(index) == layer) {
				return index;
			}
		}

		return -1;
	}

	public List<ViewLayer> getSublayers() {
		List<ViewLayer> sublayers = new ArrayList<ViewLayer>();

		int count = this.getChildCount();

		for(int index = 0; index < count; index++) {
			android.view.View child = this.getChildAt(index);

			if(child instanceof ViewLayer) {
				sublayers.add((ViewLayer)child);
			}
		}

		return sublayers;
	}

	public ViewLayer getSuperlayer() {
		ViewParent parent;
		if((parent = this.getParent()) != null) {
			if(parent instanceof ViewLayer) {
				return (ViewLayer)parent;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public void removeFromSuperlayer() {
		if(this.getParent() != null) {
			((ViewGroup)this.getParent()).removeView(this);
		}
	}
}
