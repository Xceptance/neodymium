# [DEF-20261116-1025] UI-Only Confirmation Enforcement, Refresh-Runs Route/Swap, Outer Underscore Scan, and Controller Session Hygiene

- **Status:** Resolved
- **Opened:** 2026-11-16 10:25
- **Closed:** 2026-11-16 10:30
- **Component:** `aura-manager`
- **Scope:** Framework & AI/Prompt

## Symptom

1. Chat-text confirmation (`"yes"` reply injection) allowed destructive tools (`delete_test`, `cancel_run`) to proceed via natural-language turns and complicated turn-level confirmation state management alongside the UI Confirm/Cancel buttons.
2. `ConfirmationBarrierToolCallback.extractJsonField` altered/trimmed extracted field values such that padded target strings could mismatch tokens issued by `TwoStepConfirmationManager.issueToken`.
3. Deleting a chat session in `AuraTestChatController.deleteSession` left pending confirmations in memory; `getMessages` did not trim `sessionId` before lookup and comparison; and `confirmPendingAction` marked executed-tool outcomes with an error flag when `chatResp.thinking` recorded an executed trace (`ALREADY_COMPLETED`, `NOT_FOUND`, etc.), hiding follow-up context from LLM history.
4. In `dashboard-core.js`, the `aiAction` `'refresh_runs'` handler called `htmx.ajax('GET', '/', { target: '#queueListContainer', select: '#queueListContainer', swap: 'innerHTML' })`, targeting `/` instead of `/playbooks` and swapping `innerHTML` instead of `outerHTML` without checking whether `#queueListContainer` existed in the DOM.
5. In `dashboard-chat.js`, inline markdown scanning failed on outer emphasis/strong spans wrapped around dot-suffixed underscore tokens (such as `_check _tmp_.log now_` and `__see __init__.py here__`) because dot-suffixed underscore tokens could act as openers and `findClosingDoubleDelimiter` / `findClosingUnderscore` returned `-1` at invalid internal delimiters instead of advancing past them.

## Root Cause

1. Dual confirmation paths (`priorConfirmationClaimer` / `injectConfirmationToken` in `AuraChatService` plus UI `executePendingConfirmation`) introduced fragile token claiming and prompt complexity, while `extractJsonField` normalized whitespace unevenly compared to `TwoStepConfirmationManager.issueToken`.
2. `AuraTestChatController` lacked explicit `clearPendingConfirmations(id)` cleanup on `deleteSession`, missed `sessionId` trimming in `getMessages`, and propagated `isError` unconditionally in `confirmPendingAction` even when `chatResp.thinking` showed a tool trace had already executed.
3. `dashboard-core.js` referenced the wrong route (`'/'`) and swap mode (`'innerHTML'`) for `#queueListContainer` updates.
4. `dashboard-chat.js` lacked an opener guard (`isDotSuffixedUnderscoreToken`) for dot-suffixed underscore tokens and aborted closing delimiter scans (`return -1`) rather than continuing past whitespace-preceded, slash-adjacent, or dot-boundary internal delimiters.

## Detection Gap ("What did we miss?")

- Initial tests asserted `"yes"` chat-text auto-injection rather than strict UI-button-only execution and did not test padded target round-trips, session deletion cleanup of pending confirmations, trimmed `sessionId` lookups in `getMessages`, or executed-tool trace error preservation in `confirmPendingAction`.
- Frontend JS unit tests in `DashboardChatJsTest` covered standalone dot-suffixed tokens and `aiAction` partially, missing wrapped outer emphasis/strong spans containing dot-suffixed tokens (`_check _tmp_.log now_`, `__see __init__.py here__`) and the exact `/playbooks` `outerHTML` parameters of `refresh_runs`.

## Resolution

1. **UI-Only Confirmation & Barrier Hardening ([`AuraChatService.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/chat/service/AuraChatService.java)):**
   - Removed `claimValidPriorConfirmation`, `priorConfirmationClaimer`, `isIssuedThisTurn`, `injectConfirmationToken`, and the unused `java.util.function.Function` import.
   - Simplified `wrapWithConfirmationBarrier` and `ConfirmationBarrierToolCallback.executeWithBarrier` to always strip any LLM-supplied `confirmationToken` via `stripConfirmationToken(toolInput)`, block same-turn retries via `issuedInCurrentTurn`, and never inject tokens during `runChatWorkflow` (leaving `executePendingConfirmation` as the sole confirmation execution path).
   - Updated `SYSTEM_PROMPT`, `blockSameTurnConfirmation`, and `interceptConfirmationIssuance` instructions to state that destructive tools (`delete_test`, `cancel_run`) can only be confirmed via the UI Confirm / Cancel buttons.
   - Updated `ConfirmationBarrierToolCallback.extractJsonField` to preserve exact raw non-blank `field.asText()` strings (`rawVal.trim().isEmpty()` check).
   - Added `clearPendingConfirmations(final String sessionId)` to evict session confirmations on session deletion.
2. **Controller Session & Error Hygiene ([`AuraTestChatController.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/test/controller/AuraTestChatController.java)):**
   - Trimmed `sessionId` in `getMessages` before calling `this.chatSessionService.getSession(trimmedSessionId)` and comparing `trimmedSessionId.equals(session.id)`.
   - Called `this.chatService.clearPendingConfirmations(id)` in `deleteSession`.
   - Updated `confirmPendingAction` so the `error` flag is `isError && !hasExecutedToolTrace` (where `hasExecutedToolTrace = chatResp.thinking != null && !chatResp.thinking.isBlank()`).
3. **Refresh-Runs Target & Swap Fix ([`dashboard-core.js`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/resources/static/dashboard-core.js)):**
   - Guarded `'refresh_runs'` on `document.getElementById('queueListContainer')` and updated the call to `window.htmx.ajax('GET', '/playbooks', { target: '#queueListContainer', select: '#queueListContainer', swap: 'outerHTML' })`.
4. **Markdown Delimiter Scanning ([`dashboard-chat.js`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/resources/static/dashboard-chat.js)):**
   - Added `isDotSuffixedUnderscoreToken(text, startIndex, delimiter)` and applied it to `__` and `_` openers in `appendInlineMarkdown`.
   - Updated `findClosingDoubleDelimiter` and `findClosingUnderscore` to skip past invalid/whitespace-preceded/`/`/`\`/`isStarBoundaryBlockingChar`/`isUnderscoreDotBoundary` delimiters (`idx += 2; continue;` / `idx++; continue;`) instead of returning `-1`.

## Safety Net Added

- [`AuraChatServiceTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/service/AuraChatServiceTest.java):
  - Updated `testConfirmationBarrierBlocksAutoExecutionAndRequiresUiButtonOnly`, `testPartialButtonConfirmationPreservesSiblingPendingConfirmation`, `testThinkingTraceLogsPostBarrierPrettyJsonAndRedactsInjectedToken`, and `testHallucinatedTokenInNextTurnIsStrippedAndNeverExecutesWithoutButtonClick`.
  - Added `testPaddedTargetPreservedForUiConfirmAndClearPendingConfirmationsEvictsSession`.
- [`AuraTestChatControllerTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/controller/AuraTestChatControllerTest.java):
  - Added `testGetMessagesTrimsSessionIdDeleteEvictsPendingAndConfirmPreservesExecutedToolHistory`.
- [`DashboardChatJsTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/DashboardChatJsTest.java):
  - Added wrapped underscore span assertions (`_check _tmp_.log now_`, `__see __init__.py here__`) to `testMarkdownRendererAndSubmitGuardsViaNode`.
  - Added `aiAction` `'refresh_runs'` assertions (`/playbooks` and `outerHTML`) to `testChatInputPreservationConfirmationRecoveryAndErrorScopingViaNode`.
