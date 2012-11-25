/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import mocha.graphics.Rect;
import mocha.graphics.TextAlignment;
import mocha.ui.*;

public class RootViewController extends ViewController {

	protected void loadView() {
		super.loadView();

		View view = this.getView();
		view.setBackgroundColor(Color.WHITE);

		Label label = new Label(new Rect(10.0f, 10.0f, view.getBounds().size.width - 20.0f, 80.0f));
		label.setText("Show Table View");
		label.setTextAlignment(TextAlignment.CENTER);
		label.setBackgroundColor(Color.LTGRAY);
		label.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);
		label.setUserInteractionEnabled(true);
		label.addGestureRecognizer(new TapGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				getNavigationController().pushViewController(new TableViewController(), true);
			}
		}));
		view.addSubview(label);

		label = new Label(new Rect(10.0f, 100.0f, view.getBounds().size.width - 20.0f, 80.0f));
		label.setText("Show Paging View");
		label.setTextAlignment(TextAlignment.CENTER);
		label.setBackgroundColor(Color.LTGRAY);
		label.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);
		label.setUserInteractionEnabled(true);
		label.addGestureRecognizer(new TapGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				getNavigationController().pushViewController(new PageViewController(), true);
			}
		}));
		view.addSubview(label);

		label = new Label(new Rect(10.0f, 190.0f, view.getBounds().size.width - 20.0f, 80.0f));
		label.setText("Show Pan Test");
		label.setTextAlignment(TextAlignment.CENTER);
		label.setBackgroundColor(Color.LTGRAY);
		label.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);
		label.setUserInteractionEnabled(true);
		label.addGestureRecognizer(new TapGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				getNavigationController().pushViewController(new PanTestViewController(), true);
			}
		}));
		view.addSubview(label);

		label = new Label(new Rect(10.0f, 280.0f, view.getBounds().size.width - 20.0f, 80.0f));
		label.setText("Show Swipe Test");
		label.setTextAlignment(TextAlignment.CENTER);
		label.setBackgroundColor(Color.LTGRAY);
		label.setAutoresizing(View.Autoresizing.FLEXIBLE_WIDTH);
		label.setUserInteractionEnabled(true);
		label.addGestureRecognizer(new TapGestureRecognizer(new GestureRecognizer.GestureHandler() {
			public void handleGesture(GestureRecognizer gestureRecognizer) {
				getNavigationController().pushViewController(new SwipeTestViewController(), true);
			}
		}));
		view.addSubview(label);
	}
}
