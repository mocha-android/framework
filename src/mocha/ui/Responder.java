/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package mocha.ui;

import java.util.List;

public class Responder extends mocha.foundation.Object {

	public Responder nextResponder() {
		return null;
	}

	public boolean canBecomeFirstResponder() {
		return false;
	}

	public boolean becomeFirstResponder() {
		Window window = this.findWindow();

		if(this.isFirstResponder(window)) {
			return true;
		} else if(window != null && this.canBecomeFirstResponder()) {
			Responder firstResponder = window.getFirstResponder();
			boolean allow = false;

			if(firstResponder != null) {
				if(firstResponder.canResignFirstResponder()) {
					allow = firstResponder.resignFirstResponder();
				}
			} else {
				allow = true;
			}

			if(allow) {
				window.setFirstResponder(this);
				return true;
			}
		}

		return false;
	}

	public boolean canResignFirstResponder() {
		return true;
	}

	public boolean resignFirstResponder() {
		Window window = this.findWindow();

		if(this.isFirstResponder(window)) {
			window.setFirstResponder(null);
		}

		return true;
	}

	public boolean isFirstResponder() {
		return this.isFirstResponder(findWindow());
	}

	private boolean isFirstResponder(Window window) {
		return window != null && window.getFirstResponder() == this;
	}

	public void touchesBegan(List<Touch> touches, Event event) {
		Responder nextResponder = this.nextResponder();

		if(nextResponder != null) {
			nextResponder.touchesBegan(touches, event);
		}
	}

	public void touchesMoved(List<Touch> touches, Event event) {
		Responder nextResponder = this.nextResponder();

		if(nextResponder != null) {
			nextResponder.touchesMoved(touches, event);
		}
	}

	public void touchesEnded(List<Touch> touches, Event event) {
		Responder nextResponder = this.nextResponder();

		if(nextResponder != null) {
			nextResponder.touchesEnded(touches, event);
		}
	}

	public void touchesCancelled(List<Touch> touches, Event event) {
		Responder nextResponder = this.nextResponder();

		if(nextResponder != null) {
			nextResponder.touchesCancelled(touches, event);
		}
	}

	private Window findWindow() {
		if(this instanceof View) {
			return ((View)this).getWindow();
		} else {
			Responder nextResponder = this.nextResponder();

			if(nextResponder != null) {
				return nextResponder.findWindow();
			} else {
				return null;
			}
		}
	}
}
