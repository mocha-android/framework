/*
 *  @author Shaun
 *	@date 2/5/13
 *	@copyright	2013 enormego. All rights reserved.
 */

package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Rect;
import mocha.graphics.Size;

abstract public class TableViewReuseableView extends View {
	boolean _isQueued;
	Info _dataSourceInfo;


	public TableViewReuseableView() {
		super();
	}

	public TableViewReuseableView(Rect frame) {
		super(frame);
	}

	static class Info {
		enum Type {
			CELL, FOOTER, HEADER
		}

		Type type;
		int section;
		IndexPath indexPath;
		Size size = new Size();

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
