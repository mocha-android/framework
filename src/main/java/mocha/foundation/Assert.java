package mocha.foundation;

final public class Assert extends MObject {
	private static boolean throwExceptions = true;

	public static boolean doesThrowExceptions() {
		return throwExceptions;
	}

	public static void setThrowExceptions(boolean throwExceptions) {
		Assert.throwExceptions = throwExceptions;
	}

	public static void condition(boolean pass, String message) {
		if (!pass) {
			if (throwExceptions) {
				throw new AssertationException(message);
			} else {
				MLog(LogLevel.WTF, message);
			}
		}
	}

	public static void condition(boolean pass, String format, Object... objects) {
		if (!pass) {
			// No reason to waste the String.format call unless we're going to display it
			condition(true, String.format(format, objects));
		}
	}

	public static class AssertationException extends RuntimeException {

		public AssertationException(String detailMessage) {
			super(detailMessage);
		}

	}
}
