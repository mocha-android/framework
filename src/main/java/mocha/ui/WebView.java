package mocha.ui;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import mocha.foundation.WeakReference;
import mocha.graphics.Rect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WebView extends View {

	public interface Delegate {
		public boolean shouldStartLoad(WebView webView, String url, NavigationType navigationType);

		public void didStartLoad(WebView webView);

		public void didFinishLoad(WebView webView);

		public void didFailLoad(WebView webView);
	}

	public enum NavigationType {
		LINK_CLICKED,
		FORM_SUBMITTED,
		BACK_FORWARD,
		RELOAD,
		FORM_RESUBMITTED,
		OTHER
	}

	private android.webkit.WebView webView;
	private NativeView<android.webkit.WebView> nativeView;
	private WeakReference<Delegate> delegate;
	private boolean isLoading;
	private boolean scalesPageToFit;
	private EvaluateJavascriptInterface evaluateJavascriptInterface;
	private NavigationType lastNavigationType;

	private static Method evaluateJavascriptMethod = null;
	private static boolean loadedEvaluateJavascriptMethod = false;

	public WebView(Rect frame) {
		super(frame);
	}

	public WebView() {
		super();
	}

	@Override
	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		if (!loadedEvaluateJavascriptMethod) {
			try {
				evaluateJavascriptMethod = android.webkit.WebView.class.getMethod("evaluateJavascript", String.class, ValueCallback.class);
			} catch (NoSuchMethodException e) {
				evaluateJavascriptMethod = null;
			}

			loadedEvaluateJavascriptMethod = true;
		}

		this.setClipsToBounds(true);

		this.webView = new android.webkit.WebView(Application.sharedApplication().getContext());
		this.webView.getSettings().setJavaScriptEnabled(true);
		this.webView.getSettings().setSupportZoom(true);
		this.webView.getSettings().setUseWideViewPort(true);
		this.webView.setWebViewClient(new WebViewClient());

		this.nativeView = new NativeView<android.webkit.WebView>(this.webView);
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.nativeView);

		this.evaluateJavascriptInterface = new EvaluateJavascriptInterface();
		this.evaluateJavascriptInterface.register();

		this.setScalesPageToFit(false);
	}

	public void enableJavascriptLogging() {
		this.webView.setWebChromeClient(WebChromeClient.instance());
	}

	public void disableJavascriptLogging() {
		this.webView.setWebChromeClient(null);
	}

	@Override
	public void setUserInteractionEnabled(boolean userInteractionEnabled) {
		super.setUserInteractionEnabled(userInteractionEnabled);
		this.nativeView.setUserInteractionEnabled(userInteractionEnabled);
	}

	public void willMoveToWindow(Window newWindow) {
		super.willMoveToWindow(newWindow);

		if (newWindow == null) {
			if (this.nativeView.getSuperview() != null) {
				this.nativeView.removeFromSuperview();
			}
		} else {
			if (this.nativeView.getSuperview() == null) {
				this.addSubview(this.nativeView);
				this.nativeView.setFrame(this.getBounds());
			}
		}
	}

	public void didMoveToWindow() {
		super.didMoveToWindow();

		this.recursiveFix();
	}

	private void recursiveLayout(ViewLayerNative layer) {
		layer.updateSize();
		layer.getView()._layoutSubviews();
		layer.setNeedsDisplay();

		for (ViewLayer sublayer : layer.getSublayers()) {
			this.recursiveLayout((ViewLayerNative) sublayer);
		}
	}

	private void recursiveFix() {
		Window window = getWindow();
		if (window != null) {
			recursiveLayout((ViewLayerNative) window.getLayer());

			this.webView.postInvalidate();
			this.webView.forceLayout();
		}
	}


	/**
	 * Get the delegate for this web view
	 *
	 * @return Delegate
	 */
	public Delegate getDelegate() {
		return WeakReference.get(this.delegate);
	}

	/**
	 * Set the web view delegate
	 *
	 * @param delegate Delegate
	 */
	public void setDelegate(Delegate delegate) {
		this.delegate = WeakReference.replace(this.delegate, delegate);
	}

	@Override
	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);
		this.nativeView.setBackgroundColor(backgroundColor);
	}

	@Override
	public void setNeedsLayout() {
		super.setNeedsLayout();

		if (this.nativeView != null) {
			this.nativeView.getNativeView().invalidate();
		}
	}

	@Override
	public void setNeedsDisplay() {
		super.setNeedsDisplay();

		if (this.nativeView != null) {
			this.nativeView.getNativeView().forceLayout();
		}
	}

	/**
	 * Load a url and provide extra request headers
	 *
	 * @param url          URL to load
	 * @param extraHeaders Extra request headers to be sent
	 */
	public void loadUrl(String url, Map<String, String> extraHeaders) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadUrl(url, extraHeaders);
	}

	/**
	 * Load a url
	 *
	 * @param url URL to load
	 */
	public void loadUrl(String url) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadUrl(url);
	}

	/**
	 * Post a URL with a post body
	 *
	 * @param url      URL to post to
	 * @param postData Post body
	 */
	public void postUrl(String url, byte[] postData) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.postUrl(url, postData);
	}

	/**
	 * Load an HTML string with a base URL
	 *
	 * @param htmlString HTML String to load
	 * @param baseUrl    Base URL for the document
	 */
	public void loadHTMLString(String htmlString, String baseUrl) {
		this.lastNavigationType = NavigationType.OTHER;
		this.loadData(htmlString, "text/html", "utf-8", baseUrl);
	}

	/**
	 * Load data with a base URL
	 *
	 * @param data             Data to load
	 * @param mimeType         Mime type of the data
	 * @param textEncodingName Name of the data encoding
	 * @param baseUrl          Base URL for the document
	 */
	public void loadData(String data, String mimeType, String textEncodingName, String baseUrl) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadDataWithBaseURL(baseUrl, data, mimeType, textEncodingName, null);
	}

	/**
	 * Execute javascript on the current page
	 *
	 * @param javascript Javascript to evaluate
	 */
	public void executeJavascript(String javascript) {
		this.evaluateJavascript(javascript, null);
	}

	/**
	 * Execute javascript on the current page and notify a callback of it's response
	 *
	 * @param javascript Javascript to evaluate
	 * @param callback   Called on the main thread with the response of the execution
	 */
	public void evaluateJavascript(String javascript, ValueCallback<String> callback) {
		if (evaluateJavascriptMethod != null) {
			try {
				evaluateJavascriptMethod.invoke(this.webView, javascript, callback);
			} catch (Exception e) {
				MWarn(e, "Javascript execution failed: '%s'", javascript);
			}
		} else {
			this.evaluateJavascriptInterface.evaluateJavascript(javascript, callback);
		}
	}

	/**
	 * Reload the current page
	 */
	public void reload() {
		this.lastNavigationType = NavigationType.RELOAD;
		this.webView.reload();
	}

	/**
	 * Stop a page from loading
	 */
	public void stopLoading() {
		this.webView.stopLoading();
	}

	/**
	 * Go backwards in the history stack
	 */
	public void goBack() {
		this.lastNavigationType = NavigationType.BACK_FORWARD;
		this.webView.goBack();
	}

	/**
	 * Go forwards in the history stack
	 */
	public void goForward() {
		this.lastNavigationType = NavigationType.BACK_FORWARD;
		this.webView.goForward();
	}

	/**
	 * Whether or not the web view can go backwards
	 *
	 * @return true if there's more items in the history stack, false otherwise
	 */
	public boolean canGoBack() {
		return this.webView.canGoBack();
	}

	/**
	 * Whether or not the web view can go fowards
	 *
	 * @return true if we're not at the front of the history stack, false otherwise
	 */
	public boolean canGoForward() {
		return this.webView.canGoForward();
	}

	/**
	 * Check whether the page is loading
	 *
	 * @return true if the page is loading, false otherwise
	 */
	public boolean isLoading() {
		return this.isLoading;
	}

	/**
	 * Get the url for the current page
	 *
	 * @return Url of the current page
	 */
	public String getUrl() {
		return this.webView.getUrl();
	}

	/**
	 * Get the title of the document
	 *
	 * @return Document title
	 */
	public String getTitle() {
		return this.webView.getTitle();
	}

	/**
	 * Get whether or not pages auto-scale to fit the viewport
	 *
	 * @return true if pages auto-scale to fit the viewport, false otherwise
	 */
	public boolean getScalesPageToFit() {
		return this.scalesPageToFit;
	}

	/**
	 * Set whther or not pages auto-scale to fit the viewport
	 *
	 * @param scalesPageToFit If true, large pages will be scaled down to fit the viewport
	 *                        If false, large pages will appear as their actual size and panning is required
	 */
	public void setScalesPageToFit(boolean scalesPageToFit) {
		this.scalesPageToFit = scalesPageToFit;
		this.webView.setInitialScale(scalesPageToFit ? 100 : 0);
		this.webView.getSettings().setLoadWithOverviewMode(scalesPageToFit);
	}

	/**
	 * Get the height of the current content
	 *
	 * @return Content height
	 */
	public float getContentHeight() {
		return this.webView.getContentHeight();
	}

	/**
	 * Based on NIWebView
	 * https://github.com/lolboxen/AndroidNIWebView/blob/master/NIWebView.java
	 */
	private class EvaluateJavascriptInterface {
		private Map<String, Command> commands = new HashMap<String, Command>();
		private int executionOrdinal = 0;
		private static final String JS_CODE = "javascript:try { " +
			"__mochaWebViewJSInterface.commandExecuted('%s', eval(__mochaWebViewJSInterface.getCommand('%s')));" +
			"} catch(err) { __mochaWebViewJSInterface.commandExecuted('%s', ''); }";


		private class Command {
			private final ValueCallback<String> callback;
			private final String key;
			private final String javascript;

			private boolean failed;
			private boolean finished;
			private Runnable compileTimeoutCallback;
			private Runnable executionTimeoutCallback;

			Command(String key, String javascript, final ValueCallback<String> callback) {
				this.key = key;
				this.javascript = javascript;
				this.callback = callback;
			}

			void willStart() {
				this.compileTimeoutCallback = performOnMainAfterDelay(5000, new Runnable() {
					public void run() {
						MWarn("Javascript failed to compile: '%s'", javascript);
						compileTimeoutCallback = null;
						commandFailed(Command.this);
						failed = true;
					}
				});
			}

			String get() {
				if (!failed) {
					cancelCallbacks(this.compileTimeoutCallback);
					this.compileTimeoutCallback = null;

					this.executionTimeoutCallback = performOnMainAfterDelay(30000, new Runnable() {
						public void run() {
							MWarn("Timed out waiting for javascript to execute: '%s'", javascript);
							executionTimeoutCallback = null;
							commandFailed(Command.this);
							failed = true;
						}
					});

					return this.javascript;
				} else {
					return null;
				}
			}

			String key() {
				return this.key;
			}

			void didFinish(String result) {
				if (this.executionTimeoutCallback != null) {
					cancelCallbacks(this.executionTimeoutCallback);
					this.executionTimeoutCallback = null;
				}

				if (!finished && !failed) {
					finished = true;

					if (this.callback != null) {
						this.callback.onReceiveValue(result);
					}
				}
			}
		}

		public void register() {
			webView.addJavascriptInterface(this, "__mochaWebViewJSInterface");
		}

		public void evaluateJavascript(String javascript, final ValueCallback<String> callback) {
			String key = String.valueOf(++this.executionOrdinal);
			Command command = new Command(key, javascript, callback);
			this.commands.put(key, command);

			command.willStart();
			webView.loadUrl(String.format(JS_CODE, key, key, key));
		}

		@JavascriptInterface
		public String getCommand(String key) {
			Command command = this.commands.get(key);
			if (command != null) {
				return command.get();
			} else {
				return null;
			}
		}

		@JavascriptInterface
		public void commandExecuted(String key, String result) {
			Command command = this.commands.get(key);
			if (command != null) {
				command.didFinish(result);
				commandEnded(command);
			}
		}

		private void commandFailed(Command command) {
			command.didFinish("");
			commandEnded(command);
		}

		private void commandEnded(Command command) {
			this.commands.remove(command.key());
		}

	}

	private class WebViewClient extends android.webkit.WebViewClient {

		public boolean shouldOverrideUrlLoading(android.webkit.WebView view, final String url) {
			if (delegate != null && (url == null || !url.startsWith("javascript:"))) {
				final NavigationType navigationType;

				if (lastNavigationType != null) {
					navigationType = lastNavigationType;
					lastNavigationType = null;
				} else {
					android.webkit.WebView.HitTestResult hitTestResult = view.getHitTestResult();
					if (hitTestResult != null) {
						switch (hitTestResult.getType()) {
							case android.webkit.WebView.HitTestResult.PHONE_TYPE:
							case android.webkit.WebView.HitTestResult.GEO_TYPE:
							case android.webkit.WebView.HitTestResult.EMAIL_TYPE:
							case android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE:
							case android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
								navigationType = NavigationType.LINK_CLICKED;
								break;
							case android.webkit.WebView.HitTestResult.EDIT_TEXT_TYPE:
								navigationType = NavigationType.FORM_SUBMITTED;
								break;
							case android.webkit.WebView.HitTestResult.IMAGE_TYPE:
							case android.webkit.WebView.HitTestResult.UNKNOWN_TYPE:
							default:
								navigationType = NavigationType.OTHER;
						}
					} else {
						navigationType = NavigationType.OTHER;
					}
				}

				final boolean shouldStart[] = new boolean[]{true};

				delegate.runIf(new WeakReference.HasReference<Delegate>() {
					public void hasReference(Delegate delegate) {
						shouldStart[0] = delegate.shouldStartLoad(WebView.this, url, navigationType);
					}
				});

				return !shouldStart[0];
			} else {
				return super.shouldOverrideUrlLoading(view, url);
			}
		}

		public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);

			if (url == null || !url.startsWith("javascript:")) {
				this.loadDidStart();
			}
		}

		public void onPageFinished(android.webkit.WebView view, String url) {
			super.onPageFinished(view, url);

			if (url == null || !url.startsWith("javascript:")) {
				this.loadDidEnd(false);
			}
		}

		public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);

			if (failingUrl == null || !failingUrl.startsWith("javascript:")) {
				this.loadDidEnd(true);
			}
		}

		public void onReceivedSslError(android.webkit.WebView view, SslErrorHandler handler, SslError error) {
			super.onReceivedSslError(view, handler, error);
			this.loadDidEnd(true);
		}

		private void loadDidStart() {
			isLoading = true;

			if (delegate != null) {
				delegate.runIf(new WeakReference.HasReference<Delegate>() {
					public void hasReference(Delegate delegate) {
						delegate.didStartLoad(WebView.this);
					}
				});
			}

			recursiveFix();
		}

		public void loadDidEnd(final boolean failed) {
			isLoading = false;

			recursiveFix();

			if (delegate != null) {
				delegate.runIf(new WeakReference.HasReference<Delegate>() {
					public void hasReference(Delegate delegate) {
						if (failed) {
							delegate.didFailLoad(WebView.this);
						} else {
							delegate.didFinishLoad(WebView.this);
						}
					}
				});
			}

			lastNavigationType = null;
		}

	}

	private static class WebChromeClient extends android.webkit.WebChromeClient {
		private static final String LOG_TAG = "MochaWebView";
		private static WeakReference<WebChromeClient> instance;

		static WebChromeClient instance() {
			if (instance == null || instance.get() == null) {
				instance = new WeakReference<WebChromeClient>(new WebChromeClient());
			}

			return instance.get();
		}

		public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
			String message = consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId();

			switch (consoleMessage.messageLevel()) {
				case DEBUG:
				case TIP:
					Log.d(LOG_TAG, message);
					break;
				case ERROR:
					Log.e(LOG_TAG, message);
					break;
				case LOG:
					Log.i(LOG_TAG, message);
					break;
				case WARNING:
					Log.w(LOG_TAG, message);
					break;
			}
			return true;
		}
	}

}
