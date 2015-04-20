package mocha.ui;

import mocha.graphics.Point;
import mocha.graphics.Rect;

public interface Accessibility {

	public enum Trait {
		NONE,
		BUTTON,
		LINK,
		HEADER,
		SEARCH_FIELD,
		IMAGE,
		SELECTED,
		PLAYS_SOUND,
		KEYBOARD_KEY,
		STATIC_TEXT,
		SUMMARY_ELEMENT,
		NOT_ENABLED,
		UPDATES_FREQUENTLY,
		STARTS_MEDIA_SESSION,
		ADJUSTABLE,
		ALLOWS_DIRECT_INTERACTION,
		CAUSES_PAGE_TURN
	}

	boolean isAccessibilityElement();

	void setIsAccessibilityElement(boolean isAccessibilityElement);

	String getAccessibilityLabel();

	void setAccessibilityLabel(String accessibilityLabel);


	String getAccessibilityHint();

	void setAccessibilityHint(String accessibilityHint);

	String getAccessibilityValue();

	void setAccessibilityValue(String accessibilityValue);

	Trait[] getAccessibilityTraits();

	void setAccessibilityTraits(Trait... accessibilityTraits);

	Rect getAccessibilityFrame();

	void setAccessibilityFrame(Rect accessibilityFrame);

	Point getAccessibilityActivationPoint();

	void setAccessibilityActivationPoint(Point accessibilityActivationPoint);

	boolean getAccessibilityElementsHidden();

	void setAccessibilityElementsHidden(boolean accessibilityElementsHidden);

	boolean getAccessibilityViewIsModal();

	void setAccessibilityViewIsModal(boolean accessibilityViewIsModal);

	boolean shouldGroupAccessibilityChildren();

	void setShouldGroupAccessibilityChildren(boolean shouldGroupAccessibilityChildren);

}
