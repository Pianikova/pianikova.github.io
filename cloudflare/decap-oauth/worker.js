const GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
const GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
const OAUTH_SCOPE = "repo";
const STATE_COOKIE = "oauth_state";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/auth") return handleAuth(url, env);
    if (url.pathname === "/callback") return handleCallback(request, url, env);
    if (url.pathname === "/") return new Response("Decap CMS OAuth provider is running.");
    return new Response("Not found", { status: 404 });
  },
};

async function handleAuth(url, env) {
  const state = crypto.randomUUID();
  const params = new URLSearchParams({
    client_id: env.GITHUB_CLIENT_ID,
    redirect_uri: `${url.origin}/callback`,
    scope: OAUTH_SCOPE,
    state,
  });

  return new Response(null, {
    status: 302,
    headers: {
      Location: `${GITHUB_AUTHORIZE_URL}?${params.toString()}`,
      "Set-Cookie": `${STATE_COOKIE}=${state}; Max-Age=600; Path=/; HttpOnly; Secure; SameSite=Lax`,
    },
  });
}

async function handleCallback(request, url, env) {
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const cookieState = readCookie(request, STATE_COOKIE);

  if (!code || !state || !cookieState || state !== cookieState) {
    return htmlResponse(renderErrorPage("Invalid or missing OAuth state."), 400);
  }

  const tokenResponse = await fetch(GITHUB_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      client_id: env.GITHUB_CLIENT_ID,
      client_secret: env.GITHUB_CLIENT_SECRET,
      code,
      redirect_uri: `${url.origin}/callback`,
    }),
  });

  const data = await tokenResponse.json();

  if (!tokenResponse.ok || data.error || !data.access_token) {
    return htmlResponse(renderErrorPage(data.error_description || "GitHub did not return an access token."), 400);
  }

  return new Response(renderSuccessPage(data.access_token), {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Set-Cookie": `${STATE_COOKIE}=; Max-Age=0; Path=/`,
    },
  });
}

function readCookie(request, name) {
  const header = request.headers.get("Cookie") || "";
  const match = header.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

function renderSuccessPage(token) {
  const message = "authorization:github:success:" + JSON.stringify({ token, provider: "github" });
  const safeMessage = JSON.stringify(message);

  return `<!doctype html>
<html>
<body>
<script>
(function () {
  function receiveMessage(e) {
    window.opener.postMessage(${safeMessage}, e.origin);
    window.removeEventListener("message", receiveMessage, false);
  }
  window.addEventListener("message", receiveMessage, false);
  window.opener.postMessage("authorizing:github", "*");
})();
</script>
<p>Authorized. You can close this window.</p>
</body>
</html>`;
}

function renderErrorPage(message) {
  return `<!doctype html>
<html>
<body>
<p>Authorization failed: ${escapeHtml(message)}</p>
</body>
</html>`;
}

function htmlResponse(html, status) {
  return new Response(html, { status, headers: { "Content-Type": "text/html; charset=utf-8" } });
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
