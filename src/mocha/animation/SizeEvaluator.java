/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import mocha.graphics.Size;

class SizeEvaluator extends NumericEvaluator <Size, Float> {

	public Size evaluate(float time, Size start, Size end) {
		Size size = start.copy();

		if(start.width != end.width) {
			size.width = this.interpolate(time, start.width, end.width);
		}

		if(start.height != end.height) {
			size.height = this.interpolate(time, start.height, end.height);
		}

		return size;
	}

}
