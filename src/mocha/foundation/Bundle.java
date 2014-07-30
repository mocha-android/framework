/**
 *  @author Shaun
 *  @date 3/10/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.foundation;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import mocha.graphics.Image;
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
	 * Gets the contents of an asset file as a string
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
	 * Gets the contents of an asset file as an image
	 *
	 * @param assetName Name of the asset to read
	 * @param scale Scale of the image
	 * @return Asset contents
	 */
	public Image getImageFromAssets(String assetName, float scale) {
		try {
			InputStream is = this.application.getContext().getAssets().open(assetName);
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			is.close();

			if(bitmap != null) {
				int density;

				if(scale >= 1.5) {
					if (scale >= 2.0f) {
						if (scale >= 3.0f) {
							density = DisplayMetrics.DENSITY_XXHIGH;
						} else {
							density = DisplayMetrics.DENSITY_XHIGH;
						}
					} else {
						density = DisplayMetrics.DENSITY_HIGH;
					}
				} else if(scale < 1.0f) {
					density = DisplayMetrics.DENSITY_LOW;
				} else {
					density = DisplayMetrics.DENSITY_MEDIUM;
				}

				bitmap.setDensity(density);
				return new Image(bitmap);
			} else {
				return null;
			}
		} catch (IOException e) {
			MWarn(e, "Couldn't read asset " + assetName);
			return null;
		}
	}

	/**
	 * Gets the name of the package
	 * @return Name
	 */
	public String getName() {
		return this.application.getContext().getPackageName();
	}

	/**
	 * Gets the version of the package
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
