/**
 *  @author Shaun
 *  @date 3/10/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import mocha.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Properly implement show() methods, currently just shows in the middle like an alert view of the screen.

public class ActionSheet extends mocha.foundation.Object {

	public interface Listener {
		/**
		 * Called when the button is clicked, before the dialog
		 * is dismissed.
		 *
		 * @param actionSheet Action sheet
		 * @param buttonIndex Button index that clicked (guaranteed not to be cancel button index)
		 */
		void onClickedButtonAtIndex(ActionSheet actionSheet, int buttonIndex);

		/**
		 * Called when the dialog was cancelled
		 *
		 * @param actionSheet Action sheet
		 */
		void onCancel(ActionSheet actionSheet);


		public interface Presentation extends Listener {
			/**
			 * Called before the action sheet appears on screen
			 *
			 * @param actionSheet Action sheet
			 */
			void willPresentActionSheet(ActionSheet actionSheet);

			/**
			 * Calls after the action sheet appears on screen
			 *
			 * @param actionSheet Action sheet
			 */
			void didPresentActionSheet(ActionSheet actionSheet);

			/**
			 * Called before the action sheet dismisses from screen, after Listener#onClickedButtonAtIndex is called.
			 *
			 * @param actionSheet Action sheet
			 * @param buttonIndex Button index that was clicked causing the dismisall (could be cancel button index)
			 */
			void willDismissWithButtonIndex(ActionSheet actionSheet, int buttonIndex);

			/**
			 * Called after the action sheet is removed from the screen.
			 *
			 * @param actionSheet Action sheet
			 * @param buttonIndex Button index that was clicked causing the dismisall (could be cancel button index)
			 */
			void didDismissWithButtonIndex(ActionSheet actionSheet, int buttonIndex);
		}
	}

	public enum Style {
		LIGHT, DARK
	}

	private Listener listener;
	private Listener.Presentation presentationListener;
	private CharSequence title;
	private CharSequence cancelButtonTitle;
	private List<CharSequence> otherButtonTitles;
	private AlertDialog alertDialog;
	private Style style;
	private int dismissedWithButtonIndex;
	private final static int CANCEL_BUTTON_INDEX = -1;

	/**
	 * Create an action sheet
	 *
	 * @param title Title of the action sheet
	 * @param listener Listener for the action sheet
	 * @param cancelButtonTitle Cancel button title, may be null
	 * @param otherButtonTitles Other button titles, may be null
	 */
	public ActionSheet(CharSequence title, Listener listener, CharSequence cancelButtonTitle, CharSequence... otherButtonTitles) {
		this.style = Style.DARK;
		this.setListener(listener);

		this.title = title;
		this.cancelButtonTitle = cancelButtonTitle;
		this.otherButtonTitles = new ArrayList<CharSequence>();

		if(otherButtonTitles != null) {
			Collections.addAll(this.otherButtonTitles, otherButtonTitles);
		}
	}

	/**
	 * Get the current listener
	 *
	 * @return Listener
	 */
	public Listener getListener() {
		return this.listener;
	}

	/**
	 * Set the current listener
	 *
	 * @param listener Listener
	 */
	public void setListener(Listener listener) {
		this.listener = listener;

		if(this.listener instanceof Listener.Presentation) {
			this.presentationListener = (Listener.Presentation)this.listener;
		}
	}

	/**
	 * Get the title of the action sheet
	 *
	 * @return Action sheet title
	 */
	public CharSequence getTitle() {
		return this.title;
	}

	/**
	 * Set the action sheet title
	 *
	 * @param title Action sheet title
	 */
	public void setTitle(CharSequence title) {
		this.title = title;
	}

	/**
	 * Add a button and return it's index
	 *
	 * @param title Button title
	 * @return Index of the button added
	 */
	public int addButton(CharSequence title) {
		this.otherButtonTitles.add(title);
		return this.otherButtonTitles.size() - 1;
	}

	/**
	 * Get the title of the button at an index
	 *
	 * @param index Index of the button to get the title from
	 * @return Button title index
	 */
	public CharSequence getButtonTitle(int index) {
		if(index == this.CANCEL_BUTTON_INDEX) {
			return this.cancelButtonTitle;
		} else {
			return this.otherButtonTitles.get(index);
		}
	}

	/**
	 * Get the style of the action sheet
	 *
	 * @return Action sheet style
	 */
	public Style getStyle() {
		return style;
	}

	/**
	 * Set the style of the action sheet
	 *
	 * @param style Action sheet style
	 */
	public void setStyle(Style style) {
		this.style = style;
	}

	/**
	 * Check whether or not the action sheet is currently visible
	 *
	 * @return Is visible
	 */
	public boolean isVisible() {
		return this.alertDialog != null && this.alertDialog.isShowing();
	}

	/**
	 * Get the index of the cancel button
	 *
	 * @return Cancel button index
	 */
	public int getCancelButtonIndex() {
		return this.CANCEL_BUTTON_INDEX;
	}

	/**
	 * Show the action sheet from a bar button item
	 *
	 * @param item Bar button item to show from
	 * @param animated Whether or not the presentation is animated
	 */
	public void show(BarButtonItem item, boolean animated) {
		this.show();
	}

	/**
	 * Show the action sheet from a view, around the rect
	 *
	 * @param rect Rect to show from
	 * @param view View to show in
	 * @param animated Whether or not the presentation is animated
	 */
	public void show(Rect rect, View view, boolean animated) {
		this.show();
	}

	/**
	 * Show the action sheet from a view
	 *
	 * @param view View to show from
	 */
	public void show(View view) {
		this.show();
	}

	/**
	 * Show the actions heet
	 */
	private void show() {
		AlertDialog.Builder builder = new AlertDialog.Builder(Application.sharedApplication().getContext(), this.style == Style.LIGHT ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK);

		if(this.title != null) {
			builder.setTitle(this.title);
		}

		if(this.cancelButtonTitle != null) {
			builder.setNegativeButton(this.cancelButtonTitle, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int which) {
					dialogInterface.cancel();
				}
			});

			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialogInterface) {
					if(listener != null) {
						_dismissWithClickedButtonIndex(CANCEL_BUTTON_INDEX);
					}
				}
			});
		} else {
			builder.setCancelable(false);
		}

		if(this.otherButtonTitles.size() > 0) {
			CharSequence[] otherButtonTitles = this.otherButtonTitles.toArray(new CharSequence[this.otherButtonTitles.size()]);
			builder.setItems(otherButtonTitles, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int which) {
					_dismissWithClickedButtonIndex(which);
				}
			});
		}

		this.alertDialog = builder.create();

		AlertListener alertListener = new AlertListener();
		this.alertDialog.setOnShowListener(alertListener);
		this.alertDialog.setOnDismissListener(alertListener);

		if(this.presentationListener != null) {
			this.presentationListener.willPresentActionSheet(this);
		}

		this.alertDialog.show();
	}

	/**
	 * Dismiss the action sheet with a specified button index
	 * This call is not needed if the user presses a button
	 *
	 * @param buttonIndex Button index
	 */
	public void dismissWithClickedButtonIndex(int buttonIndex) {
		if(this.isVisible()) {
			_dismissWithClickedButtonIndex(buttonIndex);
			this.alertDialog.dismiss();
		}
	}

	private void _dismissWithClickedButtonIndex(int buttonIndex) {
		if(this.listener != null) {
			if(buttonIndex == CANCEL_BUTTON_INDEX) {
				this.listener.onCancel(this);
			} else {
				this.listener.onClickedButtonAtIndex(this, buttonIndex);
			}
		}

		if(this.presentationListener != null) {
			this.presentationListener.willDismissWithButtonIndex(this, buttonIndex);
		}

		this.dismissedWithButtonIndex = buttonIndex;
	}

	private class OnClickListener implements android.content.DialogInterface.OnClickListener {
		private int which;

		private OnClickListener(int which) {
			this.which = which;
		}

		public void onClick(DialogInterface dialogInterface, int which) {
			_dismissWithClickedButtonIndex(this.which);
		}
	}

	private class AlertListener implements DialogInterface.OnDismissListener, DialogInterface.OnShowListener {
		public void onDismiss(DialogInterface dialogInterface) {
			if(presentationListener != null) {
				presentationListener.didDismissWithButtonIndex(ActionSheet.this, dismissedWithButtonIndex);
			}

			alertDialog = null;
		}

		public void onShow(DialogInterface dialogInterface) {
			if(presentationListener != null) {
				presentationListener.didPresentActionSheet(ActionSheet.this);
			}
		}
	}

}
