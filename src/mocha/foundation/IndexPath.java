/*
 *  @author Shaun
 *	@date 11/19/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.util.Arrays;

public class IndexPath extends MObject implements mocha.foundation.Copying <IndexPath> {
	private final int[] indexes;
	private final int hashCode;

	/**
	 * Convenience getter to get first index
	 *
	 * Used primarily for speed in {@link mocha.ui.TableView}
	 */
	public final int section;

	/**
	 * Convenience getter to get second index
	 *
	 * Used primarily for speed in {@link mocha.ui.TableView}
	 */
	public final int row;

	/**
	 * Convenience getter to get second index
	 *
	 * Synonymous with {@link #row}
	 */
	public final int item;

	/**
	 * Create an IndexPath with a single index
	 *
	 * @param index Index
	 * @return IndexPath
	 */
	public static IndexPath withIndex(int index) {
		return new IndexPath(index);
	}

	/**
	 * Create an IndexPath with a list of indexes
	 * @param indexes List of indexes
	 * @return IndexPath
	 */
	public static IndexPath withIndexes(int... indexes) {
		return new IndexPath(indexes);
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

	/**
	 * @return Size of the IndexPath
	 */
	public int size() {
		return this.indexes.length;
	}

	/**
	 * @param position Position of the index to get
	 * @return The index at the position
	 */
	public int get(int position) {
		return this.indexes[position];
	}

	/**
	 * @return All indexes
	 */
	public int[] getIndexes() {
		return this.indexes.clone();
	}

	/**
	 * Convenience method to create a row/section IndexPath
	 */
	public static IndexPath withRowInSection(int row, int section) {
		return withIndexes(section, row);
	}

	/**
	 * Convenience method to create an iten/section IndexPath
	 */
	public static IndexPath withItemInSection(int item, int section) {
		return withIndexes(section, item);
	}

	@Override
	public boolean equals(java.lang.Object object) {
		return object == this || (object instanceof IndexPath && this.hashCode == ((IndexPath)object).hashCode);
	}

	@Override
	public String toString() {
		return String.format("<%s 0x%d indexes=%s>", this.getClass(), this.hashCode, Arrays.toString(this.indexes));
	}

	@Override
	public IndexPath copy() {
		int[] indexes = this.indexes.clone();
		return new IndexPath(indexes);
	}

}
