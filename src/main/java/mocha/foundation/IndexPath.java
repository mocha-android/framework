package mocha.foundation;

import java.util.Arrays;

public final class IndexPath extends MObject implements Copying<IndexPath>, Comparable<IndexPath> {
	private final int[] indexes;
	private final int hashCode;

	/**
	 * Convenience getter to get first index
	 * <p/>
	 * Used primarily for speed in {@link mocha.ui.TableView}
	 */
	public final int section;

	/**
	 * Convenience getter to get second index
	 * <p/>
	 * Used primarily for speed in {@link mocha.ui.TableView}
	 */
	public final int row;

	/**
	 * Convenience getter to get second index
	 * <p/>
	 * Synonymous with {@link #row}
	 */
	public final int item;

	/**
	 * Create an IndexPath with a single index
	 *
	 * @param index Index
	 *
	 * @return IndexPath
	 */
	public static IndexPath withIndex(int index) {
		return IndexPath.withIndexes(index);
	}

	/**
	 * Create an IndexPath with a list of indexes
	 *
	 * @param indexes List of indexes
	 *
	 * @return IndexPath
	 */
	public static IndexPath withIndexes(int... indexes) {
		int hashCode = Arrays.hashCode(indexes);
		// TODO: Effecient cache/reusing
		return new IndexPath(hashCode, indexes);
	}

	public IndexPath(int index) {
		this(new int[]{index});
	}

	public IndexPath(int... index) {
		this(Arrays.hashCode(index), index);
	}

	private IndexPath(int hashCode, int... index) {
		this.indexes = index;
		this.hashCode = hashCode;

		if (this.indexes.length > 0) {
			this.section = this.indexes[0];

			if (this.indexes.length > 1) {
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
	 *
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
	 * Convenience method to create an item/section IndexPath
	 */
	public static IndexPath withItemInSection(int item, int section) {
		return withIndexes(section, item);
	}

	/**
	 * Compare index paths
	 *
	 * @param other Other index path to compare
	 *
	 * @return ComparisonResult
	 */
	public ComparisonResult compareTo(IndexPath other) {
		int length = this.indexes.length;
		int otherLength = other.indexes.length;

		for (int index = 0; index < length; index++) {
			if (index < otherLength) {
				if (this.indexes[index] != other.indexes[index]) {
					return this.indexes[index] < other.indexes[index] ? ComparisonResult.ASCENDING : ComparisonResult.DESCENDING;
				}
			} else {
				return ComparisonResult.DESCENDING;
			}
		}

		if (length == otherLength) {
			return ComparisonResult.SAME;
		} else {
			// For loop catches length > otherLength
			// so if we're here, it means length < otherLength
			return ComparisonResult.ASCENDING;
		}
	}

	@Override
	public boolean equals(java.lang.Object object) {
		return object != null && (object == this || (object instanceof IndexPath && this.hashCode == ((IndexPath) object).hashCode));
	}

	@Override
	protected String toStringExtra() {
		return "indexes=" + Arrays.toString(this.indexes);
	}

	public IndexPath copy() {
		int[] indexes = this.indexes.clone();
		return new IndexPath(indexes);
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}
}
