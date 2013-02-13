package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Offset;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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
	private BarMatricsStorage<Float> backgroundVerticalPositionAdjustments;
	private BarMatricsStorage<Offset> titlePositionAdjustments;
	private BarMatricsStorage<Map<EnumSet<Control.State>, Image>> backgroundImages;
	private BarMatricsStorage<Float> backButtonBackgroundVerticalPositionAdjustments;
	private BarMatricsStorage<Offset> backButtonTitlePositionAdjustments;
	private BarMatricsStorage<Map<EnumSet<Control.State>, Image>> backButtonBackgroundImages;

	private static mocha.ui.Appearance.Manager<BarButtonItem, Appearance> appearanceManager;

	public static <E extends BarButtonItem> Appearance appearance(Class<E> cls) {
		if(appearanceManager == null) {
			appearanceManager = new mocha.ui.Appearance.Manager<BarButtonItem, Appearance>(BarButtonItem.class, Appearance.class);
		}

		return appearanceManager.appearance(cls);
	}

	public static Appearance appearance() {
		return appearance(BarButtonItem.class);
	}

	public BarButtonItem() {
		this.style = Style.PLAIN;
		this.backgroundVerticalPositionAdjustments = new BarMatricsStorage<Float>();
		this.titlePositionAdjustments = new BarMatricsStorage<Offset>();
		this.backgroundImages = new BarMatricsStorage<Map<EnumSet<Control.State>, Image>>();
		this.backButtonBackgroundVerticalPositionAdjustments = new BarMatricsStorage<Float>();
		this.backButtonTitlePositionAdjustments = new BarMatricsStorage<Offset>();
		this.backButtonBackgroundImages = new BarMatricsStorage<Map<EnumSet<Control.State>, Image>>();

		if(appearanceManager != null) {
			appearanceManager.apply(this);
		}
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
		this(image, null, style, action);
	}

	public BarButtonItem(Image image, Image landscapeImagePhone, Style style, Action action) {
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

	public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics, Control.State... state) {
		Map<EnumSet<Control.State>, Image> backgroundImages = this.backgroundImages.get(barMetrics);

		if(backgroundImages == null) {
			backgroundImages = new HashMap<EnumSet<Control.State>, Image>();
			this.backgroundImages.set(barMetrics, backgroundImages);
		}

		EnumSet<Control.State> stateSet = Control.getStateSet(state);

		if(backgroundImage == null) {
			backgroundImages.remove(stateSet);
		} else {
			backgroundImages.put(stateSet, backgroundImage);
		}
	}

	public Image getBackgroundImage(BarMetrics barMetrics, Control.State... state) {
		Map<EnumSet<Control.State>, Image> backgroundImages = this.backgroundImages.get(barMetrics);
		return backgroundImages == null ? null :  backgroundImages.get(Control.getStateSet(state));
	}

	public void setBackgroundVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
		this.backgroundVerticalPositionAdjustments.set(barMetrics, adjustment);
	}

	public float getBackgroundVerticalPositionAdjustmentForBarMetrics(BarMetrics barMetrics) {
		return this.backgroundVerticalPositionAdjustments.get(barMetrics);
	}

	public void setTitlePositionAdjustment(Offset adjustment, BarMetrics barMetrics) {
		this.titlePositionAdjustments.set(barMetrics, adjustment);
	}

	public Offset getTitlePositionAdjustment(BarMetrics barMetrics) {
		return this.titlePositionAdjustments.get(barMetrics);
	}

	public void setBackButtonBackgroundImage(Image backgroundImage, BarMetrics barMetrics, Control.State... state) {
		Map<EnumSet<Control.State>, Image> backgroundImages = this.backButtonBackgroundImages.get(barMetrics);

		if(backgroundImages == null) {
			backgroundImages = new HashMap<EnumSet<Control.State>, Image>();
			this.backButtonBackgroundImages.set(barMetrics, backgroundImages);
		}

		EnumSet<Control.State> stateSet = Control.getStateSet(state);

		if(backgroundImage == null) {
			backgroundImages.remove(stateSet);
		} else {
			backgroundImages.put(stateSet, backgroundImage);
		}
	}

	public Image getBackButtonBackgroundImage(BarMetrics barMetrics, Control.State... state) {
		Map<EnumSet<Control.State>, Image> backgroundImages = this.backButtonBackgroundImages.get(barMetrics);
		return backgroundImages == null ? null :  backgroundImages.get(Control.getStateSet(state));
	}


	public void setBackButtonBackgroundVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
		this.backButtonBackgroundVerticalPositionAdjustments.set(barMetrics, adjustment);
	}

	public float getBackButtonBackgroundVerticalPositionAdjustment(BarMetrics barMetrics) {
		return this.backButtonBackgroundVerticalPositionAdjustments.get(barMetrics);
	}

	public void setBackButtonTitlePositionAdjustment(Offset adjustment, BarMetrics barMetrics) {
		this.backButtonTitlePositionAdjustments.set(barMetrics, adjustment);
	}

	public Offset getBackButtonTitlePositionAdjustment(BarMetrics barMetrics) {
		return this.backButtonTitlePositionAdjustments.get(barMetrics);
	}

	public static class Appearance extends mocha.ui.Appearance <BarButtonItem> {
		private Method setBackgroundImage;
		private Method setBackgroundVerticalPositionAdjustment;
		private Method setTitlePositionAdjustment;
		private Method setTitleTextAttributes;
		private Method setBackButtonBackgroundImage;
		private Method setBackButtonBackgroundVerticalPositionAdjustment;
		private Method setBackButtonTitlePositionAdjustment;

		public Appearance() {
			try {
				this.setTitleTextAttributes = BarItem.class.getMethod("setTitleTextAttributes", TextAttributes.class, Control.State[].class);

				this.setBackgroundImage = BarButtonItem.class.getMethod("setBackgroundImage", Image.class, BarMetrics.class, Control.State[].class);
				this.setBackgroundVerticalPositionAdjustment = BarButtonItem.class.getMethod("setBackgroundVerticalPositionAdjustment", Float.TYPE, BarMetrics.class);
				this.setTitlePositionAdjustment = BarButtonItem.class.getMethod("setTitlePositionAdjustment", Offset.class, BarMetrics.class);

				this.setBackButtonBackgroundImage = BarButtonItem.class.getMethod("setBackButtonBackgroundImage", Image.class, BarMetrics.class, Control.State[].class);
				this.setBackButtonBackgroundVerticalPositionAdjustment = BarButtonItem.class.getMethod("setBackButtonBackgroundVerticalPositionAdjustment", Float.TYPE, BarMetrics.class);
				this.setBackButtonTitlePositionAdjustment = BarButtonItem.class.getMethod("setBackButtonTitlePositionAdjustment", Offset.class, BarMetrics.class);
			} catch (NoSuchMethodException ignored) { }
		}

		public void setTitleTextAttributes(TextAttributes textAttributes, Control.State... state) {
			this.store(this.setTitleTextAttributes, textAttributes, state);
		}

		public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics, Control.State... state) {
			this.store(this.setBackgroundImage, backgroundImage, barMetrics, state);
		}

		public void setBackgroundVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
			this.store(this.setBackgroundVerticalPositionAdjustment, adjustment, barMetrics);
		}

		public void setTitlePositionAdjustment(Offset adjustment, BarMetrics barMetrics) {
			this.store(this.setTitlePositionAdjustment, adjustment, barMetrics);
		}

		public void setBackButtonBackgroundImage(Image backgroundImage, BarMetrics barMetrics, Control.State... state) {
			this.store(this.setBackButtonBackgroundImage, backgroundImage, barMetrics, state);
		}

		public void setBackButtonBackgroundVerticalPositionAdjustment(float adjustment, BarMetrics barMetrics) {
			this.store(this.setBackButtonBackgroundVerticalPositionAdjustment, adjustment, barMetrics);
		}

		public void setBackButtonTitlePositionAdjustment(Offset adjustment, BarMetrics barMetrics) {
			this.store(this.setBackButtonTitlePositionAdjustment, adjustment, barMetrics);
		}
	}

}
