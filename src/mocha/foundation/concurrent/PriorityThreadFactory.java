/**
 *  @author Shaun
 *  @date 7/31/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory implements ThreadFactory {
	private final String label;
	private final int threadPriority;
	private final AtomicInteger count = new AtomicInteger(1);

	public PriorityThreadFactory(String label, Priority priority) {
		this.label = label;

		switch (priority) {
			case HIGH:
				this.threadPriority = Thread.MAX_PRIORITY;
				break;
			case LOW:
				this.threadPriority = Thread.MIN_PRIORITY;
				break;
			case DEFAULT:
			default:
				this.threadPriority = Thread.NORM_PRIORITY;
				break;
		}
	}

	@SuppressWarnings("NullableProblems")
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r, this.label + " #" + this.count.getAndIncrement());

		if (thread.getPriority() != this.threadPriority) {
			thread.setPriority(this.threadPriority);
		}

		return thread;
	}
}
