package mocha.ui;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

/**
 * NativeView allows you to bind "native" Android views into Mocha's view hierarchy.
 * <p/>
 * <p>Mocha has it's own view hierarchy and touch delivery system, which means Android View's can
 * not by added to it directly. NativeView helps bridge the gap when working with third party
 * classes built for Android, but not Mocha.</p>
 * <p/>
 * {@important While almost all Mocha functionality works as you'd expect any other view inside of Mocha to,
 * the big exception is {@link GestureRecognizer}. At this time, Mocha does not support attaching a {@link GestureRecognizer}
 * to a NativeView.}
 *
 * @param <V> Android view subclass
 *
 * @example <pre> NativeView&lt;com.thirdparty.SomeView&gt; someView = new NativeView&lt;com.thirdparty.SomeView&gt;(new com.thirdparty.SomeView(Application.sharedApplication().getContext()));
 * someView.getNativeView().setThirdPartyViewProperty(true);
 * someView.setFrame(new Rect(100.0f, 100.0f, 60.0f, 120.0f));
 * someView.setAlpha(0.7f);
 * otherView.addSubview(someView);</pre>
 */
public class NativeView<V extends android.view.View> extends View {

	private V nativeView;
	boolean trackingTouches;

	// Used internally if we're going to add the native view
	// to the hierarchy somewhere else and NativeView shouldn't
	// touch it's layout
	private boolean unmanagedNativeView;

	public NativeView(V nativeView) {
		if (this.getLayer().getViewGroup() == null) {
			throw new RuntimeException("NativeView currently only works when using ViewLayerNative.");
		}

		this.setNativeView(nativeView);
		this.setUserInteractionEnabled(true);
	}

	@Override
	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		super.setUserInteractionEnabled(userInteractionEnabled);

		if (this.nativeView != null) {
			this.nativeView.setEnabled(userInteractionEnabled);
			this.nativeView.setClickable(userInteractionEnabled);
			this.nativeView.setLongClickable(userInteractionEnabled);
		}
	}

	/**
	 * Replace the underlying nativeView with a new one.
	 *
	 * @param nativeView Native view to set
	 */
	public void setNativeView(V nativeView) {
		if (this.nativeView != null) {
			this.getLayer().getViewGroup().removeView(this.nativeView);
		}

		this.nativeView = nativeView;

		if (this.nativeView != null && !this.unmanagedNativeView) {
			this.nativeView.setOnTouchListener(new android.view.View.OnTouchListener() {
				public boolean onTouch(android.view.View view, MotionEvent motionEvent) {
					if (getWindow().canDeliverToNativeView(NativeView.this, motionEvent, view)) {
						view.onTouchEvent(motionEvent);
					}

					return true;
				}
			});

			this.getLayer().getViewGroup().addView(this.nativeView);
		}
	}

	boolean isUnmanagedNativeView() {
		return unmanagedNativeView;
	}

	void setUnmanagedNativeView(boolean unmanagedNativeView) {
		this.unmanagedNativeView = unmanagedNativeView;

		if (this.unmanagedNativeView) {
			if (this.nativeView != null) {
				this.nativeView.setOnClickListener(null);
				((ViewGroup) this.nativeView.getParent()).removeView(this.nativeView);
			}
		} else {
			// TODO
		}
	}

	/**
	 * Get the underlying native view
	 *
	 * @return Native view
	 */
	public V getNativeView() {
		return nativeView;
	}

	@Override
	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);

		if (this.nativeView != null) {
			this.nativeView.setBackgroundColor(backgroundColor);
		}
	}

	@Override
	public void layoutSubviews() {
		super.layoutSubviews();
		this.updateNativeViewFrame();
	}

	private void updateNativeViewFrame() {
		if (this.nativeView == null || this.unmanagedNativeView) return;

		android.graphics.Rect frame = this.getFrame().toSystemRect(this.scale);
		int width = frame.width();
		int height = frame.height();

		this.nativeView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
	}

	@Override
	public void touchesBegan(List<Touch> touches, Event event) {
		if (this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesBegan(touches, event);
		}
	}

	@Override
	public void touchesMoved(List<Touch> touches, Event event) {
		if (this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesMoved(touches, event);
		}
	}

	@Override
	public void touchesEnded(List<Touch> touches, Event event) {
		if (this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesEnded(touches, event);
		}
	}

	@Override
	public void touchesCancelled(List<Touch> touches, Event event) {
		if (this.nativeView == null || !this.nativeView.onTouchEvent(event.getMotionEvent())) {
			super.touchesEnded(touches, event);
		}
	}

}