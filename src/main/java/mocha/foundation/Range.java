/**
 *  @author Shaun
 *  @date 3/21/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.lang.Object;

public class Range extends MObject implements Copying<Range> {
	public long location;
	public long length;

	public Range() { }
	public Range(long location, long length) {
		this.location = location;
		this.length = length;
	}

	public long max() {
		return this.location + this.length;
	}

	public boolean containsLocation(long location) {
		return location >= this.location && location < this.location + this.length;
	}

	public boolean equals(Object o) {
		if(o == this) {
			return true;
		} else if(o == null) {
			return false;
		} else if(o instanceof Range) {
			return this.location == ((Range) o).location && this.length == ((Range) o).length;
		} else {
			return false;
		}
	}

	public Range copy() {
		return new Range(this.location, this.length);
	}

	public String toString() {
		return String.format("<%s 0x%s: location = %d, length = %d>", this.getClass().getCanonicalName(), this.hashCode(), this.location, this.length);
	}

}
