/**
 *	@author Shaun
 *	@date 3/15/15
 *	@copyright 2015 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.graphics.*;
import mocha.graphics.Gradient;
import mocha.graphics.Path;
import mocha.graphics.Point;

import java.util.Arrays;
import java.util.List;

public class ViewGradientLayer extends ViewLayerNative {

	private final Point startPoint;
	private final Point endPoint;

	private int[] colors;
	private float[] locations;

	private LinearGradient linearGradient;
	private final Paint gradientPaint;

	public ViewGradientLayer(Context context) {
		super(context);

		this.gradientPaint = new Paint();
		this.gradientPaint.setAntiAlias(true);
		this.gradientPaint.setDither(true);

		this.startPoint = new Point(0.5f, 0.0f);
		this.endPoint = new Point(0.5f, 1.0f);

		this.locations = new float[] { 0.0f, 1.0f };
	}

	@Override
	protected boolean useLayoutBackgroundColor() {
		return super.useLayoutBackgroundColor() && !(this.colors != null && this.locations != null);
	}

	@Override
	protected void drawBackground(Canvas canvas, RectF rect, Paint borderPaint, boolean alwaysDraw) {
		if(this.colors != null && this.locations != null) {
			if(this.linearGradient == null) {
				float width = rect.width();
				float height = rect.height();

				this.linearGradient = new LinearGradient(this.startPoint.x * width, this.startPoint.y * height, this.endPoint.x * width, this.endPoint.y * height, this.colors, this.locations, Shader.TileMode.CLAMP);
				this.gradientPaint.setShader(this.linearGradient);
			}


			android.graphics.Path cornerPath = this.getCornerNativePath();

			if(cornerPath != null) {
				canvas.drawPath(cornerPath, this.gradientPaint);
			} else {
				canvas.drawPaint(this.gradientPaint);
			}

			if(borderPaint != null) {
				if(cornerPath != null) {
					canvas.drawPath(cornerPath, borderPaint);
				} else {
					canvas.drawPaint(borderPaint);
				}
			}
		} else {
			super.drawBackground(canvas, rect, borderPaint, alwaysDraw);
		}
	}

	public int[] getColors() {
		if(this.colors != null) {
			return Arrays.copyOf(this.colors, this.colors.length);
		} else {
			return null;
		}
	}

	public void setColors(int... colors) {
		this.colors = colors;
		this.invalidateGradient();
	}

	public void setColors(List<Integer >colors) {
		if(colors != null) {
			this.colors = new int[colors.size()];

			for(int i = 0; i < this.colors.length; i++) {
				this.colors[i] = colors.get(i);
			}
		} else {
			this.colors = null;
		}

		this.invalidateGradient();
	}

	public float[] getLocations() {
		if(this.locations != null) {
			return Arrays.copyOf(this.locations, this.locations.length);
		} else {
			return null;
		}
	}

	public void setLocations(float... locations) {
		this.locations = locations;
		this.invalidateGradient();
	}

	public void setLocations(List<Float> locations) {
		if(locations != null) {
			this.locations = new float[locations.size()];

			for(int i = 0; i < this.locations.length; i++) {
				this.locations[i] = locations.get(i);
			}
		} else {
			this.locations = null;
		}

		this.invalidateGradient();
	}

	private void invalidateGradient() {
		this.linearGradient = null;
		this.setNeedsDisplay();
	}

	@Override
	protected void onUpdateSize(float nativeWidth, float nativeHeight) {
		super.onUpdateSize(nativeWidth, nativeHeight);
		this.invalidateGradient();
	}

	public Point getStartPoint() {
		return this.startPoint.copy();
	}

	public void setStartPoint(Point startPoint) {
		this.startPoint.set(startPoint);
		this.invalidateGradient();
	}

	public Point getEndPoint() {
		return this.endPoint.copy();
	}

	public void setEndPoint(Point endPoint) {
		this.endPoint.set(endPoint);
		this.invalidateGradient();
	}

}
