/**
 *  @author Shaun
 *  @date 4/6/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.graphics;

import android.text.Layout;
import android.text.TextPaint;

/**
 * Used internally by mocha.ui and mocha.graphics to cache
 * it's layout instances.  Usage outside of this is not recommended
 * as you need to be absolutely sure you invalidate the layout
 * when appropriate.
 *
 * @hide
 */
public class TextDrawingText implements CharSequence {
	private CharSequence text;
	private Layout layout;
	private TextPaint paint;

	public TextDrawingText(CharSequence text) {
		this.text = text;
	}

	public CharSequence getText() {
		return text;
	}

	public TextPaint getPaint() {
		return paint;
	}

	Layout getLayout() {
		return layout;
	}

	void setLayout(Layout layout, TextPaint paint) {
		this.layout = layout;
		this.paint = paint;
	}

	public void invalidate() {
		this.layout = null;
	}

	public int length() {
		return this.text.length();
	}

	public char charAt(int i) {
		return this.text.charAt(i);
	}

	public CharSequence subSequence(int i, int i2) {
		return this.text.subSequence(i, i2);
	}

}
