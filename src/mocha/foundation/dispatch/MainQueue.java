/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.foundation.dispatch;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Semaphore;

public class MainQueue implements Queue {
	private Handler handler;
	private static MainQueue instance;

	/**
	 * Get the main queue of this application.
	 * The main queue is the UI thread, so you can run
	 * UI operations in tasks posted to the main queue.
	 *
	 * @return Main queue for this application
	 */
	public synchronized static MainQueue get() {
		if(instance == null) {
			instance = new MainQueue();
		}

		return instance;
	}

	private MainQueue() {
		Looper looper = Looper.getMainLooper();

		if(looper == null) {
			throw new RuntimeException("Could not find main looper.");
		} else {
			this.handler = new Handler(looper);
		}
	}

	/**
	 * @inheritDoc
	 */
	public void post(Runnable runnable) {
		this.handler.post(runnable);
	}

	/**
	 * @inheritDoc
	 */
	public void post(long delayInMillis, Runnable runnable) {
		this.handler.postDelayed(runnable, delayInMillis);
	}

	/**
	 * @inheritDoc
	 */
	public void wait(final Runnable runnable) {
		if(Looper.myLooper() == this.handler.getLooper()) {
			runnable.run();
		} else {
			final Semaphore done = new Semaphore(1);
			this.handler.post(new Runnable() {
				public void run() {
					runnable.run();
					done.release();
				}
			});
			done.acquireUninterruptibly();
		}
	}

	/**
	 * @inheritDoc
	 */
	public String getLabel() {
		return "mocha.foundation.queue.main";
	}

}
