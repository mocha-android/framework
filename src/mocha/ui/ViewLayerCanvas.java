/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.ViewGroup;
import mocha.foundation.MObject;
import mocha.graphics.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewLayerCanvas extends MObject implements ViewLayer {
	private static boolean ignoreLayout;
	private static final boolean CACHE_DRAWINGS = false;

	private android.content.Context context;
	private View view;
	private Rect frame;
	private Rect bounds;
	private boolean hidden;
	private float alpha;
	private final List<ViewLayerCanvas> sublayers;
	private List<ViewLayer> sublayersGeneric;
	private ViewLayerCanvas superlayer;
	private int backgroundColor;
	private boolean supportsDrawing;
	private Paint paint;
	private Bitmap cachedDrawing;
	private boolean clipsToBounds;
	private AffineTransform transform;
	private int shadowColor;
	private float shadowOpacity;
	private Size shadowOffset;
	private float shadowRadius;
	private Path shadowPath;
	private float cornerRadius;
	final float scale;
	final int dpi;

	private boolean needsLayout;
	private boolean needsDisplay;

	public ViewLayerCanvas(android.content.Context context) {
		this.context = context;
		this.alpha = 1.0f;
		this.hidden = false;
		this.sublayers = new ArrayList<ViewLayerCanvas>();
		this.sublayersGeneric = new ArrayList<ViewLayer>();
		this.scale = Screen.mainScreen().getScale();
		this.dpi = Screen.mainScreen().getDpi();
		this.paint = new Paint();
		this.needsDisplay = true;
		this.transform = AffineTransform.identity();
	}

	private static boolean pushIgnoreLayout() {
		boolean old = ignoreLayout;
		ignoreLayout = true;
		return old;
	}

	private static void popIgnoreLayout(boolean oldValue) {
		ignoreLayout = oldValue;
	}

	boolean layoutSublayersIfNeeded() {
		if(this.needsLayout) {
			this.layoutSublayers();
			return true;
		} else {
			return false;
		}
	}

	void layoutSublayers() {
		if(ignoreLayout) return;
		boolean ignoreLayout = pushIgnoreLayout();
		this.getView()._layoutSubviews();
		popIgnoreLayout(ignoreLayout);
		this.needsLayout = false;
	}

	public void setNeedsLayout() {
		if(!this.needsLayout) {
			this.needsLayout = true;

			WindowLayerCanvas windowLayer = this.getWindowLayer();
			if(windowLayer != null) {
				windowLayer.scheduleLayout();
			}
		}
	}

	List<ViewLayerCanvas> getSublayersCanvas() {
		return Collections.unmodifiableList(this.sublayers);
	}

	WindowLayerCanvas getWindowLayer() {
		if(this.superlayer != null) {
			return superlayer.getWindowLayer();
		} else {
			return null;
		}
	}

	public android.content.Context getContext() {
		return this.context;
	}

	public void setView(View view) {
		this.view = view;
	}

	public View getView() {
		return this.view;
	}

	public void setSupportsDrawing(boolean supportsDrawing) {
		this.supportsDrawing = supportsDrawing;
	}

	public boolean clipsToBounds() {
		return clipsToBounds;
	}

	public void setClipsToBounds(boolean clipsToBounds) {
		this.clipsToBounds = clipsToBounds;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.paint.setColor(this.backgroundColor);
	}

	public void setFrame(Rect frame, Rect bounds) {
		this.setFrame(frame, bounds, this.frame == null || frame == null || !this.frame.size.equals(frame.size));
	}

	void setFrame(Rect frame, Rect bounds, boolean setNeedsLayout) {
		this.frame = frame.copy();
		this.bounds = bounds.copy();

		if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}
	}

	public void setBounds(Rect bounds) {
		setBounds(bounds, true);
	}

	void setBounds(Rect bounds, boolean setNeedsLayout) {
		Point oldPoint = this.bounds != null ? this.bounds.origin : null;
		this.bounds = bounds.copy();

		if(oldPoint != null && !oldPoint.equals(this.bounds.origin)) {
			boolean ignoreLayout = pushIgnoreLayout();
			this.view._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		} else if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}

	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public AffineTransform getTransform() {
		return transform;
	}

	public void setTransform(AffineTransform transform) {
		this.transform = transform;
	}

	public boolean isHidden() {
		return this.hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public float getAlpha() {
		return this.alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public void setNeedsDisplay() {
		if(!this.needsDisplay) {
			this.needsDisplay = true;

			WindowLayerCanvas windowLayer = this.getWindowLayer();
			if(windowLayer != null) {
				windowLayer.getNativeView().invalidate();
			}
		}
	}

	public void setNeedsDisplay(Rect dirtyRect) {
		this.setNeedsDisplay();
	}

	public void addSublayer(ViewLayer layer) {
		if(!(layer instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, layer);

		((ViewLayerCanvas)layer).superlayer = this;

		synchronized(this.sublayers) {
			this.sublayers.add((ViewLayerCanvas)layer);
		}

		this.sublayersGeneric.add(layer);
	}

	public void insertSublayerAtIndex(ViewLayer layer, int index) {
		if(!(layer instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, layer);

		((ViewLayerCanvas)layer).superlayer = this;

		synchronized(this.sublayers) {
			this.sublayers.add(index, (ViewLayerCanvas)layer);
		}

		this.sublayersGeneric.add(index, layer);
	}

	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, sibling);

		int index;
		synchronized(this.sublayers) {
			index = this.sublayers.indexOf(sibling);
		}

		this.insertSublayerAtIndex(layer, index > 0 ? index - 1 : 0);
	}

	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerCanvas)) throw new ViewLayer.InvalidSubLayerClassException(this, sibling);

		int index;

		synchronized(this.sublayers) {
			index = this.sublayers.indexOf(sibling);
		}

		this.insertSublayerAtIndex(layer, index + 1);
	}

	public void didMoveToSuperlayer() {
		View superview = this.getView().getSuperview();

		if(superview != null) {
			boolean ignoreLayout = pushIgnoreLayout();
			this.getView()._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		}
	}

	public List<ViewLayer> getSublayers() {
		synchronized(this.sublayers) {
			return Collections.unmodifiableList(this.sublayersGeneric);
		}
	}

	public ViewLayer getSuperlayer() {
		return this.superlayer;
	}

	public void removeFromSuperlayer() {
		if(this.superlayer != null) {
			synchronized(this.sublayers) {
				this.superlayer.sublayers.remove(this);
			}

			this.superlayer.sublayersGeneric.remove(this);
			this.superlayer = null;
		}
	}

	public void renderInContext(mocha.graphics.Context context) {
		this.draw(context.getCanvas());
	}

	public float getZPosition() {
		return 0;
	}

	public void setZPosition(float zPosition) {

	}

	void draw(Canvas canvas) {
		if(this.hidden) return;
		if(this.alpha < 0.01f) return;
		if(this.frame.size.height == 0.0f || this.frame.size.width <= 0.0f) return;

		android.graphics.RectF systemFrame = this.frame.toSystemRectF(this.scale);
		android.graphics.RectF systemBounds = this.bounds.toSystemRectF(this.scale);
		boolean needsAlphaLayer = this.alpha < 1.0f;

		canvas.save();

		if(this.clipsToBounds()) {
			canvas.clipRect(systemFrame);
		}

		canvas.translate((systemFrame.left - systemBounds.left), (systemFrame.top - systemBounds.top));

		if(needsAlphaLayer) {
			canvas.saveLayerAlpha(0, 0, systemFrame.width(), systemFrame.height(), (int)(this.alpha * 255.0f), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
		}

		if(this.backgroundColor != Color.TRANSPARENT && this.paint != null) {
			canvas.drawRect(systemBounds, this.paint);
		}

		if(this.supportsDrawing) {
			if(CACHE_DRAWINGS) {
				if(this.needsDisplay) {
					int bitmapWidth = (int)Math.ceil(systemFrame.width());
					int bitmapHeight = (int)Math.ceil(systemFrame.height());
					boolean reused;

					// Only create a new bitmap if our size changed, otherwise we can reuse the one we have
					if(this.cachedDrawing == null || this.cachedDrawing.getWidth() != bitmapWidth || this.cachedDrawing.getHeight() != bitmapHeight) {
						if(this.cachedDrawing != null) {
							this.cachedDrawing.recycle();
							this.cachedDrawing = null;
						}

						this.cachedDrawing = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
						this.cachedDrawing.setDensity(dpi);
						reused = false;
					} else {
						reused = true;
					}

					Canvas drawCanvas = new Canvas(this.cachedDrawing);

					if(reused) {
						// Clear out old data (in the future, we should check for the dirty rect)
						this.cachedDrawing.eraseColor(Color.TRANSPARENT);
					}

					view.draw(new Context(drawCanvas, scale), this.bounds.copy());
				}

				if(this.cachedDrawing != null) {
					canvas.drawBitmap(
						this.cachedDrawing,
						new android.graphics.Rect(0,0,this.cachedDrawing.getWidth(),this.cachedDrawing.getHeight()),
						new android.graphics.RectF(0.0f, 0.0f, systemFrame.width(), systemFrame.height()),
						null
					);
				}
			} else {
				view.draw(new Context(canvas, scale), this.bounds.copy());
			}
		}

		if(this.sublayers.size() > 0) {
			for(ViewLayerCanvas layer : this.getSublayersCanvas()) {
				if(layer != null) {
					layer.draw(canvas);
				}
			}
		}

		if(needsAlphaLayer) {
			canvas.restore();
		}

		canvas.restore();
		this.needsDisplay = false;
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

	public void setCornerRadius(float cornerRadius) {
		this.cornerRadius = cornerRadius;
	}

	public ViewGroup getViewGroup() {
		return null;
	}
}
