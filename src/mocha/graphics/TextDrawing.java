/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import mocha.foundation.*;
import mocha.ui.Screen;

import java.util.Arrays;

public class TextDrawing extends mocha.foundation.Object {

	public static Size draw(Context context, CharSequence text, Rect rect, Font font) {
		return draw(context, text, rect, font, TextAlignment.LEFT);
	}

	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment) {
		return draw(context, text, rect, font, TextAlignment.LEFT, LineBreakMode.WORD_WRAPPING);
	}

	// TODO: Implement line break mode
	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment, LineBreakMode lineBreakMode) {
		float scale = context.getScale();

		TextPaint textPaint = context.getTextPaint();
		textPaint.setTypeface(font.getTypeface());
		textPaint.setTextSize(font.getPointSize() * scale);


		text = fitToWidth(context, text, rect.size.width, textPaint);
		float textWidth = textPaint.measureText(text, 0, text.length());
		rect = rect.getScaledRect(scale);

		float x;

		if(textAlignment == TextAlignment.CENTER) {
			x = rect.origin.x + ((rect.size.width - textWidth) / 2);
		} else if(textAlignment == TextAlignment.RIGHT) {
			x = rect.origin.x + (rect.size.width - textWidth);
		} else {
			x = rect.origin.x;
		}

		float y = rect.origin.y + (rect.size.height / 2.0f) + ((font.getLineHeight() * scale) / 2.0f) - scale;

		context.getCanvas().drawText(text, 0, text.length(), x, y, textPaint);

		return new Size(textWidth / scale, font.getLineHeight());
	}

	public static CharSequence fitToRect(Context context, CharSequence text, Rect targetRect, Font font) {
		return fitToWidth(context, text, targetRect.size.width, createPaintForFont(font, context.getScale()));
	}

	private static CharSequence fitToWidth(Context context, CharSequence text, float width, TextPaint paint) {
		text = TextUtils.ellipsize(text, paint, width * context.getScale(), TextUtils.TruncateAt.END);

		if(text == null) {
			return null;
		} else {
			if(text instanceof String) {
				return ((String)text).replaceAll("( |\\.|\\|_|\\-)+…$", "…");
			} else {
				return text;
			}
		}
	}

	public static float getTextWidth(CharSequence text, Font font) {
		return getTextWidth(text, font, Float.MAX_VALUE, Screen.mainScreen().getScale());
	}

	public static float getTextWidth(CharSequence text, Font font, float width) {
		return getTextWidth(text, font, width, Screen.mainScreen().getScale());
	}

	public static float getTextWidth(CharSequence text, Font font, float width, float screenScale) {
		width = constrainWidth(width * screenScale);
		float[] measuredWidth = new float[] { 0.0f };

		font.paintForScreenScale(screenScale).breakText(text, 0, text.length(), true, width, measuredWidth);
		return measuredWidth[0] / screenScale;
	}

	public static Size getTextSize(CharSequence text, Font font) {
		return getTextSize(text, font, new Size(Float.MAX_VALUE, Float.MAX_VALUE), LineBreakMode.WORD_WRAPPING);
	}

	public static Size getTextSize(CharSequence text, Font font, Size constrainedToSize) {
		return getTextSize(text, font, constrainedToSize, LineBreakMode.WORD_WRAPPING);
	}

	public static Size getTextSize(CharSequence text, Font font, Size constrainedToSize, LineBreakMode lineBreakMode) {
		return getTextSize(text, font, constrainedToSize, lineBreakMode, Screen.mainScreen().getScale());
	}

	public static Size getTextSize(CharSequence text, Font font, Size constrainedToSize, LineBreakMode lineBreakMode, float screenScale) {
		return getTextSize(text, font, font.paintForScreenScale(screenScale), constrainedToSize, lineBreakMode, screenScale);
	}

	// TODO: Implement line break mode
	private static Size getTextSize(CharSequence text, Font font, TextPaint textPaint, Size constrainedToSize, LineBreakMode lineBreakMode, float screenScale) {
		constrainedToSize.width = constrainWidth(constrainedToSize.width * screenScale);

		int lineCount = 0;

		int index = 0;
		int length = text.length();
		float[] measuredWidth = new float[] { 0.0f };
		float width = 0.0f;

		while(index < length - 1) {
			int measured = textPaint.breakText(text, index, length, true, constrainedToSize.width, measuredWidth);
			index += measured;
			lineCount++;

			width = Math.max(measuredWidth[0], width);

			if(measured == 0) {
				break;
			}
		}

		return new Size(width / screenScale, lineCount * font.getLineHeight());
	}

	private static float constrainWidth(float width) {
		// There seems to be a race condition in breakText when MAX_VALUE is passed and the text is too small.
		// Using a large value like 10000 seems to fix the issue, and shouldn't cause any problems since
		// we really shouldn't be rendering to a width that large anyway.
		return Math.min(width, 10000);
	}

	/**
	 * @deprecated Use Font#paintForScreenScale(float) instead
	 * @param font Font to get get paint for
	 * @param screenScale screen scale to get paint for
	 * @see Font#paintForScreenScale(float)
	 * @return TextPaint instance for Font
	 */
	static TextPaint createPaintForFont(Font font, float screenScale) {
		return font.paintForScreenScale(screenScale);
	}

}
