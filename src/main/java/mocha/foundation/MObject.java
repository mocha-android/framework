package mocha.foundation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class MObject {
	private static ThreadLocal<Handler> handler = new ThreadLocal<Handler>();
	private static Handler mainHandler;
	private static String LOG_TAG = "Mocha";
	private static boolean loggingEnabled = true;

	public enum LogLevel {
		VERBOSE(android.util.Log.VERBOSE),
		DEBUG(android.util.Log.DEBUG),
		INFO(android.util.Log.INFO),
		WARN(android.util.Log.WARN),
		ERROR(android.util.Log.ERROR),
		WTF(android.util.Log.ASSERT);

		private int level;

		private LogLevel(int level) {
			this.level = level;
		}

		void log(String message) {
			android.util.Log.println(this.level, LOG_TAG, message);
		}

		void log(Throwable throwable, String message) {
			android.util.Log.println(this.level, LOG_TAG, message + '\n' + android.util.Log.getStackTraceString(throwable));
		}
	}

	public static boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	public static void setLoggingEnabled(boolean loggingEnabled) {
		MObject.loggingEnabled = loggingEnabled;
	}

	@Override
	public String toString() {
		String extra = this.toStringExtra();

		int hash = System.identityHashCode(this);

		Class cls = this.getClass();
		String className = cls.getCanonicalName();

		if (className == null || className.length() == 0) {
			className = cls.getSimpleName();
		}

		if (className == null || className.length() == 0) {
			className = cls.getName();
		}

		if (className == null || className.length() == 0) {
			className = cls.toString();
		}

		if (extra != null && extra.length() > 0) {
			return String.format("<%s@%s; %s>", className, hash, this.toStringExtra());
		} else {
			return String.format("<%s@%s>", className, hash);
		}
	}

	/**
	 * Extra data to be automaticallyed appended in toString()
	 *
	 * @return Extra data
	 */
	protected String toStringExtra() {
		return null;
	}

	private static Handler getMainHandler() {
		return getMainHandler(false);
	}

	private static Handler getMainHandler(boolean allowNullReturn) {
		if (mainHandler == null) {
			if (allowNullReturn) {
				return null;
			}

			Looper mainLooper = Looper.getMainLooper();

			if (mainLooper == null) {
				throw new RuntimeException("Could not find main looper.");
			} else {
				mainHandler = new Handler(mainLooper);
			}
		}

		return mainHandler;
	}

	private static Handler getHandler() {
		return getHandler(false);
	}

	private static Handler getHandler(boolean allowNullReturn) {
		Handler handler = MObject.handler.get();

		if (handler == null) {
			if (allowNullReturn) return null;

			handler = new Handler();
			MObject.handler.set(handler);
		}

		return handler;
	}

	/**
	 * Run a callback after a delay on the current thread.
	 *
	 * @param delayInMillis milliseconds before the callback should be called
	 * @param callback      callback to be called after a delay
	 *
	 * @return The same Runnable callback that was passed in, to allow for easy assignment when using
	 * anonymous classes.  This callback should be passed to {@link MObject#cancelCallbacks} when cancelling.
	 */
	public static Runnable performAfterDelay(long delayInMillis, Runnable callback) {
		// If we're on the main looper, there's no sense in creating
		// another local looper/thread just for this.

		if (Looper.myLooper() == getMainHandler().getLooper()) {
			getMainHandler().postDelayed(callback, delayInMillis);
		} else {
			getHandler().postDelayed(callback, delayInMillis);
		}

		return callback;
	}

	/**
	 * Run a callback after a delay on the main UI thread.
	 *
	 * @param delayInMillis milliseconds before the callback should be called
	 * @param callback      callback to be called after a delay
	 *
	 * @return The same Runnable callback that was passed in, to allow for easy assignment when using
	 * anonymous classes.  This callback should be passed to {@link MObject#cancelCallbacks} when cancelling.
	 */
	public static Runnable performOnMainAfterDelay(long delayInMillis, Runnable callback) {
		getMainHandler().postDelayed(callback, delayInMillis);
		return callback;
	}

	/**
	 * Run a callback on the main thread, while optionally blocking until execution has completed.
	 *
	 * @param waitUntilDone If true, this will block until execution is complete.  If the current
	 *                      thread is the main thread, then callback will be called immediately,
	 *                      otherwise it will be posted to the front of the main thread to be processed.
	 *                      If false, the callback will be posted to the main thread.
	 * @param callback      Callback to be called on the main thread
	 *
	 * @return The same Runnable callback that was passed in, to allow for easy assignment when using
	 * anonymous classes.  This callback should be passed to {@link MObject#cancelCallbacks} when cancelling.
	 */
	public static Runnable performOnMain(boolean waitUntilDone, final Runnable callback) {
		if (waitUntilDone) {
			if (Looper.myLooper() == getMainHandler().getLooper()) {
				callback.run();
			} else {
				final Semaphore done = new Semaphore(1);

				getMainHandler().postAtFrontOfQueue(new Runnable() {
					public void run() {
						callback.run();
						done.release();
					}
				});

				done.acquireUninterruptibly();
			}
		} else {
			getMainHandler().post(callback);
		}

		return callback;
	}

	/**
	 * Cancel callbacks made with {@link #performAfterDelay}, {@link #performOnMainAfterDelay} or {@link #performOnMain}
	 *
	 * @param runnable Callback passed to {@link #performAfterDelay}, {@link #performOnMainAfterDelay} or {@link #performOnMain}
	 *
	 * @see MObject#performAfterDelay(long, Runnable)
	 * @see MObject#performOnMainAfterDelay(long, Runnable)
	 * @see MObject#performOnMain(boolean, Runnable)
	 */
	public static void cancelCallbacks(Runnable runnable) {
		Handler handler = getHandler(true);

		if (handler != null) {
			handler.removeCallbacks(runnable);
		}

		handler = getMainHandler(true);

		if (handler != null) {
			handler.removeCallbacks(runnable);
		}
	}

	public static void MLog(LogLevel logLevel, String message) {
		if (loggingEnabled) {
			if (logLevel == null) {
				logLevel = LogLevel.VERBOSE;
			}

			logLevel.log(message);
		}
	}

	public static void MLog(LogLevel logLevel, Throwable throwable, String message) {
		if (loggingEnabled) {
			if (logLevel == null) {
				logLevel = LogLevel.DEBUG;
			}

			logLevel.log(throwable, message);
		}
	}

	public static void MLog(String message) {
		MLog(LogLevel.DEBUG, message);
	}

	public static void MWarn(String message) {
		MLog(LogLevel.WARN, message);
	}

	public static void MLog(Throwable throwable, String message) {
		MLog(LogLevel.DEBUG, throwable, message);
	}

	public static void MWarn(Throwable throwable, String message) {
		MLog(LogLevel.WARN, throwable, message);
	}

	public static void MLogException(Throwable throwable) {
		MLogException(throwable, throwable.getMessage());
	}

	public static void MLogException(Throwable throwable, String message) {
		MLog(LogLevel.ERROR, throwable, message);
	}

	public static void MLog(String format, java.lang.Object... args) {
		if (loggingEnabled) {
			MLog(String.format(format, args));
		}
	}

	public static void MWarn(String format, java.lang.Object... args) {
		if (loggingEnabled) {
			MWarn(String.format(format, args));
		}
	}

	public static void MLog(Throwable throwable, String format, java.lang.Object... args) {
		if (loggingEnabled) {
			MLog(throwable, String.format(format, args));
		}
	}

	public static void MWarn(Throwable throwable, String format, java.lang.Object... args) {
		if (loggingEnabled) {
			MWarn(throwable, String.format(format, args));
		}
	}

	protected static String MGetCurrentMethodName() {
		StackTraceElement stackTraceElement = MGetCurrentStackTraceElement("MGetCurrentMethodName");

		if (stackTraceElement != null) {
			return stackTraceElement.getMethodName();
		} else {
			return null;
		}
	}

	protected static int MGetCurrentLineNumber() {
		StackTraceElement stackTraceElement = MGetCurrentStackTraceElement("MGetCurrentLineNumber");

		if (stackTraceElement != null) {
			return stackTraceElement.getLineNumber();
		} else {
			return -1;
		}
	}

	public static void MLogStackTrace(String format, java.lang.Object... args) {
		if (!loggingEnabled) return;

		String message = String.format(format, args);

		StringBuilder stringBuilder = new StringBuilder();
		if (message != null) {
			stringBuilder.append(message).append(" | ");
		}

		stringBuilder.append("Strack Trace:");

		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		boolean logElement = false;

		for (StackTraceElement element : elements) {
			if (logElement) {
				stringBuilder.append("\n").append(element.getClassName()).append(".").append(element.getMethodName()).append(":").append(element.getLineNumber());
			} else {
				logElement = element.getMethodName().equals("MLogStackTrace");
			}
		}

		MLog(stringBuilder.toString());
	}

	private static StackTraceElement MGetCurrentStackTraceElement(String returnAfterMethodName) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();

		boolean returnNextElement = false;

		if (returnAfterMethodName == null) {
			returnAfterMethodName = "MGetCurrentStackTraceElement";
		}

		for (StackTraceElement element : elements) {
			if (returnNextElement) {
				return element;
			}

			returnNextElement = element.getMethodName().equals(returnAfterMethodName);
		}

		return null;
	}

}
