# [DEF-20261118-1415] MCP Tool Layer Fails to Parse Multi-Browser Profiles and Silently Falls Back to Default Chrome

- **Status:** `Resolved`
- **Opened:** 2026-11-18 14:15
- **Closed:** 2026-11-18 14:20
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  When requesting tests or suites via Neodymium AI Chat or MCP tools (`run_test` / `run_suite`) with multiple browser profiles (e.g. `"Chrome_1500x1000, Firefox_1500x1000"`), the test executes only on the default single Chrome profile instead of all requested browsers.
- **Root Cause:**
  1. `AuraMcpToolService` declared `browserProfile` as a singular scalar `String` with no multi-profile delimiter parsing in `McpExecutionCoordinator`.
  2. `McpExecutionCoordinator` wrapped delimited profile strings verbatim into `List.of(browserProfile)`, causing `AuraQueueService.getEffectiveBrowserProfiles` to reject the compound string as an unknown profile ID and fall back to the default Chrome profile.
  3. `AuraChatService`'s system prompt lacked instructions on how to invoke multi-browser test executions.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `McpExecutionCoordinatorTest` and `AuraMcpToolServiceTest` tested only single browser strings (`"Chrome"`) or null, never validating comma-delimited strings or multi-browser execution payloads.
- **Resolution:**
  1. Implemented `parseBrowserProfiles(String)` in `McpExecutionCoordinator` to cleanly split comma-, semicolon-, or bracketed/quoted strings into `List<String>`.
  2. Updated `@McpToolParam` descriptions on `runTest` and `runSuite` in `AuraMcpToolService` to explicitly document comma-separated profile support.
  3. Updated `SYSTEM_PROMPT` in `AuraChatService` to instruct the AI assistant that multiple browser profiles can be passed as comma-separated tags.
- **Safety Net Added:**
  - `McpExecutionCoordinatorTest.testParseBrowserProfilesVariations`: Direct assertion across null, empty, single, comma-separated, semicolon-separated, brackets, and quotes.
  - `McpExecutionCoordinatorTest.testSubmitRunWithMultipleBrowserProfiles`: Asserts compound strings like `"Chrome_1500x1000, Firefox_1500x1000"` are correctly parsed and passed to both `globalBrowserProfiles` and `DatasetSelection.browserProfiles`.
  - `McpExecutionCoordinatorTest.testSubmitSuiteWithMultipleBrowserProfiles`: Asserts suite runs parse multiple profiles across datasets.
