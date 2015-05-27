package mocha.ui;

import android.util.SparseArray;
import mocha.foundation.IndexPath;
import mocha.foundation.Range;
import mocha.foundation.Sets;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.lang.reflect.Constructor;
import java.util.*;

public class TableView extends ScrollView implements GestureRecognizer.Delegate {
	static final float PLAIN_HEADER_HEIGHT = 23.0f;
	static final float GROUPED_TABLE_Y_MARGIN = 10.0f;
	private static final float DEFAULT_ROW_HEIGHT = 64.0f;
	private static final TableViewCell.State DEFAULT_STATE[] = new TableViewCell.State[]{TableViewCell.State.DEFAULT};
	private static final TableViewCell.State EDIT_CONTROL_STATE[] = new TableViewCell.State[]{TableViewCell.State.SHOWING_EDIT_CONTROL};

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
	private final EdgeInsets separatorInset;
	private int separatorColor;
	private boolean separatorInsetShouldInsetBackgroundViews;
	private boolean editing;
	private boolean fullEditing;
	private float rowHeight;
	private boolean allowsSelection;
	private boolean allowsMultipleSelection;
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
	private Map<Object, List<TableViewCell>> cellsQueuedForReuse;
	private Map<String, List<TableViewHeaderFooterView>> headerFooterViewsQueuedForReuse;
	private Map<Object, Constructor<? extends TableViewCell>> registeredCellClasses;
	private Map<String, Constructor<? extends TableViewHeaderFooterView>> registeredHeaderFooterViewClasses;
	private Runnable cellTouchCallback;
	private TableViewCell touchedCell;
	private boolean touchesMoved;
	private boolean ignoreTouches;
	private IndexPath selectedRowIndexPath;
	private Set<IndexPath> selectedRowsIndexPaths;
	private List<IndexPath> cellsBeingEditedPaths;
	private View backgroundView;
	private View tableHeaderView;
	private View tableFooterView;
	private boolean tableHeaderAttached;
	private boolean tableFooterAttached;
	private boolean hasLoadedData;
	private boolean showingDeleteConfirmation;
	private boolean restoreScrollingEnabled;

	private Range visibleRows;
	private List<TableViewCell> visibleCells;
	private SparseArray<TableViewSubview> visibleHeaderViews;
	private SparseArray<TableViewSubview> visibleFooterViews;
	private List<TableViewSubview> viewsToHideForReuseOrRemove;
	private boolean shouldUpdateVisibleViewFrames;

	public TableView(Style style) {
		this(style, new Rect(0.0f, 0.0f, 320.0f, 480.0f));
	}

	public TableView(Rect frame, Style style) {
		this(style, frame);
	}

	public TableView(Style style, Rect frame) {
		super(frame);

		this.tableStyle = style == null ? Style.PLAIN : style;
		this.separatorInset = new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f);
		this.separatorInsetShouldInsetBackgroundViews = true;
		this.setAllowsSelection(true);
		this.rowData = new TableViewRowData(this);
		this.cellsQueuedForReuse = new HashMap<>();
		this.headerFooterViewsQueuedForReuse = new HashMap<>();
		this.registeredCellClasses = new HashMap<>();
		this.registeredHeaderFooterViewClasses = new HashMap<>();
		this.touchedCell = null;
		this.selectedRowIndexPath = null;
		this.selectedRowsIndexPaths = new HashSet<>();
		this.editing = false;
		this.cellsBeingEditedPaths = new ArrayList<>();

		this.visibleCells = new ArrayList<>();
		this.visibleHeaderViews = new SparseArray<>();
		this.visibleFooterViews = new SparseArray<>();
		this.viewsToHideForReuseOrRemove = new ArrayList<>();

		this.setAlwaysBounceVertical(true);

		if (style == Style.GROUPED) {
			this.registerHeaderFooterViewClass(TableViewHeaderFooterGroupedView.class, TableViewHeaderFooterGroupedView.REUSE_IDENTIFIER);
		} else {
			this.registerHeaderFooterViewClass(TableViewHeaderFooterPlainView.class, TableViewHeaderFooterPlainView.REUSE_IDENTIFIER);
		}

		LongPressGestureRecognizer longPressGestureRecognizer = new LongPressGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				handleCellSwipe(gestureRecognizer);
			}
		});
		longPressGestureRecognizer.setDelegate(this);
		this.addGestureRecognizer(longPressGestureRecognizer);
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

		for (TableViewCell cell : this.visibleCells) {
			cell.setSeparatorStyle(this.separatorStyle);
		}
	}

	public EdgeInsets getSeparatorInset() {
		return this.separatorInset.copy();
	}

	public void setSeparatorInset(EdgeInsets separatorInset) {
		this.separatorInset.set(separatorInset);

		for (TableViewCell cell : this.visibleCells) {
			cell.setInheritedSeparatorInset(this.separatorInset);
		}
	}

	public int getSeparatorColor() {
		return separatorColor;
	}

	public void setSeparatorColor(int separatorColor) {
		this.separatorColor = separatorColor;

		for (TableViewCell cell : this.visibleCells) {
			cell.setSeparatorColor(separatorColor);
		}
	}

	public boolean getSeparatorInsetShouldInsetBackgroundViews() {
		return this.separatorInsetShouldInsetBackgroundViews;
	}

	public void setSeparatorInsetShouldInsetBackgroundViews(boolean separatorInsetShouldInsetBackgroundViews) {
		this.separatorInsetShouldInsetBackgroundViews = separatorInsetShouldInsetBackgroundViews;

		for (TableViewCell cell : this.visibleCells) {
			cell.setInheritedSeparatorInsetShouldInsetBackgroundViews(separatorInsetShouldInsetBackgroundViews);
		}
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		if (dataSource != null) {
			this.dataSource = dataSource;

			if (dataSource instanceof DataSource.Editing) {
				this.dataSourceEditing = (DataSource.Editing) dataSource;
			} else {
				this.dataSourceEditing = null;
			}

			if (dataSource instanceof DataSource.Headers) {
				this.dataSourceHeaders = (DataSource.Headers) dataSource;
			} else {
				this.dataSourceHeaders = null;
			}

			if (dataSource instanceof DataSource.Footers) {
				this.dataSourceFooters = (DataSource.Footers) dataSource;
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

	public Delegate getDelegate() {
		return this.delegate;
	}

	public void setDelegate(Delegate delegate) {
		if (delegate != null) {
			this.delegate = delegate;

			if (delegate instanceof Delegate.RowSizing) {
				this.delegateRowSizing = (Delegate.RowSizing) delegate;
			} else {
				this.delegateRowSizing = null;
			}

			if (delegate instanceof Delegate.RowDisplay) {
				this.delegateRowDisplay = (Delegate.RowDisplay) delegate;
			} else {
				this.delegateRowDisplay = null;
			}

			if (delegate instanceof Delegate.Selection) {
				this.delegateSelection = (Delegate.Selection) delegate;
			} else {
				this.delegateSelection = null;
			}

			if (delegate instanceof Delegate.Deselection) {
				this.delegateDeselection = (Delegate.Deselection) delegate;
			} else {
				this.delegateDeselection = null;
			}

			if (delegate instanceof Delegate.Highlighting) {
				this.delegateHighlighting = (Delegate.Highlighting) delegate;
			} else {
				this.delegateHighlighting = null;
			}

			if (delegate instanceof Delegate.Headers) {
				this.delegateHeaders = (Delegate.Headers) delegate;
			} else {
				this.delegateHeaders = null;
			}

			if (delegate instanceof Delegate.Footers) {
				this.delegateFooters = (Delegate.Footers) delegate;
			} else {
				this.delegateFooters = null;
			}

			if (delegate instanceof Delegate.Editing) {
				this.delegateEditing = (Delegate.Editing) delegate;
			} else {
				this.delegateEditing = null;
			}

			if (delegate instanceof Delegate.AccessorySelection) {
				this.delegateAccessorySelection = (Delegate.AccessorySelection) delegate;
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

	public boolean isEditing() {
		return this.editing || this.fullEditing;
	}

	public void setEditing(boolean editing) {
		this.setEditing(editing, false);
	}

	public void setEditing(boolean editing, boolean animated) {
		if (editing == this.fullEditing) {
			return;
		}

		if (!editing && this.showingDeleteConfirmation) {
			this.showingDeleteConfirmation = false;

			if (this.restoreScrollingEnabled) {
				this.setScrollEnabled(true);
				this.restoreScrollingEnabled = false;
			}
		}

		this.fullEditing = editing;
		this.editing = editing;

		if (this.fullEditing) {
			this.transitionCellsToState(this.visibleCells, animated, true, TableViewCell.State.SHOWING_EDIT_CONTROL);
		} else {
			this.transitionCellsToState(this.visibleCells, animated, false, TableViewCell.State.DEFAULT);
		}
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

	public boolean getAllowsMultipleSelection() {
		return allowsMultipleSelection;
	}

	public void setAllowsMultipleSelection(boolean allowsMultipleSelection) {
		this.allowsMultipleSelection = allowsMultipleSelection;
	}

	public View getBackgroundView() {
		return this.backgroundView;
	}

	public void setBackgroundView(View backgroundView) {
		if (this.backgroundView != null) {
			this.backgroundView.removeFromSuperview();
			this.backgroundView = null;
		}

		this.backgroundView = backgroundView;

		if (this.backgroundView != null) {
			this.insertSubview(this.backgroundView, 0);
			this.setNeedsLayout();
		}
	}

	public View getTableHeaderView() {
		return this.tableHeaderView;
	}

	public void setTableHeaderView(View tableHeaderView) {
		if (this.tableHeaderView != tableHeaderView) {
			if (this.tableHeaderView != null) {
				this.tableHeaderView.removeFromSuperview();
			}

			this.tableHeaderView = tableHeaderView;
		}

		this.rowData.tableHeaderHeightDidChangeToHeight(this.tableHeaderView != null ? this.tableHeaderView.getFrame().size.height : 0.0f);
		this.tableHeaderAttached = false;

		if (this.hasLoadedData) {
			this.reloadAllViews();
		}
	}

	public View getTableFooterView() {
		return this.tableFooterView;
	}

	public void setTableFooterView(View tableFooterView) {
		if (this.tableFooterView != tableFooterView) {
			if (this.tableFooterView != null) {
				this.tableFooterView.removeFromSuperview();
			}

			this.tableFooterView = tableFooterView;
		}

		this.rowData.tableFooterHeightDidChangeToHeight(this.tableFooterView != null ? this.tableFooterView.getFrame().size.height : 0.0f);

		Size contentSize = this.getContentSize();
		contentSize.height += this.rowData.getTableHeight();
		this.setContentSize(contentSize);

		if (this.hasLoadedData) {
			this.reloadAllViews();
		}
	}

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;

		super.setFrame(frame);

		if (!frame.size.equals(oldSize)) {
			if (oldSize.width != frame.size.width) {
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

			for (TableViewCell cell : cells) {
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
		return cell.indexPath;
	}

	public IndexPath getIndexPathForRowAtPoint(Point point) {
		return this.rowData.getIndexPathForRowAtPoint(point);
	}

	private void clearAllViews() {
		boolean areAnimationsEnabled = View.areAnimationsEnabled();
		View.setAnimationsEnabled(false);

		List<View> subviews = new ArrayList<View>(this.getSubviews());
		for (View subview : subviews) {
			if (subview instanceof TableViewSubview) {
				subview.removeFromSuperview();

				if (subview instanceof TableViewCell) {
					TableViewCell cell = (TableViewCell) subview;

					if (cell.reuseIdentifier != null) {
						if (cell.needsTransitionToState(DEFAULT_STATE)) {
							cell.willTransitionToState(DEFAULT_STATE);
							cell.didTransitionToState(DEFAULT_STATE);
							cell.setEditing(false, false);
						}

						List<TableViewCell> cells = this.cellsQueuedForReuse.get(cell.reuseIdentifier);

						if (cells == null) {
							cells = new ArrayList<TableViewCell>();
							this.cellsQueuedForReuse.put(cell.reuseIdentifier, cells);
						}

						cells.add(cell);
					}

					if (this.delegateRowDisplay != null) {
						this.delegateRowDisplay.didEndDisplayingCell(this, cell, cell.indexPath);
					}
				}
			}
		}

		this.visibleHeaderViews.clear();
		this.visibleFooterViews.clear();
		this.visibleCells.clear();
		this.headerFooterViewsQueuedForReuse.clear();
		this.viewsToHideForReuseOrRemove.clear();
		this.visibleRows = null;
		View.setAnimationsEnabled(areAnimationsEnabled);
	}

	private void reloadAllViews() {
		this.reloadAllViews(true);
	}

	private void reloadAllViews(boolean clearOldViews) {
		boolean areAnimationsEnabled = View.areAnimationsEnabled();
		View.setAnimationsEnabled(false);

		if (clearOldViews) {
			this.clearAllViews();
		}

		final EdgeInsets contentInset = this.getContentInset();
		final float contentSizeHeight = this.getContentSizeHeight();
		final Point contentOffset = this.getContentOffset();
		final float boundsHeight = this.getBoundsHeight();
		final float totalContentHeight = contentInset.top + contentSizeHeight + contentInset.bottom;
		final float maxOffsetY = ScreenMath.round(Math.max(-contentInset.top, totalContentHeight - boundsHeight - contentInset.top));

		if (maxOffsetY < contentOffset.y) {
			if (!this.isDragging() && !this.isDecelerating()) {
				this.setContentOffset(new Point(contentOffset.x, maxOffsetY));
			}
		}

		this.layoutSubviews();
		View.setAnimationsEnabled(areAnimationsEnabled);
	}

	public void reloadData() {
		if (this.reloadingData) {
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

	@Deprecated
	/** This will only exist until begin/endUpdates do this properly */
	public void updateRowSizes() {
		this.reloadingData = true;
		this.rowData.reloadData();
		this.setContentSize(new Size(this.getBounds().size.width, this.rowData.getTableHeight()));
		this.shouldUpdateVisibleViewFrames = true;
		this.reloadAllViews(false);
		this.shouldUpdateVisibleViewFrames = false;
		this.reloadingData = false;
	}

	public void didMoveToWindow() {
		super.didMoveToWindow();

		if (this.isEmpty() && this.getWindow() != null) {
			this.reloadData();
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if (this.backgroundView != null) {
			this.backgroundView.setFrame(this.getBounds());
		}

		if (this.rowData.getNumberOfSections() == 0) {
			return;
		}

		this.updateVisibleCells();

		for (TableViewSubview view : this.viewsToHideForReuseOrRemove) {
			if (view.createdByTableView) {
				view.getLayer().setHidden(true);
			} else {
				view.removeFromSuperview();
			}
		}

		this.viewsToHideForReuseOrRemove.clear();
	}

	private void updateVisibleCells() {
		Rect bounds = this.getBounds();

		Range visibleRows = this.rowData.getGlobalRowsInRect(bounds);

		if (this.shouldUpdateVisibleViewFrames || this.visibleRows == null || !this.visibleRows.equals(visibleRows)) {
			this.visibleRows = visibleRows;

			Iterator<TableViewCell> cells = this.visibleCells.iterator();

			while (cells.hasNext()) {
				TableViewCell cell = cells.next();

				if (!visibleRows.containsLocation(cell.globalRow)) {
					this.enqueueCell(cell);
					cells.remove();
				} else if (this.shouldUpdateVisibleViewFrames) {
					cell.setFrame(this.rowData.getRectForGlobalRow(cell.globalRow));
				}
			}

			int visibleCellsSize = this.visibleCells.size();
			int visibleCellsStart = visibleCellsSize > 0 ? this.visibleCells.get(0).globalRow : Integer.MAX_VALUE;
			int visibleCellsEnd = visibleCellsSize > 0 ? this.visibleCells.get(visibleCellsSize - 1).globalRow : Integer.MIN_VALUE;

			int start = (int) visibleRows.location;
			int end = (int) visibleRows.max();

			for (int globalRow = start; globalRow < end; globalRow++) {
				if (globalRow < visibleCellsStart || globalRow > visibleCellsEnd) {
					IndexPath indexPath = this.rowData.getIndexPathForRowAtGlobalRow(globalRow);
					TableViewCell cell = this.createPreparedCellForRowAtIndexPath(indexPath);
					cell.globalRow = globalRow;
					cell.setFrame(this.rowData.getRectForGlobalRow(globalRow));

					if (this.fullEditing) {
						boolean canEdit = this.dataSourceEditing != null && this.dataSourceEditing.canEditRowAtIndexPath(this, indexPath);
						TableViewCell.State[] state = canEdit ? EDIT_CONTROL_STATE : DEFAULT_STATE;

						if (cell.needsTransitionToState(state)) {
							cell.willTransitionToState(state);
							cell.didTransitionToState(state);
							cell.setEditing(canEdit, false);
						}
					} else if (cell.isEditing()) {
						cell.setEditing(false, false);
						cell.willTransitionToState(DEFAULT_STATE);
						cell.didTransitionToState(DEFAULT_STATE);
					}

					if (this.delegateRowDisplay != null) {
						this.delegateRowDisplay.willDisplayCell(this, cell, indexPath);
					}

					if (cell.getSuperview() != this) {
						cell.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
						this.insertSubview(cell, this.backgroundView == null ? 0 : 1);
					}

					cell.layoutSubviews();

					int index = globalRow - start;

					if (index < 0) {
						index = 0;
					} else if (index > visibleCellsSize) {
						index = visibleCellsSize;
					}

					this.visibleCells.add(index, cell);
					visibleCellsSize++;
				}
			}
		}

		this.updateTableHeadersAndFooters(bounds);
		this.updateVisibleHeadersAndFooters(bounds);
	}

	private void updateTableHeadersAndFooters(Rect bounds) {
		float minY = bounds.origin.y;
		float maxY = minY + bounds.size.height;

		if (this.tableHeaderView != null) {
			if (minY <= this.rowData.getHeightForTableHeaderView()) {
				if (!this.tableHeaderAttached) {
					this.tableHeaderView.setFrame(this.rowData.getRectForTableHeaderView());
					this.tableHeaderView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
					this.insertSubview(this.tableHeaderView, this.backgroundView == null ? 0 : 1);
					this.tableHeaderAttached = true;
				}
			} else if (this.tableHeaderAttached) {
				this.tableHeaderView.removeFromSuperview();
				this.tableHeaderAttached = false;
			}
		}

		if (this.tableFooterView != null) {
			float offset = this.getContentSize().height - this.rowData.getHeightForTableFooterView();
			if (maxY >= offset) {
				if (!this.tableFooterAttached) {
					this.tableFooterView.setFrame(this.rowData.getRectForTableFooterView());
					this.tableFooterView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
					this.insertSubview(this.tableFooterView, this.backgroundView == null ? 0 : 1);
					this.tableFooterAttached = true;
				} else if (this.tableFooterView.getFrame().origin.y < offset) {
					this.tableFooterView.setFrame(this.rowData.getRectForTableFooterView());
				}
			} else if (this.tableFooterAttached) {
				this.tableFooterView.removeFromSuperview();
				this.tableFooterAttached = false;
			}
		}
	}

	private void updateVisibleHeadersAndFooters(Rect visibleBounds) {
		Range visibleSections = this.rowData.getSectionsInRect(visibleBounds);

		this.ensureHeaderFooterViewsInRange(this.visibleHeaderViews, visibleSections, true);
		this.ensureHeaderFooterViewsInRange(this.visibleFooterViews, visibleSections, false);

		int start = (int) visibleSections.location;
		int end = (int) visibleSections.max();

		for (int section = start; section < end; section++) {
			boolean hasHeader = this.rowData.hasHeaderForSection(section);
			boolean hasFooter = this.rowData.hasFooterForSection(section);

			if (hasHeader || hasFooter) {
				if (hasHeader) {
					TableViewSubview headerView = this.visibleHeaderViews.get(section);

					if (headerView == null || this.tableStyle != Style.PLAIN) {
						Rect headerRect = this.rowData.getRectForHeaderInSection(section);

						if (visibleBounds.intersects(headerRect) || this.tableStyle == Style.PLAIN) {
							if (headerView == null) {
								headerView = this.createPreparedHeaderForSection(section);
								headerView.setFrame(headerRect);
								this.addSubview(headerView);
								this.visibleHeaderViews.put(section, headerView);
							}
						} else if (headerView != null) {
							this.enqueueHeaderFooterView(headerView, true);
						}
					}
				}

				if (hasFooter) {
					Rect footerRect = this.rowData.getRectForFooterInSection(section);
					TableViewSubview footerView = this.visibleFooterViews.get(section);

					if (visibleBounds.intersects(footerRect)) {
						if (footerView == null) {
							footerView = this.createPreparedFooterForSection(section);
							footerView.setFrame(footerRect);

							if (footerView.getSuperview() != this) {
								this.addSubview(footerView);
							}

							this.visibleFooterViews.put(section, footerView);
						}
					} else if (footerView != null) {
						this.enqueueHeaderFooterView(footerView, false);
					}
				}
			}
		}

		if (this.tableStyle == Style.PLAIN) {
			this.updatePinnedTableHeader(visibleBounds);
		}
	}

	private void ensureHeaderFooterViewsInRange(SparseArray<TableViewSubview> headerFooterViews, Range visibleSections, boolean headers) {
		if (headerFooterViews.size() == 0) return;

		boolean _break = false;

		while (!_break) {
			_break = true;

			int size = headerFooterViews.size();

			for (int i = 0; i < size; i++) {
				int section = headerFooterViews.keyAt(i);

				if (!visibleSections.containsLocation(section)) {
					this.enqueueHeaderFooterView(headerFooterViews.get(section), headers);
					headerFooterViews.removeAt(i);
					_break = false;
					break;
				}
			}
		}
	}

	private void updatePinnedTableHeader(Rect bounds) {
		if (this.tableStyle == Style.PLAIN) {
			int numberOfSections = this.visibleHeaderViews.size();

			for (int i = 0; i < numberOfSections; i++) {
				int section = this.visibleHeaderViews.keyAt(i);
				this.visibleHeaderViews.get(section).setFrame(this.rowData.getFloatingRectForHeaderInSection(section, bounds));
			}
		}
	}

	private void enqueueCell(TableViewCell cell) {
		cell.createdByTableView = cell.reuseIdentifier != null; // Unlike header/footers, as long as a cell has a reuse identifier, it's queued

		List<TableViewCell> queuedViews = this.cellsQueuedForReuse.get(cell.reuseIdentifier);

		if (queuedViews == null) {
			List<TableViewCell> queuedCells = new ArrayList<>();
			this.cellsQueuedForReuse.put(cell.reuseIdentifier, queuedCells);
			queuedViews = queuedCells;
		}

		this.enqueueView(queuedViews, cell);
	}

	private void enqueueHeaderFooterView(TableViewSubview headerFooterView, boolean header) {
		if (header) {
			this.visibleHeaderViews.remove(headerFooterView.section);
		} else {
			this.visibleFooterViews.remove(headerFooterView.section);
		}

		if (headerFooterView.createdByTableView && headerFooterView instanceof TableViewHeaderFooterView) {
			String reuseIdentifier = ((TableViewHeaderFooterView) headerFooterView).getReuseIdentifier();

			if (reuseIdentifier != null) {
				List<TableViewHeaderFooterView> queuedViews = this.headerFooterViewsQueuedForReuse.get(reuseIdentifier);

				if (queuedViews == null) {
					List<TableViewHeaderFooterView> queuedHeaderFooterViews = new ArrayList<>();
					this.headerFooterViewsQueuedForReuse.put(reuseIdentifier, queuedHeaderFooterViews);
					queuedViews = queuedHeaderFooterViews;
				}

				this.enqueueView(queuedViews, (TableViewHeaderFooterView) headerFooterView);
			} else {
				this.viewsToHideForReuseOrRemove.add(headerFooterView);
			}
		} else {
			this.viewsToHideForReuseOrRemove.add(headerFooterView);
		}
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
		return new ArrayList<>(this.visibleCells);
	}

	public List<IndexPath> getIndexPathsForVisibleRows() {
		List<IndexPath> indexPaths = new ArrayList<IndexPath>();

		for (TableViewCell cell : this.visibleCells) {
			indexPaths.add(this.getIndexPathForCell(cell));
		}

		return indexPaths;
	}

	private TableViewSubview createPreparedHeaderForSection(int section) {
		if (section >= this.rowData.getNumberOfSections()) {
			return null;
		}

		TableViewSubview header = null;

		if (this.delegateHeaders != null) {
			header = this.delegateHeaders.getViewForHeaderInSection(this, section);

			if (header != null) {
				header.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				header.createdByTableView = false;
			}
		}

		if (header == null) {
			if (this.tableStyle == Style.GROUPED) {
				header = this.dequeueReusableHeaderFooterViewWithIdentifier(TableViewHeaderFooterGroupedView.REUSE_IDENTIFIER);
			} else {
				header = this.dequeueReusableHeaderFooterViewWithIdentifier(TableViewHeaderFooterPlainView.REUSE_IDENTIFIER);
			}
		}

		header.setFrame(frame);

		if (header instanceof TableViewHeaderFooterView) {
			String text = this.dataSourceHeaders != null ? this.dataSourceHeaders.getTitleForHeaderInSection(this, section) : null;
			((TableViewHeaderFooterView) header).getTextLabel().setText(text);
		}

		header.isQueued = false;
		header.section = section;

		return header;
	}

	private TableViewSubview createPreparedFooterForSection(int section) {
		if (section >= this.rowData.getNumberOfSections()) {
			return null;
		}

		TableViewSubview footer = null;

		if (this.delegateFooters != null) {
			footer = this.delegateFooters.getViewForFooterInSecton(this, section);

			if (footer != null) {
				footer.createdByTableView = false;
				footer.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			}
		}

		if (footer == null) {
			if (this.tableStyle == Style.GROUPED) {
				footer = this.dequeueReusableHeaderFooterViewWithIdentifier(TableViewHeaderFooterGroupedView.REUSE_IDENTIFIER);
			} else {
				footer = this.dequeueReusableHeaderFooterViewWithIdentifier(TableViewHeaderFooterPlainView.REUSE_IDENTIFIER);
			}
		}

		if (footer instanceof TableViewHeaderFooterPlainView) {
			((TableViewHeaderFooterPlainView) footer).getTextLabel().setText(this.dataSourceFooters != null ? this.dataSourceFooters.getTitleForFooterInSection(this, section) : null);
		}

		footer.isQueued = false;
		footer.section = section;

		return footer;
	}

	private TableViewCell createPreparedCellForRowAtIndexPath(IndexPath indexPath) {
		if (this.dataSource == null) return null; // Should never get here, but may as well check.

		TableViewCell cell = this.dataSource.getCellForRowAtIndexPath(this, indexPath);
		cell.isFirstRowInSection = indexPath.row == 0;
		cell.isLastRowInSection = (indexPath.row == this.rowData.getNumberOfRowsInSection(indexPath.section) - 1);
		cell.indexPath = indexPath;
		cell.setTableStyle(this.getTableStyle());

		cell.setSelected(this.isRowAtIndexPathSelected(indexPath));

		if (cell.isLastRowInSection && this.tableStyle == Style.GROUPED) {
			cell.setSeparatorStyle(TableViewCell.SeparatorStyle.NONE);
			cell.setSeparatorColor(Color.TRANSPARENT);
		} else {
			cell.setSeparatorStyle(this.separatorStyle);
			cell.setSeparatorColor(this.separatorColor);
		}

		cell.setInheritedSeparatorInset(this.separatorInset);
		cell.setInheritedSeparatorInsetShouldInsetBackgroundViews(this.separatorInsetShouldInsetBackgroundViews);

		return cell;
	}

	@Deprecated
	public void registerClass(Class<? extends TableViewCell> cellClass, String reuseIdentifier) {
		this.registerCellClass(cellClass, reuseIdentifier);
	}

	public void registerCellClass(Class<? extends TableViewCell> cellClass, String reuseIdentifier) {
		Constructor<? extends TableViewCell> constructor = null;

		try {
			constructor = cellClass.getConstructor(TableViewCell.Style.class, Object.class);
		} catch (NoSuchMethodException e) {
			try {
				constructor = cellClass.getConstructor(TableViewCell.Style.class, String.class);
			} catch (NoSuchMethodException ignore) {
			}
		}

		if (constructor != null) {
			this.registeredCellClasses.put(reuseIdentifier, constructor);
		} else {
			throw new RuntimeException(String.format("Could not find constructor in registered cell class %s for reuseIdentifier %s", cellClass, reuseIdentifier));
		}
	}

	public void registerHeaderFooterViewClass(Class<? extends TableViewHeaderFooterView> headerFooterViewClass, String reuseIdentifier) {
		Constructor<? extends TableViewHeaderFooterView> constructor;

		try {
			constructor = headerFooterViewClass.getConstructor(String.class);
		} catch (NoSuchMethodException e) {
			constructor = null;
		}

		if (constructor != null) {
			this.registeredHeaderFooterViewClasses.put(reuseIdentifier, constructor);
		} else {
			throw new RuntimeException(String.format("Could not find constructor in registered header footer view class %s for reuseIdentifier %s", headerFooterViewClass, reuseIdentifier));
		}
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

		if (cell == null) {
			Constructor<? extends TableViewCell> constructor = this.registeredCellClasses.get(reuseIdentifier);

			if (constructor == null) {
				return null;
			} else {
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

	public TableViewHeaderFooterView dequeueReusableHeaderFooterViewWithIdentifier(String reuseIdentifier) {
		TableViewHeaderFooterView headerFooterView = this.dequeueView(this.headerFooterViewsQueuedForReuse.get(reuseIdentifier));

		if (headerFooterView != null) {
			headerFooterView.prepareForReuse();
		} else {
			Constructor<? extends TableViewHeaderFooterView> constructor = this.registeredHeaderFooterViewClasses.get(reuseIdentifier);

			if (constructor == null) {
				return null;
			} else {
				try {
					headerFooterView = constructor.newInstance(reuseIdentifier);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				headerFooterView.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
				headerFooterView.createdByTableView = true;
			}
		}

		return headerFooterView;
	}

	private <T extends TableViewSubview> void enqueueView(List<T> queuedViews, T view) {
		if (view.createdByTableView) {
			queuedViews.add(view);
		}

		view.isQueued = true;
		this.viewsToHideForReuseOrRemove.add(view);
	}

	private <T extends TableViewSubview> T dequeueView(List<T> queuedViews) {
		if (queuedViews == null || queuedViews.size() == 0) {
			return null;
		}

		T view = queuedViews.remove(queuedViews.size() - 1);
		view.isQueued = false;
		view.getLayer().setHidden(false);
		this.viewsToHideForReuseOrRemove.remove(view);

		return view;
	}

	private boolean isRowAtIndexPathSelected(IndexPath indexPath) {
		for (IndexPath selectedIndexPath : this.selectedRowsIndexPaths) {
			if (selectedIndexPath.equals(indexPath)) {
				return true;
			}
		}

		return false;
	}

	public IndexPath getIndexPathForSelectedRow() {
		if (this.selectedRowsIndexPaths.size() > 0) {
			return this.selectedRowsIndexPaths.iterator().next();
		} else {
			return null;
		}
	}

	public Set<IndexPath> getIndexPathsForSelectedRows() {
		return Sets.copy(this.selectedRowsIndexPaths);
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

		if (animated && !alreadyAnimating) {
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

		if (animated && !alreadyAnimating) {
			View.commitAnimations();
		}
	}

	public void selectRowAtIndexPath(IndexPath indexPath, boolean animated) {
		this.selectRowAtIndexPath(indexPath, animated, ScrollPosition.NONE);
	}

	public void selectRowAtIndexPath(IndexPath indexPath, boolean animated, ScrollPosition scrollPosition) {
		if (!this.rowData.isValidIndexPath(indexPath)) {
			throw new RuntimeException("Tried to select row with invalid index path: " + indexPath);
		}

		TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

		if (animated) {
			View.beginAnimations(null, null);
			View.setAnimationCurve(AnimationCurve.LINEAR);
			View.setAnimationDuration(TableViewCell.ANIMATED_HIGHLIGHT_DURATION);
		}

		if (this.selectedRowsIndexPaths.size() != 1 || !this.selectedRowsIndexPaths.contains(indexPath)) {
			Set<IndexPath> oldSelectedIndexPaths = new HashSet<IndexPath>(this.selectedRowsIndexPaths);
			oldSelectedIndexPaths.remove(indexPath);

			for (IndexPath oldSelectedIndexPath : oldSelectedIndexPaths) {
				this.deselectRowAtIndexPath(oldSelectedIndexPath, animated, animated);
			}
		}

		this.selectedRowsIndexPaths.add(indexPath);

		if (cell != null) {
			this.markCellAsSelected(cell, true, false);
		} else {
			this.selectionDidChangeForRowAtIndexPath(indexPath, true);
		}

		if (animated) {
			View.commitAnimations();
		}

		if (scrollPosition != ScrollPosition.NONE) {
			this.scrollToRowAtIndexPath(indexPath, scrollPosition, animated);
		}
	}

	private void markCellAsSelected(TableViewCell cell, boolean selected, boolean animated) {
		cell.setSelected(selected, animated);
		this.selectionDidChangeForRowAtIndexPath(cell.indexPath, selected);
	}

	void editAccessoryWasSelectedAtIndexPath(IndexPath indexPath) {
		boolean isEditing = (this.cellsBeingEditedPaths.indexOf(indexPath) > -1);
		this.resetAllEditingCells();

		if (isEditing) {
			TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);

			if (this.delegateEditing != null) {
				CharSequence title = this.delegateEditing.getTitleForDeleteConfirmationButtonForRowAtIndexPath(this, indexPath);
				if (title != null) {
					// TODO: Connect.
					// cell.deleteButton.setTitle(title);
				}
			}

			cell.setEditing(true);
			this.cellsBeingEditedPaths.add(indexPath);
		}
	}

	private void resetAllEditingCells() {
		for (IndexPath indexPath : this.cellsBeingEditedPaths) {
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

		if (this.dataSourceEditing != null) {
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

			if ((indexPath.row == 0 || this.tableStyle == Style.PLAIN) && this.rowData.hasHeaderForSection(indexPath.section)) {
				float headerHeight = this.rowData.getHeightForHeaderInSection(indexPath.section);
				rect.origin.y -= headerHeight;
				rect.size.height += headerHeight;
			}
		}

		Rect bounds = this.getBounds();
		float calculateY;

		if (scrollPosition == ScrollPosition.NONE) {
			if (bounds.contains(rect)) {
				return;
			} else if (rect.origin.y < bounds.origin.y) {
				scrollPosition = ScrollPosition.TOP;
			} else if (rect.maxY() > bounds.maxY()) {
				scrollPosition = ScrollPosition.BOTTOM;
			} else {
				scrollPosition = ScrollPosition.MIDDLE;
			}
		}

		switch (scrollPosition) {
			case TOP:
				rect = new Rect(rect.origin.x, rect.origin.y, rect.size.width, bounds.size.height);
				break;

			case BOTTOM:
				calculateY = Math.max(rect.origin.y - (bounds.size.height - rect.size.height), -this.getContentInset().top);
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

		this.ignoreTouches = this.decelerating;
		if (this.ignoreTouches) return;

		if (this.showingDeleteConfirmation) {
			this.hideDeleteConfirmation();
			this.ignoreTouches = true;
			return;
		}

		if (touches.size() == 1) {
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
		if (this.ignoreTouches) return;

		if (this.panGestureRecognizer.getState() != GestureRecognizer.State.POSSIBLE) {
			if (this.cellTouchCallback != null) {
				cancelCallbacks(cellTouchCallback);
				this.cellTouchCallback = null;
			}

			if (this.touchedCell != null) {
				this.touchedCell.setHighlighted(false);
				this.touchedCell = null;
			}

			this.touchesMoved = true;
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		super.touchesEnded(touches, event);
		if (this.ignoreTouches) return;

		if (this.cellTouchCallback != null) {
			cancelCallbacks(this.cellTouchCallback);
			this.cellTouchCallback = null;
		}

		if (this.touchedCell != null) {
			this.selectCellDueToTouchEvent(this.touchedCell, this.getIndexPathForCell(this.touchedCell), false);
			this.touchedCell = null;
		} else if (!this.touchesMoved) {
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

		if (!cell.isHighlighted()) {
			if (this.delegateHighlighting != null && !this.delegateHighlighting.shouldHighlightRowAtIndexPath(this, indexPath)) {
				return;
			}

			cell.setHighlighted(true, animated);

			if (this.delegateHighlighting != null) {
				this.delegateHighlighting.didHighlightRowAtIndexPath(this, indexPath);
			}
		}

		if (this.selectedRowsIndexPaths.size() > 0 && !this.allowsMultipleSelection) {
			for (IndexPath selectedRowIndexPath : this.selectedRowsIndexPaths) {
				if (this.delegateDeselection != null) {
					this.delegateDeselection.willDeselectRowAtIndexPath(this, selectedRowIndexPath);
				}

				this.deselectRowAtIndexPath(selectedRowIndexPath, animated, animated);

				if (this.delegateDeselection != null) {
					this.delegateDeselection.didDeselectRowAtIndexPath(this, selectedRowIndexPath);
				}
			}
		}

		if (this.delegateSelection != null) {
			IndexPath newIndexPath = this.delegateSelection.willSelectRowAtIndexPath(this, indexPath);

			if (newIndexPath == null || !newIndexPath.equals(indexPath)) {
				selectingCell.setHighlighted(false, animated);

				if (this.delegateHighlighting != null) {
					this.delegateHighlighting.didHighlightRowAtIndexPath(this, indexPath);
				}

				if (newIndexPath == null) {
					return;
				} else {
					selectingCell = this.getCellForRowAtIndexPath(newIndexPath);

					if (selectingCell != null) {
						selectingCell.setHighlighted(true, animated);
						indexPath = newIndexPath;

						if (this.delegateHighlighting != null) {
							this.delegateHighlighting.didHighlightRowAtIndexPath(this, newIndexPath);
						}
					} else {
						return;
					}
				}
			}
		}

		this.markCellAsSelected(selectingCell, true, animated);

		if (this.delegateSelection != null) {
			if (selectingCell.shouldPlayClickSoundOnSelection()) {
				selectingCell.playClickSound();
			}

			this.delegateSelection.didSelectRowAtIndexPath(this, indexPath);
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		super.touchesCancelled(touches, event);

		if (this.cellTouchCallback != null) {
			cancelCallbacks(this.cellTouchCallback);
			this.cellTouchCallback = null;
		}

		if (this.touchedCell != null) {
			this.touchedCell.setHighlighted(false);
			this.touchedCell = null;
		}
	}

	// - Cell editing

	private void handleCellSwipe(GestureRecognizer gestureRecognizer) {
		if (gestureRecognizer.getState() != GestureRecognizer.State.RECOGNIZED && gestureRecognizer.getState() != GestureRecognizer.State.BEGAN) {
			// MWarn("gesture: %s", gestureRecognizer.getState());
			return;
		}

		Point location = gestureRecognizer.locationInView(this);

		IndexPath indexPath = this.getIndexPathForRowAtPoint(location);
		MWarn("Location: %s | IP: %s", location, indexPath);
		if (indexPath == null) return;

		this.showDeleteConfirmation(indexPath);
	}

	private void showDeleteConfirmation(final IndexPath indexPath) {
		if (this.dataSourceEditing == null || !this.dataSourceEditing.canEditRowAtIndexPath(this, indexPath)) {
			return;
		}

		final TableViewCell cell = this.getCellForRowAtIndexPath(indexPath);
		if (cell == null) return;

		this.editing = true;

		final TableViewCell.State[] newState;

		if (this.fullEditing) {
			newState = new TableViewCell.State[]{TableViewCell.State.SHOWING_EDIT_CONTROL, TableViewCell.State.SHOWING_DELETE_CONFIRMATION};
		} else {
			newState = new TableViewCell.State[]{TableViewCell.State.SHOWING_DELETE_CONFIRMATION};
		}

		this.restoreScrollingEnabled = this.isScrollEnabled();
		this.setScrollEnabled(false);
		this.showingDeleteConfirmation = true;

		cell.willTransitionToState(newState);

		final TableViewCell.EditButton deleteConfirmationButton = (TableViewCell.EditButton) cell.getDeleteConfirmationButton();
		deleteConfirmationButton.layoutSubviews();

		if (this.delegateEditing != null) {
			cell.setDeleteConfirmationButtonTitle(this.delegateEditing.getTitleForDeleteConfirmationButtonForRowAtIndexPath(this, indexPath));
		}

		View.animateWithDuration(200, new Animations() {
			public void performAnimatedChanges() {
				cell.layoutSubviews();
			}
		}, new AnimationCompletion() {
			public void animationCompletion(boolean finished) {
				if (finished) {
					cell.didTransitionToState(newState);
				}
			}
		});
	}

	void deletionConfirmed(TableViewCell cell) {
		if (this.dataSourceEditing != null) {
			this.dataSourceEditing.commitEditingStyleForRowAtIndexPath(this, TableViewCell.EditingStyle.DELETE, cell.indexPath);
		}
	}

	void editControlSelected(TableViewCell cell) {
		this.showDeleteConfirmation(cell.indexPath);
	}

	private void hideDeleteConfirmation() {
		final TableViewCell.State[] newState;

		if (this.fullEditing) {
			newState = new TableViewCell.State[]{TableViewCell.State.SHOWING_EDIT_CONTROL};
		} else {
			newState = new TableViewCell.State[]{TableViewCell.State.DEFAULT};
		}

		this.editing = this.fullEditing;
		this.showingDeleteConfirmation = false;

		this.transitionCellsToState(this.visibleCells, true, this.fullEditing, newState);

		if (this.restoreScrollingEnabled) {
			this.setScrollEnabled(true);
			this.restoreScrollingEnabled = false;
		}
	}

	private void transitionCellsToState(List<TableViewCell> cells, boolean animated, boolean editing, TableViewCell.State... state) {
		final List<android.util.Pair<TableViewCell, TableViewCell.State[]>> transitions = new ArrayList<android.util.Pair<TableViewCell, TableViewCell.State[]>>();

		for (TableViewCell cell : cells) {
			TableViewCell.State[] _state;
			boolean canEdit = this.dataSourceEditing != null && this.dataSourceEditing.canEditRowAtIndexPath(this, cell.indexPath);

			if (canEdit && cell.needsTransitionToState(state)) {
				_state = state;
			} else if (!canEdit && cell.needsTransitionToState(DEFAULT_STATE)) {
				_state = DEFAULT_STATE;
			} else {
				continue;
			}

			cell.willTransitionToState(_state);
			cell.setEditing(editing, animated);
			transitions.add(new android.util.Pair<TableViewCell, TableViewCell.State[]>(cell, _state));
		}

		if (animated) {
			View.animateWithDuration(300, new Animations() {
				public void performAnimatedChanges() {
					for (android.util.Pair<TableViewCell, TableViewCell.State[]> transition : transitions) {
						transition.first.layoutSubviews();
					}
				}
			}, new AnimationCompletion() {
				public void animationCompletion(boolean finished) {
					if (finished) {
						for (android.util.Pair<TableViewCell, TableViewCell.State[]> transition : transitions) {
							transition.first.didTransitionToState(transition.second);
						}
					}
				}
			});
		} else {
			for (android.util.Pair<TableViewCell, TableViewCell.State[]> transition : transitions) {
				transition.first.didTransitionToState(transition.second);
			}
		}
	}

	public boolean shouldBegin(GestureRecognizer gestureRecognizer) {
		if (!this.showingDeleteConfirmation) {
			Point location = gestureRecognizer.locationInView(this);
			IndexPath indexPath = this.getIndexPathForRowAtPoint(location);

			return indexPath != null && !(this.dataSourceEditing == null || !this.dataSourceEditing.canEditRowAtIndexPath(this, indexPath));
		} else {
			return false;
		}
	}

	public boolean shouldReceiveTouch(GestureRecognizer gestureRecognizer, Touch touch) {
		return !this.showingDeleteConfirmation;
	}

	public boolean shouldRecognizeSimultaneously(GestureRecognizer gestureRecognizer, GestureRecognizer otherGestureRecognizer) {
		return false;
	}

	public boolean shouldRequireFailureOfGestureRecognizer(GestureRecognizer gestureRecognizer, GestureRecognizer otherGestureRecognizer) {
		return false;
	}

	public boolean shouldBeRequiredToFailByGestureRecognizer(GestureRecognizer gestureRecognizer, GestureRecognizer otherGestureRecognizer) {
		return false;
	}

}