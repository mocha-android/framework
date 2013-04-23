package mocha.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The NavigationItem class encapsulates information about a navigation item pushed
 * on a NavigationBar object’s stack. A navigation bar is a control used to navigate
 * hierarchical content. A NavigationItem specifies what is displayed on the navigation
 * bar when it is the top item and also how it is represented when it is the back item.
 *
 * Use the initWithTitle: method to create a navigation item specifying the item’s title.
 * The item cannot be represented on the navigation bar without a title. Use the
 * backBarButtonItem property if you want to use a different title when this item is the
 * back item. The backBarButtonItem property is displayed as the back button unless a
 * custom left view is specified.
 *
 * The navigation bar displays a back button on the left and the title in the center by
 * default. You can change this behavior by specifying custom left, center, or right views.
 * Use the setLeftBarButtonItems:animated: and setRightBarButtonItems:animated: methods to
 * change the left and right views; you can specify that the change be animated. Use the
 * titleView method to change the center view to a custom view.
 *
 * These custom views can be system buttons. Use the UIBarButtonItem class to create custom
 * views to add to navigation items.
 */
public class NavigationItem extends mocha.foundation.Object {
	private String title;
	private BarButtonItem backBarButtonItem;
	private Button backBarButton;
	private boolean hidesBackButton;
	private boolean leftItemsSupplementBackButton;
	private List<BarButtonItem> leftBarButtonItems;
	private List<BarButtonItem> rightBarButtonItems;
	private View titleView;
	private Delegate delegate;

	interface Delegate {
		void titleChanged(NavigationItem navigationItem);
		void titleViewChanged(NavigationItem navigationItem);
		void leftBarButtonItemChanged(NavigationItem navigationItem, boolean animated);
		void rightBarButtonItemChanged(NavigationItem navigationItem, boolean animated);
	}

	public NavigationItem() {
		this.leftBarButtonItems = new ArrayList<BarButtonItem>();
		this.rightBarButtonItems = new ArrayList<BarButtonItem>();
	}

	/**
	 * Creates a navigation item initialized with the specified title
	 *
	 * @param title The string to set as the navigation item’s title displayed in the center
	 *                 of the navigation bar.
	 * @see NavigationItem#getTitle()
	 * @see NavigationItem#setTitle(String)
	 */
	public NavigationItem(String title) {
		this();
	}

	/**
	 * The navigation item’s title displayed in the center of the navigation bar
	 *
	 * When the receiver is on the navigation item stack and is second from the top—in
	 * other words, its view controller manages the views that the user would navigate back
	 * to—the value in this property is used for the back button on the top-most navigation
	 * bar. If the value of this property is NULL, the system uses the string “Back” as the
	 * text of the back button.
	 *
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the title displayed in the center of the navigation bar
	 *
	 * @param title The string to set as the navigation item’s title displayed in the center
	 *                 of the navigation bar.
	 * @see NavigationItem#getTitle()
	 */
	public void setTitle(String title) {
		this.title = title;


		if(this.delegate != null) {
			this.delegate.titleViewChanged(this);
		}
	}

	/**
	 * The bar button item to use when a back button is needed on the navigation bar
	 *
	 * When this navigation item is immediately below the top item in the stack, the navigation
	 * controller derives the back button for the navigation bar from this navigation item. When
	 * this property is NULL, the navigation item uses the value in its title property to create
	 * an appropriate back button. If you want to specify a custom image or title for the back
	 * button, you can assign a custom bar button item (with your custom title or image) to this
	 * property instead. When configuring your bar button item, do not assign a custom view to it;
	 * the navigation item ignores custom views in the back bar button anyway.
	 *
	 * Default value is NULL
	 * @return Back bar button item
	 */
	public BarButtonItem getBackBarButtonItem() {
		return backBarButtonItem;
	}

	/**
	 * Set the back bar button item
	 *
	 * @param backBarButtonItem Back bar button item
	 * @see NavigationItem#getBackBarButtonItem()
	 */
	public void setBackBarButtonItem(BarButtonItem backBarButtonItem) {
		this.backBarButtonItem = backBarButtonItem;
		this.backBarButton = null;
	}

	Button getBackBarButton() {
		return backBarButton;
	}

	void setBackBarButton(Button backBarButton) {
		this.backBarButton = backBarButton;
	}

	/**
	 * When set to true, the back button is hidden when this navigation item is the top item.
	 * This is true regardless of the value in the leftItemsSupplementBackButton property. When
	 * set to false, the back button is shown if it is still present. (It can be replaced by values
	 * in either the leftBarButtonItem or leftBarButtonItems properties.) The default value is false.
	 *
	 * @return A Boolean value that determines whether the back button is hidden.
	 */
	public boolean hidesBackButton() {
		return hidesBackButton;
	}

	/**
	 * Sets whether the back button is hidden
	 *
	 * @param hidesBackButton Specify true if the back button should be hidden when this navigation
	 *                           item is the top item. Specify false if the back button should be
	 *                           visible, assuming it has not been replaced by a custom item.
	 *
	 * @see NavigationItem#setHidesBackButton(boolean, boolean)
	 * @see NavigationItem#hidesBackButton()
	 */
	public void setHidesBackButton(boolean hidesBackButton) {
		this.setHidesBackButton(hidesBackButton, false);
	}

	/**
	 * Sets whether the back button is hidden, optionally animating the transition.
	 *
	 * @param hidesBackButton Specify true if the back button should be hidden when this navigation
	 *                           item is the top item. Specify false if the back button should be
	 *                           visible, assuming it has not been replaced by a custom item.
	 * @param animated true to animate the transition; otherwise, no.
	 *
	 * @see NavigationItem#hidesBackButton()
	 */
	public void setHidesBackButton(boolean hidesBackButton, boolean animated) {
		this.hidesBackButton = hidesBackButton;
	}

	/**
	 * Normally, the presence of custom left bar button items causes the back button to be removed in
	 * favor of the custom items. Setting this property to true causes the items in the leftBarButtonItems
	 * or leftBarButtonItem property to be displayed to the right of the back button—that is, they are
	 * displayed in addition to, and not instead of, the back button. When set to false, the items in those
	 * properties are displayed instead of the back button. The default value of this property is false.
	 *
	 * The value in the hidesBackButton property still determines whether the back button is actually displayed.
	 *
	 * @return A Boolean value indicating whether the left items are displayed in addition to the back button
	 */
	public boolean leftItemsSupplementBackButton() {
		return leftItemsSupplementBackButton;
	}

	/**
	 * @param leftItemsSupplementBackButton A Boolean value indicating whether the left items are displayed
	 *                                         in addition to the back button
	 *
	 * @see NavigationItem#leftItemsSupplementBackButton()
	 */
	public void setLeftItemsSupplementBackButton(boolean leftItemsSupplementBackButton) {
		this.leftItemsSupplementBackButton = leftItemsSupplementBackButton;
	}

	public List<BarButtonItem> getLeftBarButtonItems() {
		return leftBarButtonItems;
	}

	public void setLeftBarButtonItems(List<BarButtonItem> leftBarButtonItems) {
		this.setLeftBarButtonItems(leftBarButtonItems, false);
	}

	public void setLeftBarButtonItems(List<BarButtonItem> leftBarButtonItems, boolean animated) {
		this.leftBarButtonItems.clear();
		this.leftBarButtonItems.addAll(leftBarButtonItems);

		if(this.delegate != null) {
			this.delegate.leftBarButtonItemChanged(this, animated);
		}
	}

	public BarButtonItem getLeftBarButtonItem() {
		if(this.leftBarButtonItems.size() > 0) {
			return this.leftBarButtonItems.get(0);
		} else {
			return null;
		}
	}

	public void setLeftBarButtonItem(BarButtonItem leftBarButtonItem) {
		this.leftBarButtonItems.clear();
		this.leftBarButtonItems.add(leftBarButtonItem);

		if(this.delegate != null) {
			this.delegate.leftBarButtonItemChanged(this, false);
		}
	}

	public void setLeftBarButtonItem(BarButtonItem leftBarButtonItem, boolean animated) {
		if(animated) {
			List<BarButtonItem> items = new ArrayList<BarButtonItem>();
			items.add(leftBarButtonItem);

			this.setLeftBarButtonItems(items, true);
		} else {
			this.setLeftBarButtonItem(leftBarButtonItem);
		}
	}

	public List<BarButtonItem> getRightBarButtonItems() {
		return rightBarButtonItems;
	}

	public void setRightBarButtonItems(List<BarButtonItem> rightBarButtonItems) {
		this.setRightBarButtonItems(rightBarButtonItems, false);
	}

	public void setRightBarButtonItems(List<BarButtonItem> rightBarButtonItems, boolean animated) {
		this.rightBarButtonItems.clear();
		this.rightBarButtonItems.addAll(rightBarButtonItems);

		if(this.delegate != null) {
			this.delegate.rightBarButtonItemChanged(this, animated);
		}
	}

	public BarButtonItem getRightBarButtonItem() {
		if(this.rightBarButtonItems.size() > 0) {
			return this.rightBarButtonItems.get(0);
		} else {
			return null;
		}
	}

	public void setRightBarButtonItem(BarButtonItem rightBarButtonItem) {
		this.rightBarButtonItems.clear();
		this.rightBarButtonItems.add(rightBarButtonItem);

		if(this.delegate != null) {
			this.delegate.rightBarButtonItemChanged(this, false);
		}
	}

	public void setRightBarButtonItem(BarButtonItem rightBarButtonItem, boolean animated) {
		if(animated) {
			List<BarButtonItem> items = new ArrayList<BarButtonItem>();
			items.add(rightBarButtonItem);

			this.setRightBarButtonItems(items, true);
		} else {
			this.setRightBarButtonItem(rightBarButtonItem);
		}
	}

	public View getTitleView() {
		return titleView;
	}

	public void setTitleView(View titleView) {
		this.titleView = titleView;

		if(this.delegate != null) {
			this.delegate.titleViewChanged(this);
		}
	}

	Delegate getDelegate() {
		return delegate;
	}

	void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}
}
