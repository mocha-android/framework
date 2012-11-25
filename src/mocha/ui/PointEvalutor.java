/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.animation.TypeEvaluator;
import mocha.graphics.Point;

public class PointEvalutor extends mocha.foundation.Object implements TypeEvaluator <Point> {

	public Point evaluate(float fraction, Point start, Point end) {
		Point current = new Point();
		current.x = start.x + (fraction * (end.x - start.x));
		current.y = start.y + (fraction * (end.y - start.y));
		return current;
	}

}
