/**
 *  @author Shaun
 *  @date 11/20/12
 *  @copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Rect;
import mocha.graphics.Size;
import mocha.graphics.TextDrawing;

abstract class TableViewCellLayoutManager extends mocha.foundation.Object {
	private static TableViewCellLayoutManager DEFAULT = new Default();
	private static TableViewCellLayoutManager SUBTITLE = new Subtitle();
	private static TableViewCellLayoutManager CUSTOM = new Custom();

	public static TableViewCellLayoutManager getLayoutManagerForTableViewCellStyle(TableViewCell.Style style) {
		switch (style) {
			case DEFAULT:
				return DEFAULT;
			case VALUE_1: // TODO
			case VALUE_2: // TODO
			case SUBTITLE:
				return SUBTITLE;
			case CUSTOM:
				return CUSTOM;
		}

		if(style == TableViewCell.Style.SUBTITLE) {
			return SUBTITLE;
		} else {
			return DEFAULT;
		}
	}


	abstract Rect getContentViewRectForCell(TableViewCell cell);

	abstract Rect getAccessoryViewRectForCell(TableViewCell cell);

	abstract Rect getBackgroundViewRectForCell(TableViewCell cell);

	abstract Rect getSeparatorViewRectForCell(TableViewCell cell);

	abstract Rect getImageViewRectForCell(TableViewCell cell);

	abstract Rect getTextLabelRectForCell(TableViewCell cell);

	abstract Rect getDetailTextLabelRectForCell(TableViewCell cell);

	static class Default extends TableViewCellLayoutManager {
		Rect getContentViewRectForCell(TableViewCell cell) {
			// Collect pertinent information
			Rect accessoryRect = this.getAccessoryViewRectForCell(cell);
			Rect separatorRect = this.getSeparatorViewRectForCell(cell);
			float accessoryPadding = this._accessoryViewPaddingForCell(cell);
			Rect bounds = cell.getBounds();

			float width = bounds.size.width - accessoryRect.size.width - accessoryPadding;
			float height = bounds.size.height - separatorRect.size.height;

			return new Rect(0.0f, 0.0f, width, height);
		}

		float _accessoryViewPaddingForCell(TableViewCell cell) {
			View accessoryView = cell.getActualAccessoryView();
			if (null == accessoryView) {
				return 0.0f;
			}

			// NOTE: We can do this only because the SIZE of the accessory view
			// never changes, even though the origin might.
			Size accessorySize = cell.getActualAccessoryView().getBounds().size;
			Rect cellBounds = cell.getBounds();
			Rect separatorRect = this.getSeparatorViewRectForCell(cell);

			// Padding is ALWAYS 10 px on the left, but is the LESSER 
			// (including negative numbers) of 10.0 or the height difference,
			// on the right, top, and bottom
			float heightDifference = (float)Math.floor((cellBounds.size.height - separatorRect.size.height - accessorySize.height) / 2.0);

			return heightDifference < 10.0f ? heightDifference : 10.0f;
		}

		Rect getAccessoryViewRectForCell(TableViewCell cell) {
			if(cell.getActualAccessoryView() == null) return Rect.zero();

			View accessoryView = cell.getAccessoryView();

			Rect separatorRect = this.getSeparatorViewRectForCell(cell);
			Rect bounds = cell.getBounds();
			Rect cellBounds = new Rect(bounds.origin, new Size(bounds.size.width, bounds.size.height - separatorRect.size.height));

			// Custom accessory view always wins
			if (accessoryView != null) {
				Size accessorySize = accessoryView.sizeThatFits(cellBounds.size);

				// Provide a rect from the right-hand side of the cell,
				// with the frame centered in the cell

				float padding = this._accessoryViewPaddingForCell(cell);
				float x = cellBounds.size.width - accessorySize.width - padding;
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

					return new Rect(cellBounds.size.width - width, 0.0f, width, cellBounds.size.height);

				default:
					return Rect.zero();
			}
		}

		Rect getBackgroundViewRectForCell(TableViewCell cell) {
			Rect separatorRect = this.getSeparatorViewRectForCell(cell);
			return new Rect(0.0f, 0.0f, cell.getBounds().size.width, cell.getBounds().size.height - separatorRect.size.height);
		}

		Rect getSeparatorViewRectForCell(TableViewCell cell) {
			if(cell.getSeparatorStyle() != TableViewCell.SeparatorStyle.NONE) {
				Rect bounds  = cell.getBounds();
				return new Rect(0.0f, bounds.size.height - 1.0f, bounds.size.width, 1.0f);
			} else {
				return Rect.zero();
			}
		}

		Rect getImageViewRectForCell(TableViewCell cell) {
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
			Rect separatorRect = this.getSeparatorViewRectForCell(cell);
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

		Rect getTextLabelRectForCell(TableViewCell cell) {
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

			Rect contentRect = this.getContentViewRectForCell(cell);
			Rect imageRect = this.getImageViewRectForCell(cell);

			float originX = 0.0f;
			float width = 0.0f;

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

		Rect getDetailTextLabelRectForCell(TableViewCell cell) {
			return Rect.zero();
		}
	}

	static class Subtitle extends Default {

		private Size _combinedLabelsSizeForCell(TableViewCell cell, Size tvSize, Size dvSize) {
			Label textLabel = cell.getTextLabel(false);
			Label detailTextLabel = cell.getDetailTextLabel(false);

			if (textLabel == null && detailTextLabel == null) {
				return Size.zero();
			}

			if(tvSize == null) tvSize = Size.zero();
			if(dvSize == null) dvSize = Size.zero();

			if(textLabel != null && textLabel.getText() != null && textLabel.getText().length() > 0) {
				tvSize.set(TextDrawing.getTextSize(textLabel.getText(), textLabel.getFont()));
			}

			if(detailTextLabel != null && detailTextLabel.getText() != null && detailTextLabel.getText().length() > 0) {
				dvSize.set(TextDrawing.getTextSize(detailTextLabel.getText(), detailTextLabel.getFont()));
			}

			return new Size(tvSize.width > dvSize.width ? tvSize.width : dvSize.width, tvSize.height + dvSize.height);
		}

		Rect getTextLabelRectForCell(TableViewCell cell) {
			Label textLabel = cell.getTextLabel(false);
			
			if (textLabel == null) {
				return Rect.zero();
			}

			// RULES
			// =======
			// 10 pixel padding from the image rect or if no image rect, 10 pixel in
			// origin.x, unlike default, has no max, but is inf if past the accessory view frame edge

			// size.height always == original height
			// origin.y always == (cv.height - (tv.height + dv.height)) / 2

			// The allowable width is always from the right side of the content rect - 10 (padding)
			// to the greater of the end of the content rect OR the final bounds of the image view - 10 (padding)

			Size originalSize = new Size();
			Size combinedSize = this._combinedLabelsSizeForCell(cell, originalSize, null);

			Rect contentRect = this.getContentViewRectForCell(cell);
			Rect imageRect = this.getImageViewRectForCell(cell);

			float originX = 0.0f;
			float width = 0.0f;

			//float maxXOrigin = contentRect.size.width - 10.0f;
			float imageRectLastX = imageRect.origin.x + imageRect.size.width + 20.0f;

			if (imageRectLastX > contentRect.size.width) {
				originX = Float.MAX_VALUE;
				width = 0.0f;
			} else {
				originX = imageRectLastX - 10.0f;

				float maxWidth = contentRect.size.width - originX - 10.0f;
				width = maxWidth <= originalSize.width ? maxWidth : originalSize.width;
			}

			float originY = (float)Math.round((contentRect.size.height - combinedSize.height) / 2.0);
			return new Rect(originX, originY, width, originalSize.height);
		}

		Rect getDetailTextLabelRectForCell(TableViewCell cell) {
			Label detailTextLabel = cell.getDetailTextLabel(false);
			if (detailTextLabel == null) {
				return Rect.zero();
			}

			// RULES
			// =======
			// 10 pixel padding from the image rect or if no image rect, 10 pixel in
			// origin.x, unlike default, has no max, but is inf if past the accessory view frame edge

			// size.height always == original height
			// origin.y always == (cv.height - (tv.height + dv.height)) / 2

			// The allowable width is always from the right side of the content rect - 10 (padding)
			// to the greater of the end of the content rect OR the final bounds of the image view - 10 (padding)

			Size originalSize = new Size();
			Size combinedSize = this._combinedLabelsSizeForCell(cell, null, originalSize);

			Rect contentRect = this.getContentViewRectForCell(cell);
			Rect imageRect = this.getImageViewRectForCell(cell);

			float originX = 0.0f;
			float width = 0.0f;

			//float maxXOrigin = contentRect.size.width - 10.0f;
			float imageRectLastX = imageRect.origin.x + imageRect.size.width + 20.0f;

			if (imageRectLastX > contentRect.size.width) {
				originX = Float.MAX_VALUE;
				width = 0.0f;
			} else {
				originX = imageRectLastX - 10.0f;

				float maxWidth = contentRect.size.width - originX - 10.0f;
				width = maxWidth <= originalSize.width ? maxWidth : originalSize.width;
			}

			float originY = (float)Math.round(((contentRect.size.height - combinedSize.height) / 2.0) + (combinedSize.height - originalSize.height));
			return new Rect(originX, originY, width, originalSize.height);
		}
	}

	static class Custom extends Default {

		Rect getAccessoryViewRectForCell(TableViewCell cell) {
			return Rect.zero();
		}

		Rect getImageViewRectForCell(TableViewCell cell) {
			return Rect.zero();
		}

		Rect getTextLabelRectForCell(TableViewCell cell) {
			return Rect.zero();
		}

		Rect getDetailTextLabelRectForCell(TableViewCell cell) {
			return Rect.zero();
		}

	}

}
