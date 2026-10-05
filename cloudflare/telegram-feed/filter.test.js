import assert from "node:assert/strict";
import test from "node:test";
import { postExcerpt, siteLinks } from "./filter.js";

test("accepts the exact site host in text, captions, hidden links and buttons", () => {
  const message = {
    text: "Сайт: https://pianikova.com/concerts и новости",
    entities: [{ type: "url", offset: 6, length: 31 }],
    caption: "Афиша",
    caption_entities: [{ type: "text_link", url: "https://www.pianikova.com/", offset: 0, length: 5 }],
    reply_markup: { inline_keyboard: [[{ text: "Tickets", url: "https://pianikova.com/tickets" }]] },
  };
  assert.deepEqual(siteLinks(message), [
    "https://pianikova.com/concerts",
    "https://www.pianikova.com/",
    "https://pianikova.com/tickets",
  ]);
});

test("accepts a bare site address and Telegram UTF-16 entity offsets", () => {
  assert.deepEqual(siteLinks({ text: "🎻 pianikova.com/news" }), ["https://pianikova.com/news"]);
  assert.deepEqual(siteLinks({
    text: "🎻 ссылка",
    entities: [{ type: "text_link", offset: 3, length: 6, url: "https://pianikova.com/" }],
  }), ["https://pianikova.com/"]);
});

test("rejects lookalike domains, credentials and other sites containing the name", () => {
  assert.deepEqual(siteLinks({
    text: "https://pianikova.com.evil.test pianikova.com.evil.test https://evil.test/pianikova.com notpianikova.com https://pianikova.com@evil.test/",
  }), []);
});

test("keeps preview text plain and bounded", () => {
  assert.equal(postExcerpt({ caption: "Скоро концерт\n\nв Москве" }), "Скоро концерт в Москве");
  assert.equal([...postExcerpt({ text: "🎻".repeat(500) })].length, 360);
});
