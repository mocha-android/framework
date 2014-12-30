/**
 *  @author Shaun
 *  @date 7/30/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation.concurrent;

public interface ExceptionHandler {

	public void handleException(Throwable throwable, Queue queue);

}
