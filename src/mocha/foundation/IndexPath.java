/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.foundation;

import java.util.Arrays;

public class IndexPath extends mocha.foundation.Object implements mocha.foundation.Copying <IndexPath> {
	private final int[] indexes;
	private final int hashCode;

	// Convenience vars to avoid getters
	public final int section;
	public final int row;
	public final int item;

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

		if(this.indexes.length > 0) {
			this.section = this.indexes[0];

			if(this.indexes.length > 1) {
				this.row = this.indexes[1];
				this.item = this.indexes[1];
			} else {
				this.row = -1;
				this.item = -1;
			}
		} else {
			this.section = -1;
			this.row = -1;
			this.item = -1;
		}
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
		return this.row;
	}

	public int getItem() {
		return this.item;
	}

	public int getSection() {
		return this.section;
	}

	public boolean equals(java.lang.Object object) {
		return object == this || (object instanceof IndexPath && this.hashCode == ((IndexPath)object).hashCode);
	}

	// Only works with section/row index paths
	public boolean lowerThan(IndexPath indexPath) {
		return ((indexPath != null && ((this.section < indexPath.section) || (this.section == indexPath.section && this.row < indexPath.row))));
	}

	// Only works with section/row index paths
	public boolean greaterThan(IndexPath indexPath) {
		return ((indexPath != null && ((this.section > indexPath.section) || (this.section == indexPath.section && this.row > indexPath.row))));
	}


	public String toString() {
		return String.format("<%s 0x%d indexes=%s>", this.getClass(), this.hashCode, Arrays.toString(this.indexes));
	}

	public IndexPath copy() {
		int[] indexes = this.indexes.clone();
		return new IndexPath(indexes);
	}

}
