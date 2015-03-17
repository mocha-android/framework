/**
 *  @author Shaun
 *  @date 2/5/2013
 *  @copyright 2013 Mocha. All rights reserved.
 */

package mocha.ui;

class TableViewFooter extends TableViewHeaderFooterView {
	public static final String REUSE_IDENTIFIER = "TableViewHeaderFooterPlainViewIdentifier";

	TableViewFooter(String reuseIdentifier) {
		super(reuseIdentifier);
	}

	public static float getHeight(CharSequence title, float constrainedToWidth) {
		// TODO: Fully implement
		return 30.0f;
	}

}