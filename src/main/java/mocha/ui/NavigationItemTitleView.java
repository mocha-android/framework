/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

class NavigationItemTitleView extends View {
	private NavigationItem navigationItem;
	private Rect destinationFrame;
	private float verticalPositionAdjustment;
	private Label label;
	private NavigationBar.TitleAlignment titleAlignment;

	NavigationItemTitleView(Rect frame, NavigationItem navigationItem, TextAttributes textAttributes, float verticalPositionAdjustment) {
		super(frame);

		this.navigationItem = navigationItem;
		this.verticalPositionAdjustment = verticalPositionAdjustment;
		this.setNeedsDisplay();

		Font font;

		if(textAttributes != null && textAttributes.font != null) {
			if(textAttributes.font.getPointSize() == 0.0f) {
				font = textAttributes.font.getFontWithSize(20.0f);
			} else {
				font = textAttributes.font;
			}
		} else {
			font = Font.getBoldSystemFontWithSize(20.0f);
		}

		this.label = new Label();
		this.label.setFont(font);
		this.label.setText(this.navigationItem.getTitle());
		this.label.setTextAlignment(TextAlignment.LEFT);
		this.label.setLineBreakMode(LineBreakMode.TRUNCATING_MIDDLE);
		this.label.setBackgroundColor(Color.TRANSPARENT	);

		if(textAttributes != null && textAttributes.textColor != 0) {
			this.label.setTextColor(textAttributes.textColor);
		} else {
			this.label.setTextColor(Color.WHITE);
		}

		if(textAttributes != null && textAttributes.shadowOffset != null) {
			this.label.setShadowOffset(textAttributes.shadowOffset);
			this.label.setShadowColor(textAttributes.shadowColor);
		} else {
			this.label.setShadowOffset(new Size(0.0f, -1.0f));
			this.label.setShadowColor(Color.white(0.0f, 0.5f));
		}

		this.addSubview(this.label);
	}

	public void setTitleAlignment(NavigationBar.TitleAlignment titleAlignment) {
		this.titleAlignment = titleAlignment;
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;
		super.setFrame(frame);

		/*if(!oldSize.equals(frame.size)) {
			this.setNeedsLayout();
		}*/
	}

	public Size sizeThatFits(Size size) {
		String title = this.navigationItem.getTitle();

		if(title != null && title.length() > 0) {
			return this.label.sizeThatFits(size);
		} else {
			return Size.zero();
		}
	}

	/**
	 * @return rect text will be drawn into
	 */
	public Rect getTextRect() {
		return this.label.getFrame();
	}

	/**
	 * Set the destination frame when it's actually on the navigation
	 * bar and not animating
	 *
	 * @param frame final frame
	 * @param parentBounds bounds for superview
	 */
	public void setDestinationFrame(Rect frame, Rect parentBounds) {
		this.destinationFrame = frame;
		this.updateTextRect(parentBounds);
	}

	private void updateTextRect(Rect parentBounds) {
		Rect bounds = this.getBounds();
		Size size = this.label.sizeThatFits(bounds.size);

		Point offset = new Point();
		offset.y = floorf((this.destinationFrame.size.height - size.height) / 2.0f) + this.verticalPositionAdjustment;

		switch (this.titleAlignment) {
			case LEFT:
				offset.x = 0.0f;
				break;
			case CENTER:
				offset.x = floorf((parentBounds.size.width - size.width) / 2.0f);
				offset.x -= this.destinationFrame.origin.x;
				break;
			case RIGHT:
				offset.x = bounds.size.width - size.width;
				break;
		}

		if(offset.x < 0.0f) offset.x = 0.0f;

		Rect frame = new Rect();
		frame.origin = offset;
		frame.size.height = size.height;
		frame.size.width = bounds.size.width - offset.x;
		this.label.setFrame(frame);
	}

}
