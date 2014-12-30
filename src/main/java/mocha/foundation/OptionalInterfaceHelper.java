/**
 *  @author Shaun
 *  @date 4/4/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.lang.reflect.Method;

public class OptionalInterfaceHelper extends MObject {

	public static boolean hasImplemented(OptionalInterface implementation, Class<? extends OptionalInterface> optionalInterface, String methodName, Class<?>... parameterTypes) {
		if(optionalInterface == null) {
			return false;
		}

		try {
			return hasImplemented(implementation, optionalInterface.getMethod(methodName, parameterTypes));
		} catch (NoSuchMethodException e) {
			MLogException(e);
			return false;
		}
	}

	public static boolean hasImplemented(OptionalInterface implementation, Method method) {
		if(implementation == null) {
			return false;
		}

		if(!method.isAnnotationPresent(OptionalInterface.Optional.class)) {
			MWarn("Checking for implementation presence of non-optional method %s on %s.", method, method.getClass());
		}

		try {
			Method implementedMethod = implementation.getClass().getMethod(method.getName(), method.getParameterTypes());
			return !implementedMethod.isAnnotationPresent(OptionalInterface.NotImplemented.class);
		} catch (NoSuchMethodException e) {
			MLogException(e);
			return false;
		}
	}

}
