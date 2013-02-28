/**
 *  @author Shaun
 *  @date 2/28/13
 *  @copyright 2013 enormego. All rights reserved.
 */

package mocha.ui;

import mocha.graphics.Point;
import mocha.graphics.Rect;

public interface Accessibility {

	public enum Trait {
		// Used when the element has no traits.
		NONE,

		// Used when the element should be treated as a button.
		BUTTON,

		// Used when the element should be treated as a link.
		LINK,

		// Used when an element acts as a header for a content section (e.g. the title of a navigation bar).
		HEADER,

		// Used when the text field element should also be treated as a search field.
		SEARCH_FIELD,

		// Used when the element should be treated as an image. Can be combined with button or link, for example.
		IMAGE,

		/*
		 Used when the element is selected.
		 For example, a selected row in a table or a selected button within a segmented control.
		 */
		SELECTED,

		// Used when the element plays its own sound when activated.
		PLAYS_SOUND,

		// Used when the element acts as a keyboard key.
		KEYBOARD_KEY,

		// Used when the element should be treated as static text that cannot change.
		STATIC_TEXT,

		/*
		 Used when an element can be used to provide a quick summary of current 
		 conditions in the app when the app first launches.  For example, when Weather
		 first launches, the element with today's weather conditions is marked with
		 this trait.
		 */
		SUMMARY_ELEMENT,

		// Used when the control is not enabled and does not respond to user input.
		NOT_ENABLED,

		/*
		 Used when the element frequently updates its label or value, but too often to send notifications. 
		 Allows an accessibility client to poll for changes. A stopwatch would be an example.
		 */
		UPDATES_FREQUENTLY,

		/*
		 Used when activating an element starts a media session (e.g. playing a movie, recording audio) 
		 that should not be interrupted by output from an assistive technology, like VoiceOver.
		 */
		STARTS_MEDIA_SESSION,

		/*
		 Used when an element can be "adjusted" (e.g. a slider). The element must also 
		 implement accessibilityIncrement and accessibilityDecrement.
		 */
		ADJUSTABLE,

		// Used when an element allows direct touch interaction for VoiceOver users (for example, a view representing a piano keyboard).
		ALLOWS_DIRECT_INTERACTION,

		/*
		 Informs VoiceOver that it should scroll to the next page when it finishes reading the contents of the
		 element. VoiceOver will scroll by calling accessibilityScroll: with UIAccessibilityScrollDirectionNext and will 
		 stop scrolling when it detects the content has not changed.
		 */
		CAUSES_PAGE_TURN
	}

	public boolean isAccessibilityElement();
	public void setIsAccessibilityElement(boolean isAccessibilityElement);

	public String getAccessibilityLabel();
	public void setAccessibilityLabel(String accessibilityLabel);


	public String getAccessibilityHint();
	public void setAccessibilityHint(String accessibilityHint);

	public String getAccessibilityValue();
	public void setAccessibilityValue(String accessibilityValue);

	public Trait[] getAccessibilityTraits();
	public void setAccessibilityTraits(Trait... accessibilityTraits);

	public Rect getAccessibilityFrame();
	public void setAccessibilityFrame(Rect accessibilityFrame);

	public Point getAccessibilityActivationPoint();
	public void setAccessibilityActivationPoint(Point accessibilityActivationPoint);

	public boolean getAccessibilityElementsHidden();
	public void setAccessibilityElementsHidden(boolean accessibilityElementsHidden);

	public boolean getAccessibilityViewIsModal();
	public void setAccessibilityViewIsModal(boolean accessibilityViewIsModal);

	public boolean shouldGroupAccessibilityChildren();
	public void setShouldGroupAccessibilityChildren(boolean shouldGroupAccessibilityChildren);

}
