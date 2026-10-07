window.pianikovaJournal = (() => {
    let progressBound = false;
    let lightbox = null;

    const bindProgress = () => {
        if (progressBound) return;
        progressBound = true;
        let ticking = false;
        const update = () => {
            ticking = false;
            const progress = document.querySelector(".journal-header ~ .scroll-progress .scroll-progress-value");
            if (!progress) return;
            const article = document.querySelector(".journal-article");
            let ratio;
            if (article) {
                const rect = article.getBoundingClientRect();
                ratio = (innerHeight - rect.top) / Math.max(rect.height, 1);
            } else {
                ratio = scrollY / Math.max(document.documentElement.scrollHeight - innerHeight, 1);
            }
            progress.style.transform = `scaleX(${Math.min(Math.max(ratio, 0), 1)})`;
        };
        const schedule = () => {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(update);
        };
        addEventListener("scroll", schedule, { passive: true });
        addEventListener("resize", schedule, { passive: true });
        schedule();
    };

    const duration = seconds => {
        const value = Number(seconds);
        if (!Number.isFinite(value) || value <= 0) return "";
        const minutes = Math.floor(value / 60);
        return `${minutes}:${String(Math.floor(value % 60)).padStart(2, "0")}`;
    };

    const element = (tag, className, text) => {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text) node.textContent = text;
        return node;
    };

    // A link or image that is the only content of its paragraph replaces the whole paragraph.
    const replaceBlock = (node, replacement) => {
        const parent = node.parentElement;
        if (parent && parent.tagName === "P" && parent.textContent.trim() === node.textContent.trim() && parent.children.length === 1) {
            parent.replaceWith(replacement);
        } else {
            node.replaceWith(replacement);
        }
    };

    const enhanceAudio = (body, labels) => {
        body.querySelectorAll("a.audio").forEach(link => {
            const local = /\.(m4a|mp3|ogg|oga|wav)$/i.test(new URL(link.href).pathname);
            const card = element(local ? "div" : "a", "journal-audio");
            if (!local) {
                card.href = link.href;
                card.target = "_blank";
                card.rel = "noopener noreferrer";
            }
            card.append(element("span", "journal-audio-icon", local ? "♪" : "▶"));
            const info = element("span", "journal-audio-info");
            info.append(element("span", "journal-audio-title", link.textContent));
            const meta = element("span", "journal-audio-meta", [duration(link.dataset.duration), local ? "" : `${labels.listen} ↗`].filter(Boolean).join(" · "));
            info.append(meta);
            card.append(info);
            if (local) {
                card.classList.add("is-local");
                const audio = element("audio");
                audio.controls = true;
                audio.preload = "none";
                audio.src = link.href;
                card.append(audio);
            }
            replaceBlock(link, card);
        });

        // Consecutive tracks read as one playlist.
        body.querySelectorAll(".journal-audio").forEach(card => {
            if (card.parentElement?.classList.contains("journal-playlist")) return;
            const list = element("div", "journal-playlist");
            card.before(list);
            let next = card;
            while (next && next.classList?.contains("journal-audio")) {
                const following = next.nextElementSibling;
                list.append(next);
                next = following;
            }
        });
    };

    const enhanceVideo = (body, labels) => {
        body.querySelectorAll("a.video").forEach(link => {
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            link.setAttribute("aria-label", labels.watch);
            const badge = element("span", "journal-video-badge");
            badge.append(element("span", "journal-video-play", "▶"));
            const caption = [duration(link.dataset.duration), `${labels.watch} ↗`].filter(Boolean).join(" · ");
            badge.append(element("span", "", caption));
            link.append(badge);
            const parent = link.parentElement;
            if (parent?.tagName === "P") parent.classList.add("journal-media");
        });
    };

    const enhanceSpoilers = (body, labels) => {
        body.querySelectorAll(".spoiler").forEach(spoiler => {
            spoiler.tabIndex = 0;
            spoiler.setAttribute("role", "button");
            spoiler.setAttribute("aria-label", labels.spoiler);
            spoiler.title = labels.spoiler;
            const reveal = event => {
                if (spoiler.classList.contains("revealed")) return;
                event.preventDefault();
                event.stopPropagation();
                spoiler.classList.add("revealed");
                spoiler.removeAttribute("role");
                spoiler.removeAttribute("aria-label");
                spoiler.removeAttribute("title");
            };
            spoiler.addEventListener("click", reveal);
            spoiler.addEventListener("keydown", event => {
                if (event.key === "Enter" || event.key === " ") reveal(event);
            });
        });
    };

    const ensureLightbox = labels => {
        if (lightbox) return lightbox;
        const root = element("div", "journal-lightbox");
        root.hidden = true;
        root.setAttribute("role", "dialog");
        root.setAttribute("aria-modal", "true");
        const image = element("img");
        image.alt = "";
        const counter = element("p", "journal-lightbox-count");
        const close = element("button", "journal-lightbox-close", "✕");
        const previous = element("button", "journal-lightbox-prev", "‹");
        const next = element("button", "journal-lightbox-next", "›");
        close.type = previous.type = next.type = "button";
        close.setAttribute("aria-label", labels.close);
        previous.setAttribute("aria-label", "←");
        next.setAttribute("aria-label", "→");
        root.append(image, counter, close, previous, next);
        document.body.append(root);

        const state = { images: [], index: 0, opener: null };
        const show = index => {
            state.index = (index + state.images.length) % state.images.length;
            const source = state.images[state.index];
            image.src = source.getAttribute("src");
            image.style.backgroundColor = source.style.backgroundColor || "";
            counter.textContent = state.images.length > 1 ? `${state.index + 1} / ${state.images.length}` : "";
            previous.hidden = next.hidden = state.images.length < 2;
        };
        const hide = () => {
            root.hidden = true;
            document.documentElement.classList.remove("journal-lightbox-open");
            state.opener?.focus?.();
        };
        close.addEventListener("click", hide);
        previous.addEventListener("click", () => show(state.index - 1));
        next.addEventListener("click", () => show(state.index + 1));
        root.addEventListener("click", event => { if (event.target === root) hide(); });
        addEventListener("keydown", event => {
            if (root.hidden) return;
            if (event.key === "Escape") hide();
            else if (event.key === "ArrowLeft") show(state.index - 1);
            else if (event.key === "ArrowRight") show(state.index + 1);
        });
        let touchStart = null;
        root.addEventListener("touchstart", event => { touchStart = event.touches[0].clientX; }, { passive: true });
        root.addEventListener("touchend", event => {
            if (touchStart === null) return;
            const delta = event.changedTouches[0].clientX - touchStart;
            touchStart = null;
            if (Math.abs(delta) > 50) show(state.index + (delta < 0 ? 1 : -1));
        });

        lightbox = {
            open: (images, index, opener) => {
                state.images = images;
                state.opener = opener;
                show(index);
                root.hidden = false;
                document.documentElement.classList.add("journal-lightbox-open");
                close.focus();
            }
        };
        return lightbox;
    };

    const enhanceImages = (body, labels) => {
        const images = Array.from(body.querySelectorAll("img")).filter(image => !image.closest("a"));
        images.forEach((image, index) => {
            const parent = image.parentElement;
            if (parent?.tagName === "P" && !parent.closest(".gallery") && parent.textContent.trim() === "") parent.classList.add("journal-figure");
            image.tabIndex = 0;
            image.addEventListener("load", () => image.classList.add("is-loaded"), { once: true });
            if (image.complete) image.classList.add("is-loaded");
            const open = event => {
                if (image.classList.contains("spoiler") && !image.classList.contains("revealed")) return;
                event.preventDefault();
                ensureLightbox(labels).open(images, index, image);
            };
            image.addEventListener("click", open);
            image.addEventListener("keydown", event => { if (event.key === "Enter") open(event); });
        });
        body.querySelectorAll(".gallery").forEach(gallery => {
            const count = gallery.querySelectorAll("img").length;
            gallery.dataset.count = String(Math.min(count, 5));
        });
        body.querySelectorAll("a.video img").forEach(image => {
            image.addEventListener("load", () => image.classList.add("is-loaded"), { once: true });
            if (image.complete) image.classList.add("is-loaded");
        });
    };

    return {
        page: () => bindProgress(),

        issue: (selector, labels) => {
            bindProgress();
            window.scrollTo({ top: 0, behavior: "instant" });
            const body = document.querySelector(selector);
            if (!body || body.dataset.enhanced === "true") return;
            body.dataset.enhanced = "true";
            enhanceAudio(body, labels);
            enhanceVideo(body, labels);
            enhanceSpoilers(body, labels);
            enhanceImages(body, labels);
            const opening = body.querySelector(":scope > p:not(.journal-figure):not(.journal-media)");
            if (opening && opening.textContent.trim().length > 40 && !opening.querySelector("img")) opening.classList.add("journal-dropcap");
        }
    };
})();
