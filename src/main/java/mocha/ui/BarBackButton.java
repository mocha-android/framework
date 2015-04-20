package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;
import mocha.graphics.Size;

class BarBackButton extends Control {
	private float imageHeight;
	private final EdgeInsets contentInset;
	private final ImageView backIndicatorImageView;

	public BarBackButton(NavigationBar navigationBar, NavigationItem navigationItem) {
		super();

		Image backIndicatorImage = navigationBar.getBackIndicatorImage();

		if (backIndicatorImage == null) {
			backIndicatorImage = Image.imageNamed(R.drawable.mocha_navigation_back_indicator);
		}

		if (backIndicatorImage.getRenderingMode() == Image.RenderingMode.AUTOMATIC) {
			backIndicatorImage = backIndicatorImage.imageWithRenderingMode(Image.RenderingMode.ALWAYS_TEMPLATE);
		}

		this.imageHeight = backIndicatorImage.getHeight();

		this.backIndicatorImageView = new ImageView(backIndicatorImage);
		this.backIndicatorImageView.setFrame(new Rect(0.0f, 0.0f, backIndicatorImage.getWidth(), this.imageHeight));
		this.backIndicatorImageView.setContentMode(ContentMode.CENTER);
		this.addSubview(backIndicatorImageView);

		this.setFrame(new Rect(backIndicatorImage.getSize()));
		this.contentInset = navigationBar.getBackIndicatorContentInset().copy();
	}


	@Override
	public void layoutSubviews() {
		super.layoutSubviews();

		Rect backIndicatorImageViewRect = this.backIndicatorImageView.getFrame();
		backIndicatorImageViewRect.origin.y = 0.0f;
		backIndicatorImageViewRect.origin.x = this.contentInset.left;
		backIndicatorImageViewRect.size.height = this.getBoundsHeight();
		this.backIndicatorImageView.setFrame(backIndicatorImageViewRect);
	}

	public EdgeInsets getContentInset() {
		return this.contentInset.copy();
	}

	public void setContentInset(EdgeInsets contentInset) {
		this.contentInset.set(contentInset);
	}

	@Override
	public Size sizeThatFits(Size size) {
		Size sizeThatFits = size.copy();
		sizeThatFits.width = this.backIndicatorImageView.getFrameWidth() + this.contentInset.left + this.contentInset.right;
		sizeThatFits.height = Math.max(sizeThatFits.height, this.imageHeight);

		return sizeThatFits;
	}

}
