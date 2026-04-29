package net.montoyo.wd.utilities.browser.handlers;

import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.utilities.browser.handlers.js.Scripts;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;

public class DisplayHandler implements CefDisplayHandler {

    public static final CefDisplayHandler INSTANCE = new DisplayHandler();

    public static void injectScripts(CefBrowser browser) {
        if (browser == null)
            return;

        browser.executeJavaScript(Scripts.POINTER_LOCK, "WebDisplays", 0);
        browser.executeJavaScript(Scripts.SAME_TAB_NAVIGATION, "WebDisplays", 0);
        browser.executeJavaScript(Scripts.VIRTUAL_NAV_OVERLAY, "WebDisplays", 0);
    }

    public static void injectScripts(CefBrowser browser, CefFrame frame) {
        if (frame != null && frame.isValid()) {
            frame.executeJavaScript(Scripts.POINTER_LOCK, "WebDisplays", 0);
            frame.executeJavaScript(Scripts.SAME_TAB_NAVIGATION, "WebDisplays", 0);
            frame.executeJavaScript(Scripts.VIRTUAL_NAV_OVERLAY, "WebDisplays", 0);
        }

        injectScripts(browser);
    }

    @Override
    public void onAddressChange(CefBrowser browser, CefFrame cefFrame, String url) {
        ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);

        if (browser != null) {
            long t = System.currentTimeMillis();

            for (ClientProxy.PadData pd : proxy.getPads()) {
                if (pd.view == browser && t - pd.lastSent() >= 1000) {
                    if (WebDisplays.isSiteBlacklisted(url))
                        pd.view.loadURL(WebDisplays.BLACKLIST_URL);
                    else {
                        pd.updateTime(); //Avoid spamming the server with porn URLs
                        WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(pd.id, url));
                    }

                    break;
                }
            }

            for (ScreenBlockEntity tes : proxy.getScreens())
                tes.updateClientSideURL(browser, url);
        }

        if (browser != null)
            injectScripts(browser, cefFrame != null && cefFrame.isMain() ? cefFrame : browser.getMainFrame());
    }

    @Override
    public void onTitleChange(CefBrowser cefBrowser, String s) {
    }

    @Override
    public boolean onTooltip(CefBrowser cefBrowser, String s) {
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser cefBrowser, String s) {
    }

    @Override
    public boolean onConsoleMessage(CefBrowser cefBrowser, CefSettings.LogSeverity logSeverity, String s, String s1, int i) {
        return false;
    }

    @Override
    public boolean onCursorChange(CefBrowser cefBrowser, int i) {
        return false;
    }
}
