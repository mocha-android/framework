/*
 *  @author Shaun
 *	@date 2/5/13
 *	@copyright	2013 Mocha. All rights reserved.
 */

package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Rect;

abstract public class TableViewSubview extends View {
	boolean _isQueued;
	int _section;

	// Only applies to cells
	int _globalRow;
	IndexPath _indexPath;

	// Only applies to headers/footers
	boolean createdByTableView;

	public TableViewSubview() {
		super();
	}

	public TableViewSubview(Rect frame) {
		super(frame);
	}

}
