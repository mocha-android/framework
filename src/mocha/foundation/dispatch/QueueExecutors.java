/**
 *  @author Shaun
 *  @date 3/18/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import mocha.foundation.MObject;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class QueueExecutors extends MObject {

	@SuppressWarnings("unchecked")
	public static ExecutorService serialQueue(Queue.Priority priority, String label, long keepAliveTime, TimeUnit unit) {
		return concurrentQueue(priority, label, 1, keepAliveTime, unit);
	}

	@SuppressWarnings("unchecked")
	public static ExecutorService concurrentQueue(Queue.Priority priority, String label, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		ExecutorThreadFactory threadFactory = new ExecutorThreadFactory(priority, label);
		ThreadPoolExecutor threadPoolExecutor = new ScalingThreadPoolExecutor(0, maximumPoolSize, keepAliveTime, unit, threadFactory);
		threadFactory.setExecutorService(threadPoolExecutor);
		return threadPoolExecutor;
	}

	static boolean isThreadOwnedByExecutorService(Thread thread, ExecutorService executorService) {
		return thread instanceof ExecutorThreadFactory.Thread && ((ExecutorThreadFactory.Thread) thread).getExecutorService() == executorService;
	}

	private static class ExecutorThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		private final int threadPriority;
		private java.util.concurrent.ExecutorService executorService;

		ExecutorThreadFactory(Queue.Priority threadPriority, String threadLabel) {
			SecurityManager securityManager = System.getSecurityManager();
			this.group = (securityManager != null)? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			this.namePrefix = threadLabel + "-thread-";

			switch (threadPriority) {
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

		void setExecutorService(java.util.concurrent.ExecutorService executorService) {
			this.executorService = executorService;
		}

		class Thread extends java.lang.Thread {
			Thread(ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
				super(group, runnable, threadName, stackSize);
			}

			public java.util.concurrent.ExecutorService getExecutorService() {
				return ExecutorThreadFactory.this.executorService;
			}
		}

		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0);

			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}

			if (thread.getPriority() != this.threadPriority) {
				thread.setPriority(this.threadPriority);
			}

			return thread;
		}
	}


}
