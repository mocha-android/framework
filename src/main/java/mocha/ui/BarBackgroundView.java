/*
 *	@author Shaun
 *	@date 3/15/15
 *	@copyright	2015 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

class BarBackgroundView extends ImageView {
	private final BarMetricsStorage<Image> images;

	BarBackgroundView(Rect frame) {
		super(frame);

		this.images = new BarMetricsStorage<>();
	}

	BarMetricsStorage<Image> getImages() {
		return images;
	}

	void update(BarMetrics metrics) {
		this.setImage(this.images.get(metrics));
		this.setHidden(this.getImage() == null);
	}

}
