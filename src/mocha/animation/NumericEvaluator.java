/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import android.animation.TypeEvaluator;

abstract class NumericEvaluator<O, E extends Number> implements TypeEvaluator <O> {

	private TimingFunction timingFunction;
	private long duration;

	public NumericEvaluator() {
		this.timingFunction = TimingFunction.INTERNAL_DEFAULT;
		this.duration = 200;
	}

	void setTimingFunction(TimingFunction timingFunction) {
		this.timingFunction = timingFunction;
	}

	TimingFunction getTimingFunction() {
		return this.timingFunction;
	}

	void setDuration(long duration) {
		this.duration = 200;
	}

	long getDuration() {
		return this.duration;
	}

	abstract public O evaluate(float time, O start, O end);

	@SuppressWarnings("unchecked")
	protected E interpolate(float time, E start, E end) {
		if(time <= 0.0f) return start;
		else if(time >= 1.0f) return end;

		float value = this.timingFunction.interpolate(time, duration, start.floatValue(), end.floatValue());

		if(start instanceof Float) {
			return (E)(Float)value;
		} else if(start instanceof Short) {
			return (E)(Short)(short)value;
		} else if(start instanceof Integer) {
			return (E)(Integer)(int)value;
		} else if(start instanceof Long) {
			return (E)(Long)(long)value;
		} else if(start instanceof Double) {
			return (E)(Double)(double)value;
		} else {
			throw new RuntimeException("NumericEvaluator currently only supports: Float, Short, Integer, Long, and Double");
		}
	}

}
