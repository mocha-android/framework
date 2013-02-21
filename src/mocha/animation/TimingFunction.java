/*
 *  @author Shaun
 *  @date 11/17/12
 *  @copyright	2012 enormego. All rights reserved.
 */

package mocha.animation;

abstract public class TimingFunction {

	abstract public float interpolate(float time, long duration, float start, float end);

	public static final TimingFunction LINEAR = new TimingFunction() {
		public float interpolate(float time, long duration, float start, float end) {
			return time * end + (1.0f - time) * start;
		}
	};

	public static final TimingFunction DEFAULT = new CubicBezierCurveTimingFunction(0.25f, 0.1f, 0.25f, 0.1f);
	public static final TimingFunction EASE_IN = new CubicBezierCurveTimingFunction(0.42f, 0.0f, 1.0f, 1.0f);
	public static final TimingFunction EASE_OUT = new CubicBezierCurveTimingFunction(0.0f, 0.0f, 0.58f, 1.0f);
	public static final TimingFunction EASE_IN_OUT = new CubicBezierCurveTimingFunction(0.42f, 0.0f, 0.58f, 1.0f);

	public static class CubicBezierCurveTimingFunction extends TimingFunction {
		private final float c1x;
		private final float c1y;
		private final float c2x;
		private final float c2y;
		private double duration;
		private float time;
		private float ax, bx, cx, ay, by, cy;

		public CubicBezierCurveTimingFunction(float c1x, float c1y, float c2x, float c2y) {
			this.c1x = c1x;
			this.c1y = c2y;
			this.c2x = c2x;
			this.c2y = c2y;
		}

		public float interpolate(float time, long duration, float start, float end) {
			this.duration = time / 1000.0;
			this.time = time;
			return LINEAR.interpolate(calculate(), duration, start, end);
		}


		private float calculate() {
			// Calculate the polynomial coefficients, implicit first and last control points are (0,0) and (1,1).
			cx = 3.0f * c1x;
			bx = 3.0f * (c2x - c1x) - cx;
			ax = 1.0f - cx - bx;
			cy = 3.0f * c1y;
			by = 3.0f * (c2y - c1y) - cy;
			ay = 1.0f - cy - by;

			// Convert from input time to parametric value in curve, then from that to output time.
			return solve(time, solveEpsilon(duration));
		}

		// `ax time^3 + bx time^2 + cx time' expanded using Horner's rule.
		private float sampleCurveX(float t) {
			return ((ax * t + bx) * t + cx) * t;
		}

		private float sampleCurveY(float t) {
			return ((ay * t + by) * t + cy) * t;
		}

		private float sampleCurveDerivativeX(float t) {
			return (3.0f * ax * t + 2.0f * bx) * t + cx;
		}

		// The epsilon value to pass given that the animation is going to run over |duration| seconds. The longer the animation, the more precision is needed in the timing function result to avoid ugly discontinuities.
		private double solveEpsilon(double duration) {
			return 1.0 / (200.0 * duration);
		}

		private float solve(float x, double epsilon) {
			return sampleCurveY(solveCurveX(x, epsilon));
		}

		// Given an x value, find a parametric value it came from.
		float solveCurveX(float x, double epsilon) {
			float t0,
					t1,
					t2 = x,
					x2,
					d2,
					i = 0;

			// First try a few iterations of Newton's method -- normally very fast.
			for (; i < 8; i++) {
				x2 = sampleCurveX(t2) - x;

				if (Math.abs(x2) < epsilon)
					return t2;

				d2 = sampleCurveDerivativeX(t2);

				if (Math.abs(d2) < 1e-6)
					break;

				t2 = t2 - x2 / d2;
			}

			// Fall back to the bisection method for reliability.
			t0 = 0.0f;
			t1 = 1.0f;
			t2 = x;

			if (t2 < t0)
				return t0;

			if (t2 > t1)
				return t1;

			while (t0 < t1) {
				x2 = sampleCurveX(t2);

				if (Math.abs(x2 - x) < epsilon)
					return t2;

				if (x > x2)
					t0 = t2;

				else
					t1 = t2;

				t2 = (t1 - t0) * 0.5f + t0;
			}

			return t2; // Failure.
		}
	}
}