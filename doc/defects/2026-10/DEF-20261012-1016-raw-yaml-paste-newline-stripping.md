# [DEF-20261012-1016] Raw YAML Editor Strips Newlines on Clipboard Paste

- **Status:** `Resolved`
- **Opened:** 2026-10-12 10:16
- **Closed:** 2026-10-12 10:36
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Pasting multi-line YAML test data into the "Raw YAML" textarea of the test editor removes all newlines, flattening the entire document into a single line separated by spaces.
- **Root Cause:** A global paste listener in `dashboard-editor.js` intended for single-line visual editor inputs matched all `TEXTAREA` elements and lacked an `isRaw` editor mode guard. In addition, `index.html` hardcoded asset version `?v=1.0.7` without disabling static resource caching in development, causing browsers to serve cached JavaScript containing the legacy paste flattening logic.
- **Detection Gap ("What did we miss?"):** Existing UI tests in `AuraManagerEditorUiTest` covered visual step editing, fragment inclusion, and test creation/deletion, but did not simulate paste events inside the `#rawYamlTextarea` element in Raw YAML mode nor verify client-side cache busting on modified static assets.
- **Resolution:** Excluded `#rawYamlTextarea` and `#rawEditorPanel` from visual editor paste interception, added top-level `isRaw` mode check, removed `TEXTAREA` from single-line sanitization, bumped script version in `index.html` to `?v=1.0.8`, disabled static caching in `application.properties`, synchronized fixes to `neodymium-core` (`dashboard-editor.js` and `playbook-editor-mockup.html`), and built/installed fresh artifacts to `~/.m2`.
- **Safety Net Added:** `AuraManagerEditorUiTest.testPasteIntoRawYamlEditorPreservesFormatting`
