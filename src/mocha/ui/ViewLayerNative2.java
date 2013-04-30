/**
 *  @author Shaun
 *  @date 4/20/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
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

public class ViewLayerNative2 extends MObject implements ViewLayer {

	private FrameLayout layout;
	private Context context;

	private Rect frame;
	private Rect bounds;

	private boolean supportsDrawing;
	private boolean clipsToBounds;
	private int backgroundColor;
	private AffineTransform transform;
	private int shadowColor;
	private float shadowOpacity;
	private Size shadowOffset;
	private float shadowRadius;
	private Path shadowPath;

	private List<ViewLayerNative2> sublayers;
	private ViewLayerNative2 superlayer;

	private View view;
	private Runnable layoutCallback;

	private View clipToView;
	private Matrix matrix;
	private float tx;
	private float ty;

	final float scale;

	public ViewLayerNative2(Context context) {
		this.context = context;

		this.layout = this.createLayout(context);
		this.layout.setClipToPadding(false);
		this.layout.setClipChildren(false);

		this.sublayers = new ArrayList<ViewLayerNative2>();

		this.frame = Rect.zero();
		this.bounds = Rect.zero();
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
		this.layout.setWillNotDraw(!supportsDrawing);
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.layout.setBackgroundColor(backgroundColor);
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
		for(ViewLayerNative2 sublayer : this.sublayers) {
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
		Rect oldFrame = this.frame;
		frame = frame == null ? Rect.zero() : frame.copy();
		this.frame = frame;


		if(!frame.origin.equals(oldFrame.origin)) {
			this.updatePosition();
		}

		if(!frame.size.equals(oldFrame.size)) {
			this.updateSize();
			this.setNeedsLayout();
		}

		this.setBounds(bounds);
	}

	public void setBounds(Rect bounds) {
		if(!this.bounds.equals(bounds)) {
			this.bounds = bounds == null ? Rect.zero() : bounds.copy();

			for(ViewLayerNative2 sublayer : this.sublayers) {
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
		android.graphics.Rect frame = this.frame.toSystemRect(scale);
		this.layout.setLayoutParams(new FrameLayout.LayoutParams(frame.width(), frame.height()));
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

		this.layout.setX(frame.origin.x - offsetX + this.tx);
		this.layout.setY(frame.origin.y - offsetY + this.ty);
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
		if(this.layoutCallback != null) {
			return;
		}

		this.layoutCallback = performOnMainAfterDelay(0, new Runnable() {
			public void run() {
				layoutCallback = null;
				view._layoutSubviews();
			}
		});

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
		if(transform == null) transform = AffineTransform.identity();
		if(this.transform.equals(transform)) return;
		this.transform = transform.copy();

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

			// Check for a simple scale animation
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
			}

			this.resetTranslation();
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
		ViewLayerNative2 layer1 = this.assertLayerType(layer);

		if(layer1.getSuperlayer() != null && layer1.getSuperlayer() != this) {
			layer1.removeFromSuperlayer();
		}

		int size = this.sublayers.size();
		index = Math.min(size, Math.max(0, index));

		if(layer1.getSuperlayer() == this) {
			this.layout.removeView(layer1.layout);
		}

		layer1.setClipToView(this.clipsToBounds ? this.getView() : this.clipToView);

		if(index == size) {
			this.layout.addView(layer1.layout);
			this.sublayers.add(layer1);
		} else {
			this.layout.addView(layer1.layout, index);
			this.sublayers.add(index, layer1);
		}

		if(layer1.getSuperlayer() != this) {
			layer1.superlayer = this;
			layer1.didMoveToSuperlayer();
		}

		layer1.updateLayout();
	}

	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling) {
		ViewLayerNative2 sibling1 = this.assertLayerType(sibling);
		int index = this.sublayers.indexOf(sibling1);
		this.insertSublayerAtIndex(layer, index > 0 ? index - 1 : 0);
	}

	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling) {
		ViewLayerNative2 sibling1 = this.assertLayerType(sibling);
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

	private ViewLayerNative2 assertLayerType(ViewLayer layer) {
		if(layer instanceof ViewLayerNative2) {
			return (ViewLayerNative2) layer;
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

	public void renderInContext(mocha.graphics.Context context) {
		Rect bounds = this.view.getBounds();
		Rect rect = new Rect(0.0f, 0.0f, bounds.size.width, bounds.size.height);

		if(this.backgroundColor != Color.TRANSPARENT) {
			context.setFillColor(this.backgroundColor);
			context.fillRect(rect);
		}

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

	class Layout extends FrameLayout {
		Layout(Context context) {
			super(context);
		}

		private ViewLayerNative2 getLayer() {
			return ViewLayerNative2.this;
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

			view.draw(new mocha.graphics.Context(canvas, scale), new Rect(view.getBounds()));
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
