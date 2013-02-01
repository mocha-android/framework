/*
 *  @author Shaun
 *	@date 11/28/12
 *	@copyright	2012 enormego All rights reserved.
 */
package mocha.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLUtils;
import mocha.foundation.Benchmark;
import mocha.graphics.Context;
import mocha.graphics.Point;
import mocha.graphics.Rect;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewLayerGL extends mocha.foundation.Object implements ViewLayer {
	private static boolean ignoreLayout;

	private android.content.Context context;
	private View view;
	private Rect frame;
	private Rect bounds;
	private boolean hidden;
	private float alpha;
	private final ArrayList<ViewLayerGL> sublayers;
	private List<ViewLayer> sublayersGeneric;
	private ViewLayerGL superlayer;
	private int backgroundColor;
	private boolean supportsDrawing;
	private Texture texture;
	final float scale;
	final int dpi;

	private final short[] indices = { 0, 1, 2, 0, 2, 3 };
	private FloatBuffer vertexBuffer;
	private ShortBuffer indexBuffer;
	private float[] vertices;

	private boolean needsLayout;
	private boolean needsDisplay;

	public ViewLayerGL(android.content.Context context) {
		this.context = context;
		this.alpha = 1.0f;
		this.hidden = false;
		this.sublayers = new ArrayList<ViewLayerGL>();
		this.sublayersGeneric = new ArrayList<ViewLayer>();
		this.scale = context.getResources().getDisplayMetrics().density;
		this.dpi = context.getResources().getDisplayMetrics().densityDpi;

		ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		ibb.order(ByteOrder.nativeOrder());
		indexBuffer = ibb.asShortBuffer();
		indexBuffer.put(indices);
		indexBuffer.position(0);

	}

	private static boolean pushIgnoreLayout() {
		boolean old = ignoreLayout;
		ignoreLayout = true;
		return old;
	}

	private static void popIgnoreLayout(boolean oldValue) {
		ignoreLayout = oldValue;
	}

	boolean layoutSublayersIfNeeded() {
		if(this.needsLayout) {
			this.layoutSublayers();
			return true;
		} else {
			return false;
		}
	}

	void layoutSublayers() {
		if(ignoreLayout) return;
		boolean ignoreLayout = pushIgnoreLayout();
		this.getView()._layoutSubviews();
		popIgnoreLayout(ignoreLayout);
		this.needsLayout = false;
	}

	public void setNeedsLayout() {
		if(!this.needsLayout) {
			this.needsLayout = true;

			WindowLayerGL windowLayer = this.getWindowLayer();
			if(windowLayer != null) {
				windowLayer.scheduleLayout();
			}
		}
	}

	List<ViewLayerGL> getSublayersGL() {
		List<ViewLayerGL> sublayersGL;

		synchronized(this.sublayers) {
			sublayersGL = new ArrayList<ViewLayerGL>(this.sublayers);
		}

		return sublayersGL;
	}

	WindowLayerGL getWindowLayer() {
		if(this.superlayer != null) {
			return superlayer.getWindowLayer();
		} else {
			return null;
		}
	}

	public android.content.Context getContext() {
		return this.context;
	}

	public void setView(View view) {
		this.view = view;
	}

	public View getView() {
		return this.view;
	}

	public void setSupportsDrawing(boolean supportsDrawing) {
		this.supportsDrawing = supportsDrawing;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public void setFrame(Rect frame, Rect bounds) {
		this.setFrame(frame, bounds, true);
	}

	void setFrame(Rect frame, Rect bounds, boolean setNeedsLayout) {
		if(!frame.equals(this.frame)) {
			float minX = frame.origin.x;
			float maxX = frame.maxX();

			float minY = frame.origin.y;
			float maxY = frame.maxY();

			this.vertices = new float[] {
					minX, minY, 0.0f,  // 0, Top Left
					minX, maxY, 0.0f,  // 1, Bottom Left
					maxX, maxY, 0.0f,  // 2, Bottom Right
					maxX, minY, 0.0f,  // 3, Top Right
			};

			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
			byteBuffer.order(ByteOrder.nativeOrder());
			this.vertexBuffer = byteBuffer.asFloatBuffer();
			this.vertexBuffer.put(this.vertices);
			this.vertexBuffer.position(0);

			if(this.texture != null) {
				this.texture.setFrame(frame);
			}
		}

		this.frame = frame.copy();
		this.bounds = bounds.copy();

		if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}
	}

	public void setBounds(Rect bounds) {
		setBounds(bounds, true);
	}

	void setBounds(Rect bounds, boolean setNeedsLayout) {
		Point oldPoint = this.bounds != null ? this.bounds.origin : null;
		this.bounds = bounds.copy();

		if(oldPoint != null && !oldPoint.equals(this.bounds.origin)) {
			boolean ignoreLayout = pushIgnoreLayout();
			this.view._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		} else if(setNeedsLayout) {
			this.view.setNeedsLayout();
		}

	}

	public Rect getBounds() {
		return this.bounds.copy();
	}

	public boolean isHidden() {
		return this.hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public float getAlpha() {
		return this.alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public void setNeedsDisplay() {
		if(!this.needsDisplay) {
			this.needsDisplay = true;

			WindowLayerGL windowLayer = this.getWindowLayer();
			if(windowLayer != null) {
				windowLayer.scheduleLayout();
			}
		}
	}

	public void setNeedsDisplay(Rect dirtyRect) {
		this.setNeedsDisplay();
	}

	public void addSublayer(ViewLayer layer) {
		if(!(layer instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, layer);

		((ViewLayerGL)layer).superlayer = this;

		synchronized(this.sublayers) {
			this.sublayers.add((ViewLayerGL)layer);
		}

		this.sublayersGeneric.add(layer);
	}

	public void insertSublayerAtIndex(ViewLayer layer, int index) {
		if(!(layer instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, layer);

		((ViewLayerGL)layer).superlayer = this;

		synchronized(this.sublayers) {
			this.sublayers.add(index, (ViewLayerGL)layer);
		}

		this.sublayersGeneric.add(index, layer);
	}

	public void insertSublayerBelow(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, sibling);

		int index;
		synchronized(this.sublayers) {
			index = this.sublayers.indexOf(sibling);
		}

		this.insertSublayerAtIndex(layer, index > 0 ? index - 1 : 0);
	}

	public void insertSublayerAbove(ViewLayer layer, ViewLayer sibling) {
		if(!(layer instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, layer);
		if(!(sibling instanceof ViewLayerGL)) throw new InvalidSubLayerClassException(this, sibling);

		int index;

		synchronized(this.sublayers) {
			index = this.sublayers.indexOf(sibling);
		}

		this.insertSublayerAtIndex(layer, index + 1);
	}

	public void didMoveToSuperlayer() {
		View superview = this.getView().getSuperview();

		if(superview != null) {
			boolean ignoreLayout = pushIgnoreLayout();
			this.getView()._layoutSubviews();
			popIgnoreLayout(ignoreLayout);
		}
	}

	public List<ViewLayer> getSublayers() {
		return Collections.unmodifiableList(this.sublayersGeneric);
	}

	public ViewLayer getSuperlayer() {
		return this.superlayer;
	}

	public void removeFromSuperlayer() {
		if(this.superlayer != null) {
			synchronized(this.sublayers) {
				this.superlayer.sublayers.remove(this);
			}

			this.superlayer.sublayersGeneric.remove(this);
			this.superlayer = null;
		}
	}

	//

	void draw(GL10 gl) {
		if(this.hidden) return;

		if(this.backgroundColor != Color.TRANSPARENT) {
			// Counter-clockwise winding.
			gl.glFrontFace(GL10.GL_CCW);
			// Enable face culling.
			gl.glEnable(GL10.GL_CULL_FACE);
			// What faces to remove with the face culling.
			gl.glCullFace(GL10.GL_BACK);

			gl.glColor4f(Color.redf(this.backgroundColor), Color.greenf(this.backgroundColor), Color.bluef(this.backgroundColor), Color.alphaf(this.backgroundColor));

			// Enabled the vertices buffer for writing and to be used during
			// rendering.
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			// Specifies the location and data format of an array of vertex
			// coordinates to use when rendering.
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

			gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_SHORT, indexBuffer);

			// Disable the vertices buffer.
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			// Disable face culling.
			gl.glDisable(GL10.GL_CULL_FACE);
		}

		if(this.supportsDrawing) {
			if(this.needsDisplay || this.texture == null) {
				if(this.texture == null) {
					this.texture = new Texture();
					this.texture.setFrame(this.frame);
				}

				this.texture.update(gl);
				this.needsDisplay = false;
			}

			if(this.texture != null) {
				gl.glColor4f(1.0f, 1.0f, 1.0f, this.alpha);
				this.texture.draw(gl);
			}
		}

		if(this.sublayers.size() > 0) {
			gl.glPushMatrix();

//			if(this.view.clipsToBounds()) {
//				Point point = this.view.convertPointToWindow(Point.zero());
//				Rect frame = (new Rect(point, this.frame.size)).getScaledRect(scale);
//				Point max = frame.max();
//
//				gl.glViewport((int)frame.origin.x, (int)frame.origin.y, (int)Math.ceil(max.x), (int)Math.ceil(max.y));
//
//				gl.glTranslatef(-this.bounds.origin.x, -this.bounds.origin.y, 0.0f);
//			} else {
				gl.glTranslatef((this.frame.origin.x - this.bounds.origin.x), (this.frame.origin.y - this.bounds.origin.y), 0.0f);
//			}

			for(ViewLayerGL layer : this.getSublayersGL()) {
				if(layer != null) {
					layer.draw(gl);
				}
			}

			gl.glPopMatrix();
		}
	}

	class Texture {
		private int id;

		private float vertices[];

		private FloatBuffer vertexBuffer;	// buffer holding the vertices
		private FloatBuffer textureBuffer;	// buffer holding the texture coordinates
		private float texture[] = {
			// Mapping coordinates for the vertices
			0.0f, 1.0f,		// top left		(V2)
			0.0f, 0.0f,		// bottom left	(V1)
			1.0f, 1.0f,		// top right	(V4)
			1.0f, 0.0f		// bottom right	(V3)
		};

		public Texture() {
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
			byteBuffer.order(ByteOrder.nativeOrder());
			textureBuffer = byteBuffer.asFloatBuffer();
			textureBuffer.put(texture);
			textureBuffer.position(0);
		}

		public void setFrame(Rect frame) {
			Rect potFrame = frame.copy();

			Rect scaledFrame = frame.getScaledRect(scale);
			float potWidth = upperPowerOfTwo((int)Math.ceil(scaledFrame.size.width));
			float potHeight = upperPowerOfTwo((int)Math.ceil(scaledFrame.size.height));

			potFrame.size.width *= (potWidth / scaledFrame.size.width);
			potFrame.size.height *= (potHeight / scaledFrame.size.height);

			Point min = potFrame.origin;
			Point max = potFrame.max();

			this.vertices = new float[] {
					min.x, max.y,  0.0f,		// V1 - bottom left
					min.x, min.y,  0.0f,		// V2 - top left
					max.x, max.y,  0.0f,		// V3 - bottom right
					max.x, min.y,  0.0f			// V4 - top right
			};

			// a float has 4 bytes so we allocate for each coordinate 4 bytes
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
			byteBuffer.order(ByteOrder.nativeOrder());

			// allocates the memory from the byte buffer
			vertexBuffer = byteBuffer.asFloatBuffer();

			// fill the vertexBuffer with the vertices
			vertexBuffer.put(vertices);

			// set the cursor position to the beginning of the buffer
			vertexBuffer.position(0);
		}

		public void update(GL10 gl) {
			// MLog("Drawing %s", view);

			Rect scaledFrame = frame.getScaledRect(scale);
			int width = upperPowerOfTwo((int)Math.ceil(scaledFrame.size.width));
			int height = upperPowerOfTwo((int)Math.ceil(scaledFrame.size.height));
			Benchmark benchmark = new Benchmark();
			benchmark.start();

			Bitmap bitmap = width > 0 && height > 0 ? Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) : null;

			if(bitmap != null) {
				benchmark.step("Create bitmap");

				bitmap.setDensity(dpi);
				benchmark.step("Set density");

				Canvas canvas = new Canvas(bitmap);
				// bitmap.eraseColor(0);

				benchmark.step("Create canvas");

				Context context = new Context(canvas, scale);
				benchmark.step("Create context");
				view.draw(context, bounds.copy());
				benchmark.step("Draw");

				int[] id = new int[1];
				gl.glGenTextures(1, id, 0);
				this.id = id[0];

				gl.glBindTexture(GL10.GL_TEXTURE_2D, this.id);

				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
				benchmark.step("Store texture");

				bitmap.recycle();
			} else {
				this.id = -1;
			}

			benchmark.step("Recycle");

			benchmark.end();
			// benchmark.log();
		}

		private int upperPowerOfTwo(int v) {
			v--;
			v |= v >> 1;
			v |= v >> 2;
			v |= v >> 4;
			v |= v >> 8;
			v |= v >> 16;
			v++;
			return v;
		}

		public void draw(GL10 gl) {
			gl.glEnable(GL10.GL_TEXTURE_2D);

			// bind the previously generated texture
			gl.glBindTexture(GL10.GL_TEXTURE_2D, this.id);

			// Point to our buffers
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

			// Set the face rotation
			gl.glFrontFace(GL10.GL_CW);

			// Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

			// Draw the vertices as triangle strip
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

			//Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

			gl.glDisable(GL10.GL_TEXTURE_2D);
		}

		public int getId() {
			return id;
		}
	}
}
