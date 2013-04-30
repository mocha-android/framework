/**
*  @author Shaun
*  @date 2/5/2013
*  @copyright 2013 Mocha. All rights reserved.
*/

package mocha.ui;

import mocha.graphics.*;

abstract class TableViewHeader extends TableViewSubview {
	protected Label label;

	TableViewHeader(Rect frame) {
		super(frame);

		this.label = new Label(this.getBounds());
		this.label.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.label);
	}

	public void setText(String text) {
		this.label.setText(text);
	}

	public String getText() {
		return this.label.getText().toString();
	}

	static class Plain extends TableViewHeader {

		Plain() { this(new Rect(0.0f, 0.0f, 320.0f, 30.0f)); }

		Plain(Rect frame) {
			super(frame);

			this.label.setFrame(new Rect(12.0f, -1.0f, frame.size.width - 24.0f, frame.size.height));
			this.label.setFont(Font.getBoldSystemFontWithSize(18.0f));
			this.label.setTextColor(Color.WHITE);
			this.label.setBackgroundColor(Color.TRANSPARENT);
			this.label.setShadowColor(Color.white(0.0f, 0.5f));
			this.label.setShadowOffset(new Size(0.0f, 1.0f));
		}

		public void draw(Context context, Rect rect) {
			Image.imageNamed(R.drawable.mocha_table_view_section_header_background).draw(context, rect);
		}
	}

	static class Grouped extends TableViewHeader {

		Grouped() { this(Rect.zero()); }

		Grouped(Rect frame) {
			super(frame);

			this.label.setTextColor(Color.BLUE);
		}

	}
}