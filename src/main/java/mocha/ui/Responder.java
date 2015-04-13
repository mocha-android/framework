/*
 *  @author Shaun
 *	@date 11/24/12
 *	@copyright	2012 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.foundation.MObject;

import java.util.List;

public class Responder extends MObject {
	private boolean transitioningFirstResponders;
	private boolean forceTransitionFirstResponder;

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
		} else if(window != null && this.canBecomeFirstResponder() && this.isInCompleteResponderChain()) {
			Responder firstResponder = window.getFirstResponder();
			boolean allow;

			if(firstResponder != null) {
				firstResponder.transitioningFirstResponders = true;
				allow = firstResponder.resignFirstResponder();
				firstResponder.transitioningFirstResponders = false;
			} else {
				allow = true;
			}

			if(allow || this.forceTransitionFirstResponder) {
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

		if((this.isFirstResponder(window) && this.canResignFirstResponder()) || this.forceTransitionFirstResponder) {
			window.setFirstResponder(null);
		}

		this.makeNextFirstResponderActiveIfAllowed();

		return true;
	}

	public boolean isFirstResponder() {
		return this.isFirstResponder(findWindow());
	}

	private boolean isFirstResponder(Window window) {
		return window != null && window.getFirstResponder() == this;
	}

	private void makeNextFirstResponderActiveIfAllowed() {
		if(!this.transitioningFirstResponders) {
			this.makeNextFirstResponderActive();
		}
	}

	private void makeNextFirstResponderActive() {
		Responder nextResponder = this;

		this.forceTransitionFirstResponder = true;

		while((nextResponder = nextResponder.nextResponder()) != null) {
			nextResponder.forceTransitionFirstResponder = true;

			if(nextResponder.becomeFirstResponder()) {
				nextResponder.forceTransitionFirstResponder = false;
				break;
			} else {
				nextResponder.forceTransitionFirstResponder = false;
			}
		}

		this.forceTransitionFirstResponder = false;
	}

	void promoteDeepestDefaultFirstResponder() {
		Responder[] rootResponder = new Responder[1];

		if(this.isInCompleteResponderChain(rootResponder)) {
			Responder firstResponder = rootResponder[0].getDefaultFirstResponder();

			while(firstResponder != null) {
				if(firstResponder.canBecomeDefaultFirstResponder()) {
					if(firstResponder.becomeFirstResponder()) {
						break;
					}
				}

				firstResponder = firstResponder.nextResponder();
			}
		}
	}

	boolean canBecomeDefaultFirstResponder() {
		return false;
	}

	Responder getDefaultFirstResponder() {
		Responder nextResponder = this.nextResponder();

		if(nextResponder != null) {
			return nextResponder.getDefaultFirstResponder();
		} else {
			return null;
		}
	}

	boolean isInCompleteResponderChain() {
		return isInCompleteResponderChain(null);
	}

	private boolean isInCompleteResponderChain(Responder[] rootResponder) {
		Responder responder = this;

		while(responder != null) {
			if(responder == Application.sharedApplication()) {
				if(rootResponder != null) {
					rootResponder[0] = responder;
				}

				return true;
			} else {
				responder = responder.nextResponder();
			}
		}

		return false;
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
