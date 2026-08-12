window.pianikovaScrollEffects = (() => {
    let cleanup = null;

    const initialize = () => {
        cleanup?.();

        const root = document.documentElement;
        const header = document.querySelector(".site-header");
        const hero = document.querySelector(".hero");
        const progress = document.querySelector(".scroll-progress-value");
        const navigationLinks = Array.from(document.querySelectorAll('.main-nav a[href^="#"]'));
        const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
        const wideScreen = matchMedia("(min-width: 901px)");
        const disposers = [];

        if (!header || !hero || !progress) return;

        const revealTargets = Array.from(document.querySelectorAll(
            ".numbered-section, .press, .next-concert, .project-card, .concert-row"
        ));

        revealTargets.forEach((element, index) => {
            element.classList.add("reveal-on-scroll");
            element.style.setProperty("--reveal-delay", `${Math.min(index % 4, 3) * 70}ms`);
        });

        if (reducedMotion) {
            revealTargets.forEach(element => element.classList.add("is-visible"));
        } else {
            const revealObserver = new IntersectionObserver(entries => {
                entries.forEach(entry => {
                    if (!entry.isIntersecting) return;
                    entry.target.classList.add("is-visible");
                    revealObserver.unobserve(entry.target);
                });
            }, { rootMargin: "0px 0px -8%", threshold: 0.08 });
            revealTargets.forEach(element => revealObserver.observe(element));
            disposers.push(() => revealObserver.disconnect());
        }

        const sectionLinks = new Map(navigationLinks.map(link => [link.hash.slice(1), link]));
        const observedSections = Array.from(sectionLinks.keys())
            .map(id => document.getElementById(id))
            .filter(Boolean);
        const sectionObserver = new IntersectionObserver(entries => {
            const visible = entries
                .filter(entry => entry.isIntersecting)
                .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
            if (!visible) return;
            navigationLinks.forEach(link => link.classList.toggle("active", link === sectionLinks.get(visible.target.id)));
        }, { rootMargin: "-25% 0px -60%", threshold: [0, 0.15, 0.5] });
        observedSections.forEach(section => sectionObserver.observe(section));
        disposers.push(() => sectionObserver.disconnect());

        let ticking = false;
        const updateScrollState = () => {
            const scrollTop = Math.max(window.scrollY, 0);
            const scrollRange = Math.max(document.documentElement.scrollHeight - innerHeight, 1);
            progress.style.transform = `scaleX(${Math.min(scrollTop / scrollRange, 1)})`;
            header.classList.toggle("is-scrolled", scrollTop > Math.min(hero.offsetHeight * 0.55, 440));

            if (!reducedMotion && wideScreen.matches) {
                const parallax = Math.min(scrollTop * 0.1, 42);
                hero.style.setProperty("--hero-parallax", `${parallax}px`);
                hero.style.setProperty("--hero-shade-parallax", `${parallax * 0.35}px`);
            } else {
                hero.style.removeProperty("--hero-parallax");
                hero.style.removeProperty("--hero-shade-parallax");
            }
            ticking = false;
        };
        const onScroll = () => {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(updateScrollState);
        };
        addEventListener("scroll", onScroll, { passive: true });
        addEventListener("resize", onScroll, { passive: true });
        updateScrollState();
        root.classList.add("scroll-effects-ready");

        disposers.push(() => {
            removeEventListener("scroll", onScroll);
            removeEventListener("resize", onScroll);
            root.classList.remove("scroll-effects-ready");
        });
        cleanup = () => disposers.splice(0).forEach(dispose => dispose());
    };

    return { initialize };
})();
