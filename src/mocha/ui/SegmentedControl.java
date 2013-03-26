/**
 *  @author Shaun
 *  @date 3/25/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Image;
import mocha.graphics.Offset;
import mocha.graphics.Rect;
import mocha.graphics.Size;

import java.util.List;

public class SegmentedControl extends Control {
	public static final int NO_SEGMENT = -1;

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
	private boolean apportionsSegmentWidthsByContent;

	public SegmentedControl() { }
	public SegmentedControl(Rect frame) { super(frame); }

	public SegmentedControl(List items) {
		this();

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
	}

	public boolean isApportionsSegmentWidthsByContent() {
		return apportionsSegmentWidthsByContent;
	}

	public void setApportionsSegmentWidthsByContent(boolean apportionsSegmentWidthsByContent) {
		this.apportionsSegmentWidthsByContent = apportionsSegmentWidthsByContent;
	}

	public int getNumberOfSegments() {
		return 0;
	}

	// insert before segment number. 0..#segments. value pinned
	public void insertSegmentWithTitle(String title, int atIndex, boolean animated) {

	}

	public void insertSegmentWithImage(Image image, int atIndex, boolean animated) {

	}

	public void removeSegmentAtIndex(int segment, boolean animated) {

	}

	public void removeAllSegments() {

	}

	// can only have image or title, not both. must be 0..#segments - 1 (or ignored). default is nil
	public void setTitle(String title, int segment) {

	}

	public String titleForSegmentAtIndex(int segment) {
		return null;
	}

	// can only have image or title, not both. must be 0..#segments - 1 (or ignored). default is nil
	public void setTitle(Image image, int segment) {

	}

	public Image imageForSegmentAtIndex(int segment) {
		return null;
	}

	// set to 0.0 width to autosize. default is 0.0
	public void setWidth(float width, int segment) {

	}

	public float widthForSegmentAtIndex(int segment) {
		return 0.0f;
	}

	// adjust offset of image or text inside the segment. default is (0,0)
	public void setContentOffset(Size offset, int segment) {

	}

	public Size contentOffsetForSegmentAtIndex(int segment) {
		return null;
	}

	// default is YES
	public void setEnabled(boolean enabled, int segment) {

	}

	public boolean isEnabledForSegmentAtIndex(int segment) {
		return true;
	}

	// ignored in momentary mode. returns last segment pressed. default is UISegmentedControlNoSegment until a segment is pressed
	// the UIControlEventValueChanged action is invoked when the segment changes via a user event. set to UISegmentedControlNoSegment to turn off selection
	public int getSelectedSegmentIndex() {
		return NO_SEGMENT;
	}

	public void setSelectedSegmentIndex(int segment) {

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

	public void setBackgroundImage(Image backgroundImage, BarMetrics barMetrics1, State... state)  {

	}

	public Image backgroundImageForState(BarMetrics barMetrics1, State... state) {
		return null;
	}

	/* To customize the segmented control appearance you will need to provide divider images to go between two unselected segments (leftSegmentState:UIControlStateNormal rightSegmentState:UIControlStateNormal), selected on the left and unselected on the right (leftSegmentState:UIControlStateSelected rightSegmentState:UIControlStateNormal), and unselected on the left and selected on the right (leftSegmentState:UIControlStateNormal rightSegmentState:UIControlStateSelected).
	 */
	public void setDividerImage(Image dividerImage, BarMetrics barMetrics, State[] leftState, State[] rightState) {

	}

	public Image dividerImage(BarMetrics barMetrics, State[] leftState, State[] rightState) {
		return null;
	}

	/* You may specify the font, text color, text shadow color, and text shadow offset for the title in the text attributes dictionary, using the keys found in UIStringDrawing.h.
	 */
	public void setTitleTextAttributes(TextAttributes titleTextAttributes, State... state) {

	}

	public TextAttributes titleTextAttributesForState(State... state) {
		return null;
	}

	/* For adjusting the position of a title or image within the given segment of a segmented control.
	 */
	public void setContentPositionAdjustment(Offset adjustment, SegmentType leftCenterRightOrAlone, BarMetrics barMetrics) {

	}

	public Offset contentPositionAdjustmentForSegmentType(SegmentType leftCenterRightOrAlone, BarMetrics barMetrics) {
		return null;
	}


}
