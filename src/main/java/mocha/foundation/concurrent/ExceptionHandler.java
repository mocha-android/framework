package mocha.foundation.concurrent;

public interface ExceptionHandler {

	public void handleException(Throwable throwable, Queue queue);

}
