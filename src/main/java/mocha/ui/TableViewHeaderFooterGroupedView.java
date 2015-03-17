/**
 *  @author Shaun
 *  @date 2/5/2013
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

public class TableViewHeaderFooterGroupedView extends TableViewHeaderFooterView {
	public static final String REUSE_IDENTIFIER = "TableViewHeaderFooterGroupedViewIdentifier";

	public TableViewHeaderFooterGroupedView(String reuseIdentifier) {
		super(reuseIdentifier);

		this.getTextLabel().setTextColor(Color.BLUE);
	}

	public static float getHeight(CharSequence title, float constrainedToWidth) {
		// TODO: Fully implement
		return 30.0f;
	}

}
