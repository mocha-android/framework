/*
 *  @author Shaun
 *	@date 1/30/13
 *	@copyright	2013 enormego All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

class BarButton extends Button {
	private float backgroundImageVerticalAdjustment = 0.0f;
	private BarButtonItem barButtonItem;

	static BarButton backButton(NavigationItem navigationItem) {
		if(navigationItem.getBackBarButtonItem() != null) {
			return new BarButton(navigationItem.getBackBarButtonItem(), true);
		} else {
			return new BarButton(new BarButtonItem(navigationItem.getTitle(), BarButtonItem.Style.BORDERED, null), true);
		}
	}

	static BarButton button(final BarButtonItem barButtonItem) {
		BarButtonItem.SystemItem systemItem = barButtonItem.getSystemItem();

		if(systemItem == BarButtonItem.SystemItem.FIXED_SPACE || systemItem == BarButtonItem.SystemItem.FLEXIBLE_SPACE) {
			return null;
		} else {
			BarButton button = new BarButton(barButtonItem, false);
			button.addActionTarget(new Control.ActionTarget() {
				public void onControlEvent(Control control, ControlEvent controlEvent, Event event) {
					BarButtonItem.Action action = barButtonItem.getAction();

					if(action != null) {
						action.action(barButtonItem, event);
					}
				}
			}, Control.ControlEvent.TOUCH_UP_INSIDE);
			return button;
		}
	}

	private BarButton(BarButtonItem item, boolean back) {
		this.barButtonItem = item;
		TextAttributes normalAttributes = item.getTitleTextAttributesForState(State.NORMAL);
		float fontSize = item.getStyle() == BarButtonItem.Style.PLAIN ? 18.0f : 12.0f;

		if(normalAttributes != null && normalAttributes.font != null) {
			Font font = normalAttributes.font;

			if(font.getPointSize() == 0.0f) {
				font = font.getFontWithSize(fontSize);
			}

			this.getTitleLabel().setFont(font);
		} else {
			this.getTitleLabel().setFont(Font.getBoldSystemFontWithSize(fontSize));
		}

		if(normalAttributes != null && normalAttributes.shadowOffset != null) {
			this.getTitleLabel().setShadowOffset(normalAttributes.shadowOffset);
		} else {
			this.getTitleLabel().setShadowOffset(new Size(0.0f, -1.0f));
		}

		if(normalAttributes != null && normalAttributes.textColor != 0) {
			this.setTitleColor(normalAttributes.textColor, State.NORMAL);
		} else {
			this.setTitleColor(Color.WHITE, State.NORMAL);
		}

		if(normalAttributes != null && normalAttributes.shadowColor != 0) {
			this.setTitleShadowColor(normalAttributes.shadowColor, State.NORMAL);
		} else {
			this.setTitleShadowColor(Color.white(0.0f, 0.5f), State.NORMAL);
		}

		State[] highlightedStates = new State[] { State.NORMAL, State.HIGHLIGHTED };

		TextAttributes highlightedAttributes = item.getTitleTextAttributesForState(highlightedStates);

		if(highlightedAttributes != null && highlightedAttributes.textColor != 0) {
			this.setTitleColor(highlightedAttributes.textColor, highlightedStates);
		}

		if(highlightedAttributes != null && highlightedAttributes.shadowColor != 0) {
			this.setTitleShadowColor(highlightedAttributes.shadowColor, highlightedStates);
		}

		this.setTitle(item.getTitle(), State.NORMAL);

		if(item.getStyle() != BarButtonItem.Style.PLAIN) {
			this.setContentEdgeInsets(new EdgeInsets(0.0f, back ? 15.0f : 7.0f, 0.0f, 7.0f));

			EdgeInsets capInsets;
			Image backgroundImage;
			Image highlightedBackgroundImage;

			if(!back) {
				this.setImage(item.getImage(), State.NORMAL);

				backgroundImage = item.getBackgroundImage(BarMetrics.DEFAULT, State.NORMAL);
				capInsets = new EdgeInsets(0.0f, 5.0f, 0.0f, 5.0f);

				if(backgroundImage != null) {
					highlightedBackgroundImage = item.getBackgroundImage(BarMetrics.DEFAULT, highlightedStates);
					this.backgroundImageVerticalAdjustment = item.getBackgroundVerticalPositionAdjustmentForBarMetrics(BarMetrics.DEFAULT);
				} else {
					backgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_button);
					highlightedBackgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_button_pressed);
				}

				this.setContentEdgeInsets(new EdgeInsets(0.0f, 9.0f, 0.0f, 9.0f));
				this.setImageEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));

			} else {
				backgroundImage = item.getBackButtonBackgroundImage(BarMetrics.DEFAULT, State.NORMAL);
				capInsets = new EdgeInsets(6.0f, 14.0f, 6.0f, 6.0f);

				if(backgroundImage != null) {
					highlightedBackgroundImage = item.getBackButtonBackgroundImage(BarMetrics.DEFAULT, highlightedStates);
					this.backgroundImageVerticalAdjustment = item.getBackButtonBackgroundVerticalPositionAdjustment(BarMetrics.DEFAULT);
				} else {
					backgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_back);
					highlightedBackgroundImage = Image.imageNamed(R.drawable.mocha_navigation_bar_default_back_pressed);
				}

				this.setContentEdgeInsets(new EdgeInsets(0.0f, 14.0f, 0.0f, 9.0f));
				this.setImageEdgeInsets(new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f));

			}

			if(backgroundImage.getCapInsets() == null) {
				backgroundImage = backgroundImage.resizableImageWithCapInsets(capInsets);
			}

			this.setBackgroundImage(backgroundImage, State.NORMAL);

			if(highlightedBackgroundImage != null) {
				if(highlightedBackgroundImage.getCapInsets() == null) {
					highlightedBackgroundImage = highlightedBackgroundImage.resizableImageWithCapInsets(capInsets);
				}

				this.setBackgroundImage(highlightedBackgroundImage, highlightedStates);
			}
		}

		Offset titlePositionAdjustment;
		EdgeInsets titleEdgeInsets = EdgeInsets.zero();

		if(!back) {
			titlePositionAdjustment = item.getTitlePositionAdjustment(BarMetrics.DEFAULT);
		} else {
			titlePositionAdjustment = item.getBackButtonTitlePositionAdjustment(BarMetrics.DEFAULT);
		}


		if(titlePositionAdjustment != null) {
			titleEdgeInsets.top += titlePositionAdjustment.vertical;
			titleEdgeInsets.left += titlePositionAdjustment.horizontal;

			titleEdgeInsets.bottom -= titlePositionAdjustment.vertical;
			titleEdgeInsets.right -= titlePositionAdjustment.horizontal;
		}

		this.setTitleEdgeInsets(titleEdgeInsets);
	}

	public Rect getTitleRectForContentRect(Rect contentRect) {
		Rect rect = super.getTitleRectForContentRect(contentRect);
		rect.origin.x -= 1.0f;
		rect.size.width += 2.0f;
		return rect;
	}

	public Rect getBackgroundRectForBounds(Rect bounds) {
		Rect rect = super.getBackgroundRectForBounds(bounds);
		rect.origin.y += this.backgroundImageVerticalAdjustment;
		return rect;
	}

	public Size sizeThatFits(Size size) {
		Size size1 = super.sizeThatFits(size);

		if(this.barButtonItem.getStyle() != BarButtonItem.Style.PLAIN) {
			size1.height = Math.min(30.0f, size1.height);
		}

		if(this.barButtonItem.getWidth() > 0.0f) {
			size1.width = Math.min(size.width, this.barButtonItem.getWidth());
		}

		return size1;
	}
}