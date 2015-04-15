/**
 *  @author Shaun
 *  @date 3/6/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.text.Editable;
import android.util.TypedValue;
import android.view.Gravity;
import mocha.foundation.Range;
import mocha.graphics.*;

public class TextView extends View implements TextInput, TextInput.Traits {

	public interface Delegate {
		public void didChange(TextView textView);

		public interface BeginEditing extends Delegate {
			public boolean shouldBeginEditing(TextView textView);
			public void didBeginEditing(TextView textView);
		}

		public interface EndEditing extends Delegate {
			public boolean shouldEndEditing(TextView textView);
			public void didEndEditing(TextView textView);
		}

		public interface ShouldChange extends Delegate {
			public boolean shouldChangeCharacters(TextView textView, Range inRange, CharSequence replacementText);
		}

		public interface ShouldReturn extends Delegate {
			public boolean shouldReturn(TextView textView);
		}

	}

	private EditText editText;
	private NativeView<EditText> nativeView;
	private TextWatcher textWatcher;
	private boolean ignoreTextChanges;

	private TextInput.AutocapitalizationType autocapitalizationType;
	private TextInput.AutocorrectionType autocorrectionType;
	private TextInput.SpellCheckingType spellCheckingType;
	private TextInput.Keyboard.Type keyboardType;
	private TextInput.Keyboard.Appearance keyboardAppearance;
	private TextInput.Keyboard.ReturnKeyType returnKeyType;
	private boolean enablesReturnKeyAutomatically;
	private boolean secureTextEntry;
	private boolean forceEndEditing;

	private int textColor;
	private Font font;
	private TextAlignment textAlignment;
	private Delegate delegate;
	private Delegate.BeginEditing delegateBeginEditing;
	private Delegate.EndEditing delegateEndEditing;
	private Delegate.ShouldChange delegateShouldChange;
	private Delegate.ShouldReturn delegateShouldReturn;
	private boolean editable;
	private EdgeInsets contentInset;

	public TextView() { }
	public TextView(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.editable = true;

		this.textWatcher = new TextWatcher();
		this.editText = new EditText(Application.sharedApplication().getContext(), this, true) {
			public void onEditorAction(int actionCode) {
				returnKeyPressed();
			}
		};

		this.editText.addTextChangedListener(this.textWatcher);
		TraitsHelper.setupDefaultTraits(this);

		this.nativeView = new NativeView<EditText>(this.editText);
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.nativeView);

		this.setContentInset(new EdgeInsets(4.0f, 8.0f, 4.0f, 8.0f));

		this.setTextColor(Color.BLACK);
		this.setFont(Font.getSystemFontWithSize(12.0f));
		this.setTextAlignment(TextAlignment.LEFT);
	}

	public boolean canBecomeFirstResponder() {
		return this.editable;
	}

	public boolean becomeFirstResponder() {
		if(this.delegateBeginEditing != null && !this.delegateBeginEditing.shouldBeginEditing(this)) {
			return false;
		}

		if(!this.editable) {
			return false;
		}

		EditText.LEAVE_KEYBOARD = true;
		if(super.becomeFirstResponder()) {
			this.ignoreTextChanges = false;
			this.editText._requestFocus();

			if(this.delegateBeginEditing != null) {
				this.delegateBeginEditing.didBeginEditing(this);
			}

			this.forceEndEditing = false;

			EditText.LEAVE_KEYBOARD = false;
			return true;
		} else {
			EditText.LEAVE_KEYBOARD = false;
			return false;
		}
	}

	public boolean resignFirstResponder() {
		if(this.delegateEndEditing != null && !this.delegateEndEditing.shouldEndEditing(this) && !this.forceEndEditing) {
			return false;
		}

		if(super.resignFirstResponder()) {
			this.editText.forceClearFocus();
			this.ignoreTextChanges = true;

			if(this.delegateEndEditing != null) {
				this.delegateEndEditing.didEndEditing(this);
			}

			return true;
		} else {
			return false;
		}
	}

	public void willMoveToWindow(Window newWindow) {
		super.willMoveToWindow(newWindow);
		this.forceEndEditing = newWindow == null;
	}

	private void returnKeyPressed() {
		// TODO: Implementation real functionality here
		if(this.delegateShouldReturn != null) {
			this.delegateShouldReturn.shouldReturn(this);
		}
	}

	public CharSequence getText() {
		CharSequence text = this.editText.getText();
		return text != null ? text : "";
	}

	public void setText(CharSequence text) {
		boolean ignoreTextChanges = this.ignoreTextChanges;
		this.ignoreTextChanges = true;
		this.editText.setText(text);
		this.setNeedsDisplay();
		this.ignoreTextChanges = ignoreTextChanges;
	}

	public int getTextColor() {
		return this.textColor;
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
		this.editText.setTextColor(textColor);
	}

	public Font getFont() {
		return this.font;
	}

	public void setFont(Font font) {
		this.font = font;
		this.editText.setTypeface(font.getTypeface());
		this.editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, font.getPointSize() * this.scale);
	}

	public TextAlignment getTextAlignment() {
		return this.textAlignment;
	}

	public void setTextAlignment(TextAlignment textAlignment) {
		this.textAlignment = textAlignment;

		switch (this.textAlignment) {
			case LEFT:
				this.editText.setGravity(Gravity.LEFT);
				break;
			case CENTER:
				this.editText.setGravity(Gravity.CENTER_HORIZONTAL);
				break;
			case RIGHT:
				this.editText.setGravity(Gravity.RIGHT);
				break;
		}
	}

	public Delegate getDelegate() {
		return this.delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;

		if(this.delegate instanceof Delegate.BeginEditing) {
			this.delegateBeginEditing = (Delegate.BeginEditing)this.delegate;
		} else {
			this.delegateBeginEditing = null;
		}

		if(this.delegate instanceof Delegate.EndEditing) {
			this.delegateEndEditing = (Delegate.EndEditing)this.delegate;
		} else {
			this.delegateEndEditing = null;
		}

		if(this.delegate instanceof Delegate.ShouldChange) {
			this.delegateShouldChange = (Delegate.ShouldChange)this.delegate;
		} else {
			this.delegateShouldChange = null;
		}

		if(this.delegate instanceof Delegate.ShouldReturn) {
			this.delegateShouldReturn = (Delegate.ShouldReturn)this.delegate;
		} else {
			this.delegateShouldReturn = null;
		}
	}

	public boolean isEditing() {
		return this.editText.isInEditMode();
	}

	public boolean isEditable() {
		return editable;
	}

	public EdgeInsets getContentInset() {
		return contentInset.copy();
	}

	public void setContentInset(EdgeInsets contentInset) {
		if(contentInset == null) {
			contentInset = EdgeInsets.zero();
		} else {
			contentInset = contentInset.copy();
		}

		this.contentInset = contentInset;

		int top = (int)floorf(contentInset.top * this.scale);
		int left = (int)floorf(contentInset.left * this.scale);
		int bottom = (int)floorf(contentInset.bottom * this.scale);
		int right = (int)floorf(contentInset.right * this.scale);

		this.nativeView.getNativeView().setPadding(left, top, right, bottom);
	}

	public void setEditable(boolean editable) {
		if(this.editable != editable) {
			if(this.editable && this.isFirstResponder()) {
				this.resignFirstResponder();
			}

			this.editable = editable;
			this.editText.setFocusable(editable);
			this.editText.setFocusableInTouchMode(editable);
		}
	}

	public void setScrollEnabled(boolean scrollEnabled) {
		if(scrollEnabled) {
			if(this.editText.getMovementMethod() == null) {
				this.editText.setMovementMethod(this.editText.getDefaultMovementMethod());
			}
		} else {
			if(this.editText.getMovementMethod() != null) {
				this.editText.setMovementMethod(null);
			}
		}
	}

	public boolean isScrollEnabled() {
		return this.editText.getMovementMethod() != null;
	}

	// - TextInput

	@Override
	public Rect getCaretRectForPosition(int position) {
		if(position >= 0) {
			android.text.Layout layout = this.editText.getLayout();
			int line = layout.getLineForOffset(position);

			float x = layout.getPrimaryHorizontal(position);
			float y = layout.getLineBaseline(line) + layout.getLineAscent(line);

			return new Rect(this.contentInset.left + (x / this.scale), this.contentInset.top + (y / this.scale), 1.0f, (layout.getLineBottom(line) - layout.getLineTop(line)) / this.scale);
		} else {
			return Rect.zero();
		}
	}

	@Override
	public TextRange getSelectedTextRange() {
		return new TextRange(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
	}

	// - TextInput.Traits

	public TextInput.AutocapitalizationType getAutocapitalizationType() {
		return this.autocapitalizationType;
	}

	public void setAutocapitalizationType(TextInput.AutocapitalizationType autocapitalizationType) {
		if(this.autocapitalizationType != autocapitalizationType) {
			this.autocapitalizationType = autocapitalizationType;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public TextInput.AutocorrectionType getAutocorrectionType() {
		return this.autocorrectionType;
	}

	public void setAutocorrectionType(TextInput.AutocorrectionType autocorrectionType) {
		if(this.autocorrectionType != autocorrectionType) {
			this.autocorrectionType = autocorrectionType;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public TextInput.SpellCheckingType getSpellCheckingType() {
		return this.spellCheckingType;
	}

	public void setSpellCheckingType(TextInput.SpellCheckingType spellCheckingType) {
		if(this.spellCheckingType != spellCheckingType) {
			this.spellCheckingType = spellCheckingType;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public TextInput.Keyboard.Type getKeyboardType() {
		return this.keyboardType;
	}

	public void setKeyboardType(TextInput.Keyboard.Type keyboardType) {
		if(keyboardType == null) {
			keyboardType = TextInput.Keyboard.Type.DEFAULT;
		}

		if(this.keyboardType != keyboardType) {
			this.keyboardType = keyboardType;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public TextInput.Keyboard.Appearance getKeyboardAppearance() {
		return this.keyboardAppearance;
	}

	public void setKeyboardAppearance(TextInput.Keyboard.Appearance keyboardAppearance) {
		if(this.keyboardAppearance != keyboardAppearance) {
			this.keyboardAppearance = keyboardAppearance;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public TextInput.Keyboard.ReturnKeyType getReturnKeyType() {
		return this.returnKeyType;
	}

	public void setReturnKeyType(TextInput.Keyboard.ReturnKeyType returnKeyType) {
		if(this.returnKeyType != returnKeyType) {
			this.returnKeyType = returnKeyType;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public boolean enablesReturnKeyAutomatically() {
		return this.enablesReturnKeyAutomatically;
	}

	public void setEnablesReturnKeyAutomatically(boolean enablesReturnKeyAutomatically) {
		if(this.enablesReturnKeyAutomatically != enablesReturnKeyAutomatically) {
			this.enablesReturnKeyAutomatically = enablesReturnKeyAutomatically;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	public boolean isSecureTextEntry() {
		return this.secureTextEntry;
	}

	public void setSecureTextEntry(boolean secureTextEntry) {
		if(this.secureTextEntry != secureTextEntry) {
			this.secureTextEntry = secureTextEntry;
			this.editText.setupEditTextWithTraits(this);
		}
	}

	private class TextWatcher implements android.text.TextWatcher {
		private CharSequence previousText;

		public void beforeTextChanged(CharSequence text, int start, int count, int after) {
			if(ignoreTextChanges) return;

			try {
				if(after < count) {
					this.previousText = text.subSequence(start + after, start + count);
				} else if(count < after) {
					if(start + after > text.length()) {
						this.previousText = null;
					} else {
						this.previousText = text.subSequence(start + count, start + after);
					}
				} else {
					this.previousText = text.subSequence(start, start + count);
				}
			} catch (Exception e) {
				this.previousText = null;
				MWarn(e, "Invalid range? length: %d, start: %d, count: %d", text.length(), start, count);
			}
		}

		public void afterTextChanged(Editable editable) { }

		public void onTextChanged(CharSequence text, int start, int before, int count) {
			if(ignoreTextChanges) return;

			if(delegateShouldChange != null) {
				CharSequence replacementText = null;
				Range range = new Range();

				try {
					if(count > before) {
						range.location = start + before;
						range.length = count - before;
						replacementText = text.subSequence(start + before, start + count);
					} else if(count < before) {
						range.location = before;
						replacementText = null;
					} else {
						range.location = start;
						range.length = count;
						replacementText = text.subSequence(start, start + count);
					}
				} catch (Exception e) {
					MWarn(e, "Invalid range? length: %d, start: %d, count: %d", text.length(), start, count);
				}

				if(!delegateShouldChange.shouldChangeCharacters(TextView.this, range, replacementText)) {
					ignoreTextChanges = true;
					Editable editable = editText.getEditableText();

					try {
						if(count > before) {
							if(previousText == null) {
								editable.delete(start + before, start + count);
							} else {
								editable.replace(start + before, start + count, this.previousText);
							}
						} else if(count < before) {
							editable.insert(start + count, this.previousText);
						} else {
							editable.replace(start, start + count, this.previousText);
						}
					} catch (Exception e) {
						MWarn(e, "Couldn't prevent change, length: %d, start: %d, count: %d", text.length(), start, count);
					}

					editText.setSelection(start + before);
					ignoreTextChanges = false;
					return;
				}
			}

			if(delegate != null) {
				delegate.didChange(TextView.this);
			}
		}

	}
}