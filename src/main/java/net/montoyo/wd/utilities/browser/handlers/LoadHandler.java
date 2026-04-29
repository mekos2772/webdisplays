package net.montoyo.wd.utilities.browser.handlers;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

public class LoadHandler extends CefLoadHandlerAdapter {
    public static final LoadHandler INSTANCE = new LoadHandler();

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        if (browser == null || frame == null || !frame.isMain())
            return;

        DisplayHandler.injectScripts(browser, frame);
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        if (browser == null || frame == null || !frame.isMain())
            return;

        DisplayHandler.injectScripts(browser, frame);
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        if (browser != null && !isLoading)
            DisplayHandler.injectScripts(browser, browser.getMainFrame());
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
        if (browser != null && frame != null && frame.isMain())
            DisplayHandler.injectScripts(browser, frame);
    }
}
