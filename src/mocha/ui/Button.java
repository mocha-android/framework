/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

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
	private HashMap<EnumSet<State>, HashMap<ContentType, Object>> content;

	private enum ContentType {
		TITLE, TITLE_COLOR, TITLE_SHADOW_COLOR, BACKGROUND_IMAGE, IMAGE
	}

	public Button() { this(Rect.zero()); }

	public Button(Rect frame) {
		super(frame);

		this.content = new HashMap<EnumSet<State>, HashMap<ContentType, Object>>();

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
	}

	public void setTitle(CharSequence title, State... states) {
		this.setContent(ContentType.TITLE, states, title);
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

	public void setBackgroundImage(Image backgroundImage, State... states) {
		this.setContent(ContentType.BACKGROUND_IMAGE, states, backgroundImage);
	}

	private void setContent(ContentType type, State[] states, Object value) {
		EnumSet<State> set = this.getStateSet(states);

		HashMap<ContentType, Object> content = this.content.get(set);

		if(content == null) {
			content = new HashMap<ContentType, Object>();
			this.content.put(set, content);
		}


		if(value != null) {
			content.put(type, value);
		} else {
			content.remove(type);
		}

		this.updateContent();
	}

	public CharSequence getTitleForState(State... states) {
		return (CharSequence)this.getContent(ContentType.TITLE, states);
	}

	public int getTitleColor(State... states) {
		return this.getColor(ContentType.TITLE_COLOR, states);
	}

	public int getTitleShadowColor(State... states) {
		return this.getColor(ContentType.TITLE_SHADOW_COLOR, states);
	}

	public Image getImage(State... states) {
		return (Image)this.getContent(ContentType.IMAGE, states);
	}

	public Image getBackgroundImage(State... states) {
		return (Image)this.getContent(ContentType.BACKGROUND_IMAGE, states);
	}

	private Object getContent(ContentType type, State... states) {
		EnumSet<State> stateSet = this.getStateSet(states);

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

	private EnumSet<State> getStateSet(State... states) {
		if(states != null && states.length == 1) {
			return EnumSet.of(states[0]);
		} else if(states != null && states.length > 0) {
			return EnumSet.of(states[0], states);
		} else {
			return EnumSet.noneOf(State.class);
		}
	}

	private int getColor(ContentType type, State... states) {
		Object color = this.getContent(type, states);
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
	}

	public EdgeInsets getTitleEdgeInsets() {
		return this.titleEdgeInsets.copy();
	}

	public void setTitleEdgeInsets(EdgeInsets titleEdgeInsets) {
		this.titleEdgeInsets = titleEdgeInsets == null ? EdgeInsets.zero() : titleEdgeInsets;
	}

	public EdgeInsets getImageEdgeInsets() {
		return this.imageEdgeInsets.copy();
	}

	public void setImageEdgeInsets(EdgeInsets imageEdgeInsets) {
		this.imageEdgeInsets = imageEdgeInsets == null ? EdgeInsets.zero() : imageEdgeInsets;
	}

	public CharSequence getCurrentTitle() {
		return this.titleLabel != null ? this.titleLabel.getText() : null;
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

	private void updateContent() {
		State[] states = this.getStates();


		this.titleLabel.setText(this.getTitleForState(states));
		this.titleLabel.setTextColor(this.getTitleColor());
		this.titleLabel.setShadowColor(this.getTitleShadowColor(states));

		this.imageView.setImage(this.getImage(states));
		this.backgroundImageView.setImage(this.getBackgroundImage(states));

		this.setNeedsLayout();
	}

	public Rect getBackgroundRectForBounds(Rect bounds) {
		return bounds;
	}

	public Rect getContentRectForBounds(Rect bounds) {
		return this.contentEdgeInsets == null ? bounds : this.contentEdgeInsets.inset(bounds);
	}

	private Size backgroundSizeForState(State... states) {
		Image backgroundImage = this.getBackgroundImage(states);
		return backgroundImage != null ? backgroundImage.getSize() : Size.zero();
	}

	private Size titleSizeForState(State... states) {
		CharSequence title = this.getTitleForState(states);
		Size size = title != null && title.length() > 0 ? TextDrawing.getTextSize(title, this.titleLabel.getFont()) : Size.zero();
		size.width = ceilf(size.width);
		size.height = ceilf(size.height);
		return size;
	}

	private Size imageSizeForState(State... states) {
		Image image = this.getImage(states);
		return image != null ? image.getSize() : Size.zero();
	}

	private Rect componentRectForSize(Size size, Rect contentRect, State... states) {
		Rect rect = new Rect(contentRect.origin, size);

		// clamp the right edge of the rect to the contentRect - this is what the real UIButton appears to do.
		if (rect.maxX() > contentRect.maxX()) {
			rect.size.width -= rect.maxX() - contentRect.maxX();
		}

		switch (this.getContentHorizontalAlignment()) {
			case CENTER:
				rect.origin.x += floorf((contentRect.size.width/2.f) - (rect.size.width/2.f));
				break;

			case RIGHT:
				rect.origin.x += contentRect.size.width - rect.size.width;
				break;

			case FILL:
				rect.size.width = contentRect.size.width;
				break;

			case LEFT:
				// Already left aligned
				break;
		}

		switch (this.getContentVerticalAlignment()) {
			case CENTER:
				rect.origin.y += floorf((contentRect.size.height/2.f) - (rect.size.height/2.f));
				break;

			case BOTTOM:
				rect.origin.y += contentRect.size.height - rect.size.height;
				break;

			case FILL:
				rect.size.height = contentRect.size.height;
				break;

			case TOP:
				// Already top aligned
				break;
		}

		return rect;
	}

	public Rect getTitleRectForContentRect(Rect contentRect) {
		State[] states = this.getStates();

		EdgeInsets inset = this.titleEdgeInsets == null ? EdgeInsets.zero() : this.titleEdgeInsets.copy();
		inset.left += this.imageSizeForState(states).width;

		return this.componentRectForSize(this.titleSizeForState(states), inset.inset(contentRect), states);
	}

	public Rect getImageRectForContentRect(Rect contentRect) {
		State[] states = this.getStates();

		EdgeInsets inset = this.imageEdgeInsets == null ? EdgeInsets.zero() : this.imageEdgeInsets.copy();
		inset.right += this.getTitleRectForContentRect(contentRect).size.width;

		return this.componentRectForSize(this.imageSizeForState(states), inset.inset(contentRect), states);
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		Rect bounds = this.getBounds();
		Rect contentRect = this.getContentRectForBounds(bounds);

		this.backgroundImageView.setFrame(this.getBackgroundRectForBounds(bounds));
		this.imageView.setFrame(this.getImageRectForContentRect(contentRect));
		this.titleLabel.setFrame(this.getTitleRectForContentRect(contentRect));
		MLog("Button title frame: %s", this.titleLabel.getFrame());
	}

	public Size sizeThatFits(Size size) {
		State[] states = this.getStates();

		Size imageSize = this.imageSizeForState(states);
		Size titleSize = this.titleSizeForState(states);
		MLog("title size: %s", titleSize);
		Size fitSize = new Size();
		EdgeInsets insets = this.contentEdgeInsets == null ? EdgeInsets.zero() : this.contentEdgeInsets;
		fitSize.width = insets.left + insets.right + titleSize.width + imageSize.width;
		fitSize.height = insets.top + insets.bottom + Math.max(titleSize.height,imageSize.height);

		Image background = this.getCurrentBackgroundImage();

		if(background != null) {
			Size backgroundSize = background.getSize();
			fitSize.width = Math.max(fitSize.width, backgroundSize.width);
			fitSize.height = Math.max(fitSize.height, backgroundSize.height);
		}

		return fitSize;
	}

	protected void stateDidChange() {
		super.stateDidChange();
		this.updateContent();
	}

}
