/**
 *  @author Shaun
 *  @date 3/15/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.util.HashMap;
import java.util.Map;

public class Maps {

	private Maps() { }

	public static <K,V> Map<K,V> create(K key, V value) {
		Map<K,V> map = new HashMap<K,V>();
		map.put(key, value);
		return map;
	}

	public static <K,V> Map<K,V> create(K key1, V value1, K k2, V v2) {
		Map<K,V> map = new HashMap<K, V>();
		map.put(key1, value1);
		map.put(k2, v2);
		return map;
	}

	public static <K,V> Map<K,V> create(K key1, V value1, K k2, V v2, K k3, V v3) {
		Map<K,V> map = new HashMap<K, V>();
		map.put(key1, value1);
		map.put(k2, v2);
		map.put(k3, v3);
		return map;
	}

	public static <K,V> Map<K,V> create(K key1, V value1, K k2, V v2, K k3, V v3, K k4, V v4) {
		Map<K,V> map = new HashMap<K, V>();
		map.put(key1, value1);
		map.put(k2, v2);
		map.put(k3, v3);
		map.put(k4, v4);
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <K> Map<K,Object> create(K firstKey, Object... firstValueAndKeysAndValues) {
		Map<K,Object> map = new HashMap<K, Object>();

		if(firstValueAndKeysAndValues != null && firstValueAndKeysAndValues.length > 0) {
			map.put(firstKey, firstValueAndKeysAndValues[0]);

			for(int i = 1; i < firstValueAndKeysAndValues.length; i += 2) {
				map.put((K)firstValueAndKeysAndValues[i], firstValueAndKeysAndValues[i+1]);
			}
		}

		return map;
	}
}
