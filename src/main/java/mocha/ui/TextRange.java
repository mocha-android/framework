package mocha.ui;

public class TextRange {
	private int start;
	private int end;

	TextRange(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public boolean isEmpty() {
		return this.end - this.start <= 0;
	}

}
