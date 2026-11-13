# [DEF-20261110-1300] Nested Relative Include Drift, Sticky Stale Linter Registry, and Dead CSS Cleanup

- **Status:** `Resolved`
- **Opened:** 2026-11-10 13:00
- **Closed:** 2026-11-10 13:05
- **Component:** `neodymium-core` & `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. Multi-level nested relative includes in subfolders (e.g. `fragments/checkout/payment.steps` including `billing.steps`) failed to resolve when resolving disk fragments via `readYamlFileContent()` because `resolveInclude()` dropped the parent directory context.
  2. In standalone `PlaybookLinter.lint(path, content)`, if the initial call occurred before provider credentials were configured or when no provider was registered, `this.standaloneRegistry` remained cached in a broken/empty state, permanently blocking subsequent calls even if provider keys were set dynamically at runtime.
  3. Dead CSS rules for `.btn-editor.btn-review` remained in `dashboard-styles.css` despite being superseded by `.btn-editor.btn-ai-review`.
- **Root Cause:** 
  1. `AuraTestEditorController.resolveInclude(parentIdentifier, relativePath)` directly returned raw `relativePath`, ignoring `parentIdentifier` directory structure.
  2. `PlaybookLinter.getOrCreateStandaloneRegistry()` initialized `this.standaloneRegistry` before provider resolution was confirmed and did not reset it to `null` if `provider == null` or on error during standalone linting.
  3. Legacy styling was left behind during initial toolbar button refactoring.
- **Detection Gap ("What did we miss?"):** 
  - Tests for disk include resolution only validated root-level includes (e.g. `shared.steps`), missing nested directory relative path calculations.
  - Unit tests for `PlaybookLinter` always injected or configured a mock provider ahead of time, never testing provider absence followed by subsequent runtime recovery.
- **Resolution:** 
  1. Updated `AuraTestEditorController.resolveInclude` to delegate to `this.classpathManager.resolveInclude(parentIdentifier, relativePath)` ensuring nested directory hierarchy is preserved across disk reads.
  2. In `PlaybookLinter`, reset `this.standaloneRegistry = null` when `provider == null` or inside the `catch` block on error.
  3. Removed obsolete `.btn-editor.btn-review` CSS blocks from `dashboard-styles.css`.
- **Safety Net Added:** 
  - `AuraTestEditorControllerTest.testAiReviewResolvesNestedRelativeIncludeFragment`
  - `PlaybookLinterTest.testStandaloneRegistryRebootstrapsWhenProviderInitiallyNull`
