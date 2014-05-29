/**
 *  @author Shaun
 *  @date 5/29/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

public interface TypedResultBlock <T, R> {

	public R block(T t);

}
