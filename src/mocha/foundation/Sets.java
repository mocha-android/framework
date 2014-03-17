/**
 *  @author Shaun
 *  @date 3/15/14
 *  @copyright 2014 TV Guide, Inc. All rights reserved.
 */
package mocha.foundation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sets {

	private Sets() { }

	public static <T> Set<T> create(T... items) {
		Set<T> list = new HashSet<T>();
		Collections.addAll(list, items);
		return list;
	}

	public static <T> T any(Set<T> set) {
		return set.iterator().next();
	}

}
