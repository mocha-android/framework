/**
 *  @author Shaun
 *  @date 4/21/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.graphics;

import mocha.foundation.Copying;
import mocha.ui.Color;

public class Shadow implements Copying<Shadow> {

	private Offset offset;
	private float blurRadius;
	private int color;

	public Shadow() {
		this.offset = Offset.zero();
		this.blurRadius = 0.0f;
		this.color = Color.white(0.0f, 1.0f / 3.0f);
	}

	public Shadow(Offset offset, float blurRadius, int color) {
		this();

		this.offset.set(offset);
		this.blurRadius = blurRadius;
		this.color = color;
	}

	public Offset getOffset() {
		return this.offset.copy();
	}

	public void setOffset(Offset offset) {
		this.offset.set(offset);
	}

	public float getBlurRadius() {
		return this.blurRadius;
	}

	public void setBlurRadius(float blurRadius) {
		this.blurRadius = blurRadius;
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public Shadow copy() {
		return new Shadow(this.offset, this.blurRadius, this.color);
	}
}
