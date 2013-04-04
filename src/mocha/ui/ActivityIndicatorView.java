/**
 *  @author Shaun
 *  @date 3/28/13
 *  @copyright 2013 TV Guide, Inc. All rights reserved.
 */
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

	public void sizeToFit() {
		Rect frame = this.getFrame();

		if(this.style == Style.WHITE_LARGE || this.style == Style.DARK_LARGE) {
			frame.size.width = 46.0f;
			frame.size.height = 46.0f;
		} else {
			frame.size.width = 28.0f;
			frame.size.height = 28.0f;
		}

		this.setFrame(frame);
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		if(style == null) {
			style = Style.WHITE;
		}

		if(this.style != style) {
			this.style = style;

			if(this.progressBar != null) {
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


			this.progressBar = new NativeView<ProgressBar>(new ProgressBar(Application.sharedApplication().getContext(), null, nativeStyle));
			this.progressBar.getNativeView().setIndeterminate(true);
			this.progressBar.getNativeView().setPadding(0, 0, 0, 0);
			this.progressBar.getNativeView().setInterpolator(this.animating ? this.animatingInterpolator : this.stoppedInterpolator);
			this.progressBar.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
			this.addSubview(this.progressBar);
		}
	}

	public void layoutSubviews() {
		super.layoutSubviews();

		if(this.progressBar != null) {
			float inset = this.style == Style.WHITE_LARGE || this.style == Style.DARK_LARGE ? -6.0f : -4.0f;
			this.progressBar.setFrame(this.getBounds().inset(inset, inset));
		}
	}

	public boolean hidesWhenStopped() {
		return hidesWhenStopped;
	}

	public void setHidesWhenStopped(boolean hidesWhenStopped) {
		if(this.hidesWhenStopped != hidesWhenStopped) {
			this.hidesWhenStopped = hidesWhenStopped;

			if(this.hidesWhenStopped && !this.animating) {
				this.setHidden(true);
			} else if(!this.hidesWhenStopped) {
				this.setHidden(false);
			}
		}
	}

	public void startAnimating() {
		if(this.animating) return;

		this.progressBar.getNativeView().setInterpolator(this.animatingInterpolator);
		this.animating = true;

		if(this.hidesWhenStopped) {
			this.setHidden(false);
		}
	}

	public void stopAnimating() {
		if(!this.animating) return;

		this.progressBar.getNativeView().setInterpolator(this.stoppedInterpolator);

		if(this.hidesWhenStopped) {
			this.setHidden(true);
		}
	}

	public boolean isAnimating() {
		return this.animating;
	}

}
