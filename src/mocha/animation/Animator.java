/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */

package mocha.animation;

import java.util.Collection;
import java.util.HashMap;

public abstract class Animator extends mocha.foundation.Object {

	private HashMap<Listener, AnimatorListener> listeners;

	public interface Listener {
		public void onAnimationStart(Animator animator);
		public void onAnimationEnd(Animator animator);
		public void onAnimationCancel(Animator animator);
		public void onAnimationRepeat(Animator animator);
	}

	public void start() {
		this.getSystemAnimator().start();
	}

	public void cancel() {
		this.getSystemAnimator().cancel();
	}

	abstract public Animator setTimingFunction(TimingFunction timingFunction);
	abstract public TimingFunction getTimingFunction();

	abstract protected android.animation.Animator getSystemAnimator();

	public Animator setDuration(long duration) {
		this.getSystemAnimator().setDuration(duration);
		return this;
	}

	public long getDuration() {
		return this.getSystemAnimator().getDuration();
	}

	public Animator setStartDelay(long delay) {
		this.getSystemAnimator().setStartDelay(delay);
		return this;
	}

	public long getStartDelay() {
		return this.getSystemAnimator().getStartDelay();
	}

	public boolean isRunning() {
		return this.getSystemAnimator().isRunning();
	}

	public boolean isStarted() {
		return this.getSystemAnimator().isStarted();
	}

	public void addListener(Listener listener) {
		if(listener == null) return;

		if(this.listeners == null) {
			this.listeners = new HashMap<Listener, AnimatorListener>();
		}

		AnimatorListener animatorListener = new AnimatorListener(listener);
		this.listeners.put(listener, animatorListener);
		this.getSystemAnimator().addListener(animatorListener);
	}

	public void removeListener(Listener listener) {
		if(this.listeners != null) {
			AnimatorListener animatorListener = this.listeners.get(listener);

			if(animatorListener != null) {
				this.listeners.remove(listener);
				this.getSystemAnimator().removeListener(animatorListener);
			}
		}
	}

	public void removeAllListeners() {
		this.getSystemAnimator().removeAllListeners();
		this.listeners.clear();
	}

	public Collection<Listener> getAllListeners() {
		return this.listeners.keySet();
	}

	private class AnimatorListener implements android.animation.Animator.AnimatorListener {
		private Listener listener;

		public AnimatorListener(Listener listener) {
			this.listener = listener;
		}

		public void onAnimationStart(android.animation.Animator animator) {
			listener.onAnimationStart(Animator.this);
		}

		public void onAnimationEnd(android.animation.Animator animator) {
			listener.onAnimationEnd(Animator.this);
		}

		public void onAnimationCancel(android.animation.Animator animator) {
			listener.onAnimationCancel(Animator.this);
		}

		public void onAnimationRepeat(android.animation.Animator animator) {
			listener.onAnimationRepeat(Animator.this);
		}
	}

}
