/**
 *  @author Shaun
 *  @date 4/14/14
 *  @copyright 2014 Mocha. All rights reserved.
 */

package mocha.foundation;

public enum ComparisonResult {
	ASCENDING(-1), SAME(0), DESCENDING(1);

	private final int value;

	ComparisonResult(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}


	public static ComparisonResult compare(int i, int j) {
		if(i > j) {
			return DESCENDING;
		} else if(i < j) {
			return ASCENDING;
		} else {
			return SAME;
		}
	}

	public static ComparisonResult compare(long i, long j) {
		if(i > j) {
			return DESCENDING;
		} else if(i < j) {
			return ASCENDING;
		} else {
			return SAME;
		}
	}

	public static ComparisonResult compare(String i, String j) {
		int comparison = i.compareTo(j);

		if(comparison > 0) {
			return DESCENDING;
		} else if(comparison < 0) {
			return ASCENDING;
		} else {
			return SAME;
		}
	}


}
