// ====== UI elements ======
const btnAnalyzePage = document.getElementById('btn-analyze-page');
const btnUpload = document.getElementById('btn-upload');
const filePicker = document.getElementById('file-picker');
const btnAnalyzeUrl = document.getElementById('btn-analyze-url');
const urlInput = document.getElementById('url-input');
const btnAnalyzeText = document.getElementById('btn-analyze-text');
const textInput = document.getElementById('text-input');

const progressBar = document.getElementById('progress-bar');
const progressLabel = document.getElementById('progress-label');

const results = document.getElementById('results');
const sourceValue = document.getElementById('source-value');
const scoreValue = document.getElementById('score-value');
const tagContainer = document.getElementById('tag-container');
const snippetsBox = document.getElementById('snippets');
const toggleHighlights = document.getElementById('toggle-highlights');

document.getElementById('bd-close')?.addEventListener('click', () => window.close());

// ====== Helpers ======
function setProgress(pct, text) {
  progressBar.style.width = `${pct}%`;
  progressLabel.textContent = text ?? `Bias detection — ${pct}%`;
}
function showResults(payload) {
  results.hidden = false;
  sourceValue.textContent = payload?.source?.nameOrTitle ?? '—';
  scoreValue.textContent = Math.round(payload?.score ?? 0);

  // Tags
  tagContainer.innerHTML = '';
  (payload?.categories || []).forEach(tag => {
    const chip = document.createElement('span');
    chip.className = 'bd-tag';
    chip.textContent = tag;
    tagContainer.appendChild(chip);
  });

  // Snippets
  const list = payload?.highlights || [];
  if (!list.length) {
    snippetsBox.textContent = 'No biased snippets found.';
  } else {
    snippetsBox.innerHTML = '';
    list.forEach(h => {
      const div = document.createElement('div');
      div.className = 'bd-snippet';
      div.title = (h.explanation || '').toString();
      div.textContent = h.text;
      snippetsBox.appendChild(div);
    });
  }
}

async function analyzeTextFlow(text, sourceLabel) {
  if (!text || !text.trim()) {
    alert('No text to analyze.');
    return;
  }

  setProgress(20, 'Uploading text…');
  try {
    const res = await BiasAPI.analyzeText(text);
    setProgress(100, 'Done');
    const payload = normalizePayload(res, { type: 'text', nameOrTitle: sourceLabel || 'Copied Text' });
    showResults(payload);

    if (toggleHighlights.checked) {
      // ask content script to highlight snippets by text
      const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
      if (tabs[0]?.id) {
        chrome.tabs.sendMessage(tabs[0].id, {
          type: 'APPLY_HIGHLIGHTS',
          highlights: (payload.highlights || []).map(h => h.text)
        });
      }
    }
  } catch (e) {
    console.error(e);
    alert('Failed to analyze text.');
    setProgress(0, 'Bias detection — 0%');
  }
}

function normalizePayload(apiResponse, source) {
  // Expecting the backend to return a compatible object; attach source for UI
  const p = apiResponse || {};
  p.source = source || p.source || { type: 'unknown', nameOrTitle: 'Unknown' };
  p.categories = p.categories || [];
  p.highlights = p.highlights || [];
  p.score = typeof p.score === 'number' ? p.score : 0;
  return p;
}

// ====== Handlers ======
btnAnalyzePage?.addEventListener('click', async () => {
  setProgress(10, 'Extracting page…');

  // ask content script for page text
  const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
  const tabId = tabs[0]?.id;
  if (!tabId) return;

  const pageText = await chrome.tabs.sendMessage(tabId, { type: 'GET_PAGE_TEXT' });
  if (!pageText || !pageText.text) {
    alert('Could not extract page text.');
    setProgress(0, 'Bias detection — 0%');
    return;
  }

  setProgress(30, 'Analyzing…');
  try {
    const res = await BiasAPI.analyzeText(pageText.text);
    setProgress(100, 'Done');
    const source = { type: 'page', nameOrTitle: pageText.title || 'Current Page' };
    const payload = normalizePayload(res, source);
    showResults(payload);

    if (toggleHighlights.checked) {
      chrome.tabs.sendMessage(tabId, {
        type: 'APPLY_HIGHLIGHTS',
        highlights: (payload.highlights || []).map(h => h.text)
      });
    }
  } catch (e) {
    console.error(e);
    alert('Failed to analyze page.');
    setProgress(0, 'Bias detection — 0%');
  }
});

btnUpload?.addEventListener('click', () => filePicker.click());

filePicker?.addEventListener('change', async (ev) => {
  const file = ev.target.files?.[0];
  if (!file) return;

  sourceValue.textContent = `File: ${file.name}`;
  setProgress(25, 'Uploading file…');
  try {
    const arrayBuf = await file.arrayBuffer();
    const res = await BiasAPI.analyzeFile({
      name: file.name,
      type: file.type || 'application/octet-stream',
      bytes: arrayBuf
    });
    setProgress(100, 'Done');
    const payload = normalizePayload(res, { type: 'file', nameOrTitle: file.name });
    showResults(payload);
  } catch (e) {
    console.error(e);
    alert('Failed to analyze file.');
    setProgress(0, 'Bias detection — 0%');
  } finally {
    filePicker.value = '';
  }
});

btnAnalyzeUrl?.addEventListener('click', async () => {
  const url = urlInput.value.trim();
  if (!url) return alert('Please paste a URL.');
  setProgress(25, 'Fetching URL…');
  try {
    const res = await BiasAPI.analyzeUrl(url);
    setProgress(100, 'Done');
    const title = res?.source?.nameOrTitle || url;
    const payload = normalizePayload(res, { type: 'url', nameOrTitle: title });
    showResults(payload);
  } catch (e) {
    console.error(e);
    alert('Failed to analyze URL.');
    setProgress(0, 'Bias detection — 0%');
  }
});

btnAnalyzeText?.addEventListener('click', async () => {
  await analyzeTextFlow(textInput.value, 'Copied Text');
});
