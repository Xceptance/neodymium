# [DEF-20261012-1530] First Substep Line Retains Folded Scalar Trigger Characters

- **Status:** `Resolved`
- **Opened:** 2026-10-12 15:05
- **Closed:** 2026-10-12 15:32
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When promoting a step row into a sub-step by typing folded scalar trigger indicators (`>-` or `- >-`), the `>-` characters remained visibly rendered inside the first sub-step editable step line instead of being stripped, leaving redundant trigger syntax in the step instruction text.
- **Root Cause:** When `checkAndUpdateCommentRow()` matched the `isSubstepPrefix` trigger condition, it added the `step-row-substep` class and rendered the group header, but did not strip the leading `>- ` / `- >- ` prefix from `content.innerText` and `data-raw`. Furthermore, an immediate reversion branch `else if (!isSubstepPrefix && isCurrentlySubstep && row.getAttribute('data-manual-substep') === 'true')` in `checkAndUpdateCommentRow()` caused subsequent bubbling `input` listeners to revert the sub-step styling once the trigger characters were stripped.
- **Detection Gap ("What did we miss?"):** Prior automated tests only verified that typing `>-` added the `.step-row-substep` CSS class and persisted the folded scalar YAML block structure upon save, but did not assert that `content.exactText` was cleansed of the leading indicator prefix inside the editable DOM.
- **Resolution:**
  1. Updated `checkAndUpdateCommentRow()` in `dashboard-editor.js` (in both `aura-manager` and `neodymium-core`) to sanitize `content.innerText` and `data-raw`, stripping leading `>- `, `- >- `, and `  - ` prefixes while preserving caret offset.
  2. Removed the automatic de-escalation `else if` branch in `checkAndUpdateCommentRow()` that incorrectly stripped the sub-step class during subsequent input events once `>-` was no longer in the line text. De-escalation back to a normal step is exclusively handled via `handleKeyDown` when Backspace is pressed at caret offset 0.
  3. Added `.step-row-substep .substep-folded-badge { display: none !important; }` in `dashboard-styles.css` to ensure tokenized badges never render inside sub-step lines once the toggle bar is present.
  4. Updated `AuraManagerEditorUiTest` to assert exact cleansed text in `.step-content` and verify Backspace un-indentation.
- **Safety Net Added:** `AuraManagerEditorUiTest.testTypeFoldedScalarIndicatorStylesRowAsSubstepImmediatelyAndPersistsOnSave` in `aura-manager`.
