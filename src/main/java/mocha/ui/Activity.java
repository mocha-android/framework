/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import mocha.foundation.Lists;
import mocha.foundation.MObject;
import mocha.foundation.NotificationCenter;

import java.util.ArrayList;
import java.util.List;

public class Activity extends android.app.Activity {
	private List<Window> windows;
	private Application application;
	private boolean setup;
	private boolean hasPreviouslyLaunched;
	private Configuration currentConfiguration;
	private InterfaceOrientation currentOrientation;
	private int currentRotation;

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		this.setTheme(android.R.style.Theme_Holo);
		super.onCreate(savedInstanceState);

		if(!this.setup) {
			this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			this.windows = new ArrayList<>();
			Screen.setupMainScreen(this);

			this.application = new Application(this);
			Application.setSharedApplication(this.application);

			this.setup = true;
		}

		this.setCurrentConfiguration(this.getResources().getConfiguration());
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		Application.Delegate appDelegate = this.application.getDelegate();

		if(appDelegate != null) {
			appDelegate.didFinishLaunchingWithOptions(this.application, null);
		}
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Uri uri = intent.getData();
		if(uri != null) {
			Application.Delegate delegate = this.application.getDelegate();

			if (delegate != null) {
				delegate.handleOpenUri(this.application, uri, null, null);
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(this.currentConfiguration.orientation != newConfig.orientation || this.currentRotation != this.getWindowRotation()) {
			final InterfaceOrientation fromInterfaceOrientation = this.currentOrientation;
			final List<Window> windows = Lists.copy(this.windows);
			final Window keyWindow = Lists.last(windows);

			for(Window window : windows) {
				window.willRotateToInterfaceOrientation(this.application.getStatusBarOrientation());
			}

			super.onConfigurationChanged(newConfig);

			if(keyWindow != null) {
				keyWindow.makeKeyAndVisible();
			}

			if(this.currentConfiguration.orientation != newConfig.orientation) {
				MObject.performOnMainAfterDelay(0, new Runnable() {
					public void run() {
						for(Window window : windows) {
							window.didRotateFromInterfaceOrientation(fromInterfaceOrientation);
						}
					}
				});
			}
		} else {
			super.onConfigurationChanged(newConfig);
		}

		this.setCurrentConfiguration(newConfig);
	}

	/**
	 * Default implementation sets this activity's
	 * content view to the keyWindow's native view.
	 *
	 * Most apps should not need to override this method.
	 *
	 * @param keyWindow Key window to present
	 */
	protected void presentKeyWindow(Window keyWindow) {
		this.setContentView(keyWindow.getLayer().getNativeView());
	}

	private void setCurrentConfiguration(Configuration configuration) {
		this.currentConfiguration = new Configuration(configuration);
		this.currentOrientation = this.application.getStatusBarOrientation();
		this.currentRotation = this.getWindowRotation();
	}

	protected Configuration getCurrentConfiguration() {
		return currentConfiguration;
	}

	private int getWindowRotation() {
		return ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	}

	void addWindow(Window window) {
		this.windows.add(window);
	}

	List<Window> getWindows() {
		return this.windows;
	}

	@Override
	public void onBackPressed() {
		if(this.application.isIgnoringInteractionEvents()) return;

		if(this.windows.size() > 0) {
			final ViewController rootViewController = this.windows.get(0).getRootViewController();

			if(rootViewController != null) {
				ViewController viewController = rootViewController;

				while(viewController.getPresentedViewController() != null) {
					viewController = viewController.getPresentedViewController();
				}

				viewController.dispatchBackKeyPressed(Event.systemEvent(this.windows.get(0)));
				return;
			}
		}

		super.onBackPressed();
	}

	void backKeyPressed(Event event) {
		MObject.MLog("Back key event finished, calling super");
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();

		NotificationCenter.defaultCenter().post(Application.WILL_RESIGN_ACTIVE_NOTIFICATION, this.application);

		for(Window window : this.windows) {
			window.onPause();
		}

		this.application.setState(Application.State.BACKGROUND);
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.application.setState(Application.State.ACTIVE);

		for(Window window : this.windows) {
			window.onResume();
		}

		if(this.hasPreviouslyLaunched) {
			NotificationCenter.defaultCenter().post(Application.DID_BECOME_ACTIVE_NOTIFICATION, this.application);
		} else {
			this.hasPreviouslyLaunched = true;
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		NotificationCenter.defaultCenter().post(Application.DID_RECEIVE_MEMORY_WARNING_NOTIFICATION, this.application);
	}

	@Override
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