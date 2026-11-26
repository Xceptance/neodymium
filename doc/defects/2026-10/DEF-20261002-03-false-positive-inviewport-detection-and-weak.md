# [DEF-20261002-03] False Positive inViewport Detection and Weak Selectors in query_dom Tool

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.live()`, Step #2 ("Validate that 'United States' is shown as the current country context") consumed 11 agent turns and 95,598 tokens. `query_dom` returned matching elements located inside closed modals/overlays (e.g. `visibility: hidden; opacity: 0;`) with `inViewport: true` and generic selectors like `span`. This misled the AI agent into believing the element was actively visible in the current viewport, triggering repetitive inspection loops and locator failures.
- **Root Cause:**
  1. `query_dom` evaluated viewport inclusion purely via layout boundaries (`rect.top < window.innerHeight && rect.bottom > 0 ...`), completely ignoring CSS `display`, `visibility`, `opacity`, and `content-visibility`. Full-screen modal overlays covering `(0, 0, 1200, 800)` satisfied the bounding box checks despite being styled with `visibility: hidden` and `opacity: 0`.
  2. Element visibility was never explicitly evaluated or returned as a boolean property (`visible`) in `query_dom` result payloads.
  3. `selector` generation used naive `tag + idStr + clsStr`, emitting bare tag names (e.g. `span`) when no ID was present and including invalid CSS selector characters (e.g. Tailwind `:` in `hover:bg-gray-100`) without leveraging deterministic `data-ai` automation IDs.
  4. Search results did not prioritize visible elements over hidden elements, and did not inform the agent when all matching elements were hidden inside closed containers.
- **Detection Gap ("What did we miss?"):**
  Existing `query_dom` unit tests (`testBrowserQueryDomAncestorSuppressionAndSorting`) tested ancestor suppression and literal text matching against simple visible DOM trees, but did not test hidden elements (`visibility: hidden`, `display: none`, `opacity: 0`) or verify accurate boolean flags (`visible`, `inViewport`) and selector enrichment.
- **Resolution:**
  1. Implemented comprehensive `isVisible(node)` checking in `query_dom`'s injected JavaScript using modern `node.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true })` with recursive ancestor computed-style fallback checking `display`, `visibility`, and `opacity < 0.01`.
  2. Gated `inViewport` calculation so that `inViewport = vis && inRect`, guaranteeing invisible elements never report `inViewport: true`.
  3. Enriched result objects with explicit `visible: vis` and `dataAi: autoId || null` fields.
  4. Prioritized visible elements over hidden elements in candidate sorting (`visB - visA`).
  5. Enhanced selector construction: uses `tag#id` if valid ID exists, falls back to `tag[data-ai="..."]` if automation ID exists, and sanitizes class names to prevent invalid CSS selectors with colons or slashes.
  6. Added diagnostic `note` when all matching elements are hidden: `"All X matching element(s) are currently hidden (visible=false). Verify if a parent dropdown, menu, or modal needs to be opened first"`.
- **Safety Net Added:**
  Added unit and regression test `testBrowserQueryDomAccurateVisibilityAndDataAiSelector` in `BrowserToolsTest.java` verifying that elements with `visibility: hidden`, `opacity: 0`, and `display: none` return `visible: false` and `inViewport: false`, that `data-ai` selectors are properly constructed, and that the diagnostic hidden note is returned.
