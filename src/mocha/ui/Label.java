/*
 *  @author Shaun
 *	@date 11/14/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.graphics.Color;
import mocha.graphics.*;

public class Label extends View implements Highlightable {


	private CharSequence text;
	private Font font;
	private int textColor;
	private int highlightedTextColor;
	private int shadowColor;
	private Size shadowOffset;
	private TextAlignment textAlignment;
	private LineBreakMode lineBreakMode;
	private boolean enabled;
	private int numberOfLines;
	private boolean highlighted;
	private boolean textNeedsMeasuring;
	private Size lastSize;
	private Size textSize;

	public Label() { }
	public Label(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.setUserInteractionEnabled(false);
		this.textAlignment = TextAlignment.LEFT;
		this.lineBreakMode = LineBreakMode.TRUNCATING_TAIL;
		this.textColor = Color.BLACK;
		this.setBackgroundColor(Color.WHITE);
		this.enabled = true;
		this.font = Font.getSystemFontWithSize(17.0f);
		this.numberOfLines = 1;
		this.setClipsToBounds(true);
		this.shadowOffset = new Size(0.0f, -1.0f);
	}

	public CharSequence getText() {
		return text == null ? "" : text;
	}

	public void setText(CharSequence text) {
		if(this.text != text) {
			this.text = text;
			this.textNeedsMeasuring = true;
			this.setNeedsDisplay();
		}
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		if(this.font != font) {
			this.font = font;
			this.textNeedsMeasuring = true;
			this.setNeedsDisplay();
		}
	}

	public int getTextColor() {
		return textColor;
	}

	public void setTextColor(int textColor) {
		if(this.textColor != textColor) {
			this.textColor = textColor;
			this.setNeedsDisplay();
		}
	}

	public int getHighlightedTextColor() {
		return highlightedTextColor;
	}

	public void setHighlightedTextColor(int highlightedTextColor) {
		this.highlightedTextColor = highlightedTextColor;
		this.setNeedsDisplay();
	}

	public int getShadowColor() {
		return shadowColor;
	}

	public void setShadowColor(int shadowColor) {
		if(this.shadowColor != shadowColor) {
			this.shadowColor = shadowColor;
			this.setNeedsDisplay();
		}
	}

	public Size getShadowOffset() {
		return shadowOffset;
	}

	public void setShadowOffset(Size shadowOffset) {
		if(shadowOffset == null || !shadowOffset.equals(this.shadowOffset)) {
			this.shadowOffset = shadowOffset;
			this.setNeedsDisplay();
		}
	}

	public void setShadowOffset(Offset shadowOffset) {
		this.setShadowOffset(shadowOffset.toSize());
	}

	public TextAlignment getTextAlignment() {
		return textAlignment;
	}

	public void setTextAlignment(TextAlignment textAlignment) {
		if(this.textAlignment != textAlignment) {
			this.textAlignment = textAlignment;
			this.setNeedsDisplay();
		}
	}

	public LineBreakMode getLineBreakMode() {
		return lineBreakMode;
	}

	public void setLineBreakMode(LineBreakMode lineBreakMode) {
		if(this.lineBreakMode != lineBreakMode) {
			this.lineBreakMode = lineBreakMode;
			this.textNeedsMeasuring = true;
			this.setNeedsDisplay();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if(this.enabled != enabled) {
			this.enabled = enabled;
			this.setNeedsDisplay();
		}
	}

	public int getNumberOfLines() {
		return numberOfLines;
	}

	public void setNumberOfLines(int numberOfLines) {
		if(this.numberOfLines != numberOfLines) {
			this.numberOfLines = numberOfLines;
			this.textNeedsMeasuring = true;
			this.setNeedsDisplay();
		}
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		if(this.highlighted != highlighted) {
			this.highlighted = highlighted;
			this.setNeedsDisplay();
		}
	}

	public void setFrame(Rect frame) {
		Rect oldFrame = this.getFrame();
		super.setFrame(frame);

		if(!this.getFrame().size.equals(oldFrame.size)) {
			this.setNeedsDisplay();
		}
	}

	public Rect getTextRectForBound(Rect bounds, int numberOfLines) {
		if(this.text != null && this.text.length() > 0) {
			Size maxSize = new Size(bounds.size);

			if (numberOfLines > 0) {
				maxSize.height = this.font.getLineHeight() * numberOfLines;
			}

			return new Rect(bounds.origin, TextDrawing.getTextSize(this.text, this.font, maxSize, this.lineBreakMode));
		}

		return new Rect(bounds.origin.x, bounds.origin.y, 0.0f, 0.0f);
	}

	public void drawTextInRect(mocha.graphics.Context context, Rect rect) {
		context.save();
		context.setShadow(this.shadowOffset, 0.0f, this.shadowColor);
		context.setFillColor(this.highlighted && this.highlightedTextColor != 0 ? this.highlightedTextColor : this.textColor);
		TextDrawing.draw(context, text, rect, this.font, this.textAlignment, this.lineBreakMode);
		context.restore();
	}

	public void draw(Context context, Rect rect) {
		if(this.text == null || this.text.length() == 0) return;

		Rect bounds = this.getBounds();
		Rect drawRect = Rect.zero();

		if(lastSize == null || this.textNeedsMeasuring || bounds.size.equals(lastSize)) {
			Size maxSize = bounds.size.copy();

			if (this.numberOfLines > 0) {
				maxSize.height = this.font.getLineHeight() * this.numberOfLines;
			}

			this.textSize = TextDrawing.getTextSize(this.text, this.font, maxSize, this.lineBreakMode);
			lastSize = bounds.size.copy();
			this.textNeedsMeasuring = false;
		}

		drawRect.size = this.textSize;
		drawRect.origin.y = roundf((bounds.size.height - drawRect.size.height) / 2.0f);
		drawRect.origin.x = 0;
		drawRect.size.width = bounds.size.width;

		this.drawTextInRect(context, drawRect);
	}
}
