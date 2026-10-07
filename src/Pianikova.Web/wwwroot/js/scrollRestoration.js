// Pages render asynchronously (content, then the journal), so on Back/Forward the browser's own
// scroll restoration lands on a page that is still short and clamps to the top. Remember the
// position of every visited URL and, after a history navigation, keep scrolling back to it while
// the page grows, until it is reached, the reader scrolls themselves, or the page stops growing.
// A reload or a return from another site restores the same way once the app has booted.
(() => {
    const storageKey = "pianikova-scroll-positions";
    const maxEntries = 50;
    const idleTimeout = 4000;
    const bootTimeout = 20000;
    const settleTime = 500;

    const read = () => {
        try { return JSON.parse(sessionStorage.getItem(storageKey)) ?? {}; }
        catch { return {}; }
    };
    const positions = read();
    const write = () => {
        try { sessionStorage.setItem(storageKey, JSON.stringify(positions)); }
        catch { /* Private mode or a full storage only loses restoration. */ }
    };

    if ("scrollRestoration" in history) history.scrollRestoration = "manual";

    let restoring = null;

    const save = () => {
        if (restoring) return;
        const url = location.href;
        delete positions[url];
        positions[url] = Math.round(scrollY);
        const urls = Object.keys(positions);
        urls.slice(0, Math.max(urls.length - maxEntries, 0)).forEach(old => delete positions[old]);
        write();
    };

    let saveScheduled = false;
    addEventListener("scroll", () => {
        if (saveScheduled) return;
        saveScheduled = true;
        setTimeout(() => { saveScheduled = false; save(); }, 100);
    }, { passive: true });
    // Capture phase: runs before Blazor's router intercepts the link and swaps the page.
    addEventListener("click", save, { capture: true });
    addEventListener("pagehide", save);

    const stop = () => {
        if (!restoring) return;
        restoring.dispose();
        restoring = null;
    };

    const restore = (target, timeout) => {
        stop();
        if (!target) return;

        // The previous page may still be on screen when this starts, so the position only counts as
        // restored once it has held for a while without the page changing height.
        let height = -1;
        let settledSince = performance.now();
        let deadline = settledSince + timeout;
        const attempt = () => {
            const now = performance.now();
            const grown = document.documentElement.scrollHeight;
            if (grown !== height) {
                height = grown;
                settledSince = now;
                deadline = Math.max(deadline, now + idleTimeout);
            }
            if (Math.abs(scrollY - target) > 1) {
                scrollTo({ top: target, behavior: "instant" });
                settledSince = now;
            }
            if (now - settledSince > settleTime || now > deadline) stop();
        };
        const timer = setInterval(attempt, 50);
        const interruptions = ["wheel", "touchstart", "keydown", "mousedown"];
        interruptions.forEach(type => addEventListener(type, stop, { passive: true }));

        restoring = {
            dispose: () => {
                clearInterval(timer);
                interruptions.forEach(type => removeEventListener(type, stop));
            }
        };
        attempt();
    };

    window.pianikovaScrollRestoration = { isRestoring: () => restoring !== null };

    // Registered before Blazor starts, so this runs before the router renders the new page.
    addEventListener("popstate", () => restore(positions[location.href], idleTimeout));

    const navigationType = performance.getEntriesByType("navigation")[0]?.type;
    if (navigationType === "reload" || navigationType === "back_forward") restore(positions[location.href], bootTimeout);
})();
