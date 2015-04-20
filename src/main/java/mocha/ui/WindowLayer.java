package mocha.ui;

public interface WindowLayer extends ViewLayer {

	public android.view.View getNativeView();

	void onWindowPause();

	void onWindowResume();

}
