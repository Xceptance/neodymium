# [DEF-20261116-1342] Session ID Trimming, Sequence Marker Pruning / Expired-Click Race, and Inline Markdown Delimiters

- **Status:** Resolved
- **Opened:** 2026-11-16 13:38
- **Closed:** 2026-11-16 13:42
- **Component:** `aura-manager`
- **Scope:** Framework

## Symptom

1. `AuraTestChatController.deleteSession` and `AuraTestChatController.renameSession` rejected whitespace-padded `id` / `sessionId` parameters with HTTP `400` (`IllegalArgumentException` from `AuraChatSessionService.SAFE_SESSION_ID_PATTERN`) even though `getMessages`, `handleChat`, `confirmPendingAction`, and `cancelPendingAction` trimmed `sessionId`.
2. In `AuraChatService`, `sessionClearedSeqBySession` and `resolvedSeqBySession` retained resolved/cleared sequence entries indefinitely without pruning; clicking an already-expired confirmation card recorded a resolution sequence in `removeByActionId` before expiry validation, blocking concurrent in-flight turns for the same target with `CONFIRMATION_ALREADY_HANDLED`; and `cancelPendingConfirmation` returned `action = null` (`error = false`) for missing/expired actions, persisting stale notices into LLM history.
3. In `dashboard-chat.js`, star+underscore globs (`tests/*_helper.yaml and tests/_a_*.yaml`) rendered stray `<em>` tags, `___hello___` failed to parse (`___hello___`), `**foo ** bar**` closed early on whitespace-preceded `**`, and nested same-delimiter spans (`_check _inner_ now_`, `**outer **inner** end**`) closed on the first inner delimiter.

## Root Cause

1. `deleteSession` and `renameSession` selected `idParam` vs `sessionIdParam` without trimming.
2. `AuraChatService` lacked an active-turn sequence watermark to safely evict obsolete markers when idle/after concurrent turns drained; `removeByActionId` did not check `expiresAt().isAfter(now)` before recording into `resolvedSeqBySession`; and `cancelPendingConfirmation` built a normal `ChatResponse` rather than `errorConfirmationResponse` on `removed == null`.
3. `dashboard-chat.js` shared `isStarBoundaryBlockingChar` across `*` and `_` (missing `*` blocking for `_`, while blocking `_` next to `__`), skipped `**` boundary guards in `findClosingDoubleDelimiter`, jumped past triple delimiters via `idx += 2`, and returned on the first valid closer without tracking nested delimiter depth.

## Detection Gap ("What did we miss?")

- Controller tests verified trimmed `sessionId` only on `getMessages`, not `deleteSession` or `renameSession`.
- Service tests used wall-clock `Instant.now()` and never asserted marker map pruning or expired-click behavior during an active turn or `action = "error"` on missing/expired cancel.
- JS tests covered `tests/*.yaml`, `_tmp_.log`, and `_check __init__ now_`, missing `*_` globs, `___...___` / `***...***`, spaced `**`, and nested same-delimiter spans.

## Resolution

1. **[`AuraTestChatController.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/test/controller/AuraTestChatController.java):** Added `resolveTrimmedId(primaryId, fallbackId)` and applied it in `deleteSession` and `renameSession`.
2. **[`AuraChatService.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/chat/service/AuraChatService.java):**
   - Added `Clock` injection (package-private constructor for deterministic expiry tests, `@Autowired` default using `Clock.systemUTC()`).
   - Tracked active `runChatWorkflow` start sequences under `turnWatermarkLock` (`registerInFlightTurn`, `releaseInFlightTurnAndPrune`, `pruneIdleResolutionMarkers`, `pruneObsoleteResolutionMarkers`) so obsolete `sessionClearedSeqBySession` and `resolvedSeqBySession` entries are evicted once turns below their watermark finish (and immediately when idle).
   - Updated `removeByActionId` to record `resolvedSeqBySession` only when `removedHolder[0].expiresAt().isAfter(now)`.
   - Updated `cancelPendingConfirmation` to return `errorConfirmationResponse(...)` when `removed == null`.
3. **[`dashboard-chat.js`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/resources/static/dashboard-chat.js):**
   - Added `isUnderscoreBoundaryBlockingChar` (`/[a-zA-Z0-9_/\\*]/`) and applied it to `__` and `_` openers/closers so `*_` and `_*` globs stay plain text.
   - Updated `findClosingDoubleDelimiter`, `findClosingSingleAsterisk`, and `findClosingUnderscore` to shift across runs of 3+ matching delimiters (`idx++` when `text[idx + 2] === delimChar`), enforce whitespace/slash boundary guards on `**` as well as `__`, and balance nested delimiters with `firstFallbackClose` fallback.

## Safety Net Added

- [`AuraTestChatControllerTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/controller/AuraTestChatControllerTest.java): Added `testDeleteAndRenameSessionTrimPaddedIds`.
- [`AuraChatServiceTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/service/AuraChatServiceTest.java): Added `testResolutionMarkersPrunedWhenIdleAndExpiredClickDoesNotBlockInFlightTurn`.
- [`DashboardChatJsTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/DashboardChatJsTest.java): Extended `testMarkdownRendererAndSubmitGuardsViaNode` with `tests/*_helper.yaml and tests/_a_*.yaml`, `___hello___` / `***world***`, `**foo ** bar**`, and `_check _inner_ now_` / `**outer **inner** end**`.
