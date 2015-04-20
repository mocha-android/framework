package mocha.ui;

import android.view.ViewGroup;
import mocha.graphics.*;

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

	public int getShadowColor();

	public void setShadowColor(int shadowColor);

	public float getShadowOpacity();

	public void setShadowOpacity(float shadowOpacity);

	public Size getShadowOffset();

	public void setShadowOffset(Size shadowOffset);

	public float getShadowRadius();

	public void setShadowRadius(float shadowRadius);

	public Path getShadowPath();

	public void setShadowPath(Path shadowPath);

	public float getCornerRadius();

	public void setCornerRadius(float cornerRadius);

	public float getBorderWidth();

	public void setBorderWidth(float borderWidth);

	public int getBorderColor();

	public void setBorderColor(int shadowColor);

	public void renderInContext(Context context);

	public float getZPosition();

	public void setZPosition(float zPosition);

	public ViewGroup getViewGroup(); // May return null

	public class InvalidSubLayerClassException extends RuntimeException {
		public InvalidSubLayerClassException(ViewLayer parent, ViewLayer child) {
			super(parent.getClass().getCanonicalName() + " does not support sub layers for class: " + child.getClass().getCanonicalName());
		}
	}

}
