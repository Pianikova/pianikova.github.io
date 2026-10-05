const URL_PATTERN = /(?:https?:\/\/|www\.)[^\s<>"']*/giu;
const BARE_SITE_PATTERN = /(?:^|[\s(«„“])(?<url>pianikova\.com(?![\p{L}\p{N}_.:@-])(?:[/?#][^\s<>"']*)?)/giu;

export function siteLinks(message) {
  const candidates = [];
  collectTextLinks(message.text, message.entities, candidates);
  collectTextLinks(message.caption, message.caption_entities, candidates);

  for (const row of message.reply_markup?.inline_keyboard ?? []) {
    for (const button of row) {
      if (button.url) candidates.push(button.url);
    }
  }

  if (message.link_preview_options?.url) {
    candidates.push(message.link_preview_options.url);
  }

  return [...new Set(candidates.map(siteUrl).filter(Boolean))];
}

export function postExcerpt(message) {
  const source = (message.text || message.caption || "").replace(/\s+/gu, " ").trim();
  const characters = [...source];
  return characters.length > 360 ? `${characters.slice(0, 359).join("")}…` : source;
}

function collectTextLinks(text, entities, candidates) {
  if (typeof text !== "string") return;

  for (const entity of entities ?? []) {
    if (entity.type === "text_link" && entity.url) {
      candidates.push(entity.url);
    } else if (entity.type === "url" && Number.isInteger(entity.offset) && Number.isInteger(entity.length)) {
      // Telegram entity offsets use UTF-16 code units, like JavaScript string indices.
      candidates.push(text.slice(entity.offset, entity.offset + entity.length));
    }
  }

  for (const match of text.matchAll(URL_PATTERN)) {
    candidates.push(match[0]);
  }
  for (const match of text.matchAll(BARE_SITE_PATTERN)) {
    candidates.push(match.groups.url);
  }
}

function siteUrl(raw) {
  if (typeof raw !== "string") return null;
  const candidate = raw.trim().replace(/[.,!?;:)}\]]+$/u, "");
  try {
    const url = new URL(/^https?:\/\//iu.test(candidate) ? candidate : `https://${candidate}`);
    if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.port) return null;
    return ["pianikova.com", "www.pianikova.com"].includes(url.hostname.toLowerCase()) ? url.href : null;
  } catch {
    return null;
  }
}
