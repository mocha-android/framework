/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 enormego. All rights reserved.
 */

package mocha.foundation.dispatch;

public interface Queue {

	public enum Priority {
		DEFAULT, HIGH, LOW
	}

	/**
	 * Post a task to be executed on this queue
	 *
	 * @param runnable task to be executed
	 */
	public void post(Runnable runnable);

	/**
	 * Post a task to be executed on this queue after a delay
	 *
	 * @param delayInMillis time to wait before executing
	 * @param runnable task to be executed
	 */
	public void post(long delayInMillis, Runnable runnable);

	/**
	 * Run a task on this queue and wait to return until it
	 * finishes.
	 *
	 * @param runnable task to be executed
	 */
	public void wait(Runnable runnable);

	/**
	 * Get the label of this queue
	 *
	 * @return queue label
	 */
	public String getLabel();
}
