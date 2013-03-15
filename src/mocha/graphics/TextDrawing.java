/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.text.TextPaint;
import android.text.TextUtils;
import android.util.FloatMath;
import mocha.ui.Screen;

public class TextDrawing extends mocha.foundation.Object {

	public static Size draw(Context context, CharSequence text, Rect rect, Font font) {
		return draw(context, text, rect, font, TextAlignment.LEFT);
	}

	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment) {
		return draw(context, text, rect, font, textAlignment, LineBreakMode.WORD_WRAPPING);
	}

	// TODO: Implement real line break mode instead of just char wrapping, fix truncation issues.
	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment, LineBreakMode lineBreakMode) {
		float scale = context.getScale();
		float lineHeight = font.getLineHeight() * scale;
		float maxWidth = constrainWidth(rect.size.width * scale);
		float maxHeight = rect.size.height * scale;
		float lineHeightAdjustment = FloatMath.floor((font.getLineHeightDrawingAdjustment()) * scale);

		TextPaint textPaint = context.getTextPaint();
		textPaint.setTypeface(font.getTypeface());
		textPaint.setTextSize(font.getPointSize() * scale);

		int index = 0;
		int length = text.length();
		float[] measuredWidth = new float[] { 0.0f };
		Size size = new Size();

		float y = (rect.origin.y * scale) + (lineHeight / 2.0f) + lineHeightAdjustment;
		float originX = rect.origin.x * scale;

		while(index < length - 1) {
			int measured = textPaint.breakText(text, index, length, true, maxWidth, measuredWidth);

			size.width = Math.max(measuredWidth[0], size.width);
			size.height += lineHeight;

			boolean lastLine = size.height + lineHeight > maxHeight;

			CharSequence textToDraw;

			if(lastLine) {
				// textToDraw = TextUtils.ellipsize(TextUtils.substring(text, index, text.length()), textPaint, maxWidth, lineBreakMode.truncateAt());
				textToDraw = TextUtils.substring(text, index, index + measured);
			} else  {
				textToDraw = TextUtils.substring(text, index, index + measured);
			}

			index += measured;
			float x;

			if(textAlignment == TextAlignment.CENTER || textAlignment == TextAlignment.RIGHT) {
				float textWidth = textPaint.measureText(textToDraw, 0, textToDraw.length());

				if(textAlignment == TextAlignment.RIGHT) {
					x = originX + (maxWidth - textWidth);
				} else {
					x = originX + ((maxWidth - textWidth) / 2);
				}
			} else {
				x = originX;
			}

			context.getCanvas().drawText(textToDraw, 0, textToDraw.length(), x, y, textPaint);

			y += lineHeight;

			if(measured == 0 || lastLine) {
				break;
			}
		}

		return new Size(FloatMath.ceil(size.width / scale), FloatMath.ceil(size.height / scale));
	}

	/**
	 * Draws a single line of text, line breaks are ignored.
	 * Truncated based on LineBreakMode.WORD_WRAPPING.
	 *
	 * @param context Context to draw text in
	 * @param text Text to draw
	 * @param point Point to draw text at
	 * @param font Font to draw text with
	 * @return Size of the drawing rounded up to the nearest point.
	 */
	public static Size draw(Context context, CharSequence text, Point point, Font font) {
		return draw(context, text, point, font, 0.0f, LineBreakMode.WORD_WRAPPING);
	}

	/**
	 * Draws a single line of text capped at a max width, line breaks are ignored.
	 *
	 * @param context Context to draw text in
	 * @param text Text to draw
	 * @param point Point to draw text at
	 * @param font Font to draw text with
	 * @param maxWidth Maximum width text can be, if <= 0.0f, no max width is assumed.
	 * @param lineBreakMode Determines how truncation should work, still only draws a single line.
	 * @return Size of the drawing rounded up to the nearest point.
	 */
	public static Size draw(Context context, CharSequence text, Point point, Font font, float maxWidth, LineBreakMode lineBreakMode) {
		float scale = context.getScale();

		TextPaint textPaint = context.getTextPaint();
		textPaint.setTypeface(font.getTypeface());
		textPaint.setTextSize(font.getPointSize() * scale);

		if(maxWidth > 0.0f) {
			text = fitToWidth(context, text, maxWidth, lineBreakMode, textPaint);
		}

		float textWidth = textPaint.measureText(text, 0, text.length());

		float x = point.x * scale;
		float lineHeightAdjustment = (font.getLineHeightDrawingAdjustment() * scale);
		float lineHeight = font.getLineHeight() * scale;
		float y = (point.y * scale) + (lineHeight / 2.0f) + lineHeightAdjustment;

		context.getCanvas().drawText(text, 0, text.length(), x, y, textPaint);

		return new Size(FloatMath.ceil(textWidth / scale), FloatMath.ceil(font.getLineHeight()));
	}

	private static CharSequence fitToWidth(Context context, CharSequence text, float width, LineBreakMode lineBreakMode, TextPaint paint) {
		if(lineBreakMode == null) lineBreakMode = LineBreakMode.WORD_WRAPPING;

		text = TextUtils.ellipsize(text, paint, width * context.getScale(), lineBreakMode.truncateAt());

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
		Size size = new Size();
		float lineHeight = font.getLineHeight() * screenScale;
		float maxHeight = constrainedToSize.height * screenScale;

		while(index < length - 1) {
			int measured = textPaint.breakText(text, index, length, true, constrainedToSize.width, measuredWidth);
			index += measured;
			lineCount++;

			size.width = Math.max(measuredWidth[0], size.width);
			size.height += lineHeight;

			if(measured == 0 || (size.height + lineHeight > maxHeight)) {
				break;
			}
		}

		return new Size(FloatMath.ceil(size.width / screenScale), FloatMath.ceil(size.height / screenScale));
	}

	private static float constrainWidth(float width) {
		// There seems to be a race condition in breakText when MAX_VALUE is passed and the text is too small.
		// Using a large value like 10000 seems to fix the issue, and shouldn't cause any problems since
		// we really shouldn't be rendering to a width that large anyway.
		return Math.min(width, 10000);
	}

}
