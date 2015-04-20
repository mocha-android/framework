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
