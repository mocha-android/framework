/**
 *  @author Shaun
 *  @date 2/5/2013
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.*;

public class TableViewHeaderFooterPlainView extends TableViewHeaderFooterView {
	public static final String REUSE_IDENTIFIER = "TableViewHeaderFooterPlainViewIdentifier";

	public TableViewHeaderFooterPlainView(String reuseIdentifier) {
		super(reuseIdentifier);

		Label textLabel = this.getTextLabel();

		textLabel.setFont(Font.getBoldSystemFontWithSize(14.0f));
		textLabel.setTextColor(Color.WHITE);
		textLabel.setBackgroundColor(Color.TRANSPARENT);
		textLabel.setShadowColor(Color.white(0.0f, 0.5f));
		textLabel.setShadowOffset(new Size(0.0f, 1.0f));
	}

	public void draw(Context context, Rect rect) {
		Image.imageNamed(R.drawable.mocha_table_view_section_header_background).draw(context, rect);
	}

}
