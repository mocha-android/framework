/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.res.Configuration;
import android.view.WindowManager;
import mocha.foundation.MObject;
import mocha.foundation.NotificationCenter;

import java.util.ArrayList;
import java.util.List;

public class Activity extends android.app.Activity {
	private List<Window> windows;
	private Application application;
	private boolean setup;
	private boolean hasPreviouslyLaunched;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		this.setTheme(android.R.style.Theme_Holo);
		super.onCreate(savedInstanceState);

		if(!this.setup) {
			this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			this.windows = new ArrayList<Window>();
			Screen.setupMainScreen(this);

			this.application = new Application(this);
			Application.setSharedApplication(this.application);

			this.setup = true;
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.windows.get(0).makeKeyAndVisible();
	}

	void addWindow(Window window) {
		this.windows.add(window);
	}

	List<Window> getWindows() {
		return this.windows;
	}

	public void onBackPressed() {
		MObject.MLog("Back key pressed");
		if(this.application.isIgnoringInteractionEvents()) return;

		if(this.windows.size() > 0) {
			Responder responder = this.windows.get(0).getFirstResponder();

			if(responder != null) {
				MObject.MLog("Back key pressed with first responder: %s", responder);
				responder.backKeyPressed(Event.systemEvent(this.windows.get(0)));
				return;
			}
		}

		super.onBackPressed();
	}

	void backKeyPressed(Event event) {
		MObject.MLog("Back key event finished, calling super");
		super.onBackPressed();
	}

	protected void onPause() {
		super.onPause();

		NotificationCenter.defaultCenter().post(Application.WILL_RESIGN_ACTIVE_NOTIFICATION, this.application);

		for(Window window : this.windows) {
			window.onPause();
		}
	}

	protected void onResume() {
		super.onResume();

		for(Window window : this.windows) {
			window.onResume();
		}

		if(this.hasPreviouslyLaunched) {
			NotificationCenter.defaultCenter().post(Application.DID_BECOME_ACTIVE_NOTIFICATION, this.application);
		} else {
			this.hasPreviouslyLaunched = true;
		}
	}

	public void onLowMemory() {
		super.onLowMemory();

		NotificationCenter.defaultCenter().post(Application.DID_RECEIVE_MEMORY_WARNING_NOTIFICATION, this.application);
	}

	protected void onDestroy() {
		NotificationCenter.defaultCenter().post(Application.WILL_TERMINATE_NOTIFICATION, this.application);

		if(Application.sharedApplication() == this.application) {
			Application.setSharedApplication(null);
			this.application = null;
		}

		super.onDestroy();

		// Yes, this goes against Android everything stands for. However, Mocha is not Activity based, we use a
		// single activity to run the entire app, so in our case, when the single Activity (app) is destroyed,
		// we want the actual process to be killed as well and not reused the next time the app opens.
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}