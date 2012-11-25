/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.foundation;

import java.util.Arrays;

public class IndexPath extends mocha.foundation.Object {
	private final int[] indexes;
	private final int hashCode;

	public static IndexPath withIndex(int index) {
		return new IndexPath(index);
	}

	public static IndexPath withIndexes(int... index) {
		return new IndexPath(index);
	}

	public IndexPath(int index) {
		this(new int[] { index });
	}

	public IndexPath(int... index) {
		this.indexes = index;
		this.hashCode = Arrays.hashCode(this.indexes);
	}

	public int size() {
		return this.indexes.length;
	}

	public int get(int position) {
		return this.indexes[position];
	}

	public int[] getIndexes() {
		return this.indexes.clone();
	}

	// UI additions

	public static IndexPath withRowInSection(int row, int section) {
		return withIndexes(section, row);
	}

	public static IndexPath withItemInSection(int item, int section) {
		return withIndexes(section, item);
	}

	public int getRow() {
		return this.indexes[1];
	}

	public int getItem() {
		return this.indexes[1];
	}

	public int getSection() {
		return this.indexes[0];
	}

	public boolean equals(java.lang.Object object) {
		return object == this || (object instanceof IndexPath && this.hashCode == ((IndexPath)object).hashCode);
	}

	public String toString() {
		return String.format("<%s 0x%d indexes=%s>", this.getClass(), this.hashCode, Arrays.toString(this.indexes));
	}

}
