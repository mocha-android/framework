/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
import mocha.foundation.MObject;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.regex.Pattern;

// TODO: Build out!

public final class Device extends MObject {

	public static final Device INSTANCE = new Device();

	public enum UserInterfaceIdiom {
		PHONE,
		TABLET
	}

	private UserInterfaceIdiom userInterfaceIdiom;
	private int numberOfCores = -1;
	private String uniqueIdentifier;

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

	/**
	 * Gets the total number of cores on the device
	 *
	 * @see http://stackoverflow.com/a/10377934
	 * @return Number of cores
	 */
	public int getNumberOfCores() {
		if(this.numberOfCores == -1) {
			try {
				// Get directory containing CPU info
				File dir = new File("/sys/devices/system/cpu/");
				// Filter to only list the devices we care about
				File[] files = dir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						// Check if filename is "cpu", followed by a single digit number
						return Pattern.matches("cpu[0-9]+", file.getName());
					}
				});
				// Return the number of cores (virtual CPU devices)
				this.numberOfCores = files.length;
			} catch(Exception e) {
				// Most likely always going to be 1, but better than nothing.
				this.numberOfCores = Runtime.getRuntime().availableProcessors();
				MLogException(e, "Unable to detect number of cores");
			}

		}

		return this.numberOfCores;
	}

	public String getUniqueIdentifier() {
		if(this.uniqueIdentifier == null) {
			this.uniqueIdentifier = Settings.Secure.getString(Application.sharedApplication().getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
		}

		return this.uniqueIdentifier;
	}

	public String getSystemVersion() {
		return Build.VERSION.RELEASE;
	}

	public String getModel() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;

		if (model.startsWith(manufacturer)) {
			return model;
		} else {
			return manufacturer + " " + model;
		}
	}


}
