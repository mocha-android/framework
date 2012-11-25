/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import mocha.graphics.Font;
import mocha.graphics.Rect;
import mocha.graphics.TextAlignment;
import mocha.ui.*;

import java.util.EnumSet;

public class SwipeTestViewController extends ToastViewController {

	protected void loadView() {
		super.loadView();

		this.getView().setBackgroundColor(Color.WHITE);

		Label label = new Label(new Rect(0.0f, 0.0f, this.getView().getBounds().size.width, 44.0f));
		label.setBackgroundColor(Color.WHITE);
		label.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);
		label.setText("Swipe in any direction");
		label.setFont(Font.getBoldSystemFontWithSize(22.0f));
		label.setTextAlignment(TextAlignment.CENTER);
		this.getView().addSubview(label);

		GestureRecognizer.GestureHandler swipeHandler = new GestureRecognizer.GestureHandler() {

			public void handleGesture(GestureRecognizer gestureRecognizer) {
				if(gestureRecognizer.getState() == GestureRecognizer.State.ENDED) {
					SwipeGestureRecognizer.Direction direction = ((SwipeGestureRecognizer)gestureRecognizer).getDirection();

					showMessage("SWIPE " + direction + " RECOGNIZED");
				}
			}
		};

		View view = this.getView();

		for(final SwipeGestureRecognizer.Direction direction : EnumSet.allOf(SwipeGestureRecognizer.Direction.class)) {
			view.addGestureRecognizer(new SwipeGestureRecognizer(direction, swipeHandler));
		}
	}

}
