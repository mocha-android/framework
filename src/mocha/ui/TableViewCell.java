/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Context;
import mocha.graphics.Font;
import mocha.graphics.Rect;

public class TableViewCell extends View {

	public enum AccessoryType {
		NONE,
		DISCLOSURE_INDICATOR,
		DETAIL_DISCLOSURE_BUTTON,
		CHECKMARK
	}

	public enum CellStyle {
		DEFAULT, VALUE_1, VALUE_2, SUBTITLE
	}

	public enum EditingStyle {
		NONE, DELETE, INSERT
	}

	public enum SeparatorStyle {
		NONE, SINGLE_LINE, SINGLE_LINED_ETCHED
	}

	public enum SelectionStyle {
		NONE, BLUE, GRAY
	}

	private View contentView;
	private Label textLabel;
	private Label detailTextLabel;
	private ImageView imageView;
	private View backgroundView;
	private View selectedBackgroundView;
	private SelectionStyle selectionStyle;
	private int indentationLevel;
	private AccessoryType accessoryType;
	private View accessoryView;
	private boolean selected;
	private boolean highlighted;
	private boolean editing; // not yet implemented
	private boolean showingDeleteConfirmation;  // not yet implemented
	final String reuseIdentifier;
	private final CellStyle style;
	private float indentationWidth; // 10 per default
	private SeparatorStyle separatorStyle;
	private int separatorColor;
	private TableViewCellLayoutManager layoutManager;
	private boolean usingDefaultSelectedBackgroundView;
	boolean tableViewStyleIsGrouped;

	public TableViewCell(CellStyle cellStyle, String reuseIdentifier) {
		this.reuseIdentifier = reuseIdentifier;
		this.style = cellStyle;
		this.indentationWidth = 10.0f;
		this.accessoryType = AccessoryType.NONE;
		this.selectionStyle = SelectionStyle.BLUE;
		this.usingDefaultSelectedBackgroundView = true;

		this.layoutManager = TableViewCellLayoutManager.getLayoutManagerForTableViewCellStyle(this.style);
		this.contentView = new ContentView(this.layoutManager.contentViewRectForCell(this));
		this.addSubview(this.contentView);

		this.setBackgroundColor(Color.WHITE);
	}

	public void prepareForReuse() {

	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if (this.accessoryView != null) {
			this.accessoryView.setFrame(this.layoutManager.accessoryViewRectForCell(this));
		}
		if (this.backgroundView != null) {
			this.backgroundView.setFrame(this.layoutManager.backgroundViewRectForCell(this));
		}
		if (this.selectedBackgroundView != null) {
			this.selectedBackgroundView.setFrame(this.layoutManager.backgroundViewRectForCell(this));
		}
		if (this.contentView != null) {
			this.contentView.setFrame(this.layoutManager.contentViewRectForCell(this));
		}
//		if (this.separatorView != null) {
//			this.separatorView.frame = this.layoutManager.seperatorViewRectForCell(this);
//		}
		if (this.imageView != null) {
			this.imageView.setFrame(this.layoutManager.imageViewRectForCell(this));
		}
		if (this.textLabel != null) {
			this.textLabel.setFrame(this.layoutManager.textLabelRectForCell(this));
		}
		if (this.detailTextLabel != null) {
			this.detailTextLabel.setFrame(this.layoutManager.detailTextLabelRectForCell(this));
		}
	}

	// Setters + Getters

	public View getContentView() {
		if (this.contentView == null) {
			this.contentView = new View();
			this.contentView.setBackgroundColor(this.getBackgroundColor());
			this.contentView.setFrame(this.layoutManager.contentViewRectForCell(this));
			this.addSubview(this.contentView);
		}
		
		return this.contentView;
	}

	Label getTextLabel(boolean autoCreate) {
		if (this.textLabel == null && autoCreate) {
			this.textLabel = new Label();
			this.textLabel.setBackgroundColor(this.getBackgroundColor());
			this.textLabel.setTextColor(Color.BLACK);
			this.textLabel.setHighlightedTextColor(Color.WHITE);
			this.textLabel.setFont(Font.getBoldSystemFontWithSize(17.0f));
			this.contentView.addSubview(this.textLabel);
		}

		return this.textLabel;
	}

	public Label getTextLabel() {
		return getTextLabel(true);
	}

	Label getDetailTextLabel(boolean autoCreate) {
		if (this.detailTextLabel == null && autoCreate && this.style == CellStyle.SUBTITLE) {
			this.detailTextLabel = new Label();
			this.detailTextLabel.setBackgroundColor(this.getBackgroundColor());
			this.detailTextLabel.setTextColor(Color.GRAY);
			this.detailTextLabel.setHighlightedTextColor(Color.WHITE);
			this.detailTextLabel.setFont(Font.getBoldSystemFontWithSize(14.0f));
			this.contentView.addSubview(this.detailTextLabel);
		}

		return this.detailTextLabel;
	}

	public Label getDetailTextLabel() {
		return getDetailTextLabel(true);
	}

	ImageView getImageView(boolean autoCreate) {
		if (this.imageView == null && autoCreate) {
			this.imageView = new ImageView();
			// this.imageView.contentMode = UIViewContentModeCenter;
			this.contentView.addSubview(this.imageView);
		}

		return imageView;
	}

	public ImageView getImageView() {
		return getImageView(true);
	}

	public View getBackgroundView() {
		if (this.backgroundView == null && tableViewStyleIsGrouped) {
			this.backgroundView = new TableViewCell.BackgroundView();
			this.insertSubview(this.backgroundView, 0);
		}

		return this.backgroundView;
	}

	public void setBackgroundView(View backgroundView) {
		if (backgroundView != this.backgroundView) {
			this.backgroundView.removeFromSuperview();

			if (backgroundView != null) {
				this.backgroundView = backgroundView;
				this.insertSubview(this.backgroundView, 0);
			}
		}
	}

	public View getSelectedBackgroundView() {
		if (usingDefaultSelectedBackgroundView) {
			return null;
		} else {
			return this.selectedBackgroundView;
		}
	}

	public void setSelectedBackgroundView(View selectedBackgroundView) {
		if (selectedBackgroundView != this.selectedBackgroundView) {
			this.selectedBackgroundView.removeFromSuperview();
			this.usingDefaultSelectedBackgroundView = false;

			if (selectedBackgroundView != null) {
				this.selectedBackgroundView = selectedBackgroundView;

				if (this.isHighlighted()) {
					if (this.backgroundView != null) {
						this.insertSubviewAboveSubview(this.selectedBackgroundView, this.backgroundView);
					} else {
						this.insertSubview(this.selectedBackgroundView, 0);
					}

					this.setNeedsLayout();
				}
			}
		}
	}

	public int getIndentationLevel() {
		return indentationLevel;
	}

	public void setIndentationLevel(int indentationLevel) {
		this.indentationLevel = indentationLevel;
	}

	public AccessoryType getAccessoryType() {
		return accessoryType;
	}

	public void setAccessoryType(AccessoryType accessoryType) {
		this.accessoryType = accessoryType;
	}

	public View getAccessoryView() {
		return accessoryView;
	}

	public void setAccessoryView(View accessoryView) {
		this.accessoryView = accessoryView;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.setSelected(selected, false);
	}

	public void setSelected(boolean selected, boolean animated) {
		this.selected = selected;
	}

	public SelectionStyle getSelectionStyle() {
		return selectionStyle;
	}

	public void setSelectionStyle(SelectionStyle selectionStyle) {
		this.selectionStyle = selectionStyle;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.setHighlighted(highlighted, false);
	}

	public void setHighlighted(boolean highlighted, boolean  animated) {
		this.highlighted = highlighted;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	public boolean isShowingDeleteConfirmation() {
		return showingDeleteConfirmation;
	}

	public float getIndentationWidth() {
		return indentationWidth;
	}

	public void setIndentationWidth(float indentationWidth) {
		this.indentationWidth = indentationWidth;
	}

	int getSeparatorColor() {
		return separatorColor;
	}

	void setSeparatorColor(int separatorColor) {
		this.separatorColor = separatorColor;
	}

	SeparatorStyle getSeparatorStyle() {
		return separatorStyle;
	}

	void setSeparatorStyle(SeparatorStyle separatorStyle) {
		this.separatorStyle = separatorStyle;
	}

	class BackgroundView extends View {

		BackgroundView() {
			super(TableViewCell.this.getLayer().getContext());
		}

	}

	class SelectedBackgroundView extends View {

		SelectedBackgroundView() {
			super(TableViewCell.this.getLayer().getContext());
		}

		public void draw(Context context, Rect rect) {
			if(selectionStyle == SelectionStyle.BLUE) {
				context.setFillColor(Color.BLUE);
			} else if(selectionStyle == SelectionStyle.GRAY) {
				context.setFillColor(Color.GRAY);
			}

			if(selectionStyle != SelectionStyle.NONE) {
				context.fillRect(rect);
			}
		}
	}

	class ContentView extends View {

		ContentView(Rect frame) {
			super(TableViewCell.this.getLayer().getContext(), frame);
		}

	}

	class Separator extends View {

		Separator(Rect frame) {
			super(TableViewCell.this.getLayer().getContext(), frame);
		}

		public void draw(Context context, Rect rect) {
			if(separatorStyle == SeparatorStyle.SINGLE_LINE) {
				context.setFillColor(separatorColor);
				context.fillRect(rect);
			}
		}
	}

	class UnhighlightedState {
		Color backgroundColor;
		boolean highlighted;
		boolean opaque;
	}
}
