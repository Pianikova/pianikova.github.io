import { postExcerpt, siteLinks } from "./filter.js";

const UPSERT_POST = `
  INSERT INTO posts (chat_id, message_id, posted_at, event_at, update_id, matches, excerpt, telegram_url)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  ON CONFLICT (chat_id, message_id) DO UPDATE SET
    posted_at = excluded.posted_at,
    event_at = excluded.event_at,
    update_id = excluded.update_id,
    matches = excluded.matches,
    excerpt = excluded.excerpt,
    telegram_url = excluded.telegram_url
  WHERE excluded.event_at > posts.event_at
     OR (excluded.event_at = posts.event_at AND excluded.update_id > posts.update_id)`;

export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname;
    if (path === "/feed" && request.method === "GET") return feed(env);
    if (path === "/telegram/webhook" && request.method === "POST") return webhook(request, env);
    return new Response("Not found", { status: 404 });
  },
};

async function feed(env) {
  if (!env.DB) return new Response("Feed unavailable", { status: 503 });

  const { results } = await env.DB.prepare(`
    SELECT message_id, posted_at, excerpt, telegram_url
    FROM posts WHERE matches = 1
    ORDER BY posted_at DESC, message_id DESC LIMIT 5
  `).all();

  return Response.json({ posts: results.map(row => ({
    id: row.message_id,
    date: new Date(row.posted_at * 1000).toISOString(),
    text: row.excerpt,
    url: row.telegram_url,
  })) }, {
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=60",
    },
  });
}

async function webhook(request, env) {
  if (!env.DB || !env.TELEGRAM_CHANNEL_ID || !env.TELEGRAM_WEBHOOK_SECRET || !/^[A-Za-z0-9_]+$/u.test(env.CHANNEL_USERNAME ?? "")) {
    return new Response("Webhook unavailable", { status: 503 });
  }

  if (!sameSecret(request.headers.get("X-Telegram-Bot-Api-Secret-Token") ?? "", env.TELEGRAM_WEBHOOK_SECRET)) {
    return new Response("Unauthorized", { status: 401 });
  }

  let update;
  try {
    update = await request.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }

  const post = update?.channel_post ?? update?.edited_channel_post;
  if (!post || String(post.chat?.id) !== String(env.TELEGRAM_CHANNEL_ID)) {
    return new Response("OK");
  }

  if (!Number.isInteger(post.message_id) || !Number.isInteger(post.date) || !Number.isInteger(update.update_id)) {
    return new Response("Invalid post", { status: 400 });
  }

  const links = siteLinks(post);
  const eventAt = Number.isInteger(post.edit_date) ? post.edit_date : post.date;
  const excerpt = postExcerpt(post) || links[0] || "";
  const telegramUrl = `https://t.me/${env.CHANNEL_USERNAME}/${post.message_id}`;
  await env.DB.prepare(UPSERT_POST).bind(
    String(post.chat.id), post.message_id, post.date, eventAt, update.update_id,
    links.length > 0 ? 1 : 0, excerpt, telegramUrl,
  ).run();

  return new Response("OK");
}

function sameSecret(received, expected) {
  if (received.length !== expected.length) return false;
  let difference = 0;
  for (let index = 0; index < received.length; index++) {
    difference |= received.charCodeAt(index) ^ expected.charCodeAt(index);
  }
  return difference === 0;
}
