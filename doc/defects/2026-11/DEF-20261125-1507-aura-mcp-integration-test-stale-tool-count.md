# [DEF-20261125-1507] AuraMcpIntegrationTest Asserts Stale MCP Tool Count After analyze_report_differential Addition

- **Status:** `Resolved`
- **Opened:** 2026-11-25 15:07
- **Closed:** 2026-11-25 15:07
- **Component:** `aura-manager` (`AuraMcpIntegrationTest`)
- **Scope:** `Test/Harness`
- **Issue:** GitLab #175
- **Symptom:**
  `AuraMcpIntegrationTest#testSettingsModalIncludesAllRegisteredMcpAndAnalyticsTools` failed on line 484 with `expected: <15> but was: <16>` during full test runs.
- **Root Cause:**
  PR #573 (`447b79865`) registered a 16th `@McpTool` (`analyze_report_differential`) in `AuraAnalyticsToolService` alongside the existing 11 tools in `AuraMcpToolService` and 4 analytics tools in `AuraAnalyticsToolService`. Commit `42e96ed9c` updated the unit tests (`AuraTestSettingsControllerTest` and `AuraMcpToolCallbackProviderTest`) from `15` to `16` and added `"analyze_report_differential"`, but missed `AuraMcpIntegrationTest`.
- **Detection Gap ("What did we miss?"):**
  The `unit-tests` Maven profile in `aura-manager/pom.xml` excludes `**/*IntegrationTest.java`, so unit-only verification skipped `AuraMcpIntegrationTest` when updating the MCP tool count assertions.
- **Resolution:**
  Updated `AuraMcpIntegrationTest#testSettingsModalIncludesAllRegisteredMcpAndAnalyticsTools` to expect `16` registered tools and assert `"analyze_report_differential"` in `mcpToolsList`, and added `"analyze_report_differential"` to the expected tool names checked in `AuraMcpIntegrationTest#testMcpHttpEndpointExposesAndCallsAnalyticsTools`.
- **Safety Net Added:**
  - Verified `AuraMcpIntegrationTest` end-to-end against both the Settings modal (`mcpToolsCount == 16`) and the live `/mcp` `tools/list` JSON-RPC endpoint including `analyze_report_differential`.
