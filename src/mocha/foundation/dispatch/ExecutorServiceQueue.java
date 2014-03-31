/**
 *  @author Shaun
 *  @date 3/29/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import java.util.concurrent.*;

abstract class ExecutorServiceQueue extends Queue {
	protected String label;
	protected ExecutorService executorService;
	protected Priority priority;
	protected Queue targetQueue;
	protected boolean global;
	private Semaphore lock = new Semaphore(1);
	private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
	private static Runnable scheduledThreadPoolExecutorChecker;

	/**
	 * Queue's with a target queue will delegate their tasks
	 * to the target queue's thread, rather than create and managing
	 * their own thread.
	 *
	 * Unless there is a specific need for ensuring your queue has a
	 * dedicated thread, it's recommended you set a global queue as
	 * it's target queue to avoid unnecessary extra threads.
	 *
	 * Creating a cyclical dependency by setting a target queue who's
	 * target queue is that queue will result in a deadlock.
	 *
	 * @param queue target queue
	 */
	protected synchronized void setTargetQueue(Queue queue) {
		if(this.global) return;

		if(this.executorService != null) {
			this.executorService.shutdownNow();
			this.executorService = null;
		}

		this.targetQueue = queue;
	}

	/**
	 * @inheritDoc
	 */
	public void post(Runnable runnable) {
		if(this.targetQueue != null) {
			this.targetQueue.post(runnable);
		} else {
			this.getExecutorService().submit(runnable);
		}
	}

	/**
	 * @inheritDoc
	 */
	public void post(long delayInMillis, final Runnable runnable) {
		if(this.targetQueue != null) {
			this.targetQueue.post(delayInMillis, runnable);
		} else {
			if(scheduledThreadPoolExecutor == null) {
				scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
			}

			scheduledThreadPoolExecutor.schedule(new Runnable() {
				public void run() {
					post(runnable);
				}
			}, delayInMillis, TimeUnit.MILLISECONDS);

			if(scheduledThreadPoolExecutorChecker == null) {
				scheduledThreadPoolExecutorChecker = new Runnable() {
					public void run() {
						checkScheduledThreadPool();
					}
				};
			}

			scheduledThreadPoolExecutor.schedule(scheduledThreadPoolExecutorChecker, delayInMillis + 60000, TimeUnit.MILLISECONDS);
		}
	}

	private static void checkScheduledThreadPool() {
		if(scheduledThreadPoolExecutor == null) return;

		if(scheduledThreadPoolExecutor.getActiveCount() == 1) {
			scheduledThreadPoolExecutor.shutdown();
			scheduledThreadPoolExecutor = null;
			scheduledThreadPoolExecutorChecker = null;
		}
	}

	/**
	 * @inheritDoc
	 */
	public void wait(final Runnable runnable) {
		if(this.targetQueue != null) {
			this.targetQueue.wait(runnable);
		} else {
			if(QueueExecutors.isThreadOwnedByExecutorService(Thread.currentThread(), this.getExecutorService())) {
				runnable.run();
			} else {
				this.lock.acquireUninterruptibly();

				this.getExecutorService().execute(new Runnable() {
					public void run() {
						runnable.run();
						lock.release();
					}
				});

				this.lock.acquireUninterruptibly();
				this.lock.release();
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * @return Executor service for this Queue
	 */
	abstract ExecutorService getExecutorService();

	/**
	 * Shuts down the thread for this queue, if one exists
	 * Has no affect on global queues or queues with a target
	 */
	public void destroy() {
		if(this.global) return;

		if(this.executorService != null) {
			this.executorService.shutdownNow();
			this.executorService = null;
		}
	}

}
