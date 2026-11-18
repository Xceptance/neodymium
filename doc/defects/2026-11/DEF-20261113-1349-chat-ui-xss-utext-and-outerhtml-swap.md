# [DEF-20261113-1349] Chat Panel Stored XSS via th:utext, HTMX outerHTML Container Destruction, and Stale UI Sync

- **Status:** `Resolved`
- **Opened:** 2026-11-13 13:15
- **Closed:** 2026-11-13 13:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Assistant responses in `fragments/chat.html` were rendered with raw `th:utext="${msg.content}"` (bypassing HTML escaping), enabling stored/reflected XSS if an LLM or tool output contained malicious HTML or script tags.
  2. Deleting or renaming a chat session via `dashboard-chat.js` used `swap: 'outerHTML'` targeting `#chatContainer`, destroying the outer `#chatContainer` element wrapper and breaking subsequent HTMX chat operations; meanwhile, `fragments/chat.html` bound static `th:hx-post` URLs that stayed pinned to the initial `currentSessionId` after switching sessions.
  3. Sending messages omitted the currently open YAML file context (`activeFile`), auto-renamed sessions left `#chatSessionSelect` showing the old title until full page reload, and `refresh_runs` triggered by AI actions updated `#queueListContainer` without invoking `pollStatus()`.

- **Root Cause:**
  1. `chat.html` relied on `th:utext` instead of escaping text server-side and rendering markdown safely on the client.
  2. `dashboard-chat.js` passed `swap: 'outerHTML'` for fragments that return `chatContainerContent` (`<th:block>` inner contents, not the `#chatContainer` wrapper), while `chat.html` embedded `${currentSessionId}` directly in button attributes rather than reading the live `#chatSessionSelect` value.
  3. Neither `chat.html` nor `dashboard-chat.js` synced `window.activeEditingFile` into the form, nor did `chat.html` include an OOB `<select id="chatSessionSelect" hx-swap-oob="outerHTML">` update when `oobSessionSelect` is set.

- **Detection Gap ("What did we miss?"):**
  - Dashboard chat template changes were refactored across modules without an end-to-end or MockMvc regression assertion checking for `th:utext` removal, `innerHTML` swap preservation, `hiddenActiveFile` presence, or `oobSessionSelect` emission on auto-rename.

- **Resolution:**
  1. Switched `fragments/chat.html` message rendering to escaped `th:text="${msg.content}"` with class `chat-markdown` for AI messages, added safe DOM-node client-side markdown rendering (`buildMarkdownFragment` / `renderChatMarkdown` in `dashboard-chat.js`, restricting links to `https://`, `http://`, or `/`), and styled markdown elements in `dashboard-styles.css`.
  2. Replaced static `th:hx-post` rename/delete attributes in `chat.html` with JS helpers (`renameCurrentChatSession()`, `deleteCurrentChatSession()`) reading `#chatSessionSelect` and swapping `#chatContainer` with `swap: 'innerHTML'`.
  3. Added `<input type="hidden" name="activeFile" id="hiddenActiveFile">` synced via `syncHiddenActiveFile()` on send, added `hx-swap-oob="outerHTML"` `#chatSessionSelect` updates when auto-renamed, updated welcome prompt chips to match active MCP tools, and added `pollStatus()` invocation under `aiAction` `refresh_runs` in `dashboard-core.js`.

- **Safety Net Added:**
  - `AuraTestChatControllerTest.testChatTemplateEscapesContentAndChatJsUsesInnerHtmlSwap`
  - `AuraTestChatControllerTest.testHandleChatEmitsHxTriggerAndOobSelectWhenAutoRenamed`
  - `StaticResourceSyntaxTest` (validates JS/CSS syntax across all 75 static resources including `dashboard-chat.js`, `dashboard-core.js`, and `dashboard-styles.css`).
