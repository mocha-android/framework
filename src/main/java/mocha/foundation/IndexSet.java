/**
 *  @author Shaun
 *  @date 4/16/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

// TODO: Explore not using HashSet for this.

public class IndexSet extends HashSet<Integer> {

	public IndexSet() {

	}

	public IndexSet(int capacity) {
		super(capacity);
	}

	public IndexSet(Collection<? extends Integer> collection) {
		super(collection);
	}

	public IndexSet(Range range) {
		super((int)range.length);

		long length = range.max();
		for(long i = range.location; i < length; i++) {
			this.add((int)i);
		}
	}

	public int getFirstIndex() {
		int size = this.size();

		if(size > 0) {
			return this.toIntArray()[0];
		} else {
			return -1;
		}
	}

	public int getLastIndex() {
		int size = this.size();

		if(size > 0) {
			return this.toIntArray()[size - 1];
		} else {
			return -1;
		}
	}

	public int[] toIntArray() {
		int[] array = new int[this.size()];

		int idx = 0;
		for(Integer i : this) {
			array[idx++] = i;
		}

		Arrays.sort(array);

		return array;
	}

}
