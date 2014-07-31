/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.concurrent;

import java.util.HashMap;
import java.util.Map;

public class ConcurrentQueue extends BackgroundQueue {
	private static Map<Priority,ConcurrentQueue> globalQueues = new HashMap<>();

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
			globalQueue = new ConcurrentQueue("mocha.foundation.global." + priority, priority);
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
		super(createThreadPoolExecutor(label, priority), label);
	}

}
