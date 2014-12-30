/**
 *  @author Shaun
 *  @date 2/12/13
 *  @copyright	2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Offset;

public final class TextAttributes {
	/**
	 * Font to be used, if 0.0 point size is used, the default font size will be used.
	 */
	public final Font font;

	public final int textColor;
	public final int shadowColor;
	public final Offset shadowOffset;

	public static TextAttributes make(Font font) {
		return new TextAttributes(font, 0, 0, null);
	}

	public static TextAttributes make(Font font, int textColor) {
		return new TextAttributes(font, textColor, 0, null);
	}

	public static TextAttributes make(int textColor) {
		return new TextAttributes(null, textColor, 0, null);
	}

	public static TextAttributes make(int shadowColor, Offset shadowOffset) {
		return new TextAttributes(null, 0, shadowColor, shadowOffset);
	}

	public static TextAttributes make(Font font, int shadowColor, Offset shadowOffset) {
		return new TextAttributes(font, 0, shadowColor, shadowOffset);
	}

	public static TextAttributes make(int textColor, int shadowColor, Offset shadowOffset) {
		return new TextAttributes(null, textColor, shadowColor, shadowOffset);
	}

	public static TextAttributes make(Font font, int textColor, int shadowColor, Offset shadowOffset) {
		return new TextAttributes(font, textColor, shadowColor, shadowOffset);
	}

	private TextAttributes(Font font, int textColor, int shadowColor, Offset shadowOffset) {
		this.font = font;
		this.textColor = textColor;
		this.shadowColor = shadowColor;
		this.shadowOffset = shadowOffset;
	}

}
