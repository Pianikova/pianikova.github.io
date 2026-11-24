import assert from "node:assert/strict";
import test from "node:test";
import { postExcerpt } from "./filter.js";

test("keeps preview text plain and bounded", () => {
  assert.equal(postExcerpt({ caption: "Скоро концерт\n\nв Москве" }), "Скоро концерт в Москве");
  assert.equal([...postExcerpt({ text: "🎻".repeat(500) })].length, 360);
});
