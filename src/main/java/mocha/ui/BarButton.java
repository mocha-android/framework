/**
 *	@author Shaun
 *	@date 1/30/13
 *	@copyright	2015 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

class BarButton extends Button {
	private float backgroundImageVerticalAdjustment = 0.0f;
	private final BarButtonItem barButtonItem;
	private boolean bordered;

	static BarButton button(final BarButtonItem barButtonItem) {
		BarButtonItem.SystemItem systemItem = barButtonItem.getSystemItem();

		if(systemItem == BarButtonItem.SystemItem.FIXED_SPACE || systemItem == BarButtonItem.SystemItem.FLEXIBLE_SPACE) {
			return null;
		} else {
			BarButton button = new BarButton(barButtonItem);
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

	private BarButton(BarButtonItem item) {
		this.barButtonItem = item;
		this.setContentMode(ContentMode.CENTER);

		TextAttributes normalAttributes = item.getTitleTextAttributesForState(State.NORMAL);
		final float fontSize = item.getStyle() == BarButtonItem.Style.BORDERED ? 12.0f : 18.0f;

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

		BarButtonItem.SystemItem systemItem = barButtonItem.getSystemItem();

		if(systemItem != null) {
			if(systemItem == BarButtonItem.SystemItem.CANCEL) {
				this.setTitle("Cancel", State.NORMAL);
			} else if(systemItem == BarButtonItem.SystemItem.EDIT) {
				this.setTitle("Edit", State.NORMAL);
			} else {
				int imageId = -1;

				switch (systemItem) {
					case DONE:
						imageId = R.drawable.mocha_bar_button_system_item_done;
						break;
					case SAVE:
						imageId = R.drawable.mocha_bar_button_system_item_save;
						break;
					case ADD:
						imageId = R.drawable.mocha_bar_button_system_item_save;
						break;
					case COMPOSE:
						imageId = R.drawable.mocha_bar_button_system_item_compose;
						break;
					case REPLY:
						imageId = R.drawable.mocha_bar_button_system_item_reply;
						break;
					case ACTION:
						imageId = R.drawable.mocha_bar_button_system_item_action;
						break;
					case ORGANIZE:
						imageId = R.drawable.mocha_bar_button_system_item_organize;
						break;
					case BOOKMARKS:
						imageId = R.drawable.mocha_bar_button_system_item_bookmarks;
						break;
					case SEARCH:
						imageId = R.drawable.mocha_bar_button_system_item_search;
						break;
					case REFRESH:
						imageId = R.drawable.mocha_bar_button_system_item_refresh;
						break;
					case STOP:
						imageId = R.drawable.mocha_bar_button_system_item_stop;
						break;
					case CAMERA:
						imageId = R.drawable.mocha_bar_button_system_item_camera;
						break;
					case TRASH:
						imageId = R.drawable.mocha_bar_button_system_item_trash;
						break;
					case PLAY:
						imageId = R.drawable.mocha_bar_button_system_item_play;
						break;
					case PAUSE:
						imageId = R.drawable.mocha_bar_button_system_item_pause;
						break;
					case REWIND:
						imageId = R.drawable.mocha_bar_button_system_item_rewind;
						break;
					case FAST_FORWARD:
						imageId = R.drawable.mocha_bar_button_system_item_fast_forward;
						break;
					case UNDO:
						imageId = R.drawable.mocha_bar_button_system_item_undo;
						break;
					case REDO:
						imageId = R.drawable.mocha_bar_button_system_item_redo;
						break;
				}

				if(imageId != -1) {
					Image image = Image.imageNamed(imageId).imageWithRenderingMode(Image.RenderingMode.ALWAYS_TEMPLATE);
					this.setImage(image, State.NORMAL);

					EdgeInsets imageInset = item.getImageInsets();

					if(imageInset != null) {
						this.setImageEdgeInsets(imageInset);
					}
				}
			}
		} else if(item.getTitle() != null && item.getTitle().length() > 0) {
			this.setTitle(item.getTitle(), State.NORMAL);
		} else if(item.getImage() != null) {
			Image image = item.getImage();

			if(image.getRenderingMode() == Image.RenderingMode.AUTOMATIC) {
				image = image.imageWithRenderingMode(Image.RenderingMode.ALWAYS_TEMPLATE);
			}

			this.setImage(image, State.NORMAL);

			EdgeInsets imageInset = item.getImageInsets();

			if(imageInset != null) {
				this.setImageEdgeInsets(imageInset);
			}
		}

		if(item.getStyle() == BarButtonItem.Style.BORDERED) {
			this.setContentEdgeInsets(new EdgeInsets(0.0f, 7.0f, 0.0f, 7.0f));
			this.bordered = true;

			EdgeInsets capInsets;
			Image backgroundImage = null;
			Image highlightedBackgroundImage = null;

			backgroundImage = item.getBackgroundImage(BarMetrics.DEFAULT, State.NORMAL);
			capInsets = new EdgeInsets(0.0f, 5.0f, 0.0f, 5.0f);

			if(backgroundImage != null) {
				highlightedBackgroundImage = item.getBackgroundImage(BarMetrics.DEFAULT, highlightedStates);
				this.backgroundImageVerticalAdjustment = item.getBackgroundVerticalPositionAdjustmentForBarMetrics(BarMetrics.DEFAULT);
			}

			this.setContentEdgeInsets(new EdgeInsets(0.0f, 9.0f, 0.0f, 9.0f));

			if(backgroundImage != null && backgroundImage.getCapInsets() == null) {
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

		titlePositionAdjustment = item.getTitlePositionAdjustment(BarMetrics.DEFAULT);


		if(titlePositionAdjustment != null) {
			titleEdgeInsets.top += titlePositionAdjustment.vertical;
			titleEdgeInsets.left += titlePositionAdjustment.horizontal;

			titleEdgeInsets.bottom -= titlePositionAdjustment.vertical;
			titleEdgeInsets.right -= titlePositionAdjustment.horizontal;
		}

		this.setTitleEdgeInsets(titleEdgeInsets);
	}

	public boolean isBordered() {
		return bordered;
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

		if (this.barButtonItem.getStyle() != BarButtonItem.Style.PLAIN) {
			size1.height = Math.min(size.height, size1.height);
		}

		if (this.barButtonItem.getWidth() > 0.0f) {
			size1.width = Math.min(size.width, this.barButtonItem.getWidth());
		}

		return size1;
	}

	public boolean pointInside(Point point, Event event) {
		return this.getBounds().inset(-10.0f, -10.0f).contains(point);
	}

}