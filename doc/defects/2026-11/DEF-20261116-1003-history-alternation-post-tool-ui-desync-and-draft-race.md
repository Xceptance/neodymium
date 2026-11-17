# [DEF-20261116-1003] History User-Turn Alternation, Post-Tool UI Desync, Confirmation Sibling Consistency, and Draft Wipe Race

- **Status:** `Resolved`
- **Opened:** 2026-11-16 10:03
- **Closed:** 2026-11-16 10:06
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  1. Failed AI turns (`error = true`) omitted from replay left consecutive `UserMessage` entries or a trailing `UserMessage` directly preceding the next prompt.
  2. Standalone dot-suffixed underscore filenames such as `__init__.py` and `_tmp_.log` rendered as `<strong>`/`<em>`.
  3. Overlapping `/api/chat` requests cleared `pendingSubmittedPrompt` on the first completion, causing subsequent completions to wipe a newly typed draft.
  4. Early error paths (`invalid actionId`, not-found, expired, unavailable tool) and `"ALREADY_COMPLETED"` status in `executePendingConfirmation` dropped sibling `pendingConfirmations` or reported `"Confirmed..."`.
  5. When a tool executed (`create_test`, `delete_test`, `run_test`, `run_suite`, `cancel_run`) and the LLM summary call then threw, `uiActions` was discarded and `HX-Trigger` never refreshed the UI.
- **Root Cause:**
  1. `AuraChatService.buildHistoryMessages` coalesced consecutive `AssistantMessage` turns only; `runChatWorkflow` appended `.user(userPrompt)` directly onto any trailing `UserMessage`.
  2. `dashboard-chat.js` blocked `/`, `\`, and word chars around `_`/`__`, but did not stop at `.[wordChar]` extensions.
  3. `htmx:afterRequest` cleared `#chatInput` whenever `pendingSubmittedPrompt === null` after any response reset it.
  4. `executePendingConfirmation` duplicated error `ChatResponse` builders without `getPendingConfirmations(sessionKey)` and `isToolErrorStatus` checked only `"ERROR"` / `"NOT_FOUND"`.
  5. `AuraChatService.runChatWorkflow`'s `catch` passed `null` for `actions`, and `AuraTestChatController` gated `HX-Trigger` solely on `!isError`.
- **Detection Gap ("What did we miss?"):** Unit tests previously covered adjacent `AssistantMessage`s only, `/_tmp_/` slash boundaries only, single-flight `/api/chat` requests, `"NOT_FOUND"` status, and exceptions thrown before any tool mutation.
- **Resolution:**
  - Added consecutive `UserMessage` coalescing in `buildHistoryMessages` and `mergeTrailingUserPrompt` in `AuraChatService.runChatWorkflow`.
  - Added `isUnderscoreDotBoundary` handling in `dashboard-chat.js` for `_` and `__`.
  - Updated `htmx:afterRequest` in `dashboard-chat.js` to compare against `detail.requestConfig.parameters.prompt` first, never clear `#chatInput` on unknown/null prompt tracking, and only clear `pendingSubmittedPrompt` when it matches the finishing request.
  - Extracted `errorConfirmationResponse(sessionKey, ...)` in `AuraChatService` and broadened `isToolErrorStatus` to flag any non-empty status other than `"SUCCESS"` / `"CANCELLED"`.
  - Preserved `uiActions` on post-tool `runChatWorkflow` errors and emitted non-`"error"` `HX-Trigger` actions from `AuraTestChatController`.
- **Safety Net Added:**
  - `AuraChatServiceTest.testBuildHistoryMessagesCoalescesAdjacentUsersAndRunWorkflowMergesTrailingUserAfterError`
  - `AuraChatServiceTest.testExecutePendingConfirmationHandlesAlreadyCompletedAndPreservesSiblingsOnMissingOrExpired`
  - `AuraChatServiceTest.testPostToolLlmFailurePreservesEmittedUiActions`
  - `AuraTestChatControllerTest.testHandleChatEmitsHxTriggerWhenErrorFollowsSuccessfulToolAction`
  - Extended assertions in `DashboardChatJsTest.testMarkdownRendererAndSubmitGuardsViaNode` and `DashboardChatJsTest.testChatInputPreservationConfirmationRecoveryAndErrorScopingViaNode`
