import assert from "node:assert/strict";
import test from "node:test";
import worker from "./worker.js";

function environment() {
  const writes = [];
  return {
    CHANNEL_USERNAME: "gattavasis",
    TELEGRAM_CHANNEL_ID: "-1001234567890",
    TELEGRAM_WEBHOOK_SECRET: "shared-secret",
    DB: {
      prepare(sql) {
        return {
          bind(...values) { writes.push({ sql, values }); return this; },
          async run() { return { success: true }; },
          async all() { return { results: [] }; },
        };
      },
    },
    writes,
  };
}

function postRequest(update, secret = "shared-secret") {
  return new Request("https://feed.example/telegram/webhook", {
    method: "POST",
    headers: { "X-Telegram-Bot-Api-Secret-Token": secret },
    body: JSON.stringify(update),
  });
}

test("webhook ignores unauthorized and unrelated channel updates", async () => {
  const env = environment();
  const update = { update_id: 1, channel_post: { message_id: 10, date: 100, chat: { id: -1001234567890 }, text: "pianikova.com" } };
  assert.equal((await worker.fetch(postRequest(update, "wrong"), env)).status, 401);
  assert.equal(env.writes.length, 0);
  update.channel_post.chat.id = -1009876543210;
  assert.equal((await worker.fetch(postRequest(update), env)).status, 200);
  assert.equal(env.writes.length, 0);
});

test("webhook stores matches and tombstones edits without a site link", async () => {
  const env = environment();
  const post = { message_id: 10, date: 100, chat: { id: -1001234567890 }, text: "https://pianikova.com/concert" };
  assert.equal((await worker.fetch(postRequest({ update_id: 1, channel_post: post }), env)).status, 200);
  assert.deepEqual(env.writes[0].values.slice(0, 6), ["-1001234567890", 10, 100, 100, 1, 1]);
  assert.equal(env.writes[0].values[7], "https://t.me/gattavasis/10");

  assert.equal((await worker.fetch(postRequest({
    update_id: 2,
    edited_channel_post: { ...post, edit_date: 200, text: "Ссылка убрана" },
  }), env)).status, 200);
  assert.deepEqual(env.writes[1].values.slice(3, 6), [200, 2, 0]);
});

test("public feed contains no bot secrets", async () => {
  const response = await worker.fetch(new Request("https://feed.example/feed"), environment());
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), "*");
  assert.deepEqual(await response.json(), { posts: [] });
});
