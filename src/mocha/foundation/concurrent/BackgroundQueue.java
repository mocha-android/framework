/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.concurrent;

import java.util.concurrent.*;

abstract class BackgroundQueue extends Queue {
	private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
	private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
	private static final int KEEP_ALIVE = 1;

	private final Executor executor;
	private final String label;

	static ThreadPoolExecutor createConcurrentThreadPoolExecutor(String label, Priority priority, boolean global) {
		return new ScalingThreadPoolExecutor(global ? CORE_POOL_SIZE : 0, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new PriorityThreadFactory(label, priority));
	}

	static ThreadPoolExecutor createSerialThreadPoolExecutor(String label, Priority priority, boolean global) {
		return new ScalingThreadPoolExecutor(global ? 1 : 0, 1, KEEP_ALIVE, TimeUnit.SECONDS, new PriorityThreadFactory(label, priority));
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
