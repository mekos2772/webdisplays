package net.montoyo.wd.utilities.browser.handlers;

import net.montoyo.wd.WebDisplays;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

public class LifeSpanHandler extends CefLifeSpanHandlerAdapter {
    public static final LifeSpanHandler INSTANCE = new LifeSpanHandler();

    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
        if (browser != null && targetUrl != null && !targetUrl.isBlank()) {
            browser.loadURL(WebDisplays.applyBlacklist(targetUrl));
        }
        return true;
    }
}
