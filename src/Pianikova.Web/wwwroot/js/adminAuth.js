window.pianikovaAdmin = {
    // Opens the GitHub OAuth popup and resolves with the access token once the
    // Cloudflare Worker (cloudflare/decap-oauth) hands it back via postMessage.
    // This mirrors the handshake Decap CMS's own admin page used to perform —
    // the worker itself is unchanged, only the "opener" is now this page.
    login: (authUrl) => new Promise((resolve, reject) => {
        const popup = window.open(authUrl, "github-oauth", "width=600,height=700");
        if (!popup) {
            reject("Браузер заблокировал всплывающее окно. Разрешите всплывающие окна для этого сайта и попробуйте снова.");
            return;
        }

        function onMessage(e) {
            if (e.source !== popup) {
                return;
            }

            if (e.data === "authorizing:github") {
                popup.postMessage("authorizing:github", "*");
                return;
            }

            if (typeof e.data === "string" && e.data.startsWith("authorization:github:success:")) {
                window.removeEventListener("message", onMessage);
                popup.close();
                const payload = JSON.parse(e.data.slice("authorization:github:success:".length));
                resolve(payload.token);
            }
        }

        window.addEventListener("message", onMessage);
    }),

    getToken: () => {
        try {
            return window.sessionStorage.getItem("pianikova.admin.token");
        } catch {
            return null;
        }
    },

    setToken: (token) => {
        try {
            window.sessionStorage.setItem("pianikova.admin.token", token);
        } catch {
            // Editing still works for the current page load.
        }
    },

    clearToken: () => {
        try {
            window.sessionStorage.removeItem("pianikova.admin.token");
            window.sessionStorage.removeItem("pianikova.admin.user");
        } catch {
            // Nothing to clean up.
        }
    },

    getUsername: () => {
        try {
            return window.sessionStorage.getItem("pianikova.admin.user");
        } catch {
            return null;
        }
    },

    setUsername: (username) => {
        try {
            window.sessionStorage.setItem("pianikova.admin.user", username);
        } catch {
            // Editing still works for the current page load.
        }
    }
};
