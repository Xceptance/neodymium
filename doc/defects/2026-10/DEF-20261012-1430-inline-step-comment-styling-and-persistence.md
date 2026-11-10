# [DEF-20261012-1430] Inline Step Comment Styling And Save Persistence

- **Status:** `Resolved`
- **Opened:** 2026-10-12 14:24
- **Closed:** 2026-10-12 14:33
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When a user wrote or typed an inline comment inside a step line (e.g. `Click the "Add to Cart" button. # verify cart opens`), the comment portion was not styled as comment text. Furthermore, if `#` was followed by non-whitespace characters, it was erroneously rendered as a selector `.hint-badge`.
- **Root Cause:**
  1. `formatStepToTokens()` in `dashboard-editor.js` only checked for full-line comments starting with `#`. It lacked scanner logic to identify inline comment boundaries outside of quotes.
  2. The input handler `checkAndUpdateCommentRow()` only applied comment styles when the whole line began with `#`. Typing an inline `#` inside a step line left the comment text unstyled until blur/reload.
  3. `serializeStepRows()` in `compilePlaybookToYaml()` did not normalize browser contenteditable newline artifacts (`\r?\n+`), causing browser-inserted linebreaks to decouple inline comments from their parent step into separate, unindented lines upon serialization.
- **Detection Gap ("What did we miss?"):** Previous test cases only verified comment rows where the `#` was located at index 0 of the step line. Inline comment presence and real-time typing inside an existing step row were never tested in the UI test suite.
- **Resolution:**
  1. Implemented `findInlineCommentIndex(text)` in `dashboard-editor.js` (both `aura-manager` and `neodymium-core`) to scan strings for `#` while strictly respecting single (`'...'`) and double (`"..."`) quotes, and requiring preceding whitespace or quote boundaries to prevent false positives on URL anchors (e.g. `https://example.com/#anchor`).
  2. Updated `formatStepToTokens()` to split steps into instruction and inline comment portions, wrapping comments in `<span class="step-inline-comment">`.
  3. Added `.step-inline-comment` styles in `dashboard-styles.css` with `font-style: italic`, slate color `var(--text-secondary, #64748b)`, and transparent background.
  4. Updated `checkAndUpdateCommentRow()` to detect inline `#` typing in real time, dynamically wrapping the comment span and preserving caret position via `setCaretOffset`.
  5. Normalized browser newline artifacts in `serializeStepRows()` and `checkAndUpdateCommentRow()` to ensure clean single-line step serialization with preserved inline comments.
  6. Bumped asset query strings to `?v=1.2.2` across templates for cache invalidation.
- **Safety Net Added:** Added `@Test` `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeHashInsideStepLineStylesInlineCommentImmediatelyAndPersistsOnSave`, which verifies:
  - Immediate visual styling of the inline comment (`.step-inline-comment` with italic font) when typing ` # verify cart opens`.
  - Retention of normal step status and step numbering (step 1 and step 2 remain intact; row does not convert to `.step-row-comment`).
  - Correct YAML serialization to disk (`Click the "Add to Cart" button. # verify cart opens`).
  - Full persistence of inline comment styling across editor reload.
  - Reversion to normal step text when the inline comment is removed.
