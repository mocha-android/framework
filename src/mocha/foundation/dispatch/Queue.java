/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 Mocha. All rights reserved.
 */

package mocha.foundation.dispatch;

import mocha.foundation.MObject;

public abstract class Queue extends MObject {

	public enum Priority {
		DEFAULT, HIGH, LOW
	}

	/**
	 * Get the main queue
	 * @see mocha.foundation.dispatch.MainQueue#get()
	 *
	 * @return Main queue
	 */
	public static Queue main() {
		return MainQueue.get();
	}

	/**
	 * Get a global serial queue
	 * @see SerialQueue#getGlobalQueue(mocha.foundation.dispatch.Queue.Priority)
	 *
	 * @param priority queue priority
	 * @return Global serial queue
	 */
	public static Queue serial(Priority priority) {
		return SerialQueue.getGlobalQueue(priority);
	}

	/**
	 * Get a global concurrent queue
	 * @see ConcurrentQueue#getGlobalQueue(mocha.foundation.dispatch.Queue.Priority)
	 *
	 * @param priority queue priority
	 * @return Global serial queue
	 */
	public static Queue concurrent(Priority priority) {
		return ConcurrentQueue.getGlobalQueue(priority);
	}

	/**
	 * Post a task to be executed on this queue
	 *
	 * @param runnable task to be executed
	 */
	abstract public void post(Runnable runnable);

	/**
	 * Post a task to be executed on this queue after a delay
	 *
	 * @param delayInMillis time to wait before executing
	 * @param runnable task to be executed
	 */
	abstract public void post(long delayInMillis, Runnable runnable);

	/**
	 * Run a task on this queue and wait to return until it
	 * finishes.
	 *
	 * @param runnable task to be executed
	 */
	abstract public void wait(Runnable runnable);

	/**
	 * Get the label of this queue
	 *
	 * @return queue label
	 */
	abstract public String getLabel();
}
