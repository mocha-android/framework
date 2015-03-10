/*
 *  @author Shaun
 *	@date 3/10/15
 *	@copyright	2015 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class RuntimeTargetAction<O> {

	private static <O> Method resolveActionMethodName(Object target, String actionMethodName, Class<O> optionalClass) {
		Method method;

		try {
			method = target.getClass().getMethod(actionMethodName, optionalClass);
		} catch (NoSuchMethodException e) {
			try {
				method = target.getClass().getMethod(actionMethodName);
			} catch (NoSuchMethodException e1) {
				throw new RuntimeException("Could not find method " + actionMethodName + " on target " + target + " that accepts a ControlEvent parameter or none at all.");
			}
		}

		return method;
	}

	private boolean methodTakesControlEventParameter;
	private WeakReference<Object> target;
	private Method action;

	public RuntimeTargetAction(Object target, String actionMethodName, Class<O> optionalClass) {
		this(target, resolveActionMethodName(target, actionMethodName, optionalClass), optionalClass);
	}

	public RuntimeTargetAction(Object target, Method action, Class<O> optionalClass) {
		boolean passedParameterCheck;
		this.methodTakesControlEventParameter = action.getParameterTypes().length == 1;

		if(this.methodTakesControlEventParameter) {
			Class parameter = action.getParameterTypes()[0];
			//noinspection unchecked
			passedParameterCheck = !(parameter != optionalClass && !optionalClass.isAssignableFrom(parameter));
		} else {
			passedParameterCheck = action.getParameterTypes().length == 0;
		}

		if(!passedParameterCheck) {
			throw new RuntimeException("Control target action can only accept a single ControlEvent parameter or no parameters at all.");
		}

		this.target = new WeakReference<>(target);
		this.action = action;
	}

	public void invoke(O optional) {
		Object target = this.target.get();
		if(target == null) return;

		try {
			if(this.methodTakesControlEventParameter) {
				this.action.invoke(target, optional);
			} else {
				this.action.invoke(target);
			}
		} catch (IllegalAccessException e) {
			MObject.MWarn(e, "Could not invoke target/action to %s#%s", this.target, this.action);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
