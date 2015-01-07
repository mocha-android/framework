/**
 *  @author Shaun
 *  @date 3/17/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.lang.ref.ReferenceQueue;

public class WeakReference <T> extends java.lang.ref.WeakReference <T> {

	public interface HasReference<T> {
		public void hasReference(T t);
	}

	public WeakReference(T r) {
		super(r);
	}

	public WeakReference(T r, ReferenceQueue<? super T> q) {
		super(r, q);
	}

	public void runIf(HasReference<T> hasReference) {
		T t = this.get();

		if(t != null && hasReference != null) {
			hasReference.hasReference(t);
		}
	}

	public static <T> T get(WeakReference<T> weakReference) {
		if(weakReference != null) {
			return weakReference.get();
		} else {
			return null;
		}
	}

	public static <T> WeakReference<T>replace(WeakReference<T> oldReference, T t) {
		if(oldReference != null) {
			oldReference.clear();
		}

		return create(t);
	}

	public static <T> WeakReference<T>create(T t) {
		if(t != null) {
			return new WeakReference<T>(t);
		} else {
			return null;
		}
	}

}
