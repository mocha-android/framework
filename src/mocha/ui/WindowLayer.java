/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 enormego. All rights reserved.
 */

package mocha.ui;

interface WindowLayer extends ViewLayer {

	android.view.View getNativeView();

	void onWindowPause();
	void onWindowResume();

}
