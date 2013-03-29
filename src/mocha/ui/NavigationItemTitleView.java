/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

class NavigationItemTitleView extends View {
	private NavigationItem navigationItem;
	private Font font;
	private Rect textRect;
	private Rect destinationFrame;
	private TextAttributes textAttributes;
	private float verticalPositionAdjustment;

	NavigationItemTitleView(NavigationItem navigationItem, TextAttributes textAttributes, float verticalPositionAdjustment) {
		this.navigationItem = navigationItem;
		this.textAttributes = textAttributes;
		this.verticalPositionAdjustment = verticalPositionAdjustment;
		this.setNeedsDisplay();

		if(this.textAttributes != null && this.textAttributes.font != null) {
			if(this.textAttributes.font.getPointSize() == 0.0f) {
				this.font = this.textAttributes.font.getFontWithSize(20.0f);
			} else {
				this.font = this.textAttributes.font;
			}
		} else {
			this.font = Font.getBoldSystemFontWithSize(20.0f);
		}
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;
		super.setFrame(frame);

		if(!oldSize.equals(frame.size)) {
			this.setNeedsDisplay();
		}
	}

	/**
	 * @return rect text will be drawn into
	 */
	public Rect getTextRect() {
		if(this.textRect != null) {
			return this.textRect.copy();
		} else {
			return Rect.zero();
		}
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
		CharSequence text = this.navigationItem.getTitle();

		if(text == null) {
			this.textRect = null;
			this.setNeedsDisplay();
			return;
		}

		Rect bounds = this.getBounds();
		Size size = TextDrawing.getTextSize(text, this.font, bounds.size);

		Point offset = new Point();
		offset.x = floorf((parentBounds.size.width - size.width) / 2.0f);
		offset.y = floorf((this.destinationFrame.size.height - size.height) / 2.0f);
		offset.x -= this.destinationFrame.origin.x;

		if(offset.x < 0.0f) offset.x = 0.0f;

		this.textRect = new Rect();
		this.textRect.origin = offset;
		this.textRect.size.height = size.height;
		this.textRect.size.width = bounds.size.width - offset.x;
		this.setNeedsDisplay();
	}

	public void draw(Context context, Rect rect) {
		if(this.textRect == null) return;

		if(this.textAttributes != null && this.textAttributes.textColor != 0) {
			context.setFillColor(this.textAttributes.textColor);
		} else {
			context.setFillColor(Color.WHITE);
		}

		if(this.textAttributes != null && this.textAttributes.shadowOffset != null) {
			context.setShadow(this.textAttributes.shadowOffset, 0.0f, this.textAttributes.shadowColor);
		} else {
			context.setShadow(0.0f, -1.0f, 0.0f, Color.white(0.0f, 0.5f));
		}

		TextDrawing.draw(context, this.navigationItem.getTitle(), textRect, this.font);
	}
}
