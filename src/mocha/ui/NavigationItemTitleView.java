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

	NavigationItemTitleView(NavigationItem navigationItem) {
		this.navigationItem = navigationItem;
		this.font = Font.getBoldSystemFontWithSize(20.0f);
		this.setNeedsDisplay();
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
		return this.textRect.copy();
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

		context.setFillColor(Color.WHITE);
		context.setShadow(new Size(0.0f, -1.0f), 0.0f, Color.white(0.0f, 0.5f));

		TextDrawing.draw(context, this.navigationItem.getTitle(), textRect, this.font);
	}
}
