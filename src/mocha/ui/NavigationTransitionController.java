/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Rect;

abstract public class NavigationTransitionController {
	private NavigationController navigationController;

	/**
	 * Initialize with the navigation controller that owns it
	 *
	 * @param navigationController Navigation Controller
	 */
	public NavigationTransitionController(NavigationController navigationController) {
		this.navigationController = navigationController;
	}

	/**
	 * Handle the animation between the from and to view controllers.
	 * Guaranteed to have both a from and to view controller, and we're
	 * always animated if we're at this point.
	 *
	 * @param fromViewController View controller transitioning from
	 * @param toViewController   View controller transition to
	 * @param push               Whether or not we're adding the view controller, or removing it
	 * @param completion         Callback to run upon completion, may be null
	 */
	abstract public void transitionFromViewController(ViewController fromViewController, ViewController toViewController, boolean push, Runnable completion);

	/**
	 * The navigation controller that owns this transition controller
	 *
	 * @return Navigation controller
	 */
	public NavigationController getNavigationController() {
		return this.navigationController;
	}


	protected void pushNavigationItem(NavigationItem navigationItem, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		NavigationBar navigationBar = this.navigationController.getNavigationBar();

		NavigationBar.Delegate delegate = navigationBar.getDelegate();
		navigationBar.setDelegate(null);

		navigationBar.pushNavigationItem(navigationItem, animated, additionalTransitions, transitionCompleteCallback);

		navigationBar.setDelegate(delegate);
	}

	protected void popToNavigationItemAnimated(NavigationItem navigationItem, boolean animated, Runnable additionalTransitions, Runnable transitionCompleteCallback) {
		NavigationBar navigationBar = this.navigationController.getNavigationBar();

		NavigationBar.Delegate delegate = navigationBar.getDelegate();
		navigationBar.setDelegate(null);

		navigationBar.popToNavigationItemAnimated(navigationItem, animated, additionalTransitions, transitionCompleteCallback);

		navigationBar.setDelegate(delegate);
	}

	protected Rect getContentBounds() {
		return this.navigationController.getContentBounds();
	}

	protected void setTopView(View view) {
		this.navigationController.setTopView(view);
	}

}
