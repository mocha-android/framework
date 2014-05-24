/**
 *  @author Shaun
 *  @date 4/21/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.graphics;

import android.graphics.Canvas;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.*;
import android.util.FloatMath;
import mocha.foundation.Copying;
import mocha.foundation.Range;
import mocha.ui.Application;
import mocha.ui.Color;
import mocha.ui.Screen;
import mocha.ui.ScreenMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A lot of work needed here. Some confusion around set/add and SpannableStringBuilder.
 */
public class AttributedString implements CharSequence, android.text.Spannable, Copying<AttributedString> {

	public enum Attribute {
		/**
		 * Expects value to be {@link mocha.graphics.Font}
		 */
		FONT,

		/**
		 * Expects value to be {@link mocha.graphics.ParagraphStyle}
		 */
		PARAGRAPH_STYLE,

		/**
		 * Expects value to be Integer
		 */
		FOREGROUND_COLOR,

		/**
		 * Expects value to be Integer
		 */
		BACKGROUND_COLOR,

		/**
		 * Expects value to be {@link mocha.graphics.Shadow}
		 */
		SHADOW,

		/**
		 * Expects value to be {@link mocha.graphics.TextAttachment}
		 */
		ATTACHMENT
	}

	private SpannableStringBuilder builder;
	private TextPaint textPaint;

	private StaticLayout layout;
	private TextAlignment layoutAlignment;

	private StaticLayout constrainedLayout;
	private float constrainedLayoutWidth;
	private TextAlignment constrainedLayoutAlignment;

	public AttributedString() {
		this.builder = new SpannableStringBuilder();
	}

	public AttributedString(String source, Map<Attribute,?> attributes) {
		this.builder = new SpannableStringBuilder(source);
		this.addAttributes(attributes, new Range(0, source.length()));
	}

	public AttributedString(AttributedString source) {
		this.builder = new SpannableStringBuilder(source.builder);
	}

	public void addAttributes(Map<Attribute,?> attributes, Range range) {
		if(attributes == null || range == null || range.length <= 0) return;

		int start = (int)range.location;
		int end = (int)range.length;

		for(Map.Entry<Attribute,?> entry : attributes.entrySet()) {
			Attribute attribute = entry.getKey();
			if(attribute == null) continue;
			this.add(attribute, entry.getValue(), start, end);
		}
	}

	public void add(Attribute attribute, Object value, Range range) {
		this.add(attribute, value, (int)range.location, (int)range.length);
	}

	private void add(Attribute attribute, Object value, int start, int end) {
		if(attribute == Attribute.PARAGRAPH_STYLE) {
			for(Object span : this.getParagraphStyleSpans((ParagraphStyle)value)) {
				this.builder.setSpan(span, start, end, 0);
			}
		} else {
			Object span = this.getSpan(attribute, value);

			if(span != null) {
				this.builder.setSpan(span, start, end, 0);
			}
		}

		this.layout = null;
	}

	private Object getSpan(Attribute attribute, Object value) {
		switch (attribute) {
			case FONT:
				return new FontSpan((Font)value);
			case PARAGRAPH_STYLE:
				return null;
			case FOREGROUND_COLOR:
				return new ForegroundColorSpan((Integer)value);
			case BACKGROUND_COLOR:
				return new BackgroundColorSpan((Integer)value);
			case SHADOW:
				return new ShadowSpan((Shadow)value);
			case ATTACHMENT:
				return new TextAttachmentSpan((TextAttachment)value);
		}

		throw new IllegalArgumentException();
	}

	private List<Object> getParagraphStyleSpans(ParagraphStyle paragraphStyle) {
		List<Object> spans = new ArrayList<>();

		if(paragraphStyle.getLineSpacing() != Float.MIN_VALUE) {
			// TODO
		}

		if(paragraphStyle.getFirstLineHeadIndent() != Float.MIN_VALUE) {
			// TODO Replace with a version that can get scale from paint, using mainScreen here is not ideal.
			spans.add(new LeadingMarginSpan.Standard((int)Math.floor(paragraphStyle.getFirstLineHeadIndent() * Screen.mainScreen().getScale()), 0));
		}

		if(paragraphStyle.getAlignment() != null) {
			spans.add(new AlignmentSpan.Standard(paragraphStyle.getAlignment().getLayoutAlignemnt()));
		}

		return spans;
	}

	public void remove(Attribute attribute, Range range) {
		// TODO
	}

	public void replace(Range range, String string) {
		this.builder.replace((int)range.location, (int)range.length, string);
		this.layout = null;
	}

	public void setAttributes(Map<Attribute,?> attributes, Range range) {
		// TODO
	}

	public void append(String string, Map<Attribute,?> attributes) {
		AttributedString attributedString = new AttributedString(string, attributes);
		this.builder.append(attributedString.builder);
	}

	/**
	 * @hide
	 */
	public void setSpan(Object what, int start, int end, int flags) {
		this.builder.setSpan(what, start, end, flags);
		this.layout = null;
	}

	/**
	 * @hide
	 */
	public void removeSpan(Object what) {
		this.builder.removeSpan(what);
		this.layout = null;
	}

	/**
	 * @hide
	 */
	public<T> T[] getSpans(int queryStart, int queryEnd, java.lang.Class<T> kind) {
		return this.builder.getSpans(queryStart, queryEnd, kind);
	}

	/**
	 * @hide
	 */
	public int getSpanStart(Object what) {
		return this.builder.getSpanStart(what);
	}

	/**
	 * @hide
	 */
	public int getSpanEnd(Object what) {
		return this.builder.getSpanEnd(what);
	}

	/**
	 * @hide
	 */
	public int getSpanFlags(Object what) {
		return this.builder.getSpanEnd(what);
	}

	/**
	 * @hide
	 */
	public int nextSpanTransition(int start, int limit, Class kind) {
		return this.builder.nextSpanTransition(start, limit, kind);
	}

	public int length() {
		return this.builder.length();
	}

	public char charAt(int where) {
		return this.builder.charAt(where);
	}

	public CharSequence subSequence(int start, int end) {
		return this.builder.subSequence(start, end);
	}

	public String toString() {
		return this.builder.toString();
	}

	public AttributedString copy() {
		return new AttributedString(this);
	}

	/**
	 * Draw into context without size constraints
	 *
	 * @param context Context to draw into
	 * @param point Point to start drawing at
	 */
	public void draw(Context context, Point point) {
		this.draw(context, point, TextAlignment.LEFT);
	}

	/**
	 * Draw into context without size constraints
	 *
	 * @param context Context to draw into
	 * @param point Point to start drawing at
	 * @param defaultAlignment Default text alignment if a PARAGRAPH_STYLE attribute isn't provided.
	 */
	public void draw(Context context, Point point, TextAlignment defaultAlignment) {
		this.buildLayout(defaultAlignment);

		this.layout.getPaint().set(context.getTextPaint());

		float scale = context.getScale();

		Canvas canvas = context.getCanvas();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(point.x * scale, point.y * scale);
		this.layout.draw(canvas);
		canvas.restore();
	}

	/**
	 * Draw into context constrained to rect
	 *
	 * @param context Context to draw into
	 * @param rect Rect to constrain text to
	 */
	public void draw(Context context, Rect rect) {
		this.draw(context, rect, TextAlignment.LEFT);
	}

	/**
	 * Draw into context constrained to rect
	 *
	 * @param context Context to draw into
	 * @param rect Rect to constrain text to
	 * @param defaultAlignment Default text alignment if a PARAGRAPH_STYLE attribute isn't provided.
	 */
	public void draw(Context context, Rect rect, TextAlignment defaultAlignment) {
		this.buildConstrainedLayout(defaultAlignment, rect.size.width, context.getScale());
		this.constrainedLayout.getPaint().set(context.getTextPaint());

		float scale = context.getScale();

		Canvas canvas = context.getCanvas();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(rect.origin.x * scale, rect.origin.y * scale);
		this.constrainedLayout.draw(canvas);
		canvas.restore();
	}

	/**
	 * Get size without size constraints
	 *
	 * @return Size
	 */
	public Size size() {
		return this.size(TextAlignment.LEFT);
	}

	/**
	 * Get size without size constraints
	 *
	 * @param defaultAlignment Default text alignment if a PARAGRAPH_STYLE attribute isn't provided.
	 * @return Size
	 */
	public Size size(TextAlignment defaultAlignment) {
		if(this.length() == 0) return Size.zero();

		if(this.layout == null) {
			this.buildLayout(defaultAlignment);
		}

		float width = Float.MIN_VALUE;
		float height = 0;
		int lines = this.layout.getLineCount();

		for(int line = 0; line < lines; line++) {
			float w = this.layout.getLineWidth(line);

			if(w > width) {
				width = w;
			}

			height += this.layout.getLineBottom(line) - this.layout.getLineTop(line);
		}

		float scale = Screen.mainScreen().getScale();
		return new Size(width / scale, height / scale);
	}

	/**
	 * Get the bounding rect for the specified size
	 *
	 * @param size Max size
	 * @return Bounding rect
	 */
	public Rect getBoundingRectWithSize(Size size) {
		return this.getBoundingRectWithSize(size, TextAlignment.LEFT);
	}

	/**
	 * Get the bounding rect for the specified size
	 *
	 * @param size Max size
	 * @param defaultAlignment Default text alignment if a PARAGRAPH_STYLE attribute isn't provided.
	 * @return Bounding rect
	 */
	public Rect getBoundingRectWithSize(Size size, TextAlignment defaultAlignment) {
		if(this.length() == 0) {
			return Rect.zero();
		}

		this.buildConstrainedLayout(defaultAlignment, size.width, Screen.mainScreen().getScale());

		float width = Float.MIN_VALUE;
		float x = Float.MAX_VALUE;
		float y = 0.0f;
		float height = 0;
		int lines = this.constrainedLayout.getLineCount();

		for(int line = 0; line < lines; line++) {
			float w = this.constrainedLayout.getLineWidth(line);
			float left = this.constrainedLayout.getLineLeft(line);

			if(w > width) {
				width = w;
			}

			if(left < x) {
				x = left;
			}

			float top = this.constrainedLayout.getLineTop(line);

			if(line == 0) {
				y = top;
			}

			height += this.constrainedLayout.getLineBottom(line) - top;
		}

		float scale = Screen.mainScreen().getScale();

		return new Rect(x / scale, y / scale, width / scale, height / scale);
	}

	private void buildLayout(TextAlignment defaultAlignment) {
		if(this.textPaint == null) {
			this.textPaint = new TextPaint();
			this.textPaint.setAntiAlias(true);
		}

		if(this.layout == null || this.layoutAlignment != defaultAlignment) {
			this.layout = new StaticLayout(this.builder, 0, this.builder.length(), this.textPaint, 10000, defaultAlignment.getLayoutAlignemnt(), 1.0f, 0.0f, false);
			this.layoutAlignment = defaultAlignment;
		}
	}

	private void buildConstrainedLayout(TextAlignment defaultAlignment, float width, float scale) {
		width *= scale;

		if(this.textPaint == null) {
			this.textPaint = new TextPaint();
			this.textPaint.setAntiAlias(true);
		}

		if(this.constrainedLayout == null || this.constrainedLayoutWidth != width || this.constrainedLayoutAlignment != defaultAlignment) {
			this.constrainedLayout = new StaticLayout(this.builder, 0, this.builder.length(), this.textPaint, (int)Math.ceil(Math.min(width, 10000)), defaultAlignment.getLayoutAlignemnt(), 1.0f, 0.0f, false);
			this.constrainedLayoutWidth = width;
			this.constrainedLayoutAlignment = defaultAlignment;
		}
	}

}
