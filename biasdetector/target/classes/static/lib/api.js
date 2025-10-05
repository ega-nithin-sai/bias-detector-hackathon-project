// Simple wrapper that asks the service worker to make API calls.
// Adjust endpoints to match your Spring Boot backend.

const BiasAPI = {
  async analyzeText(text) {
    const res = await chrome.runtime.sendMessage({
      type: 'API_FETCH',
      path: '/analyze/text',
      method: 'POST',
      bodyType: 'json',
      body: { text }
    });
    if (!res?.ok) throw new Error('API error (text)');
    return res.data;
  },

  async analyzeUrl(url) {
    const res = await chrome.runtime.sendMessage({
      type: 'API_FETCH',
      path: '/analyze/url',
      method: 'POST',
      bodyType: 'json',
      body: { url }
    });
    if (!res?.ok) throw new Error('API error (url)');
    return res.data;
  },

  async analyzeFile(fileObj /* {name, type, bytes:ArrayBuffer} */) {
    const res = await chrome.runtime.sendMessage({
      type: 'API_FETCH',
      path: '/analyze/file',
      method: 'POST',
      bodyType: 'formData',
      fields: {},      // add any extra fields here if needed
      file: {
        name: fileObj.name,
        type: fileObj.type,
        bytes: Array.from(new Uint8Array(fileObj.bytes)) // serialize for messaging
      }
    });
    if (!res?.ok) throw new Error('API error (file)');
    return res.data;
  }
};

// Optional: allow user to set backend URL from DevTools console
// chrome.storage.sync.set({ backendBase: 'http://localhost:8080' });
