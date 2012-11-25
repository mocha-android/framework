/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;

public class Button extends Control {

	private Label titleLabel;
	private ImageView imageView;
	private boolean reversesTitleShadowWhenHighlighted;
	private boolean adjustsImageWhenHighlighted;
	private boolean adjustsImageWhenDisabled;
	private boolean showsTouchWhenHighlighted; // TODO
	private EdgeInsets contentEdgeInsets;
	private EdgeInsets titleEdgeInsets;
	private EdgeInsets imageEdgeInsets;

	public void setTitle(String title, State... states) {

	}

	public void setTitleColor(int titleColor, State... states) {

	}

	public void setTitleShadowColor(int titleShadowColor, State... states) {

	}

	public void setImage(Image image, State... states) {

	}

	public void setBackgroundImage(Image backgroundImage, State... states) {

	}

	public String getTitleForState(State... states) {
		return null;
	}

	public int getTitleColor(State... states) {
		return 0;
	}

	public int getTitleShadowColor(State... states) {
		return 0;
	}

	public Image getImage(State... states) {
		return null;
	}

	public Image getBackgroundImage(State... states) {
		return null;
	}

	public Rect getBackgroundRectForBounds(Rect bounds) {
		return bounds.copy();
	}

	public Rect getContentRectForBounds(Rect bounds) {
		return bounds.copy();
	}

	public Rect getTitleRectForContentRect(Rect contentRect) {
		return contentRect.copy();
	}

	public Rect getImageRectForContentRect(Rect contentRect) {
		return contentRect.copy();
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

	public String getCurrentTitle() {
		return null;
	}

	public int getCurrentTitleColor() {
		return 0;
	}

	public int getCurrentTitleShadowColor() {
		return 0;
	}

	public Image getCurrentImage() {
		return null;
	}

	public Image getCurrentBackgroundImage() {
		return null;
	}
}
