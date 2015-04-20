package mocha.ui;

import android.graphics.Bitmap;
import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Rect;

class SnapshotView extends ImageView {
	private View underlyingView;


	public SnapshotView(View view) {
		super(view.getBounds());
		this.underlyingView = view;
		this.setImage(this.getSnapshotImage());
	}

	public SnapshotView(View view, Rect rect, EdgeInsets capInsets) {
		super(new Rect(0.0f, 0.0f, rect.size.width, rect.size.height));
		this.underlyingView = view;
		throw new IllegalArgumentException("Not yet implemented.");
	}

	private Image getSnapshotImage() {
		Rect bounds = this.underlyingView.getBounds();

		try {
			// TODO: Optimize.

			Context context = new Context(bounds.size, this.underlyingView.scale, Bitmap.Config.ARGB_8888);
			this.underlyingView.getLayer().renderInContext(context);
			return context.getImage();
		} catch (OutOfMemoryError ignored) {

		}

		return null;
	}

	@Override
	public View snapshotView() {
		return this.underlyingView.snapshotView();
	}

	@Override
	public View resizableSnapshotView(Rect rect, EdgeInsets capInsets) {
		return this.underlyingView.resizableSnapshotView(rect, capInsets);
	}


}
