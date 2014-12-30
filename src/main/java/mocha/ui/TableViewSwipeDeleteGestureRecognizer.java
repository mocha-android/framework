/**
 *  @author Shaun
 *  @date 6/6/13
 *  @copyright 2013 TV Guide, Inc. All rights reserved.
 */
package mocha.ui;

class TableViewSwipeDeleteGestureRecognizer extends SwipeGestureRecognizer {

	public TableViewSwipeDeleteGestureRecognizer(GestureHandler gestureHandler) {
		super(gestureHandler);
		super.setDirection(Direction.LEFT, Direction.RIGHT);
	}

	public void setDirection(Direction direction) {
		// Block
	}

}
