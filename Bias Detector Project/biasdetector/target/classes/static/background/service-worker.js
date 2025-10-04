// Central place for network calls (helps with CORS & URL fetches)

const BACKEND_BASE_DEFAULT = 'http://localhost:8080';

async function getBackendBase() {
  const { backendBase } = await chrome.storage.sync.get('backendBase');
  return backendBase || BACKEND_BASE_DEFAULT;
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type !== 'API_FETCH') return;

  (async () => {
    try {
      const base = await getBackendBase();
      const url = base + message.path;

      let fetchInit = {
        method: message.method || 'POST',
        headers: message.headers || {}
      };

      if (message.bodyType === 'json') {
        fetchInit.headers['Content-Type'] = 'application/json';
        fetchInit.body = JSON.stringify(message.body || {});
      } else if (message.bodyType === 'formData') {
        const fd = new FormData();
        // text fields
        Object.entries(message.fields || {}).forEach(([k, v]) => fd.append(k, v));
        // file (if any)
        if (message.file) {
          const { name, type, bytes } = message.file;
          const blob = new Blob([new Uint8Array(bytes)], { type: type || 'application/octet-stream' });
          fd.append('file', new File([blob], name, { type: blob.type }));
        }
        fetchInit.body = fd;
      } else if (message.method === 'GET') {
        // nothing
      } else if (message.rawBody) {
        fetchInit.body = message.rawBody;
      }

      const res = await fetch(url, fetchInit);
      const data = await res.json().catch(() => ({}));
      sendResponse({ ok: res.ok, status: res.status, data });
    } catch (err) {
      console.error('API_FETCH error:', err);
      sendResponse({ ok: false, status: 0, error: String(err) });
    }
  })();

  // keep the message channel open for async
  return true;
});
