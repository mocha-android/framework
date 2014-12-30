/**
 *  @author Shaun
 *  @date 3/15/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

public class SortDescriptor extends MObject {
	public final String key;
	public final boolean ascending;

	public SortDescriptor(String key, boolean ascending) {
		this.key = key;
		this.ascending = ascending;
	}

	protected String toStringExtra() {
		return String.format("key = %s; ascending = %s", this.key, this.ascending);
	}

}
