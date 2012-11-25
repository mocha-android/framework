/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import mocha.foundation.IndexPath;
import mocha.ui.TableView;
import mocha.ui.TableViewCell;
import mocha.ui.View;
import mocha.ui.ViewController;

public class TableViewController extends ViewController implements TableView.DataSource {
	private static final String cellIdentifier = "cellIdentifier";

	protected void loadView() {
		super.loadView();

		TableView tableView = new TableView(this.getView().getBounds());
		tableView.setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		tableView.setDataSource(this);
		this.getView().addSubview(tableView);
	}

	public int getNumberOfSections(TableView tableView) {
		return 5;
	}

	public int getNumberOfRowsInSection(TableView tableView, int section) {
		return 20;
	}

	public TableViewCell getCellForRowAtIndexPath(TableView tableView, IndexPath indexPath) {
		TableViewCell cell = tableView.dequeueReusableCellWithIdentifier(cellIdentifier);

		if (cell == null) {
			cell = new TableViewCell(TableViewCell.CellStyle.DEFAULT, cellIdentifier);
		}

		cell.getTextLabel().setText("Section: " + indexPath.getSection() + ", Row: " + indexPath.getRow());

		return cell;
	}

}
