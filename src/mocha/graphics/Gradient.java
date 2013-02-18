/*
 *  @author Shaun
 *  @date 2/15/13
 *  @copyright  2013 TV Guide, Inc. All rights reserved.
 */
package mocha.graphics;

public class Gradient {
	public final int[] colors;
	public final float[] locations;

	public enum DrawingOptions {
		DRAWS_BEFORE_START_LOCATION,
		DRAWS_AFTER_END_LOCATION
	}

	public Gradient(int[] colors, float[] locations) {
		this.colors = colors;
		this.locations = locations;
	}

}
