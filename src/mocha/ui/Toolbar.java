/**
 *  @author Shaun
 *  @date 3/21/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.lang.reflect.Method;
import java.util.*;

public class Toolbar extends View {

	public enum Position {
		ANY, TOP, BOTTOM
	}

	private static mocha.ui.Appearance.Storage<Toolbar, Appearance> appearanceStorage;

	public static <E extends Toolbar> Appearance appearance(Class<E> cls) {
		if(appearanceStorage == null) {
			appearanceStorage = new mocha.ui.Appearance.Storage<Toolbar, Appearance>(Toolbar.class, Appearance.class);
		}

		return appearanceStorage.appearance(cls);
	}

	public static Appearance appearance() {
		return appearance(Toolbar.class);
	}

	private BarStyle barStyle;
	private int tintColor;
	private List<BarButtonItem> items;
	private boolean translucent;
	private BarMetricsStorage<Map<Position,Image>> backgroundImages;
	private Map<Position,Image> shadowImages;
	private ImageView shadowImageView;
	private Position position;

	public Toolbar() { this(new Rect(0.0f, 0.0f, 320.0f, 44.0f)); }
	public Toolbar(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.barStyle = BarStyle.DEFAULT;
		this.tintColor = Color.TRANSPARENT;
		this.items = new ArrayList<BarButtonItem>();
		this.backgroundImages = new BarMetricsStorage<Map<Position, Image>>();
		this.shadowImages = new HashMap<Position, Image>();
		this.position = Position.ANY;

		if(appearanceStorage != null) {
			appearanceStorage.apply(this);
		}
	}

	public Size sizeThatFits(Size size) {
		return new Size(size.width, Math.min(size.height, 44.0f));
	}

	public void setFrame(Rect frame) {
		super.setFrame(frame);
		this.determinePosition();
	}

	public void didMoveToSuperview() {
		super.didMoveToSuperview();
		this.determinePosition();
	}

	private void determinePosition() {
		if(frame.origin.y <= 0.0f) {
			this.position = Position.TOP;
		} else {
			View superview = this.getSuperview();

			if(superview != null && frame.maxY() > superview.getBounds().height()) {
				this.position = Position.BOTTOM;
			} else {
				this.position = Position.ANY;
			}
		}
	}

	public int getTintColor() {
		return tintColor;
	}

	public void setTintColor(int tintColor) {
		this.tintColor = tintColor;
		this.setNeedsDisplay();
	}

	public BarStyle getBarStyle() {
		return barStyle;
	}

	public void setBarStyle(BarStyle barStyle) {
		this.barStyle = barStyle;
		this.setNeedsDisplay();
	}

	public boolean isTranslucent() {
		return translucent;
	}

	public void setTranslucent(boolean translucent) {
		this.translucent = translucent;
		this.setNeedsDisplay();
	}

	public void setBackgroundImage(Image backgroundImage, Position position, BarMetrics barMetrics) {
		Map<Position,Image> backgroundImages = this.backgroundImages.get(barMetrics);

		if(backgroundImages == null) {
			backgroundImages = new HashMap<Position, Image>();
			this.backgroundImages.set(barMetrics, backgroundImages);
		}

		backgroundImages.put(position, backgroundImage);
	}

	public Image getBackgroundImageForToolbarPosition(Position position, BarMetrics barMetrics) {
		Map<Position,Image> backgroundImages = this.backgroundImages.get(barMetrics);
		if(backgroundImages != null) {
			return backgroundImages.get(position);
		} else {
			return null;
		}
	}

	public void setShadowImage(Image shadowImage, Position position) {
		this.shadowImages.put(position, shadowImage);
	}

	public Image getShadowImageForToolbarPosition(Position position) {
		return this.shadowImages.get(position);
	}

	public List<BarButtonItem> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	public void setItems(BarButtonItem... items) {
		this.setItems(false, items);
	}

	public void setItems(boolean animated, BarButtonItem... items) {
		List<BarButtonItem> barButtonItems = new ArrayList<BarButtonItem>();
		Collections.addAll(barButtonItems, items);
		this.setItems(barButtonItems, animated);
	}

	public void setItems(List<BarButtonItem> items) {
		this.setItems(items, false);
	}

	public void setItems(List<BarButtonItem> items, boolean animated) {
		final List<BarButtonItem> oldItems = new ArrayList<BarButtonItem>(this.items);
		oldItems.removeAll(items);

		if(oldItems.size() > 0) {
			if(animated) {
				View.animateWithDuration(200, new Animations() {
					public void performAnimatedChanges() {
						for(BarButtonItem item : oldItems) {
							if(item.getView() != null) {
								item.getView().setAlpha(0.0f);
							}
						}
					}
				}, new AnimationCompletion() {
					public void animationCompletion(boolean finished) {
						for(BarButtonItem item : oldItems) {
							if(item.getView() != null) {
								item.getView().removeFromSuperview();
							}
						}
					}
				});
			} else {
				for(BarButtonItem item : oldItems) {
					if(item.getView() != null) {
						item.getView().removeFromSuperview();
					}
				}
			}
		}

		this.items.clear();

		if(items != null) {
			this.items.addAll(items);
		}

		if(animated) {
			View.beginAnimations();
			View.setAnimationDuration(200);
		}

		Rect bounds = this.getBounds();

		for(BarButtonItem item : this.items) {
			View view = item.getView();

			if(view == null) {
				item.setView((view = BarButton.button(item)));
			}

			if(view != null) {
				if(animated) {
					boolean areAnimationsEnabled = View.areAnimationsEnabled();
					View.setAnimationsEnabled(false);
					view.setAlpha(0.0f);
					view.setFrame(new Rect(Point.zero(), view.sizeThatFits(new Size(bounds.width(), bounds.size.height))));
					this.addSubview(view);
					View.setAnimationsEnabled(areAnimationsEnabled);
					view.setAlpha(1.0f);
				} else {
					view.setFrame(new Rect(Point.zero(), view.sizeThatFits(new Size(bounds.width(), bounds.size.height))));
					this.addSubview(view);
				}
			}
		}

		this.layoutItems();

		if(animated) {
			View.commitAnimations();
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();
		this.layoutItems();

		Size size;

		Image shadowImage = this.getShadowImageForToolbarPosition(this.position);

		if(shadowImage == null && this.position != Position.ANY) {
			shadowImage = this.getShadowImageForToolbarPosition(Position.ANY);
		}

		if(shadowImage != null && (size = shadowImage.getSize()).width > 0 && size.height > 0) {
			if(this.shadowImageView == null) {
				this.shadowImageView = new ImageView();
				this.addSubview(this.shadowImageView);
			}

			if(this.shadowImageView.getImage() != shadowImage) {
				this.shadowImageView.setImage(shadowImage);
			}

			Rect bounds = this.getBounds();

			if(this.position == Position.TOP) {
				this.shadowImageView.setFrame(new Rect(0.0f, bounds.size.height, bounds.size.width, size.height));
			} else {
				this.shadowImageView.setFrame(new Rect(0.0f, -size.height, bounds.size.width, size.height));
			}
		} else if(this.shadowImageView != null) {
			this.shadowImageView.removeFromSuperview();
			this.shadowImageView = null;
		}

	}

	private void layoutItems() {
		float totalItemsWidth = 0;
		int totalFlexItems = 0;

		for (BarButtonItem item : this.items) {
			if(item.getSystemItem() == BarButtonItem.SystemItem.FLEXIBLE_SPACE) {
				totalFlexItems++;
			} else {
				totalItemsWidth += Math.max(item._getWidth(), 0.0f);
			}
		}

		float itemsInset = 5.0f;
		Size size = this.getBounds().size;
		size.width -= itemsInset + itemsInset;

		float flexItemWidth = Math.max((totalFlexItems > 0) ? floorf(((size.width - totalItemsWidth) / totalFlexItems)) : 0.0f, 0.0f);
		float offset = itemsInset;

		for (BarButtonItem item : this.items) {
			if(item.getSystemItem() == BarButtonItem.SystemItem.FLEXIBLE_SPACE) {
				offset += flexItemWidth;
			} else {
				View view = item.getView();
				float width = item._getWidth();

				if (view != null) {
					Rect frame = view.getFrame();
					frame.size.width = width;
					frame.origin.x = offset;
					frame.origin.y = floorf((size.height - frame.size.height) / 2.0f);
					view.setFrame(frame);
				}

				offset += width;
			}
		}
	}

	public void draw(Context context, Rect rect) {
		Image backgroundImage = this.getBackgroundImageForToolbarPosition(this.position, BarMetrics.DEFAULT);

		if(backgroundImage == null && this.position != Position.ANY) {
			backgroundImage = this.getBackgroundImageForToolbarPosition(Position.ANY, BarMetrics.DEFAULT);
		}

		if(backgroundImage == null) {
			if(this.position == Position.TOP) {
				backgroundImage = Image.imageNamed(R.drawable.mocha_toolbar_default_background_top);
			} else {
				backgroundImage = Image.imageNamed(R.drawable.mocha_toolbar_default_background);
			}
		}

		backgroundImage.draw(context, rect);
	}

	public static class Appearance extends mocha.ui.Appearance <Toolbar> {
		private Method setShadowImage;
		private Method setBackgroundImage;

		public Appearance() {
			try {
				this.setShadowImage = Toolbar.class.getMethod("setShadowImage", Image.class, Position.class);
				this.setBackgroundImage = Toolbar.class.getMethod("setBackgroundImage", Image.class, Position.class, BarMetrics.class);
			} catch (NoSuchMethodException ignored) { }
		}

		public void setShadowImage(Image shadowImage, Position position) {
			this.store(this.setShadowImage, shadowImage, position);
		}

		public void setBackgroundImage(Image backgroundImage, Position position, BarMetrics barMetrics) {
			this.store(this.setBackgroundImage, backgroundImage, position, barMetrics);
		}

	}
}
