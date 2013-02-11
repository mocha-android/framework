/*
 *  @author Shaun
 *  @date 2/11/13
 *  @copyright  2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.IndexPath;

import java.util.Set;

abstract public class TableViewController extends ViewController implements TableView.DataSource, TableView.Delegate {
	private final TableView.Style style;
	private boolean clearsSelectionOnViewWillAppear;

	public TableViewController() {
		this(TableView.Style.PLAIN);
	}

	public TableViewController(TableView.Style style) {
		this.style = style == null ? TableView.Style.PLAIN : style;
		this.clearsSelectionOnViewWillAppear = true;
	}

	protected void loadView() {
		TableView tableView = new TableView(style);
		tableView.setDelegate(this);
		tableView.setDataSource(this);
		super.setView(tableView);
	}

	public void setView() {
		throw new RuntimeException("You can not set the view property on TableViewController");
	}

	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);

		if(this.getTableView().isEmpty()) {
			this.getTableView().reloadData();
		}

		if(this.clearsSelectionOnViewWillAppear) {
			Set<IndexPath> indexPaths = this.getTableView().indexPathsForSelectedRows();

			if(indexPaths.size() > 0) {
				for(IndexPath indexPath : indexPaths) {
					this.getTableView().deselectRowAtIndexPath(indexPath, animated);
				}
			}
		}
	}

	public void viewDidAppear(boolean animated) {
		super.viewDidAppear(animated);
		this.getTableView().flashScrollIndicators();
	}

	public TableView getTableView() {
		return (TableView)this.getView();
	}

	public boolean doesClearSelectionOnViewWillAppear() {
		return clearsSelectionOnViewWillAppear;
	}

	public void setClearsSelectionOnViewWillAppear(boolean clearsSelectionOnViewWillAppear) {
		this.clearsSelectionOnViewWillAppear = clearsSelectionOnViewWillAppear;
	}

	public int getNumberOfSections(TableView tableView) {
		return 1;
	}

	abstract public int getNumberOfRowsInSection(TableView tableView, int section);
	abstract public TableViewCell getCellForRowAtIndexPath(TableView tableView, IndexPath indexPath);

}
