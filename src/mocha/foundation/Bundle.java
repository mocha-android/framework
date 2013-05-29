/**
 *  @author Shaun
 *  @date 3/10/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.foundation;

import android.content.pm.PackageManager;
import mocha.ui.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Bundle provides information about the current application.
 *
 * @see mocha.ui.Application#getBundle()
 */
public class Bundle extends MObject {

	private Application application;

	/**
	 * Create a bundle for an application
	 *
	 * @param application Application
	 * @hide
	 */
	public Bundle(Application application) {
		this.application = application;
	}

	/**
	 * Get's the contents of an asset file as a string
	 *
	 * @param assetName Name of the asset to read
	 * @return Asset contents
	 */
	public String getStringFromAssets(String assetName) {
		try {
			InputStream is = this.application.getContext().getAssets().open(assetName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			reader.close();
			is.close();

			return sb.toString();
		} catch (IOException e) {
			MWarn(e, "Couldn't read asset " + assetName);
			return null;
		}
	}

	/**
	 * Get's the name of the package
	 * @return Name
	 */
	public String getName() {
		return this.application.getContext().getPackageName();
	}

	/**
	 * Get's the version of the package
	 * @return Version
	 */
	public String getVersion() {
		try {
			return this.application.getContext().getPackageManager().getPackageInfo(this.getName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			return null;
		}
	}

}
