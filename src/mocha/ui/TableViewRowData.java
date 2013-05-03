/**
 *  @author Shaun
 *  @date 5/2/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.util.SparseArray;
import mocha.foundation.IndexPath;
import mocha.foundation.MObject;
import mocha.foundation.Range;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TableViewRowData {
	private TableView tableView;
	private TableViewSectionRowData[] sectionRowData;
	private float tableHeaderHeight;
	private float tableFooterHeight;
	private float tableWidth;
	private int numberOfRows;

	private int lastGetGlobalRowsMiddleRow;
	private int lastGetSectionsMiddleSection;
	private SparseArray<Rect> cachedGlobalRects;
	private SparseArray<Rect> cachedSectionRects;

	TableViewRowData(TableView tableView) {
		this.tableView = tableView;
		this.tableViewWidthDidChangeToWidth(this.tableView.getFrame().size.width);
		this.sectionRowData = new TableViewSectionRowData[0];
		this.lastGetGlobalRowsMiddleRow = -1;
		this.cachedGlobalRects = new SparseArray<Rect>();
		this.cachedSectionRects = new SparseArray<Rect>();
	}

	public boolean isEmpty() {
		return this.sectionRowData.length < 1 || (this.sectionRowData.length == 1 && this.sectionRowData[0].numberOfRows == 0);
	}

	public boolean isValidSection(int section) {
		return section >= 0 && section < this.sectionRowData.length;
	}

	public boolean isValidIndexPath(IndexPath indexPath) {
		return this.isValidSection(indexPath.section) && this.sectionRowData[indexPath.section].isValidRow(indexPath.row);
	}

	public void reloadData() {
		if(this.tableView.dataSource == null) {
			this.sectionRowData = new TableViewSectionRowData[0];
			return;
		}

		int numberOfSections = Math.max(0, this.tableView.dataSource.getNumberOfSections(this.tableView));

		if(this.sectionRowData.length == 0) {
			this.sectionRowData = new TableViewSectionRowData[numberOfSections];
		} else if(this.sectionRowData.length != numberOfSections) {
			this.sectionRowData = Arrays.copyOf(this.sectionRowData, numberOfSections);
		}

		View view;

		if((view = this.tableView.getTableHeaderView()) != null) {
			this.tableHeaderHeight = view.getFrame().size.height;
		}

		if((view = this.tableView.getTableFooterView()) != null) {
			this.tableFooterHeight = view.getFrame().size.height;
		}

		float offset = this.tableHeaderHeight;
		this.numberOfRows = 0;

		for(int section = 0; section < numberOfSections; section++) {
			TableViewSectionRowData rowData = this.sectionRowData[section];

			if(rowData == null) {
				rowData = new TableViewSectionRowData();
				this.sectionRowData[section] = rowData;
			}

			rowData.refresh(this.tableView, section, offset);
			offset = rowData.sectionOffset + rowData.sectionHeight;
			this.numberOfRows += rowData.numberOfRows;
		}

		this.lastGetGlobalRowsMiddleRow = -1;
		this.cachedGlobalRects.clear();
		this.cachedSectionRects.clear();
	}

	public Range getGlobalRowsInRect(Rect visibleRect) {
		int minRow = 0;
		int maxRow = this.numberOfRows - 1;

		int startAt = 0;

		if(visibleRect.origin.y > 0.0f) {
			if(this.lastGetGlobalRowsMiddleRow != -1 && visibleRect.intersects(this.getRectForGlobalRow(this.lastGetGlobalRowsMiddleRow))) {
				startAt = this.lastGetGlobalRowsMiddleRow;
			} else {
				int reference = this.numberOfRows / 2;

				while(reference > 0 && reference < this.numberOfRows) {
					Rect rect = this.getRectForGlobalRow(reference);

					if(rect.intersects(visibleRect)) {
						startAt = reference;
						break;
					} else {
						int delta;

						if(rect.maxY() < visibleRect.origin.y) {
							minRow = reference;
							delta = ((maxRow - reference) / 2);
						} else {
							maxRow = reference;
							delta = -((reference - minRow) / 2);
						}

						if(delta == 0) {
							startAt = reference;
							break;
						} else {
							reference += delta;
						}
					}
				}
			}
		}

		minRow = 0;

		for(int globalRow = startAt; globalRow > 0; globalRow--) {
			Rect rect = this.getRectForGlobalRow(globalRow);
			if(rect.origin.y < visibleRect.origin.y) {
				minRow = globalRow;
				break;
			}
		}

		maxRow = this.numberOfRows - 1;

		for(int globalRow = startAt; globalRow < this.numberOfRows; globalRow++) {
			Rect rect = this.getRectForGlobalRow(globalRow);
			if(rect.maxY() > visibleRect.maxY()) {
				maxRow = globalRow;
				break;
			}
		}

		this.lastGetGlobalRowsMiddleRow = minRow + ((maxRow - minRow) / 2);

		return new Range(minRow, (maxRow - minRow) + 1);
	}

	private int[] getSectionRowForGlobalRow(int globalRow) {
		int numberOfSections = this.getNumberOfSections();

		if(numberOfSections == 0) {
			return new int[] { -1, -1 };
		}
		if(numberOfSections == 1) {
			return new int[] { 0, globalRow };
		} else {
			int section;

			for(section = 0; section < numberOfSections; section++) {
				int numberOfRows = this.sectionRowData[section].numberOfRows;
				if(globalRow >= numberOfRows) {
					globalRow -= numberOfRows;
				} else {
					break;
				}
			}

			return new int[] { section, globalRow };
		}
	}

	public Rect getRectForGlobalRow(int globalRow) {
		Rect rect = this.cachedGlobalRects.get(globalRow);

		if(rect == null) {
			int[] sectionRow = this.getSectionRowForGlobalRow(globalRow);
			rect = this.getRectForRow(sectionRow[0], sectionRow[1]);
			this.cachedGlobalRects.put(globalRow, rect);
		}

		rect.size.width = tableWidth;

		return rect;
	}

	public Range getSectionsInRect(Rect visibleRect) {
		int numberOfSections = this.sectionRowData.length;

		if(numberOfSections < 2) {
			return new Range(0, numberOfSections);
		}

		int minSection = 0;
		int maxSection = numberOfSections - 1;

		int startAt = 0;

		if(visibleRect.origin.y > 0.0f) {
			if(this.lastGetSectionsMiddleSection != -1 && visibleRect.intersects(this.getRectForSection(this.lastGetSectionsMiddleSection))) {
				startAt = this.lastGetSectionsMiddleSection;
			} else {
				int reference = numberOfSections / 2;

				while(reference > 0 && reference < numberOfSections) {
					Rect rect = this.getRectForSection(reference);

					if(rect.intersects(visibleRect)) {
						startAt = reference;
						break;
					} else {
						int delta;

						if(rect.maxY() < visibleRect.origin.y) {
							minSection = reference;
							delta = ((maxSection - reference) / 2);
						} else {
							maxSection = reference;
							delta = -((reference - minSection) / 2);
						}

						if(delta == 0) {
							startAt = reference;
							break;
						} else {
							reference += delta;
						}
					}
				}
			}
		}

		minSection = 0;

		for(int section = startAt; section > 0; section--) {
			Rect rect = this.getRectForSection(section);
			if(rect.origin.y < visibleRect.origin.y) {
				minSection = section;
				break;
			}
		}

		maxSection = numberOfSections - 1;

		for(int section = startAt; section < numberOfSections; section++) {
			Rect rect = this.getRectForSection(section);
			if(rect.maxY() > visibleRect.maxY()) {
				maxSection = section;
				break;
			}
		}

		this.lastGetSectionsMiddleSection = minSection + ((maxSection - minSection) / 2);

		return new Range(minSection, (maxSection - minSection) + 1);
	}

	public int getSectionAtPoint(Point point) {
		int numberOfSections = this.getNumberOfSections();

		for(int section = 0; section < numberOfSections; section++) {
			if(point.y <= this.sectionRowData[section].sectionOffset) {
				return section - 1;
			}
		}

		if(numberOfSections > 0) {
			TableViewSectionRowData lastSection = this.sectionRowData[numberOfSections - 1];

			if(point.y < lastSection.sectionOffset + lastSection.sectionHeight) {
				return numberOfSections - 1;
			}
		}

		return -1;
	}

	public List<IndexPath> getIndexPathsForRowsInRect(Rect rect) {
		List<IndexPath> indexPaths = new ArrayList<IndexPath>();

		int start = this.getSectionAtPoint(rect.origin);

		if(start != -1) {
			int numberOfSections = this.sectionRowData.length;
			Point point = rect.origin.copy();
			float max = rect.maxY();

			for(int section = start; section < numberOfSections; section++) {
				TableViewSectionRowData sectionRowData = this.sectionRowData[section];

				int startRow = Math.max(0, sectionRowData.getRowForPoint(point));
				int numberOfRows = sectionRowData.rowOffsets.length;

				boolean stop = false;

				for(int row = startRow; row < numberOfRows; row++) {
					float offset = sectionRowData.rowOffsets[row];

					if(offset >= point.y) {
						if(offset > max) {
							stop = true;
							break;
						} else {
							indexPaths.add(IndexPath.withRowInSection(row, section));
						}
					}
				}

				if(stop) {
					break;
				}
			}
		}

		return indexPaths;
	}

	public IndexPath getIndexPathForRowAtPoint(Point point) {
		int section = this.getSectionAtPoint(point);

		if(section == -1) {
			return null;
		} else {
			int row = this.sectionRowData[section].getRowForPoint(point);

			if(row == -1) {
				return null;
			} else {
				return IndexPath.withRowInSection(row, section);
			}
		}
	}

	public IndexPath getIndexPathForRowAtGlobalRow(int globalRow) {
		int[] sectionRow = this.getSectionRowForGlobalRow(globalRow);
		return IndexPath.withRowInSection(sectionRow[1], sectionRow[0]);
	}

	public Rect getRectForRow(int section, int row) {
		if(section < 0 || section >= this.sectionRowData.length || row < 0) {
			return Rect.zero();
		} else {
			TableViewSectionRowData sectionRowData = this.sectionRowData[section];

			if(row < sectionRowData.numberOfRows) {
				Rect rect = new Rect();
				rect.origin.x = 0.0f;
				rect.origin.y = sectionRowData.rowOffsets[row];
				rect.size.width = this.tableWidth;
				rect.size.height = sectionRowData.rowHeights[row];

				return rect;
			} else {
				return Rect.zero();
			}
		}
	}

	public float getTableHeight() {
		int numberOfSections = this.getNumberOfSections();

		if(numberOfSections > 0) {
			TableViewSectionRowData lastSection = this.sectionRowData[numberOfSections - 1];
			return lastSection.sectionOffset + lastSection.sectionHeight + this.tableFooterHeight;
		} else {
			return this.tableHeaderHeight + this.tableFooterHeight;
		}
	}

	public Rect getRectForFooterInSection(int section) {
		if(section < 0 || section >= this.sectionRowData.length) {
			return Rect.zero();
		} else {
			TableViewSectionRowData sectionRowData = this.sectionRowData[section];

			Rect rect = new Rect();
			rect.origin.x = 0.0f;
			rect.origin.y = sectionRowData.footerOffset;
			rect.size.width = this.tableWidth;
			rect.size.height = sectionRowData.footerHeight;

			return rect;
		}
	}

	public Rect getFloatingRectForHeaderInSection(int section, Rect bounds) {
		TableViewSectionRowData sectionRowData = this.sectionRowData[section];

		Rect rect = this.getRectForHeaderInSection(section);
		float headerMaxY = sectionRowData.footerOffset;

		if(bounds.origin.y >= sectionRowData.headerOffset && bounds.origin.y < headerMaxY) {
			if(bounds.origin.y + sectionRowData.headerHeight > headerMaxY) {
				// Being pushed out	of the viewport
				rect.origin.y = headerMaxY - sectionRowData.headerHeight;
			} else {
				// Normal, floating within bounds, pinned to the top of the viewport
				rect.origin.y = bounds.origin.y;
			}
		}

		// else: Not floating yet or not on screen, leave as is

		return rect;
	}

	public Rect getRectForHeaderInSection(int section) {
		if(section < 0 || section >= this.sectionRowData.length) {
			return Rect.zero();
		} else {
			TableViewSectionRowData sectionRowData = this.sectionRowData[section];

			Rect rect = new Rect();
			rect.origin.x = 0.0f;
			rect.origin.y = sectionRowData.headerOffset;
			rect.size.width = this.tableWidth;
			rect.size.height = sectionRowData.headerHeight;

			return rect;
		}
	}

	public Rect getRectForSection(int section) {
		if(section < 0 || section >= this.sectionRowData.length) {
			return Rect.zero();
		} else {
			Rect rect = this.cachedSectionRects.get(section);

			if(rect == null) {
				TableViewSectionRowData sectionRowData = this.sectionRowData[section];

				rect = new Rect();
				rect.origin.x = 0.0f;
				rect.origin.y = sectionRowData.sectionOffset;
				rect.size.height = sectionRowData.sectionHeight;

				this.cachedSectionRects.put(section, rect);
			}

			rect.size.width = this.tableWidth;

			return rect;
		}
	}

	public Rect getRectForTable() {
		Rect rect = new Rect();
		rect.origin.x = 0.0f;
		rect.origin.y = 0.0f;
		rect.size.width = this.tableWidth;
		rect.size.height = this.getTableHeight();

		return rect;

	}

	public Rect getRectForTableFooterView() {
		Rect rect = new Rect();
		rect.origin.x = 0.0f;
		rect.size.width = this.tableWidth;
		rect.size.height = this.tableFooterHeight;

		int numberOfSections = this.getNumberOfSections();

		if(numberOfSections > 0) {
			TableViewSectionRowData lastSection = this.sectionRowData[numberOfSections - 1];
			rect.origin.y = lastSection.sectionOffset + lastSection.sectionHeight;
		} else {
			rect.origin.y = this.tableHeaderHeight;
		}

		return rect;
	}

	public Rect getRectForTableHeaderView() {
		Rect rect = new Rect();
		rect.origin.x = 0.0f;
		rect.origin.y = 0.0f;
		rect.size.width = this.tableWidth;
		rect.size.height = this.tableHeaderHeight;

		return rect;
	}

	public float getHeightForRow(int section, int row) {
		return this.sectionRowData[section].rowHeights[row];
	}

	public float getHeightForFooterInSection(int section) {
		return this.sectionRowData[section].footerHeight;
	}

	public float getHeightForHeaderInSection(int section) {
		return this.sectionRowData[section].headerHeight;
	}

	public float getHeightForSection(int section) {
		return this.sectionRowData[section].sectionHeight;
	}

	public float getHeightForTableFooterView() {
		return this.tableFooterHeight;
	}

	public float getHeightForTableHeaderView() {
		return this.tableHeaderHeight;
	}

	public boolean hasFooterForSection(int section) {
		return this.sectionRowData[section].footerHeight > 0.0f;
	}

	public boolean hasHeaderForSection(int section) {
		return this.sectionRowData[section].headerHeight > 0.0f;
	}

	public float getOffsetForSection(int section) {
		return this.sectionRowData[section].sectionOffset;
	}

	public float getOffsetForFooterInSection(int section) {
		return this.sectionRowData[section].footerOffset;
	}

	public float getOffsetForHeaderInSection(int section) {
		return this.sectionRowData[section].headerOffset;
	}

	public int getNumberOfRowsInSection(int section) {
		return this.sectionRowData[section].numberOfRows;
	}

	public int getNumberOfSections() {
		return this.sectionRowData.length;
	}

	public void tableFooterHeightDidChangeToHeight(float height) {
		this.tableFooterHeight = height;
	}

	public void tableHeaderHeightDidChangeToHeight(float height) {
		float delta = height - this.tableHeaderHeight;

		if(delta != 0.0f) {
			this.tableHeaderHeight = height;

			for(TableViewSectionRowData rowData : this.sectionRowData) {
				rowData.adjustSectionOffsetBy(delta);
			}
		}
	}

	public void tableViewWidthDidChangeToWidth(float width) {
		this.tableWidth = width;
	}

}
