/**
 *  @author Shaun
 *  @date 3/21/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.R;
import android.content.Context;
import android.text.InputType;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

class EditText extends android.widget.EditText implements View.OnFocusChangeListener, View.OnClickListener {

	private boolean allowMultipleLines;
	private mocha.ui.View containerView;
	private boolean allowFocusChange;
	private boolean hasSetFocusable;

	private boolean didTryToFocus;

	EditText(Context context, mocha.ui.View containerView, boolean allowMultipleLines) {
		super(context, null, R.style.Theme_Holo_Light);

		this.allowMultipleLines = allowMultipleLines;
		this.containerView = containerView;
		this.setFocusable(false);
		this.setFocusableInTouchMode(false);
		this.hasSetFocusable = false;

		this.setOnFocusChangeListener(this);
		this.setOnClickListener(this);

		this.setBackgroundDrawable(null);
		this.setPadding(0, 0, 0, 0);
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(this, 0);
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	public void clearFocus() {
		super.clearFocus();
		this.hideKeyboard();
	}

	public void _clearFocus() {
		boolean allowFocusChange = this.allowFocusChange;
		this.allowFocusChange = true;
		boolean isFocusable = this.isFocusable();
		if(isFocusable) this.setFocusable(false);
		this.clearFocus();
		if(isFocusable) this.setFocusable(true);
		this.allowFocusChange = allowFocusChange;
	}

	public void setFocusable(boolean focusable) {
		super.setFocusable(focusable);
		this.setFocusableInTouchMode(focusable);
		this.hasSetFocusable = true;
	}

	private void reenableFocusable() {
		if(!this.hasSetFocusable) {
			this.setFocusable(true);
		}
	}

	public void onFocusChange(View view, boolean focused) {
		if(this.allowFocusChange) return;

		if(focused) {
			this.didTryToFocus = true;
			this._clearFocus();
		}
	}

	public boolean _requestFocus() {
		this.reenableFocusable();
		this.allowFocusChange = true;
		boolean result = this.requestFocus();

		if(result) {
			this.showKeyboard();
		}

		this.allowFocusChange = false;
		return result;
	}

	public void onClick(View view) {

	}

	public void onEditorAction(int actionCode) {

	}

	public boolean onDragEvent(DragEvent event) {
		return false;
	}

	public boolean didTouchFocusSelect() {
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		this.reenableFocusable();

		this.allowFocusChange = false;
		this.didTryToFocus = false;
		boolean result = super.onTouchEvent(event);

		if(this.didTryToFocus) {
			if(this.containerView.canBecomeFirstResponder()) {
				this.containerView.becomeFirstResponder();
			}
		}

		return result;
	}

	void setupEditTextWithTraits(TextInput.Traits traits) {
		this.setImeActionLabel(null, EditorInfo.IME_NULL);

		if(traits.getReturnKeyType() != null) switch (traits.getReturnKeyType()) {
			case DEFAULT:
				this.setImeOptions(0);
				break;
			case GO:
				this.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
				break;
			case JOIN:
				this.setImeActionLabel("Join", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
				break;
			case NEXT:
				// We don't use IME_ACTION_NEXT because the actual behavior is different.
				this.setImeActionLabel("Next", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
				break;
			case ROUTE:
				this.setImeActionLabel("Route", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
				break;
			case SEARCH:
				this.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_SEARCH);
				break;
			case SEND:
				this.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_SEND);
				break;
			case DONE:
				this.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_DONE);
				break;
		}

		int inputType = 0;
		boolean inputTypeIsText = false;

		if(!(traits instanceof mocha.ui.TextView) || ((mocha.ui.TextView) traits).isEditable()) {
			if(traits.getKeyboardType() != null) switch (traits.getKeyboardType()) {
				case URL:
					inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
					inputTypeIsText = true;
					break;
				case NUMBER_PAD:
					inputType = InputType.TYPE_CLASS_NUMBER;
					break;
				case PHONE_PAD:
					inputType = InputType.TYPE_CLASS_PHONE;
					break;
				case EMAIL_ADDRESS:
					inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
					inputTypeIsText = true;
					break;
				case DECIMAL_PAD:
					inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
					break;
				case DEFAULT:
					inputType = InputType.TYPE_CLASS_TEXT;
					inputTypeIsText = true;
					break;
			}
		} else {
			inputType = InputType.TYPE_NULL;
		}

		if(inputTypeIsText) {
			if(traits.isSecureTextEntry()) {
				inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
			} else {
				if(traits.getAutocapitalizationType() != null) switch (traits.getAutocapitalizationType()) {
					case NONE:
						inputType |= InputType.TYPE_TEXT_VARIATION_FILTER;
						break;
					case WORDS:
						inputType |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
						break;
					case SENTENCES:
						inputType |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
						break;
					case ALL_CHARACTERS:
						inputType |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
						break;
				}
			}
		} else {
			if(traits.isSecureTextEntry()) {
				inputType |= InputType.TYPE_NUMBER_VARIATION_PASSWORD;
			}
		}

		if(this.allowMultipleLines) {
			inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
		}

		this.setInputType(inputType);
	}
}
