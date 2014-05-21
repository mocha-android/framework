/*
 *  @author Shaun
 *	@date 1/31/13
 *	@copyright	2013 Mocha. All rights reserved.
 */

package mocha.ui;

public interface WindowLayer extends ViewLayer {

	public android.view.View getNativeView();

	void onWindowPause();
	void onWindowResume();

}
