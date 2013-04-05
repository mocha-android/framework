/**
 *  @author Shaun
 *  @date 3/10/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.foundation;

import android.content.pm.PackageManager;
import mocha.ui.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Bundle extends mocha.foundation.Object {

	private Application application;

	public Bundle(Application application) {
		this.application = application;
	}

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

	public String getName() {
		return this.application.getContext().getPackageName();
	}

	public String getVersion() {
		try {
			return this.application.getContext().getPackageManager().getPackageInfo(this.getName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			return null;
		}
	}

}
