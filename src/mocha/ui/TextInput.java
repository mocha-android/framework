/**
 *  @author Shaun
 *  @date 3/21/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

public final class TextInput {
	private TextInput() { }

	public enum AutocapitalizationType {
		NONE,
		WORDS,
		SENTENCES,
		ALL_CHARACTERS,
	}

	public enum AutocorrectionType {
		DEFAULT,
		NO,
		YES,
	}

	public enum SpellCheckingType {
		DEFAULT,
		NO,
		YES,
	}

	public static final class Keyboard {
		private Keyboard() { }

		public enum Type {
			DEFAULT,                // Default type for the current input method.
			ASCII_Capable,           // Displays a keyboard which can enter ASCII characters, non-ASCII keyboards remain active
			NUMBERS_AND_PUNCTUATION,  // Numbers and assorted punctuation.
			URL,                    // A type optimized for URL entry (shows . / .com prominently).
			NUMBER_PAD,              // A number pad (0-9). Suitable for PIN entry.
			PHONE_PAD,               // A phone pad (1-9, *, 0, #, with letters under the numbers).
			NAME_PHONE_PAD,           // A type optimized for entering a person's name or phone number.
			EMAIL_ADDRESS,           // A type optimized for multiple email address entry (shows space @ . prominently).
			DECIMAL_PAD,             // A number pad with a decimal point.
			TWITTER,                // A type optimized for twitter text entry (easy access to @ #)
		}


		public enum Appearance {
			DEFAULT,          // Default apperance for the current input method.
			ALERT             // Appearance suitable for use in "alert" scenarios.
		}

		public enum ReturnKeyType {
			DEFAULT,
			GO,
			GOOGLE,
			JOIN,
			NEXT,
			ROUTE,
			SEARCH,
			SEND,
			YAHOO,
			DONE,
			EMERGENCY_CALL,
		}
	}

}
