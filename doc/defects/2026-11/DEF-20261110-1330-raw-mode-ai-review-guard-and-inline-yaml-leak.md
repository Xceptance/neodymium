# [DEF-20261110-1330] Editor Raw Mode AI Review Guard, Validate File Raw Enablement, and Inline YAML Artifact Leak

- **Status:** `Resolved`
- **Opened:** 2026-11-10 13:30
- **Closed:** 2026-11-10 13:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. Switching to Raw mode in the playbook editor left `aiReviewBtn` enabled even though raw textareas cannot host interactive suggestion cards, while mistakenly disabling `reviewStepsBtn` ("Validate File") and preventing raw YAML syntax and variable checks.
  2. `editorPanel.getAttribute('data-mode')` permanently remained `"visual"` across editor mode toggles.
  3. Findings originating in the root playbook displayed a confusing source badge labeled `📄 inline.yaml` instead of the user's active file name.
- **Root Cause:** 
  1. `setEditorMode(mode)` failed to synchronize `editorPanel.setAttribute('data-mode', mode)`.
  2. `setEditorMode('raw')` unconditionally disabled `reviewStepsBtn` and lacked raw-mode disabling for `aiReviewBtn`.
  3. `AuraTestEditorController.aiReview()` returned the raw parser artifact `"inline.yaml"` in findings JSON rather than substituting the request's actual `file` parameter.
- **Detection Gap ("What did we miss?"):** 
  - UI toolbar tests tested toolbar actions only in the default Visual editor mode without toggling to Raw mode.
  - Controller tests did not assert that findings targeting the root file use the request's file name rather than `"inline.yaml"`.
- **Resolution:** 
  1. Updated `setEditorMode(mode)` in `dashboard-editor.js` to synchronize `editorPanel.setAttribute('data-mode', mode)`.
  2. In Raw mode, disabled `aiReviewBtn` with an advisory tooltip directing users to Visual mode, and ensured `reviewStepsBtn` remains enabled for raw YAML validation.
  3. In `AuraTestEditorController.aiReview()`, mapped `"inline.yaml"` to `file`.
  4. In `showRawModeFindingsDialog`, suppressed redundant root file badges.
  5. Removed uncalled legacy function `applySuggestionHint`.
- **Safety Net Added:** 
  - `AuraManagerEditorAiReviewToolbarTest.testRawModeDisablesAiReviewAndEnablesValidateFile`
  - `AuraTestEditorControllerTest.testAiReviewReplacesInlineYamlSourceFileWithRequestFileName`
