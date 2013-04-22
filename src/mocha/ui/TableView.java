/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 enormego. All rights reserved.
*/

package mocha.ui;

import android.view.ViewGroup;
import mocha.foundation.IndexPath;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.lang.reflect.Constructor;
import java.util.*;

public class TableView extends ScrollView {
	private static final float PLAIN_HEADER_HEIGHT = 23.0f;
	private static final float DEFAULT_ROW_HEIGHT = 44.0f;
	private static final float GROUPED_TABLE_Y_MARGIN = 10.0f;

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

		public interface RowDisplay extends Delegate {
			public void willDisplayCell(TableView tableView, TableViewCell cell, IndexPath indexPath);
			public void didEndDisplayingCell(TableView tableView, TableViewCell cell, IndexPath indexPath);
		}

		public interface Selection extends Delegate {
			public IndexPath willSelectRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didSelectRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Deselection extends Delegate {
			public IndexPath willDeselectRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didDeselectRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Highlighting extends Delegate {
			/**
			 * Control whether or not a row can be highlighted.  Called when a touch
			 * first comes down on a row.
			 *
			 * @param tableView Table view for the row
			 * @param indexPath Index path for the row
			 * @return false to halt the selection process, if false, the currently
			 * selected row (if any) will not be deselected.
			 */
			public boolean shouldHighlightRowAtIndexPath(TableView tableView, IndexPath indexPath);

			public void didHighlightRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didUnhighlightRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface Headers extends Delegate {
			public TableViewSubview getViewForHeaderInSecton(TableView tableView, int section);
			public float getHeightForHeaderInSection(TableView tableView, int section);
		}

		public interface Footers extends Delegate {
			public TableViewSubview getViewForFooterInSecton(TableView tableView, int section);
			public float getHeightForFooterInSection(TableView tableView, int section);
		}

		public interface Editing extends Delegate {
			public void willBeginEditingRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public void didEndEditingRowAtIndexPath(TableView tableView, IndexPath indexPath);
			public CharSequence getTitleForDeleteConfirmationButtonForRowAtIndexPath(TableView tableView, IndexPath indexPath);
		}

		public interface AccessorySelection extends Delegate {
			public void accessoryButtonTappedForRowWithIndexPath(TableView tableView, IndexPath indexPath);
		}

	}

	public enum ScrollPosition {
		NONE,
		TOP,
		MIDDLE,
		BOTTOM
	}

	static class SectionInfo {
		int numberOfRows;
		Object header;
		Object footer;
		float y;
		float headerHeight;
		float footerHeight;
		float[] rowHeights;
		float cumulativeRowHeight;

		float getMaxY() {
			return y + headerHeight + footerHeight + cumulativeRowHeight;
		}
	}

	static class TableViewCellIndexPathComparator implements Comparator<TableViewCell> {

		public int compare(TableViewCell cellA, TableViewCell cellB) {
			IndexPath indexPathA = cellA._dataSourceInfo.indexPath;
			IndexPath indexPathB = cellB._dataSourceInfo.indexPath;

			if (indexPathA.lowerThan(indexPathB)) {
				return -1;
			} else {
				if (indexPathA.greaterThan(indexPathB)) {
					return 1;
				} else {
					return 0;
				}
			}
		}

	}

	static class TableViewSectionComparator implements Comparator<TableViewSubview> {

		public int compare(TableViewSubview viewA, TableViewSubview viewB) {
			int sectionA = viewA._dataSourceInfo.section;
			int sectionB = viewB._dataSourceInfo.section;

			if (sectionA < sectionB) {
				return -1;
			} else if (sectionA > sectionB) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	public enum Style {
		PLAIN, GROUPED, CUSTOM
	}

	private Style tableStyle;
	private TableViewCell.SeparatorStyle separatorStyle;
	private int separatorColor;
	private boolean editing;
	private float rowHeight;
	private boolean allowsSelection;
	private boolean reloadingData;

	private DataSource dataSource;
	private DataSource.Editing dataSourceEditing;
	private DataSource.Headers dataSourceHeaders;
	private DataSource.Footers dataSourceFooters;

	private Delegate delegate;
	private Delegate.RowSizing delegateRowSizing;
	private Delegate.RowDisplay delegateRowDisplay;
	private Delegate.Selection delegateSelection;
	private Delegate.Deselection delegateDeselection;
	private Delegate.Highlighting delegateHighlighting;
	private Delegate.Headers delegateHeaders;
	private Delegate.Footers delegateFooters;
	private Delegate.Editing delegateEditing;
	private Delegate.AccessorySelection delegateAccessorySelection;

	private int numberOfSections;
	private List<SectionInfo> sectionsInfo;
	private Map<Object,List<TableViewCell>> cellsQueuedForReuse;
	private Map<Object,Class<? extends TableViewCell>> registeredClasses;
	private List<TableViewSubview> headersQueuedForReuse;
	private List<TableViewSubview> footersQueuedForReuse;
	private List<TableViewSubview> visibleHeaders;
	private List<TableViewSubview> visibleSubviews;
	private boolean usesCustomRowHeights;
	private Runnable cellTouchCallback;
	private TableViewCell touchedCell;
	private boolean touchesMoved;
	private boolean ignoreTouches;
	private IndexPath selectedRowIndexPath;
	private Set<IndexPath> selectedRowsIndexPaths;
	private List<IndexPath> cellsBeingEditedPaths;
	private View tableHeaderView;
	private View tableFooterView;
	private float tableHeaderHeight;
	private float tableFooterHeight;
	private boolean tableHeaderAttached;
	private boolean tableFooterAttached;
	private boolean hasLoadedData;

	private List<TableViewSubview> tableViewHeaders;
	private List<TableViewCell> tableViewCells;
	private List<TableViewSubview> viewsToRemove;

	public TableView(Style style) {
		this(style, new Rect(0.0f, 0.0f, 320.0f, 480.0f));
	}

	public TableView(Style style, Rect frame) {
		super(frame);

		this.setBackgroundColor(Color.WHITE);
		this.tableStyle = style == null ? Style.PLAIN : style;
		this.separatorStyle = TableViewCell.SeparatorStyle.SINGLE_LINE;
		this.separatorColor = Color.white(0.88f, 1.0f);
		this.numberOfSections = 1;
		this.setRowHeight(DEFAULT_ROW_HEIGHT);
		this.setAllowsSelection(true);
		this.sectionsInfo = new ArrayList<SectionInfo>();
		this.cellsQueuedForReuse = new HashMap<Object, List<TableViewCell>>();
		this.registeredClasses = new HashMap<Object, Class<? extends TableViewCell>>();
		this.headersQueuedForReuse = new ArrayList<TableViewSubview>();
		this.footersQueuedForReuse = new ArrayList<TableViewSubview>();
		this.visibleHeaders = new ArrayList<TableViewSubview>();
		this.visibleSubviews = new ArrayList<TableViewSubview>();
		this.usesCustomRowHeights = false;
		this.touchedCell = null;
		this.selectedRowIndexPath = null;
		this.selectedRowsIndexPaths = new HashSet<IndexPath>();
		this.editing = false;
		this.cellsBeingEditedPaths = new ArrayList<IndexPath>();

		this.tableViewHeaders = new ArrayList<TableViewSubview>();
		this.tableViewCells = new ArrayList<TableViewCell>();
		this.viewsToRemove = new ArrayList<TableViewSubview>();

		this.setAlwaysBounceVertical(true);
	}

	public Style getTableStyle() {
		return tableStyle;
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

	public DataSource getDataSource() {
		return this.dataSource;
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
	}

	private void debugSubviews() {
		MLog("============================");
		MLog("Subview Debug (Bounds: %s):", this.getBounds());
		MLog("----------------------------");
		String format = "| %-35s | %-7s | %-7s | %-35s | %-10s | %-10s | %-11s | %-6s | %-5s |";
		MLog(format, "Native Frame", "Section", "Type", "Frame", "In Headers", "In Cells", "In Subviews", "Queued", "Y");
		for(View view : this.getSubviews()) {
			if(view instanceof TableViewSubview) {
				TableViewSubview subview = (TableViewSubview)view;
				ViewGroup viewGroup = subview.getLayer().getViewGroup();

				MLog(format, new Rect(viewGroup.getX(), viewGroup.getY(), viewGroup.getWidth(), viewGroup.getHeight()),
						(subview instanceof TableViewCell ? subview._dataSourceInfo.indexPath.section + "x" + subview._dataSourceInfo.indexPath.row : subview._dataSourceInfo.section),
						subview._dataSourceInfo.type, subview.getFrame(), this.visibleHeaders.contains(subview),
						(subview instanceof TableViewCell ? this.tableViewCells.contains(subview) : "false"),
						this.visibleSubviews.contains(subview), subview._isQueued, viewGroup.getY());
			}
		}

		MLog("============================");
	}

	private void debugSectionInfo() {
		MLog("============================");
		MLog("Section Info Debug");
		MLog("----------------------------");
		int s = 0;
		for(SectionInfo sectionInfo : this.sectionsInfo) {
			MLog("| Section: %-2s | Rows: %-2s | Offset: %-5s | header: %-5s", s, sectionInfo.numberOfRows, sectionInfo.y, sectionInfo.headerHeight);
			float y = sectionInfo.y + sectionInfo.headerHeight;
			int r = 0;

			float[] rowHeights;

			if(!this.usesCustomRowHeights) {
				rowHeights = new float[sectionInfo.numberOfRows];

				for(int i = 0; i < sectionInfo.numberOfRows; i++) {
					rowHeights[i] = this.rowHeight;
				}
			} else {
				rowHeights = sectionInfo.rowHeights;
			}

			for(float rowHeight : rowHeights) {
				MLog("| Row: %-2s, %-2s | Offset: %-5s | Height: %-5s", s, r, y, rowHeight);
				y += rowHeight;
				r++;
			}

			s++;
		}

		MLog("============================");
	}

	public Delegate getDelegate() {
		return this.delegate;
	}

	public void setDelegate(Delegate delegate) {
		if(delegate != null) {
			this.delegate = delegate;

			if(delegate instanceof Delegate.RowSizing) {
				this.delegateRowSizing = (Delegate.RowSizing) delegate;
			} else {
				this.delegateRowSizing = null;
			}

			if(delegate instanceof Delegate.RowDisplay) {
				this.delegateRowDisplay = (Delegate.RowDisplay) delegate;
			} else {
				this.delegateRowDisplay = null;
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

			if(delegate instanceof Delegate.Highlighting) {
				this.delegateHighlighting = (Delegate.Highlighting) delegate;
			} else {
				this.delegateHighlighting = null;
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

			if(delegate instanceof Delegate.AccessorySelection) {
				this.delegateAccessorySelection = (Delegate.AccessorySelection)delegate;
			} else {
				this.delegateAccessorySelection = null;
			}
		} else {
			this.delegate = null;
			this.delegateRowSizing = null;
			this.delegateSelection = null;
			this.delegateDeselection = null;
			this.delegateHighlighting = null;
			this.delegateHeaders = null;
			this.delegateFooters = null;
			this.delegateEditing = null;
			this.delegateAccessorySelection = null;
		}
	}

	public void setEditing(boolean editing) {
		if (this.editing == editing) {
			return;
		}

		if (!(this.editing = editing)) {
			this.resetAllEditingCells();
		}
	}

	public boolean isEditing() {
		return this.editing;
	}

	public void setRowHeight(float rowHeight) {
		this.rowHeight = rowHeight;
	}

	public float getRowHeight() {
		return this.rowHeight;
	}

	public void setAllowsSelection(boolean allowsSelection) {
		this.allowsSelection = allowsSelection;
	}

	public boolean allowsSelection() {
		return this.allowsSelection;
	}

	public View getTableHeaderView() {
		return this.tableHeaderView;
	}

	public void setTableHeaderView(View tableHeaderView) {
		if(this.tableHeaderView != tableHeaderView) {
			if(this.tableHeaderView != null) {
				this.tableHeaderView.removeFromSuperview();
			}

			this.tableHeaderView = tableHeaderView;
		}


		float oldHeight = this.tableHeaderHeight;
		this.tableHeaderHeight = this.tableHeaderView != null ? this.tableHeaderView.getFrame().size.height : 0.0f;
		this.updateTableHeaderHeight(oldHeight, this.tableHeaderHeight);
		this.tableHeaderAttached = false;

		this.setNeedsLayout();
	}

	public View getTableFooterView() {
		return this.tableFooterView;
	}

	public void setTableFooterView(View tableFooterView) {
		if(this.tableFooterView != tableFooterView) {
			if(this.tableFooterView != null) {
				this.tableFooterView.removeFromSuperview();
			}

			this.tableFooterView = tableFooterView;
		}

		float oldHeight = this.tableFooterHeight;
		this.tableFooterHeight = this.tableFooterView != null ? this.tableFooterView.getFrame().size.height : 0.0f;
		this.tableFooterAttached = false;

		Size contentSize = this.getContentSize();
		contentSize.height += this.tableFooterHeight - oldHeight;
		this.setContentSize(contentSize);

		this.setNeedsLayout();
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;
		super.setFrame(frame);
		Size newSize = this.getFrame().size;

		if(!newSize.equals(oldSize)) {
			this.updateIndex();

			/*if (this.tableStyle == Style.GROUPED) {
				if(this.hasLoadedData) {
					this.reloadData();
				}
			} else {*/
				this.setContentSize(new Size(newSize.width, this.getContentSize().height));
			//}

			this.layoutSubviews();
		}
	}

	public int getNumberOfSections() {
		return this.sectionsInfo != null ? this.sectionsInfo.size() : 0;
	}

	public int getNumberOfRowsInSection(int section) {
		if (this.sectionsInfo.size() <= section) {
			if(this.dataSource != null) {
				return this.dataSource.getNumberOfRowsInSection(this, section);
			} else {
				return 0;
			}

		} else {
			if(this.isSectionValid(section)) {
				return this.sectionsInfo.get(section).numberOfRows;
			}
		}

		return 0;
	}

	private boolean isSectionValid(int section) {
		return (section >= 0 && section < this.numberOfSections);
	}

	private boolean isIndexPathValid(IndexPath indexPath) {
		if(indexPath == null) return false;

		int section = indexPath.section;
		int row = indexPath.row;

		return (this.isSectionValid(section) && row >= 0 && row < this.sectionsInfo.get(section).numberOfRows);
	}

	public TableViewCell getCellForRowAtIndexPath(IndexPath indexPath) {
		if (indexPath != null && this.isIndexPathValid(indexPath)) {
			List<TableViewCell> cells = this.getVisibleCells();

			for(TableViewCell cell : cells) {
				if (indexPath.equals(this.getIndexPathForCell(cell))) {
					return cell;
				}
			}
		} else {
			return null;
		}

		return null;
	}

	public IndexPath getIndexPathForCell(TableViewCell cell) {
		return cell._dataSourceInfo.indexPath;
	}

	private int getSectionAtPoint(Point point) {
		int section = 0;

		for(SectionInfo sectionInfo : this.sectionsInfo) {
			if(point.y < sectionInfo.getMaxY()) {
				break;
			} else {
				section++;
			}
		}

		return (section >= this.numberOfSections) ? this.numberOfSections - 1 : section;
	}

	public IndexPath getIndexPathForRowAtPoint(Point point) {
		int section = this.getSectionAtPoint(point);

		if (section == -1) {
			return null;
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		float offsetY = Math.max(point.y - sectionInfo.y, 0);

		if(offsetY < sectionInfo.headerHeight) {
			return null;
		}

		if (offsetY >= (sectionInfo.headerHeight + sectionInfo.cumulativeRowHeight)) {
			return null;
		}

		if(this.tableStyle == Style.GROUPED) {
			offsetY -= sectionInfo.headerHeight;
		}

		int row = 0;
		float current = 0;
		for(int i = 0; i < sectionInfo.numberOfRows; i++) {
			current += this.usesCustomRowHeights ? sectionInfo.rowHeights[i] : this.rowHeight;

			if(offsetY < current) {
				break;
			} else {
				row++;
			}
		}

		return new IndexPath(section, row);
	}

	public void reloadData() {
		this.hasLoadedData = true;
		this.updateSectionsInfo();

		boolean areAnimationsEnabled = View.areAnimationsEnabled();
		View.setAnimationsEnabled(false);

		List<View> subviews = new ArrayList<View>(this.getSubviews());
		for(View subview : subviews) {
			if(subview instanceof TableViewSubview) {
				subview.removeFromSuperview();

				if(subview instanceof TableViewCell) {
					TableViewCell cell = (TableViewCell)subview;

					if(cell._reuseIdentifier != null) {
						List<TableViewCell> cells = this.cellsQueuedForReuse.get(cell._reuseIdentifier);

						if(cells == null) {
							cells = new ArrayList<TableViewCell>();
							this.cellsQueuedForReuse.put(cell._reuseIdentifier, cells);
						}

						cells.add(cell);
					}

					if(this.delegateRowDisplay != null) {
						this.delegateRowDisplay.didEndDisplayingCell(this, cell, cell._dataSourceInfo.indexPath);
					}
				}
			}
		}

		this.tableViewCells.clear();
		this.visibleSubviews.clear();
		this.visibleHeaders.clear();
		this.headersQueuedForReuse.clear();
		this.footersQueuedForReuse.clear();
		this.viewsToRemove.clear();

		Size contentSize = this.getContentSize();
		Point contentOffset = this.getContentOffset();
		Rect bounds = this.getBounds();

		if(contentOffset.y + bounds.size.height > contentSize.height) {
			float offsetY;

			if(contentSize.height < bounds.size.height) {
				offsetY = -this.getContentInset().top;
			} else {
				offsetY = contentSize.height - bounds.size.height;
			}

			this.setContentOffset(new Point(contentOffset.x, offsetY));
		}

		this.layoutSubviews();
		View.setAnimationsEnabled(areAnimationsEnabled);
	}

	boolean isEmpty() {
		return this.sectionsInfo == null || this.sectionsInfo.size() == 0;
	}

	private void updateSectionsInfo() {
		if (this.dataSource == null) {
			return;
		}

		if(this.reloadingData) {
			MWarn("WARNING: Nested call to TableView.reloadData() detected.  This is unsupported.");
		}

		this.reloadingData = true;

		boolean isCustomStyle = (this.tableStyle == Style.CUSTOM);
		boolean isGroupedStyle = (this.tableStyle == Style.GROUPED);
		boolean isPlainStyle = (this.tableStyle == Style.PLAIN);

		this.sectionsInfo.clear();
		this.usesCustomRowHeights = this.delegateRowSizing != null;
		this.numberOfSections = this.dataSource.getNumberOfSections(this);

		boolean hasHeaderTitles = this.dataSourceHeaders != null;
		boolean hasFooterTitles = this.dataSourceFooters != null;

		boolean hasHeaderViews = this.delegateHeaders != null;
		boolean hasFooterViews = this.delegateFooters != null;

		TableViewHeader header = null;
		TableViewFooter footer = null;

		if (isGroupedStyle) {
			header = new TableViewHeader.Grouped();

			footer = new TableViewFooter();
		}

		float tableHeight = this.tableHeaderHeight;

		for (int section = 0; section < this.numberOfSections; section++) {
			SectionInfo sectionInfo = new SectionInfo();
			sectionInfo.numberOfRows = this.getNumberOfRowsInSection(section);
			sectionInfo.y = tableHeight;

			float headerHeight = 0.0f;
			float footerHeight = 0.0f;
			String headerTitle = null;
			String footerTitle = null;

			if (!isCustomStyle) {
				if(hasHeaderViews) {
					headerHeight = this.delegateHeaders.getHeightForHeaderInSection(this, section);
				}

				if (hasHeaderTitles) {
					headerTitle = this.dataSourceHeaders.getTitleForHeaderInSections(this, section);
				}

				if(headerHeight <= 0.0f) {
					if (isGroupedStyle) {
						if (headerTitle == null) {
							headerHeight = (section == 0) ? GROUPED_TABLE_Y_MARGIN : (2 * GROUPED_TABLE_Y_MARGIN + 1);
						} else if(header != null) {
							header.setText(headerTitle);
							headerHeight = header.sizeThatFits(this.getBounds().size).height;
						}
					} else {
						if (headerTitle != null) {
							headerHeight = PLAIN_HEADER_HEIGHT;
						}
					}
				}

				if (isGroupedStyle && (hasFooterTitles || hasFooterViews)) {
					if(hasFooterViews) {
						footerHeight = this.delegateFooters.getHeightForFooterInSection(this, section);
					}

					if(hasFooterTitles) {
						footerTitle = this.dataSourceFooters.getTitleForFooterInSections(this, section);
					}

					if (footerTitle != null) {
						footer.setText(footerTitle);

						if(footerHeight <= 0.0f) {
							footerHeight = footer.sizeThatFits(this.getBounds().size).height;
						}
					}
				}
			}

			sectionInfo.header = headerTitle;
			sectionInfo.footer = footerTitle;
			sectionInfo.headerHeight = headerHeight;
			sectionInfo.footerHeight = footerHeight;

			if (this.usesCustomRowHeights) {
				float[] rowHeights = new float[sectionInfo.numberOfRows];
				sectionInfo.cumulativeRowHeight = 0.0f;

				for (int row = 0; row < sectionInfo.numberOfRows; row++) {
					float height = this.delegateRowSizing.getHeightForRowAtIndexPath(this, new IndexPath(section, row));
					sectionInfo.cumulativeRowHeight += height;
					rowHeights[row] = height;
				}

				sectionInfo.rowHeights = rowHeights;
			} else {
				sectionInfo.cumulativeRowHeight = sectionInfo.numberOfRows * this.rowHeight;
			}

			this.sectionsInfo.add(sectionInfo);
			tableHeight += sectionInfo.cumulativeRowHeight + headerHeight + footerHeight;
		}

		if (isGroupedStyle) {
			tableHeight += GROUPED_TABLE_Y_MARGIN + 1.0f;
		}

		tableHeight += this.tableFooterHeight;

		this.setContentSize(new Size(this.getBounds().size.width, tableHeight));
		this.reloadingData = false;
	}

	private void updateTableHeaderHeight(float oldHeight, float newHeight) {
		float delta = newHeight - oldHeight;
		for(SectionInfo sectionInfo : this.sectionsInfo) {
			sectionInfo.y += delta;
		}

		Size contentSize = this.getContentSize();
		contentSize.height += delta;
		this.setContentSize(contentSize);
	}

	public void beginUpdates() {
		// TODO
	}

	public void endUpdates() {
		// TODO
		this.reloadData();
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if (this.sectionsInfo.size() == 0) {
			return;
		}

		float minY = this.getContentOffset().y;
		float maxY = minY + this.getBounds().size.height;

		if(this.tableHeaderView != null) {
			if(minY <= this.tableHeaderHeight) {
				if(!this.tableHeaderAttached) {
					this.tableHeaderView.setFrame(new Rect(0.0f, 0.0f, this.getFrame().size.width, this.tableHeaderHeight));
					this.tableHeaderView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
					this.insertSubview(this.tableHeaderView, 0);
					this.tableHeaderAttached = true;
				}
			} else if(this.tableHeaderAttached) {
				this.tableHeaderView.removeFromSuperview();
				this.tableHeaderAttached = false;
			}
		}

		if(this.tableFooterView != null) {
			float offset = this.getContentSize().height - tableFooterHeight;
			if(maxY >= offset) {
				if(!this.tableFooterAttached) {
					this.tableFooterView.setFrame(new Rect(0.0f, offset, this.getFrame().size.width, this.tableFooterHeight));
					this.tableFooterView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
					this.insertSubview(this.tableFooterView, 0);
					this.tableFooterAttached = true;
				} else if(this.tableFooterView.getFrame().origin.y < offset) {
					this.tableFooterView.setFrame(new Rect(0.0f, offset, this.getFrame().size.width, this.tableFooterHeight));
				}
			} else if(this.tableFooterAttached) {
				this.tableFooterView.removeFromSuperview();
				this.tableFooterAttached = false;
			}
		}

		int section = this.getSectionAtPoint(this.getContentOffset());

		if (minY >= 0 && minY <= this.maxPoint.y) {
			this.scanSubviewsForQueuing(minY, maxY, section);
		}

		this.addSubviewsAtTop(minY);
		this.addSubviewsAtBottom(maxY, section);

		if(this.getSubviews().size() > 20) {
			MWarn("subviews > 20, offset: %f", this.getContentOffset().y);
		}

		for(TableViewSubview view : this.viewsToRemove) {
			view.removeFromSuperview();
		}

		this.viewsToRemove.clear();

		if (this.tableStyle == Style.PLAIN) {
			this.updateStickyHeaderPositions(minY, section);
		}
	}

	private void scanSubviewsForQueuing(float minY, float maxY, int section) {
		boolean isPlain = (this.tableStyle == Style.PLAIN);

		for(View view : this.getSubviews()) {
			if(!(view instanceof TableViewSubview)) continue;
			TableViewSubview subview = (TableViewSubview)view;

			if(subview._isQueued) continue;

			if((view.getFrame().maxY() <= minY || view.getFrame().origin.y >= maxY)) {
				List<? extends TableViewSubview> queuedViews = null;
				TableViewSubview.Info info = subview._dataSourceInfo;

				if (info.type == TableViewSubview.Info.Type.CELL) {
					TableViewCell cell = (TableViewCell)subview;

					queuedViews = this.cellsQueuedForReuse.get(cell._reuseIdentifier);

					if(queuedViews == null) {
						List<TableViewCell> queuedCells = new ArrayList<TableViewCell>();
						this.cellsQueuedForReuse.put(cell._reuseIdentifier, queuedCells);
						queuedViews = queuedCells;
					}

					if(this.delegateRowDisplay != null) {
						this.delegateRowDisplay.didEndDisplayingCell(this, cell, info.indexPath);
					}
				} else {
					if (info.type == TableViewSubview.Info.Type.HEADER) {
						if (isPlain && subview._dataSourceInfo.section == section) {
							continue;
						}

						queuedViews = this.headersQueuedForReuse;
					} else {
						queuedViews = this.footersQueuedForReuse;
					}
				}

				this.enqueueView(queuedViews, subview);
			}
		}
	}

	private void addSubviewsAtTop(float minY) {
		TableViewSubview visibleSubview = this.visibleSubviews.size() > 0 ? this.visibleSubviews.get(0) : null;

		if (visibleSubview == null) {
			return;
		}

		float offsetY = visibleSubview.getFrame().origin.y;
		boolean isPlain = this.tableStyle == Style.PLAIN;
		View header = null;

		if (offsetY >= minY && isPlain && this.visibleHeaders.size() > 0 && visibleSubview == this.visibleHeaders.get(0)) {
			if(this.visibleSubviews.size() > 1) {
				header = this.visibleHeaders.get(0);
				visibleSubview = this.visibleSubviews.get(1);
				offsetY = visibleSubview.getFrame().origin.y;
			} else {
				return;
			}
		}

		boolean changedHeaders = false;

		while (offsetY >= minY) {
			visibleSubview = this.getPopulatedSubviewForInfo(this.getInfoForPreviousView(visibleSubview._dataSourceInfo));

			if (visibleSubview == null) {
				break;
			}

			offsetY -= visibleSubview.getBounds().size.height;

			if (visibleSubview == header) {
				header = null;
				continue;
			}

			Rect frame = visibleSubview.getFrame();
			frame.origin.x = 0.0f;
			frame.origin.y = offsetY;
			visibleSubview.setFrame(frame);

			if (header != null) {
				this.visibleSubviews.add(1, visibleSubview);
			} else {
				this.visibleSubviews.add(0, visibleSubview);
			}

			if (visibleSubview._dataSourceInfo.type == TableViewSubview.Info.Type.HEADER) {
				this.visibleHeaders.add(0, visibleSubview);
				changedHeaders = true;
			} else if(visibleSubview instanceof TableViewCell) {
				this.tableViewCells.add((TableViewCell) visibleSubview);
			}
		}

		if (isPlain && (this.visibleHeaders.size() == 0 || this.visibleHeaders.get(0) != this.visibleSubviews.get(0))) {
			TableViewSubview.Info info = this.visibleSubviews.get(0)._dataSourceInfo;
			int section = (info.type == TableViewSubview.Info.Type.CELL) ? info.indexPath.section : info.section;

			if (this.sectionsInfo.get(section).header != null) {
				visibleSubview = this.getPopulatedSubviewForInfo(this.getInfoForHeader(section));
				this.visibleSubviews.add(0, visibleSubview);
				this.visibleHeaders.add(0, visibleSubview);
				changedHeaders = true;
			}
		}

		if(changedHeaders) {
			this.headersChanged();
		}
	}

	private void addSubviewsAtBottom(float maxY, int section) {
		float offsetY;

		int size = this.visibleSubviews.size();

		if(size == 0) {
			IndexPath indexPath = this.getIndexPathForRowAtPoint(this.getBounds().origin);

			if(indexPath != null) {
				if(this.getTableStyle() == Style.PLAIN) {
					SectionInfo info = this.sectionsInfo.get(indexPath.section);

					if(info.headerHeight > 0.0f) {
						TableViewSubview header = this.getPopulatedHeader(this.getInfoForHeader(indexPath.section));
						this.addSubview(header);
						this.visibleHeaders.add(header);
						this.visibleSubviews.add(header);
						size++;
					}
				}

				TableViewSubview visibleSubview = this.getPopulatedSubviewForInfo(this.getInfoForCell(indexPath));

				Rect frame = this.getRectForRowAtIndexPath(indexPath);

				if(frame != null) {
					visibleSubview.setFrame(frame);
					this.visibleSubviews.add(visibleSubview);
					this.tableViewCells.add((TableViewCell) visibleSubview);
					size++;
				}
			}
		}

		TableViewSubview visibleSubview = (size > 0) ? this.visibleSubviews.get(size - 1) : null;

		if (visibleSubview == null) {
			visibleSubview = new TableViewFooter();

			if (section == 0) {
				offsetY = this.tableHeaderHeight;
				visibleSubview._dataSourceInfo = this.getInfoForFooter(-1);
			} else {
				offsetY = this.sectionsInfo.get(section).y;
				visibleSubview._dataSourceInfo = this.getInfoForFooter(section - 1);
			}
		} else {
			offsetY = visibleSubview.getFrame().maxY();
		}

		boolean changedHeaders = false;

		while (offsetY <= maxY) {
			visibleSubview = this.getPopulatedSubviewForInfo(this.getInfoForNextView(visibleSubview._dataSourceInfo));

			if (visibleSubview == null) {
				break;
			}

			Rect frame = visibleSubview.getFrame();
			frame.origin.x = 0.0f;
			frame.origin.y = offsetY;
			visibleSubview.setFrame(frame);

			offsetY += frame.size.height;

			this.visibleSubviews.add(visibleSubview);

			if (visibleSubview._dataSourceInfo.type == TableViewSubview.Info.Type.HEADER) {
				this.visibleHeaders.add(visibleSubview);
				changedHeaders = true;
			} else if(visibleSubview instanceof TableViewCell) {
				this.tableViewCells.add((TableViewCell) visibleSubview);
			}
		}

		if(changedHeaders) {
			this.headersChanged();
		}
	}

	private void headersChanged() {
		if(this.tableStyle != Style.PLAIN) return;

		Collections.sort(this.visibleHeaders, new TableViewSectionComparator());

		if(this.visibleHeaders.size() > 0) {
			for(View view : this.visibleHeaders) {
				this.bringSubviewToFront(view);
			}
		}
	}

	private void updateStickyHeaderPositions(float minY, int section) {
		List<TableViewSubview> visibleHeaders = this.visibleHeaders;
		TableViewSubview header;
		int headerSection;
		TableViewSubview nextHeader;
		float y;

		for (int idx = visibleHeaders.size() - 1; idx >= 0; idx--) {
			header = visibleHeaders.get(idx);
			headerSection = header._dataSourceInfo.section;

			if (headerSection == section) {
				if (visibleHeaders.size() > idx + 1 && (nextHeader = visibleHeaders.get(idx + 1)) != null && nextHeader.getFrame().origin.y < minY + header.getFrame().size.height) {
					y = nextHeader.getFrame().origin.y - nextHeader.getFrame().size.height;
				} else {
					if(this.tableHeaderView != null) {
						y = Math.max(minY, this.tableHeaderHeight);
					} else {
						y = minY;
					}
				}

				float y2 = Math.max(0.0f, y);

				if(y2 != y) {
					y = y2;
				}
			} else {
				y = this.sectionsInfo.get(headerSection).y;
			}

			Rect frame = header.getFrame();
			frame.origin.x = 0.0f;
			frame.origin.y = y;
			header.setFrame(frame);
		}
	}

	private TableViewSubview.Info getInfoForCell(IndexPath indexPath) {
		return new TableViewSubview.Info(TableViewSubview.Info.Type.CELL, indexPath);
	}

	private TableViewSubview.Info getInfoForHeader(int section) {
		return new TableViewSubview.Info(TableViewSubview.Info.Type.HEADER, section);
	}

	private TableViewSubview.Info getInfoForFooter(int section) {
		return new TableViewSubview.Info(TableViewSubview.Info.Type.FOOTER, section);
	}

	private TableViewSubview.Info getInfoForFirstViewInSection(int section) {
		SectionInfo sectionInfo = this.sectionsInfo.get(section);

		if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
			return this.getInfoForHeader(section);
		} else {
			if (sectionInfo.numberOfRows > 0) {
				return this.getInfoForCell(new IndexPath(section, 0));
			} else {
				if (sectionInfo.footerHeight > 0) {
					return this.getInfoForFooter(section);
				}
			}
		}

		return null;
	}

	private TableViewSubview.Info getInfoForLastViewInSection(int section) {
		SectionInfo sectionInfo = this.sectionsInfo.get(section);

		if (sectionInfo.footerHeight > 0) {
			return this.getInfoForFooter(section);
		} else {
			if (sectionInfo.numberOfRows > 0) {
				return this.getInfoForCell(new IndexPath(section, sectionInfo.numberOfRows - 1));
			}
		}

		if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
			return this.getInfoForHeader(section);
		}

		return null;
	}

	private TableViewSubview.Info getInfoForNextView(TableViewSubview.Info info) {
		TableViewSubview.Info nextViewInfo = null;

		if (info.type == TableViewSubview.Info.Type.CELL) {
			IndexPath indexPath = info.indexPath;
			SectionInfo sectionInfo = this.sectionsInfo.get(indexPath.section);
			if ((indexPath.row + 1) < sectionInfo.numberOfRows) {
				nextViewInfo = this.getInfoForCell(new IndexPath(indexPath.section, indexPath.row + 1));
			} else {
				if (sectionInfo.footerHeight > 0.0f) {
					nextViewInfo = this.getInfoForFooter(indexPath.section);
				} else {
					if ((indexPath.section + 1) < this.numberOfSections) {
						nextViewInfo = this.getInfoForFirstViewInSection(indexPath.section + 1);
					}
				}
			}
		} else {
			if (info.type == TableViewSubview.Info.Type.HEADER) {
				int section = info.section;
				SectionInfo sectionInfo = this.sectionsInfo.get(section);

				if (sectionInfo.numberOfRows > 0) {
					nextViewInfo = this.getInfoForCell(new IndexPath(section, 0));
				} else {
					if (sectionInfo.footerHeight > 0) {
						nextViewInfo = this.getInfoForFooter(section);
					} else {
						if (++section < this.numberOfSections) {
							nextViewInfo = this.getInfoForFirstViewInSection(section);
						}
					}
				}
			} else {
				int section = info.section;

				if (++section < this.numberOfSections) {
					nextViewInfo = this.getInfoForFirstViewInSection(section);
				}
			}
		}

		return nextViewInfo;
	}

	private TableViewSubview.Info getInfoForPreviousView(TableViewSubview.Info info) {
		TableViewSubview.Info previousInfo = null;

		if (info.type == TableViewSubview.Info.Type.CELL) {
			IndexPath indexPath = info.indexPath;

			if (indexPath.row >= 1) {
				previousInfo = this.getInfoForCell(new IndexPath(indexPath.section, indexPath.row - 1));
			} else {
				SectionInfo sectionInfo = this.sectionsInfo.get(indexPath.section);

				if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
					previousInfo = this.getInfoForHeader(indexPath.section);
				} else {
					if (indexPath.section >= 1) {
						previousInfo = this.getInfoForLastViewInSection(indexPath.section - 1);
					}
				}
			}
		} else {
			if (info.type == TableViewSubview.Info.Type.HEADER) {
				int section = info.section;
				if (--section >= 0) {
					previousInfo = this.getInfoForLastViewInSection(section);
				}
			} else {
				int section = info.section;
				SectionInfo sectionInfo = this.sectionsInfo.get(section);
				if (sectionInfo.numberOfRows > 0) {
					previousInfo = this.getInfoForCell(new IndexPath(section, sectionInfo.numberOfRows - 1));
				} else {
					if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
						previousInfo = this.getInfoForHeader(section);
					} else {
						if (--section >= 0) {
							previousInfo = this.getInfoForLastViewInSection(section);
						}
					}
				}
			}
		}

		return previousInfo;
	}

	public Rect getRectForSection(int section) {
		if (!this.isSectionValid(section)) {
			return Rect.zero();
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		float height = sectionInfo.headerHeight + sectionInfo.cumulativeRowHeight + sectionInfo.footerHeight;
		return new Rect(0, sectionInfo.y, this.getBounds().size.width, height);
	}

	public Rect getRectForRowAtIndexPath(IndexPath indexPath) {
		if (!this.isIndexPathValid(indexPath)) {
			return Rect.zero();
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(indexPath.section);
		float y = sectionInfo.y + sectionInfo.headerHeight;
		float height;

		if (this.usesCustomRowHeights) {
			int row = indexPath.row;

			for (int i = 0; i < row; i++) {
				y += sectionInfo.rowHeights[i];
			}

			height = sectionInfo.rowHeights[row];
		} else {
			y += indexPath.row * this.rowHeight;
			height = this.rowHeight;
		}

		return new Rect(0, y, this.getBounds().size.width, height);
	}

	public Rect getRectForFooterInSection(int section) {
		if (!this.isSectionValid(section) || this.tableStyle != Style.GROUPED) {
			return Rect.zero();
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		float y = sectionInfo.y + sectionInfo.headerHeight + sectionInfo.cumulativeRowHeight;
		return new Rect(0, y, this.getBounds().size.width, sectionInfo.footerHeight);
	}

	public Rect getRectForHeaderInSection(int section) {
		if (!this.isSectionValid(section)) {
			return Rect.zero();
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		float y = sectionInfo.y;

		if (this.tableStyle == Style.PLAIN) {
			for(TableViewSubview header : this.tableViewHeaders) {
				if (header._dataSourceInfo.section == section) {
					if (!header._isQueued) {
						return header.getFrame();
					}

					break;
				}
			}
		}

		return new Rect(0.0f, y, this.getBounds().size.width, sectionInfo.headerHeight);
	}

	public List<TableViewCell> getVisibleCells() {
		List<TableViewCell> visibleCells = new ArrayList<TableViewCell>();

		for(TableViewCell cell : this.tableViewCells) {
			if(!cell._isQueued) {
				visibleCells.add(cell);
			}
		}

		Collections.sort(visibleCells, new TableViewCellIndexPathComparator());

		return visibleCells;
	}

	public List<IndexPath> getIndexPathsForVisibleRows() {
		List<TableViewCell> visibleCells = this.getVisibleCells();

		if (visibleCells.size() == 0) {
			return null;
		}

		List<IndexPath> indexPaths = new ArrayList<IndexPath>();

		for(TableViewCell cell : visibleCells) {
			indexPaths.add(this.getIndexPathForCell(cell));
		}

		return indexPaths;
	}

	private TableViewSubview getPopulatedSubviewForInfo(TableViewSubview.Info info) {
		if (info == null) {
			return null;
		}

		TableViewSubview view;

		if (info.type == TableViewSubview.Info.Type.CELL) {
			view = this.getPopulatedCell(info);
		} else {
			if (info.type == TableViewSubview.Info.Type.HEADER) {
				view = this.getPopulatedHeader(info);
			} else {
				view = this.getPopulatedFooter(info);
			}
		}

		if (view == null) {
			return null;
		} else {
			return view;
		}
	}

	private TableViewSubview getPopulatedHeader(TableViewSubview.Info info) {
		if (info.section >= this.numberOfSections) {
			return null;
		}

		if (this.visibleHeaders.size() > 0 && this.visibleHeaders.get(0)._dataSourceInfo.section == info.section) {
			return this.visibleHeaders.get(0);
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(info.section);
		if(sectionInfo.headerHeight <= 0.0f) return null;

		TableViewSubview header = null;

		if(this.delegateHeaders != null) {
			header = this.delegateHeaders.getViewForHeaderInSecton(this, info.section);

			if(header != null) {
				header.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				header.createdByTableView = false;
			}
		}

		if(header == null) {
			header = this.dequeueView(this.headersQueuedForReuse);

			if (header == null) {
				header = this.tableStyle == Style.GROUPED ? new TableViewHeader.Grouped() : new TableViewHeader.Plain();
				header.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				header.setBackgroundColor(Color.TRANSPARENT);
				header.createdByTableView = true;
			}
		}

		Rect frame = header.getFrame();
		frame.size.height = sectionInfo.headerHeight;
		frame.size.width = this.getBounds().size.width;
		header.setFrame(frame);

		if(header instanceof TableViewHeader) {
			String text = (String)sectionInfo.header;
			((TableViewHeader) header).setText(text);
			((TableViewHeader) header).setHidden(text == null || text.length() == 0);
		}

		header._isQueued = false;
		header._dataSourceInfo = info;

		if(header.getSuperview() != this) {
			this.addSubview(header);
		}

		return header;
	}

	private TableViewSubview getPopulatedFooter(TableViewSubview.Info info) {
		if (info.section >= this.numberOfSections) {
			return null;
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(info.section);
		if(sectionInfo.footerHeight <= 0.0f) {
			return null;
		}


		TableViewSubview footer = null;

		if(this.delegateFooters != null) {
			footer = this.delegateFooters.getViewForFooterInSecton(this, info.section);

			if(footer != null) {
				footer.createdByTableView = false;
				footer.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			}
		}

		if(footer == null) {
			footer = this.dequeueView(this.footersQueuedForReuse);

			if (footer == null) {
				footer = new TableViewFooter();
				footer.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				footer.createdByTableView = true;
			}
		}

		Rect frame = footer.getFrame();
		frame.size.height = sectionInfo.footerHeight;
		frame.size.width = this.getBounds().size.width;
		footer.setFrame(frame);

		if(footer.getSuperview() != this) {
			this.addSubview(footer);
		}

		if(footer instanceof TableViewFooter) {
			((TableViewFooter) footer).setText((String)sectionInfo.footer);
		}

		footer._isQueued = false;
		footer._dataSourceInfo = info;

		return footer;
	}

	private TableViewCell getPopulatedCell(TableViewSubview.Info info) {
		IndexPath indexPath = info.indexPath;

		if(this.dataSource == null) return null; // Should never get here, but may as well check.

		TableViewCell cell = this.dataSource.getCellForRowAtIndexPath(this, indexPath);
		cell._firstRowInSection = indexPath.row == 0;
		cell._lastRowInSection = (indexPath.row == this.sectionsInfo.get(indexPath.section).numberOfRows - 1);
		cell._dataSourceInfo = info;
		cell.setTableStyle(this.getTableStyle());

		cell.setSelected(this.isRowAtIndexPathSelected(indexPath));

		if(cell._lastRowInSection && this.tableStyle == Style.GROUPED) {
			cell.setSeparatorStyle(TableViewCell.SeparatorStyle.NONE);
			cell.setSeparatorColor(Color.TRANSPARENT);
		} else {
			cell.setSeparatorStyle(this.separatorStyle);
			cell.setSeparatorColor(this.separatorColor);
		}

		Rect frame = cell.getFrame();
		frame.size.width = this.getBounds().size.width;
		frame.size.height = this.usesCustomRowHeights ? this.sectionsInfo.get(indexPath.section).rowHeights[indexPath.row] : this.getRowHeight();
		cell.setFrame(frame);

		if(this.delegateRowDisplay != null) {
			this.delegateRowDisplay.willDisplayCell(this, cell, indexPath);
		}

		if (cell.getSuperview() != this) {
			cell.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			this.insertSubview(cell, 0);
		}

		if(View.VIEW_LAYER_CLASS == ViewLayerNative.class) {
			cell.layoutIfNeeded();
		} else {
			cell.setNeedsLayout();
		}

		return cell;
	}

	public void registerClass(Class<? extends TableViewCell> cellClass, String reuseIdentifier) {
		this.registeredClasses.put(reuseIdentifier, cellClass);
	}

	public TableViewCell dequeueReusableCellWithIdentifier(Object reuseIdentifier) {
		TableViewCell cell = this.dequeueView(this.cellsQueuedForReuse.get(reuseIdentifier));

		if (cell != null) {
			cell.prepareForReuse();
		}

		return cell;
	}

	public TableViewCell dequeueReusableCellWithIdentifier(Object reuseIdentifier, IndexPath indexPath) {
		TableViewCell cell = this.dequeueReusableCellWithIdentifier(reuseIdentifier);

		if(cell == null) {
			Class<? extends TableViewCell> cellClass = this.registeredClasses.get(reuseIdentifier);

			if(cellClass == null) {
				return null;
			} else {
				Constructor<? extends TableViewCell> constructor;

				try {
					constructor = cellClass.getConstructor(TableViewCell.Style.class, Object.class);
				} catch (NoSuchMethodException e) {
					MWarn(e, "Could not find construct in registered cell class %s for reuseIdentifier %s", cellClass, reuseIdentifier);
					return null;
				}

				try {
					cell = constructor.newInstance(TableViewCell.Style.DEFAULT, reuseIdentifier);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		// cell.setFrame(getRectForRowAtIndexPath(indexPath));

		return cell;
	}

	@SuppressWarnings("unchecked")
	private void enqueueView(List queuedViews, TableViewSubview view) {
		this.visibleSubviews.remove(view);
		this.visibleHeaders.remove(view);

		boolean isCell = (view instanceof TableViewCell);
		if(isCell) {
			this.tableViewCells.remove((TableViewCell) view);
		}

		if(isCell || view.createdByTableView) {
			queuedViews.add(view);
		}

		view._isQueued = true;
		this.viewsToRemove.add(view);
	}

	private <T extends TableViewSubview> T dequeueView(List<T> queuedViews) {
		if (queuedViews == null || queuedViews.size() == 0) {
			return null;
		}

		T view = queuedViews.remove(queuedViews.size() - 1);
		view._isQueued = false;
		this.viewsToRemove.remove(view);

		return view;
	}

	private boolean isRowAtIndexPathSelected(IndexPath indexPath) {
		for(IndexPath selectedIndexPath : this.selectedRowsIndexPaths) {
			if(selectedIndexPath.equals(indexPath)) {
				return true;
			}
		}

		return false;
	}

	public IndexPath indexPathForSelectedRow() {
		if(this.selectedRowsIndexPaths.size() > 0) {
			return this.selectedRowsIndexPaths.iterator().next();
		} else {
			return null;
		}
	}

	public Set<IndexPath> indexPathsForSelectedRows() {
		return Collections.unmodifiableSet(this.selectedRowsIndexPaths);
	}

	private void selectionDidChangeForRowAtIndexPath(IndexPath indexPath, boolean selected) {
		if (selected) {
			if (this.isRowAtIndexPathSelected(indexPath)) {
				return;
			}

			this.selectedRowsIndexPaths.add(indexPath);
		} else {
			this.selectedRowsIndexPaths.remove(indexPath);
		}
	}

	public void deselectRowAtIndexPath(IndexPath indexPath, boolean animated) {
		this.deselectRowAtIndexPath(indexPath, animated, false);
	}

	private void deselectRowAtIndexPath(IndexPath indexPath, boolean animated, boolean alreadyAnimating) {
		if (indexPath == null) {
			return;
		}

		if(animated && !alreadyAnimating) {
			View.beginAnimations(null, null);
			View.setAnimationCurve(AnimationCurve.LINEAR);
			View.setAnimationDuration(TableViewCell.ANIMATED_HIGHLIGHT_DURATION);
		}

		TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);
		this.selectedRowsIndexPaths.remove(indexPath);

		if (cell != null) {
			this.markCellAsSelected(cell, false, animated);
		} else {
			this.selectionDidChangeForRowAtIndexPath(indexPath, false);
		}

		if(animated && !alreadyAnimating) {
			View.commitAnimations();
		}
	}

	public void selectRowAtIndexPath(IndexPath indexPath, boolean animated) {
		if (!this.isIndexPathValid(indexPath)) {
			throw new RuntimeException("Tried to select row with invalid index path: " + indexPath);
		}

		TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

		if(animated) {
			View.beginAnimations(null, null);
			View.setAnimationCurve(AnimationCurve.LINEAR);
			View.setAnimationDuration(TableViewCell.ANIMATED_HIGHLIGHT_DURATION);
		}

		if(this.selectedRowsIndexPaths.size() != 1 || !this.selectedRowsIndexPaths.contains(indexPath)) {
			Set<IndexPath> oldSelectedIndexPaths = new HashSet<IndexPath>(this.selectedRowsIndexPaths);
			oldSelectedIndexPaths.remove(indexPath);

			for(IndexPath oldSelectedIndexPath : oldSelectedIndexPaths) {
				this.deselectRowAtIndexPath(oldSelectedIndexPath, animated, animated);
			}
		}

		this.selectedRowsIndexPaths.add(indexPath);

		if (cell != null) {
			this.markCellAsSelected(cell, true, false);
		} else {
			this.selectionDidChangeForRowAtIndexPath(indexPath, true);
		}

		if(animated) {
			View.commitAnimations();
		}
	}

	private void markCellAsSelected(TableViewCell cell, boolean selected, boolean animated) {
		cell.setSelected(selected, animated);
		this.selectionDidChangeForRowAtIndexPath(cell._dataSourceInfo.indexPath, selected);
	}

	void disclosureButtonWasSelectedAtIndexPath(IndexPath indexPath) {
		TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

		if (cell.getAccessoryType() == TableViewCell.AccessoryType.DETAIL_DISCLOSURE_BUTTON) {
			if(this.delegateAccessorySelection != null) {
				this.delegateAccessorySelection.accessoryButtonTappedForRowWithIndexPath(this, indexPath);
			}
		}
	}

	void editAccessoryWasSelectedAtIndexPath(IndexPath indexPath) {
		boolean isEditing = (this.cellsBeingEditedPaths.indexOf(indexPath) > -1);
		this.resetAllEditingCells();

		if (isEditing) {
			TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);
			if(this.delegateEditing != null) {
				CharSequence title = this.delegateEditing.getTitleForDeleteConfirmationButtonForRowAtIndexPath(this, indexPath);
				if(title != null) {
					// TODO: Connect.
					// cell.deleteButton.setTitle(title);
				}
			}

			cell.setEditing(true);
			this.cellsBeingEditedPaths.add(indexPath);
		}
	}

	private void resetAllEditingCells() {
		for(IndexPath indexPath : this.cellsBeingEditedPaths) {
			this.getCellForRowAtIndexPath(indexPath).setEditing(false);
		}

		this.cellsBeingEditedPaths.clear();
	}

	private void deleteButtonWasSelectedAtIndexPath(IndexPath indexPath) {
		this.resetAllEditingCells();
		TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

		if (cell.isSelected()) {
			this.deselectRowAtIndexPath(indexPath, false);
		}

		if(this.dataSourceEditing != null) {
			this.dataSourceEditing.commitEditingStyleForRowAtIndexPath(this, TableViewCell.EditingStyle.DELETE, indexPath);
		}
	}

	private void updateIndex() {

	}

	private void scrollRectToVisible(Rect rect, ScrollPosition scrollPosition, boolean animated) {
		Rect bounds = this.getBounds();

		if (rect != null && rect.size.height > 0) {
			// adjust the rect based on the desired scroll position setting
			switch (scrollPosition) {
				case TOP: {
					rect.size.height = bounds.size.height;
					break;
				}

				case MIDDLE: {
					rect.origin.y -= (bounds.size.height / 2.0f) - rect.size.height;
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

			if(rect.origin.y < 0.0f) {
				rect.origin.y = 0.0f;
			}

			if(rect.maxY() > bounds.maxY()) {
				rect.origin.y -= rect.maxY() - bounds.maxY();
			}

			this.scrollRectToVisible(rect, animated);
		}
	}

	public void scrollToRowAtIndexPath(IndexPath indexPath, ScrollPosition scrollPosition, boolean animated) {
		Rect rect;

		if (indexPath.row == 0 && indexPath.section == 0) {
			Rect bounds = this.getBounds();
			rect = new Rect(0.0f, 0.0f, bounds.size.width, bounds.size.height);
		} else if (indexPath.row > 0) {
			rect = this.getRectForRowAtIndexPath(indexPath);
		} else {
			rect = this.getRectForSection(indexPath.section);
		}

		// this.scrollRectToVisible(rect, scrollPosition, animated);
	}

	public void touchesBegan(List<Touch> touches, Event event) {
		super.touchesBegan(touches, event);
		this.touchesMoved = false;

		ignoreTouches = this.decelerating;
		if(ignoreTouches) return;

		if(touches.size() == 1) {
			final Touch touch = touches.get(0);

			this.cellTouchCallback = performAfterDelay(100, new Runnable() {
				public void run() {
					IndexPath indexPath = getIndexPathForRowAtPoint(touch.locationInView(TableView.this));
					touchedCell = getCellForRowAtIndexPath(indexPath);

					if (touchedCell != null) {
						if (delegateHighlighting != null) {
							if (!delegateHighlighting.shouldHighlightRowAtIndexPath(TableView.this, indexPath)) {
								return;
							}
						}

						touchedCell.setHighlighted(true);

						if (delegateHighlighting != null) {
							delegateHighlighting.didHighlightRowAtIndexPath(TableView.this, indexPath);
						}
					}
				}
			});
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		super.touchesMoved(touches, event);
		if(ignoreTouches) return;

		if(this.panGestureRecognizer.getState() != GestureRecognizer.State.POSSIBLE) {
			if(this.cellTouchCallback != null) {
				cancelCallbacks(cellTouchCallback);
				this.cellTouchCallback = null;
			}

			if(this.touchedCell != null) {
				this.touchedCell.setHighlighted(false);
				this.touchedCell = null;
			}

			this.touchesMoved = true;
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		super.touchesEnded(touches, event);
		if(ignoreTouches) return;

		if(this.cellTouchCallback != null) {
			cancelCallbacks(this.cellTouchCallback);
			this.cellTouchCallback = null;
		}

		if(this.touchedCell != null) {
			this.selectCellDueToTouchEvent(this.touchedCell, this.getIndexPathForCell(this.touchedCell), false);
			this.touchedCell = null;
		} else if(!this.touchesMoved) {
			Touch touch = touches.get(0);
			IndexPath indexPath = this.getIndexPathForRowAtPoint(touch.locationInView(TableView.this));
			TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

			if (cell != null) {
				this.selectCellDueToTouchEvent(cell, indexPath, false);
			}
		}
	}

	private void selectCellDueToTouchEvent(TableViewCell cell, IndexPath indexPath, boolean animated) {
		TableViewCell selectingCell = cell;

		if(!cell.isHighlighted()) {
			if(this.delegateHighlighting != null && !this.delegateHighlighting.shouldHighlightRowAtIndexPath(this, indexPath)) {
				return;
			}

			cell.setHighlighted(true, animated);

			if(this.delegateHighlighting != null) {
				this.delegateHighlighting.didHighlightRowAtIndexPath(this, indexPath);
			}
		}

		if(this.selectedRowsIndexPaths.size() > 0) {
			for(IndexPath selectedRowIndexPath : this.selectedRowsIndexPaths) {
				// TODO: Handle delegate call
				this.deselectRowAtIndexPath(selectedRowIndexPath, animated, animated);
			}
		}

		if(this.delegateSelection != null) {
			IndexPath newIndexPath = this.delegateSelection.willSelectRowAtIndexPath(this, indexPath);

			if(newIndexPath == null || !newIndexPath.equals(indexPath)) {
				selectingCell.setHighlighted(false, animated);

				if(this.delegateHighlighting != null) {
					this.delegateHighlighting.didHighlightRowAtIndexPath(this, indexPath);
				}

				if(newIndexPath == null) {
					return;
				} else {
					selectingCell = this.getCellForRowAtIndexPath(newIndexPath);

					if(selectingCell != null) {
						selectingCell.setHighlighted(true, animated);
						indexPath = newIndexPath;

						if(this.delegateHighlighting != null) {
							this.delegateHighlighting.didHighlightRowAtIndexPath(this, newIndexPath);
						}
					} else {
						return;
					}
				}
			}
		}

		this.markCellAsSelected(selectingCell, true, animated);

		if(this.delegateSelection != null) {
			if(selectingCell.shouldPlayClickSoundOnSelection()) {
				selectingCell.playClickSound();
			}

			this.delegateSelection.didSelectRowAtIndexPath(this, indexPath);
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		super.touchesCancelled(touches, event);

		if(this.cellTouchCallback != null) {
			cancelCallbacks(this.cellTouchCallback);
			this.cellTouchCallback = null;
		}

		if(this.touchedCell != null) {
			this.touchedCell.setHighlighted(false);
			this.touchedCell = null;
		}
	}

}