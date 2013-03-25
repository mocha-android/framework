package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Offset;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class BarButtonItem extends BarItem implements Accessibility {
	public enum SystemItem {
		DONE, CANCEL, EDIT, SAVE, ADD, FLEXIBLE_SPACE, FIXED_SPACE,
		COMPOSE, REPLY, ACTION, ORGANIZE, BOOKMARKS, SEARCH, REFRESH,
		STOP, CAMERA, TRASH, PLAY, PAUSE, REWIND, FAST_FORWARD, UNDO,
		REDO, PAGE_CURL
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
	private View view;
	private Action action;
	private boolean isSystemItem;
	private SystemItem systemItem;
	private BarMetricsStorage<Float> backgroundVerticalPositionAdjustments;
	private BarMetricsStorage<Offset> titlePositionAdjustments;
	private BarMetricsStorage<Map<EnumSet<Control.State>, Image>> backgroundImages;
	private BarMetricsStorage<Float> backButtonBackgroundVerticalPositionAdjustments;
	private BarMetricsStorage<Offset> backButtonTitlePositionAdjustments;
	private BarMetricsStorage<Map<EnumSet<Control.State>, Image>> backButtonBackgroundImages;

	private static mocha.ui.Appearance.Storage<BarButtonItem, Appearance> appearanceStorage;

	public static <E extends BarButtonItem> Appearance appearance(Class<E> cls) {
		if(appearanceStorage == null) {
			appearanceStorage = new mocha.ui.Appearance.Storage<BarButtonItem, Appearance>(BarButtonItem.class, Appearance.class);
		}

		return appearanceStorage.appearance(cls);
	}

	public static Appearance appearance() {
		return appearance(BarButtonItem.class);
	}

	private BarButtonItem() {
		this.style = Style.PLAIN;
		this.backgroundVerticalPositionAdjustments = new BarMetricsStorage<Float>();
		this.titlePositionAdjustments = new BarMetricsStorage<Offset>();
		this.backgroundImages = new BarMetricsStorage<Map<EnumSet<Control.State>, Image>>();
		this.backButtonBackgroundVerticalPositionAdjustments = new BarMetricsStorage<Float>();
		this.backButtonTitlePositionAdjustments = new BarMetricsStorage<Offset>();
		this.backButtonBackgroundImages = new BarMetricsStorage<Map<EnumSet<Control.State>, Image>>();

		if(appearanceStorage != null) {
			appearanceStorage.apply(this);
		}

		MLog("Background images: %s", this.backgroundImages.get(BarMetrics.DEFAULT));
	}

	private BarButtonItem(Action action, Style style) {
		this();

		this.action = action;
		this.style = style == null ? Style.PLAIN : style;
	}

	public BarButtonItem(SystemItem systemItem, Action action) {
		this(systemItem, Style.BORDERED, action);
	}

	public BarButtonItem(SystemItem systemItem, Style style, Action action) {
		this(action, style);

		this.isSystemItem = true;
		this.systemItem = systemItem;
	}

	public BarButtonItem(View customView) {
		this();
		this.customView = customView;
	}

	public BarButtonItem(String title, Style style, Action action) {
		this(action, style);
		this.setTitle(title);
	}

	public BarButtonItem(Image image, Style style, Action action) {
		this(image, null, style, action);
	}

	public BarButtonItem(Image image, Image landscapeImagePhone, Style style, Action action) {
		this(action, style);
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

	float _getWidth() {
		return width > 0.0f ? width : this.getView() != null ? this.getView().getFrame().size.width : 0.0f;
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

	void setView(View view) {
		this.view = view;
	}

	View getView() {
		if(this.isSystemItem || this.view != null) {
			return this.view;
		} else {
			return this.customView;
		}
	}

	public SystemItem getSystemItem() {
		return systemItem;
	}

	public boolean isSystemItem() {
		return isSystemItem;
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
		Float adjustment = this.backgroundVerticalPositionAdjustments.get(barMetrics);
		return adjustment == null ? 0.0f : adjustment;
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
		Float adjustment = this.backButtonBackgroundVerticalPositionAdjustments.get(barMetrics);
		return adjustment != null ? adjustment : 0.0f;
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

	// TODO


	public boolean isAccessibilityElement() {
		return false;
	}

	public void setIsAccessibilityElement(boolean isAccessibilityElement) {
	}

	public String getAccessibilityLabel() {
		return null;
	}

	public void setAccessibilityLabel(String accessibilityLabel) {
	}

	public String getAccessibilityHint() {
		return null;
	}

	public void setAccessibilityHint(String accessibilityHint) {
	}

	public String getAccessibilityValue() {
		return null;
	}

	public void setAccessibilityValue(String accessibilityValue) {
	}

	public Trait[] getAccessibilityTraits() {
		return new Trait[0];
	}

	public void setAccessibilityTraits(Trait... accessibilityTraits) {
	}

	public Rect getAccessibilityFrame() {
		return null;
	}

	public void setAccessibilityFrame(Rect accessibilityFrame) {
	}

	public Point getAccessibilityActivationPoint() {
		return null;
	}

	public void setAccessibilityActivationPoint(Point accessibilityActivationPoint) {
	}

	public boolean getAccessibilityElementsHidden() {
		return false;
	}

	public void setAccessibilityElementsHidden(boolean accessibilityElementsHidden) {
	}

	public boolean getAccessibilityViewIsModal() {
		return false;
	}

	public void setAccessibilityViewIsModal(boolean accessibilityViewIsModal) {
	}

	public boolean shouldGroupAccessibilityChildren() {
		return false;
	}

	public void setShouldGroupAccessibilityChildren(boolean shouldGroupAccessibilityChildren) {

	}
}
