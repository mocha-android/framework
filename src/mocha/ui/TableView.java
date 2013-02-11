/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 enormego. All rights reserved.
*/

package mocha.ui;

import mocha.foundation.IndexPath;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

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

		public interface AccessorySelection extends Delegate {
			public void accessoryButtonTappedForRowWithIndexPath(TableView tableView, IndexPath indexPath);
		}
	}

	private static class SectionInfo {
		int numberOfRows;
		Object header;
		Object footer;
		float y;
		float headerHeight;
		float footerHeight;
		float[] rowHeights;
		float cumulativeRowHeight;
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

	static class TableViewSectionComparator implements Comparator<TableViewReuseableView> {

		public int compare(TableViewReuseableView viewA, TableViewReuseableView viewB) {
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
	private boolean editing;
	private float rowHeight;
	private boolean allowsSelection;

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
	private Delegate.AccessorySelection delegateAccessorySelection;

	private int numberOfSections;
	private boolean populatesAllCells;
	private List<SectionInfo> sectionsInfo;
	private HashMap<Object,List<TableViewCell>> cellsQueuedForReuse;
	private List<TableViewReuseableView> headersQueuedForReuse;
	private List<TableViewReuseableView> footersQueuedForReuse;
	private List<TableViewReuseableView> visibleHeaders;
	private List<TableViewReuseableView> visibleSubviews;
	private boolean usesCustomRowHeights;
	private TableViewCell touchedCell;
	private IndexPath selectedRowIndexPath;
	private Set<IndexPath> selectedRowsIndexPaths;
	private List<IndexPath> cellsBeingEditedPaths;

	private List<TableViewReuseableView> tableViewHeaders;
	private List<TableViewCell> tableViewCells;
	private List<TableViewReuseableView> viewsToRemove;

	public TableView(Style style) {
		this(style, new Rect(0.0f, 0.0f, 320.0f, 480.0f));
	}

	public TableView(Style style, Rect frame) {
		super(frame);

		this.setBackgroundColor(Color.WHITE);
		this.tableStyle = style == null ? Style.PLAIN : style;
		this.separatorStyle = TableViewCell.SeparatorStyle.SINGLE_LINE;
		this.numberOfSections = 1;
		this.setRowHeight(DEFAULT_ROW_HEIGHT);
		this.setAllowsSelection(true);
		this.populatesAllCells = false;
		this.sectionsInfo = new ArrayList<SectionInfo>();
		this.cellsQueuedForReuse = new HashMap<Object, List<TableViewCell>>();
		this.headersQueuedForReuse = new ArrayList<TableViewReuseableView>();
		this.footersQueuedForReuse = new ArrayList<TableViewReuseableView>();
		this.visibleHeaders = new ArrayList<TableViewReuseableView>();
		this.visibleSubviews = new ArrayList<TableViewReuseableView>();
		this.usesCustomRowHeights = false;
		this.touchedCell = null;
		this.selectedRowIndexPath = null;
		this.selectedRowsIndexPaths = new HashSet<IndexPath>();
		this.editing = false;
		this.cellsBeingEditedPaths = new ArrayList<IndexPath>();

		this.tableViewHeaders = new ArrayList<TableViewReuseableView>();
		this.tableViewCells = new ArrayList<TableViewCell>();
		this.viewsToRemove = new ArrayList<TableViewReuseableView>();

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

	public void setFrame(Rect frame) {
		Size oldSize = this.getFrame().size;
		super.setFrame(frame);
		Size newSize = this.getFrame().size;

		if(!newSize.equals(oldSize)) {
			this.updateIndex();

			if (this.tableStyle == Style.GROUPED) {
				this.reloadData();
			} else {
				this.setContentSize(new Size(newSize.width, this.getContentSize().height));
			}

			this.layoutSubviews();
		}
	}

	public int numberOfRowsInSection(int section) {
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

	public TableViewCell cellForRowAtIndexPath(IndexPath indexPath) {
		if (indexPath != null && this.isIndexPathValid(indexPath)) {
			List<TableViewCell> cells = this.populatesAllCells ? this.tableViewCells : this.getVisibleCells();

			for(TableViewCell cell : cells) {
				if (indexPath.equals(this.indexPathForCell(cell))) {
					return cell;
				}
			}
		}

		return null;
	}

	public IndexPath indexPathForCell(TableViewCell cell) {
		return cell._dataSourceInfo.indexPath;
	}

	public int sectionAtPoint(Point point) {
		int section = 0;

		for(int b = 0, a = this.sectionsInfo.size(); b < a; b++) {
			if (this.sectionsInfo.get(b).y > point.y) {
				section = b;
				break;
			}
		}

		section = Math.max(0, section - 1);

		return (section >= this.numberOfSections) ? -1 : section;
	}

	public IndexPath indexPathForRowAtPoint(Point point) {
		int section = this.sectionAtPoint(point);

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

		int row;
		if (this.usesCustomRowHeights) {
			int numberOfRows = sectionInfo.numberOfRows;

			float current = 0;
			for (row = 1; row < numberOfRows; row++) {
				current += sectionInfo.rowHeights[row];
				if (offsetY < current) {
					row--;
					break;
				}
			}
		} else {
			row = (int)Math.floor((offsetY - sectionInfo.headerHeight) / this.getRowHeight());
		}

		return new IndexPath(section, row);
	}

	public void reloadData() {
		this.updateSectionsInfo();

		List<View> subviews = new ArrayList<View>(this.getSubviews());
		for(View subview : subviews) {
			if(subview instanceof TableViewReuseableView) {
				subview.removeFromSuperview();
			}
		}

		this.visibleSubviews.clear();
		this.visibleHeaders.clear();
		this.headersQueuedForReuse.clear();
		this.cellsQueuedForReuse.clear();
		this.footersQueuedForReuse.clear();
		this.viewsToRemove.clear();

		this.layoutSubviews();
	}

	private void updateSectionsInfo() {
		if (this.dataSource == null) {
			return;
		}

		boolean isCustomStyle = (this.tableStyle == Style.CUSTOM);
		boolean isGroupedStyle = (this.tableStyle == Style.GROUPED);
		boolean isPlainStyle = (this.tableStyle == Style.PLAIN);

		this.sectionsInfo.clear();
		this.usesCustomRowHeights = this.delegateRowSizing != null;
		this.numberOfSections = this.dataSource.getNumberOfSections(this);

		boolean hasHeaderTitles = this.dataSourceHeaders != null;
		boolean hasFooterTitles = this.dataSourceFooters != null;

		TableViewHeader header = null;
		TableViewFooter footer = null;

		if (isGroupedStyle) {
			header = new TableViewHeader.Grouped();

			footer = new TableViewFooter();
		}

		float tableHeight = 0;

		for (int section = 0; section < this.numberOfSections; section++) {
			SectionInfo sectionInfo = new SectionInfo();
			sectionInfo.numberOfRows = this.numberOfRowsInSection(section);
			sectionInfo.y = tableHeight;

			float headerHeight = 0.0f;
			float footerHeight = 0.0f;
			String headerTitle = null;
			String footerTitle = null;

			if (!isCustomStyle) {
				if (hasHeaderTitles) {
					headerTitle = this.dataSourceHeaders.getTitleForHeaderInSections(this, section);
				}

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

				if (isGroupedStyle && hasFooterTitles) {
					footerTitle = this.dataSourceFooters.getTitleForFooterInSections(this, section);

					if (footerTitle != null) {
						footer.setText(footerTitle);
						footerHeight = footer.sizeThatFits(this.getBounds().size).height;
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

		this.setContentSize(new Size(this.getBounds().size.width, tableHeight));
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if (this.sectionsInfo.size() == 0) {
			return;
		}

		float minY = this.getContentOffset().y;
		float maxY = minY + this.getBounds().size.height;

		int section = this.sectionAtPoint(this.getContentOffset());

		if (!this.populatesAllCells && minY >= 0 && minY <= this.maxPoint.y) {
			this.scanSubviewsForQueuing(minY, maxY, section);
		}

		this.addSubviewsAtTop(minY);
		this.addSubviewsAtBottom(maxY, section);

		if(this.getSubviews().size() > 20) {
			MWarn("subviews > 20, offset: %f", this.getContentOffset().y);
		}

		for(TableViewReuseableView view : this.viewsToRemove) {
			view.removeFromSuperview();
		}

		this.viewsToRemove.clear();

		if (this.tableStyle == Style.PLAIN) {
			this.updatePlainHeaderPositions(minY, section);
		}
	}

	private void scanSubviewsForQueuing(float minY, float maxY, int section) {
		boolean isPlain = (this.tableStyle == Style.PLAIN);

		for(View view : this.getSubviews()) {
			if(!(view instanceof TableViewReuseableView)) continue;
			TableViewReuseableView reuseableView = (TableViewReuseableView)view;

			if(reuseableView._isQueued) continue;

			if((view.getFrame().maxY() <= minY || view.getFrame().origin.y >= maxY)) {
				List<? extends TableViewReuseableView> queuedViews;

				if (reuseableView instanceof TableViewCell) {
					TableViewCell cell = (TableViewCell)reuseableView;

					queuedViews = this.cellsQueuedForReuse.get(cell._reuseIdentifier);

					if(queuedViews == null) {
						List<TableViewCell> queuedCells = new ArrayList<TableViewCell>();
						this.cellsQueuedForReuse.put(cell._reuseIdentifier, queuedCells);
						queuedViews = queuedCells;
					}
				} else {
					if (reuseableView instanceof TableViewHeader) {
						if (isPlain && reuseableView._dataSourceInfo.section == section) {
							continue;
						}

						queuedViews = this.headersQueuedForReuse;
						this.visibleHeaders.remove(reuseableView);
					} else {
						queuedViews = this.footersQueuedForReuse;
					}
				}

				this.enqueueView(queuedViews, reuseableView);
			}
		}
	}

	private void addSubviewsAtTop(float minY) {
		TableViewReuseableView visibleSubview = this.visibleSubviews.size() > 0 ? this.visibleSubviews.get(0) : null;

		if (visibleSubview == null) {
			return;
		}

		float offsetY = this.populatesAllCells ? 0 : visibleSubview.getFrame().origin.y;
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
			visibleSubview = this.getPopulatedSubviewForInfo(this.infoForPreviousView(visibleSubview._dataSourceInfo));

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

			if (visibleSubview instanceof TableViewHeader) {
				this.visibleHeaders.add(0, visibleSubview);
				changedHeaders = true;
			}
		}

		if (isPlain && (this.visibleHeaders.size() == 0 || this.visibleHeaders.get(0) != this.visibleSubviews.get(0))) {
			TableViewReuseableView.Info info = this.visibleSubviews.get(0)._dataSourceInfo;
			int section = (info.type == TableViewReuseableView.Info.Type.CELL) ? info.indexPath.section : info.section;

			if (this.sectionsInfo.get(section).header != null) {
				visibleSubview = this.getPopulatedSubviewForInfo(this.infoForHeader(section));
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
		TableViewReuseableView visibleSubview = (this.visibleSubviews.size() > 0) ? this.visibleSubviews.get(this.visibleSubviews.size() - 1) : null;
		float offsetY;

		if (visibleSubview == null) {
			visibleSubview = new TableViewFooter();
			if (section == 0) {
				offsetY = 0;
				visibleSubview._dataSourceInfo = this.infoForFooter(-1);
			} else {
				offsetY = this.sectionsInfo.get(section).y;
				visibleSubview._dataSourceInfo = this.infoForFooter(section - 1);
			}
		} else {
			offsetY = visibleSubview.getFrame().maxY();
		}

		if (this.populatesAllCells) {
			maxY = this.getContentSize().height;
		}


		boolean changedHeaders = false;

		while (offsetY <= maxY) {
			visibleSubview = this.getPopulatedSubviewForInfo(this.infoForNextView(visibleSubview._dataSourceInfo));

			if (visibleSubview == null) {
				break;
			}

			Rect frame = visibleSubview.getFrame();
			frame.origin.x = 0.0f;
			frame.origin.y = offsetY;
			visibleSubview.setFrame(frame);

			offsetY += frame.size.height;

			this.visibleSubviews.add(visibleSubview);

			if (visibleSubview instanceof TableViewHeader) {
				this.visibleHeaders.add(visibleSubview);
				changedHeaders = true;
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

	private void updatePlainHeaderPositions(float minY, int section) {
		List<TableViewReuseableView> visibleHeaders = this.visibleHeaders;
		TableViewReuseableView header;
		int headerSection;
		TableViewReuseableView nextHeader;
		float y;

		float boundsY = this.getBounds().origin.y;

		for (int idx = visibleHeaders.size() - 1; idx >= 0; idx--) {
			header = visibleHeaders.get(idx);
			headerSection = header._dataSourceInfo.section;
			int path;

			if (headerSection == section) {
				if (visibleHeaders.size() > idx + 1 && (nextHeader = visibleHeaders.get(idx + 1)) != null && nextHeader.getFrame().origin.y < minY + header.getFrame().size.height) {
					y = nextHeader.getFrame().origin.y - nextHeader.getFrame().size.height;
					path = 1;
				} else {
					y = minY;
					path = 2;
				}

				float y2 = Math.max(0.0f, y);

				if(y2 != y) {
					if(path == 1) {
						path = 5;
					} else {
						path = 6;
					}

					y = y2;
				}
			} else {
				y = this.sectionsInfo.get(headerSection).y;
				path = 3;
			}

			Rect frame = header.getFrame();
			frame.origin.x = 0.0f;
			frame.origin.y = path == 2 ? boundsY : y;
			header.setFrame(frame);
		}
	}

	private TableViewReuseableView.Info infoForCell(IndexPath indexPath) {
		return new TableViewReuseableView.Info(TableViewReuseableView.Info.Type.CELL, indexPath);
	}

	private TableViewReuseableView.Info infoForHeader(int section) {
		return new TableViewReuseableView.Info(TableViewReuseableView.Info.Type.HEADER, section);
	}

	private TableViewReuseableView.Info infoForFooter(int section) {
		return new TableViewReuseableView.Info(TableViewReuseableView.Info.Type.FOOTER, section);
	}

	private TableViewReuseableView.Info infoForFirstViewInSection(int section) {
		SectionInfo sectionInfo = this.sectionsInfo.get(section);

		if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
			return this.infoForHeader(section);
		} else {
			if (sectionInfo.numberOfRows > 0) {
				return this.infoForCell(new IndexPath(section, 0));
			} else {
				if (sectionInfo.footerHeight > 0) {
					return this.infoForFooter(section);
				}
			}
		}

		return null;
	}

	private TableViewReuseableView.Info infoForLastViewInSection(int section) {
		SectionInfo sectionInfo = this.sectionsInfo.get(section);

		if (sectionInfo.footerHeight > 0) {
			return this.infoForFooter(section);
		} else {
			if (sectionInfo.numberOfRows > 0) {
				return this.infoForCell(new IndexPath(section, sectionInfo.numberOfRows - 1));
			}
		}

		if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
			return this.infoForHeader(section);
		}

		return null;
	}

	private TableViewReuseableView.Info infoForNextView(TableViewReuseableView.Info info) {
		TableViewReuseableView.Info nextViewInfo = null;

		if (info.type == TableViewReuseableView.Info.Type.CELL) {
			IndexPath indexPath = info.indexPath;
			SectionInfo sectionInfo = this.sectionsInfo.get(indexPath.section);
			if ((indexPath.row + 1) < sectionInfo.numberOfRows) {
				nextViewInfo = this.infoForCell(new IndexPath(indexPath.section, indexPath.row + 1));
			} else {
				if (sectionInfo.footerHeight > 0.0f) {
					nextViewInfo = this.infoForFooter(indexPath.section);
				} else {
					if ((indexPath.section + 1) < this.numberOfSections) {
						nextViewInfo = this.infoForFirstViewInSection(indexPath.section + 1);
					}
				}
			}
		} else {
			if (info.type == TableViewReuseableView.Info.Type.HEADER) {
				int section = info.section;
				SectionInfo sectionInfo = this.sectionsInfo.get(section);

				if (sectionInfo.numberOfRows > 0) {
					nextViewInfo = this.infoForCell(new IndexPath(section, 0));
				} else {
					if (sectionInfo.footerHeight > 0) {
						nextViewInfo = this.infoForFooter(section);
					} else {
						if (++section < this.numberOfSections) {
							nextViewInfo = this.infoForFirstViewInSection(section);
						}
					}
				}
			} else {
				int section = info.section;

				if (++section < this.numberOfSections) {
					nextViewInfo = this.infoForFirstViewInSection(section);
				}
			}
		}

		return nextViewInfo;
	}

	private TableViewReuseableView.Info infoForPreviousView(TableViewReuseableView.Info info) {
		TableViewReuseableView.Info previousInfo = null;

		if (info.type == TableViewReuseableView.Info.Type.CELL) {
			IndexPath indexPath = info.indexPath;

			if (indexPath.row >= 1) {
				previousInfo = this.infoForCell(new IndexPath(indexPath.section, indexPath.row - 1));
			} else {
				SectionInfo sectionInfo = this.sectionsInfo.get(indexPath.section);

				if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
					previousInfo = this.infoForHeader(indexPath.section);
				} else {
					if (indexPath.section >= 1) {
						previousInfo = this.infoForLastViewInSection(indexPath.section - 1);
					}
				}
			}
		} else {
			if (info.type == TableViewReuseableView.Info.Type.HEADER) {
				int section = info.section;
				if (--section >= 0) {
					previousInfo = this.infoForLastViewInSection(section);
				}
			} else {
				int section = info.section;
				SectionInfo sectionInfo = this.sectionsInfo.get(section);
				if (sectionInfo.numberOfRows > 0) {
					previousInfo = this.infoForCell(new IndexPath(section, sectionInfo.numberOfRows - 1));
				} else {
					if (sectionInfo.header != null || sectionInfo.headerHeight > 0.0f) {
						previousInfo = this.infoForHeader(section);
					} else {
						if (--section >= 0) {
							previousInfo = this.infoForLastViewInSection(section);
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
			for(TableViewReuseableView header : this.tableViewHeaders) {
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
			indexPaths.add(this.indexPathForCell(cell));
		}

		return indexPaths;
	}

	private TableViewReuseableView getPopulatedSubviewForInfo(TableViewReuseableView.Info info) {
		if (info == null) {
			return null;
		}

		TableViewReuseableView view;

		if (info.type == TableViewReuseableView.Info.Type.CELL) {
			view = this.getPopulatedCellForRowAtIndexPath(info.indexPath);
		} else {
			if (info.type == TableViewReuseableView.Info.Type.HEADER) {
				view = this.getPopulatedHeaderForSection(info.section);
			} else {
				view = this.getPopulatedFooterForSection(info.section);
			}
		}

		if (view == null) {
			return null;
		} else {
			view._dataSourceInfo = info;
			return view;
		}
	}

	private TableViewReuseableView getPopulatedHeaderForSection(int section) {
		TableViewReuseableView header;

		if (section >= this.numberOfSections) {
			return null;
		}

		if (this.visibleHeaders.size() > 0 && this.visibleHeaders.get(0)._dataSourceInfo.section == section) {
			return this.visibleHeaders.get(0);
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		header = this.dequeueView(this.headersQueuedForReuse);

		if (header == null) {
			header = this.tableStyle == Style.GROUPED ? new TableViewHeader.Grouped() : new TableViewHeader.Plain();
			header.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			this.addSubview(header);
		}

		Rect frame = header.getFrame();
		frame.size.height = sectionInfo.headerHeight;
		frame.size.width = this.getBounds().size.width;
		header.setFrame(frame);

		if(header instanceof TableViewHeader) {
			((TableViewHeader) header).setText((String)sectionInfo.header);
		}

		return header;
	}

	private TableViewReuseableView getPopulatedFooterForSection(int section) {
		TableViewReuseableView footer;

		if (section >= this.numberOfSections) {
			return null;
		}

		SectionInfo sectionInfo = this.sectionsInfo.get(section);
		footer = this.dequeueView(this.footersQueuedForReuse);
		if (footer == null) {
			footer = new TableViewFooter();
			this.addSubview(footer);
		}

		Rect frame = footer.getFrame();
		frame.size.height = sectionInfo.footerHeight;
		frame.size.width = this.getBounds().size.width;
		footer.setFrame(frame);

		if(footer instanceof TableViewFooter) {
			((TableViewFooter) footer).setText((String)sectionInfo.footer);
		}

		return footer;
	}

	private TableViewCell getPopulatedCellForRowAtIndexPath(IndexPath indexPath) {
		if(this.dataSource == null) return null; // Should never get here, but may as well check.

		TableViewCell cell = this.dataSource.getCellForRowAtIndexPath(this, indexPath);
		cell._firstRowInSection = indexPath.row == 0;
		cell._lastRowInSection = (indexPath.row == this.sectionsInfo.get(indexPath.section).numberOfRows - 1);

		if (cell._dataSourceInfo != null) {
			cell._dataSourceInfo.indexPath = indexPath;
		} else {
			cell._dataSourceInfo = this.infoForCell(indexPath);
		}

		cell.setSelected(this.isRowAtIndexPathSelected(indexPath));
		cell.setSeparatorStyle(this.separatorStyle);

		Rect frame = cell.getFrame();
		frame.size.width = this.getBounds().size.width;
		frame.size.height = this.usesCustomRowHeights ? this.sectionsInfo.get(indexPath.section).rowHeights[indexPath.row] : this.getRowHeight();
		cell.setFrame(frame);

		if (cell.getSuperview() != this) {
			cell.setAutoresizing(Autoresizing.FLEXIBLE_WIDTH);
			this.insertSubview(cell, 0);
		}

		cell.layoutIfNeeded();

		return cell;
	}

	public TableViewCell dequeueReusableCellWithIdentifier(Object reuseIdentifier) {
		TableViewCell cell = this.dequeueView(this.cellsQueuedForReuse.get(reuseIdentifier));

		if (cell != null) {
			cell.prepareForReuse();
		}

		return cell;
	}

	@SuppressWarnings("unchecked")
	private void enqueueView(List queuedViews, TableViewReuseableView view) {
		this.visibleSubviews.remove(view);
		this.visibleHeaders.remove(view);

		queuedViews.add(view);
		view._isQueued = true;
		this.viewsToRemove.add(view);
	}

	private <T extends TableViewReuseableView> T dequeueView(List<T> queuedViews) {
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

	private void deselectRowAtIndexPathAnimated(IndexPath indexPath, boolean animated) {
		if (indexPath == null) {
			return;
		}

		if(this.delegateDeselection != null) {
			this.delegateDeselection.willDeselectRowAtIndexPath(this, indexPath);
		}

		TableViewCell cell = this.cellForRowAtIndexPath(indexPath);
		this.selectedRowsIndexPaths.remove(indexPath);

		if (cell != null) {
			this.markCellAsSelected(cell, false, animated);
		} else {
			this.selectionDidChangeForRowAtIndexPath(indexPath, false);
		}

		if(this.delegateDeselection != null) {
			this.delegateDeselection.didDeselectRowAtIndexPath(this, indexPath);
		}
	}

	private void selectRowAtIndexPath(IndexPath indexPath) {
		if (!this.isIndexPathValid(indexPath)) {
			throw new RuntimeException("Tried to select row with invalid index path: " + indexPath);
		}

		TableViewCell cell = this.cellForRowAtIndexPath(indexPath);

		if(this.delegateSelection != null) {
			this.delegateSelection.willSelectRowAtIndexPath(this, indexPath);
		}

		if(this.selectedRowsIndexPaths.size() != 1 || !this.selectedRowsIndexPaths.contains(indexPath)) {
			Set<IndexPath> oldSelectedIndexPaths = new HashSet<IndexPath>(this.selectedRowsIndexPaths);
			oldSelectedIndexPaths.remove(indexPath);

			for(IndexPath oldSelectedIndexPath : oldSelectedIndexPaths) {
				this.deselectRowAtIndexPathAnimated(oldSelectedIndexPath, false);
			}
		}

		this.selectedRowsIndexPaths.add(indexPath);

		if (cell != null) {
			cell.setHighlighted(false);
			this.markCellAsSelected(cell, true, false);
		} else {
			this.selectionDidChangeForRowAtIndexPath(indexPath, true);
		}

		if(this.delegateSelection != null) {
			this.delegateSelection.didSelectRowAtIndexPath(this, indexPath);
		}
	}

	private void markCellAsSelected(TableViewCell cell, boolean selected, boolean animated) {
		cell.setSelected(selected, animated);
		this.selectionDidChangeForRowAtIndexPath(cell._dataSourceInfo.indexPath, selected);
	}

	void disclosureButtonWasSelectedAtIndexPath(IndexPath indexPath) {
		TableViewCell cell = this.cellForRowAtIndexPath(indexPath);

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
			TableViewCell cell = this.cellForRowAtIndexPath(indexPath);
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
			this.cellForRowAtIndexPath(indexPath).setEditing(false);
		}

		this.cellsBeingEditedPaths.clear();
	}

	private void deleteButtonWasSelectedAtIndexPath(IndexPath indexPath) {
		this.resetAllEditingCells();
		TableViewCell cell = this.cellForRowAtIndexPath(indexPath);

		if (cell.isSelected()) {
			this.deselectRowAtIndexPathAnimated(indexPath, false);
		}

		if(this.dataSourceEditing != null) {
			this.dataSourceEditing.commitEditingStyleForRowAtIndexPath(this, TableViewCell.EditingStyle.DELETE, indexPath);
		}
	}

	private void updateIndex() {

	}

}