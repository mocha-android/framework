/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.util.Log;
import mocha.foundation.IndexPath;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.*;

public class TableView extends ScrollView {
	public static final float DEFAULT_ROW_HEIGHT = 44.0f;
	public static final float DEFAULT_SECTION_HEADER_HEIGHT = 22.0f;
	public static final float DEFAULT_SECTION_FOOTER_HEIGHT = 22.0f;

	/// Interfaces

	/**
	 * DataSource + Delegate Note: A rather odd design pattern here, but without optional methods in Java, I couldn't think
	 * think of a better way to implement this without requiring a lot of boilerplate code in every implementation.
	 */
	public interface DataSource {
		// Required
		public int getNumberOfSections(TableView tableView);
		public int getNumberOfRowsInSection(TableView tableView, int section);
		public TableViewCell getCellForRowAtIndexPath(TableView tableView, IndexPath indexPath);

		// Optional additional interfaces to implement
		public interface Editing extends DataSource {
			public boolean canEditRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void commitEditingStyleForRowAtIndexPath(TableView tableView, TableViewCell.EditingStyle editingStyle, IndexPath indexPath);
		}

		public interface Headers extends DataSource {
			public String getTitleForHeaderInSections(TableView tableView, int section);
		}

		public interface Footers extends DataSource {
			public String getTitleForFooterInSections(TableView tableView, int section);
		}
	}

	public interface Delegate {
		// All Optional

		public interface RowSizing extends Delegate {
			public float getHeightForRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Selection extends Delegate {
			public IndexPath willSelectRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didSelectRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Deselection extends Delegate {
			public IndexPath willDeselectRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didDeselectRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Headers extends Delegate {
			public View getViewForHeaderInSecton(TableView tableView, int section);
			public float getHeightForHeaderInSection(TableView tableView, int section);
		}

		public interface Footers extends Delegate {
			public View getViewForFooterInSecton(TableView tableView, int section);
			public float getHeightForFooterInSection(TableView tableView, int section);
		}

		public interface Editing extends Delegate {
			public void willBeginEditingRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didEndEditingRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public CharSequence getTitleForDeleteConfirmationButtonForRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

	}

	/// Enums

	public enum Style {
		PLAIN, GROUPED
	}

	public enum ScrollPosition {
		NONE, TOP, MIDDLE, BOTTOM
	}

	public enum RowAnimation {
		FADE, RIGHT, LEFT, TOP, BOTTOM, NONE, MIDDLE
	}

	/// Properties

	private DataSource dataSource;
	private DataSource.Editing dataSourceEditing;
	private DataSource.Headers dataSourceHeaders;
	private DataSource.Footers dataSourceFooters;

	private Delegate delegate;
	private Delegate.RowSizing delegateRowSizing;
	private Delegate.Selection delegateSelection;
	private Delegate.Deselection delegateDeselection;
	private Delegate.Headers delegateHeaders;
	private Delegate.Footers delegateFooters;
	private Delegate.Editing delegateEditing;

	private Style style;
	private float rowHeight;
	private TableViewCell.SeparatorStyle separatorStyle;
	private int separatorColor;
	private View tableHeaderView;
	private View tableFooterView;
	private boolean allowsSelection;
	private boolean editing;
	private float sectionHeaderHeight;
	private float sectionFooterHeight;

	/// Internal variables
	private boolean needsReload;
	private HashMap<IndexPath, TableViewCell> visibleCells;
	private HashMap<String, HashSet<TableViewCell>> reuseableCells;
	private ArrayList<Section> sections;
	private ArrayList<IndexPath> selectedRows;
	private float contentHeight;
	private Size lastSize;
	private boolean layoutSubviewsReentrancyGuard;

	/// Constructors

	public TableView(Rect frame) {
		this(frame, null);
	}

	public TableView(Rect frame, Style style) {
		super(frame);

		this.visibleCells = new HashMap<IndexPath, TableViewCell>();
		this.reuseableCells = new HashMap<String, HashSet<TableViewCell>>();
		this.selectedRows = new ArrayList<IndexPath>();

		this.separatorColor = Color.rgba(0.88f, 0.88f, 0.88f, 1.0f);
		this.separatorStyle = TableViewCell.SeparatorStyle.SINGLE_LINE;
		this.setShowsHorizontalScrollIndicator(false);
		this.allowsSelection = true;
		this.sectionHeaderHeight = DEFAULT_SECTION_HEADER_HEIGHT;
		this.sectionFooterHeight = DEFAULT_SECTION_FOOTER_HEIGHT;
		this.rowHeight = DEFAULT_ROW_HEIGHT;
		this.style = style == null ? Style.PLAIN : style;

		if(this.style == Style.PLAIN) {
			this.setBackgroundColor(Color.WHITE);
		}

		this.setNeedsReload();
	}

	/// Info

	public int getNumberOfSections() {
		if(this.sections != null) {
			return this.sections.size();
		} else {
			return 0;
		}
	}

	public int getNumberOfRowsInSection(int section) {
		if(this.sections != null) {
			return this.sections.get(section).getNumberOfRows();
		} else {
			return 0;
		}
	}

	public List<IndexPath> getIndexPathsForRowsInRect(Rect rect) {
		ArrayList<IndexPath> indexPaths = new ArrayList<IndexPath>();

		if(this.sections == null) {
			return indexPaths;
		}

		for(Section section : this.sections) {
			int sectionIndex = section.getSectionIndex();
			int numberOfRows = section.getNumberOfRows();

			for(int row = 0; row < numberOfRows; row++) {
				if(rect.intersects(this.getRectForRow(row, section))) {
					indexPaths.add(IndexPath.withRowInSection(row, sectionIndex));
				}
			}
		}

		return indexPaths;
	}

	/**
	 * Obtain the index path of the row at the specified point
	 *
	 * If the point is not valid or no row exists at that point, nil is
	 * returned.
	 *
	 * @param point location in the table view
	 * @return index path of the row at point
	 */
	public IndexPath getIndexPathForRowAtPoint(Point point) {
		if(this.sections == null || point.x < 0.0f || point.x > this.getBounds().size.height) {
			return null;
		}

		float offset = point.y;

		for(Section section : this.sections) {
			int sectionIndex = section.getSectionIndex();
			int numberOfRows = section.getNumberOfRows();

			for(int row = 0; row < numberOfRows; row++) {
				float rowOffset = section.getRowOffset(row);

				if(offset >= rowOffset) {
					if(offset <= rowOffset + section.getRowHeight(row)) {
						return IndexPath.withRowInSection(row, sectionIndex);
					}
				}
			}
		}

		return null;
	}

	public IndexPath getIndexPathForCell(TableViewCell cell) {
		for(IndexPath indexPath : this.visibleCells.keySet()) {
			if(this.visibleCells.get(indexPath) == cell) {
				return indexPath;
			}
		}

		return null;
	}

	public Set<IndexPath> getIndexPathsForVisibleRows() {
		return this.visibleCells.keySet();
	}

	public Collection<TableViewCell> getVisibleCells() {
		return this.visibleCells.values();
	}

	public TableViewCell getCellForRowAtIndexPath(IndexPath indexPath) {
		return this.visibleCells.get(indexPath);
	}

	public Rect getRectForSection(int section) {
		if(section >= 0 && this.sections != null && section < this.sections.size()){
			Section s = this.sections.get(section);
			return new Rect(0, s.getSectionOffset(), this.getBounds().size.width, s.getSectionHeight());
		}

		return Rect.zero();
	}

	public Rect getRectForHeaderInSection(int section) {
		if(section >= 0 && this.sections != null && section < this.sections.size()){
			Section s = this.sections.get(section);
			return new Rect(0, s.getSectionOffset(), this.getBounds().size.width, s.getHeaderHeight());
		}

		return Rect.zero();
	}

	public Rect getRectForFooterInSection(int section) {
		if(section >= 0 && this.sections != null && section < this.sections.size()){
			Section s = this.sections.get(section);
			float height = s.getFooterHeight();
			return new Rect(0, s.getSectionOffset() + s.getHeaderHeight() - height, this.getBounds().size.width, height);
		}

		return Rect.zero();
	}

	public Rect getRectForRowAtIndexPath(IndexPath indexPath) {
		return getRectForRow(indexPath.getRow(), indexPath.getSection());
	}

	private Rect getRectForRow(int row, int section) {
		if(section >= 0 && this.sections != null && section < this.sections.size()) {
			return getRectForRow(row, this.sections.get(section));
		}

		return Rect.zero();
	}

	private Rect getRectForRow(int row, Section section) {
		return new Rect(0.0f, section.getRowOffset(row), this.getBounds().size.width, section.getRowHeight(row));
	}

	/// Reuseability

	public void registerClass(Class<? extends TableViewCell> cellClass, String reuseIdentifier) {

	}

	public TableViewCell dequeueReusableCellWithIdentifier(String identifier) {
		HashSet<TableViewCell> cells = this.reuseableCells.get(identifier);

		if(cells != null && cells.size() > 0) {
			TableViewCell cell = cells.iterator().next();
			cells.remove(cell);
			return cell;
		} else {
			return null;
		}
	}

	// TODO: Implement auto cell creation
	public TableViewCell dequeueReusableCellWithIdentifier(String identifier, IndexPath indexPath) {
		return this.dequeueReusableCellWithIdentifier(identifier);
	}

	private void queueCellForReuse(TableViewCell cell) {
		if(cell.reuseIdentifier == null) return;

		HashSet<TableViewCell> cells = this.reuseableCells.get(cell.reuseIdentifier);

		if(cells == null) {
			cells = new HashSet<TableViewCell>();
			this.reuseableCells.put(cell.reuseIdentifier, cells);
		}

		cells.add(cell);
	}

	/// Updating

	public void beginUpdates() {
		// uiview begin animations
	}

	public void endUpdates() {
		this.updateSectionsCache();
		this.layoutCells(true);

		// uiview commit animations
	}

	// TODO: Make work.
	public void insertSections(int[] sections, RowAnimation animation) {
		this.reloadData();
	}

	public void deleteSections(int[] sections, RowAnimation animation) {
		this.reloadData();
	}

	public void reloadSections(int[] sections, RowAnimation animation) {
		this.reloadData();
	}

	public void insertRowsAtIndexPaths(List<IndexPath> indexPaths, RowAnimation animation) {
		this.reloadData();
	}

	public void deleteRowsAtIndexPaths(List<IndexPath> indexPaths, RowAnimation animation) {
		this.reloadData();
	}

	public void reloadRowsAtIndexPaths(List<IndexPath> indexPaths, RowAnimation animation) {
		this.reloadData();
	}

	// Reloading + Layout

	public void reloadData() {
		for(TableViewCell cell : this.visibleCells.values()) {
			queueCellForReuse(cell);
			cell.removeFromSuperview();
		}

		this.visibleCells.clear();
		this.selectedRows.clear();


		if(this.sections != null) {
			for(Section section : this.sections) {
				if(section.headerView != null) {
					section.headerView.removeFromSuperview();
				}

				if(section.footerView != null) {
					section.footerView.removeFromSuperview();
				}
			}

			this.sections = null;
		}

		this.needsReload = false;
		this.layoutSubviews();
	}

	private void setNeedsReload() {
		this.needsReload = true;
		this.setNeedsLayout();
	}

	private void reloadDataIfNeeded() {
		if (this.needsReload) {
			this.reloadData();
		}
	}

	private void updateSectionsCache() {
		if(this.sections != null) {
			for(Section section : this.sections) {
				if(section.headerView != null) section.headerView.removeFromSuperview();
				if(section.footerView != null) section.footerView.removeFromSuperview();
			}
		}

		float offset = this.getContentInset().top;

		if(this.tableHeaderView != null) {
			offset += this.tableHeaderView.getFrame().size.height;
		}

		this.sections = new ArrayList<Section>();

		if(this.dataSource != null) {
			int numberOfSections = this.dataSource.getNumberOfSections(this);

			for(int s = 0; s < numberOfSections; ++s) {
				Section section = new Section(this.dataSource.getNumberOfRowsInSection(this, s), s);
				section.setupRowHeights();
				section.sectionOffset = offset;
				offset += section.getSectionHeight();
				this.sections.add(section);
			}
		}

		if(this.tableFooterView != null) {
			offset += this.tableFooterView.getFrame().size.height;
		}

		this.contentHeight = offset + this.getContentInset().bottom;
	}

	private boolean preLayoutCells() {
		Rect bounds = this.getBounds();

		if(this.sections == null) {
			this.updateSectionsCache();
			this.setContentSize(new Size(bounds.size.width, this.contentHeight));

			this.lastSize = bounds.size.copy();
			return true; // needs visible cells to be redisplayed
		}

		return false; // just need to do the recycling
	}

	public void layoutSubviews() {
		if(!this.layoutSubviewsReentrancyGuard) {
			this.layoutSubviewsReentrancyGuard = true;

			boolean visibleCellsNeedRelayout = this.preLayoutCells();
			super.layoutSubviews();
//			[self _layoutSectionHeaders:visibleCellsNeedRelayout];
			this.layoutCells(visibleCellsNeedRelayout);



			this.layoutSubviewsReentrancyGuard = false;
		}
	}

	private void layoutCells(boolean visibleCellsNeedRelayout) {
		if(visibleCellsNeedRelayout) {
			// update remaining visible cells if needed
			for(IndexPath indexPath : this.visibleCells.keySet()) {
				TableViewCell cell  = this.visibleCells.get(indexPath);
				cell.setFrame(this.getRectForRowAtIndexPath(indexPath));
				cell.setNeedsLayout();
			}
		}

		Rect visible = this.getBounds();

		// Example:
		// old:            0 1 2 3 4 5 6 7
		// new:                2 3 4 5 6 7 8 9
		// to remove:      0 1
		// to add:                         8 9

		List<IndexPath> oldVisibleIndexPaths = new ArrayList<IndexPath>(this.visibleCells.keySet());
		List<IndexPath> newVisibleIndexPaths = this.getIndexPathsForRowsInRect(visible);

		ArrayList<IndexPath> indexPathsToRemove = new ArrayList<IndexPath>(oldVisibleIndexPaths);
		indexPathsToRemove.removeAll(newVisibleIndexPaths);

		ArrayList<IndexPath> indexPathsToAdd = new ArrayList<IndexPath>(newVisibleIndexPaths);
		indexPathsToAdd.removeAll(oldVisibleIndexPaths);

		// remove off screen cells
		for(IndexPath indexPath : indexPathsToRemove) {
			TableViewCell cell = this.visibleCells.get(indexPath);
			this.queueCellForReuse(cell);
			cell.removeFromSuperview();
			this.visibleCells.remove(indexPath);
		}

		// add new cells
		for(IndexPath indexPath : indexPathsToAdd) {
			if(visibleCells.get(indexPath) != null) {
				Log.w("UIKit", "!!! Warning: already have a cell in place for index path " + indexPath);
			} else {
				TableViewCell cell = this.dataSource.getCellForRowAtIndexPath(this, indexPath);
				cell.setFrame(this.getRectForRowAtIndexPath(indexPath));
				cell.setNeedsLayout();
				cell.setSelected(this.selectedRows.contains(indexPath), false);

				this.addSubview(cell);

				this.visibleCells.put(indexPath, cell);
			}
		}

		if(this.tableHeaderView != null) {
			Rect frame = new Rect(0.0f, 0.0f, visible.size.width, this.tableHeaderView.frame.size.height);
			boolean hidden = this.tableHeaderView.isHidden();
			if(visible.intersects(frame) && hidden) {
				this.tableHeaderView.setFrame(frame);
				this.tableHeaderView.setNeedsLayout();
				this.tableHeaderView.setHidden(false);
			} else if(!hidden) {
				this.tableHeaderView.setHidden(true);
			}
		}

		if(this.tableFooterView != null) {
			float footerHeight = this.tableFooterView.frame.size.height;
			Rect frame = new Rect(0.0f, this.contentHeight - footerHeight, visible.size.width, footerHeight);
			boolean hidden = this.tableFooterView.isHidden();
			if(visible.intersects(frame) && hidden) {
				this.tableFooterView.setFrame(frame);
				this.tableFooterView.setNeedsLayout();
				this.tableFooterView.setHidden(false);
			} else if(!hidden) {
				this.tableFooterView.setHidden(true);
			}
		}
	}

	/// Selection

	public IndexPath getIndexPathForSelectedRow() {
		if(this.selectedRows.size() == 0) return null;
		return this.selectedRows.get(0);
	}

	public void selectRowAtIndexPath(IndexPath indexPath, ScrollPosition scrollPosition, boolean animated) {
		this.reloadDataIfNeeded();

		this.scrollToRowAtIndexPath(indexPath, scrollPosition, animated);

		if(this.selectedRows.size() > 0) {
			for(IndexPath selectedIndexPath : this.selectedRows) {
				if(selectedIndexPath.equals(indexPath)) continue;
				this.deselectRowAtIndexPath(selectedIndexPath, animated);
			}
		}

		if(!this.selectedRows.contains(indexPath)) {
			this.selectedRows.add(indexPath);
			this.getCellForRowAtIndexPath(indexPath).setSelected(true);
		}

	}

	public void deselectRowAtIndexPath(IndexPath indexPath, boolean animated) {
		if(indexPath == null) return;

		int index = this.selectedRows.indexOf(indexPath);

		if (index >= 0) {
			this.getCellForRowAtIndexPath(indexPath).setSelected(false);
			this.selectedRows.remove(index);
		}
	}

	// Scrolling

	private void scrollRectToVisible(Rect rect, ScrollPosition scrollPosition, boolean animated) {
		if(rect == null || rect.size.height == 0.0f) return;

		Rect bounds = this.getBounds();

		// adjust the rect based on the desired scroll position setting
		switch (scrollPosition) {
			case TOP: {
				rect.size.height = bounds.size.height;
				break;
			}

			case MIDDLE: {
				rect.origin.y -= (bounds.size.height / 2.f) - rect.size.height;
				rect.size.height = bounds.size.height;
				break;
			}

			case BOTTOM: {
				rect.origin.y -= bounds.size.height - rect.size.height;
				rect.size.height = bounds.size.height;
				break;
			}

			case NONE: {
				break;
			}
		}

		this.scrollRectToVisible(rect, animated);
	}

	public void scrollToNearestSelectedRowAtScrollPosition(ScrollPosition scrollPosition, boolean animated) {
		this.scrollToRowAtIndexPath(this.getIndexPathForSelectedRow(), scrollPosition, animated);
	}

	public void scrollToRowAtIndexPath(IndexPath indexPath, ScrollPosition scrollPosition, boolean animated) {
		Rect rect;
		Rect bounds = this.getBounds();

		if (indexPath == null || (indexPath.getRow() == 0 && indexPath.getSection() == 0)) {
			rect = new Rect(0.0f, 0.0f, bounds.size.width, bounds.size.height);
		} else {
			rect = this.getRectForRowAtIndexPath(indexPath);
		}

		this.scrollRectToVisible(rect, scrollPosition, animated);
	}

	/// Editing


	/// Boilerplate Property Setter+Getters

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		if(dataSource != null) {
			this.dataSource = dataSource;

			if(dataSource instanceof DataSource.Editing) {
				this.dataSourceEditing = (DataSource.Editing)dataSource;
			} else {
				this.dataSourceEditing = null;
			}

			if(dataSource instanceof DataSource.Headers) {
				this.dataSourceHeaders = (DataSource.Headers)dataSource;
			} else {
				this.dataSourceHeaders = null;
			}

			if(dataSource instanceof DataSource.Footers) {
				this.dataSourceFooters = (DataSource.Footers)dataSource;
			} else {
				this.dataSourceFooters = null;
			}
		} else {
			this.dataSource = null;
			this.dataSourceEditing = null;
			this.dataSourceHeaders = null;
			this.dataSourceFooters = null;
		}

		this.setNeedsReload();
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		if(delegate != null) {
			this.delegate = delegate;

			if(delegate instanceof Delegate.RowSizing) {
				this.delegateRowSizing = (Delegate.RowSizing) delegate;
			} else {
				this.delegateRowSizing = null;
			}

			if(delegate instanceof Delegate.Selection) {
				this.delegateSelection = (Delegate.Selection) delegate;
			} else {
				this.delegateSelection = null;
			}

			if(delegate instanceof Delegate.Deselection) {
				this.delegateDeselection = (Delegate.Deselection) delegate;
			} else {
				this.delegateDeselection = null;
			}

			if(delegate instanceof Delegate.Headers) {
				this.delegateHeaders = (Delegate.Headers) delegate;
			} else {
				this.delegateHeaders = null;
			}

			if(delegate instanceof Delegate.Footers) {
				this.delegateFooters = (Delegate.Footers) delegate;
			} else {
				this.delegateFooters = null;
			}

			if(delegate instanceof Delegate.Editing) {
				this.delegateEditing = (Delegate.Editing) delegate;
			} else {
				this.delegateEditing = null;
			}
		} else {
			this.delegate = null;
			this.delegateRowSizing = null;
			this.delegateSelection = null;
			this.delegateDeselection = null;
			this.delegateHeaders = null;
			this.delegateFooters = null;
			this.delegateEditing = null;
		}

		this.setNeedsReload();
	}

	public Style getStyle() {
		return style;
	}

	public float getRowHeight() {
		return rowHeight;
	}

	public void setRowHeight(float rowHeight) {
		this.rowHeight = rowHeight;
		this.setNeedsLayout();
	}

	public TableViewCell.SeparatorStyle getSeparatorStyle() {
		return separatorStyle;
	}

	public void setSeparatorStyle(TableViewCell.SeparatorStyle separatorStyle) {
		this.separatorStyle = separatorStyle;
	}

	public int getSeparatorColor() {
		return separatorColor;
	}

	public void setSeparatorColor(int separatorColor) {
		this.separatorColor = separatorColor;
	}

	public View getTableHeaderView() {
		return tableHeaderView;
	}

	public void setTableHeaderView(View tableHeaderView) {
		if (tableHeaderView != this.tableHeaderView) {
			if(this.tableHeaderView != null) {
				this.tableHeaderView.removeFromSuperview();
			}

			tableHeaderView.setHidden(true);
			this.tableHeaderView = tableHeaderView;
			this.setNeedsLayout();
			this.addSubview(tableHeaderView);
		}
	}

	public View getTableFooterView() {
		return tableFooterView;
	}

	public void setTableFooterView(View tableFooterView) {
		if (tableFooterView != this.tableFooterView) {
			if(this.tableFooterView != null) {
				this.tableFooterView.removeFromSuperview();
			}

			tableFooterView.setHidden(true);
			this.tableFooterView = tableFooterView;
			this.setNeedsLayout();
			this.addSubview(tableFooterView);
		}
	}

	public boolean allowsSelection() {
		return allowsSelection;
	}

	public void setAllowsSelection(boolean allowsSelection) {
		this.allowsSelection = allowsSelection;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	public float getSectionHeaderHeight() {
		return sectionHeaderHeight;
	}

	public void setSectionHeaderHeight(float sectionHeaderHeight) {
		this.sectionHeaderHeight = sectionHeaderHeight;
	}

	public float getSectionFooterHeight() {
		return sectionFooterHeight;
	}

	public void setSectionFooterHeight(float sectionFooterHeight) {
		this.sectionFooterHeight = sectionFooterHeight;
	}

	class Section {
		private View headerView;
		private View footerView;
		private int sectionIndex;
		private int numberOfRows;
		private float sectionHeight;
		private float sectionOffset;
		private RowInfo[] rowInfo;

		public Section(int numberOfRows, int sectionIndex) {
			this.numberOfRows = numberOfRows;
			this.sectionIndex = sectionIndex;
			this.rowInfo = new RowInfo[numberOfRows];
		}

		public float getSectionOffset() {
			return sectionOffset;
		}

		public void setSectionOffset(float sectionOffset) {
			this.sectionOffset = sectionOffset;
		}

		public int getSectionIndex() {
			return sectionIndex;
		}

		public int getNumberOfRows() {
			return numberOfRows;
		}

		private void setupRowHeights() {
			sectionHeight = 0.0f;

			View view = this.getHeaderView();

			if(view != null) {
				sectionHeight += roundf(view.frame.size.height);
			}

			view = this.getFooterView();

			if(view != null) {
				sectionHeight += roundf(view.frame.size.height);
			}

			for(int i = 0; i < numberOfRows; i++) {
				float height;
				if(delegateRowSizing != null) {
					height = delegateRowSizing.getHeightForRowAtIndexPath(TableView.this, IndexPath.withRowInSection(i, sectionIndex));
				} else {
					height = rowHeight;
				}

				rowInfo[i] = new RowInfo(sectionHeight, height);
				sectionHeight += height;
			}

		}

		public float getRowHeight(int i) {
			if(i >= 0 && i < numberOfRows) {
				return rowInfo[i].height;
			}

			return 0.0f;
		}

		public float getSectionRowOffset(int i) {
			if(i >= 0 && i < numberOfRows){
				return rowInfo[i].offset;
			}
			return 0.0f;
		}

		public float getRowOffset(int i) {
			return sectionOffset + this.getSectionRowOffset(i);
		}

		public float getRowsHeight() {
			RowInfo lastRow = rowInfo[rowInfo.length - 1];
			return lastRow.offset + lastRow.offset;
		}

		public float getSectionHeight() {
			return sectionHeight;
		}

		public float getHeaderHeight() {
			return (this.getHeaderView() != null) ? this.getHeaderView().getFrame().size.height : 0.0f;
		}

		public float getFooterHeight() {
			return (this.getFooterView() != null) ? this.getFooterView().getFrame().size.height : 0.0f;
		}

		public View getHeaderView() {
			if(this.headerView == null) {
				if(delegateHeaders != null) {
					this.headerView = delegateHeaders.getViewForHeaderInSecton(TableView.this, this.sectionIndex);
				} else if(dataSourceHeaders != null) {
					this.headerView = new TableViewSectionLabel(dataSourceHeaders.getTitleForHeaderInSections(TableView.this, this.sectionIndex));
				}

				if(this.headerView != null) {
					float headerHeight;

					if(delegateHeaders != null) {
						headerHeight = delegateHeaders.getHeightForHeaderInSection(TableView.this, this.sectionIndex);
					} else {
						headerHeight = sectionHeaderHeight;
					}

					this.headerView.frame = new Rect(0.0f, 0.0f, TableView.this.getBounds().size.width, headerHeight);
					this.headerView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				}
			}

			return this.headerView;
		}

		public View getFooterView() {
			if(this.footerView == null) {
				if(delegateFooters != null) {
					this.footerView = delegateFooters.getViewForFooterInSecton(TableView.this, this.sectionIndex);
				} else if(dataSourceFooters != null) {
					this.footerView = new TableViewSectionLabel(dataSourceFooters.getTitleForFooterInSections(TableView.this, this.sectionIndex));
				}

				if(this.footerView != null) {
					float footerHeight;

					if(delegateFooters != null) {
						footerHeight = delegateFooters.getHeightForFooterInSection(TableView.this, this.sectionIndex);
					} else {
						footerHeight = sectionFooterHeight;
					}

					this.footerView.frame = new Rect(0.0f, 0.0f, TableView.this.getBounds().size.width, footerHeight);
					this.footerView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				}
			}

			return this.footerView;
		}
	}

	class RowInfo {
		public final float offset; // from beginning of section
		public final float height;

		RowInfo(float offset, float height) {
			this.offset = offset;
			this.height = height;
		}
	}

}
