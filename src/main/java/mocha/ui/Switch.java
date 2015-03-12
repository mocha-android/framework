/**
 *  @author Shaun
 *  @date 4/10/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.view.ViewGroup;
import android.widget.CompoundButton;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class Switch extends Control {

	private NativeView<android.widget.Switch> nativeView;
	private Size controlSize;

	public Switch() { this(new Rect(0.0f, 0.0f, 30.0f, 20.0f)); }
	public Switch(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.nativeView = new NativeView<android.widget.Switch>(new android.widget.Switch(Application.sharedApplication().getContext()));
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.nativeView);

		android.widget.Switch switchWidget = this.nativeView.getNativeView();
		switchWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				sendActionsForControlEvents(ControlEvent.VALUE_CHANGED);
			}
		});

		switchWidget.onMeasure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		this.controlSize = new Size();
		this.controlSize.width = ceilf((float)switchWidget.getMeasuredWidth() / scale);
		this.controlSize.height = ceilf((float)switchWidget.getMeasuredHeight() / scale);

		this.setBackgroundColor(Color.TRANSPARENT);
	}

	public Size sizeThatFits(Size size) {
		return new Size(Math.min(size.width, this.controlSize.width), Math.min(size.height, this.controlSize.height));
	}

	public void setOn(boolean on) {
		this.setOn(on, false);
	}

	public void setOn(boolean on, boolean animated) {
		// TODO: Animation
		this.nativeView.getNativeView().setChecked(on);
	}

	public boolean isOn() {
		return this.nativeView.getNativeView().isChecked();
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.nativeView.getNativeView().setEnabled(enabled);
	}

}
