/**
 *  @author Shaun
 *  @date 5/2/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Point;

class TableViewSectionRowData {

	float headerHeight;
	float footerHeight;
	float headerOffset;
	float footerOffset;
	float[] rowHeights;
	float[] rowOffsets;
	float sectionHeight;
	float sectionOffset;
	int numberOfRows;

	TableViewSectionRowData() {

	}

	public void refresh(TableView tableView, int section, float sectionOffset) {
		this.sectionOffset = sectionOffset;

		TableView.Style style = tableView.getTableStyle();
		boolean groupedStyle = style == TableView.Style.GROUPED;
		boolean plainStyle = style == TableView.Style.PLAIN;

		//
		// Header
		//

		boolean headerSizing = tableView.delegateHeaders != null;
		boolean headerTitles = tableView.dataSourceHeaders != null;

		if(groupedStyle) {
			this.sectionOffset += TableView.GROUPED_TABLE_Y_MARGIN;
		}

		float offset = this.sectionOffset;

		if(headerSizing) {
			this.headerHeight = Math.max(tableView.delegateHeaders.getHeightForHeaderInSection(tableView, section), 0.0f);
		} else {
			this.headerHeight = 0.0f;
		}

		if(this.headerHeight == 0.0f) {
			if(headerTitles) {
				String title = tableView.dataSourceHeaders.getTitleForHeaderInSection(tableView, section);

				if(title != null) {
					if(plainStyle) {
						this.headerHeight = TableView.PLAIN_HEADER_HEIGHT;
					} else {
						this.headerHeight = TableViewHeader.Grouped.getHeight(title, tableView.getFrame().size.width);
					}
				} else {
					if(tableView.getTableStyle() == TableView.Style.GROUPED) {
						if(section == 0) {
							this.sectionOffset += TableView.GROUPED_TABLE_Y_MARGIN;
							offset += TableView.GROUPED_TABLE_Y_MARGIN;
						}
					}
				}
			}
		}

		this.headerOffset = offset;
		offset += this.headerHeight;

		//
		// Rows
		//


		boolean usesCustomRowHeights = tableView.delegateRowSizing != null;
		this.numberOfRows = tableView.dataSource.getNumberOfRowsInSection(tableView, section);
		this.rowHeights = new float[this.numberOfRows];
		this.rowOffsets = new float[this.numberOfRows];
		float defaultRowHeight = usesCustomRowHeights ? 0.0f : tableView.getRowHeight();

		for(int i = 0; i < this.numberOfRows; i++) {
			float rowHeight;
			IndexPath indexPath = IndexPath.withRowInSection(i, section);

			if(usesCustomRowHeights) {
				rowHeight = tableView.delegateRowSizing.getHeightForRowAtIndexPath(tableView, indexPath);
			} else {
				rowHeight = defaultRowHeight;
			}

			this.rowHeights[i] = rowHeight;
			this.rowOffsets[i] = offset;

			offset += rowHeight;
		}

		//
		// Footer
		//

		boolean footerSizing = tableView.delegateFooters != null;
		boolean footerTitles = tableView.dataSourceFooters != null;

		if(footerSizing) {
			this.footerHeight = Math.max(0.0f, tableView.delegateFooters.getHeightForFooterInSection(tableView, section));
		} else {
			this.footerHeight = 0.0f;
		}

		if(this.footerHeight == 0.0f) {
			if(footerTitles) {
				String title = tableView.dataSourceFooters.getTitleForFooterInSection(tableView, section);

				if(title != null) {
					if(plainStyle) {
						this.footerHeight = TableView.PLAIN_HEADER_HEIGHT;
					} else {
						this.footerHeight = TableViewFooter.getHeight(title, tableView.getFrame().size.width);
					}
				}
			}
		}

		this.footerOffset = offset;
		offset += this.footerHeight;

		//
		// Finish up
		//

		this.sectionHeight = offset - this.sectionOffset;
	}

	public void adjustSectionOffsetBy(float delta) {
		this.sectionOffset += delta;
		this.headerOffset += delta;
		this.footerOffset += delta;

		int numberOfRows = this.rowOffsets.length;

		for(int i = 0; i < numberOfRows; i++) {
			this.rowOffsets[i] += delta;
		}
	}

	public int getRowForPoint(Point point) {
		if(point.y < this.sectionOffset || point.y > this.sectionOffset + this.sectionHeight || this.rowHeights == null || this.rowHeights.length == 0) {
			return -1;
		} else {
			int numberOfRows = this.rowOffsets.length;

			for (int row = 0; row < numberOfRows; row++) {
				if(point.y < this.rowOffsets[row]) {
					return row - 1;
				}
			}

			if(point.y < this.footerOffset) {
				return numberOfRows - 1;
			} else {
				return -1;
			}
		}
	}

	public boolean isValidRow(int row) {
		return row >= 0 && row < this.rowOffsets.length;
	}

}
