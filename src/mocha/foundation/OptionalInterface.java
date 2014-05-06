/**
 *  @author Shaun
 *  @date 4/4/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface OptionalInterface {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Optional {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NotImplemented {

	}


}
