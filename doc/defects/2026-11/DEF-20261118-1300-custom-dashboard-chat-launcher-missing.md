# [DEF-20261118-1300] Custom Dashboard Chat Launcher Missing & Downstream GenAI Classpath Conflict

- **Status:** `Resolved`
- **Opened:** 2026-11-18 12:47
- **Closed:** 2026-11-18 13:15
- **Component:** `aura-manager`
- **Scope:** `Framework & Infra/Build`
- **Symptom:**
  1. When viewing custom dashboard pages (`/dashboard-pages/{id}`) or the root dashboard route (`/`), the Neodymium AI Assistant launcher button is completely missing from the UI.
  2. When asking the assistant to analyze dashboards or runs (e.g. *"Analyze the current dashboard and highlight any issues or failures"*), Spring AI exhausts retries and crashes with `java.lang.NoSuchMethodError: 'java.util.Optional com.google.genai.types.Part.toolCall()'`.
- **Root Cause:**
  1. `report-manager.css` line 871 suppresses `#appHeader` via `body:has(#pageCustomDashboard) #appHeader { display: none; }` because custom dashboards render their own header toolbar `#customDashboardPageToolbar` in `fragments/custom-dashboard-page.html`. PR #564 added `chatLauncherBtn` only to `fragments/header.html` and omitted it from `custom-dashboard-page.html`, making the launcher completely inaccessible on all custom dashboard views.
  2. The downstream consumer project (`neodymium-ai-testcase-hub/pom.xml`) had an obsolete direct dependency on `com.google.genai:google-genai:1.28.0`. Under Maven dependency mediation rules, direct dependencies in a root consumer POM override transitive dependencies from imported libraries. In `google-genai:1.28.0`, `part.toolCall()` did not exist (only `part.functionCall()`), causing the JVM to fail with `NoSuchMethodError` as soon as Spring AI 2.0.0 deserialized candidate tool calls.
- **Detection Gap ("What did we miss?"):**
  1. `StaticResourceSyntaxTest.testAuraChatLauncherInTopHeaderAndTopRightOverlay` verified that `#auraChatLauncher` was added twice to `fragments/header.html`, but never tested whether it was present in `fragments/custom-dashboard-page.html` or rendered when `#appHeader` is suppressed by CSS.
  2. Unit tests in `AuraChatServiceTest` and `AuraMcpToolCallbackProviderTest` mocked `ChatModel` via Mockito, bypassing real Google GenAI SDK candidate deserialization and masking the binary classpath incompatibility.
- **Resolution:**
  1. Added `<th:block th:replace="~{fragments/header :: chatLauncherBtn}"></th:block>` to `#customDashboardPageToolbar` in `fragments/custom-dashboard-page.html`.
  2. Updated `openAuraChat()` and `closeAuraChat()` in `dashboard-runner.js` to toggle the `.active` class across `document.querySelectorAll('.aura-chat-launcher')` to support multiple toolbar locations without ID collision.
  3. Added auto-refresh fallback in `dashboard-core.js` so that `actions.includes('refresh_runs')` updates `#mainViewContainer` via `/runs/refresh` when `#queueListContainer` is absent.
  4. Enforced `com.google.genai:google-genai:1.58.0` in `aura-manager/pom.xml`'s `<dependencyManagement>` and `<dependencies>`, and enabled `<parameters>true</parameters>` in `maven-compiler-plugin`.
  5. Removed the obsolete `google-genai:1.28.0` direct dependency from `neodymium-ai-testcase-hub/pom.xml`.
- **Safety Net Added:**
  - Added regression assertion in `StaticResourceSyntaxTest.testAuraChatLauncherInTopHeaderAndTopRightOverlay` verifying `fragments/custom-dashboard-page.html` includes `fragments/header :: chatLauncherBtn`.
  - Added `AuraChatModelFactoryTest.testGoogleGenAiPartToolCallMethodAvailableOnClasspath` asserting `Part.toolCall()` returns `Optional` on the runtime classpath.
