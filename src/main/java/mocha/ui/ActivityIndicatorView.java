package mocha.ui;

// TODO: Build out.

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import mocha.graphics.Rect;

public class ActivityIndicatorView extends View {

	public enum Style {
		WHITE_LARGE,
		WHITE,
		DARK_LARGE,
		DARK,

		/**
		 * Use Style.DARK instead.
		 *
		 * @deprecated
		 */
		GRAY
	}

	private Style style;
	private boolean hidesWhenStopped;
	private NativeView<ProgressBar> progressBar;
	private boolean animating;
	private Interpolator animatingInterpolator;
	private Interpolator stoppedInterpolator;

	/**
	 * Create a new ActivityIndicatorView
	 *
	 * @param style Style of the activity indicator
	 */
	public ActivityIndicatorView(Style style) {
		this.hidesWhenStopped = true;
		this.setHidden(true);
		this.animatingInterpolator = new LinearInterpolator();
		this.stoppedInterpolator = new Interpolator() {
			public float getInterpolation(float v) {
				return 0.5f;
			}
		};


		this.setStyle(style);
		this.sizeToFit();
		this.setBackgroundColor(Color.TRANSPARENT);
	}

	@Override
	public void sizeToFit() {
		Rect frame = this.getFrame();

		if (this.style == Style.WHITE_LARGE || this.style == Style.DARK_LARGE) {
			frame.size.width = 46.0f;
			frame.size.height = 46.0f;
		} else {
			frame.size.width = 28.0f;
			frame.size.height = 28.0f;
		}

		this.setFrame(frame);
	}

	/**
	 * Get the activity indicator style
	 *
	 * @return Activity indicator style
	 */
	public Style getStyle() {
		return style;
	}

	/**
	 * Set the activity indicator style
	 *
	 * @param style Style of the activity indicator
	 */
	public void setStyle(Style style) {
		if (style == null) {
			style = Style.WHITE;
		}

		if (this.style != style) {
			this.style = style;

			if (this.progressBar != null) {
				this.progressBar.removeFromSuperview();
			}

			int nativeStyle;
			switch (style) {
				case WHITE_LARGE:
					nativeStyle = android.R.attr.progressBarStyleLarge;
					break;
				case WHITE:
					nativeStyle = android.R.attr.progressBarStyle;
					break;
				case DARK_LARGE:
					nativeStyle = android.R.attr.progressBarStyleLargeInverse;
					break;
				case DARK:
				case GRAY:
				default:
					nativeStyle = android.R.attr.progressBarStyleInverse;
					break;
			}


			this.progressBar = new NativeView<>(new ProgressBar(Application.sharedApplication().getContext(), null, nativeStyle));
			this.progressBar.getNativeView().setIndeterminate(true);
			this.progressBar.getNativeView().setPadding(0, 0, 0, 0);
			this.progressBar.getNativeView().setInterpolator(this.animating ? this.animatingInterpolator : this.stoppedInterpolator);
			this.progressBar.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
			this.addSubview(this.progressBar);
		}
	}

	@Override
	public void layoutSubviews() {
		super.layoutSubviews();

		if (this.progressBar != null) {
			float inset = this.style == Style.WHITE_LARGE || this.style == Style.DARK_LARGE ? -6.0f : -4.0f;
			this.progressBar.setFrame(this.getBounds().inset(inset, inset));
		}
	}

	/**
	 * Get whether or not this view hides when activity is stopped
	 *
	 * @return if true, the hidden state of this view is automatically toggled
	 * depending on whether or not we're animating.
	 */
	public boolean getHidesWhenStopped() {
		return hidesWhenStopped;
	}

	/**
	 * Set whether or not this view hides when activity is stopped
	 *
	 * @param hidesWhenStopped If true, this view will be automatically set to hidden when it's stopped, and set to visible when it's animating.
	 *                         If false, hidden is unchanged regardless of whether it's stopped or not.
	 */
	public void setHidesWhenStopped(boolean hidesWhenStopped) {
		if (this.hidesWhenStopped != hidesWhenStopped) {
			this.hidesWhenStopped = hidesWhenStopped;

			if (this.hidesWhenStopped && !this.animating) {
				this.setHidden(true);
			} else if (!this.hidesWhenStopped) {
				this.setHidden(false);
			}
		}
	}

	/**
	 * Start the activity animation
	 */
	public void startAnimating() {
		if (this.animating) return;

		if (this.hidesWhenStopped) {
			this.setHidden(false);
		}

		this.progressBar.getNativeView().setInterpolator(this.animatingInterpolator);
		this.progressBar.getNativeView().setVisibility(android.view.View.GONE);
		this.progressBar.getNativeView().setVisibility(android.view.View.VISIBLE);
		this.animating = true;
	}

	/**
	 * Stop the activity animation
	 */
	public void stopAnimating() {
		if (!this.animating) return;

		this.progressBar.getNativeView().setInterpolator(this.stoppedInterpolator);
		this.progressBar.getNativeView().setVisibility(android.view.View.GONE);
		this.progressBar.getNativeView().setVisibility(android.view.View.VISIBLE);

		if (this.hidesWhenStopped) {
			this.setHidden(true);
		}

		this.animating = false;
	}

	/**
	 * Check if we're animating
	 *
	 * @return true if we're animating, false otherwise
	 */
	public boolean isAnimating() {
		return this.animating;
	}

}
