{
    (function() {
        if (window.__wdSameTabInstalled)
            return;

        window.__wdSameTabInstalled = true;

        function toUrl(raw) {
            if (typeof raw !== "string" || raw.length === 0)
                return null;

            try {
                return new URL(raw, window.location.href).toString();
            } catch (e) {
                return null;
            }
        }

        function isNavigableUrl(url) {
            return !!url &&
                !url.startsWith("javascript:") &&
                !url.startsWith("mailto:") &&
                !url.startsWith("tel:");
        }

        function shouldHandleAnchor(anchor) {
            if (!anchor || anchor.tagName !== "A")
                return false;

            const href = anchor.getAttribute("href");
            const next = toUrl(href);
            if (!isNavigableUrl(next))
                return false;

            const target = (anchor.getAttribute("target") || "").toLowerCase();
            return target === "" || target === "_blank" || target === "_new" || target === "_top";
        }

        function rewriteAnchor(anchor) {
            if (!shouldHandleAnchor(anchor))
                return;

            anchor.setAttribute("target", "_self");
            anchor.setAttribute("rel", "");
            anchor.setAttribute("data-wd-same-tab", "1");
        }

        function rewriteAnchors(root) {
            if (!root)
                return;

            if (root.tagName === "A")
                rewriteAnchor(root);

            if (typeof root.querySelectorAll !== "function")
                return;

            const anchors = root.querySelectorAll("a[href]");
            for (let i = 0; i < anchors.length; i++)
                rewriteAnchor(anchors[i]);
        }

        function findAnchor(node) {
            while (node) {
                if (node.tagName === "A")
                    return node;
                node = node.parentElement;
            }

            return null;
        }

        function navigate(rawUrl, replace) {
            const next = toUrl(rawUrl);
            if (!isNavigableUrl(next))
                return false;

            if (replace)
                window.location.replace(next);
            else
                window.location.assign(next);

            return true;
        }

        window.__wdNavApi = window.__wdNavApi || {};
        window.__wdNavApi.canGoBack = function() {
            try {
                return history.length > 1;
            } catch (e) {
                return false;
            }
        };
        window.__wdNavApi.canGoForward = function() {
            return false;
        };
        window.__wdNavApi.goBack = function() {
            try {
                history.back();
                return true;
            } catch (e) {
                return false;
            }
        };
        window.__wdNavApi.goForward = function() {
            try {
                history.forward();
                return true;
            } catch (e) {
                return false;
            }
        };
        window.__wdNavApi.reload = function() {
            try {
                location.reload();
                return true;
            } catch (e) {
                return false;
            }
        };
        window.__wdNavApi.navigate = function(url, replace) {
            return navigate(url, replace);
        };

        const originalOpen = window.open;
        window.open = function(url, target) {
            const t = typeof target === "string" ? target.toLowerCase() : "";
            if (t === "" || t === "_blank" || t === "_new" || t === "_top")
                if (navigate(url, false))
                    return window;

            return originalOpen ? originalOpen.apply(this, arguments) : null;
        };

        const originalAnchorClick = HTMLAnchorElement.prototype.click;
        HTMLAnchorElement.prototype.click = function() {
            rewriteAnchor(this);
            if (shouldHandleAnchor(this) && navigate(this.getAttribute("href"), false))
                return;

            return originalAnchorClick.apply(this, arguments);
        };

        document.addEventListener("click", function(ev) {
            if (ev.defaultPrevented || ev.button !== 0 || ev.metaKey || ev.ctrlKey || ev.shiftKey || ev.altKey)
                return;

            const anchor = findAnchor(ev.target);
            if (!shouldHandleAnchor(anchor))
                return;

            const href = anchor.getAttribute("href");
            if (!navigate(href, false))
                return;

            ev.preventDefault();
            ev.stopPropagation();
            if (typeof ev.stopImmediatePropagation === "function")
                ev.stopImmediatePropagation();
        }, true);

        const observer = new MutationObserver(function(mutations) {
            for (let i = 0; i < mutations.length; i++) {
                const mutation = mutations[i];
                for (let j = 0; j < mutation.addedNodes.length; j++)
                    rewriteAnchors(mutation.addedNodes[j]);
            }
        });

        if (document.documentElement) {
            observer.observe(document.documentElement, {
                childList: true,
                subtree: true
            });
        }

        rewriteAnchors(document);
    })();
}
