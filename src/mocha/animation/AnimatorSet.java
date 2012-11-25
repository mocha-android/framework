/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnimatorSet extends Animator {

	public static AnimatorSet withAnimators(Animator... animators) {
		return new AnimatorSet(Arrays.asList(animators));
	}

	public static AnimatorSet withAnimators(List<Animator> animators) {
		return new AnimatorSet(animators);
	}

	private List<Animator> animators;
	private android.animation.AnimatorSet animatorSet;
	private TimingFunction timingFunction;

	private AnimatorSet(List<Animator> animators) {
		this.animators = animators;

		ArrayList<android.animation.Animator> systemAnimators = new ArrayList<android.animation.Animator>();

		for(Animator animator : this.animators) {
			systemAnimators.add(animator.getSystemAnimator());
		}

		this.animatorSet = new android.animation.AnimatorSet();
		this.animatorSet.playTogether(systemAnimators);

		this.setTimingFunction(TimingFunction.INTERNAL_DEFAULT);
		this.setDuration(200);
	}

	public AnimatorSet setTimingFunction(TimingFunction timingFunction) {
		this.timingFunction = timingFunction;

		for(Animator animator : this.animators) {
			animator.setTimingFunction(timingFunction);
		}

		return this;
	}

	public TimingFunction getTimingFunction() {
		return this.timingFunction;
	}

	public AnimatorSet setDuration(long duration) {
		super.setDuration(duration);

		for(Animator animator : this.animators) {
			animator.setDuration(duration);
		}

		return this;
	}

	protected android.animation.Animator getSystemAnimator() {
		return this.animatorSet;
	}

}
