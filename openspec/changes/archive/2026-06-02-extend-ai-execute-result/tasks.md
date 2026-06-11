## 1. Core Result Classes (AGPLv3 under package `com.xceptance.neodymium.ai.core`)

- [x] 1.1 Create `LlmCallDetails.java` to document system/user prompts, DOM string, screenshot, token usage, and exception details
- [x] 1.2 Create `StepDetails.java` to document raw/expanded instructions, actions, duration, failure reasons, and PESAP warnings
- [x] 1.3 Create `LookupDetails.java` to track placeholder keys, resolved values, source string, and localization flags
- [x] 1.4 Create `EscalationDetails.java` to record context escalation levels, trigger type, and reasons
- [x] 1.5 Create `AiExecutionResult.java` aggregating steps, lookups, escalations, token stats, and test data snapshot
- [x] 1.6 Create `AiTestRunResult.java` representing composite results for before, steps, and after stages

## 2. Reusable Test Utilities (AGPLv3 under package `com.xceptance.neodymium.ai.testing`)

- [x] 2.1 Create `AiMockResponse.java` modeling sequential mock outcomes, latency, custom tokens, and simulated HTTP/connection failures
- [x] 2.2 Create `MockLlmClient.java` extending `LlmClient` overriding public chat methods using the queue of mock responses
- [x] 2.3 Create `MockPageAnalyzer.java` extending `PageAnalyzer` to return canned DOMs/screenshots and record requested levels/titles
- [x] 2.4 Create `MockActionExecutor.java` extending `ActionExecutor` overriding both `execute(Action)` and `executeAll(List<Action>)` to block Selenide completely
- [x] 2.5 Add static `configureForOffline()` / `configureForOffline(String, boolean)` on `MockLlmClient` to set the system properties `neodymium.ai.apiKey` and `neodymium.ai.pesap.enabled` to satisfy the config checks and prevent unwanted PESAP LLM calls.

## 3. Pluggable Dependency Injection

- [x] 3.1 Add a protected no-arg constructor to `LlmClient.java` to permit browserless instantiations without live config dependencies
- [x] 3.2 Update `AiBrowser.java` constructors to align `aiStats` reference with the injected `LlmClient`'s stats reference
- [x] 3.3 Update `AiBrowser.java` constructor to support constructor DI injecting `LlmClient`, `PageAnalyzer`, and `ActionExecutor`
- [x] 3.4 Update `Neodymium.java` to expose `getLastAiExecutionResult()`, `getLastAiTestRunResult()`, and `setAiBrowser(AiBrowser)`

## 4. Execution Result Thread Aggregation & Logging

- [x] 4.1 Update `AiBrowser.resolveTestDataToPrompt(...)` with an overloaded method accepting `List<LookupDetails> lookupsCollector`
- [x] 4.2 Update `AiBrowser.execute(String)` to instantiate `AiExecutionResult` at start and pass it to `AiAgent.execute(...)`
- [x] 4.3 Update `AiAgent.execute(...)` to both receive AND return `AiExecutionResult` and record step-level logs, durations, and details directly to it
- [x] 4.4 Update `AiAgent.execute(...)` to calculate token delta snapshotted differences using the aligned `AiStats` instance
- [x] 4.5 Cache the executed results on `AiBrowser` instance upon successful completion or when exceptions are caught/thrown

## 5. Documentation & Recipes

- [x] 5.1 Create a comprehensive testing guide `doc/AI_TESTING.md` using the updated constructor DI recipes for browserless testing
- [x] 5.2 Create the rich set of JUnit demo tests `src/test/java/com/xceptance/neodymium/ai/AiExecutionResultDemoTest.java` covering self-healing, vision escalations, and template resolutions fully offline
- [x] 5.3 Add comprehensive documentation about the API key guard check and PESAP's default-enabled LLM queries under `doc/AI_TESTING.md`

## 6. Verification

- [x] 6.1 Execute the test verification suite to ensure all assertions pass browserlessly:
  ```bash
  mvn clean test -Dtest=com.xceptance.neodymium.ai.AiExecutionResultDemoTest
  ```
