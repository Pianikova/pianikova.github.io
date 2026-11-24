import assert from "node:assert/strict";
import test from "node:test";
import worker from "./worker.js";

function environment() {
  const writes = [];
  const reads = [];
  return {
    CHANNEL_USERNAME: "gattavasis",
    TELEGRAM_CHANNEL_ID: "-1001234567890",
    TELEGRAM_WEBHOOK_SECRET: "shared-secret",
    DB: {
      prepare(sql) {
        return {
          bind(...values) { writes.push({ sql, values }); return this; },
          async run() { return { success: true }; },
          async all() { reads.push(sql); return { results: [] }; },
        };
      },
    },
    writes,
    reads,
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

test("webhook keeps posts and edits without a site link", async () => {
  const env = environment();
  const post = { message_id: 10, date: 100, chat: { id: -1001234567890 }, text: "https://pianikova.com/concert" };
  assert.equal((await worker.fetch(postRequest({ update_id: 1, channel_post: post }), env)).status, 200);
  assert.deepEqual(env.writes[0].values.slice(0, 6), ["-1001234567890", 10, 100, 100, 1, 1]);
  assert.equal(env.writes[0].values[7], "https://t.me/gattavasis/10");

  assert.equal((await worker.fetch(postRequest({
    update_id: 2,
    edited_channel_post: { ...post, edit_date: 200, text: "Ссылка убрана" },
  }), env)).status, 200);
  assert.deepEqual(env.writes[1].values.slice(3, 6), [200, 2, 1]);
});

test("public feed contains no bot secrets", async () => {
  const response = await worker.fetch(new Request("https://feed.example/feed"), environment());
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), "*");
  assert.deepEqual(await response.json(), { posts: [] });
});


test("webhook accepts new text, captions and media without site links", async () => {
  for (const content of [{ text: "Новости канала" }, { caption: "Афиша" }, { photo: [{ file_id: "photo" }] }]) {
    const env = environment();
    const response = await worker.fetch(postRequest({ update_id: 1, channel_post: {
      message_id: 11, date: 100, chat: { id: -1001234567890 }, ...content,
    } }), env);
    assert.equal(response.status, 200);
    assert.equal(env.writes[0].values[5], 1);
    assert.equal(env.writes[0].values[6], content.text || content.caption || "Telegram");
  }
});

test("feed includes previously stored posts without site links", async () => {
  const env = environment();
  await worker.fetch(new Request("https://feed.example/feed"), env);
  assert.doesNotMatch(env.reads[0], /matches\s*=/u);
});

test("database failures leave webhook delivery retryable", async () => {
  const env = environment();
  env.DB.prepare = () => ({ bind() { return this; }, async run() { throw new Error("DB unavailable"); } });
  await assert.rejects(worker.fetch(postRequest({ update_id: 1, channel_post: {
    message_id: 11, date: 100, chat: { id: -1001234567890 }, text: "Новости",
  } }), env), /DB unavailable/u);
});
