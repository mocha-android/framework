/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.concurrent;

public abstract class ScheduledQueue extends Queue {

	/**
	 * Post a task to be executed on this queue after a delay
	 *
	 * @param delayInMillis time to wait before executing
	 * @param runnable task to be executed
	 */
	abstract public void post(long delayInMillis, Runnable runnable);

}
