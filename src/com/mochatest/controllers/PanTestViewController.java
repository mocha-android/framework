/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import mocha.graphics.Point;
import mocha.graphics.Rect;
import mocha.ui.*;

public class PanTestViewController extends ToastViewController {
	private boolean panning;

	protected void loadView() {
		super.loadView();


		View view = this.getView();
		view.setBackgroundColor(Color.WHITE);

		final View redBlock = new View( new Rect(10, 10, 100, 100));
		redBlock.setBackgroundColor(Color.RED);

		view.addGestureRecognizer(new PanGestureRecognizer(new PanGestureHandler(redBlock)));
		view.addSubview(redBlock);

		View greenBlock = new View(new Rect(120, 120, 100, 100));
		greenBlock.setBackgroundColor(Color.GREEN);
		greenBlock.addGestureRecognizer(new TapGestureRecognizer(2, 1, new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				if(gestureRecognizer.getState() == GestureRecognizer.State.BEGAN) {
					showMessage("DOUBLE TAP RECOGNIZED");
				}
			}
		}));
		greenBlock.addGestureRecognizer(new LongPressGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				if(gestureRecognizer.getState() == GestureRecognizer.State.BEGAN) {
					showMessage("LONG PRESS RECOGNIZED");
				}
			}
		}));
		view.addGestureRecognizer(new PanGestureRecognizer(new PanGestureHandler(greenBlock)));
		view.addSubview(greenBlock);

		View bottomBar = new View(new Rect(0, view.getFrame().size.height - 20, view.getFrame().size.width, 10));
		bottomBar.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH, View.Autoresizing.FLEXIBLE_TOP_MARGIN);
		bottomBar.setBackgroundColor(Color.MAGENTA);
		view.addSubview(bottomBar);
	}

	class PanGestureHandler implements GestureRecognizer.GestureHandler {
		private View view;
		private boolean _panning;
		private Point start;

		public PanGestureHandler(View view) {
			this.view = view;
		}

		public void handleGesture(GestureRecognizer gestureRecognizer) {
			if(panning && !_panning) return;

			if(gestureRecognizer.getState() == GestureRecognizer.State.BEGAN) {
				if((panning = view.getBounds().contains(gestureRecognizer.locationInView(view)))) {
					showMessage("PAN BEGAN");
					view.getSuperview().bringSubviewToFront(view);
					this.start = view.getFrame().origin.copy();
				}
			} else if(gestureRecognizer.getState() == GestureRecognizer.State.CHANGED) {
				if(panning) {
					Point translation = ((PanGestureRecognizer)gestureRecognizer).translationInView(this.view);
					Rect rect = this.view.getFrame();
					rect.origin.x = this.start.x + translation.x;
					rect.origin.y = this.start.y + translation.y;
					this.view.setFrame(rect);
				}
			} else if(gestureRecognizer.getState() == GestureRecognizer.State.ENDED || gestureRecognizer.getState() == GestureRecognizer.State.CANCELLED) {
				if(panning) {
					showMessage("PAN ENDED");
				}

				panning = false;
			}

			_panning = panning;
		}

	}

}
