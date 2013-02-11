/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 enormego. All rights reserved.
*/

package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Rect;

public class TableViewCell extends TableViewReuseableView implements Highlightable {

	public enum AccessoryType {
		NONE, DISCLOSURE_INDICATOR, DETAIL_DISCLOSURE_BUTTON, CHECKMARK
	}

	public enum SelectionStyle {
		NONE, BLUE, GRAY
	}

	/**
	 * Cell styles to be used when creating a table view cell.
	 *
	 * @see TableViewCell#getTextLabel() primary text label
	 * @see TableViewCell#getDetailTextLabel() detail text label
	 * @see TableViewCell#getImageView() image view
	 */
	public enum Style {
		/**
		 * A single text label with black text.
		 * No detail text label is supported and will always be NULL
		 * An optional image view is supported, and will be aligned to the left of the text label
		 */
		DEFAULT,

		/**
		 * Two text labels, the primary text label is on the left side with left aligned black text
		 * The detail text label is on the right side with right aligned smaller blue text
		 * An optional image view is supported, and will be aligned to the left of the primary text label
		 */
		VALUE_1,

		/**
		 * Two text labels, the primary text label is on the left side with right aligned small blue text
		 * The detail text label is on the right side of the primary text label with left aligned black text
		 * An optional image view is supported, and will be aligned to the left of the primary text label
		 */
		VALUE_2,

		/**
		 * Two text labels, the primary text label is on the top with left aligned black text
		 * The detail text label is on the bottom with left aligned smaller gray text
		 * An optional image view is supported, and will be aligned to the left of the both text labels
		 */
		SUBTITLE,

		/**
		 * Essentially a blank slate.
		 * No support for text labels, both will return NULL.
		 * No support for image view, will return NULL.
		 * No support for accessories, setting will have no affect.
		 * Background View, Selected Background View and Content View are all still supported.
		 */
		CUSTOM
	}

	public enum SeparatorStyle {
		NONE, SINGLE_LINE, SINGLE_LINE_ETCHED
	}

	public enum EditingStyle {
		NONE, DELETE, INSERT
	}

	private Label textLabel;
	private Label detailTextLabel;

	private SeparatorStyle separatorStyle;
	private SelectionStyle selectionStyle;
	private AccessoryType accessoryType;
	private boolean highlighted;
	private boolean selected;
	private Style cellStyle;
	private EditingStyle editingStyle;
	private boolean editing;
	private boolean usingDefaultSelectedBackgroundView;
	private int indentationLevel;

	private View contentView;
	private View backgroundView;
	private View selectedBackgroundView;
	private View accessoryView;
	private View actualAccessoryView;
	private ImageView imageView;
	private View separatorView;

	private TableViewCellLayoutManager layoutManager;

	Object _reuseIdentifier;
	boolean _firstRowInSection;
	boolean _lastRowInSection;

	public TableViewCell(Style style, Object reuseIdentifier) {
		this(new Rect(0.0f, 0.0f, 320.0f, 44.0f), style, reuseIdentifier);
	}

	TableViewCell(Rect frame, Style style, Object reuseIdentifier) {
		super(frame);

		this.cellStyle = style != null ? style : Style.DEFAULT;
		this.selectionStyle = SelectionStyle.BLUE;
		this.separatorStyle = SeparatorStyle.SINGLE_LINE;
		this.accessoryType = AccessoryType.NONE;
		this.editingStyle = EditingStyle.NONE;
		this.selected = false;
		this.highlighted = false;
		this._reuseIdentifier = reuseIdentifier != null ? reuseIdentifier : this.getClass().getName();
		this.layoutManager = TableViewCellLayoutManager.getLayoutManagerForTableViewCellStyle(this.cellStyle);

		this.setBackgroundColor(Color.WHITE);
		this.getContentView();
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if(this.backgroundView != null || this.selectedBackgroundView != null) {
			Rect frame = this.layoutManager.getBackgroundViewRectForCell(this);

			if(this.backgroundView != null) {
				this.backgroundView.setFrame(frame);
			}

			if(this.selectedBackgroundView != null) {
				this.selectedBackgroundView.setFrame(frame);
			}
		}

		this.contentView.setFrame(this.layoutManager.getContentViewRectForCell(this));

		if(this.cellStyle != Style.CUSTOM) {
			if(this.textLabel != null) {
				this.textLabel.setFrame(this.layoutManager.getTextLabelRectForCell(this));
			}

			if(this.detailTextLabel != null) {
				this.detailTextLabel.setFrame(this.layoutManager.getDetailTextLabelRectForCell(this));
			}

			this.setupAccessoryView();

			if(this.actualAccessoryView != null) {
				this.actualAccessoryView.setFrame(this.layoutManager.getAccessoryViewRectForCell(this));
			}

			if(this.imageView != null) {
				this.imageView.setFrame(this.layoutManager.getImageViewRectForCell(this));
			}
		}

		this.setupSeparatorView();

		if(this.separatorView != null) {
			this.separatorView.setFrame(this.layoutManager.getSeparatorViewRectForCell(this));
		}
	}

	private void setupAccessoryView() {
		if(this.actualAccessoryView != null) return;
		if(this.accessoryView == null && this.accessoryType == AccessoryType.NONE) return;

		if(this.accessoryView != null) {
			this.actualAccessoryView = this.accessoryView;
		} else if(this.accessoryType == AccessoryType.DETAIL_DISCLOSURE_BUTTON) {
			Button button = new Button();
			button.setImage(R.drawable.mocha_table_view_cell_accessory_detailed_disclosure_indicator, Control.State.NORMAL);
			button.setImage(R.drawable.mocha_table_view_cell_accessory_detailed_disclosure_indicator_pressed, Control.State.NORMAL, Control.State.HIGHLIGHTED);
			button.setBackgroundColor(this.getBackgroundColor());
			this.actualAccessoryView = button;
		} else {
			ImageView imageView = new ImageView();
			imageView.setContentMode(ContentMode.CENTER);
			imageView.setBackgroundColor(this.getBackgroundColor());

			if(accessoryType == AccessoryType.DISCLOSURE_INDICATOR) {
				imageView.setImage(R.drawable.mocha_table_view_cell_accessory_disclosure_indicator);
				imageView.setHighlightedImage(R.drawable.mocha_table_view_cell_accessory_disclosure_indicator_selected);
			} else {
				imageView.setImage(R.drawable.mocha_table_view_cell_accessory_checkmark);
				imageView.setHighlightedImage(R.drawable.mocha_table_view_cell_accessory_checkmark_selected);
			}

			this.actualAccessoryView = imageView;
		}

		this.addSubview(this.actualAccessoryView);
	}

	private void setupSeparatorView() {
		if(this.separatorView != null) return;
		if(this.getSeparatorStyle() == SeparatorStyle.NONE) return;

		this.separatorView = new View();
		this.separatorView.setBackgroundColor(Color.white(0.88f, 1.0f));
		this.addSubview(this.separatorView);
	}

	public void setSelectionStyle(SelectionStyle selectionStyle) {
		this.selectionStyle = selectionStyle;

		if(usingDefaultSelectedBackgroundView) {
			this.selectedBackgroundView.removeFromSuperview();
			this.selectedBackgroundView = null;

			if(this.selected) {
				this.setSelected(false);
				this.setSelected(true);
			}

			if(this.highlighted) {
				this.setHighlighted(false);
				this.setHighlighted(true);
			}
		}
	}

	public SelectionStyle getSelectionStyle() {
		return this.selectionStyle;
	}

	public void setAccessoryType(AccessoryType accessoryType) {
		if (this.cellStyle == Style.CUSTOM) {
			return;
		}

		if(this.accessoryType != accessoryType && this.accessoryView == null) {
			this.accessoryType = accessoryType;
			this.actualAccessoryView = null;
		}
	}

	public AccessoryType getAccessoryType() {
		return this.accessoryType;
	}

	public View getAccessoryView() {
		return accessoryView;
	}

	public void setAccessoryView(View accessoryView) {
		this.accessoryView = accessoryView;
		this.actualAccessoryView = accessoryView;
	}

	View getActualAccessoryView() {
		return this.actualAccessoryView;
	}

	void setSeparatorStyle(SeparatorStyle separatorStyle) {
		if(this.separatorStyle != separatorStyle) {
			this.separatorStyle = separatorStyle;

			if(this.separatorView != null) {
				this.separatorView.removeFromSuperview();
				this.separatorView = null;
			}

			this.setNeedsLayout();
		}
	}

	SeparatorStyle getSeparatorStyle() {
		return this.separatorStyle;
	}

	public void setHighlighted(boolean highlighted) {
		this.setHighlighted(highlighted, false);
	}

	public void setHighlighted(boolean highlighted, boolean animated) {
		if (this.highlighted == highlighted) {
			return;
		}

		this.highlighted = highlighted;
	}

	public boolean isHighlighted() {
		return this.highlighted;
	}

	public void setSelected(boolean selected) {
		this.setSelected(selected, false);
	}

	public void setSelected(boolean selected, boolean animated) {
		if (this.selected == selected) {
			return;
		}

		this.selected = selected;
		// TODO: Make views selected.
	}

	public boolean isSelected() {
		return this.selected;
	}

	public View getContentView() {
		if (this.contentView == null) {
			this.contentView = new View();
			this.contentView.setBackgroundColor(this.getBackgroundColor());
			this.contentView.setFrame(this.layoutManager.getContentViewRectForCell(this));
			this.addSubview(this.contentView);
		}

		return this.contentView;
	}

	Label getTextLabel(boolean autoCreate) {
		if(this.cellStyle == Style.CUSTOM) return null;

		if (this.textLabel == null && autoCreate) {
			this.textLabel = new Label();
			this.textLabel.setBackgroundColor(this.getBackgroundColor());
			this.textLabel.setTextColor(Color.BLACK);
			this.textLabel.setHighlightedTextColor(Color.WHITE);
			this.textLabel.setFont(Font.getBoldSystemFontWithSize(20.0f));
			this.contentView.addSubview(this.textLabel);
		}

		return this.textLabel;
	}

	public Label getTextLabel() {
		return getTextLabel(true);
	}

	Label getDetailTextLabel(boolean autoCreate) {
		if(this.cellStyle == Style.CUSTOM || this.cellStyle == Style.DEFAULT) return null;

		if (this.detailTextLabel == null && autoCreate) {
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
		if(this.cellStyle == Style.CUSTOM) return null;

		if (this.imageView == null && autoCreate) {
			this.imageView = new ImageView();
			this.imageView.setContentMode(ContentMode.CENTER);
			this.contentView.addSubview(this.imageView);
		}

		return imageView;
	}

	public ImageView getImageView() {
		return getImageView(true);
	}

	public View getBackgroundView() {
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
			} else {
				this.selectedBackgroundView = null;
			}
		}
	}

	public int getIndentationLevel() {
		return this.indentationLevel;
	}

	public void setIndentationLevel(int indentationLevel) {
		this.indentationLevel = indentationLevel;
	}

	public void setEditingStyle(EditingStyle editingStyle) {
		this.editingStyle = editingStyle;
	}

	public EditingStyle getEditingStyle() {
		return this.editingStyle;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	public boolean isEditing() {
		return this.editing;
	}

	public void prepareForReuse() {

	}

}