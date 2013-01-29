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

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		this.windows = new ArrayList<Window>();
		Screen.setupMainScreen(this);
	}

	void addWindow(Window window) {
		this.windows.add(window);
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
