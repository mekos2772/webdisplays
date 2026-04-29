package net.montoyo.wd.utilities.browser.handlers;

import net.montoyo.wd.WebDisplays;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;

public class RequestHandler extends CefRequestHandlerAdapter {
    public static final RequestHandler INSTANCE = new RequestHandler();

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
        if (browser != null && targetUrl != null && !targetUrl.isBlank()) {
            browser.loadURL(WebDisplays.applyBlacklist(targetUrl));
            return true;
        }
        return false;
    }
}
