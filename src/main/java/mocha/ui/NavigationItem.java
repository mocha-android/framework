package mocha.ui;

import mocha.foundation.MObject;

import java.util.ArrayList;
import java.util.List;

public class NavigationItem extends MObject {
	private String title;
	private BarButtonItem backBarButtonItem;
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
		this.leftBarButtonItems = new ArrayList<>();
		this.rightBarButtonItems = new ArrayList<>();
	}

	public NavigationItem(String title) {
		this();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;


		if (this.delegate != null) {
			this.delegate.titleChanged(this);
		}
	}

	public BarButtonItem getBackBarButtonItem() {
		return backBarButtonItem;
	}

	public void setBackBarButtonItem(BarButtonItem backBarButtonItem) {
		this.backBarButtonItem = backBarButtonItem;
	}

	public boolean hidesBackButton() {
		return hidesBackButton;
	}

	public void setHidesBackButton(boolean hidesBackButton) {
		this.setHidesBackButton(hidesBackButton, false);
	}

	public void setHidesBackButton(boolean hidesBackButton, boolean animated) {
		this.hidesBackButton = hidesBackButton;
	}

	public boolean leftItemsSupplementBackButton() {
		return leftItemsSupplementBackButton;
	}

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

		if (this.delegate != null) {
			this.delegate.leftBarButtonItemChanged(this, animated);
		}
	}

	public BarButtonItem getLeftBarButtonItem() {
		if (this.leftBarButtonItems.size() > 0) {
			return this.leftBarButtonItems.get(0);
		} else {
			return null;
		}
	}

	public void setLeftBarButtonItem(BarButtonItem leftBarButtonItem) {
		this.leftBarButtonItems.clear();

		if (leftBarButtonItem != null) {
			this.leftBarButtonItems.add(leftBarButtonItem);
		}

		if (this.delegate != null) {
			this.delegate.leftBarButtonItemChanged(this, false);
		}
	}

	public void setLeftBarButtonItem(BarButtonItem leftBarButtonItem, boolean animated) {
		if (animated) {
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

		if (this.delegate != null) {
			this.delegate.rightBarButtonItemChanged(this, animated);
		}
	}

	public BarButtonItem getRightBarButtonItem() {
		if (this.rightBarButtonItems.size() > 0) {
			return this.rightBarButtonItems.get(0);
		} else {
			return null;
		}
	}

	public void setRightBarButtonItem(BarButtonItem rightBarButtonItem) {
		this.rightBarButtonItems.clear();
		this.rightBarButtonItems.add(rightBarButtonItem);

		if (this.delegate != null) {
			this.delegate.rightBarButtonItemChanged(this, false);
		}
	}

	public void setRightBarButtonItem(BarButtonItem rightBarButtonItem, boolean animated) {
		if (animated) {
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

		if (this.delegate != null) {
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
