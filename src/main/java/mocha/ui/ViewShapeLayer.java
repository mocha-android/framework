package mocha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import mocha.graphics.Path;

public class ViewShapeLayer extends ViewLayerNative {

	private Paint pathPaint;
	private Path path;
	private android.graphics.Path nativePath;

	public ViewShapeLayer(Context context) {
		super(context);
	}


	@Override
	protected boolean useLayoutBackgroundColor() {
		return super.useLayoutBackgroundColor() && this.path == null;
	}

	@Override
	protected void drawBackground(Canvas canvas, RectF rect, Paint borderPaint, boolean alwaysDraw) {
		if (this.path != null) {
			if (this.pathPaint == null) {
				this.pathPaint = new Paint();
				this.pathPaint.setAntiAlias(true);
				this.pathPaint.setDither(true);
			}

			this.pathPaint.setColor(this.getBackgroundColor());

			canvas.drawPath(this.getNativePath(), this.pathPaint);

			if (borderPaint != null) {
				canvas.drawPath(this.getNativePath(), borderPaint);
			}
		} else {
			super.drawBackground(canvas, rect, borderPaint, alwaysDraw);
		}
	}

	public Path getPath() {
		return this.path;
	}

	public void setPath(Path path) {
		this.path = path;
		this.nativePath = null;
	}

	private android.graphics.Path getNativePath() {
		if (this.path != null) {
			if (this.nativePath == null) {
				this.nativePath = this.path.getScaledNativePath(this.scale);
			}

			return this.nativePath;
		} else {
			return null;
		}
	}

}
