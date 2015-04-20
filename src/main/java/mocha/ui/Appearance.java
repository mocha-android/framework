package mocha.ui;

import mocha.foundation.MObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class Appearance<C> extends MObject {
	private List<StorageItem> storage;

	private static class StorageItem {
		Method method;
		Object[] objects;

		private StorageItem(Method method, Object[] objects) {
			this.method = method;
			this.objects = objects;
		}
	}

	public void apply(C instance) {
		if (storage == null) return;

		for (StorageItem item : storage) {
			try {
				item.method.invoke(instance, item.objects);
			} catch (Exception e) {
				MWarn(e, "Could not apply method: " + item.method.getName());
			}
		}
	}

	protected void store(Method method, Object... args) {
		if (this.storage == null) {
			this.storage = new ArrayList<StorageItem>();
		}

		this.storage.add(new StorageItem(method, args));
	}

	public static class Storage<C, A extends Appearance<C>> extends MObject {

		private Map<Class, A> appearances;
		private Class<A> appearanceClass;
		private Class<C> rootClass;

		public Storage(Class<C> rootClass, Class<A> appearanceClass) {
			this.appearances = new HashMap<Class, A>();
			this.appearanceClass = appearanceClass;
			this.rootClass = rootClass;
		}

		public A appearance(Class<? extends C> cls) {
			if (this.appearances == null) {
				this.appearances = new HashMap<Class, A>();
			}

			A appearance = this.appearances.get(cls);

			if (appearance == null) {
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
			if (cls != this.rootClass) {
				Class c = instance.getClass();

				while (c != this.rootClass) {
					classes.add(0, c);
					c = c.getSuperclass();
				}
			}

			classes.add(0, this.rootClass);

			for (Class<? extends C> c : classes) {
				appearance(c).apply(instance);
			}
		}

	}
}
