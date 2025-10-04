# Bias Detector (Chrome Extension - MV3)

## Run (Developer Mode)
1. `chrome://extensions` → enable **Developer mode**.
2. Click **Load unpacked** → select this folder.
3. Pin the extension and click the toolbar icon.

## Backend
- Default backend base: `http://localhost:8080`
- To change at runtime: open DevTools Console on the popup and run:
  ```js
  chrome.storage.sync.set({ backendBase: 'http://YOUR_HOST:PORT' })
