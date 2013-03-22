/**
 *  @author Shaun
 *  @date 3/6/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import mocha.graphics.*;

public class TextView extends View implements TextInput.Traits {//ScrollView implements ScrollView.Listener {

	private android.widget.TextView textView;
	private NativeView<android.widget.TextView> nativeView;
	
	// Traits
	private TextInput.AutocapitalizationType autocapitalizationType;
	private TextInput.AutocorrectionType autocorrectionType;
	private TextInput.SpellCheckingType spellCheckingType;
	private TextInput.Keyboard.Type keyboardType;
	private TextInput.Keyboard.Appearance keyboardAppearance;
	private TextInput.Keyboard.ReturnKeyType returnKeyType;
	private boolean enablesReturnKeyAutomatically;
	private boolean secureTextEntry;
	

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

		this.font = Font.getSystemFontWithSize(17.0f);
		this.textAlignment = TextAlignment.LEFT;

		TextInput.setupDefaultTraits(this);
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
		this.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_PX, font.getPointSize() * this.scale);
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

			if(!this.editable && this.textView != null && (this.textView instanceof EditText) && this.textView.isFocused()) {
				((EditText)this.textView)._clearFocus();
				this.resignFirstResponder();
			}

			if(this.textView != null) {
				android.widget.TextView oldTextView = this.textView;
				this.textView = null;

				android.widget.TextView newTextView = this.getTextView();
				newTextView.setText(oldTextView.getText());
				newTextView.setTextColor(oldTextView.getCurrentTextColor());
				newTextView.setTypeface(this.font.getTypeface());
				newTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.font.getPointSize() * this.scale);
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
				this.textView = new EditText(Application.sharedApplication().getContext(), this, true);
				this.synchronizeTextInputTraits();
			} else {
				this.textView = new android.widget.TextView(Application.sharedApplication().getContext());
			}

			this.textView.setTypeface(this.font.getTypeface());
			this.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.font.getPointSize() * this.scale);
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

	public boolean canBecomeFirstResponder() {
		return true;
	}

	public boolean becomeFirstResponder() {
		if(super.becomeFirstResponder()) {
			if(this.textView instanceof EditText && this.editable) {
				((EditText)this.textView)._requestFocus();
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean resignFirstResponder() {
		if(super.resignFirstResponder()) {
			if(this.textView instanceof EditText && this.editable && this.textView.isFocused()) {
				((EditText)this.textView)._clearFocus();
			}

			return true;
		} else {
			return false;
		}
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

	public TextInput.AutocapitalizationType getAutocapitalizationType() {
		return autocapitalizationType;
	}

	public void setAutocapitalizationType(TextInput.AutocapitalizationType autocapitalizationType) {
		this.autocapitalizationType = autocapitalizationType;
		this.synchronizeTextInputTraits();
	}

	public TextInput.AutocorrectionType getAutocorrectionType() {
		return autocorrectionType;
	}

	public void setAutocorrectionType(TextInput.AutocorrectionType autocorrectionType) {
		this.autocorrectionType = autocorrectionType;
		this.synchronizeTextInputTraits();
	}

	public TextInput.SpellCheckingType getSpellCheckingType() {
		return spellCheckingType;
	}

	public void setSpellCheckingType(TextInput.SpellCheckingType spellCheckingType) {
		this.spellCheckingType = spellCheckingType;
		this.synchronizeTextInputTraits();
	}

	public TextInput.Keyboard.Type getKeyboardType() {
		return keyboardType;
	}

	public void setKeyboardType(TextInput.Keyboard.Type keyboardType) {
		if(keyboardType != null) {
			this.keyboardType = keyboardType;
		} else {
			this.keyboardType = TextInput.Keyboard.Type.DEFAULT;
		}

		this.synchronizeTextInputTraits();
	}

	public TextInput.Keyboard.Appearance getKeyboardAppearance() {
		return keyboardAppearance;
	}

	public void setKeyboardAppearance(TextInput.Keyboard.Appearance keyboardAppearance) {
		this.keyboardAppearance = keyboardAppearance;
		this.synchronizeTextInputTraits();
	}

	public TextInput.Keyboard.ReturnKeyType getReturnKeyType() {
		return returnKeyType;
	}

	public void setReturnKeyType(TextInput.Keyboard.ReturnKeyType returnKeyType) {
		this.returnKeyType = returnKeyType;
		this.synchronizeTextInputTraits();
	}

	public boolean enablesReturnKeyAutomatically() {
		return enablesReturnKeyAutomatically;
	}

	public void setEnablesReturnKeyAutomatically(boolean enablesReturnKeyAutomatically) {
		this.enablesReturnKeyAutomatically = enablesReturnKeyAutomatically;
		this.synchronizeTextInputTraits();
	}

	public boolean isSecureTextEntry() {
		return secureTextEntry;
	}

	public void setSecureTextEntry(boolean secureTextEntry) {
		this.secureTextEntry = secureTextEntry;
		this.synchronizeTextInputTraits();
	}

	private void synchronizeTextInputTraits() {
		if(this.textView != null && this.textView instanceof EditText) {
			((EditText) this.textView).setupEditTextWithTraits(this);
		}
	}

}
