package mocha.ui;

import mocha.foundation.MObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class RuntimeTargetAction {

	private static Method resolveActionMethodName(Object target, String actionMethodName) {
		Method[] methods = target.getClass().getMethods();
		Method method = null;
		int count = 0;

		for (Method m : methods) {
			if (m.getName().equals(actionMethodName)) {
				method = m;
				count++;
			}
		}

		if (count > 1) {
			throw new RuntimeException("Found multiple public methods named " + actionMethodName + " on target " + target + ".");
		} else if (method == null) {
			throw new RuntimeException("Could not find public method " + actionMethodName + " on target " + target + ".");
		}

		return method;
	}

	private WeakReference<Object> target;
	private Method action;
	private List<Class> argumentClasses;

	public RuntimeTargetAction(Object target, String actionMethodName, Class... availableParameterTypes) {
		this(target, resolveActionMethodName(target, actionMethodName), availableParameterTypes);
	}

	public RuntimeTargetAction(Object target, Method action, Class... availableParameterTypes) {
		this.argumentClasses = new ArrayList<>();

		for (Class parameterType : action.getParameterTypes()) {
			boolean matched = false;

			for (Class availableParameterType : availableParameterTypes) {
				if (parameterType.isAssignableFrom(availableParameterType)) {
					this.argumentClasses.add(availableParameterType);
					matched = true;
					break;
				}
			}

			if (!matched) {
				throw new RuntimeException(String.format("%s on %s requires parameter type %s which is not available." + action.getName(), target));
			}
		}

		this.target = new WeakReference<>(target);
		this.action = action;
	}

	public void invoke(Object... arguments) {
		Object target = this.target.get();
		if (target == null) return;

		try {
			if (this.argumentClasses.size() > 0) {
				Object[] args = new Object[this.argumentClasses.size()];
				int i = 0;

				for (Class argumentType : this.argumentClasses) {
					Object arg = null;

					for (Object a : arguments) {
						if (argumentType.isInstance(a)) {
							arg = a;
							break;
						}
					}

					args[i++] = arg;
				}

				this.action.invoke(target, args);
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
