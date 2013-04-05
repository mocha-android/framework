/*
 *  @author Shaun
 *  @date 2/15/13
 *  @copyright  2013 TV Guide, Inc. All rights reserved.
 */
package mocha.graphics;

import java.util.Arrays;

public class Gradient {
	public final int[] colors;
	public final float[] locations;

	public enum DrawingOptions {
		DRAWS_BEFORE_START_LOCATION,
		DRAWS_AFTER_END_LOCATION
	}

	public Gradient(int[] colors, float[] locations) {
		if(colors != null) {
			this.colors = Arrays.copyOf(colors, colors.length);
		} else {
			this.colors = null;
		}

		if(locations != null) {
			this.locations = Arrays.copyOf(locations, locations.length);
		} else {
			this.locations = null;
		}
	}

}
