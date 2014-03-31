/**
 *  @author Shaun
 *  @date 3/29/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import mocha.ui.Device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ConcurrentQueue extends ExecutorServiceQueue {
	private static Map<Priority,ConcurrentQueue> globalQueues = new HashMap<Priority, ConcurrentQueue>();

	/**
	 * Get a global queue based on the priority you request
	 * Queue's with higher priorities will have their tasks
	 * executed before jobs with lower priorities.
	 *
	 * @param priority queue priority
	 * @return Global queue for requested priority
	 */
	public static synchronized ConcurrentQueue getGlobalQueue(Priority priority) {
		if(priority == null) priority = Priority.DEFAULT;
		ConcurrentQueue globalQueue = globalQueues.get(priority);

		if(globalQueue == null) {
			globalQueue = new ConcurrentQueue("mocha.foundation.global." + priority);
			globalQueue.global = true;
			globalQueue.priority = priority;
			globalQueues.put(priority, globalQueue);
		}

		return globalQueue;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 */
	public ConcurrentQueue(String label) {
		this(label, Priority.DEFAULT);
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param priority Priority for the queue
	 */
	public ConcurrentQueue(String label, Priority priority) {
		this.label = label;
		this.priority = priority == null ? Priority.DEFAULT : priority;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param targetQueue target queue for this queue
	 */
	public ConcurrentQueue(String label, ConcurrentQueue targetQueue) {
		this(label);
		this.setTargetQueue(targetQueue);
	}

	/**
	 * @inheritDoc
	 */
	public void setTargetQueue(ConcurrentQueue queue) {
		super.setTargetQueue(queue);
	}

	synchronized ExecutorService getExecutorService() {
		if(this.executorService == null) {
			MLog(LogLevel.ERROR, "Procs: " + Runtime.getRuntime().availableProcessors() + ", Cores: " + Device.get().getNumberOfCores());
			this.executorService = QueueExecutors.concurrentQueue(this.priority, this.label, Runtime.getRuntime().availableProcessors() * 2, 60, TimeUnit.SECONDS);
		}

		return this.executorService;
	}
}
