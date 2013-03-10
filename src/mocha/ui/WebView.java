/**
 *  @author Shaun
 *  @date 3/8/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.ui;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.webkit.SslErrorHandler;
import mocha.graphics.Rect;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
	private Delegate delegate;
	private boolean isLoading;
	private boolean ignorePageLoadChanges;
	private boolean scalesPageToFit;
	private EvaluateJavascriptInterface evaluateJavascriptInterface;
	private NavigationType lastNavigationType;

	public WebView(Rect frame) { super(frame); }
	public WebView() { super(); }

	protected void onCreate(Rect frame) {
		super.onCreate(frame);

		this.setClipsToBounds(true);

		this.webView = new android.webkit.WebView(Application.sharedApplication().getContext());
		this.webView.setWebViewClient(new WebViewClient());
		this.webView.getSettings().setJavaScriptEnabled(true);

		this.nativeView = new NativeView<android.webkit.WebView>(this.webView);
		this.nativeView.setFrame(this.getBounds());
		this.nativeView.setAutoresizing(Autoresizing.FLEXIBLE_SIZE);
		this.addSubview(this.nativeView);

		this.evaluateJavascriptInterface = new EvaluateJavascriptInterface();
		this.evaluateJavascriptInterface.register();

		this.setScalesPageToFit(false);
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}

	public void setBackgroundColor(int backgroundColor) {
		super.setBackgroundColor(backgroundColor);
		this.nativeView.setBackgroundColor(backgroundColor);
	}

	public void loadUrl(String url, Map<String,String> extraHeaders) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadUrl(url, extraHeaders);
	}

	public void loadUrl(String url) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadUrl(url);
	}

	public void postUrl(String url, byte[] postData) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.postUrl(url, postData);
	}
	
	public void loadHTMLString(String htmlString, String baseUrl) {
		this.lastNavigationType = NavigationType.OTHER;
		this.loadData(htmlString, "text/html", "utf-8", baseUrl);
	}

	public void loadData(String data, String mimeType, String textEncodingName, String baseUrl) {
		this.lastNavigationType = NavigationType.OTHER;
		this.webView.loadDataWithBaseURL(baseUrl, data, mimeType, textEncodingName, null);
	}

	public String getStringByEvaluatingJavaScriptFromString(String javascript) {
		return this.evaluateJavascriptInterface.getStringByEvaluatingJavaScriptFromString(javascript);
	}

	public void reload() {
		this.lastNavigationType = NavigationType.RELOAD;
		this.webView.reload();
	}

	public void stopLoading() {
		this.webView.stopLoading();
	}

	public void goBack() {
		this.lastNavigationType = NavigationType.BACK_FORWARD;
		this.webView.goBack();
	}

	public void goForward() {
		this.lastNavigationType = NavigationType.BACK_FORWARD;
		this.webView.goForward();
	}

	public boolean canGoBack() {
		return this.webView.canGoBack();
	}

	public boolean canGoForward() {
		return this.webView.canGoForward();
	}

	public boolean isLoading() {
		return this.isLoading;
	}

	public String getUrl() {
		return this.webView.getUrl();
	}

	public String getTitle() {
		return this.webView.getTitle();
	}

	public boolean getScalesPageToFit() {
		return this.scalesPageToFit;
	}

	public void setScalesPageToFit(boolean scalesPageToFit) {
		this.scalesPageToFit = scalesPageToFit;
		this.webView.setInitialScale(scalesPageToFit ? 100 : 0);
	}

	/**
	 * Ported from NIWebView
	 * https://github.com/lolboxen/AndroidNIWebView/blob/master/NIWebView.java
	 * @author Trent Ahrens
	 */
	private class EvaluateJavascriptInterface {
		private CountDownLatch compileLatch;
		private CountDownLatch finishLatch;
		private String returnValue;

		public void register() {
			webView.addJavascriptInterface(this, "__mochaWebViewJSInterface");
		}

		public String getStringByEvaluatingJavaScriptFromString(String javascript) {
			String escapedJavascript = javascript.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\"");
			String finalCode = "javascript:try { " +
							"__mochaWebViewJSInterface.didCompile();" +
							"__mochaWebViewJSInterface.setReturnValue(eval(\"" + escapedJavascript + "\"));" +
							"} catch(err) {" +
							"__mochaWebViewJSInterface.setReturnValue('');" +
							"}";
			ignorePageLoadChanges = true;
			webView.loadUrl(finalCode);
			String returnValue = this.getReturnValue();
			ignorePageLoadChanges = false;
			return returnValue;
		}

		private String getReturnValue() {
			this.returnValue = "";

			this.compileLatch = new CountDownLatch(1);
			this.finishLatch = new CountDownLatch(1);

			try {
				if (!this.compileLatch.await(5, TimeUnit.SECONDS)) {
					MWarn("Script did not compile");
					return this.returnValue;
				}
			} catch (InterruptedException e) {
				return this.returnValue;
			}

			try {
				finishLatch.await(30, TimeUnit.SECONDS);
				return this.returnValue;
			} catch (InterruptedException e) {
				MWarn("Timed out waiting for JS response");
				return this.returnValue;
			}
		}

		@SuppressWarnings("unused")
		public void setReturnValue(String returnValue) {
			this.returnValue = returnValue;
			this.finishLatch.countDown();
		}

		@SuppressWarnings("unused")
		public void didCompile() {
			this.compileLatch.countDown();
		}
	}

	private class WebViewClient extends android.webkit.WebViewClient {

		public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
			if(delegate != null && !ignorePageLoadChanges) {
				NavigationType navigationType;

				if(lastNavigationType != null) {
					navigationType = lastNavigationType;
					lastNavigationType = null;
				} else {
					android.webkit.WebView.HitTestResult hitTestResult = view.getHitTestResult();
					if(hitTestResult != null) {
						switch (hitTestResult.getType()) {
							case android.webkit.WebView.HitTestResult.ANCHOR_TYPE:
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
							case android.webkit.WebView.HitTestResult.IMAGE_ANCHOR_TYPE:
							case android.webkit.WebView.HitTestResult.UNKNOWN_TYPE:
							default:
								navigationType = NavigationType.OTHER;
						}
					} else {
						navigationType = NavigationType.OTHER;
					}
				}

				return !delegate.shouldStartLoad(WebView.this, url, navigationType);
			} else {
				return super.shouldOverrideUrlLoading(view, url);
			}
		}

		public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			this.loadDidStart();
		}

		public void onPageFinished(android.webkit.WebView view, String url) {
			super.onPageFinished(view, url);
			this.loadDidEnd(false);
		}

		public void onTooManyRedirects(android.webkit.WebView view, Message cancelMsg, Message continueMsg) {
			super.onTooManyRedirects(view, cancelMsg, continueMsg);
			this.loadDidEnd(true);
		}

		public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			this.loadDidEnd(true);
		}

		public void onReceivedSslError(android.webkit.WebView view, SslErrorHandler handler, SslError error) {
			super.onReceivedSslError(view, handler, error);
			this.loadDidEnd(true);
		}

		private void loadDidStart() {
			isLoading = true;
			delegate.didStartLoad(WebView.this);
		}

		public void loadDidEnd(boolean failed) {
			isLoading = false;

			if(delegate != null && !ignorePageLoadChanges) {
				if(failed) {
					delegate.didFailLoad(WebView.this);
				} else {
					delegate.didFinishLoad(WebView.this);
				}
			}

			lastNavigationType = null;
		}

	}


}
