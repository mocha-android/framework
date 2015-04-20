package mocha.ui;

import mocha.graphics.Rect;

class TableViewCellLayoutManagerCustom extends TableViewCellLayoutManager {

	Rect getAccessoryViewRectForCell(TableViewCell cell, TableView.Style style) {
		return Rect.zero();
	}

	Rect getImageViewRectForCell(TableViewCell cell, TableView.Style style) {
		return Rect.zero();
	}

	Rect getTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
		return Rect.zero();
	}

	Rect getDetailTextLabelRectForCell(TableViewCell cell, TableView.Style style) {
		return Rect.zero();
	}

}
