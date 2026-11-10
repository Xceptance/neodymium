# [DEF-20261110-1225] AI Review Included Fragment Step Drift and Overwrite Collision

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:25
- **Closed:** 2026-11-10 12:35
- **Component:** `aura-manager` (dashboard-editor.js, AuraManagerEditorAiReviewToolbarTest)
- **Scope:** `Tooling & UI Editor`
- **Symptom:**
  1. When running AI Review on a playbook containing `_include:` directives, findings for steps inside the included fragment fell back to `allRows[f.stepIndex - 1]`, rendering suggestion cards under unrelated caller steps or the `_include:` directive itself.
  2. Clicking "Apply Rewrite" on an included step replaced and permanently destroyed the unrelated caller step or `_include:` directive.
  3. The findings dialog omitted `f.sourceFile`, making unanchored suggestions from included fragments confusing to trace.
  4. In-flight editor mode switching from visual to raw left suggestion cards inserted into the hidden visual DOM.
- **Root Cause:**
  1. `YamlPlaybookParser.parseString()` flattens included steps into `playbook.getAllSteps()`. When content matching failed for fragment steps in the caller DOM, `renderStepSuggestionCards()` fell back to raw index lookups (`allRows[f.stepIndex - 1]`).
  2. `showRawModeFindingsDialog()` did not render `f.sourceFile`.
  3. `runAiReview()` closed over `isRawMode` evaluated before the asynchronous fetch.
- **Detection Gap ("What did we miss?"):**
  Previous UI tests tested AI review cards on directly declared steps within the same file, but omitted scenarios where playbooks contain `_include:` directives whose child steps receive findings.
- **Resolution:**
  1. Eliminated the blind `stepIndex` fallback in `renderStepSuggestionCards()`; unanchored or external-file findings route directly to `unanchoredFindings` to protect caller steps from accidental overwrite.
  2. Rendered `f.sourceFile` badges in `showRawModeFindingsDialog()` when findings originate from included fragments with an explicit note to apply changes in the fragment file.
  3. Dynamically evaluated the active editor mode upon promise resolution in `runAiReview()`.
- **Safety Net Added:**
  - `AuraManagerEditorAiReviewToolbarTest.testAiReviewFindingsFromIncludedFragmentRouteToUnanchoredPromptDialogWithoutCorruptingCallerSteps`: End-to-end Selenide UI test verifying that included fragment findings route cleanly to the unanchored dialog displaying their `sourceFile` without attaching to or corrupting caller steps.
