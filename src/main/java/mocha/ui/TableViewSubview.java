package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Rect;

abstract public class TableViewSubview extends View {
	boolean isQueued;
	int section;

	// Only applies to cells
	int globalRow;
	IndexPath indexPath;

	// Only applies to headers/footers
	boolean createdByTableView;

	public TableViewSubview() {
		super();
	}

	public TableViewSubview(Rect frame) {
		super(frame);
	}

}
