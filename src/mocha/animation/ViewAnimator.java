/*
 *  @author Shaun
 *	@date 11/17/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.ui.View;

public class ViewAnimator<O, E extends Number> extends mocha.animation.Animator {

	public static ViewAnimator ofBounds(View view, Rect... values) {
		return ofRect(view, "bounds", values);
	}

	public static ViewAnimator ofFrame(View view, Rect... values) {
		return ofRect(view, "frame", values);
	}

	public static ViewAnimator ofAlpha(View view, Float... values) {
		return ofFloat(view, "alpha", values);
	}

	public static ViewAnimator ofBackgroundColor(View view, Integer... values) {
		return ofInteger(view, "backgroundColor", values);
	}

	public static ViewAnimator ofRect(View view, String property, Rect... values) {
		return new ViewAnimator<Rect, Float>(view, property, new RectEvaluator(), Rect.class, values);
	}

	public static ViewAnimator ofPoint(View view, String property, Point... values) {
		return new ViewAnimator<Point, Float>(view, property, new PointEvaluator(), Point.class, values);
	}

	public static ViewAnimator ofFloat(View view, String property, Float... values) {
		return new ViewAnimator<Float, Float>(view, property, new FloatEvaluator(), Float.class, values);
	}

	public static ViewAnimator ofInteger(View view, String property, Integer... values) {
		return new ViewAnimator<Integer, Integer>(view, property, new IntegerEvaluator(), Integer.class, values);
	}

	private View view;
	private ObjectAnimator objectAnimator;
	private NumericEvaluator<O, E> numericEvaluator;
	private String property;

	private ViewAnimator() { }

	private ViewAnimator(View view, String property, NumericEvaluator<O, E> numericEvaluator, Class<O> propertyClass, O... values) {
		this.objectAnimator = ObjectAnimator.ofObject(view, property, numericEvaluator, values);
		this.numericEvaluator = numericEvaluator;
		this.numericEvaluator.setDuration(this.objectAnimator.getDuration());
		this.property = property;
		this.view = view;
	}

	public ViewAnimator<O, E> setTimingFunction(TimingFunction timingFunction) {
		this.numericEvaluator.setTimingFunction(timingFunction);
		return this;
	}

	public TimingFunction getTimingFunction() {
		return this.numericEvaluator.getTimingFunction();
	}

	public ViewAnimator<O, E> setView(View view) {
		this.view = view;
		this.objectAnimator.setTarget(view);
		return this;
	}

	public View getView() {
		return this.view;
	}

	public String getProperty() {
		return property;
	}

	public Animator getSystemAnimator() {
		return this.objectAnimator;
	}

}