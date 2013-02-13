/**
 *  @author Shaun
 *  @date 2/12/13
 *  @copyright	2013 enormego. All rights reserved.
 */
package mocha.ui;

class BarMetricsStorage<E> {
	private E defaultValue;
	private E landscapePhoneValue;

	public void set(BarMetrics barMetrics, E value) {
		switch (barMetrics) {
			case DEFAULT:
				this.defaultValue = value;
				break;
			case LANDSCAPE_PHONE:
				this.landscapePhoneValue = value;
				break;
		}
	}

	public E get(BarMetrics barMetrics) {
		switch (barMetrics) {
			case LANDSCAPE_PHONE:
				return this.landscapePhoneValue;
			case DEFAULT:
			default:
				return this.defaultValue;
		}
	}
}
