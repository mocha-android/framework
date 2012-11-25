/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import mocha.graphics.Rect;

class RectEvaluator extends NumericEvaluator <Rect, Float> {

	public Rect evaluate(float time, Rect start, Rect end) {
		Rect rect = new Rect();
		rect.origin.x = this.interpolate(time, start.origin.x, end.origin.x);
		rect.origin.y = this.interpolate(time, start.origin.y, end.origin.y);
		rect.size.width = this.interpolate(time, start.size.width, end.size.width);
		rect.size.height = this.interpolate(time, start.size.height, end.size.height);
		return rect;
	}

}
