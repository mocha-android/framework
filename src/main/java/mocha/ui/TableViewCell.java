/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 Mocha. All rights reserved.
*/

package mocha.ui;

import mocha.graphics.Font;
import mocha.graphics.Image;
import mocha.graphics.Rect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TableViewCell extends TableViewSubview implements Highlightable {
	static long ANIMATED_HIGHLIGHT_DURATION = NavigationBar.ANIMATION_DURATION;

	public enum AccessoryType {
			NONE, DISCLOSURE_INDICATOR, CHECKMARK
	}

	public enum SelectionStyle {
		NONE, DEFAULT
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
		NONE, SINGLE_LINE
	}

	public enum EditingStyle {
		NONE, DELETE, INSERT
	}

	public enum State {
		DEFAULT,
		SHOWING_EDIT_CONTROL,
		SHOWING_DELETE_CONFIRMATION
	}

	private static class UnhighlightedState {
		boolean highlighted;
		int backgroundColor;
	}

	class EditButton extends Button {
		boolean confirmationButton;

		EditButton() {
			this.addActionTarget(new Control.ActionTarget() {
				public void onControlEvent(Control control, Control.ControlEvent controlEvent, Event event) {
					View tableView = EditButton.this;

					while(tableView != null && !(tableView instanceof TableView)) {
						tableView = tableView.getSuperview();
					}

					if(tableView != null && (tableView instanceof TableView)) {
						if(confirmationButton) {
							((TableView)tableView).deletionConfirmed(TableViewCell.this);
						} else {
							((TableView)tableView).editControlSelected(TableViewCell.this);
						}
					}
				}
			}, Control.ControlEvent.TOUCH_UP_INSIDE);
		}
	}

	private Label textLabel;
	private Label detailTextLabel;

	private SeparatorStyle separatorStyle;
	private int separatorColor;
	private SelectionStyle selectionStyle;
	private boolean highlighted;
	private boolean selected;
	private Style cellStyle;
	private boolean usingDefaultSelectedBackgroundView;
	private Map<View,UnhighlightedState> unhighlightedStates;
	private Runnable highlightStateCallback;
	private Runnable restoreBackgroundCallback;
	private TableView.Style tableStyle;
	private boolean playClickSoundOnSelection;

	private View contentView;
	private View backgroundView;
	private View selectedBackgroundView;
	private ImageView imageView;

	private View separatorView;
	final EdgeInsets separatorInset;
	private boolean inheritsSeparatorInset;
	private boolean changedSeparatorInset;
	private boolean separatorInsetShouldInsetBackgroundViews;
	private boolean inheritsSeparatorInsetShouldInsetBackgroundViews;

	private AccessoryType accessoryType;
	private View customAccessoryView;
	private View accessoryView;

	private AccessoryType editingAccessoryType;
	private View customEditingAccessoryView;
	private View editingAccessoryView;

	private TableViewCellLayoutManager layoutManager;

	private int indentationLevel;
	private float indentationWidth;

	private EditingStyle editingStyle;
	private boolean editing;

	private boolean showingDeleteConfirmation;
	private boolean willShowDeleteConfirmation;
	private boolean showingEditControl;
	private boolean willShowEditControl;
	private boolean restoreUserInteractionEnabledOnHideDeleteConfirmation;

	private State[] state;

	private Button deleteConfirmationButton;
	private Button editControl;

	final Object reuseIdentifier;
	boolean isFirstRowInSection;
	boolean isLastRowInSection;

	public TableViewCell(Style style, Object reuseIdentifier) {
		this(new Rect(0.0f, 0.0f, 320.0f, 44.0f), style, reuseIdentifier);
	}

	TableViewCell(Rect frame, Style style, Object reuseIdentifier) {
		super(frame);

		this.cellStyle = style != null ? style : Style.DEFAULT;
		this.selectionStyle = SelectionStyle.DEFAULT;
		this.separatorStyle = SeparatorStyle.SINGLE_LINE;
		this.separatorColor = Color.white(0.88f, 1.0f);
		this.separatorInset = EdgeInsets.zero();
		this.inheritsSeparatorInset = true;
		this.inheritsSeparatorInsetShouldInsetBackgroundViews = true;
		this.accessoryType = AccessoryType.NONE;
		this.editingStyle = EditingStyle.DELETE;
		this.selected = false;
		this.highlighted = false;
		this.reuseIdentifier = reuseIdentifier != null ? reuseIdentifier : this.getClass().getName();
		this.unhighlightedStates = new HashMap<>();
		this.layoutManager = TableViewCellLayoutManager.getLayoutManagerForTableViewCellStyle(this.cellStyle);
		this.playClickSoundOnSelection = true;
		this.state = new State[] { State.DEFAULT };

		this.indentationLevel = 0;
		this.indentationWidth = 10.0f;

		this.setBackgroundColor(Color.WHITE);
		this.getContentView();
	}

	void setTableStyle(TableView.Style tableStyle) {
		this.tableStyle = tableStyle;
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if(this.backgroundView != null || this.selectedBackgroundView != null) {
			Rect frame = this.layoutManager.getBackgroundViewRectForCell(this, this.tableStyle);

			if(this.backgroundView != null) {
				this.backgroundView.setFrame(frame);
			}

			if(this.selectedBackgroundView != null) {
				this.selectedBackgroundView.setFrame(frame);
			}
		}

		this.contentView.setFrame(this.layoutManager.getContentViewRectForCell(this, this.tableStyle));

		if(this.cellStyle != Style.CUSTOM) {
			if(this.textLabel != null) {
				this.textLabel.setFrame(this.layoutManager.getTextLabelRectForCell(this, this.tableStyle));
			}

			if(this.detailTextLabel != null) {
				this.detailTextLabel.setFrame(this.layoutManager.getDetailTextLabelRectForCell(this, this.tableStyle));
			}

			this.setupAccessoryView();

			View accessoryView = null;

			if(this.isEditing()) {
				accessoryView = this.getActiveEditingAccessoryView();
			}

			if(accessoryView == null) {
				accessoryView = this.getActiveAccessoryView();
			}

			if(accessoryView != null) {
				accessoryView.setFrame(this.layoutManager.getAccessoryViewRectForCell(this, this.tableStyle));
			}

			if(this.imageView != null) {
				this.imageView.setFrame(this.layoutManager.getImageViewRectForCell(this, this.tableStyle));
			}
		}

		if(this.deleteConfirmationButton != null) {
			this.deleteConfirmationButton.setFrame(this.layoutManager.getDeleteConfirmationButtonRectForCell(this, this.tableStyle));
		}

		if(this.editControl != null) {
			this.editControl.setFrame(this.layoutManager.getEditControlRectForCell(this, this.tableStyle));
		}

		this.setupSeparatorView();

		if(this.separatorView != null) {
			this.separatorView.setFrame(this.layoutManager.getSeparatorViewRectForCell(this, this.tableStyle));
		}
	}

	private void setupAccessoryView() {
		if(this.accessoryView != null || (this.customAccessoryView == null && this.accessoryType == AccessoryType.NONE)) return;

		ImageView imageView = new ImageView();
		imageView.setContentMode(ContentMode.CENTER);
		imageView.setBackgroundColor(Color.TRANSPARENT);

		if(accessoryType == AccessoryType.DISCLOSURE_INDICATOR) {
			imageView.setImage(R.drawable.mocha_table_next);
			imageView.setAlpha(0.65f);
		} else {
			imageView.setImage(Image.imageNamed(R.drawable.mocha_table_checkmark).imageWithRenderingMode(Image.RenderingMode.ALWAYS_TEMPLATE));
			imageView.setAlpha(1.0f);
		}

		this.accessoryView = imageView;

		if(this.accessoryView.getSuperview() != this) {
			this.addSubview(this.accessoryView);
		}
	}

	private void setupSeparatorView() {
		if(this.separatorView != null) return;
		if(this.getSeparatorStyle() == SeparatorStyle.NONE) return;

		this.separatorView = new View();
		this.separatorView.setBackgroundColor(this.separatorColor);
		this.addSubview(this.separatorView);
	}

	public void setSelectionStyle(SelectionStyle selectionStyle) {
		this.selectionStyle = selectionStyle;

		if(this.usingDefaultSelectedBackgroundView) {
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

		if(this.accessoryType != accessoryType && this.customAccessoryView == null) {
			this.accessoryType = accessoryType;

			if(this.accessoryView != null) {
				this.accessoryView.removeFromSuperview();
				this.accessoryView = null;
			}

			if(this.getSuperview() != null) {
				this.layoutSubviews();

				if(this.highlighted && this.accessoryView != null && this.customAccessoryView == null) {
					this.saveViewState(this.accessoryView);
					this.setViewToTransparent(this.accessoryView);
					this.setViewToHighlightedState(this.accessoryView);
				}
			}
		}
	}

	public AccessoryType getAccessoryType() {
		return this.accessoryType;
	}

	public View getAccessoryView() {
		return this.customAccessoryView;
	}

	public void setAccessoryView(View accessoryView) {
		if(this.customAccessoryView != accessoryView) {
			if(this.customAccessoryView != null) {
				this.customAccessoryView.removeFromSuperview();
			}

			if(this.accessoryView != null) {
				this.accessoryView.removeFromSuperview();
				this.accessoryView = null;
			}

			this.customAccessoryView = accessoryView;

			if(this.customAccessoryView != null) {
				this.addSubview(this.customAccessoryView);
			}

			this.layoutSubviews();
		}
	}

	View getActiveAccessoryView() {
		return this.customAccessoryView == null ? this.accessoryView : this.customAccessoryView;
	}

	View getActiveEditingAccessoryView() {
		return this.customEditingAccessoryView == null ? this.editingAccessoryView : this.customEditingAccessoryView;
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

	int getSeparatorColor() {
		return separatorColor;
	}

	void setSeparatorColor(int separatorColor) {
		if(this.separatorColor != separatorColor) {
			this.separatorColor = separatorColor;

			if(this.separatorView != null) {
				this.separatorView.removeFromSuperview();
				this.separatorView = null;

				this.setNeedsLayout();
			}
		}
	}

	public EdgeInsets getSeparatorInset() {
		return this.separatorInset.copy();
	}

	public void setSeparatorInset(EdgeInsets separatorInset) {
		this.separatorInset.set(separatorInset);
		this.inheritsSeparatorInset = false;
		this.changedSeparatorInset = true;
		this.setNeedsLayout();
	}

	final void setInheritedSeparatorInset(EdgeInsets separatorInset) {
		if(this.inheritsSeparatorInset) {
			this.separatorInset.set(separatorInset);
			this.changedSeparatorInset = true;
			this.setNeedsLayout();
		}
	}

	public boolean getSeparatorInsetShouldInsetBackgroundViews() {
		return this.separatorInsetShouldInsetBackgroundViews;
	}

	public void setSeparatorInsetShouldInsetBackgroundViews(boolean separatorInsetShouldInsetBackgroundViews) {
		if(this.separatorInsetShouldInsetBackgroundViews != separatorInsetShouldInsetBackgroundViews) {
			this.separatorInsetShouldInsetBackgroundViews = separatorInsetShouldInsetBackgroundViews;
			this.inheritsSeparatorInsetShouldInsetBackgroundViews = false;
			this.setNeedsLayout();
		}
	}

	void setInheritedSeparatorInsetShouldInsetBackgroundViews(boolean separatorInsetShouldInsetBackgroundViews) {
		if(this.inheritsSeparatorInsetShouldInsetBackgroundViews && this.separatorInsetShouldInsetBackgroundViews != separatorInsetShouldInsetBackgroundViews) {
			this.separatorInsetShouldInsetBackgroundViews = separatorInsetShouldInsetBackgroundViews;
			this.setNeedsLayout();
		}
	}

	public void setHighlighted(boolean highlighted) {
		this.setHighlighted(highlighted, false);
	}

	public void setHighlighted(boolean highlighted, boolean animated) {
		if (this.highlighted == highlighted) {
			return;
		}

		this.updateSelectionState(highlighted, animated);
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
		this.updateSelectionState(selected, animated);
	}

	public boolean isSelected() {
		return this.selected;
	}

	private void updateSelectionState(boolean highlighted, boolean animated) {
		if(this.selectionStyle == SelectionStyle.NONE || this.highlighted == highlighted) return;
		this.highlighted = highlighted;

		if(this.highlightStateCallback != null) {
			cancelCallbacks(this.highlightStateCallback);
			this.highlightStateCallback = null;
		}

		if(this.restoreBackgroundCallback != null) {
			cancelCallbacks(this.restoreBackgroundCallback);
			this.restoreBackgroundCallback = null;
		}

		if(this.highlighted) {
			if(this.selectedBackgroundView == null) {
				this.selectedBackgroundView = new View(this.layoutManager.getBackgroundViewRectForCell(this, this.tableStyle));
				this.selectedBackgroundView.setBackgroundColor(Color.white(0.0f, 0.1f));

				if(animated) {
					this.selectedBackgroundView.setAlpha(0.0f);
				}
			}

			if(this.backgroundView != null) {
				this.insertSubviewAboveSubview(this.selectedBackgroundView, this.backgroundView);
			} else {
				this.insertSubview(this.selectedBackgroundView, 0);
			}

			this.selectedBackgroundView.setAlpha(1.0f);
			this.saveViewState(this);

			boolean areAnimationsEnabled = View.areAnimationsEnabled();
			View.setAnimationsEnabled(false);
			this.setViewToTransparent(this);
			View.setAnimationsEnabled(areAnimationsEnabled);

			if(animated) {
				this.highlightStateCallback = performAfterDelay(ANIMATED_HIGHLIGHT_DURATION / 2, new Runnable() {
					public void run() {
						setViewToHighlightedState(TableViewCell.this);
						highlightStateCallback = null;
					}
				});
			} else {
				this.setViewToHighlightedState(this);
			}
		} else {

			if(animated) {
				this.selectedBackgroundView.setAlpha(0.0f);

				this.highlightStateCallback = performAfterDelay(ANIMATED_HIGHLIGHT_DURATION / 2, new Runnable() {
					public void run() {
						restoreViewHighlightedState(TableViewCell.this);
						unhighlightedStates.clear();
						highlightStateCallback = null;
					}
				});

				this.restoreBackgroundCallback = performAfterDelay(ANIMATED_HIGHLIGHT_DURATION, new Runnable() {
					public void run() {
						restoreViewBackgroundState(TableViewCell.this);
						restoreBackgroundCallback = null;
					}
				});
			} else {
				this.restoreViewBackgroundState(this);
				this.selectedBackgroundView.removeFromSuperview();
				this.restoreViewHighlightedState(this);
				this.unhighlightedStates.clear();
			}
		}
	}

	private void saveViewState(View view) {
		if(view != this && view != this.backgroundView && view != this.selectedBackgroundView && view != this.separatorView) {
			if(!this.unhighlightedStates.containsKey(view)) {
				UnhighlightedState state = new UnhighlightedState();
				state.highlighted = (view instanceof Highlightable) && ((Highlightable)view).isHighlighted();
				state.backgroundColor = view.getBackgroundColor();
				this.unhighlightedStates.put(view, state);
			}
		}

		for(View subview : view.getSubviews()) {
			this.saveViewState(subview);
		}
	}

	private void restoreViewBackgroundState(View view) {
		UnhighlightedState state = this.unhighlightedStates.get(view);

		if(state != null) {
			view.setBackgroundColor(state.backgroundColor);
		}

		for(View subview : view.getSubviews()) {
			this.restoreViewBackgroundState(subview);
		}
	}

	private void restoreViewHighlightedState(View view) {
		UnhighlightedState state = this.unhighlightedStates.get(view);

		if(state != null) {
			if(view instanceof Highlightable) {
				((Highlightable) view).setHighlighted(state.highlighted);
			}
		}

		for(View subview : view.getSubviews()) {
			this.restoreViewHighlightedState(subview);
		}
	}

	private void setViewToHighlightedState(View view) {
		if(view != this && view != this.backgroundView && view != this.selectedBackgroundView && view != this.separatorView) {
			if(view instanceof Highlightable) {
				((Highlightable)view).setHighlighted(true);
			}
		}

		for(View subview : view.getSubviews()) {
			this.setViewToHighlightedState(subview);
		}
	}

	private void setViewToTransparent(View view) {
		if(view != this && view != this.backgroundView && view != this.selectedBackgroundView && view != this.separatorView) {
			view.setBackgroundColor(Color.colorWithAlpha(view.getBackgroundColor(), 0));
		}

		for(View subview : view.getSubviews()) {
			this.setViewToTransparent(subview);
		}
	}

	public View getContentView() {
		if (this.contentView == null) {
			this.contentView = new View();
			this.contentView.setFrame(this.layoutManager.getContentViewRectForCell(this, this.tableStyle));
			this.addSubview(this.contentView);
		}

		return this.contentView;
	}

	Label getTextLabel(boolean autoCreate) {
		if(this.cellStyle == Style.CUSTOM) return null;

		if (this.textLabel == null && autoCreate) {
			this.textLabel = new Label();
			this.textLabel.setTextColor(Color.BLACK);
			this.textLabel.setFont(Font.getSystemFontWithSize(18.0f));
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
			this.detailTextLabel.setTextColor(Color.GRAY);
			this.detailTextLabel.setFont(Font.getSystemFontWithSize(14.0f));
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
			if(this.backgroundView != null) {
				this.backgroundView.removeFromSuperview();
			}

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
			if(this.selectedBackgroundView != null) {
				this.selectedBackgroundView.removeFromSuperview();
			}

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

				this.usingDefaultSelectedBackgroundView = false;
			} else {
				this.selectedBackgroundView = null;
				this.usingDefaultSelectedBackgroundView = true;
			}
		}
	}

	public Button getEditControl() {
		return this.editControl;
	}

	public Button getDeleteConfirmationButton() {
		return this.deleteConfirmationButton;
	}

	public boolean isShowingDeleteConfirmation() {
		return this.showingDeleteConfirmation;
	}

	public int getIndentationLevel() {
		return this.indentationLevel;
	}

	public void setIndentationLevel(int indentationLevel) {
		this.indentationLevel = indentationLevel;
		this.setNeedsLayout();
	}

	public float getIndentationWidth() {
		return indentationWidth;
	}

	public void setIndentationWidth(float indentationWidth) {
		this.indentationWidth = indentationWidth;
		this.setNeedsLayout();
	}

	public void setEditingStyle(EditingStyle editingStyle) {
		this.editingStyle = editingStyle;
	}

	public EditingStyle getEditingStyle() {
		return this.editingStyle;
	}

	public void setEditing(boolean editing) {
		this.setEditing(editing, false);
	}

	public void setEditing(boolean editing, boolean animated) {
		this.editing = editing;
	}

	public boolean isEditing() {
		return this.editing;
	}

	public boolean shouldPlayClickSoundOnSelection() {
		return playClickSoundOnSelection;
	}

	public void setPlayClickSoundOnSelection(boolean playClickSoundOnSelection) {
		this.playClickSoundOnSelection = playClickSoundOnSelection;
	}

	public void prepareForReuse() {

	}

	public void willTransitionToState(State... states) {
		boolean showingDeleteConfirmation = Arrays.binarySearch(states, State.SHOWING_DELETE_CONFIRMATION) >= 0;
		boolean showingEditControl = Arrays.binarySearch(states, State.SHOWING_EDIT_CONTROL) >= 0;

		if(showingDeleteConfirmation && this.deleteConfirmationButton == null) {
			this.deleteConfirmationButton = new EditButton();
			((EditButton)this.deleteConfirmationButton).confirmationButton = true;
			this.deleteConfirmationButton.setTitle("Delete", Control.State.NORMAL);
			this.deleteConfirmationButton.setFrame(this.layoutManager.getContentViewRectForCell(this, this.tableStyle));
			this.deleteConfirmationButton.setBackgroundColor(Color.RED);
			this.deleteConfirmationButton.setTitleColor(Color.WHITE, Control.State.NORMAL);
			this.insertSubviewBelowSubview(this.deleteConfirmationButton, this.contentView);
		}

		if(showingEditControl && this.editControl == null) {
			this.editControl = new EditButton();
			((EditButton)this.editControl).confirmationButton = false;
			this.editControl.setFrame(this.layoutManager.getEditControlRectForCell(this, this.tableStyle));
			this.insertSubviewBelowSubview(this.editControl, this.contentView);
		}

		this.willShowDeleteConfirmation = showingDeleteConfirmation;
		this.willShowEditControl = showingEditControl;

		if(this.showingDeleteConfirmation) {
			this.restoreUserInteractionEnabledOnHideDeleteConfirmation = this.contentView.isUserInteractionEnabled();
			this.contentView.setUserInteractionEnabled(false);
		}
	}

	void setDeleteConfirmationButtonTitle(CharSequence title) {
		this.deleteConfirmationButton.setTitle(title, Control.State.NORMAL);
		this.deleteConfirmationButton.setFrame(this.layoutManager.getDeleteConfirmationButtonRectForCell(this, this.tableStyle));
		this.deleteConfirmationButton.layoutSubviews();
	}

	public void didTransitionToState(State... states) {
		this.showingDeleteConfirmation = Arrays.binarySearch(states, State.SHOWING_DELETE_CONFIRMATION) >= 0;
		this.showingEditControl = Arrays.binarySearch(states, State.SHOWING_EDIT_CONTROL) >= 0;

		if(!this.showingDeleteConfirmation && this.deleteConfirmationButton != null) {
			this.deleteConfirmationButton.removeFromSuperview();
			this.deleteConfirmationButton = null;
		}

		if(!this.showingDeleteConfirmation && this.restoreUserInteractionEnabledOnHideDeleteConfirmation) {
			this.contentView.setUserInteractionEnabled(true);
			this.restoreUserInteractionEnabledOnHideDeleteConfirmation = false;
		}

		if(!this.showingEditControl && this.editControl != null) {
			this.editControl.removeFromSuperview();
			this.editControl = null;
		}

		this.state = states;
	}

	boolean getWillShowDeleteConfirmation() {
		return this.willShowDeleteConfirmation;
	}

	boolean getWillShowEditControl() {
		return willShowEditControl;
	}

	boolean needsTransitionToState(State... state) {
		if(state.length != this.state.length) {
			return true;
		} else if(state.length == 1) {
			return state[0] != this.state[0];
		} else {
			return Arrays.equals(state, this.state);
		}
	}

}