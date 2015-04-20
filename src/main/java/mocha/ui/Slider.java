package mocha.ui;

import android.view.ViewGroup;
import android.widget.SeekBar;
import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class Slider extends Control {
	private NativeView<SeekBar> nativeView;
	private Size controlSize;

	public Slider() {
		this(new Rect(0.0f, 0.0f, 30.0f, 20.0f));
	}

	public Slider(Rect frame) {
		super(frame);
	}

	private int minimumValue;
	private int maximumValue;

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.minimumValue = 0;
		this.maximumValue = 1000;

		this.nativeView = new NativeView<SeekBar>(new SeekBar(Application.sharedApplication().getContext()));
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.nativeView);

		SeekBar seekBar = this.nativeView.getNativeView();
		seekBar.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		this.controlSize = new Size();
		this.controlSize.width = ceilf((float) seekBar.getMeasuredWidth() / scale);
		this.controlSize.height = ceilf((float) seekBar.getMeasuredHeight() / scale);

		this.setBackgroundColor(Color.TRANSPARENT);
	}

	public View hitTest(Point point, Event event) {
		View hitTest = super.hitTest(point, event);

		if (hitTest == this || hitTest == this.nativeView) {
			return this.nativeView;
		} else {
			return hitTest;
		}
	}

	public Size sizeThatFits(Size size) {
		return new Size(Math.min(size.width, this.controlSize.width), Math.min(size.height, this.controlSize.height));
	}

	public void setValue(int value) {
		this.nativeView.getNativeView().setThumbOffset(value - this.minimumValue);
	}

	public int getValue() {
		return this.minimumValue + this.nativeView.getNativeView().getThumbOffset();
	}

	public int getMinimumValue() {
		return minimumValue;
	}

	public void setMinimumValue(int minimumValue) {
		this.minimumValue = minimumValue;
		this.nativeView.getNativeView().setMax(this.maximumValue - this.minimumValue);
	}

	public int getMaximumValue() {
		return maximumValue;
	}

	public void setMaximumValue(int maximumValue) {
		this.maximumValue = maximumValue;
		this.nativeView.getNativeView().setMax(this.maximumValue - this.minimumValue);
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.nativeView.getNativeView().setEnabled(enabled);
	}

}
