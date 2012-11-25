/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import android.graphics.Color;
import mocha.graphics.Rect;
import mocha.graphics.Size;
import mocha.graphics.TextAlignment;
import mocha.ui.Label;
import mocha.ui.ScrollView;
import mocha.ui.View;
import mocha.ui.ViewController;

public class PageViewController extends ViewController {

	protected void loadView() {
		super.loadView();

		View view = this.getView();
		Rect bounds = view.getBounds();

		view.setBackgroundColor(Color.WHITE);

		int numberOfPages = 5;
		final ScrollView scrollView = new ScrollView(new Rect(0, 0, bounds.size.width, bounds.size.height));
		scrollView.setContentSize(new Size(bounds.size.width * numberOfPages, 320));
		scrollView.setPagingEnabled(true);
		scrollView.setBackgroundColor(Color.WHITE);
		scrollView.setAutoresizing(View.Autoresizing.FLEXIBLE_SIZE);
		scrollView.setAlwaysBounceHorizontal(true);
		view.addSubview(scrollView);

		for(int page = 0; page < numberOfPages; page++ ){
			Label label = new Label(new Rect((bounds.size.width * page) + 10.0f, 10, bounds.size.width - 20.0f, 75));
			label.setText(page + "");
			label.setTextAlignment(TextAlignment.CENTER);
			label.setTextColor(Color.BLACK);
			label.setBackgroundColor(Color.RED);
			scrollView.addSubview(label);
		}

	}

}
