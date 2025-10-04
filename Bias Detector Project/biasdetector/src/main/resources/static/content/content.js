// Extract readable text and handle highlight insertion

function extractReadableText() {
  // Heuristic: prefer <article>, else main, else body
  const root =
    document.querySelector('article') ||
    document.querySelector('main') ||
    document.body;

  const title =
    document.title ||
    document.querySelector('h1')?.innerText ||
    location.hostname;

  // Get visible text only
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode: (node) => {
      const parent = node.parentElement;
      if (!parent) return NodeFilter.FILTER_REJECT;
      const style = window.getComputedStyle(parent);
      if (style && (style.visibility === 'hidden' || style.display === 'none')) {
        return NodeFilter.FILTER_REJECT;
      }
      const text = node.nodeValue?.trim();
      if (!text) return NodeFilter.FILTER_REJECT;
      return NodeFilter.FILTER_ACCEPT;
    }
  });

  const parts = [];
  let n;
  const maxChars = 100000; // safety cap
  let count = 0;
  while ((n = walker.nextNode())) {
    let t = n.nodeValue.replace(/\s+/g, ' ').trim();
    if (!t) continue;
    if (count + t.length > maxChars) break;
    parts.push(t);
    count += t.length;
  }

  return {
    title,
    text: parts.join(' ')
  };
}

// Naive highlighter by exact substring match (case-sensitive)
function highlightPhrases(phrases) {
  if (!Array.isArray(phrases) || !phrases.length) return 0;

  const root = document.querySelector('article') ||
               document.querySelector('main') || document.body;

  let highlightsApplied = 0;

  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
  const textNodes = [];
  let node;
  while ((node = walker.nextNode())) {
    if (node.nodeValue && node.nodeValue.trim()) textNodes.push(node);
  }

  phrases.forEach((phrase) => {
    if (!phrase || phrase.length < 2) return;
    textNodes.forEach((textNode) => {
      const idx = textNode.nodeValue.indexOf(phrase);
      if (idx !== -1) {
        const range = document.createRange();
        range.setStart(textNode, idx);
        range.setEnd(textNode, idx + phrase.length);

        const mark = document.createElement('mark');
        mark.className = 'bias-highlight';
        range.surroundContents(mark);
        highlightsApplied += 1;
      }
    });
  });

  return highlightsApplied;
}

// Messages from popup
chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type === 'GET_PAGE_TEXT') {
    sendResponse(extractReadableText());
    return true;
  }

  if (msg?.type === 'APPLY_HIGHLIGHTS') {
    const count = highlightPhrases(msg.highlights || []);
    sendResponse?.({ ok: true, count });
    return true;
  }
});
