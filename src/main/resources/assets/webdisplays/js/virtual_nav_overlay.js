{
    (function() {
        if (window.__wdVirtualNavInstalled)
            return;

        window.__wdVirtualNavInstalled = true;
        var ROOT_ID = "wd-nav-root";
        var STYLE_ID = "wd-nav-style";
        var expanded = false;
        var observer = null;
        var keepAliveTimer = 0;

        function navApi() {
            return window.__wdNavApi || {};
        }

        function addStyle() {
            var head = document.head || document.documentElement;
            if (!head || document.getElementById(STYLE_ID))
                return;

            var style = document.createElement("style");
            style.id = STYLE_ID;
            style.textContent =
                "#" + ROOT_ID + "{position:fixed;top:10px;left:10px;z-index:2147483647;font:12px sans-serif;user-select:none;}" +
                "#" + ROOT_ID + " .wd-btn{display:inline-flex;align-items:center;justify-content:center;width:22px;height:18px;" +
                "margin-right:4px;border-radius:4px;border:1px solid rgba(255,255,255,.38);background:rgba(22,22,22,.72);" +
                "color:#fff;cursor:pointer;line-height:1;}" +
                "#" + ROOT_ID + " .wd-btn:hover{background:rgba(44,44,44,.86);}" +
                "#" + ROOT_ID + " .wd-hidden{display:none !important;}" +
                "#" + ROOT_ID + " .wd-disabled{opacity:.42;cursor:default !important;}";
            head.appendChild(style);
        }

        function btn(text, title) {
            var b = document.createElement("div");
            b.className = "wd-btn";
            b.textContent = text;
            b.title = title;
            return b;
        }

        function updateState(backBtn, forwardBtn) {
            var api = navApi();
            var canBack = false;
            var canForward = false;

            try {
                canBack = typeof api.canGoBack === "function" ? !!api.canGoBack() : history.length > 1;
            } catch (e) {
            }

            try {
                canForward = typeof api.canGoForward === "function" ? !!api.canGoForward() : false;
            } catch (e) {
            }

            backBtn.classList.toggle("wd-disabled", !canBack);
            forwardBtn.classList.toggle("wd-disabled", !canForward);
        }

        function setExpanded(toggle, backBtn, forwardBtn, reloadBtn, value) {
            expanded = !!value;
            toggle.textContent = expanded ? "X" : "=";
            backBtn.classList.toggle("wd-hidden", !expanded);
            forwardBtn.classList.toggle("wd-hidden", !expanded);
            reloadBtn.classList.toggle("wd-hidden", !expanded);
            updateState(backBtn, forwardBtn);
        }

        function attachKeepAlive() {
            if (observer || !document.documentElement)
                return;

            observer = new MutationObserver(function() {
                if (!document.getElementById(ROOT_ID))
                    mount();
            });

            observer.observe(document.documentElement, {
                childList: true,
                subtree: true
            });

            if (!keepAliveTimer) {
                keepAliveTimer = window.setInterval(function() {
                    if (!document.getElementById(ROOT_ID))
                        mount();
                }, 1500);
            }
        }

        function mount() {
            if (!document.body) {
                document.addEventListener("DOMContentLoaded", mount, { once: true });
                return;
            }

            addStyle();
            attachKeepAlive();

            var existing = document.getElementById(ROOT_ID);
            if (existing) {
                existing.style.display = "";
                return;
            }

            var root = document.createElement("div");
            root.id = ROOT_ID;

            var toggle = btn("=", "Toggle navigation buttons");
            var backBtn = btn("<", "Back");
            var forwardBtn = btn(">", "Forward");
            var reloadBtn = btn("R", "Reload");

            backBtn.classList.add("wd-hidden");
            forwardBtn.classList.add("wd-hidden");
            reloadBtn.classList.add("wd-hidden");

            toggle.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                setExpanded(toggle, backBtn, forwardBtn, reloadBtn, !expanded);
            }, true);

            backBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                var api = navApi();
                try {
                    if (!api.goBack || !api.goBack())
                        history.back();
                } catch (e) {
                }
            }, true);

            forwardBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                var api = navApi();
                try {
                    if (!api.goForward || !api.goForward())
                        history.forward();
                } catch (e) {
                }
            }, true);

            reloadBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                var api = navApi();
                try {
                    if (!api.reload || !api.reload())
                        location.reload();
                } catch (e) {
                }
            }, true);

            root.addEventListener("mousedown", function(ev) {
                ev.stopPropagation();
            }, true);

            root.addEventListener("mouseup", function(ev) {
                ev.stopPropagation();
            }, true);

            root.appendChild(toggle);
            root.appendChild(backBtn);
            root.appendChild(forwardBtn);
            root.appendChild(reloadBtn);
            document.body.appendChild(root);

            setExpanded(toggle, backBtn, forwardBtn, reloadBtn, false);

            window.addEventListener("popstate", function() {
                updateState(backBtn, forwardBtn);
            }, true);
            window.addEventListener("pageshow", function() {
                updateState(backBtn, forwardBtn);
                mount();
            }, true);
            window.addEventListener("hashchange", function() {
                updateState(backBtn, forwardBtn);
            }, true);
        }

        window.__wdNavApi = window.__wdNavApi || {};
        window.__wdNavApi.ensureOverlay = mount;
        mount();
    })();
}
