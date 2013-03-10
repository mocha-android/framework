/**
 *  @author Shaun
 *  @date 3/6/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.Gravity;
import android.widget.EditText;
import mocha.graphics.*;

public class TextView extends View {//ScrollView implements ScrollView.Listener {

	private android.widget.TextView textView;
	private NativeView<android.widget.TextView> nativeView;

	public TextView() { super(); }
	public TextView(Rect frame) { super(frame); }

	private Font font;
	private TextAlignment textAlignment;
	private boolean editable;

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.nativeView = new NativeView<android.widget.TextView>(null);
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.nativeView.setUserInteractionEnabled(false);
		// this.setAlwaysBounceVertical(true);
		// this.setBounces(true);
		this.setClipsToBounds(true);
		// this.setListener(this);
		this.addSubview(this.nativeView);
	}

	public void setFrame(Rect frame) {
		super.setFrame(frame);
		this.updateContentHeight();
	}

	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);
		this.nativeView.setBackgroundColor(backgroundColor);
	}

	public CharSequence getText() {
		if(this.textView == null) {
			return "";
		} else {
			CharSequence text = this.getTextView().getText();
			return text == null ? "" : null;
		}
	}

	public void setText(CharSequence text) {
		this.getTextView().setText(text);
		this.updateContentHeight();
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
		this.getTextView().setTypeface(font.getTypeface());
		this.getTextView().setTextSize(font.getPointSize() * this.scale);
		this.updateContentHeight();
	}

	public int getTextColor() {
		return this.getTextView().getCurrentTextColor();
	}

	public void setTextColor(int textColor) {
		this.getTextView().setTextColor(textColor);
	}

	public TextAlignment getTextAlignment() {
		return textAlignment;
	}

	public void setTextAlignment(TextAlignment textAlignment) {
		this.textAlignment = textAlignment;

		switch (textAlignment) {
			case CENTER:
				this.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
				break;
			case RIGHT:
				this.getTextView().setGravity(Gravity.RIGHT);
				break;
			case LEFT:
			default:
				this.getTextView().setGravity(Gravity.LEFT);
				break;
		}
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		if(this.editable != editable) {
			this.editable = editable;

			if(this.textView != null) {
				android.widget.TextView oldTextView = this.textView;
				this.textView = null;

				android.widget.TextView newTextView = this.getTextView();
				newTextView.setText(oldTextView.getText());
				newTextView.setTextColor(oldTextView.getCurrentTextColor());
				newTextView.setTypeface(oldTextView.getTypeface());
				newTextView.setTextSize(oldTextView.getTextSize());
				newTextView.setEnabled(oldTextView.isEnabled());
				newTextView.setGravity(oldTextView.getGravity());
			}
		}
	}

	private android.widget.TextView getTextView() {
		if(this.textView == null) {
			if(this.nativeView != null) {
				this.nativeView.removeFromSuperview();
				this.nativeView = null;

			}

			if(this.editable) {
				this.textView = new EditText(Application.sharedApplication().getContext());
			} else {
				this.textView = new android.widget.TextView(Application.sharedApplication().getContext());
			}

			this.textView.setBackgroundColor(Color.TRANSPARENT);
			this.textView.setPadding(0, 0, 0, 0);

			this.nativeView = new NativeView<android.widget.TextView>(this.textView);
			this.nativeView.setFrame(this.getBounds());
			this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
			this.addSubview(this.nativeView);
			this.updateContentHeight();
		}

		return this.textView;
	}

	public void didScroll(ScrollView scrollView) {
		if(this.textView != null) {
			Point contentOffset = scrollView.getContentOffset();
			this.textView.scrollTo((int)(contentOffset.x * scale), (int)(contentOffset.y * scale));
			this.nativeView.setNeedsDisplay();
		}
	}

	private void updateContentHeight() {
		// android.widget.TextView textView = this.getTextView();
		// this.setContentSize(new Size(textView.getMeasuredWidth() / scale, textView.getMeasuredHeight() / scale));
	}
}
