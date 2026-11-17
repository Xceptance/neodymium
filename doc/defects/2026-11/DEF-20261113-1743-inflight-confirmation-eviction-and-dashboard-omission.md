# [DEF-20261113-1743] In-Flight Confirmation Eviction and Dashboard Initial Load Omission

- **Status:** `Resolved`
- **Opened:** 2026-11-13 17:43
- **Closed:** 2026-11-13 17:47
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** Clicking UI **Confirm** or **Cancel** while an unrelated chat message was in flight on the same session failed with `"Confirmation request was not found or has already been handled."`, and refreshing `/dashboard` omitted active pending confirmation cards.
- **Root Cause:**
  1. `AuraChatService.runChatWorkflow` evicted the entire session entry from `pendingConfirmationsBySession` via `takeValidPriorConfirmations` for the duration of the synchronous LLM turn, leaving `removeByActionId` and `getPendingConfirmations` blind until `runChatWorkflow` returned.
  2. `AuraTestDashboardController.renderDashboard` populated `chatSessions`, `currentSessionId`, and `chatMessages`, but never injected `AuraChatService` or populated `pendingConfirmations`.
- **Detection Gap ("What did we miss?"):** `AuraChatServiceTest` ran `runChatWorkflow` and `executePendingConfirmation` strictly sequentially, and `AuraTestDashboardControllerTest` mocked `AuraChatSessionService` without verifying `pendingConfirmations` on initial load.
- **Resolution:**
  - Replaced bulk session eviction in `AuraChatService` with atomic per-key claiming (`claimValidPriorConfirmation` via `ConcurrentHashMap.computeIfPresent`) inside `ConfirmationBarrierToolCallback`, merging only `issuedInCurrentTurn` (`mergeNewlyIssuedConfirmations`) after a successful turn so UI confirmations stay live and race-safe throughout in-flight LLM calls.
  - Injected `AuraChatService` into `AuraTestDashboardController` (`@Autowired` 5-arg constructor with 4-arg fallback) and populated `pendingConfirmations` in `renderDashboard`.
- **Safety Net Added:**
  - `AuraChatServiceTest.testPendingConfirmationRemainsActionableWhileUnrelatedTurnIsInFlight`
  - `AuraTestDashboardControllerTest.testRenderDashboardPopulatesPendingConfirmationsFromChatService`
