/*
 *  @author Shaun
 *	@date 2/5/13
 *	@copyright	2013 enormego. All rights reserved.
 */

package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Rect;
import mocha.graphics.Size;

abstract public class TableViewSubview extends View {
	boolean _isQueued;
	Info _dataSourceInfo;

	// Only applies to headers/footers
	boolean createdByTableView;

	public TableViewSubview() {
		super();
	}

	public TableViewSubview(Rect frame) {
		super(frame);
	}

	static class Info {
		enum Type {
			CELL, FOOTER, HEADER
		}

		Type type;
		int section;

		// Only applies to cells
		IndexPath indexPath;

		Info(Type type, int section) {
			this.type = type;
			this.section = section;
		}

		Info(Type type, IndexPath indexPath) {
			this.type = type;
			this.section = indexPath.section;
			this.indexPath = indexPath;
		}
	}
}
