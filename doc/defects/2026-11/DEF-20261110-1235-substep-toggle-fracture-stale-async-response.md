# [DEF-20261110-1235] Substep Toggle Group Fracture and Stale Async Response on File Switch

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:35
- **Closed:** 2026-11-10 12:45
- **Component:** `aura-manager` (dashboard-editor.js, AuraManagerEditorAiReviewToolbarTest)
- **Scope:** `Tooling & UI Editor`
- **Symptom:**
  1. When a sub-step in the visual editor had an active `.review-suggestion-card`, reindexing steps or toggling lines split the contiguous sub-steps group into multiple fractured sub-step blocks with duplicate header bars.
  2. Clicking the sub-steps toggle bar header to collapse rows halted at the `.review-suggestion-card`, leaving the suggestion card and all remaining sub-steps visibly stranded and un-collapsed.
  3. When clicking "Apply Rewrite", the instruction was replaced, but `#editorUnsavedBadge` ("Unsaved") was not made visible.
  4. If a user switched files while an AI review request was in flight, the response rendered suggestion cards from the previous file into the newly active file.
- **Root Cause:**
  1. `refreshSubstepToggleBars()` iterated through children using `children[i].classList.contains('step-row-substep')`, treating the non-substep suggestion card as a group terminator.
  2. `toggleSubsteps()` looped siblings while matching only `next.classList.contains('step-row-substep')`.
  3. `applySuggestionRewrite()` omitted `checkEditorDirtyStatus()`.
  4. `runAiReview()` did not verify whether the active file had changed before rendering results.
- **Detection Gap ("What did we miss?"):**
  Previous tests validated suggestion cards on standard single-line steps, but omitted collapsible sub-step blocks and in-flight file navigation.
- **Resolution:**
  1. Updated `refreshSubstepToggleBars()` to continue scanning across `.review-suggestion-card` siblings without incrementing the sub-step count.
  2. Updated `toggleSubsteps()` to toggle visibility for both `.step-row-substep` and `.review-suggestion-card` siblings.
  3. Invoked `checkEditorDirtyStatus()` in `applySuggestionRewrite()`.
  4. Guarded `runAiReview()` to discard incoming findings if the active file has changed.
- **Safety Net Added:**
  - `AuraManagerEditorAiReviewToolbarTest.testSubstepCollapseWithSuggestionCardsRetainsSingleGroupAndCollapsesContiguously`: Verifies that a suggestion card on a sub-step does not split the sub-step toggle header and that collapsing hides both sub-steps and suggestion cards.
  - `AuraManagerEditorAiReviewToolbarTest.testApplySuggestionRewriteTriggersUnsavedChangesBadge`: Verifies that applying a suggested rewrite triggers `#editorUnsavedBadge`.
