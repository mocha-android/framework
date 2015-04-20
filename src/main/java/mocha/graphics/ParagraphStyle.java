package mocha.graphics;

public class ParagraphStyle {
	private float lineSpacing;
	private float firstLineHeadIndent;
	private TextAlignment alignment;

	public ParagraphStyle() {
		this.lineSpacing = Float.MIN_NORMAL;
		this.firstLineHeadIndent = Float.MIN_NORMAL;
		this.alignment = null;
	}

	public ParagraphStyle(TextAlignment alignment) {
		this();

		this.alignment = alignment;
	}

	public float getLineSpacing() {
		return lineSpacing;
	}

	public void setLineSpacing(float lineSpacing) {
		this.lineSpacing = lineSpacing;
	}

	public float getFirstLineHeadIndent() {
		return firstLineHeadIndent;
	}

	public void setFirstLineHeadIndent(float firstLineHeadIndent) {
		this.firstLineHeadIndent = firstLineHeadIndent;
	}

	public TextAlignment getAlignment() {
		return alignment;
	}

	public void setAlignment(TextAlignment alignment) {
		this.alignment = alignment;
	}

}
