package mocha.ui;

import mocha.graphics.Image;

public class BarButtonItem extends BarItem {
	public enum SystemItem {
		DONE, CANCEL, EDIT, SAVE, FLEXIBLE_SPACE, FIXED_SPACE,
		COMPOSE, REPLY, ACTION, ORGANIZE, BOOKMARKS, SEARCH,
		REFRESH, STOP, CAMERA, TRASH, PLAY, PAUSE, REWIND,
		FAST_FORWARD, UNDO, REDO
	}

	public enum Style {
		PLAIN, BORDERED, DONE
	}

	public interface Action {
		public void action(BarButtonItem barButtonItem);
	}

	private Style style;
	private float width;
	private View customView;
	private Action action;
	private boolean isSystemItem;
	private SystemItem systemItem;

	public BarButtonItem() {
		this.style = Style.PLAIN;
	}

	private BarButtonItem(Action action) {
		this();

		this.action = action;
	}

	public BarButtonItem(SystemItem systemItem, Action action) {
		this(action);

		this.isSystemItem = true;
		this.systemItem = systemItem;
	}

	public BarButtonItem(View customView) {
		this();
		this.customView = customView;
	}

	public BarButtonItem(String title, Style style, Action action) {
		this(action);
		this.setTitle(title);
	}

	public BarButtonItem(Image image, Style style, Action action) {
		this(action);
		this.setImage(image);
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public View getCustomView() {
		return this.isSystemItem ? null : customView;
	}

	public void setCustomView(View customView) {
		if(!this.isSystemItem) {
			this.customView = customView;
		}
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

}
