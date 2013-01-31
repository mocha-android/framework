/*
 *  @author Shaun
 *	@date 11/23/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.foundation;

import android.util.Log;

public class Object {

	public static void MLog(String message) {
		Log.d("Mocha", message);
	}

	public static void MWarn(String message) {
		Log.w("Mocha", message);
	}

	public static void MLog(Throwable throwable, String message) {
		Log.d("Mocha", message, throwable);
	}

	public static void MWarn(Throwable throwable, String message) {
		Log.w("Mocha", message, throwable);
	}

	public static void MLog(String format, java.lang.Object... args) {
		MLog(String.format(format, args));
	}

	public static void MWarn(String format, java.lang.Object... args) {
		MWarn(String.format(format, args));
	}

	public static void MLog(Throwable throwable, String format, java.lang.Object... args) {
		MLog(throwable, String.format(format, args));
	}

	public static void MWarn(Throwable throwable, String format, java.lang.Object... args) {
		MWarn(throwable, String.format(format, args));
	}

	protected static String MGetCurrentMethodName() {
		StackTraceElement stackTraceElement = MGetCurrentStackTraceElement("MGetCurrentMethodName");

		if(stackTraceElement != null) {
			return stackTraceElement.getMethodName();
		} else {
			return null;
		}
	}

	protected static int MGetCurrentLineNumber() {
		StackTraceElement stackTraceElement = MGetCurrentStackTraceElement("MGetCurrentLineNumber");

		if(stackTraceElement != null) {
			return stackTraceElement.getLineNumber();
		} else {
			return -1;
		}
	}

	public static void MLogStackTrace(String format, java.lang.Object... args) {
		String message = String.format(format, args);

		StringBuilder stringBuilder = new StringBuilder();
		if(message != null) {
			stringBuilder.append(message).append(" | ");
		}

		stringBuilder.append("Strack Trace:");

		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		boolean logElement = false;

		for(StackTraceElement element : elements) {
			if(logElement) {
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

		if(returnAfterMethodName == null) {
			returnAfterMethodName = "MGetCurrentStackTraceElement";
		}

		for(StackTraceElement element : elements) {
			if(returnNextElement) {
				return element;
			}

			returnNextElement = element.getMethodName().equals(returnAfterMethodName);
		}

		return null;
	}

}
