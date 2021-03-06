package mocha.ui;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import mocha.graphics.Rect;

public interface TextInput {

	public enum AutocapitalizationType {
		NONE,
		WORDS,
		SENTENCES,
		ALL_CHARACTERS
	}

	public enum AutocorrectionType {
		DEFAULT,
		NO,
		YES
	}

	public enum SpellCheckingType {
		DEFAULT,
		NO,
		YES
	}

	public static final class Keyboard {
		private Keyboard() {
		}

		public enum Type {
			DEFAULT,
			URL,
			NUMBER_PAD,
			PHONE_PAD,
			EMAIL_ADDRESS,
			DECIMAL_PAD
		}


		public enum Appearance {
			DEFAULT,
			DARK,
			LIGHT
		}

		public enum ReturnKeyType {
			DEFAULT,
			GO,
			JOIN,
			NEXT,
			ROUTE,
			SEARCH,
			SEND,
			DONE
		}
	}

	public interface Traits {
		/*
			private TextInput.AutocapitalizationType autocapitalizationType;
			private TextInput.AutocorrectionType autocorrectionType;
			private TextInput.SpellCheckingType spellCheckingType;
			private TextInput.Keyboard.Type keyboardType;
			private TextInput.Keyboard.Appearance keyboardAppearance;
			private TextInput.Keyboard.ReturnKeyType returnKeyType;
			private boolean enablesReturnKeyAutomatically;
			private boolean secureTextEntry;
		 */

		public AutocapitalizationType getAutocapitalizationType();

		public void setAutocapitalizationType(AutocapitalizationType autocapitalizationType);

		public AutocorrectionType getAutocorrectionType();

		public void setAutocorrectionType(AutocorrectionType autocorrectionType);

		public SpellCheckingType getSpellCheckingType();

		public void setSpellCheckingType(SpellCheckingType spellCheckingType);

		public Keyboard.Type getKeyboardType();

		public void setKeyboardType(Keyboard.Type keyboardType);

		public Keyboard.Appearance getKeyboardAppearance();

		public void setKeyboardAppearance(Keyboard.Appearance keyboardAppearance);

		public Keyboard.ReturnKeyType getReturnKeyType();

		public void setReturnKeyType(Keyboard.ReturnKeyType returnKeyType);

		public boolean enablesReturnKeyAutomatically();

		public void setEnablesReturnKeyAutomatically(boolean enablesReturnKeyAutomatically);

		public boolean isSecureTextEntry();

		public void setSecureTextEntry(boolean secureTextEntry);

	}

	public static class TraitsHelper {
		public static void setupDefaultTraits(Traits traits) {
			traits.setAutocapitalizationType(AutocapitalizationType.SENTENCES);
			traits.setAutocorrectionType(AutocorrectionType.DEFAULT);
			traits.setSpellCheckingType(SpellCheckingType.DEFAULT);
			traits.setKeyboardType(Keyboard.Type.DEFAULT);
			traits.setKeyboardAppearance(Keyboard.Appearance.DEFAULT);
			traits.setReturnKeyType(Keyboard.ReturnKeyType.DEFAULT);
			traits.setEnablesReturnKeyAutomatically(false);
			traits.setSecureTextEntry(false);
		}

		static void setupEditTextWithTraits(EditText editText, Traits traits, boolean allowMultipleLines) {
			editText.setImeActionLabel(null, EditorInfo.IME_NULL);

			switch (traits.getReturnKeyType()) {
				case DEFAULT:
					editText.setImeOptions(0);
					break;
				case GO:
					editText.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
					break;
				case JOIN:
					editText.setImeActionLabel("Join", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
					break;
				case NEXT:
					// We don't use IME_ACTION_NEXT because the actual behavior is different.
					editText.setImeActionLabel("Next", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
					break;
				case ROUTE:
					editText.setImeActionLabel("Route", EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_GO);
					break;
				case SEARCH:
					editText.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_SEARCH);
					break;
				case SEND:
					editText.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_SEND);
					break;
				case DONE:
					editText.setImeOptions(EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_DONE);
					break;
			}

			int inputType = 0;
			boolean inputTypeIsText = false;
			switch (traits.getKeyboardType()) {
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

			if (inputTypeIsText) {
				if (traits.isSecureTextEntry()) {
					inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
				} else {
					switch (traits.getAutocapitalizationType()) {
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
				if (traits.isSecureTextEntry()) {
					inputType |= InputType.TYPE_NUMBER_VARIATION_PASSWORD;
				}
			}

			if (allowMultipleLines) {
				inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
			}

			editText.setInputType(inputType);
		}
	}

	Rect getCaretRectForPosition(int position);

	TextRange getSelectedTextRange();

}
