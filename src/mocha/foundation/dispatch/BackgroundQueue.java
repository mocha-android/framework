/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BackgroundQueue extends Queue {
	private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
	private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
	private static final int KEEP_ALIVE = 1;

	private final Executor executor;
	private final String label;

	private static class BackgroundQueueThreadFactory implements ThreadFactory {
		private final String label;
		private final int threadPriority;
		private final AtomicInteger count = new AtomicInteger(1);

		private BackgroundQueueThreadFactory(String label, Priority priority) {
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

	static ThreadPoolExecutor createThreadPoolExecutor(String label, Priority priority) {
		return new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128), new BackgroundQueueThreadFactory(label, priority));
	}

	protected BackgroundQueue(Executor executor, String label) {
		this.executor = executor;
		this.label = label;
	}

	public void post(final Runnable runnable) {
		this.executor.execute(new Runnable() {
			public void run() {
				try {
					runnable.run();
				} catch(final Throwable t) {
					MWarn(t, "Error executing on queue " + label);

					if(getExceptionHandler() != null) {
						getExceptionHandler().handleException(t, BackgroundQueue.this);
					}
				}
			}
		});
	}

//	public void post(long delayInMillis, Runnable runnable) {
//		throw new UnsupportedOperationException();
//	}

//	public void wait(Runnable runnable) {
//		throw new UnsupportedOperationException();
//	}

	public String getLabel() {
		return this.label;
	}

}
