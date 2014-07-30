/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 Mocha. All rights reserved.
 */

package mocha.foundation.dispatch;

import mocha.foundation.MObject;

import java.util.concurrent.FutureTask;

public abstract class Queue extends MObject {
	private static ExceptionHandler defaultExceptionHandler;
	private ExceptionHandler exceptionHandler;

	public enum Priority {
		DEFAULT, HIGH, LOW
	}

	protected Queue() {
		this.exceptionHandler = defaultExceptionHandler;
	}

	/**
	 * Get the main queue
	 * @see mocha.foundation.dispatch.MainQueue#get()
	 *
	 * @return Main queue
	 */
	public static ScheduledQueue main() {
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
	 * Get the label of this queue
	 *
	 * @return queue label
	 */
	abstract public String getLabel();

	/**
	 * Set the default exception handler to be assigned to any new Queue created.
	 * @param defaultExceptionHandler Exception handler or null
	 */
	public static void setDefaultExceptionHandler(ExceptionHandler defaultExceptionHandler) {
		Queue.defaultExceptionHandler = defaultExceptionHandler;
	}

	/**
	 * Set the exception handler for this queue, overriding the default handler
	 * @param exceptionHandler Exception handler or null to resume using the default exception handler
	 */
	public final void setExceptionHandler(ExceptionHandler exceptionHandler) {
		if(exceptionHandler == null) {
			this.exceptionHandler = defaultExceptionHandler;
		} else {
			this.exceptionHandler = exceptionHandler;
		}
	}

	/**
	 * Get the exception handler for this queue
	 * @return Exception handler or null
	 */
	protected final ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

}
