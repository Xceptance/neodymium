# [DEF-20261110-1150] Visual Editor Step Insertion and Include Tree Suggestion Card Stranding

- **Status:** `Resolved`
- **Opened:** 2026-11-10 11:50
- **Closed:** 2026-11-10 11:58
- **Component:** `aura-manager` (dashboard-editor.js, AuraManagerEditorAiReviewToolbarTest)
- **Scope:** `Tooling & UI Editor`
- **Symptom:**
  1. In the visual editor, pressing `Enter` on a step with an active `.review-suggestion-card` inserted the new empty step directly between the originating step and its suggestion card, displacing the card beneath the newly created step.
  2. When an `include:` step with an expanded `includeTreeCard` received an AI review finding, `renderStepSuggestionCards` inserted the card between the step row and its tree card. Subsequent reordering via `moveStep()` left the suggestion card orphaned at the original position because `moveStep` assumed cards were located after the tree card.
  3. When linting composite milestone steps with nested sub-steps (`PlaybookStep.getFullInstruction()`), multi-line YAML formatting failed exact string matching against the visual editor parent row header, causing suggestion cards to fall back to unanchored dialogs.
- **Root Cause:**
  1. `insertStepAfter()` in `dashboard-editor.js` anchored insertion to `targetRow` (or its `includeTreeCard`) without advancing past adjacent `.review-suggestion-card` siblings.
  2. `renderStepSuggestionCards()` did not check for `targetRow`'s `includeTreeCard` sibling when determining the insertion anchor, violating the `[step-row] -> [includeTreeCard] -> [review-suggestion-card]` sequence.
  3. `moveStep()` collected cards only from `startEl.nextElementSibling` where `startEl = treeCard`, missing cards located between `row` and `treeCard`.
  4. Exact string matching in `renderStepSuggestionCards()` lacked fallback prefix matching for multi-line composite parent steps.
- **Detection Gap ("What did we miss?"):**
  Previous UI tests tested atomic step movement for standard steps, but did not exercise `insertStepAfter` (pressing Enter) while suggestion cards were visible or moving steps with expanded include tree cards.
- **Resolution:**
  1. Updated `insertStepAfter()` in `dashboard-editor.js` to advance `insertAfterRef` past all `.review-suggestion-card` siblings before inserting `newRow`.
  2. Updated `renderStepSuggestionCards()` to position cards after `treeCard` when present, and added multi-line composite header matching.
  3. Updated `moveStep()` to collect all suggestion cards belonging to `row` whether positioned before or after `treeCard`.
- **Safety Net Added:**
  - `AuraManagerEditorAiReviewToolbarTest.testInsertStepAfterRetainsSuggestionCardsOnOriginatingStep`: Asserts that inserting a step after a step with a suggestion card keeps the suggestion card immediately attached to the originating step.
  - `AuraManagerEditorAiReviewToolbarTest.testMoveStepWithIncludeTreeCardRetainsSuggestionCard`: Asserts that moving an include step with an expanded include tree card relocates the step, tree card, and suggestion card atomically in proper sequence.
