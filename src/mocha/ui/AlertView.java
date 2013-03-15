/**
 *  @author Shaun
 *  @date 3/10/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.*;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AlertView extends mocha.foundation.Object {

	public interface Listener {
		/**
		 * Called when the button is clicked, before the dialog
		 * is dismissed.
		 *
		 * @param alertView Alert view
		 * @param buttonIndex Button index that clicked (guaranteed not to be cancel button index)
		 */
		public void onClickedButtonAtIndex(AlertView alertView, int buttonIndex);

		/**
		 * Called when the dialog was cancelled
		 *
		 * @param alertView Alert view
		 */
		public void onCancel(AlertView alertView);


		public interface Presentation extends Listener {
			/**
			 * Called before the alert view appears on screen
			 *
			 * @param alertView Alert view
			 */
			public void willPresentAlertView(AlertView alertView);

			/**
			 * Calls after the alert view appears on screen
			 *
			 * @param alertView Alert view
			 */
			public void didPresentAlertView(AlertView alertView);

			/**
			 * Called before the alert view dismisses from screen, after Listener#onClickedButtonAtIndex is called.
			 *
			 * @param alertView Alert view
			 * @param buttonIndex Button index that was clicked causing the dismisall (could be cancel button index)
			 */
			public void willDismissWithButtonIndex(AlertView alertView, int buttonIndex);

			/**
			 * Called after the alert view is removed from the screen.
			 *
			 * @param alertView Alert view
			 * @param buttonIndex Button index that was clicked causing the dismisall (could be cancel button index)
			 */
			public void didDismissWithButtonIndex(AlertView alertView, int buttonIndex);
		}
	}

	public enum Style {
		LIGHT, DARK
	}

	private Listener listener;
	private Listener.Presentation presentationListener;
	private CharSequence title;
	private CharSequence message;
	private CharSequence cancelButtonTitle;
	private List<CharSequence> otherButtonTitles;
	private AlertDialog alertDialog;
	private Style style;
	private boolean destructive;
	private int dismissedWithButtonIndex;
	private final int cancelButtonIndex = -1;

	/**
	 * Create an alert view
	 *
	 * @param title Title of the alert view
	 * @param message Message body of the alert view (will not appear if there are more than 3 total buttons, including cancel)
	 * @param listener Listener for the alert
	 * @param cancelButtonTitle Cancel button title, may be null
	 * @param otherButtonTitles Other button titles, may be null
	 */
	public AlertView(CharSequence title, CharSequence message, Listener listener, CharSequence cancelButtonTitle, CharSequence... otherButtonTitles) {
		this.style = Style.DARK;
		this.setListener(listener);

		this.title = title;
		this.message = message;
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
	 * Get the title of the alert
	 *
	 * @return Alert title
	 */
	public CharSequence getTitle() {
		return this.title;
	}

	/**
	 * Set the alert title
	 *
	 * @param title Alert title
	 */
	public void setTitle(CharSequence title) {
		this.title = title;
	}

	/**
	 * Get the alert message body
	 *
	 * @return Alert message body
	 */
	public CharSequence getMessage() {
		return this.message;
	}

	/**
	 * Set the alert message body
	 *
	 * Note: This will not appear if there are more than 3 total buttons, including the cancel button.
	 *
	 * @param message alert message body
	 */
	public void setMessage(CharSequence message) {
		this.message = message;
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
		if(index == this.cancelButtonIndex) {
			return this.cancelButtonTitle;
		} else {
			return this.otherButtonTitles.get(index);
		}
	}

	/**
	 * Get the style of the alert
	 *
	 * @return Alert style
	 */
	public Style getStyle() {
		return style;
	}

	/**
	 * Set the style of the alert
	 *
	 * @param style Alert style
	 */
	public void setStyle(Style style) {
		this.style = style;
	}

	/**
	 * Check whether or not the view alert is currently visible
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
		return this.cancelButtonIndex;
	}

	/**
	 * Get whether or not an alert is destructive.
	 * If the alert is destructive, a warning icon will be set with the title.
	 *
	 * @return Is alert destructive
	 */
	public boolean isDestructive() {
		return destructive;
	}

	/**
	 * Set whether or not an alert is destructive.
	 * If the alert is destructive, a warning icon will be set with the title.
	 *
	 * @param destructive Is alert destructive
	 */
	public void setDestructive(boolean destructive) {
		this.destructive = destructive;
	}

	/**
	 * Show the alert
	 */
	public void show() {
		AlertDialog.Builder builder = new AlertDialog.Builder(Application.sharedApplication().getContext(), this.style == Style.LIGHT ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK);

		if(this.title != null) {
			builder.setTitle(this.title);
		}

		if(this.message != null) {
			builder.setMessage(this.message);
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
						_dismissWithClickedButtonIndex(cancelButtonIndex);
					}
				}
			});
		} else {
			builder.setCancelable(false);
		}

		if(this.otherButtonTitles.size() > 0) {
			if(this.otherButtonTitles.size() == 1) {
				builder.setPositiveButton(this.otherButtonTitles.get(0), new OnClickListener(0));
			} else if(this.otherButtonTitles.size() == 2) {
				builder.setNeutralButton(this.otherButtonTitles.get(0), new OnClickListener(0));
				builder.setPositiveButton(this.otherButtonTitles.get(1), new OnClickListener(1));
			} else {
				CharSequence[] otherButtonTitles = this.otherButtonTitles.toArray(new CharSequence[this.otherButtonTitles.size()]);
				builder.setMessage(null);
				builder.setItems(otherButtonTitles, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int which) {
						_dismissWithClickedButtonIndex(which);
					}
				});
			}
		}

		if(this.destructive) {
			builder.setIcon(android.R.drawable.ic_dialog_alert);
		}

		this.alertDialog = builder.create();

		AlertListener alertListener = new AlertListener();
		this.alertDialog.setOnShowListener(alertListener);
		this.alertDialog.setOnDismissListener(alertListener);

		if(this.presentationListener != null) {
			this.presentationListener.willPresentAlertView(this);
		}

		this.alertDialog.show();
	}

	/**
	 * Dismiss the alert view with a specified button index
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
			if(buttonIndex == this.cancelButtonIndex) {
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
				presentationListener.didDismissWithButtonIndex(AlertView.this, dismissedWithButtonIndex);
			}

			alertDialog = null;
		}

		public void onShow(DialogInterface dialogInterface) {
			if(presentationListener != null) {
				presentationListener.didPresentAlertView(AlertView.this);
			}
		}
	}

}