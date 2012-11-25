/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import mocha.graphics.Point;
import mocha.graphics.Rect;

public class ViewLayer extends ViewGroup {
	private static boolean ignoreLayout;

	private Rect frame;
	private Rect bounds;
	private View view;
	public final float scale;

	public ViewLayer(Context context) {
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

	void setView(View view) {
		this.view = view;
	}

	protected View getView() {
		return this.view;
	}

	protected Rect getFrame() {
		return frame;
	}

	protected void setFrame(Rect frame, Rect bounds) {
		this.setFrame(frame, bounds, true);
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

	void didMoveToSuperview() {
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

	protected Rect getBounds() {
		return bounds;
	}

	protected void setBounds(Rect bounds) {
		this.setBounds(bounds, true);
	}

	void setBounds(Rect bounds, boolean setNeedsLayout) {
		Point oldPoint = this.bounds != null ? this.bounds.origin : null;
		this.bounds = bounds.getScaledRect(scale);

		if(oldPoint != null && !oldPoint.equals(this.bounds.origin)) {
			boolean ignoreLayout = pushIgnoreLayout();
			for(View subview : this.view.getSubviews()) {
				subview.getLayer().layoutRelativeToBounds(this.bounds);
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

}
