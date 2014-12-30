/**
 *  @author Shaun
 *  @date 4/16/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

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

}
