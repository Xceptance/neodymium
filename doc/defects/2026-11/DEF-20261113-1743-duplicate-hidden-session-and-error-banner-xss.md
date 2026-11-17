# [DEF-20261113-1743] Duplicate `#hiddenSessionId`, Protocol-Relative URL Bypass, and Warning Banner HTML Injection

- **Status:** `Resolved`
- **Opened:** 2026-11-13 17:43
- **Closed:** 2026-11-13 17:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Selecting another chat session via `#chatSessionSelect` after initial page load or `/api/chat/create`, `/rename`, or `/delete` updated only the orphaned `#hiddenSessionId` inside `#chatMessages`, leaving `<form>` submitting prompts to the previous session.
  2. Protocol-relative Markdown URLs such as `//evil.com/phish` and `/\evil.com/phish` rendered as active links, and plain-text HTTP 400/500 responses containing `<` past index 0 rendered as HTML via `#warningBannerText.innerHTML`.
- **Root Cause:**
  1. `chatContainerContent` included `chatMessagesContent` (`fragments/chat.html`), which unconditionally rendered `<input id="hiddenSessionId" hx-swap-oob="outerHTML"/>` before the `<form>`'s own `<input id="hiddenSessionId"/>`; `document.getElementById('hiddenSessionId')` mutated only the first element in DOM order.
  2. `isSafeMarkdownUrl` matched leading `/` without rejecting `//` or `/\`, and `showWarning(msg)` wrote `bannerText.innerHTML` even for plain-text backend error responses.
- **Detection Gap ("What did we miss?"):** `DashboardChatJsTest` mounted a synthetic DOM with a single `#hiddenSessionId` rather than parsing Thymeleaf-rendered `chatContainerContent`, tested only `javascript:`/`data:` URLs, and stubbed `#warningBannerText` without asserting `innerHTML` stayed empty.
- **Resolution:**
  - Added `th:with="omitOobHiddenSessionId=true"` on `#chatMessages` inside `chatContainerContent` and guarded the OOB `#hiddenSessionId` in `chatMessagesContent` with `th:if="${omitOobHiddenSessionId != true}"`.
  - Tightened `isSafeMarkdownUrl` in `dashboard-chat.js` to reject control characters, `//`, and `/\`.
  - Updated `showWarning(msg)` in `dashboard-core.js` to assign `bannerText.textContent = msg` instead of `innerHTML`.
- **Safety Net Added:**
  - `AuraTestChatControllerTest.testChatContainerDoesNotDuplicateHiddenSessionIdWhileMessagesFragmentEmitsOob`
  - Extended assertions in `DashboardChatJsTest.testMarkdownRendererAndSubmitGuardsViaNode` and `DashboardChatJsTest.testChatInputPreservationConfirmationRecoveryAndErrorScopingViaNode`
