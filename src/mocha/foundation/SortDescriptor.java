/**
 *  @author Shaun
 *  @date 3/15/14
 *  @copyright 2014 TV Guide, Inc. All rights reserved.
 */
package mocha.foundation;

public class SortDescriptor {
	public final String key;
	public final boolean ascending;

	public SortDescriptor(String key, boolean ascending) {
		this.key = key;
		this.ascending = ascending;
	}

}
