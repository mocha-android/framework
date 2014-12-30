/*
 *  @author Shaun
 *  @date 11/17/12
 *  @copyright	2012 Mocha. All rights reserved.
 */

package mocha.animation;

abstract public class TimingFunction {

	public float interpolate(float time, long duration, float start, float end) {
		return this.interpolate(this.solve(time, duration / 1000.0f), start, end);
	}

	abstract public float interpolate(float frame, float start, float end);
	abstract public float solve(float time, float durationInSeconds);

	public static final TimingFunction LINEAR = new TimingFunction() {
		public float interpolate(float frame, float start, float end) {
			return frame * end + (1.0f - frame) * start;
		}

		public float solve(float time, float durationInSeconds) {
			return time;
		}
	};

	public static final TimingFunction DEFAULT = new CubicBezierCurveTimingFunction(0.25f, 0.1f, 0.25f, 0.1f);
	public static final TimingFunction EASE_IN = new CubicBezierCurveTimingFunction(0.42f, 0.0f, 1.0f, 1.0f);
	public static final TimingFunction EASE_OUT = new CubicBezierCurveTimingFunction(0.0f, 0.0f, 0.58f, 1.0f);
	public static final TimingFunction EASE_IN_OUT = new CubicBezierCurveTimingFunction(0.42f, 0.0f, 0.58f, 1.0f);

	public static class CubicBezierCurveTimingFunction extends TimingFunction {
		private UnitBezier unitBezier;

		public CubicBezierCurveTimingFunction(float x1, float y1, float x2, float y2) {
			this.unitBezier = new UnitBezier(x1, y1, x2, y2);
		}

		public float interpolate(float frame, float start, float end) {
			return (start + frame * (end - start));
		}

		public float solve(float time, float durationInSeconds) {
			return this.unitBezier.solve(time, solveEpsilon(durationInSeconds));
		}

		private float solveEpsilon(float duration) {
			return 1.0f / (200.0f * duration);
		}
	}

}