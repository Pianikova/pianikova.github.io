window.pianikovaLanguage = {
    getSaved: () => {
        try {
            return window.localStorage.getItem("pianikova.language");
        } catch {
            return null;
        }
    },

    getBrowserLanguages: () => {
        const languages = window.navigator.languages;
        return languages && languages.length > 0
            ? Array.from(languages)
            : [window.navigator.language || ""];
    },

    resolve: (supportedLanguages, fallbackLanguage) => {
        const normalize = language => (language || "").toLowerCase().split(/[-_]/)[0];
        const supported = supportedLanguages.map(normalize);
        const match = language => {
            const normalized = normalize(language);
            return supported.includes(normalized) ? normalized : null;
        };

        const query = new URLSearchParams(window.location.search).get("lang");
        if (match(query)) return match(query);

        const saved = window.pianikovaLanguage.getSaved();
        if (match(saved)) return match(saved);

        for (const language of window.pianikovaLanguage.getBrowserLanguages()) {
            if (match(language)) return match(language);
        }

        return match(fallbackLanguage) || supported[0] || "en";
    },

    apply: language => {
        document.documentElement.lang = language;
    },

    save: language => {
        try {
            window.localStorage.setItem("pianikova.language", language);
        } catch {
            // The selected language still applies for the current page.
        }

        document.documentElement.lang = language;
        const url = new URL(window.location.href);
        url.searchParams.set("lang", language);
        window.location.replace(url.toString());
    }
};
