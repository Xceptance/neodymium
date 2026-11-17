# [DEF-20261113-1348] Chat Session Path Traversal Vulnerability, Silent Fallback Mutation, and Custom-Name Overwrite

- **Status:** `Resolved`
- **Opened:** 2026-11-13 13:15
- **Closed:** 2026-11-13 13:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Supplying crafted `sessionId` values containing `../` or path separators to `AuraChatSessionService` (`getSession`, `renameSession`, `addMessage`, `deleteSession`) constructed arbitrary `File` paths outside `chat-history/` and could throw unhandled HTTP 500 errors or read/delete unintended files.
  2. Supplying an unknown `sessionId` caused `getSession` / `renameSession` / `addMessage` to silently fall back to session index `0` ("Default Session"), polluting or overwriting the default session with another caller's messages or title.
  3. `AuraChatSessionService` exposed live mutable `List<ChatMessageDto>` references from cached DTOs across concurrent threads, could fail to honor test/configurable history directories, and `AuraTestChatController` overwrote custom-named empty sessions whenever `session.messages.size() <= 1`, while computing history inconsistently and leaking live list mutations to the template model.

- **Root Cause:**
  1. `AuraChatSessionService.persistSession` and `deleteSession` concatenated `id + ".json"` directly onto `chatHistoryDir` without allowlist regex validation or normalized `Path.startsWith` containment checks.
  2. Lookup helpers silently returned `sessions.get(0)` when an ID was absent, and `AuraTestChatController` checked `session.messages.size() <= 1` unconditionally during auto-rename and lacked an `@ExceptionHandler(IllegalArgumentException.class)`.

- **Detection Gap ("What did we miss?"):**
  - `AuraChatSessionServiceTest` and `AuraTestChatControllerTest` previously tested only valid UUID session IDs and mocked service responses, leaving malformed IDs, missing session fallbacks, concurrent DTO list mutation, and custom-named empty sessions completely untested.

- **Resolution:**
  1. Enforced `^[a-zA-Z0-9_-]{1,128}$` validation and normalized parent-directory containment (`resolveSafeSessionFile`) across all session reads, writes, deletes, and disk reloads in `AuraChatSessionService`.
  2. Removed silent fallback to session `0` on unknown IDs: added `Optional<ChatSessionDto> findSession(String)`, logged warnings and returned cleanly on missing IDs in `addMessage` / `renameSession`, and returned defensive immutable snapshots (`List.copyOf`).
  3. Added `@Autowired` `@Value("${aura.chat.history-dir:#{null}}")` constructor to `AuraChatSessionService`.
  4. Updated `AuraTestChatController` to pass `activeFile` and `sessionId` in `ChatRequest`, snapshot `historyBeforeTurn` before appending the new user prompt, restrict auto-rename exclusively to placeholder names (`"New Chat"` / `"Default Session"`), emit `oobSessionSelect` for header dropdown sync when auto-renamed, and map `IllegalArgumentException` to HTTP 400 Bad Request.

- **Safety Net Added:**
  - `AuraChatSessionServiceTest.testRejectsPathTraversalAndUnknownIdsWithoutMutatingDefaultSession`
  - `AuraTestChatControllerTest.testCustomNamedEmptySessionIsNotAutoRenamedOnFirstPrompt`
  - `AuraTestChatControllerTest.testInvalidSessionIdReturnsBadRequest`
