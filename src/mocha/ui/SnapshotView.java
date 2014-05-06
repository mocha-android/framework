/**
 *  @author Shaun
 *  @date 4/22/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

class SnapshotView extends ImageView {
	private View underlyingView;

	public SnapshotView(View view) {
		super(view.getBounds());
		this.underlyingView = view;
	}

	public SnapshotView(View view, Rect rect, EdgeInsets capInsets) {
		super(new Rect(0.0f, 0.0f, rect.size.width, rect.size.height));
		this.underlyingView = view;
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
