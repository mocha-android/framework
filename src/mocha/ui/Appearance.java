/**
 *  @author Shaun
 *  @date 2/12/13
 *  @copyright enormego. All rights reserved.
 */
package mocha.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class Appearance<C> extends mocha.foundation.Object {
	private Map<Method,Object[]> storage;

	public void apply(C instance) {
		if(storage == null) return;

		for(Method method : storage.keySet()) {
			try {
				method.invoke(instance, storage.get(method));
			} catch (Exception e) {
				MWarn(e, "Could not apply method: " + method.getName());
			}
		}
	}

	protected void store(Method method, Object... args) {
		if(this.storage == null) {
			this.storage = new HashMap<Method, Object[]>();
		}

		this.storage.put(method, args);
	}

	public static class Manager<C, A extends Appearance<C>> extends mocha.foundation.Object {

		private HashMap<Class, A> appearances;
		private Class<A> appearanceClass;
		private Class<C> rootClass;

		public Manager(Class<C> rootClass, Class<A> appearanceClass) {
			this.appearances = new HashMap<Class, A>();
			this.appearanceClass = appearanceClass;
			this.rootClass = rootClass;
		}

		public A appearance(Class<? extends C> cls) {
			if(this.appearances == null) {
				this.appearances = new HashMap<Class, A>();
			}

			A appearance = this.appearances.get(cls);

			if(appearance == null) {
				try {
					appearance = this.appearanceClass.newInstance();
				} catch (Exception e) {
					MWarn(e, "Could not create Appearance instance");
				}

				this.appearances.put(cls, appearance);
			}

			return appearance;
		}

		@SuppressWarnings("unchecked")
		public void apply(C instance) {
			Class<? extends C> cls = (Class<? extends C>) instance.getClass();

			// Note: We want to apply in reverse order so settings for parent classes
			// don't override their children

			List<Class<? extends C>> classes = new ArrayList<Class<? extends C>>();
			if(cls != this.rootClass) {
				Class c = instance.getClass();

				while(c != this.rootClass) {
					classes.add(0, c);
					c = c.getSuperclass();
				}
			}

			classes.add(0, this.rootClass);

			for(Class<? extends C> c : classes) {
				appearance(c).apply(instance);
			}
		}

	}
}
