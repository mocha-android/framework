/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import android.view.WindowManager;

import java.util.ArrayList;

public class Activity extends android.app.Activity {
	private ArrayList<Window> windows;
	private Application application;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		this.windows = new ArrayList<Window>();
		Screen.setupMainScreen(this);

		this.application = new Application(this);
		Application.setSharedApplication(this.application);
	}

	void addWindow(Window window) {
		this.windows.add(window);
	}

	public void onBackPressed() {
		mocha.foundation.Object.MLog("Back key pressed");
		if(this.application.isIgnoringInteractionEvents()) return;

		if(this.windows.size() > 0) {
			Responder responder = this.windows.get(0).getFirstResponder();

			if(responder != null) {
				mocha.foundation.Object.MLog("Back key pressed with first responder: %s", responder);
				responder.backKeyPressed(Event.systemEvent(this.windows.get(0)));
				return;
			}
		}

		super.onBackPressed();
	}

	void backKeyPressed(Event event) {
		mocha.foundation.Object.MLog("Back key event finished, calling super");
		super.onBackPressed();
	}

	protected void onPause() {
		super.onPause();

		for(Window window : this.windows) {
			window.onPause();
		}
	}

	protected void onResume() {
		super.onResume();

		for(Window window : this.windows) {
			window.onResume();
		}
	}
}
