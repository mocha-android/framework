/**
 *  @author Shaun
 *  @date 5/15/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

abstract class NavigationTransitionController {
	private NavigationController navigationController;

	/**
	 * Initialize with the navigation controller that owns it
	 *
	 * @param navigationController Navigation Controller
	 */
	NavigationTransitionController(NavigationController navigationController) {
		this.navigationController = navigationController;
	}

	/**
	 * Handle the animation between the from and to view controllers.
	 * Guaranteed to have both a from and to view controller, and we're
	 * always animated if we're at this point.
	 *
	 * @param fromViewController View controller transitioning from
	 * @param toViewController View controller transition to
	 * @param push Whether or not we're adding the view controller, or removing it
	 * @param completion Callback to run upon completion, may be null
	 */
	abstract void transitionFromViewController(ViewController fromViewController, ViewController toViewController, boolean push, Runnable completion);

	/**
	 * The navigation controller that owns this transition controller
	 *
	 * @return Navigation controller
	 */
	NavigationController getNavigationController() {
		return this.navigationController;
	}
}
