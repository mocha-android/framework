/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

public class SerialQueue extends BackgroundQueue {
	private static Map<Priority,SerialQueue> globalQueues = new HashMap<>();

	/**
	 * Get a global queue based on the priority you request
	 * Queue's with higher priorities will have their tasks
	 * executed before jobs with lower priorities.
	 *
	 * @param priority queue priority
	 * @return Global queue for requested priority
	 */
	public static synchronized SerialQueue getGlobalQueue(Priority priority) {
		if(priority == null) priority = Priority.DEFAULT;
		SerialQueue globalQueue = globalQueues.get(priority);

		if(globalQueue == null) {
			globalQueue = new SerialQueue("mocha.foundation.global." + priority, priority);
			globalQueues.put(priority, globalQueue);
		}

		return globalQueue;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 */
	public SerialQueue(String label) {
		this(label, Priority.DEFAULT);
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param priority Priority for the queue
	 */
	public SerialQueue(final String label, final Priority priority) {
		super(new Executor() {
			final ArrayDeque<Runnable> tasks = new ArrayDeque<Runnable>();
			final ThreadPoolExecutor threadPoolExecutor = createThreadPoolExecutor(label, priority);
			Runnable active;

			@SuppressWarnings("NullableProblems")
			public synchronized void execute(final Runnable runnable) {
				this.tasks.offer(new Runnable() {
					public void run() {
						try {
							runnable.run();
						} finally {
							scheduleNext();
						}
					}
				});

				if (active == null) {
					scheduleNext();
				}
			}

			protected synchronized void scheduleNext() {
				if ((this.active = this.tasks.poll()) != null) {
					this.threadPoolExecutor.execute(this.active);
				}
			}
		}, label);
	}

}
