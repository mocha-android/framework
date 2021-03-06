package mocha.ui;

import android.graphics.Color;
import mocha.graphics.*;

public class Label extends View implements Highlightable {

	private TextDrawingText text;
	private AttributedString attributedText;
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

	public Label() {
	}

	public Label(Rect frame) {
		super(frame);
	}

	@Override
	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.setUserInteractionEnabled(false);
		this.textAlignment = TextAlignment.LEFT;
		this.lineBreakMode = LineBreakMode.TRUNCATING_TAIL;
		this.textColor = Color.BLACK;
		this.setBackgroundColor(Color.TRANSPARENT);
		this.enabled = true;
		this.numberOfLines = 1;
		this.setClipsToBounds(true);
		this.shadowOffset = new Size(0.0f, -1.0f);
	}

	public CharSequence getText() {
		if (this.attributedText != null) {
			return this.attributedText.toString();
		} else {
			return this.text == null || this.text.getText() == null ? "" : this.text.getText();
		}
	}

	public void setText(CharSequence text) {
		if ((this.text == null && text != null) || (this.text != null && this.text.getText() != text)) {
			if (text == null) {
				this.text = null;
			} else {
				this.text = new TextDrawingText(text);
			}

			this.attributedText = null;
			this.setTextNeedsMeasuring();
		}
	}

	public AttributedString getAttributedText() {
		if (this.attributedText == null) {
			return new AttributedString();
		} else {
			return this.attributedText.copy();
		}
	}

	public void setAttributedText(AttributedString attributedText) {
		if (attributedText == null) {
			this.attributedText = null;
		} else {
			this.attributedText = attributedText.copy();
			this.text = null;
		}

		this.setTextNeedsMeasuring();
	}

	public Font getFont() {
		if (this.font == null) {
			this.font = Font.getSystemFontWithSize(17.0f);
		}

		return this.font;
	}

	public void setFont(Font font) {
		if (this.font != font) {
			this.font = font;
			this.setTextNeedsMeasuring();
		}
	}

	public int getTextColor() {
		return textColor;
	}

	public void setTextColor(int textColor) {
		if (this.textColor != textColor) {
			this.textColor = textColor;
			if (this.text != null) this.text.invalidate();
			this.setNeedsDisplay();
		}
	}

	public int getHighlightedTextColor() {
		return highlightedTextColor;
	}

	public void setHighlightedTextColor(int highlightedTextColor) {
		this.highlightedTextColor = highlightedTextColor;
		if (this.text != null) this.text.invalidate();
		this.setNeedsDisplay();
	}

	public int getShadowColor() {
		return shadowColor;
	}

	public void setShadowColor(int shadowColor) {
		if (this.shadowColor != shadowColor) {
			this.shadowColor = shadowColor;
			this.setNeedsDisplay();
		}
	}

	public Size getShadowOffset() {
		return shadowOffset;
	}

	public void setShadowOffset(Size shadowOffset) {
		if (shadowOffset == null || !shadowOffset.equals(this.shadowOffset)) {
			this.shadowOffset = shadowOffset == null ? null : shadowOffset.copy();
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
		if (this.textAlignment != textAlignment) {
			this.textAlignment = textAlignment;
			this.setTextNeedsMeasuring();
		}
	}

	public LineBreakMode getLineBreakMode() {
		return lineBreakMode;
	}

	public void setLineBreakMode(LineBreakMode lineBreakMode) {
		if (this.lineBreakMode != lineBreakMode) {
			this.lineBreakMode = lineBreakMode;
			this.setTextNeedsMeasuring();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			this.setNeedsDisplay();
		}
	}

	public int getNumberOfLines() {
		return numberOfLines;
	}

	public void setNumberOfLines(int numberOfLines) {
		if (this.numberOfLines != numberOfLines) {
			this.numberOfLines = numberOfLines;
			this.setTextNeedsMeasuring();
		}
	}

	@Override
	public boolean isHighlighted() {
		return highlighted;
	}

	@Override
	public void setHighlighted(boolean highlighted) {
		if (this.highlighted != highlighted) {
			this.highlighted = highlighted;
			if (this.text != null) this.text.invalidate();
			this.setNeedsDisplay();
		}
	}

	@Override
	public void setFrame(Rect frame) {
		super.setFrame(frame);

		if (this.lastSize == null || !this.getFrame().size.equals(this.lastSize)) {
			this.setTextNeedsMeasuring();
		}
	}

	public Rect getTextRectForBound(Rect bounds, int numberOfLines) {
		if (this.attributedText != null && this.attributedText.length() > 0) {
			return new Rect(bounds.origin, this.attributedText.getBoundingRectWithSize(bounds.size, this.textAlignment).size);
		} else if (this.text != null && this.text.length() > 0) {
			Size maxSize = new Size(bounds.size);

			if (numberOfLines > 0) {
				maxSize.height = this.getFont().getLineHeight() * numberOfLines;
			}

			return new Rect(bounds.origin, TextDrawing.getTextSize(this.text, this.getFont(), maxSize, this.lineBreakMode));
		}

		return new Rect(bounds.origin.x, bounds.origin.y, 0.0f, 0.0f);
	}

	public Size sizeThatFits(Size size) {
		Size textSize;

		if (this.attributedText != null && this.attributedText.length() > 0) {
			textSize = this.attributedText.getBoundingRectWithSize(size, this.textAlignment).size;
			textSize.width = ceilf(textSize.width);
			textSize.height = ceilf(textSize.height);
		} else {
			if (this.text == null || this.text.length() == 0) {
				textSize = Size.zero();
			} else {
				if (size.width != this.getFrame().size.width) {
					this.text.invalidate();
				}

				textSize = TextDrawing.getTextSize(this.text, this.getFont(), size, this.lineBreakMode);
			}
		}

		// This seems odd, but generally if sizeThatFits is called, it's followed up
		// with a call to setFrame(), and this allows us to use the cached layout we
		// just created vs creating a new one.
		this.lastSize = Size.min(textSize, size);
		this.setNeedsDisplay();

		return this.lastSize.copy();
	}

	private void setTextNeedsMeasuring() {
		if (this.text != null) {
			this.text.invalidate();
		}

		this.textNeedsMeasuring = true;
		this.setNeedsDisplay();
	}

	public void drawTextInRect(mocha.graphics.Context context, Rect rect) {
		if ((this.text == null || this.text.length() == 0) && (this.attributedText == null || this.attributedText.length() == 0))
			return;

		context.save();
		context.setFillColor(this.highlighted && this.highlightedTextColor != 0 ? this.highlightedTextColor : this.textColor);

		if (this.attributedText != null && this.attributedText.length() > 0) {
			this.attributedText.draw(context, rect, this.textAlignment);
		} else {
			context.setShadow(this.shadowOffset, 0.0f, this.shadowColor);
			TextDrawing.draw(context, this.text, rect, this.getFont(), this.textAlignment, this.lineBreakMode);
		}

		context.restore();
	}

	@Override
	public void draw(Context context, Rect rect) {
		if ((this.attributedText == null || this.attributedText.length() == 0) && (this.text == null || this.text.length() == 0))
			return;

		Rect bounds = this.getBounds();
		Rect drawRect = Rect.zero();

		if (this.lastSize == null || this.textNeedsMeasuring || !bounds.size.equals(lastSize)) {
			Size maxSize = bounds.size.copy();

			if (this.numberOfLines > 0) {
				maxSize.height = this.getFont().getLineHeight() * this.numberOfLines;
			}

			if (this.attributedText != null && this.attributedText.length() > 0) {
				this.textSize = this.attributedText.getBoundingRectWithSize(maxSize, this.textAlignment).size;
				this.textSize.width = ceilf(textSize.width);
				this.textSize.height = ceilf(textSize.height);
			} else {
				this.textSize = TextDrawing.getTextSize(this.text, this.getFont(), maxSize, this.lineBreakMode);
			}

			this.lastSize = bounds.size.copy();
			this.textNeedsMeasuring = false;
		}

		drawRect.size = this.textSize;
		drawRect.origin.y = roundf((bounds.size.height - drawRect.size.height) / 2.0f);
		drawRect.origin.x = 0;
		drawRect.size.width = bounds.size.width;

		this.drawTextInRect(context, drawRect);
	}

	boolean getOverridesDraw() {
		return true;
	}

}
