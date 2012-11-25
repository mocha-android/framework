/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import mocha.graphics.Point;

class PointEvaluator extends NumericEvaluator<Point, Float> {

	public Point evaluate(float time, Point start, Point end) {
		Point point = new Point();
		point.x = this.interpolate(time, start.x, end.x);
		point.y = this.interpolate(time, start.y, end.y);
		return point;
	}

}
