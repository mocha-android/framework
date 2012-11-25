/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.app.Application;
import mocha.graphics.Rect;

import java.util.*;

public class Control extends View {

	public enum ControlEvent {
		TOUCH_DOWN,      // on all touch downs
		TOUCH_DOWN_REPEAT,      // on multiple touchdowns (tap count > 1)
		TOUCH_DRAG_INSIDE,
		TOUCH_DRAG_OUTSIDE,
		TOUCH_DRAG_ENTER,
		TOUCH_DRAG_EXIT,
		TOUCH_UP_INSIDE,
		TOUCH_UP_OUTSIDE,
		TOUCH_CANCEL,

		VALUE_CHANGED,     // sliders, etc.

		EDITING_DID_BEGIN,     // Text field
		EDITING_CHANGED,
		EDITING_DID_END,
		EDITING_DID_END_ON_EXIT,     // 'return key' ending editing

		ALL_TOUCH_EVENTS,  // for touch events
		ALL_EDITING_EVENTS,  // for Text field
		APPLICATION_RESERVED,  // range available for application use
		SYSTEM_RESERVED,  // range reserved for internal framework use
		ALL_EVENTS
	}

	public enum VerticalAlignment {
		CENTER,
		TOP,
		BOTTOM,
		FILL
	}

	public enum HorizontalAlignment {
		CENTER,
		LEFT,
		RIGHT,
		FILL
	}

	public enum State {
		NORMAL,
		HIGHLIGHTED, // used when isHighlighted is set
		DISABLED,
		SELECTED, // flag usable by app (see below)
		APPLICATION, // additional flags available for application use
		RESERVED // flags reserved for internal framework use
	}

	public interface ActionTarget {
		public void onControlEvent(Control control, ControlEvent controlEvent);
	}

	private boolean enabled;
	private boolean selected;
	private boolean highlighted;
	private boolean tracking;
	private boolean touchInside;
	private HorizontalAlignment contentHorizontalAlignment;
	private VerticalAlignment contentVerticalAlignment;
	private HashMap<ActionTarget,EnumSet<ControlEvent>> registeredActions;

	public Control() { this(Rect.zero()); }

	public Control(Rect frame) {
		super(frame);

		this.registeredActions = new HashMap<ActionTarget, EnumSet<ControlEvent>>();
		this.enabled = true;
		this.contentHorizontalAlignment = HorizontalAlignment.CENTER;
		this.contentVerticalAlignment = VerticalAlignment.CENTER;
	}

	public void addActionTarget(ActionTarget actionTarget, ControlEvent... controlEvents) {
		EnumSet<ControlEvent> registeredEvents;

		if((registeredEvents = this.registeredActions.get(actionTarget)) == null) {
			registeredEvents = EnumSet.noneOf(ControlEvent.class);
		}

		Collections.addAll(registeredEvents, controlEvents);

		this.registeredActions.put(actionTarget, registeredEvents);
	}

	public void removeActionTarget(ActionTarget actionTarget, ControlEvent... controlEvents) {
		EnumSet<ControlEvent> registeredEvents = this.registeredActions.get(actionTarget);
		if(registeredEvents == null) return;

		for(ControlEvent controlEvent : controlEvents) {
			registeredEvents.remove(controlEvent);
		}

		if(registeredEvents.size() == 0) {
			this.registeredActions.remove(actionTarget);
		}
	}

	public ControlEvent[] allControlEvents() {
		EnumSet<ControlEvent> registeredEvents = EnumSet.noneOf(ControlEvent.class);

		for(EnumSet<ControlEvent> controlEvents : this.registeredActions.values()) {
			registeredEvents.addAll(controlEvents);
		}

		return registeredEvents.toArray(new ControlEvent[registeredEvents.size()]);
	}

	public void sendActionsForControlEvents(ControlEvent... controlEvents) {
		for(ActionTarget actionTarget : this.registeredActions.keySet()) {
			for(ControlEvent controlEvent : controlEvents) {
				if(this.registeredActions.get(actionTarget).contains(controlEvent)) {
					actionTarget.onControlEvent(this, controlEvent);
				}
			}
		}
	}

	protected boolean beginTracking(Touch touch, Event event) {
		return true;
	}

	protected boolean continueTracking(Touch touch, Event event) {
		return true;
	}

	protected void endTracking(Touch touch, Event event) {

	}

	protected void cancelTracking(Event event) {

	}

	public void touchesBegan(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		this.touchInside = tracking;
		this.tracking = this.beginTracking(touch, event);
		this.setHighlighted(true);

		if(this.tracking) {

			ControlEvent[] controlEvents;

			if(touch.getTapCount() > 1) {
				controlEvents = new ControlEvent[] { ControlEvent.TOUCH_DOWN, ControlEvent.TOUCH_DOWN_REPEAT };
			} else {
				controlEvents = new ControlEvent[] { ControlEvent.TOUCH_DOWN };
			}

			this.sendActionsForControlEvents(controlEvents);
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		boolean wasTouchInside = this.touchInside;
		this.touchInside = this.pointInside(touch.locationInView(this), event);

		if(this.highlighted != this.touchInside) {
			this.setHighlighted(this.touchInside);
		}

		if(this.tracking) {
			this.tracking = this.continueTracking(touch, event);

			if(this.tracking) {
				ControlEvent dragEvent = this.touchInside ? ControlEvent.TOUCH_DRAG_INSIDE : ControlEvent.TOUCH_DRAG_OUTSIDE;
				ControlEvent[] controlEvents;

				if (!wasTouchInside && this.touchInside) {
					controlEvents = new ControlEvent[] { dragEvent, ControlEvent.TOUCH_DRAG_ENTER };
				} else if (wasTouchInside && !this.touchInside) {
					controlEvents = new ControlEvent[] { dragEvent, ControlEvent.TOUCH_DRAG_EXIT };
				} else {
					controlEvents = new ControlEvent[] { dragEvent };
				}

				this.sendActionsForControlEvents(controlEvents);
			}
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		Touch touch = touches.get(0);
		this.touchInside = this.pointInside(touch.locationInView(this), event);

		if(this.highlighted) {
			this.setHighlighted(false);
		}

		if(this.tracking) {
			this.endTracking(touch, event);
			this.sendActionsForControlEvents(this.touchInside ? ControlEvent.TOUCH_UP_INSIDE : ControlEvent.TOUCH_UP_OUTSIDE);
		}

		this.tracking = false;
		this.touchInside = false;
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		if(this.highlighted) {
			this.setHighlighted(false);
		}

		if(this.tracking) {
			this.cancelTracking(event);
			this.sendActionsForControlEvents(ControlEvent.TOUCH_CANCEL);
		}

		this.tracking = false;
		this.touchInside = false;
	}


	public EnumSet<State> getState() {
		EnumSet<State> state = EnumSet.of(State.NORMAL);

		if(this.highlighted) {
			state.add(State.HIGHLIGHTED);
		}

		if(this.selected) {
			state.add(State.SELECTED);
		}

		if(!this.enabled) {
			state.add(State.DISABLED);
		}

		return state;
	}

	protected void stateWillChange() {

	}

	protected void  stateDidChange() {
		this.setNeedsDisplay();
		this.setNeedsLayout();
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		if(this.enabled != enabled) {
			this.stateWillChange();
			this.enabled = enabled;
			this.setUserInteractionEnabled(this.enabled);
			this.stateDidChange();
		}
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		if(this.selected != selected) {
			this.stateWillChange();
			this.selected = selected;
			this.stateDidChange();
		}
	}

	public boolean isHighlighted() {
		return this.highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		if(this.highlighted != highlighted) {
			this.stateWillChange();
			this.highlighted = highlighted;
			this.stateDidChange();
		}
	}

	public boolean isTracking() {
		return tracking;
	}

	public boolean isTouchInside() {
		return touchInside;
	}

	public HorizontalAlignment getContentHorizontalAlignment() {
		return contentHorizontalAlignment;
	}

	public void setContentHorizontalAlignment(HorizontalAlignment contentHorizontalAlignment) {
		this.contentHorizontalAlignment = contentHorizontalAlignment;
	}

	public VerticalAlignment getContentVerticalAlignment() {
		return contentVerticalAlignment;
	}

	public void setContentVerticalAlignment(VerticalAlignment contentVerticalAlignment) {
		this.contentVerticalAlignment = contentVerticalAlignment;
	}
}
