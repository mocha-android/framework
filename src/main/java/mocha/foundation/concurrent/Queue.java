package mocha.foundation.concurrent;

import mocha.foundation.MObject;

public abstract class Queue extends MObject {
	private static ExceptionHandler defaultExceptionHandler;
	private ExceptionHandler exceptionHandler;

	protected Queue() {
		this.exceptionHandler = defaultExceptionHandler;
	}

	/**
	 * Get the main queue
	 *
	 * @return Main queue
	 *
	 * @see mocha.foundation.concurrent.MainQueue#get()
	 */
	public static ScheduledQueue main() {
		return MainQueue.get();
	}

	/**
	 * Get a global serial queue
	 *
	 * @param priority queue priority
	 *
	 * @return Global serial queue
	 *
	 * @see SerialQueue#getGlobalQueue(Priority)
	 */
	public static Queue serial(Priority priority) {
		return SerialQueue.getGlobalQueue(priority);
	}

	/**
	 * Get a global concurrent queue
	 *
	 * @param priority queue priority
	 *
	 * @return Global serial queue
	 *
	 * @see ConcurrentQueue#getGlobalQueue(Priority)
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
	 *
	 * @param defaultExceptionHandler Exception handler or null
	 */
	public static void setDefaultExceptionHandler(ExceptionHandler defaultExceptionHandler) {
		Queue.defaultExceptionHandler = defaultExceptionHandler;
	}

	/**
	 * Set the exception handler for this queue, overriding the default handler
	 *
	 * @param exceptionHandler Exception handler or null to resume using the default exception handler
	 */
	public final void setExceptionHandler(ExceptionHandler exceptionHandler) {
		if (exceptionHandler == null) {
			this.exceptionHandler = defaultExceptionHandler;
		} else {
			this.exceptionHandler = exceptionHandler;
		}
	}

	/**
	 * Get the exception handler for this queue
	 *
	 * @return Exception handler or null
	 */
	protected final ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

}
