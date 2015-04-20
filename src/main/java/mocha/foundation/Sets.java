package mocha.foundation;

import java.util.*;

public class Sets {

	private Sets() {
	}

	public static <T> Set<T> create(T... items) {
		Set<T> list = new HashSet<>();
		Collections.addAll(list, items);
		return list;
	}

	public static <T> T any(Set<T> set) {
		if (set == null || set.isEmpty()) {
			return null;
		} else {
			return set.iterator().next();
		}
	}

	public static <T> Set<T> copy(Collection<T> list) {
		Set<T> copy = new HashSet<>();

		if (list != null) {
			copy.addAll(list);
		}

		return copy;
	}

	/**
	 * Checks whether the two sets have at least one item in common
	 *
	 * @param set1
	 * @param set2
	 * @param <T>
	 *
	 * @return true if at least one item in set1 is also in set 1, otherwise false.
	 */
	public static <T> boolean intersects(Set<T> set1, Set<T> set2) {
		for (T t : set1) {
			if (set2.contains(t)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Creates a new set with only elements that are in both set1 and set2
	 *
	 * @param set1
	 * @param set2
	 * @param <T>
	 */
	public static <T> Set<T> intersectedSet(Set<T> set1, Set<T> set2) {
		Set<T> intersectedSet = new HashSet<>();
		intersectedSet.addAll(set1);

		Iterator<T> iterator = intersectedSet.iterator();

		while (iterator.hasNext()) {
			if (!set2.contains(iterator.next())) {
				iterator.remove();
			}
		}

		return intersectedSet;
	}

}
