/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 Mocha. All rights reserved.
*/

package mocha.ui;

import mocha.graphics.*;

abstract class TableViewHeader extends TableViewHeaderFooterView {

	protected TableViewHeader(String reuseIdentifier) {
		super(reuseIdentifier);
	}

	public void setText(String text) {
		this.getTextLabel().setText(text);
	}

	public String getText() {
		return this.getTextLabel().getText().toString();
	}

	static class Plain extends TableViewHeader {

		Plain(String reuseIdentifier) {
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

	static class Grouped extends TableViewHeader {

		Grouped(String reuseIdentifier) {
			super(reuseIdentifier);

			this.getTextLabel().setTextColor(Color.BLUE);
		}

		public static float getHeight(CharSequence title, float constrainedToWidth) {
			// TODO: Fully implement
			return 30.0f;
		}

	}
}