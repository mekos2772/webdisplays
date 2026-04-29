{
    (function() {
        if (window.__wdVirtualNavInstalled)
            return;

        window.__wdVirtualNavInstalled = true;
        var ROOT_ID = "wd-nav-root";
        var STYLE_ID = "wd-nav-style";
        var expanded = false;

        function addStyle() {
            if (document.getElementById(STYLE_ID))
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
            document.head.appendChild(style);
        }

        function btn(text, title) {
            var b = document.createElement("div");
            b.className = "wd-btn";
            b.textContent = text;
            b.title = title;
            return b;
        }

        function updateState(backBtn, forwardBtn) {
            var canBack = false;
            try {
                canBack = history.length > 1;
            } catch (e) {}

            if (canBack) backBtn.classList.remove("wd-disabled");
            else backBtn.classList.add("wd-disabled");
            forwardBtn.classList.remove("wd-disabled");
        }

        function mount() {
            if (!document.body) {
                document.addEventListener("DOMContentLoaded", mount, { once: true });
                return;
            }

            if (document.getElementById(ROOT_ID))
                return;

            addStyle();

            var root = document.createElement("div");
            root.id = ROOT_ID;

            var toggle = btn("=", "显示/隐藏导航按钮");
            var backBtn = btn("<", "后退");
            var forwardBtn = btn(">", "前进");
            var reloadBtn = btn("R", "刷新");

            backBtn.classList.add("wd-hidden");
            forwardBtn.classList.add("wd-hidden");
            reloadBtn.classList.add("wd-hidden");

            function setExpanded(v) {
                expanded = !!v;
                toggle.textContent = expanded ? "X" : "=";
                backBtn.classList.toggle("wd-hidden", !expanded);
                forwardBtn.classList.toggle("wd-hidden", !expanded);
                reloadBtn.classList.toggle("wd-hidden", !expanded);
                updateState(backBtn, forwardBtn);
            }

            toggle.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                setExpanded(!expanded);
            }, true);

            backBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                try { history.back(); } catch (e) {}
            }, true);

            forwardBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                try { history.forward(); } catch (e) {}
            }, true);

            reloadBtn.addEventListener("click", function(ev) {
                ev.preventDefault();
                ev.stopPropagation();
                try { location.reload(); } catch (e) {}
            }, true);

            root.addEventListener("mousedown", function(ev) {
                ev.stopPropagation();
            }, true);

            root.appendChild(toggle);
            root.appendChild(backBtn);
            root.appendChild(forwardBtn);
            root.appendChild(reloadBtn);
            document.body.appendChild(root);

            setExpanded(false);
            window.addEventListener("popstate", function() {
                updateState(backBtn, forwardBtn);
            });
        }

        mount();
    })();
}
