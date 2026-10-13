# [DEF-20261012-1412] Comment Styling Removed On Save And Asset Cache Invalidation

- **Status:** `Resolved`
- **Opened:** 2026-10-12 14:12
- **Closed:** 2026-10-12 14:20
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When a user saved a playbook containing comments (e.g. `# ...`) and reloaded the page, or edited the test step, comment styling was stripped and the step reverted to a numbered test step. Additionally, updated CSS and JS files were not immediately reflected in running browser instances due to cache headers.
- **Root Cause:** In `editor.html` (`before:`, `steps:`, and `after:` sections), comment steps were rendered with `th:text="${step.startsWith('# ') ? step.substring(2) : ...}"` and `th:data-raw="${step.startsWith('# ') ? step.substring(2) : ...}"`. This stripped the leading `# ` prefix from the DOM text upon server rendering. When the editor initialized or reloaded, `checkAndUpdateCommentRow()` inspected `content.innerText`. Because the `# ` had been stripped from `content.innerText`, `!text.trimStart().startsWith('#') && wasComment` evaluated to `true`, causing the editor to dynamically remove the `.step-row-comment` CSS class and renumber the step as an executable step. Furthermore, script and style link tags were using `?v=1.2.0`, allowing browser caches to serve outdated versions across rebuilds.
- **Detection Gap ("What did we miss?"):** Previous visual editor tests only verified initial YAML parsing into comment elements and client-side input dispatch without executing a full round-trip save-to-disk followed by a clean page reload and DOM re-inspection.
- **Resolution:**
  1. Updated `aura-manager/src/main/resources/templates/fragments/editor.html` and `neodymium-core/src/main/resources/com/xceptance/neodymium/aura/templates/fragments/editor.html` to preserve the full comment string `step` directly in `th:data-raw="${step}"` and `th:text="${step}"`.
  2. Synchronized `dashboard-styles.css` across both modules to ensure `.step-row-comment .step-content` maintains `font-style: italic`, transparent background, and `#64748b` slate coloring on focus.
  3. Bumped asset query strings to `?v=1.2.1` across `index.html`, `dashboard.html`, and `neodymium-aura-dashboard.html` to invalidate cached assets.
- **Safety Net Added:** Extended `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeHashAtBeginningOfLineStylesRowAsCommentImmediately` to perform a full round-trip test: typing `#`, verifying immediate styling, saving to disk via `#saveYamlBtn`, re-reading disk content, reloading the editor page, verifying retained comment classes, italic font style, `#` badge, and unchanged subsequent step numbers, followed by deleting `#`, reverting to normal step, and saving again.
