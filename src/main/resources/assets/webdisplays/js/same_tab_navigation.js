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

        function loadInSameTab(rawUrl) {
            const next = toUrl(rawUrl);
            if (!next)
                return false;

            if (next.startsWith("javascript:"))
                return false;

            window.location.href = next;
            return true;
        }

        const originalOpen = window.open;
        window.open = function(url, target) {
            const t = typeof target === "string" ? target.toLowerCase() : "";
            if (t === "" || t === "_blank" || t === "_new")
                if (loadInSameTab(url))
                    return window;

            return originalOpen ? originalOpen.apply(this, arguments) : null;
        };

        document.addEventListener("click", function(ev) {
            let node = ev.target;

            while (node) {
                if (node.tagName === "A") {
                    const href = node.getAttribute("href");
                    const target = (node.getAttribute("target") || "").toLowerCase();

                    if (href && (target === "_blank" || target === "_new")) {
                        if (loadInSameTab(href)) {
                            ev.preventDefault();
                            ev.stopPropagation();
                        }
                        return;
                    }
                }

                node = node.parentElement;
            }
        }, true);
    })();
}
