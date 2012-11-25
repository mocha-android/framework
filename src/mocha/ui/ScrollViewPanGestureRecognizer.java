/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Point;

import java.util.List;

class ScrollViewPanGestureRecognizer extends PanGestureRecognizer {

	// TODO: Add support for nested scroll views

	ScrollViewPanGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);
	}

	protected void touchesBegan(List<Touch> touches, Event event) {
		ScrollView scrollView;

		if(!this.tracking && !this.panning && (scrollView = this.getScrollView()) != null && scrollView.decelerating) {
			Touch touch = this.findTouch(event);

			if(touch != null) {
				this.startTouchPosition = touch.location.copy();
				this.tracking = true;
				this.lastTouchPosition = touch.location.copy();
				this.addTrackingDataPoint(this.lastTouchPosition, event);

				Point location = touch.location;
				this.translation = new Point(location.x - this.startTouchPosition.x, location.y - this.startTouchPosition.y);
				this.panning = true;
				this.setState(State.BEGAN);
			}
		}

		super.touchesBegan(touches, event);
	}

	private ScrollView getScrollView() {
		if(this.getView() instanceof ScrollView) {
			return (ScrollView) this.getView();
		} else {
			return null;
		}
	}

}