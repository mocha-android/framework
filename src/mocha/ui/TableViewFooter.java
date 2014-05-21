/**
 *  @author Shaun
 *  @date 2/5/2013
 *  @copyright 2013 Mocha. All rights reserved.
 */

package mocha.ui;

class TableViewFooter extends TableViewHeaderFooterView {

	TableViewFooter(String reuseIdentifier) {
		super(reuseIdentifier);
	}

	public void setText(String text) {
		this.getTextLabel().setText(text);
	}

	public String getText() {
		return this.getTextLabel().getText().toString();
	}

	public static float getHeight(CharSequence title, float constrainedToWidth) {
		// TODO: Fully implement
		return 30.0f;
	}

}