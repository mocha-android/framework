/**
 *  @author Shaun
 *  @date 3/25/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

import java.lang.reflect.Method;
import java.util.*;

public class SegmentedControl extends Control {
	public static final int NO_SEGMENT = -1;
	private static final int TAG = 999;

	public enum Style {
		PLAIN,     // large plain
		BORDERED,  // large bordered
		BAR       // small button/nav bar style. tintable
	}

	public enum SegmentType {
		ANY,
		LEFT,   // The capped, leftmost segment. Only applies when numSegments > 1.
		CENTER, // Any segment between the left and rightmost segments. Only applies when numSegments > 2.
		RIGHT,  // The capped,rightmost segment. Only applies when numSegments > 1.
		ALONE,  // The standalone segment, capped on both ends. Only applies when numSegments = 1.
	}

	private Style segmentedControlStyle;
	private boolean momentary;
	private List<Segment> segments;
	private int lastSelectedSegment;
	private int selectedSegment;
	private ActionTarget actionTarget;

	private Map<SegmentType,BarMetricsStorage<Offset>> contentPositionAdjustments;
	private Map<EnumSet<Control.State>,TextAttributes> titleTextAttributes;

	private BarMetricsStorage<Map<EnumSet<Control.State>, Image>> backgroundImages;
	private BarMetricsStorage<Map<LeftRightState, Image>> dividerImages;

	private static mocha.ui.Appearance.Storage<SegmentedControl, Appearance> appearanceStorage;

	public static <E extends SegmentedControl> Appearance appearance(Class<E> cls) {
		if(appearanceStorage == null) {
			appearanceStorage = new mocha.ui.Appearance.Storage<SegmentedControl, Appearance>(SegmentedControl.class, Appearance.class);
		}

		return appearanceStorage.appearance(cls);
	}

	public static Appearance appearance() {
		return appearance(SegmentedControl.class);
	}

	public SegmentedControl() { }
	public SegmentedControl(Rect frame) { super(frame); }

	public SegmentedControl(List items) {
		this();

		for(Object item : items) {
			if(item instanceof CharSequence) {
				this.segments.add(new Segment((CharSequence)item));
			} else if(item instanceof Image) {
				this.segments.add(new Segment((Image)item));
			} else {
				MWarn("SegmentedControl only supports CharSequence and Image as items. Ignoring " + item);
			}
		}
	}

	protected void onCreate(Rect frame) {
		this.contentPositionAdjustments = new HashMap<SegmentType, BarMetricsStorage<Offset>>();
		this.titleTextAttributes = new HashMap<EnumSet<Control.State>, TextAttributes>();
		this.backgroundImages = new BarMetricsStorage<Map<EnumSet<State>, Image>>();
		this.dividerImages = new BarMetricsStorage<Map<LeftRightState, Image>>();
		this.segments = new ArrayList<Segment>();
		this.selectedSegment = NO_SEGMENT;
		this.lastSelectedSegment = NO_SEGMENT;

		this.actionTarget = new ActionTarget() {
			public void onControlEvent(Control control, ControlEvent controlEvent, Event event) {
				if(control instanceof Segment.Button) {
					segmentSelected((Segment.Button)control, event);
				}
			}
		};

		if(appearanceStorage != null) {
			appearanceStorage.apply(this);
		}

		super.onCreate(frame);
	}

	public Style getSegmentedControlStyle() {
		return segmentedControlStyle;
	}

	public void setSegmentedControlStyle(Style segmentedControlStyle) {
		this.segmentedControlStyle = segmentedControlStyle;
	}

	public boolean isMomentary() {
		return momentary;
	}

	public void setMomentary(boolean momentary) {
		this.momentary = momentary;

		if(this.momentary) {
			if(this.selectedSegment != NO_SEGMENT) {
				Button button = this.segments.get(this.selectedSegment).button;

				if(button != null) {
					button.setSelected(false);
				}
			}

			this.selectedSegment = NO_SEGMENT;
		}
	}

	public int getNumberOfSegments() {
		return this.segments.size();
	}

	// insert before segment number. 0..#segments. value pinned
	public void insertSegment(CharSequence title, int atIndex, boolean animated) {
		this.segments.add(atIndex, new Segment(title));
		this.layoutSegments(animated, null);
	}

	public void insertSegment(Image image, int atIndex, boolean animated) {
		this.segments.add(atIndex, new Segment(image));
		this.layoutSegments(animated, null);
	}

	public void removeSegment(int segment, boolean animated) {
		this.layoutSegments(animated, this.segments.get(segment));
	}

	public void removeAllSegments() {
		for(Segment segment : this.segments) {
			if(segment.button != null) {
				segment.button.removeFromSuperview();
			}
		}

		this.segments.clear();
	}

	// can only have image or title, not both. must be 0..#segments - 1 (or ignored). default is nil
	public void setTitle(CharSequence title, int segment) {
		Segment segment1 = this.segments.get(segment);
		segment1.title = title;
		segment1.image = null;
		segment1.contentChanged();
	}

	public CharSequence getTitleForSegment(int segment) {
		return this.segments.get(segment).title;
	}

	// can only have image or title, not both. must be 0..#segments - 1 (or ignored). default is nil
	public void setImage(Image image, int segment) {
		Segment segment1 = this.segments.get(segment);
		segment1.title = null;
		segment1.image = image;
		segment1.contentChanged();
	}

	public Image getImageForSegment(int segment) {
		return this.segments.get(segment).image;
	}

	// set to 0.0 width to autosize. default is 0.0
	public void setWidth(float width, int segment) {
		this.setWidth(width, segment, false);
	}

	public void setWidth(float width, int segment, boolean animated) {
		Segment segment1 = this.segments.get(segment);

		if(segment1.width != width) {
			segment1.width = width;
			this.layoutSegments(animated, null);
		}
	}

	public float getWidthForSegment(int segment) {
		return this.segments.get(segment).width;
	}

	// adjust offset of image or text inside the segment. default is (0,0)
	public void setContentOffset(Offset offset, int segment) {
		this.segments.get(segment).contentOffset = offset.copy();
		this.segments.get(segment).contentChanged();
	}

	public Offset getContentOffsetForSegment(int segment) {
		return this.segments.get(segment).contentOffset;
	}

	// default is YES
	public void setEnabled(boolean enabled, int segment) {
		this.segments.get(segment).enabled = enabled;
		this.segments.get(segment).contentChanged();
	}

	public boolean isSegmentEnabled(int segment) {
		return this.segments.get(segment).enabled;
	}

	// ignored in momentary mode. returns last segment pressed. default is UISegmentedControlNoSegment until a segment is pressed
	// the UIControlEventValueChanged action is invoked when the segment changes via a user event. set to UISegmentedControlNoSegment to turn off selection
	public int getSelectedSegment() {
		return this.lastSelectedSegment;
	}

	public void setSelectedSegment(int segment) {
		if(this.momentary) return;

		if(this.selectedSegment != NO_SEGMENT) {
			Button button = this.segments.get(this.selectedSegment).button;

			if(button != null) {
				button.setSelected(false);
			}
		}

		if(segment != NO_SEGMENT) {
			Button button = this.segments.get(segment).button;

			if(button != null) {
				button.setSelected(true);
			}
		}

		this.selectedSegment = segment;
		this.lastSelectedSegment = segment;
	}

	private void segmentSelected(final Segment.Button button, Event event) {
		final int segment = button.getTag() - TAG;
		if(this.selectedSegment == segment) return;

		button.setSelected(true);

		if(this.selectedSegment != NO_SEGMENT) {
			this.segments.get(this.lastSelectedSegment).button.setSelected(false);
		}

		if(this.momentary) {
			performAfterDelay(100, new Runnable() {
				public void run() {
					button.setSelected(false);
				}
			});
		} else {
			this.selectedSegment = segment;
		}

		this.lastSelectedSegment = segment;
		this.sendActionsForControlEvents(event, ControlEvent.VALUE_CHANGED);
	}

	/* Default tintColor is nil. Only used if style is BAR
	 */

	public int getTintColor() {
		return 0;
	}

	public void setTintColor(int tintColor) {

	}

	/* If backgroundImage is an image returned from -[UIImage resizableImageWithCapInsets:] the cap widths will be calculated from that information, otherwise, the cap width will be calculated by subtracting one from the image's width then dividing by 2. The cap widths will also be used as the margins for text placement. To adjust the margin use the margin adjustment methods.

	 In general, you should specify a value for the normal state to be used by other states which don't have a custom value set.

	 Similarly, when a property is dependent on the bar metrics (on the iPhone in landscape orientation, bars have a different height from standard), be sure to specify a value for UIBarMetricsDefault.
	 In the case of the segmented control, appearance properties for UIBarMetricsLandscapePhone are only respected for segmented controls in the smaller navigation and toolbars that are used in landscape orientation on the iPhone.
	 */

	public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics, State... state)  {
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

	/* To customize the segmented control appearance you will need to provide divider images to go between two unselected segments (leftSegmentState:UIControlStateNormal rightSegmentState:UIControlStateNormal), selected on the left and unselected on the right (leftSegmentState:UIControlStateSelected rightSegmentState:UIControlStateNormal), and unselected on the left and selected on the right (leftSegmentState:UIControlStateNormal rightSegmentState:UIControlStateSelected).
	 */
	public void setDividerImage(Image dividerImage, BarMetrics barMetrics, State leftState, State rightState) {
		this.setDividerImage(dividerImage, barMetrics, new State[] { leftState }, new State[] { rightState });
	}

	public void setDividerImage(Image dividerImage, BarMetrics barMetrics, State[] leftState, State[] rightState) {
		Map<LeftRightState, Image> dividerImages = this.dividerImages.get(barMetrics);

		if(dividerImages == null) {
			dividerImages = new HashMap<LeftRightState, Image>();
			this.dividerImages.set(barMetrics, dividerImages);
		}

		LeftRightState leftRightState = new LeftRightState(Control.getStateSet(leftState), Control.getStateSet(rightState));

		if(dividerImage == null) {
			dividerImages.remove(leftRightState);
		} else {
			dividerImages.put(leftRightState, dividerImage);
		}

	}

	public Image getDividerImage(BarMetrics barMetrics, State[] leftState, State[] rightState) {
		Map<LeftRightState, Image> dividerImages = this.dividerImages.get(barMetrics);
		return dividerImages == null ? null :  dividerImages.get(new LeftRightState(Control.getStateSet(leftState), Control.getStateSet(rightState)));
	}

	/* You may specify the font, text color, text shadow color, and text shadow offset for the title in the text attributes dictionary, using the keys found in UIStringDrawing.h.
	 */
	public void setTitleTextAttributes(TextAttributes titleTextAttributes, State... state) {
		EnumSet<Control.State> stateSet = Control.getStateSet(state);

		if(titleTextAttributes == null) {
			this.titleTextAttributes.remove(stateSet);
		} else {
			this.titleTextAttributes.put(stateSet, titleTextAttributes);
		}
	}

	public TextAttributes getTitleTextAttributesForState(Control.State... state) {
		return this.titleTextAttributes.get(Control.getStateSet(state));
	}

	/* For adjusting the position of a title or image within the given segment of a segmented control.
	 */
	public void setContentPositionAdjustment(Offset adjustment, SegmentType leftCenterRightOrAlone, BarMetrics barMetrics) {
		BarMetricsStorage<Offset> adjustments = this.contentPositionAdjustments.get(leftCenterRightOrAlone);

		if(adjustments == null) {
			adjustments = new BarMetricsStorage<Offset>();
			this.contentPositionAdjustments.put(leftCenterRightOrAlone, adjustments);
		}

		adjustments.set(barMetrics, adjustment);
	}

	public Offset getContentPositionAdjustmentForSegmentType(SegmentType leftCenterRightOrAlone, BarMetrics barMetrics) {
		BarMetricsStorage<Offset> adjustments = this.contentPositionAdjustments.get(leftCenterRightOrAlone);
		Offset adjust = null;

		if(adjustments != null) {
			adjust = adjustments.get(barMetrics);
		}

		if(adjust == null && leftCenterRightOrAlone != SegmentType.ANY) {
			adjustments = this.contentPositionAdjustments.get(SegmentType.ANY);

			if(adjustments != null) {
				adjust = adjustments.get(barMetrics);
			}
		}

		if(adjust == null) {
			return Offset.zero();
		} else {
			return adjust;
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		this.layoutSegments(false, null);
	}

	private void layoutSegments(boolean animated, final Segment removeSegment) {
		float totalWidth = 0;
		int totalAutosizingSegments = 0;

		for (Segment segment : this.segments) {
			if(segment == removeSegment) continue;

			if(segment.width <= 0.0f) {
				totalAutosizingSegments++;
			} else {
				totalWidth += segment.width;
			}
		}

		Rect bounds = this.getBounds();

		float autosizingSegmentWidth = Math.max((totalAutosizingSegments > 0) ? floorf(((bounds.size.width - totalWidth) / totalAutosizingSegments)) : 0.0f, 0.0f);

		Font font;
		Offset shadowOffset;

		int normalTextColor;
		int normalShadowColor;

		int selectedTextColor;
		int selectedShadowColor;

		TextAttributes normalAttributes = this.getTitleTextAttributesForState(State.NORMAL);
		TextAttributes selectedAttributes = this.getTitleTextAttributesForState(State.NORMAL, State.SELECTED);

		if(normalAttributes != null && normalAttributes.font != null) {
			font = normalAttributes.font;
		} else {
			font = Font.getBoldSystemFontWithSize(15.0f);
		}

		if(normalAttributes != null && normalAttributes.textColor != Color.TRANSPARENT) {
			normalTextColor = normalAttributes.textColor;
		} else {
			normalTextColor = Color.GRAY;
		}

		if(normalAttributes != null && normalAttributes.shadowOffset != null) {
			shadowOffset = normalAttributes.shadowOffset;
		} else {
			shadowOffset = new Offset(0.0f, -1.0f);
		}

		if(normalAttributes != null && normalAttributes.shadowColor != Color.TRANSPARENT) {
			normalShadowColor = normalAttributes.shadowColor;
		} else {
			normalShadowColor = Color.WHITE;
		}

		if(selectedAttributes != null && selectedAttributes.textColor != Color.TRANSPARENT) {
			selectedTextColor = selectedAttributes.textColor;
		} else {
			selectedTextColor = Color.WHITE;
		}

		if(selectedAttributes != null && selectedAttributes.shadowColor != Color.TRANSPARENT) {
			selectedShadowColor = selectedAttributes.shadowColor;
		} else {
			selectedShadowColor = Color.white(0.0f, 0.5f);
		}


		if(animated) {
			View.beginAnimations();
		}

		Rect frame = new Rect();
		frame.origin.y = 0.0f;
		frame.origin.x = 0.0f;
		frame.size.height = bounds.size.height;

		Image normal = this.getBackgroundImage(BarMetrics.DEFAULT, State.NORMAL);
		Image selected = this.getBackgroundImage(BarMetrics.DEFAULT, State.NORMAL, State.SELECTED);

		int index = 0;
		int count = this.segments.size();
		Segment last = count > 0 ? this.segments.get(count - 1) : null;

		for (Segment segment : this.segments) {

			if(segment == last) {
				frame.size.width = bounds.size.width - frame.origin.x;
			} else {
				frame.size.width = segment.width <= 0.0f ? autosizingSegmentWidth : segment.width;
			}

			if(segment != removeSegment) {
				if(index == 0) {
					segment.setSegmentType(count == 1 ? SegmentType.ALONE : SegmentType.LEFT, null);
				} else {
					State[] leftState = this.segments.get(index - 1).button.getStates();
					if(index == count - 1) {
						segment.setSegmentType(SegmentType.RIGHT, leftState);
					} else {
						segment.setSegmentType(SegmentType.CENTER, leftState);
					}
				}
			}

			if(segment.button == null && segment != removeSegment) {
				boolean enabled = View.areAnimationsEnabled();
				View.setAnimationsEnabled(false);

				segment.button = segment.createButton();
				segment.button.setBackgroundColor(Color.TRANSPARENT);
				segment.button.addActionTarget(this.actionTarget, ControlEvent.TOUCH_UP_INSIDE);
				segment.button.setSelected(this.selectedSegment == index);

				segment.button.setBackgroundImage(normal, State.NORMAL);
				segment.button.setBackgroundImage(selected, State.NORMAL, State.SELECTED);

				segment.button.getTitleLabel().setFont(font);
				segment.button.getTitleLabel().setShadowOffset(shadowOffset);

				segment.button.setTitleColor(normalTextColor, State.NORMAL);
				segment.button.setTitleColor(selectedTextColor, State.NORMAL, State.SELECTED);

				segment.button.setTitleShadowColor(normalShadowColor, State.NORMAL);
				segment.button.setTitleShadowColor(selectedShadowColor, State.NORMAL, State.SELECTED);

				this.addSubview(segment.button);

				if(animated) {
					Rect tempFrame = frame.copy();
					tempFrame.origin.x += floorf(tempFrame.size.width / 2.0f);
					tempFrame.size.width = 0.0f;
					segment.button.setFrame(frame);
				}

				View.setAnimationsEnabled(enabled);
			} else if(segment == removeSegment) {
				if(segment.button != null) {
					if(animated) {
						Rect tempFrame = segment.button.getFrame();
						tempFrame.origin.x += floorf(tempFrame.size.width / 2.0f);
						tempFrame.size.width = 0.0f;
						segment.button.setFrame(frame);
					} else {
						segment.button.removeFromSuperview();
					}
				}

				continue;
			}

			segment.button.setFrame(frame);
			segment.button.setTag(TAG + index);
			index++;

			frame.origin.x = frame.maxX();
		}

		if(removeSegment != null) {
			this.segments.remove(removeSegment);
		}

		if(animated) {
			if(removeSegment != null) {
				View.setAnimationDidStopCallback(new AnimationDidStop() {
					public void animationDidStop(String animationID, boolean finished, Object context) {
						if(removeSegment.button != null) {
							removeSegment.button.removeFromSuperview();
						}
					}
				});
			}

			View.commitAnimations();
		}
	}

	private static class LeftRightState {
		final EnumSet<Control.State> leftState;
		final EnumSet<Control.State> rightState;


		private LeftRightState(EnumSet<State> leftState, EnumSet<State> rightState) {
			this.leftState = leftState;
			this.rightState = rightState;
		}

		public boolean equals(Object o) {
			if(!(o instanceof LeftRightState)) {
				return false;
			} else if(this == o) {
				return true;
			} else {
				LeftRightState other = (LeftRightState)o;
				return this.leftState.equals(other.leftState) && this.rightState.equals(other.rightState);
			}
		}
	}

	private class Segment {
		CharSequence title;
		Image image;
		float width = 0.0f;
		Offset contentOffset = Offset.zero();
		boolean enabled = true;
		Segment.Button button;
		SegmentType segmentType;
		State[] leftState;

		private Segment(CharSequence title) {
			this.title = title;
		}

		private Segment(Image image) {
			this.image = image;
		}

		void contentChanged() {
			if(this.button == null) return;

			if(contentOffset == null) {
				this.contentOffset = Offset.zero();
			}

			this.button.setupContent();
			this.button.updateDivider();
		}

		void setSegmentType(SegmentType segmentType, State[] leftState) {
			this.segmentType = segmentType;
			this.leftState = leftState;

			if(this.button != null) {
				this.button.updateDivider();
			}
		}

		Segment.Button createButton() {
			return new Button();
		}

		class Button extends mocha.ui.Button {
			private ImageView dividerView;

			Button() {
				this.setClipsToBounds(true);
				setupContent();
			}

			void setupContent() {
				this.setTitle(title, State.NORMAL);
				this.setImage(image, State.NORMAL);
				this.setEnabled(enabled);
				this.setTitleEdgeInsets(new EdgeInsets(contentOffset.vertical, contentOffset.horizontal, -contentOffset.vertical, -contentOffset.horizontal));
				this.setImageEdgeInsets(new EdgeInsets(contentOffset.vertical, contentOffset.horizontal, -contentOffset.vertical, -contentOffset.horizontal));
			}

			void updateDivider() {
				if(segmentType == SegmentType.LEFT || segmentType == SegmentType.ALONE || leftState == null) {
					if(this.dividerView != null) {
						this.dividerView.removeFromSuperview();
						this.dividerView = null;
					}

					return;
				}

				Image divider = getDividerImage(BarMetrics.DEFAULT, leftState, this.getStates());

				if(divider == null) {
					divider = getDividerImage(BarMetrics.DEFAULT, new State[] { State.NORMAL }, new State[] { State.NORMAL });
				}

				if(divider == null) {
					if(this.dividerView != null) {
						this.dividerView.removeFromSuperview();
						this.dividerView = null;
					}

					return;
				}

				if(this.dividerView == null) {
					this.dividerView = new ImageView();
					this.dividerView.setAutoresizing(Autoresizing.FLEXIBLE_HEIGHT, Autoresizing.FLEXIBLE_RIGHT_MARGIN);
					this.dividerView.setContentMode(ContentMode.SCALE_TO_FILL);
					this.addSubview(this.dividerView);
				}

				this.dividerView.setImage(divider);

				Rect bounds = this.getBounds();
				Rect frame = new Rect();
				frame.size.height = bounds.size.height;
				frame.size.width = divider.getSize().width;
				this.dividerView.setFrame(frame);
			}

			public Rect getBackgroundRectForBounds(Rect bounds) {
				Rect rect = super.getBackgroundRectForBounds(bounds);

				Image backgroundImage = this.getCurrentBackgroundImage();

				if(backgroundImage == null) {
					return rect;
				}

				float leftCap;
				float rightCap;

				if(backgroundImage.getCapInsets() != null) {
					EdgeInsets insets = backgroundImage.getCapInsets();
					leftCap = insets.left;
					rightCap = insets.right;
				} else {
					leftCap = rightCap = floorf((backgroundImage.getSize().width - 2) / 2.0f);
				}

				switch (segmentType) {
					case LEFT:
						rect.size.width += rightCap;
						break;
					case CENTER:
						rect.origin.x -= leftCap;
						rect.size.width += leftCap + rightCap;
						break;
					case RIGHT:
						rect.origin.x -= leftCap;
						rect.size.width += leftCap;
						break;
					case ALONE:
					case ANY:
					default:
						break;
				}

				return rect;
			}
		}
	}

	public static class Appearance extends mocha.ui.Appearance <SegmentedControl> {
		private Method setBackgroundImage;
		private Method setContentPositionAdjustment;
		private Method setTitleTextAttributes;
		private Method setDividerImage;

		public Appearance() {
			try {
				this.setTitleTextAttributes = SegmentedControl.class.getMethod("setTitleTextAttributes", TextAttributes.class, Control.State[].class);
				this.setContentPositionAdjustment = SegmentedControl.class.getMethod("setContentPositionAdjustment", Offset.class, BarMetrics.class);

				this.setBackgroundImage = SegmentedControl.class.getMethod("setBackgroundImage", Image.class, BarMetrics.class, Control.State[].class);

				this.setDividerImage = SegmentedControl.class.getMethod("setDividerImage", Image.class, BarMetrics.class, Control.State[].class, Control.State[].class);

			} catch (NoSuchMethodException ignored) { }
		}

		public void setTitleTextAttributes(TextAttributes textAttributes, Control.State... state) {
			this.store(this.setTitleTextAttributes, textAttributes, state);
		}

		public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics, Control.State... state) {
			this.store(this.setBackgroundImage, backgroundImage, barMetrics, state);
		}

		public void setDividerImage(Image dividerImage, BarMetrics barMetrics, State[] leftState, State[] rightState) {
			this.store(this.setDividerImage, dividerImage, barMetrics, leftState, rightState);
		}

	}

}
