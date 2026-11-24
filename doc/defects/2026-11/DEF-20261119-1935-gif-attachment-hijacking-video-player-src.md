# [DEF-20261119-1935] GIF Attachment Hijacking Video Player Source Resulting in 0:00 Duration

- **Status:** `Resolved`
- **Opened:** 2026-11-19 19:35
- **Closed:** 2026-11-19 19:38
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  In test reports where both video filming and GIF recording are enabled (e.g. via `-Dvideo.enableFilming=true -Dgif.enableFilming=true` or Cucumber tags), the video player container in the side panel displayed duration `0:00` and showed "Video preview unavailable in browser", failing to play.
- **Root Cause:**
  In `report-manager.js`, the `videoAttachment` lookup checked:
  ```javascript
  (a.name && (a.name.endsWith('.mp4') || a.name.endsWith('.webm') || a.name.startsWith('video-')))
  ```
  When GIF filming is enabled, Neodymium generates an animated GIF named `video-{index}.gif`. Because `video-{index}.gif` starts with `'video-'` and appeared before `video-{index}.mp4` in the execution's `attachments` array, `find()` matched `video-{index}.gif` instead of `video-{index}.mp4` and assigned it as the `src` attribute of the HTML5 `<video id="sidePanelVideoPlayer">` element. Browsers (Chromium, Firefox, Safari) cannot decode GIF images inside `<video>` elements and aborted the media fetch with `net::ERR_ABORTED` (since the server served `image/gif` with `nosniff`), causing the `<video>` element to freeze with duration `0:00` and trigger `videoPlayer.onerror`.
- **Detection Gap ("What did we miss?"):**
  Unit tests tested server HTTP endpoints and controller responses using isolated mock requests, but did not verify frontend DOM attachment resolution when GIF and MP4 files coexist within the same execution metadata.
- **Resolution:**
  1. In `report-manager.js`, restricted `videoAttachment` strictly to playable video extensions (`.mp4`, `.webm`, `.ogg`) and explicitly excluded `.gif`.
  2. Added dedicated `gifAttachment` detection for animated GIF recordings (`.gif` or `type === 'gif'`).
  3. In `side-panel-step-list.html`, added `<img id="sidePanelGifPlayer">` alongside `<video id="sidePanelVideoPlayer">`.
  4. When an MP4 video is present, the player loads the MP4 video with full controls and duration. When only a GIF is present, it is rendered in `<img id="sidePanelGifPlayer">` so the browser animates it natively without video decoder errors.
- **Safety Net Added:**
  Frontend DOM and attachment resolution tests ensuring that `.gif` files are never assigned to `<video id="sidePanelVideoPlayer">`, that MP4 video attachments always take precedence for HTML5 video playback, and that GIF recordings render in the dedicated image element.
