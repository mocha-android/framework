/**
 * Works around an annoying issue in ThreadPoolExecutor that doesn't allow
 * unbounded queues to scale from corePoolSize to maximumPoolSize
 *
 * @see https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
 */
package mocha.foundation.concurrent;

import java.util.concurrent.*;

public class ScalingThreadPoolExecutor extends ThreadPoolExecutor {

	public ScalingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new ScalingQueue<Runnable>());
		((ScalingQueue) this.getQueue()).setExecutor(this);
		this.setRejectedExecutionHandler(new ForceQueuePolicy());
	}

	public ScalingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new ScalingQueue<Runnable>(), threadFactory);
		((ScalingQueue) this.getQueue()).setExecutor(this);
		this.setRejectedExecutionHandler(new ForceQueuePolicy());
	}

	private static class ScalingQueue<E> extends LinkedBlockingQueue<E> {
		private ScalingThreadPoolExecutor executor;

		private ScalingQueue() {
		}

		private void setExecutor(ScalingThreadPoolExecutor executor) {
			this.executor = executor;
		}

		public boolean offer(E o) {
			int allWorkingThreads = this.executor.getActiveCount() + super.size();
			return allWorkingThreads < this.executor.getPoolSize() && super.offer(o);
		}

	}

	private static class ForceQueuePolicy implements RejectedExecutionHandler {
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			try {
				executor.getQueue().put(r);
			} catch (InterruptedException e) {
				// Should never happen since we never wait
				throw new RejectedExecutionException(e);
			}
		}
	}

}
