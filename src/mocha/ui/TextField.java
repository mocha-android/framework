/**
 *  @author Shaun
 *  @date 3/21/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.util.TypedValue;
import android.view.Gravity;
import mocha.graphics.*;

public class TextField extends Control implements TextInput.Traits {

	public static final String DID_BEGIN_EDITING_NOTIFICATION = "TEXT_FIELD_DID_BEGIN_EDITING_NOTIFICATION";
	public static final String END_EDITING_NOTIFICATION = "TEXT_FIELD_END_EDITING_NOTIFICATION";
	public static final String TEXT_DID_CHANGE_NOTIFICATION = "TEXT_FIELD_TEXT_DID_CHANGE_NOTIFICATION";

	public enum ViewMode {
		NEVER,
		WHILE_EDITING,
		UNLESS_EDITING,
		ALWAYS
	}

	public interface Delegate {

	}

	private EditText editText;
	private NativeView<EditText> nativeView;

	private TextInput.AutocapitalizationType autocapitalizationType;
	private TextInput.AutocorrectionType autocorrectionType;
	private TextInput.SpellCheckingType spellCheckingType;
	private TextInput.Keyboard.Type keyboardType;
	private TextInput.Keyboard.Appearance keyboardAppearance;
	private TextInput.Keyboard.ReturnKeyType returnKeyType;
	private boolean enablesReturnKeyAutomatically;
	private boolean secureTextEntry;

	private int textColor;
	private Font font;
	private TextAlignment textAlignment;
	private CharSequence placeholder;
	private int placeholderColor;
	private boolean clearsOnBeginEditing;
	private Delegate delegate;
	private Image background;
	private Image disabledBackground;
	private boolean editing;

	private ViewMode clearButtonMode;
	private Button clearButton;

	private ViewMode leftViewMode;
	private View leftView;

	private ViewMode rightViewMode;
	private View rightView;


	public TextField() { }
	public TextField(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.editText = new EditText(Application.sharedApplication().getContext(), this, false);
		TextInput.setupDefaultTraits(this);

		this.nativeView = new NativeView<EditText>(this.editText);
		// this.nativeView.setBackgroundColor(Color.GREEN);
		this.addSubview(this.nativeView);

		this.setTextColor(Color.BLACK);
		this.setFont(Font.getSystemFontWithSize(12.0f));
		this.setTextAlignment(TextAlignment.LEFT);
		this.setPlaceholderColor(Color.white(0.7f, 1.0f));
	}

	public boolean canBecomeFirstResponder() {
		return true;
	}

	public boolean becomeFirstResponder() {
		if(super.becomeFirstResponder()) {
			this.editText._requestFocus();
			return true;
		} else {
			return false;
		}
	}

	public boolean resignFirstResponder() {
		if(super.resignFirstResponder()) {
			this.editText.clearFocus();
			return true;
		} else {
			return false;
		}
	}
	public CharSequence getText() {
		return this.editText.getText();
	}

	public void setText(CharSequence text) {
		this.editText.setText(text);
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

	public CharSequence getPlaceholder() {
		return this.placeholder;
	}

	public void setPlaceholder(CharSequence placeholder) {
		this.placeholder = placeholder;
		this.setNeedsDisplay();
	}

	public int getPlaceholderColor() {
		return this.placeholderColor;
	}

	public void setPlaceholderColor(int placeholderColor) {
		this.placeholderColor = placeholderColor;
		this.setNeedsDisplay();
	}

	public boolean clearsOnBeginEditing() {
		return this.clearsOnBeginEditing;
	}

	public void setClearsOnBeginEditing(boolean clearsOnBeginEditing) {
		this.clearsOnBeginEditing = clearsOnBeginEditing;
	}

	public Delegate getDelegate() {
		return this.delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	public Image getBackground() {
		return background;
	}

	public void setBackground(Image background) {
		this.background = background;
		this.setNeedsDisplay();
	}

	public Image getDisabledBackground() {
		return disabledBackground;
	}

	public void setDisabledBackground(Image disabledBackground) {
		this.disabledBackground = disabledBackground;
		this.setNeedsDisplay();
	}

	public boolean isEditing() {
		return this.editText.isInEditMode();
	}

	public ViewMode getClearButtonMode() {
		return clearButtonMode;
	}

	public void setClearButtonMode(ViewMode clearButtonMode) {
		if(this.clearButtonMode != clearButtonMode) {
			this.clearButtonMode = clearButtonMode;

			if(this.clearButtonMode == ViewMode.NEVER) {
				if(this.clearButton != null) {
					this.clearButton.removeFromSuperview();
					this.clearButton = null;
				}
			} else if(this.clearButton == null) {
				this.clearButton = new Button();
				this.clearButton.setImage(R.drawable.mocha_text_field_clear_button, State.NORMAL);
				this.clearButton.setImage(R.drawable.mocha_text_field_clear_button_pressed, State.NORMAL, State.HIGHLIGHTED);
			}

			this.setNeedsLayout();
		}
	}

	public ViewMode getLeftViewMode() {
		return leftViewMode;
	}

	public void setLeftViewMode(ViewMode leftViewMode) {
		this.leftViewMode = leftViewMode;
		this.setNeedsLayout();
	}

	public View getLeftView() {
		return leftView;
	}

	public void setLeftView(View leftView) {
		this.leftView = leftView;
		this.setNeedsLayout();
	}

	public ViewMode getRightViewMode() {
		return rightViewMode;
	}

	public void setRightViewMode(ViewMode rightViewMode) {
		this.rightViewMode = rightViewMode;
		this.setNeedsLayout();
	}

	public View getRightView() {
		return rightView;
	}

	public void setRightView(View rightView) {
		this.rightView = rightView;
		this.setNeedsLayout();
	}

	protected Rect getTextRectForBounds(Rect bounds) {
		Rect rect = bounds.copy();

		if(this.leftView != null && !this.leftView.isHidden()) {
			Rect leftViewRect = this.getLeftViewRectForBounds(bounds);
			rect.origin.x = leftViewRect.maxX();
			rect.size.width -= rect.origin.x - bounds.origin.x;
		}

		float maxX = 0.0f;

		if(this.rightView != null && !this.rightView.isHidden()) {
			maxX = this.getRightViewRectForBounds(bounds).origin.x;
		}

		if(this.clearButton != null && !this.clearButton.isHidden()) {
			float x = this.getClearButtonRectForBounds(bounds).origin.x;

			if(maxX > 0.0f) {
				maxX = Math.min(x, maxX);
			} else {
				maxX = x;
			}
		}

		if(maxX > 0.0f) {
			rect.size.width -= maxX - bounds.origin.x;
		}

		return rect;
	}

	protected Rect getPlaceholderRectForBounds(Rect bounds) {
		return this.getTextRectForBounds(bounds);
	}

	protected Rect getEditingRectForBounds(Rect bounds) {
		return this.getTextRectForBounds(bounds);
	}

	protected Rect getClearButtonRectForBounds(Rect bounds) {
		if(this.clearButton != null && !this.clearButton.isHidden()) {
			Rect rect = this.clearButton.getFrame();
			rect.origin.x = bounds.size.width - rect.size.width;
			rect.origin.y = floorf((bounds.size.height - rect.size.height) / 2.0f);
			return rect;
		} else {
			return Rect.zero();
		}
	}

	protected Rect getLeftViewRectForBounds(Rect bounds) {
		if(this.leftView != null && !this.leftView.isHidden()) {
			Rect rect = this.leftView.getFrame();
			rect.origin.x = 0.0f;
			rect.origin.y = floorf((bounds.size.height - rect.size.height) / 2.0f);
			return rect;
		} else {
			return Rect.zero();
		}
	}

	protected Rect getRightViewRectForBounds(Rect bounds) {
		if(this.rightView != null && !this.rightView.isHidden()) {
			Rect rect = this.rightView.getFrame();

			Rect clearRect = this.getClearButtonRectForBounds(bounds);
			if(clearRect.empty()) {
				rect.origin.x = bounds.size.width - rect.size.width;
			} else {
				rect.origin.x = clearRect.origin.x - rect.size.width;
			}

			rect.origin.y = floorf((bounds.size.height - rect.size.height) / 2.0f);
			return rect;
		} else {
			return Rect.zero();
		}
	}


	public void layoutSubviews() {
		super.layoutSubviews();

		Rect bounds = this.getBounds();

		if(this.leftView != null) {
			this.leftView.setHidden(!this.isViewModeVisible(this.leftViewMode));
			this.leftView.setFrame(this.getLeftViewRectForBounds(bounds));
		}

		if(this.rightView != null) {
			this.rightView.setHidden(!this.isViewModeVisible(this.rightViewMode));
			this.rightView.setFrame(this.getRightViewRectForBounds(bounds));
		}

		if(this.clearButton != null) {
			this.clearButton.setHidden(!this.isViewModeVisible(this.clearButtonMode));
			this.clearButton.setFrame(this.getClearButtonRectForBounds(bounds));
		}

		this.nativeView.setFrame(this.getTextRectForBounds(bounds));
	}

	private boolean isViewModeVisible(ViewMode viewMode) {
		switch (viewMode) {
			case NEVER:
				return false;
			case WHILE_EDITING:
				return this.editing;
			case UNLESS_EDITING:
				return !this.editing;
			case ALWAYS:
				return true;
		}

		return false;
	}

	public void draw(Context context, Rect rect) {
		super.draw(context, rect);

		Image background = this.getBackground();

		if(this.getState().contains(State.DISABLED) && this.getDisabledBackground() != null) {
			background = this.getDisabledBackground();
		}

		if(background != null) {
			background.draw(context, rect);
		}

		if(this.placeholder != null && this.placeholder.length() > 0 && this.placeholderColor != Color.TRANSPARENT) {
			if(this.getText().length() <= 0) {
				context.setFillColor(this.placeholderColor);
				TextDrawing.draw(context, this.placeholder, this.getPlaceholderRectForBounds(rect), this.font, this.textAlignment);
			}
		}
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

}
