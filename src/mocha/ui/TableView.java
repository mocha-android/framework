/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 Mocha. All rights reserved.
*/

package mocha.ui;

import android.util.SparseArray;
import android.view.ViewGroup;
import mocha.foundation.IndexPath;
import mocha.foundation.Range;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.lang.reflect.Constructor;
import java.util.*;

public class TableView extends ScrollView {
	static final float PLAIN_HEADER_HEIGHT = 23.0f;
	static final float GROUPED_TABLE_Y_MARGIN = 10.0f;
	private static final float DEFAULT_ROW_HEIGHT = 44.0f;

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
			public String getTitleForHeaderInSection(TableView tableView, int section);
		}

		public interface Footers extends DataSource {
			public String getTitleForFooterInSection(TableView tableView, int section);
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
			public TableViewSubview getViewForHeaderInSection(TableView tableView, int section);
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

	DataSource dataSource;
	DataSource.Editing dataSourceEditing;
	DataSource.Headers dataSourceHeaders;
	DataSource.Footers dataSourceFooters;

	Delegate delegate;
	Delegate.RowSizing delegateRowSizing;
	Delegate.RowDisplay delegateRowDisplay;
	Delegate.Selection delegateSelection;
	Delegate.Deselection delegateDeselection;
	Delegate.Highlighting delegateHighlighting;
	Delegate.Headers delegateHeaders;
	Delegate.Footers delegateFooters;
	Delegate.Editing delegateEditing;
	Delegate.AccessorySelection delegateAccessorySelection;

	private TableViewRowData rowData;
	private Map<Object,List<TableViewCell>> cellsQueuedForReuse;
	private Map<Object,Class<? extends TableViewCell>> registeredClasses;
	private List<TableViewSubview> headersQueuedForReuse;
	private List<TableViewSubview> footersQueuedForReuse;
	private Runnable cellTouchCallback;
	private TableViewCell touchedCell;
	private boolean touchesMoved;
	private boolean ignoreTouches;
	private IndexPath selectedRowIndexPath;
	private Set<IndexPath> selectedRowsIndexPaths;
	private List<IndexPath> cellsBeingEditedPaths;
	private View tableHeaderView;
	private View tableFooterView;
	private boolean tableHeaderAttached;
	private boolean tableFooterAttached;
	private boolean hasLoadedData;

	private Range visibleRows;
	private List<TableViewCell> visibleCells;
	private SparseArray<TableViewSubview> visibleHeaderViews;
	private SparseArray<TableViewSubview> visibleFooterViews;
	private List<TableViewSubview> viewsToRemove;
	private boolean shouldUpdateVisibleViewFrames;

	public TableView(Style style) {
		this(style, new Rect(0.0f, 0.0f, 320.0f, 480.0f));
	}

	public TableView(Style style, Rect frame) {
		super(frame);

		this.tableStyle = style == null ? Style.PLAIN : style;
		this.setAllowsSelection(true);
		this.rowData = new TableViewRowData(this);
		this.cellsQueuedForReuse = new HashMap<Object, List<TableViewCell>>();
		this.registeredClasses = new HashMap<Object, Class<? extends TableViewCell>>();
		this.headersQueuedForReuse = new ArrayList<TableViewSubview>();
		this.footersQueuedForReuse = new ArrayList<TableViewSubview>();
		this.touchedCell = null;
		this.selectedRowIndexPath = null;
		this.selectedRowsIndexPaths = new HashSet<IndexPath>();
		this.editing = false;
		this.cellsBeingEditedPaths = new ArrayList<IndexPath>();

		this.visibleCells = new ArrayList<TableViewCell>();
		this.visibleHeaderViews = new SparseArray<TableViewSubview>();
		this.visibleFooterViews = new SparseArray<TableViewSubview>();
		this.viewsToRemove = new ArrayList<TableViewSubview>();

		this.setAlwaysBounceVertical(true);
	}

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.setRowHeight(DEFAULT_ROW_HEIGHT);
		this.setBackgroundColor(Color.WHITE);
		this.separatorStyle = TableViewCell.SeparatorStyle.SINGLE_LINE;
		this.separatorColor = Color.white(0.88f, 1.0f);
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

//				MLog(format, new Rect(viewGroup.getX(), viewGroup.getY(), viewGroup.getWidth(), viewGroup.getHeight()),
//						(subview instanceof TableViewCell ? subview._dataSourceInfo.indexPath.section + "x" + subview._dataSourceInfo.indexPath.row : subview._dataSourceInfo.section),
//						subview._dataSourceInfo.type, subview.getFrame(), this.visibleHeaders.contains(subview),
//						(subview instanceof TableViewCell ? this.tableViewCells.contains(subview) : "false"),
//						this.visibleSubviews.contains(subview), subview._isQueued, viewGroup.getY());
			}
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

		this.rowData.tableHeaderHeightDidChangeToHeight(this.tableHeaderView != null ? this.tableHeaderView.getFrame().size.height : 0.0f);
		this.tableHeaderAttached = false;

		if(this.hasLoadedData) {
			this.reloadAllViews();
		}
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

		this.rowData.tableFooterHeightDidChangeToHeight(this.tableFooterView != null ? this.tableFooterView.getFrame().size.height : 0.0f);

		Size contentSize = this.getContentSize();
		contentSize.height += this.rowData.getTableHeight();
		this.setContentSize(contentSize);

		if(this.hasLoadedData) {
			this.reloadAllViews();
		}
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;

		super.setFrame(frame);

		if(!frame.size.equals(oldSize)) {
			if(oldSize.width != frame.size.width) {
				this.rowData.tableViewWidthDidChangeToWidth(frame.size.width);
			}

			this.shouldUpdateVisibleViewFrames = true;
			this.updateIndex();
			this.setContentSize(new Size(frame.size.width, this.rowData.getTableHeight()));
			this.layoutSubviews();
			this.shouldUpdateVisibleViewFrames = false;
		}
	}

	public int getNumberOfSections() {
		return this.rowData.getNumberOfSections();
	}

	public int getNumberOfRowsInSection(int section) {
		return this.rowData.getNumberOfRowsInSection(section);
	}

	public TableViewCell getCellForRowAtIndexPath(IndexPath indexPath) {
		if (indexPath != null) {
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
		return cell._indexPath;
	}

	public IndexPath getIndexPathForRowAtPoint(Point point) {
		return this.rowData.getIndexPathForRowAtPoint(point);
	}

	private void clearAllViews() {
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
						this.delegateRowDisplay.didEndDisplayingCell(this, cell, cell._indexPath);
					}
				}
			}
		}

		this.visibleHeaderViews.clear();
		this.visibleFooterViews.clear();
		this.visibleCells.clear();
		this.headersQueuedForReuse.clear();
		this.footersQueuedForReuse.clear();
		this.viewsToRemove.clear();
		this.visibleRows = null;
		View.setAnimationsEnabled(areAnimationsEnabled);
	}

	private void reloadAllViews() {
		this.reloadAllViews(true);
	}

	private void reloadAllViews(boolean clearOldViews) {
		boolean areAnimationsEnabled = View.areAnimationsEnabled();
		View.setAnimationsEnabled(false);

		if(clearOldViews) {
			this.clearAllViews();
		}

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

	public void reloadData() {
		if(this.reloadingData) {
			MWarn("WARNING: Nested call to TableView.reloadData() detected.  This is unsupported.");
		}

		this.reloadingData = true;

		this.clearAllViews();

		this.hasLoadedData = true;
		this.rowData.reloadData();
		this.setContentSize(new Size(this.getBounds().size.width, this.rowData.getTableHeight()));

		this.reloadAllViews(false);

		this.reloadingData = false;
	}

	boolean isEmpty() {
		return this.rowData.isEmpty();
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

		if (this.rowData.getNumberOfSections() == 0) {
			return;
		}

		if(this.getSubviews().size() > 20) {
			MWarn("subviews > 20 (%d), offset: %f", this.getSubviews().size(), this.getContentOffset().y);
		}

		this.updateVisibleCells();

		for(TableViewSubview view : this.viewsToRemove) {
			view.removeFromSuperview();
		}

		this.viewsToRemove.clear();
	}

	private void updateVisibleCells() {
		Rect bounds = this.getBounds();

		Range visibleRows = this.rowData.getGlobalRowsInRect(bounds);

		if(this.shouldUpdateVisibleViewFrames || this.visibleRows == null || !this.visibleRows.equals(visibleRows)) {
			this.visibleRows = visibleRows;

			Iterator<TableViewCell> cells = this.visibleCells.iterator();

			while(cells.hasNext()) {
				TableViewCell cell = cells.next();

				if(!visibleRows.containsLocation(cell._globalRow)) {
					this.enqueueCell(cell);
					cells.remove();
				} else if(this.shouldUpdateVisibleViewFrames) {
					cell.setFrame(this.rowData.getRectForGlobalRow(cell._globalRow));
				}
			}

			int visibleCellsSize = this.visibleCells.size();
			int visibleCellsStart = visibleCellsSize > 0 ? this.visibleCells.get(0)._globalRow : Integer.MAX_VALUE;
			int visibleCellsEnd = visibleCellsSize > 0 ? this.visibleCells.get(visibleCellsSize - 1)._globalRow : Integer.MIN_VALUE;

			int start = (int)visibleRows.location;
			int end = (int)visibleRows.max();

			for(int globalRow = start; globalRow < end; globalRow++) {
				if(globalRow < visibleCellsStart || globalRow > visibleCellsEnd) {
					IndexPath indexPath = this.rowData.getIndexPathForRowAtGlobalRow(globalRow);
					TableViewCell cell = this.createPreparedCellForRowAtIndexPath(indexPath);
					cell._globalRow = globalRow;
					cell.setFrame(this.rowData.getRectForGlobalRow(globalRow));

					if(this.delegateRowDisplay != null) {
						this.delegateRowDisplay.willDisplayCell(this, cell, indexPath);
					}

					if (cell.getSuperview() != this) {
						cell.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
						this.insertSubview(cell, 0);
					}

					cell.layoutSubviews();

					this.visibleCells.add(globalRow - start, cell);
				}
			}
		}

		this.updateTableHeadersAndFooters(bounds);
		this.updateVisibleHeadersAndFooters(bounds);
	}

	private void updateTableHeadersAndFooters(Rect bounds) {
		float minY = bounds.origin.y;
		float maxY = minY + bounds.size.height;

		if(this.tableHeaderView != null) {
			if(minY <= this.rowData.getHeightForTableHeaderView()) {
				if(!this.tableHeaderAttached) {
					this.tableHeaderView.setFrame(this.rowData.getRectForTableHeaderView());
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
			float offset = this.getContentSize().height - this.rowData.getHeightForTableFooterView();
			if(maxY >= offset) {
				if(!this.tableFooterAttached) {
					this.tableFooterView.setFrame(this.rowData.getRectForTableFooterView());
					this.tableFooterView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
					this.insertSubview(this.tableFooterView, 0);
					this.tableFooterAttached = true;
				} else if(this.tableFooterView.getFrame().origin.y < offset) {
					this.tableFooterView.setFrame(this.rowData.getRectForTableFooterView());
				}
			} else if(this.tableFooterAttached) {
				this.tableFooterView.removeFromSuperview();
				this.tableFooterAttached = false;
			}
		}
	}

	private void updateVisibleHeadersAndFooters(Rect visibleBounds) {
		Range visibleSections = this.rowData.getSectionsInRect(visibleBounds);

		this.ensureHeaderFooterViewsInRange(this.visibleHeaderViews, visibleSections, true);
		this.ensureHeaderFooterViewsInRange(this.visibleFooterViews, visibleSections, false);

		int start = (int)visibleSections.location;
		int end = (int)visibleSections.max();

		for(int section = start; section < end; section++) {
			boolean hasHeader = this.rowData.hasHeaderForSection(section);
			boolean hasFooter = this.rowData.hasFooterForSection(section);

			if(hasHeader || hasFooter) {
				if(hasHeader) {
					TableViewSubview headerView = this.visibleHeaderViews.get(section);

					if(headerView == null || this.tableStyle != Style.PLAIN) {
						Rect headerRect = this.rowData.getRectForHeaderInSection(section);

						if(visibleBounds.intersects(headerRect) || this.tableStyle == Style.PLAIN) {
							if(headerView == null) {
								headerView = this.createPreparedHeaderForSection(section);
								headerView.setFrame(headerRect);
								this.addSubview(headerView);
								this.visibleHeaderViews.put(section, headerView);
							}
						} else if(headerView != null) {
							this.enqueueHeaderFooterView(headerView, true);
						}
					}
				}

				if(hasFooter) {
					Rect footerRect = this.rowData.getRectForFooterInSection(section);
					TableViewSubview footerView = this.visibleFooterViews.get(section);

					if(visibleBounds.intersects(footerRect)) {
						if(footerView == null) {
							footerView = this.createPreparedFooterForSection(section);
							footerView.setFrame(footerRect);

							if(footerView.getSuperview() != this) {
								this.addSubview(footerView);
							}

							this.visibleFooterViews.put(section, footerView);
						}
					} else if(footerView != null) {
						this.enqueueHeaderFooterView(footerView, false);
					}
				}
			}
		}

		if(this.tableStyle == Style.PLAIN) {
			this.updatePinnedTableHeader(visibleBounds);
		}
	}

	private void ensureHeaderFooterViewsInRange(SparseArray<TableViewSubview> headerFooterViews, Range visibleSections, boolean headers) {
		if(headerFooterViews.size() == 0) return;

		boolean _break = false;

		while(!_break) {
			_break = true;

			int size = headerFooterViews.size();

			for(int i = 0; i < size; i++) {
				int section = headerFooterViews.keyAt(i);

				if(!visibleSections.containsLocation(section)) {
					this.enqueueHeaderFooterView(headerFooterViews.get(section), headers);
					headerFooterViews.removeAt(i);
					_break = false;
					break;
				}
			}
		}
	}

	private void updatePinnedTableHeader(Rect bounds) {
		if(this.tableStyle == Style.PLAIN) {
			int numberOfSections = this.visibleHeaderViews.size();

			for(int i = 0; i < numberOfSections; i++) {
				int section = this.visibleHeaderViews.keyAt(i);
				this.visibleHeaderViews.get(section).setFrame(this.rowData.getFloatingRectForHeaderInSection(section, bounds));
			}
		}
	}

	private void enqueueCell(TableViewCell cell) {
		List<TableViewCell> queuedViews = this.cellsQueuedForReuse.get(cell._reuseIdentifier);

		if(queuedViews == null) {
			List<TableViewCell> queuedCells = new ArrayList<TableViewCell>();
			this.cellsQueuedForReuse.put(cell._reuseIdentifier, queuedCells);
			queuedViews = queuedCells;
		}

		this.enqueueView(queuedViews, cell);
	}

	private void enqueueHeaderFooterView(TableViewSubview headerFooterView, boolean header) {
		if(header) {
			this.visibleHeaderViews.remove(headerFooterView._section);
			this.enqueueView(this.headersQueuedForReuse, headerFooterView);
		} else {
			this.visibleFooterViews.remove(headerFooterView._section);
			this.enqueueView(this.footersQueuedForReuse, headerFooterView);
		}

		this.viewsToRemove.add(headerFooterView);
	}

	public Rect getRectForSection(int section) {
		return this.rowData.getRectForSection(section);
	}

	public Rect getRectForRowAtIndexPath(IndexPath indexPath) {
		return this.rowData.getRectForRow(indexPath.section, indexPath.row);
	}

	public Rect getRectForFooterInSection(int section) {
		return this.rowData.getRectForFooterInSection(section);
	}

	public Rect getRectForHeaderInSection(int section) {
		return this.rowData.getRectForHeaderInSection(section);
	}

	public List<TableViewCell> getVisibleCells() {
		return new ArrayList<TableViewCell>(this.visibleCells);
	}

	public List<IndexPath> getIndexPathsForVisibleRows() {
		List<IndexPath> indexPaths = new ArrayList<IndexPath>();

		for(TableViewCell cell : this.visibleCells) {
			indexPaths.add(this.getIndexPathForCell(cell));
		}

		return indexPaths;
	}

	private TableViewSubview createPreparedHeaderForSection(int section) {
		if (section >= this.rowData.getNumberOfSections()) {
			return null;
		}

		TableViewSubview header = null;

		if(this.delegateHeaders != null) {
			header = this.delegateHeaders.getViewForHeaderInSection(this, section);

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

		header.setFrame(frame);

		if(header instanceof TableViewHeader) {
			String text = this.dataSourceHeaders != null ? this.dataSourceHeaders.getTitleForHeaderInSection(this, section) : null;
			((TableViewHeader) header).setText(text);
		}

		header._isQueued = false;
		header._section = section;

		return header;
	}

	private TableViewSubview createPreparedFooterForSection(int section) {
		if (section >= this.rowData.getNumberOfSections()) {
			return null;
		}

		TableViewSubview footer = null;

		if(this.delegateFooters != null) {
			footer = this.delegateFooters.getViewForFooterInSecton(this, section);

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

		if(footer instanceof TableViewFooter) {
			((TableViewFooter) footer).setText(this.dataSourceFooters != null ? this.dataSourceFooters.getTitleForFooterInSection(this, section) : null);
		}

		footer._isQueued = false;
		footer._section = section;

		return footer;
	}

	private TableViewCell createPreparedCellForRowAtIndexPath(IndexPath indexPath) {
		if(this.dataSource == null) return null; // Should never get here, but may as well check.

		TableViewCell cell = this.dataSource.getCellForRowAtIndexPath(this, indexPath);
		cell._firstRowInSection = indexPath.row == 0;
		cell._lastRowInSection = (indexPath.row == this.rowData.getNumberOfRowsInSection(indexPath.section) - 1);
		cell._indexPath = indexPath;
		cell.setTableStyle(this.getTableStyle());

		cell.setSelected(this.isRowAtIndexPathSelected(indexPath));

		if(cell._lastRowInSection && this.tableStyle == Style.GROUPED) {
			cell.setSeparatorStyle(TableViewCell.SeparatorStyle.NONE);
			cell.setSeparatorColor(Color.TRANSPARENT);
		} else {
			cell.setSeparatorStyle(this.separatorStyle);
			cell.setSeparatorColor(this.separatorColor);
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
		if((view instanceof TableViewCell) || view.createdByTableView) {
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

	public IndexPath getIndexPathForSelectedRow() {
		if(this.selectedRowsIndexPaths.size() > 0) {
			return this.selectedRowsIndexPaths.iterator().next();
		} else {
			return null;
		}
	}

	public Set<IndexPath> getIndexPathsForSelectedRows() {
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
		if (!this.rowData.isValidIndexPath(indexPath)) {
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
		this.selectionDidChangeForRowAtIndexPath(cell._indexPath, selected);
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

	public void scrollToRowAtIndexPath(IndexPath indexPath, ScrollPosition scrollPosition, boolean animated) {
		Rect rect;

		if (indexPath.row < 0) {
			rect = this.getRectForSection(indexPath.section);
		} else {
			rect = this.getRectForRowAtIndexPath(indexPath);

			if((indexPath.row == 0 || this.tableStyle == Style.PLAIN) && this.rowData.hasHeaderForSection(indexPath.section)) {
				float headerHeight = this.rowData.getHeightForHeaderInSection(indexPath.section);
				rect.origin.y -= headerHeight;
				rect.size.height += headerHeight;
			}
		}

		Rect bounds = this.getBounds();
		float calculateY;

		if(scrollPosition == ScrollPosition.NONE) {
			if(bounds.contains(rect)) {
				return;
			} else if(rect.origin.y < bounds.origin.y) {
				scrollPosition = ScrollPosition.TOP;
			} else if(rect.maxY() > bounds.maxY()) {
				scrollPosition = ScrollPosition.BOTTOM;
			} else {
				scrollPosition = ScrollPosition.MIDDLE;
			}
		}

		switch(scrollPosition) {
			case TOP:
				rect = new Rect(rect.origin.x, rect.origin.y, rect.size.width, bounds.size.height);
				break;

			case BOTTOM:
				calculateY = Math.max(rect.origin.y-(bounds.size.height-rect.size.height), -this.getContentInset().top);
				rect = new Rect(rect.origin.x, calculateY, rect.size.width, bounds.size.height);
				break;

			case MIDDLE:
				calculateY = Math.max(rect.origin.y - ((bounds.size.height / 2) - (rect.size.height / 2)), -this.getContentInset().top);
				rect = new Rect(rect.origin.x, calculateY, rect.size.width, bounds.size.height);
				break;
		}

		this.scrollRectToVisible(rect, animated);
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