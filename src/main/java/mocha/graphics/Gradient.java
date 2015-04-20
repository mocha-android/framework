package mocha.graphics;

import java.util.Arrays;
import java.util.List;

public class Gradient {
	public final int[] colors;
	public final float[] locations;

	public enum DrawingOptions {
		DRAWS_BEFORE_START_LOCATION,
		DRAWS_AFTER_END_LOCATION
	}

	public Gradient(int[] colors, float[] locations) {
		if (colors != null) {
			this.colors = Arrays.copyOf(colors, colors.length);
		} else {
			this.colors = null;
		}

		if (locations != null) {
			this.locations = Arrays.copyOf(locations, locations.length);
		} else {
			this.locations = null;
		}
	}

	public Gradient(List<Integer> colors, List<Float> locations) {
		if (colors != null) {
			this.colors = new int[colors.size()];

			for (int i = 0; i < this.colors.length; i++) {
				this.colors[i] = colors.get(i);
			}
		} else {
			this.colors = null;
		}

		if (locations != null) {
			this.locations = new float[locations.size()];

			for (int i = 0; i < this.locations.length; i++) {
				this.locations[i] = locations.get(i);
			}
		} else {
			this.locations = null;
		}
	}

}
