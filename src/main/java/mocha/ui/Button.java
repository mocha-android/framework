/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Button extends Control {

	private Label titleLabel;
	private ImageView imageView;
	private ImageView backgroundImageView;
	private boolean reversesTitleShadowWhenHighlighted;
	private boolean adjustsImageWhenHighlighted;
	private boolean adjustsImageWhenDisabled;
	private boolean showsTouchWhenHighlighted; // TODO
	private EdgeInsets contentEdgeInsets;
	private EdgeInsets titleEdgeInsets;
	private EdgeInsets imageEdgeInsets;
	private Map<EnumSet<State>, HashMap<ContentType, Object>> content;

	private enum ContentType {
		TITLE, TITLE_COLOR, TITLE_SHADOW_COLOR, ATTRIBUTED_TITLE, BACKGROUND_IMAGE, IMAGE
	}

	public Button() { this(Rect.zero()); }

	public Button(Rect frame) {
		super(frame);

		this.content = new HashMap<>();

		this.contentEdgeInsets = EdgeInsets.zero();
		this.titleEdgeInsets = EdgeInsets.zero();
		this.imageEdgeInsets = EdgeInsets.zero();

		this.backgroundImageView = new ImageView();
		this.backgroundImageView.setBackgroundColor(Color.TRANSPARENT);
		this.addSubview(this.backgroundImageView);

		this.imageView = new ImageView();
		this.imageView.setBackgroundColor(Color.TRANSPARENT);
		this.addSubview(this.imageView);

		this.titleLabel = new Label();
		this.titleLabel.setShadowOffset(Size.zero());
		this.titleLabel.setTextAlignment(TextAlignment.CENTER);
		this.titleLabel.setBackgroundColor(Color.TRANSPARENT);
		this.addSubview(this.titleLabel);

		this.adjustsImageWhenDisabled = true;
	}

	public void setTitle(CharSequence title, State... states) {
		this.setContent(ContentType.TITLE, states, title);
	}

	public void setAttributedTitle(AttributedString title, State... states) {
		this.setContent(ContentType.ATTRIBUTED_TITLE, states, title);
	}

	public void setTitleColor(int titleColor, State... states) {
		this.setContent(ContentType.TITLE_COLOR, states, titleColor == 0 ? null : titleColor);
	}

	public void setTitleShadowColor(int titleShadowColor, State... states) {
		this.setContent(ContentType.TITLE_SHADOW_COLOR, states, titleShadowColor == 0 ? null : titleShadowColor);
	}

	public void setImage(Image image, State... states) {
		this.setContent(ContentType.IMAGE, states, image);
	}

	public void setImage(int resourceID, State... states) {
		this.setImage(Image.imageNamed(resourceID), states);
	}

	public void setBackgroundImage(Image backgroundImage, State... states) {
		this.setContent(ContentType.BACKGROUND_IMAGE, states, backgroundImage);
	}

	public void setBackgroundImage(int resourceID, State... states) {
		this.setBackgroundImage(Image.imageNamed(resourceID), states);
	}

	private void setContent(ContentType type, State[] states, Object value) {
		EnumSet<State> set = State.toSet(states);

		HashMap<ContentType, Object> content = this.content.get(set);

		if(content == null) {
			content = new HashMap<>();
			this.content.put(set, content);
		}


		if(value != null) {
			content.put(type, value);
		} else {
			content.remove(type);
		}

		this.updateContent(type);
	}

	public CharSequence getTitleForState(State... states) {
		return (CharSequence)this.getContent(ContentType.TITLE, states);
	}

	private CharSequence getTitleForState(EnumSet<State> stateSet) {
		return (CharSequence)this.getContent(ContentType.TITLE, stateSet);
	}

	public AttributedString getAttributedTitleForState(State... states) {
		return (AttributedString)this.getContent(ContentType.TITLE, states);
	}

	private AttributedString getAttributedTitleForState(EnumSet<State> stateSet) {
		return (AttributedString)this.getContent(ContentType.ATTRIBUTED_TITLE, stateSet);
	}

	public int getTitleColor(State... states) {
		return this.getColor(ContentType.TITLE_COLOR, states);
	}

	private int getTitleColor(EnumSet<State> stateSet) {
		return this.getColor(ContentType.TITLE_COLOR, stateSet);
	}

	public int getTitleShadowColor(State... states) {
		return this.getColor(ContentType.TITLE_SHADOW_COLOR, states);
	}

	private int getTitleShadowColor(EnumSet<State> stateSet) {
		return this.getColor(ContentType.TITLE_SHADOW_COLOR, stateSet);
	}

	public Image getImage(State... states) {
		return (Image)this.getContent(ContentType.IMAGE, states);
	}

	public Image getImage(EnumSet<State> stateSet) {
		return (Image)this.getContent(ContentType.IMAGE, stateSet);
	}

	public Image getBackgroundImage(State... states) {
		return (Image)this.getContent(ContentType.BACKGROUND_IMAGE, states);
	}

	private Image getBackgroundImage(EnumSet<State> stateSet) {
		return (Image)this.getContent(ContentType.BACKGROUND_IMAGE, stateSet);
	}

	private Object getContent(ContentType type, State... states) {
		return getContent(type, State.toSet(states));
	}

	private Object getContent(ContentType type, EnumSet<State> stateSet) {
		Object object = null;
		HashMap<ContentType, Object> content = this.content.get(stateSet);

		if(content != null) {
			object = content.get(type);
		}

		if(object == null && (stateSet.size() != 1 || !stateSet.contains(State.NORMAL))) {
			object = this.getContent(type, State.NORMAL);
		}

		return object;
	}

	private int getColor(ContentType type, State... states) {
		return this.getColor(type, State.toSet(states));
	}

	private int getColor(ContentType type, EnumSet<State> stateSet) {
		Object color = this.getContent(type, stateSet);
		return color == null ? 0 : (Integer)color;
	}

	public Label getTitleLabel() {
		return this.titleLabel;
	}

	public ImageView getImageView() {
		return this.imageView;
	}

	public boolean getReversesTitleShadowWhenHighlighted() {
		return this.reversesTitleShadowWhenHighlighted;
	}

	public void setReversesTitleShadowWhenHighlighted(boolean reversesTitleShadowWhenHighlighted) {
		this.reversesTitleShadowWhenHighlighted = reversesTitleShadowWhenHighlighted;
	}

	public boolean getAdjustsImageWhenHighlighted() {
		return this.adjustsImageWhenHighlighted;
	}

	public void setAdjustsImageWhenHighlighted(boolean adjustsImageWhenHighlighted) {
		this.adjustsImageWhenHighlighted = adjustsImageWhenHighlighted;
	}

	public boolean getAdjustsImageWhenDisabled() {
		return this.adjustsImageWhenDisabled;
	}

	public void setAdjustsImageWhenDisabled(boolean adjustsImageWhenDisabled) {
		this.adjustsImageWhenDisabled = adjustsImageWhenDisabled;
	}

	public boolean getShowsTouchWhenHighlighted() {
		return this.showsTouchWhenHighlighted;
	}

	public void setShowsTouchWhenHighlighted(boolean showsTouchWhenHighlighted) {
		this.showsTouchWhenHighlighted = showsTouchWhenHighlighted;
	}

	public EdgeInsets getContentEdgeInsets() {
		return this.contentEdgeInsets.copy();
	}

	public void setContentEdgeInsets(EdgeInsets contentEdgeInsets) {
		this.contentEdgeInsets = contentEdgeInsets == null ? EdgeInsets.zero() : contentEdgeInsets;
		this.setNeedsLayout();
	}

	public EdgeInsets getTitleEdgeInsets() {
		return this.titleEdgeInsets.copy();
	}

	public void setTitleEdgeInsets(EdgeInsets titleEdgeInsets) {
		this.titleEdgeInsets = titleEdgeInsets == null ? EdgeInsets.zero() : titleEdgeInsets;
		this.setNeedsLayout();
	}

	public EdgeInsets getImageEdgeInsets() {
		return this.imageEdgeInsets.copy();
	}

	public void setImageEdgeInsets(EdgeInsets imageEdgeInsets) {
		this.imageEdgeInsets = imageEdgeInsets == null ? EdgeInsets.zero() : imageEdgeInsets;
		this.setNeedsLayout();
	}

	public CharSequence getCurrentTitle() {
		return this.titleLabel != null ? this.titleLabel.getText() : null;
	}

	public AttributedString getCurrentAttributedTitle() {
		return this.titleLabel != null ? this.titleLabel.getAttributedText() : null;
	}

	public int getCurrentTitleColor() {
		return this.titleLabel != null ? this.titleLabel.getTextColor() : 0;
	}

	public int getCurrentTitleShadowColor() {
		return this.titleLabel != null ? this.titleLabel.getShadowColor() : 0;
	}

	public Image getCurrentImage() {
		return this.imageView != null ? this.imageView.getImage() : null;
	}

	public Image getCurrentBackgroundImage() {
		return this.backgroundImageView != null ? this.backgroundImageView.getImage() : null;
	}

	private void updateContent(ContentType changedContent) {
		EnumSet<State> states = this.getState();

		boolean needsLayout = false;

		if(changedContent == null || changedContent == ContentType.TITLE) {
			CharSequence newText = this.getTitleForState(states);
			this.titleLabel.setText(newText);
			needsLayout = true;
		}

		if(changedContent == null || changedContent == ContentType.ATTRIBUTED_TITLE) {
			this.titleLabel.setAttributedText(this.getAttributedTitleForState(states));
			needsLayout = true;
		}

		if(changedContent == null || changedContent == ContentType.TITLE_COLOR) {
			this.titleLabel.setTextColor(this.getTitleColor(states));
		}

		if(changedContent == null || changedContent == ContentType.TITLE_SHADOW_COLOR) {
			this.titleLabel.setShadowColor(this.getTitleShadowColor(states));
		}

		if(changedContent == null || changedContent == ContentType.IMAGE) {
			Image oldImage = needsLayout ? null : this.imageView.getImage();
			Image newImage = this.getImage(states);
			this.imageView.setImage(newImage);

			if(!needsLayout) {
				if(oldImage != null && newImage == null) {
					needsLayout = true;
				} else if(oldImage == null && newImage != null) {
					needsLayout = true;
				} else if (oldImage != null) {
					needsLayout = !oldImage.getSize().equals(newImage.getSize());
				}
			}
		}

		if(changedContent == null || changedContent == ContentType.BACKGROUND_IMAGE) {
			this.backgroundImageView.setImage(this.getBackgroundImage(states));
		}

		if(this.adjustsImageWhenDisabled && changedContent == null) {
			if(this.getState().contains(State.DISABLED)) {
				this.imageView.setAlpha(0.35f);
			} else {
				this.imageView.setAlpha(1.0f);
			}
		}

		if(needsLayout) {
			if(this.getSuperview() != null) {
				this.layoutSubviews();
			} else {
				this.setNeedsLayout();
			}
		}
	}

	public Rect getBackgroundRectForBounds(Rect bounds) {
		return bounds.copy();
	}

	public Rect getContentRectForBounds(Rect bounds) {
		return this.contentEdgeInsets == null ? bounds.copy() : this.contentEdgeInsets.inset(bounds);
	}

	public Rect getTitleRectForContentRect(Rect contentRect) {
		Image image = this.getCurrentImage();
		float imageWidth = image == null ? 0.0f : image.getSize().width;

		if(this.getCurrentAttributedTitle().length() > 0) {
			return this.getAttributedTitleRectForContentRect(contentRect, imageWidth);
		} else {
			return this.getPlainTitleRectForContentRect(contentRect, imageWidth);
		}
	}

	private Rect getPlainTitleRectForContentRect(Rect contentRect, float imageWidth) {
		CharSequence title = this.getCurrentTitle();
		if (title == null || title.length() == 0) return Rect.zero();

		if (this.titleEdgeInsets != null) {
			contentRect = this.titleEdgeInsets.inset(contentRect);
		}

		Font font = this.titleLabel.getFont();

		float availableWidth = contentRect.size.width - imageWidth;

		Rect rect = contentRect.copy();
		rect.size.width = Math.min(ceilf(TextDrawing.getTextWidth(title, font, availableWidth)), availableWidth);
		rect.size.height = Math.min(ceilf(font.getLineHeight()), contentRect.size.height);

		return this.getTitleRectForContentRect(contentRect, rect, imageWidth);
	}

	private Rect getAttributedTitleRectForContentRect(Rect contentRect, float imageWidth) {
		float availableWidth = contentRect.size.width - imageWidth;

		Size textSize = this.getCurrentAttributedTitle().getBoundingRectWithSize(new Size(availableWidth, contentRect.size.height)).size;
		Rect rect = contentRect.copy();
		rect.size.width = Math.min(ceilf(textSize.width), availableWidth);
		rect.size.height = Math.min(ceilf(textSize.height), contentRect.size.height);

		return this.getTitleRectForContentRect(contentRect, rect, imageWidth);
	}

	private Rect getTitleRectForContentRect(Rect contentRect, Rect rect, float imageWidth) {
		switch (this.getContentVerticalAlignment()) {
			case CENTER:
				rect.origin.y += floorf((contentRect.size.height - rect.size.height) / 2.0f);
				break;
			case TOP:
				// Already top aligned
				break;
			case BOTTOM:
				rect.origin.y = contentRect.maxY() - rect.size.height;
				break;
			case FILL:
				rect.size.height = contentRect.size.height;
				break;
		}

		switch (this.getContentHorizontalAlignment()) {
			case CENTER:
				rect.origin.x += imageWidth + floorf((contentRect.size.width - rect.size.width - imageWidth) / 2.0f);
				break;
			case LEFT:
				rect.origin.x += imageWidth;
				break;
			case RIGHT:
				rect.origin.x = contentRect.maxX() - rect.size.width;
				break;
			case FILL:
				// Not sure what to do here, will just offset by width and treat as left aligned
				rect.origin.x += imageWidth;
				break;
		}

		return rect;
	}

	public Rect getImageRectForContentRect(Rect contentRect) {
		Image image = this.getCurrentImage();
		if(image == null) return Rect.zero();

		Rect rect = contentRect.copy();
		rect.size = image.getSize();
		rect.size.width = Math.min(rect.size.width, contentRect.size.width);
		rect.size.height = Math.min(rect.size.height, contentRect.size.height);

		switch (this.getContentVerticalAlignment()) {
			case CENTER:
				rect.origin.y += floorf((contentRect.size.height - rect.size.height) / 2.0f);
				break;
			case TOP:
				// Already top aligned
				break;
			case BOTTOM:
				rect.origin.y = contentRect.maxY() - rect.size.height;
				break;
			case FILL:
				rect.size.height = contentRect.size.height;
				break;
		}

		HorizontalAlignment alignment = this.getContentHorizontalAlignment();

		if(alignment != HorizontalAlignment.LEFT && rect.size.width < contentRect.size.width) {
			CharSequence title = this.getCurrentTitle();
			float width = title != null && title.length() > 0 ? ceilf(TextDrawing.getTextWidth(title, this.getTitleLabel().getFont(), contentRect.size.width - rect.size.width)) : 0.0f;

			switch (alignment) {
				case CENTER:
					rect.origin.x += floorf((contentRect.size.width - rect.size.width - width) / 2.0f);
					break;
				case LEFT:
					// Already left aligned
					break;
				case RIGHT:
					rect.origin.x = contentRect.maxX() - rect.size.width - width;
					break;
				case FILL:
					// Not sure what to do here.
					break;
			}
		}

		return this.imageEdgeInsets == null ? rect : this.imageEdgeInsets.inset(rect);
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		Rect bounds = this.getBounds();
		Rect contentRect = this.getContentRectForBounds(bounds);

		this.backgroundImageView.setFrame(this.getBackgroundRectForBounds(bounds));
		this.imageView.setFrame(this.getImageRectForContentRect(contentRect));
		this.titleLabel.setFrame(this.getTitleRectForContentRect(contentRect));
	}

	public Size sizeThatFits(Size size) {
		Image image = this.getCurrentImage();
		Size imageSize = image == null ? Size.zero() : image.getSize();
		Size sizeThatFits = imageSize.copy();
		CharSequence title = this.getCurrentTitle();

		if(title != null && title.length() > 0) {
			float titleWidth = ceilf(this.titleLabel.sizeThatFits(new Size(10000.0f, 10000.0f)).width);
			sizeThatFits.width = imageSize.width + titleWidth;
		}

		if(title != null && title.length() > 0) {
			sizeThatFits.height = Math.max(imageSize.height, ceilf(this.titleLabel.getFont().getLineHeight()));
		}

		sizeThatFits.width += this.contentEdgeInsets.left + this.contentEdgeInsets.right;
		sizeThatFits.height += this.contentEdgeInsets.top + this.contentEdgeInsets.bottom;

		Image background = this.getCurrentBackgroundImage();

		if(background != null) {
			Size backgroundSize = background.getSize();
			sizeThatFits.width = Math.max(sizeThatFits.width, backgroundSize.width);
			sizeThatFits.height = Math.max(sizeThatFits.height, backgroundSize.height);
		}

		return sizeThatFits;
	}

	protected void stateDidChange() {
		super.stateDidChange();
		this.updateContent(null);
	}

}
