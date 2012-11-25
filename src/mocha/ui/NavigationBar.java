/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.List;

public class NavigationBar extends View {
	private static final EdgeInsets BUTTON_EDGE_INSETS = new EdgeInsets(0.0f, 0.0f, 0.0f, 0.5f);
	private static final float MIN_BUTTON_WIDTH = 33.0f;
	private static final float MAX_BUTTON_WIDTH = 200.0f;
	private static final float MAX_BUTTON_HEIGHT = 44.0f;
	private static final long ANIMATION_DURATION = 330;

	public interface Delegate {
		public boolean shouldPushItem(NavigationBar navigationBar, NavigationItem item);
		public void didPushItem(NavigationBar navigationBar, NavigationItem item);

		public boolean shouldPopItem(NavigationBar navigationBar, NavigationItem item);
		public void didPopItem(NavigationBar navigationBar, NavigationItem item);
	}

	private enum Transition {
		PUSH, POP, RELOAD
	}

	private BarStyle barStyle;
	private int tintColor;
	private List<NavigationItem> items;
	private Delegate delegate;

	private View leftView;
	private View centerView;
	private View rightView;

	public NavigationBar() { this(new Rect(0.0f, 0.0f, 320.0f, 44.0f)); }

	public NavigationBar(Rect frame) {
		super(frame);

		this.setTintColor(Color.rgba(0.529f, 0.616f, 0.722f, 1.0f));
	}

	static void setBarButtonSize(View view) {
		Rect frame = view.getFrame();
		frame.size = view.sizeThatFits(new Size(MAX_BUTTON_WIDTH, MAX_BUTTON_HEIGHT));
		frame.size.width = Math.max(frame.size.width, MIN_BUTTON_WIDTH);
		frame.size.height = MAX_BUTTON_HEIGHT;
		view.setFrame(frame);
	}

	public BarStyle getBarStyle() {
		return barStyle;
	}

	public void setBarStyle(BarStyle barStyle) {
		this.barStyle = barStyle;
	}

	public int getTintColor() {
		return tintColor;
	}

	public void setTintColor(int tintColor) {
		this.tintColor = tintColor;
		this.setBackgroundColor(tintColor);
	}

	public NavigationItem getTopItem() {
		return this.items.size() > 0 ? this.items.get(this.items.size() - 1) : null;
	}

	public NavigationItem getBackItem() {
		return this.items.size() > 1 ? this.items.get(this.items.size() - 2) : null;
	}

	public List<NavigationItem> getItems() {
		return items;
	}

	public void setItems(List<NavigationItem> items) {
		this.items = items;
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}
}
