/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.res.Configuration;

// TODO: Build out!

public final class Device {

	public static final Device INSTANCE = new Device();

	public enum UserInterfaceIdiom {
		PHONE,
		TABLET
	}

	private UserInterfaceIdiom userInterfaceIdiom;

	public static Device get() {
		return INSTANCE;
	}

	private Device() {
		android.content.Context context = Application.sharedApplication().getContext();
		this.userInterfaceIdiom = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE ? UserInterfaceIdiom.TABLET : UserInterfaceIdiom.PHONE;
	}


	public UserInterfaceIdiom getUserInterfaceIdiom() {
		return this.userInterfaceIdiom;
	}

}
