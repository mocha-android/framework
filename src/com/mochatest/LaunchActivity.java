/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest;

import android.os.Bundle;
import com.mochatest.controllers.RootViewController;
import mocha.ui.Activity;
import mocha.ui.NavigationController;
import mocha.ui.Window;

public class LaunchActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		NavigationController navigationController = new NavigationController(new RootViewController());

		Window window = new Window(this);
		window.setRootViewController(navigationController);
		window.makeKeyAndVisible();
	}

}
