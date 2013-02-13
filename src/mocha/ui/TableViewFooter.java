/**
 *  @author Shaun
 *  @date 2/5/2013
 *  @copyright 2013 enormego. All rights reserved.
 */

package mocha.ui;

import mocha.graphics.Rect;

class TableViewFooter extends TableViewSubview {
	private Label label;

	public TableViewFooter() {
		this(Rect.zero());
	}

	public TableViewFooter(Rect frame) {
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

}