/*
 *  @author Shaun
 *	@date 11/25/12
 *	@copyright	2012 enormego. All rights reserved.
 */
package com.mochatest.controllers;

import android.widget.Toast;
import mocha.ui.ViewController;

public class ToastViewController extends ViewController {
	private Toast toast;

	protected void loadView() {
		super.loadView();
		this.toast = Toast.makeText(this.getView().getLayer().getContext(), "", Toast.LENGTH_SHORT);
	}

	public void viewWillDisappear(boolean animated) {
		super.viewWillDisappear(animated);
		this.toast.cancel();
	}

	protected void showMessage(String msg) {
		this.toast.setText(msg);
		this.toast.show();
	}

}
