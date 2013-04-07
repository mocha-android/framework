/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Canvas;
import android.text.*;
import android.util.FloatMath;
import mocha.ui.Screen;

public class TextDrawing extends mocha.foundation.Object {

	public static Size draw(Context context, CharSequence text, Rect rect, Font font) {
		return draw(context, text, rect, font, TextAlignment.LEFT);
	}

	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment) {
		return draw(context, text, rect, font, textAlignment, LineBreakMode.WORD_WRAPPING);
	}

	public static Size draw(Context context, CharSequence text, Rect rect, Font font, TextAlignment textAlignment, LineBreakMode lineBreakMode) {
		if(rect.size.width < 0.0f || rect.size.height < 0.0f) {
			return Size.zero();
		}

		if(textAlignment == null) {
			textAlignment = TextAlignment.LEFT;
		}

		if(lineBreakMode == null) {
			lineBreakMode = LineBreakMode.TRUNCATING_TAIL;
		}

		float scale = context.getScale();

		TextPaint textPaint = context.getTextPaint();
		textPaint.setTypeface(font.getTypeface());
		textPaint.setTextSize(font.getPointSize() * scale);

		float maxWidth = constrainWidth(rect.size.width * scale);

		Layout layout = getLayout(text, maxWidth, heightSupportsMultipleLines(rect.size.height, font), textPaint, textAlignment, lineBreakMode);
		rect = getAdjustedRect(rect, layout, textAlignment, font, scale);

		Canvas canvas = context.getCanvas();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(rect.origin.x * scale, rect.origin.y * scale);
		layout.draw(context.getCanvas());
		canvas.restore();

		float width = (float)layout.getWidth();
		float height = (float)layout.getHeight();

		return new Size(width / scale, height / scale);
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
		if(maxWidth < 0.0f) {
			maxWidth = constrainWidth(Float.MAX_VALUE);
		}

		float scale = context.getScale();

		TextPaint textPaint = context.getTextPaint();
		textPaint.setTypeface(font.getTypeface());
		textPaint.setTextSize(font.getPointSize() * scale);

		Layout layout = getLayout(text, maxWidth, false, textPaint, TextAlignment.LEFT, lineBreakMode);
		float textWidth = textPaint.measureText(text, 0, text.length());

		Canvas canvas = context.getCanvas();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(point.x * scale, point.y * scale);
		layout.draw(canvas);
		canvas.restore();

		return new Size(FloatMath.ceil(textWidth / scale), FloatMath.ceil(font.getLineHeight()));
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

		if(text instanceof TextDrawingText) {
			text = ((TextDrawingText) text).getText();
		}

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

	private static Size getTextSize(CharSequence text, Font font, TextPaint textPaint, Size constrainedToSize, LineBreakMode lineBreakMode, float screenScale) {
		if(text == null || text.length() == 0) return Size.zero();
		if(constrainedToSize.width < 0.0f) return Size.zero();

		constrainedToSize = new Size(constrainWidth(constrainedToSize.width * screenScale), constrainedToSize.height);

		Size size = getLayoutSize(getLayout(text, constrainedToSize.width, heightSupportsMultipleLines(constrainedToSize.height, font), textPaint, TextAlignment.LEFT, lineBreakMode), font, screenScale);
		size.height = Math.max(size.height, font.getLineHeight());
		size.height = Math.min(size.height, constrainedToSize.height);
		return size;
	}

	private static boolean heightSupportsMultipleLines(float height, Font font) {
		return height == 0.0f || height >= font.getLineHeight() * 1.5f;
	}

	private static float constrainWidth(float width) {
		// There seems to be a race condition in breakText when MAX_VALUE is passed and the text is too small.
		// Using a large value like 10000 seems to fix the issue, and shouldn't cause any problems since
		// we really shouldn't be rendering to a width that large anyway.
		return Math.min(width, 10000.0f);
	}

	private static Layout getLayout(CharSequence text, float maxWidth, boolean useMultipleLines, TextPaint textPaint, TextAlignment textAlignment, LineBreakMode lineBreakMode) {
		Layout layout;
		int outerWidth = (int)FloatMath.floor(maxWidth);

		TextDrawingText textDrawingText = null;

		if(text instanceof TextDrawingText) {
			if(((TextDrawingText) text).getLayout() != null) {
				if(((TextDrawingText) text).getLayout().getAlignment() == textAlignment.getLayoutAlignemnt()) {
					((TextDrawingText) text).getPaint().set(textPaint);
					return ((TextDrawingText) text).getLayout();
				}
			}

			textDrawingText = (TextDrawingText) text;
			text = textDrawingText.getText();
			textPaint = new TextPaint(textPaint);
		}

		BoringLayout.Metrics metrics;
		boolean isBoring = (metrics = BoringLayout.isBoring(text, textPaint)) != null;

		if(isBoring && (!useMultipleLines || (float)metrics.width <= maxWidth)) {
			layout = new BoringLayout(text, textPaint, outerWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0, metrics, false, lineBreakMode.truncateAt(), outerWidth);
		} else {
			layout = new StaticLayout(text, 0, text.length(), textPaint, outerWidth, textAlignment.getLayoutAlignemnt(), 1.0f, 0.0f, false);
		}

		if(textDrawingText != null) {
			textDrawingText.setLayout(layout, textPaint);
		}

		return layout;
	}

	private static Size getLayoutSize(Layout layout, Font font, float scale) {
		CharSequence text = layout.getText();

		if(layout instanceof BoringLayout) {
			float width = layout.getPaint().measureText(text, 0, text.length());
			return new Size(FloatMath.ceil(width / scale), (float)layout.getHeight() / scale);
		} else if(layout instanceof StaticLayout) {
			float width = Float.MIN_VALUE;
			int lines = layout.getLineCount();
			float w[] = new float[1];

			for(int line = 0; line < lines; line++) {
				layout.getPaint().breakText(text, layout.getLineStart(line), layout.getLineEnd(line), true, 10000.0f, w);
				if(w[0] > width) {
					width = w[0];
				}
			}

			return new Size(FloatMath.ceil(width / scale), font.getLineHeight() * (float)lines);
		} else {
			return Size.zero();
		}
	}

	private static Rect getAdjustedRect(Rect rect, Layout layout, TextAlignment textAlignment, Font font, float scale) {
		if(textAlignment != TextAlignment.LEFT && layout instanceof BoringLayout) {
			rect = rect.copy();

			float textWidth = getLayoutSize(layout, font, scale).width;

			if(textAlignment == TextAlignment.CENTER) {
				rect.origin.x += FloatMath.floor((rect.size.width - textWidth) / 2.0f);
			} else if(textAlignment == TextAlignment.RIGHT) {
				rect.origin.x += rect.size.width - textWidth;
			}

			rect.size.width = textWidth;
		}

		return rect;
	}

}
