/*
 *  @author Shaun
 *	@date 11/28/12
 *	@copyright	2012 enormego All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class ViewLayerNative extends ViewGroup implements ViewLayer {
	private static boolean ignoreLayout;

	private Rect frame;
	private Rect bounds;
	private View view;
	private boolean supportsDrawing;
	public final float scale;

	public ViewLayerNative(Context context) {
		super(context);
		this.setClipToPadding(false);
		this.setClipChildren(false);
		this.scale = context.getResources().getDisplayMetrics().density;
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
		this.setX(this.frame.origin.x - bounds.origin.x);
		this.setY(this.frame.origin.y - bounds.origin.y);
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

	public static int ceil(float f) {
		return (int)Math.ceil((double)f);
	}

	public static int floor(float f) {
		return (int)f;
	}

	public static int round(float f) {
		return (int)(f + 0.5f);
	}

	public void draw(Canvas canvas) {
		boolean clips = this.view.clipsToBounds();

		if(clips) {
			canvas.save();
			canvas.clipRect(new RectF(0.0f, 0.0f, this.bounds.size.width, this.bounds.size.height));
		}

		super.draw(canvas);

		if(clips) {
			canvas.restore();
		}
	}

	// ViewLayer

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
