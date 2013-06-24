/**
 *  @author Shaun
 *  @date 11/20/12
 *  @copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;
import mocha.graphics.Image;
import mocha.graphics.Rect;
import mocha.graphics.Size;

class TableViewCellLayoutManager extends MObject {
	private static TableViewCellLayoutManager DEFAULT = new TableViewCellLayoutManager();
	private static TableViewCellLayoutManager SUBTITLE = new TableViewCellLayoutManagerSubtitle();
	private static TableViewCellLayoutManager VALUE_1 = new TableViewCellLayoutManagerValue1();
	private static TableViewCellLayoutManager VALUE_2 = new TableViewCellLayoutManagerValue2();
	private static TableViewCellLayoutManager CUSTOM = new TableViewCellLayoutManagerCustom();

	public static TableViewCellLayoutManager getLayoutManagerForTableViewCellStyle(TableViewCell.Style style) {
		switch (style) {
			case VALUE_1:
				return VALUE_1;
			case VALUE_2:
				return VALUE_2;
			case SUBTITLE:
				return SUBTITLE;
			case CUSTOM:
				return CUSTOM;
			case DEFAULT:
			default:
				return DEFAULT;
		}
	}

	protected static final float GROUPED_INSET = 10.0f;
	protected static final float EDIT_CONTROL_SIZE = 34.0f;

	protected final float separatorHeight;
	protected final float scale;

	TableViewCellLayoutManager() {
		this.scale = Screen.mainScreen().getScale();

		if(this.scale > 1.0f && this.scale < 2.0f) {
			if(this.scale > 1.5f) {
				this.separatorHeight = 2.0f / this.scale;
			} else {
				this.separatorHeight = 1.0f / this.scale;
			}
		} else {
			this.separatorHeight = 1.0f;
		}
	}

	Rect getContentViewRectForCell(TableViewCell cell, TableView.Style style) {
		// Collect pertinent information
		Rect accessoryRect = this.getAccessoryViewRectForCell(cell, style);
		Rect separatorRect = this.getSeparatorViewRectForCell(cell, style);
		float accessoryPadding = this.getAccessoryViewPaddingForCell(cell, style);
		Rect bounds = cell.getBounds();

		float width = bounds.size.width - accessoryRect.size.width - accessoryPadding;
		float height = bounds.size.height - separatorRect.size.height;
		float x = 0.0f;

		if(style == TableView.Style.GROUPED) {
			x += GROUPED_INSET;
			width -= GROUPED_INSET;

			if(accessoryRect.size.width == 0.0f) {
				width -= GROUPED_INSET;
			}
		}

		float indent = cell.getIndentationLevel() * cell.getIndentationWidth();
		x += indent;
		width -= indent;

		if(cell.getWillShowEditControl()) {
			x += EDIT_CONTROL_SIZE;
			width -= EDIT_CONTROL_SIZE;
		}

		Rect contentRect = new Rect(x, 0.0f, width, height);

		if(cell.getWillShowDeleteConfirmation()) {
			Rect deleteConfirmationButtonRect = this.getDeleteConfirmationButtonRectForCell(cell, style);
			float maxX = contentRect.maxX();

			if(maxX > deleteConfirmationButtonRect.origin.x) {
				float delta = maxX - deleteConfirmationButtonRect.origin.x;
				contentRect.origin.x -= delta;
			}
		}

		return contentRect;
	}

	Rect getDeleteConfirmationButtonRectForCell(TableViewCell cell, TableView.Style style) {
		Button button = cell.getDeleteConfirmationButton();

		if(button == null) {
			return null;
		} else {
			Rect backgroundRect = this.getBackgroundViewRectForCell(cell, style);

			Rect rect = new Rect();
			rect.size.width = button.sizeThatFits(backgroundRect.size).width;
			rect.size.height = backgroundRect.size.height;

			rect.origin.y = 0.0f;
			rect.origin.x = backgroundRect.size.width - rect.size.width;

			return rect;
		}
	}

	Rect getEditControlRectForCell(TableViewCell cell, TableView.Style style) {
		Button control = cell.getEditControl();

		if(control == null) {
			return null;
		} else {
			Rect backgroundRect = this.getBackgroundViewRectForCell(cell, style);

			Rect rect = new Rect();
			rect.size.height = Math.min(EDIT_CONTROL_SIZE, control.sizeThatFits(new Size(EDIT_CONTROL_SIZE, backgroundRect.size.height)).height);
			rect.size.width = EDIT_CONTROL_SIZE;

			rect.origin.y = (float)Math.floor((backgroundRect.size.height - rect.size.height) / 2.0f);

			if(cell.getWillShowDeleteConfirmation()) {
				rect.origin.x -= this.getDeleteConfirmationButtonRectForCell(cell, style).size.width;
			}

			return rect;
		}
	}

	private float getAccessoryViewPaddingForCell(TableViewCell cell, TableView.Style style) {
		View accessoryView = cell.getActiveAccessoryView();
		if (null == accessoryView) {
			return 0.0f;
		}

		// NOTE: We can do this only because the SIZE of the accessory view
		// never changes, even though the origin might.
		Size accessorySize = cell.getActiveAccessoryView().getBounds().size;
		Rect cellBounds = cell.getBounds();
		Rect separatorRect = this.getSeparatorViewRectForCell(cell, style);

		// Padding is ALWAYS 10 px on the left, but is the LESSER
		// (including negative numbers) of 10.0 or the height difference,
		// on the right, top, and bottom
		float heightDifference = (float)Math.floor((cellBounds.size.height - separatorRect.size.height - accessorySize.height) / 2.0);

		return heightDifference < 10.0f ? heightDifference : 10.0f;
	}

	Rect getAccessoryViewRectForCell(TableViewCell cell, TableView.Style style) {
		View accessoryView = cell.getActiveAccessoryView();
		View editingAccessoryView = cell.getActiveEditingAccessoryView();
		View activeAccessoryView = cell.isEditing() && editingAccessoryView != null ? editingAccessoryView : accessoryView;

		if(activeAccessoryView == null) return Rect.zero();

		Rect separatorRect = this.getSeparatorViewRectForCell(cell, style);
		Rect bounds = cell.getBounds();
		Rect cellBounds = new Rect(bounds.origin, new Size(bounds.size.width, bounds.size.height - separatorRect.size.height));

		if(style == TableView.Style.GROUPED) {
			cellBounds.inset(GROUPED_INSET, 0.0f);
		}

		// Custom accessory view always wins
		if (accessoryView != null) {
			Size accessorySize = accessoryView.sizeThatFits(cellBounds.size);

			// Provide a rect from the right-hand side of the cell,
			// with the frame centered in the cell

			float padding = this.getAccessoryViewPaddingForCell(cell, style);
			float x = cellBounds.origin.x + (cellBounds.size.width - accessorySize.width - padding);
			float y = (float)Math.round((cellBounds.size.height - accessorySize.height) / 2.0);

			return new Rect(x, y, accessorySize.width, accessorySize.height);
		}

		TableViewCell.AccessoryType accessoryType = cell.getAccessoryType();

		switch (accessoryType) {
			case NONE:
				return Rect.zero();

			case CHECKMARK:
			case DISCLOSURE_INDICATOR:
			case DETAIL_DISCLOSURE_BUTTON:
				float width = 30.0f;

				if (accessoryType == TableViewCell.AccessoryType.DETAIL_DISCLOSURE_BUTTON) {
					width = 33.0f;
				}

				return new Rect(cellBounds.origin.x + cellBounds.size.width - width, 0.0f, width, cellBounds.size.height);

			default:
				return Rect.zero();
		}
	}

	Rect getBackgroundViewRectForCell(TableViewCell cell, TableView.Style style) {
		Rect separatorRect = this.getSeparatorViewRectForCell(cell, style);

		float width = cell.getBounds().size.width;
		float x = 0.0f;

		if(style == TableView.Style.GROUPED) {
			x += GROUPED_INSET;
			width -= GROUPED_INSET + GROUPED_INSET;
		}

		return new Rect(x, 0.0f, width, cell.getBounds().size.height - separatorRect.size.height);
	}

	Rect getSeparatorViewRectForCell(TableViewCell cell, TableView.Style style) {
		if(cell.getSeparatorStyle() != TableViewCell.SeparatorStyle.NONE) {
			Rect bounds  = cell.getBounds();
			Rect rect =  new Rect(0.0f, bounds.size.height - this.separatorHeight, bounds.size.width, this.separatorHeight);

			if(style == TableView.Style.GROUPED) {
				rect.inset(GROUPED_INSET, 0.0f);
			}

			return rect;
		} else {
			return Rect.zero();
		}
	}

	Rect getImageViewRectForCell(TableViewCell cell, TableView.Style style) {
		ImageView imageView = cell.getImageView(false);
		Image image;

		if(imageView == null || (image = imageView.getImage()) == null) {
			return Rect.zero();
		}

		// Allows a maximum height of (cell.getBounds().height - 1) pixels.
		// If the image size is less, apply a padding that + image height = cell.getBounds().height px
		// THE IMAGE HEIGHT IS NEVER CONSTRAINED (tested in iOS)
		Size imageSize = image.getSize();
		Rect cellBounds = cell.getBounds();
		Rect separatorRect = this.getSeparatorViewRectForCell(cell, style);
		float maxHeight = cellBounds.size.height - separatorRect.size.height;

		if (imageSize.height < maxHeight) {
			// Image is not as tall as the cell
			float padding = (float)Math.floor((maxHeight - imageSize.height) / 2.0);
			return new Rect(padding < 0 ? 0 : padding, padding, imageSize.width, imageSize.height);
		} else if (imageSize.height == maxHeight) {
			// Image height == cell height
			return new Rect(0.0f, 0.0f, imageSize.width, imageSize.height);
		} else {
			// Image is taller than the cell
			float differencePercent = (maxHeight / imageSize.height);
			float width = (float)Math.round(imageSize.width * differencePercent);
			float height = (float)Math.round(imageSize.height * differencePercent);
			return new Rect(0.0f, 0.0f, width, height);
		}
	}

	Rect getTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
		Label textLabel = cell.getTextLabel(false);

		if (textLabel == null) {
			return Rect.zero();
		}

		// RULES
		// =======
		// 10 pixel padding from the image rect or if no image rect, 10 pixel in
		// origin.x is at max 10 pixels less than the content frame right bound

		// origin.y always == 0
		// size.height always == contentRect.size.height

		// The allowable width is always from the right side of the content rect - 10 (padding)
		// to the greater of the end of the content rect OR the final bounds of the image view - 10 (padding)

		Rect contentRect = this.getContentViewRectForCell(cell, style);
		Rect imageRect = this.getImageViewRectForCell(cell, style);

		float originX;
		float width;

		float maxXOrigin = contentRect.size.width - 10.0f;
		float imageRectLastX = imageRect.origin.x + imageRect.size.width + 10.0f;

		if (imageRectLastX > maxXOrigin) {
			originX = maxXOrigin;
			width = imageRectLastX - maxXOrigin;
		} else {
			originX = imageRectLastX;
			width = contentRect.size.width - originX - 10.0f;
		}

		return new Rect(originX, 0.0f, width, contentRect.size.height);
	}

	Rect getDetailTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
		return Rect.zero();
	}

}
