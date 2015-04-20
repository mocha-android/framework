package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Rect;

public class TableViewHeaderFooterView extends TableViewSubview {
	private static final float LABEL_INSET = 10.0f;

	private Label textLabel;
	private View contentView;
	private View backgroundView;
	private String reuseIdentifier;

	public TableViewHeaderFooterView(String reuseIdentifier) {
		super(new Rect(0.0f, 0.0f, 320.0f, 30.0f));

		Rect bounds = this.getBounds();

		this.contentView = new View(bounds);
		this.addSubview(this.contentView);

		this.textLabel = new Label(bounds.inset(LABEL_INSET, 0.0f));
		this.textLabel.setFont(Font.getBoldSystemFontWithSize(14.0f));
		this.contentView.addSubview(this.textLabel);

		this.reuseIdentifier = reuseIdentifier;
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		Rect bounds = this.getBounds();

		if (this.backgroundView != null) {
			this.backgroundView.setFrame(this.getBounds());
		}

		this.contentView.setFrame(bounds);
		this.textLabel.setFrame(bounds.inset(LABEL_INSET, 0.0f));
	}

	public void prepareForReuse() {

	}

	public Label getTextLabel() {
		return this.textLabel;
	}

	public View getContentView() {
		return this.contentView;
	}

	public View getBackgroundView() {
		return this.backgroundView;
	}

	public void setBackgroundView(View backgroundView) {
		if (this.backgroundView != null) {
			this.backgroundView.removeFromSuperview();
		}

		this.backgroundView = backgroundView;

		if (this.backgroundView != null) {
			this.insertSubview(this.backgroundView, 0);
		}

		this.setNeedsLayout();
	}

	public String getReuseIdentifier() {
		return reuseIdentifier;
	}

	void setReuseIdentifier(String reuseIdentifier) {
		this.reuseIdentifier = reuseIdentifier;
	}
}
