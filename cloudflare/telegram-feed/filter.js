export function postExcerpt(message) {
  const source = (message.text || message.caption || "").replace(/\s+/gu, " ").trim();
  const characters = [...source];
  return characters.length > 360 ? `${characters.slice(0, 359).join("")}…` : source;
}
