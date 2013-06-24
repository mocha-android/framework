/**
 *  @author Shaun
 *  @date 5/29/13
 *  @copyright 2013 TV Guide, Inc. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;
import mocha.graphics.Size;
import mocha.graphics.TextDrawing;

class TableViewCellLayoutManagerSubtitle extends TableViewCellLayoutManager {

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

	Rect getTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
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

		Rect contentRect = this.getContentViewRectForCell(cell, style);
		Rect imageRect = this.getImageViewRectForCell(cell, style);

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

	Rect getDetailTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
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

		Rect contentRect = this.getContentViewRectForCell(cell, style);
		Rect imageRect = this.getImageViewRectForCell(cell, style);

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
