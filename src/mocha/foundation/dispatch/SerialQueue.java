/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.foundation.dispatch;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class SerialQueue extends mocha.foundation.Object implements Queue {
	private String label;
	private Thread thread;
	private int threadPriority;
	private Handler handler;
	private SerialQueue targetQueue;
	private Semaphore lock = new Semaphore(1);
	private boolean global;
	private static HashMap<Priority,SerialQueue> globalQueues = new HashMap<Priority, SerialQueue>();

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
			globalQueue = new SerialQueue("mocha.foundation.global." + priority);
			globalQueue.global = true;

			switch (priority) {
				case HIGH:
					globalQueue.threadPriority = Thread.MAX_PRIORITY;
					break;
				case LOW:
					globalQueue.threadPriority = Thread.MIN_PRIORITY;
					break;
				case DEFAULT:
				default:
					globalQueue.threadPriority = Thread.NORM_PRIORITY;
					break;
			}

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
		this.label = label;
		this.threadPriority = Thread.NORM_PRIORITY;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param targetQueue target queue for this queue
	 */
	public SerialQueue(String label, SerialQueue targetQueue) {
		this(label);
		this.setTargetQueue(targetQueue);
	}

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
	public synchronized void setTargetQueue(SerialQueue queue) {
		if(this.global) return;

		if(this.thread != null) {
			if(this.handler != null) {
				this.handler.getLooper().quit();
				this.handler = null;
			}

			this.thread.stop();
			this.thread = null;
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
			if(this.handler == null) {
				this.getThread();
			}

			this.handler.post(runnable);
		}
	}

	/**
	 * @inheritDoc
	 */
	public void post(long delayInMillis, Runnable runnable) {
		if(this.targetQueue != null) {
			this.targetQueue.post(delayInMillis, runnable);
		} else {
			if(this.handler == null) {
				this.getThread();
			}

			this.handler.postDelayed(runnable, delayInMillis);
		}
	}

	/**
	 * @inheritDoc
	 */
	public void wait(final Runnable runnable) {
		if(this.targetQueue != null) {
			this.targetQueue.wait(runnable);
		} else {
			if(this.handler == null) {
				this.lock.acquireUninterruptibly();
				runnable.run();
				this.lock.release();
			} else if(Looper.myLooper() == this.handler.getLooper()) {
				runnable.run();
			} else {
				this.lock.acquireUninterruptibly();

				this.handler.post(new Runnable() {
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
	 * Shuts down the thread for this queue, if one exists
	 * Has no affect on global queues or queues with a target
	 */
	public void destroy() {
		if(global) return;

		if(this.handler != null) {
			this.handler.getLooper().quit();
			this.thread.stop();
			this.thread = null;
			this.handler = null;
		}
	}

	private synchronized Thread getThread() {
		if(this.thread == null) {
			this.thread = new Thread() {
				public void run() {
					Looper.prepare();
					handler = new Handler();
					Looper.loop();
				}
			};

			this.thread.setPriority(this.threadPriority);
			this.thread.start();

			long start = android.os.SystemClock.uptimeMillis();
			while (this.handler == null) {
				try {
					Thread.sleep(1, 0);

					if(android.os.SystemClock.uptimeMillis() - start > 100) {
						throw new RuntimeException("Unable to create SerialQueue thread, something is very wrong.");
					}
				} catch (InterruptedException ignored) { }
			}
		}

		return this.thread;
	}

}
