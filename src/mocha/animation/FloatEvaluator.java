/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

class FloatEvaluator extends NumericEvaluator <Float, Float> {

	public Float evaluate(float time, Float start, Float end) {
		return this.interpolate(time, start, end);
	}

}
