/*
 *  @author Shaun
 *	@date 11/15/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.AffineTransform;
import mocha.graphics.Rect;

import java.util.List;

public interface ViewLayer {

	public android.content.Context getContext();

	public void setView(View view);
	public View getView();

	public void setSupportsDrawing(boolean supportsDrawing);
	public void setBackgroundColor(int backgroundColor);

	public void setClipsToBounds(boolean clipsToBounds);
	public boolean clipsToBounds();

	// public void setFrame(Rect frame);
	public void setFrame(Rect frame, Rect bounds);

	public void setBounds(Rect bounds);
	public Rect getBounds();

	public boolean isHidden();
	public void setHidden(boolean hidden);

	public float getAlpha();
	public void setAlpha(float alpha);

	public void setNeedsLayout();

	public void setNeedsDisplay();
	public void setNeedsDisplay(Rect dirtyRect);

	public void addSublayer(ViewLayer layer);
	public void insertSublayerAtIndex(ViewLayer layer, int index);
	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling);
	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling);

	public void setTransform(AffineTransform transform);
	public AffineTransform getTransform();

	public void didMoveToSuperlayer();

	public List<ViewLayer> getSublayers();

	public ViewLayer getSuperlayer();
	public void removeFromSuperlayer();

	public class InvalidSubLayerClassException extends RuntimeException {
		public InvalidSubLayerClassException(ViewLayer parent, ViewLayer child) {
			super(parent.getClass().getCanonicalName() + " does not support sub layers for class: " + child.getClass().getCanonicalName());
		}
	}
}
