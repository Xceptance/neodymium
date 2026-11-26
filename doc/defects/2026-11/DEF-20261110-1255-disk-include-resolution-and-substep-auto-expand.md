# [DEF-20261110-1255] Disk-Based Include Resolution Discrepancy, Collapsed Substep Auto-Expand, and Stale Dialog Cleanup

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:55
- **Closed:** 2026-11-10 12:56
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. AI Review crashed with `Unresolved include directive in playbook 'inline string'` when referencing `.steps` fragments on disk that had not yet been copied to the classpath by Maven.
  2. Findings for collapsed sub-steps rendered orphaned or hidden cards without expanding the sub-step group.
  3. Clean review runs (0 findings) left stale modal findings dialogs from prior runs on screen.
  4. Programmatic `applySuggestionRewrite(lineNum)` failed to dismiss cards when an `includeTreeCard` preceded them.
- **Root Cause:** 
  1. `AuraTestEditorController` parsed content with `YamlPlaybookParser.parseString(content)` using default `ClasspathResourceManager`, which cannot see uncompiled files on disk under `fileService.getResourcesDirectory()`.
  2. `dashboard-editor.js:renderStepSuggestionCards` did not auto-expand preceding `.substeps-toggle-bar.collapsed` when finding rows inside collapsed groups.
  3. `runAiReview` response handling did not clean up `.var-prompt-card[data-type="ai-review-report"]` upon receiving zero findings.
  4. `applySuggestionRewrite` only inspected `row.nextElementSibling` instead of scanning forward past non-`step-row` siblings.
- **Detection Gap ("What did we miss?"):** Controller tests used mock linter calls and single-string playbooks without multi-file includes residing strictly on disk outside the classpath. UI tests did not verify re-runs with zero findings or suggestion injection while substeps were collapsed.
- **Resolution:** 
  1. Backed `YamlPlaybookParser.parseString(content, resourceManager)` with an adapter delegating to `fileService.readYamlFileContent()` before falling back to `ClasspathResourceManager`.
  2. In `renderStepSuggestionCards`, detected if `targetRow` resides in a collapsed sub-step group and triggered `toggleSubsteps(toggleBar)` to auto-expand it.
  3. Removed `.var-prompt-card[data-type="ai-review-report"]` when new results or zero-finding states arrive in `runAiReview`.
  4. Scanned forward past non-`step-row` siblings in programmatic `applySuggestionRewrite`.
- **Safety Net Added:** 
  - `AuraTestEditorControllerTest.testAiReviewResolvesDiskBasedIncludeFragment`
  - `AuraManagerEditorAiReviewToolbarTest.testSuggestionCardAutoExpandsCollapsedSubstepsAndClearsStaleDialog`
