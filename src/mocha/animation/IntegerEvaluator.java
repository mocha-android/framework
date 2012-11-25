/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

class IntegerEvaluator extends NumericEvaluator <Integer, Integer> {

	public Integer evaluate(float time, Integer start, Integer end) {
		return this.interpolate(time, start, end);
	}

}
