## 1. Core Result Classes (AGPLv3 under package `com.xceptance.neodymium.ai.core`)

- [ ] 1.1 Create `LlmCallDetails.java` to document system/user prompts, DOM string, screenshot, token usage, and exception details
- [ ] 1.2 Create `StepDetails.java` to document raw/expanded instructions, actions, duration, failure reasons, and PESAP warnings
- [ ] 1.3 Create `LookupDetails.java` to track placeholder keys, resolved values, source string, and localization flags
- [ ] 1.4 Create `EscalationDetails.java` to record context escalation levels, trigger type, and reasons
- [ ] 1.5 Create `AiExecutionResult.java` aggregating steps, lookups, escalations, token stats, and test data snapshot
- [ ] 1.6 Create `AiTestRunResult.java` representing composite results for before, steps, and after stages

## 2. Reusable Test Utilities (AGPLv3 under package `com.xceptance.neodymium.ai.testing`)

- [ ] 2.1 Create `AiMockResponse.java` modeling sequential mock outcomes, latency, custom tokens, and simulated HTTP/connection failures
- [ ] 2.2 Create `MockLlmClient.java` extending `LlmClient` overriding public chat methods using the queue of mock responses
- [ ] 2.3 Create `MockPageAnalyzer.java` extending `PageAnalyzer` to return canned DOMs/screenshots and record requested levels/titles
- [ ] 2.4 Create `MockActionExecutor.java` extending `ActionExecutor` overriding both `execute(Action)` and `executeAll(List<Action>)` to block Selenide completely
- [ ] 2.5 Add static `configureForOffline()` / `configureForOffline(String, boolean)` on `MockLlmClient` to set the system properties `neodymium.ai.apiKey` and `neodymium.ai.pesap.enabled` to satisfy the config checks and prevent unwanted PESAP LLM calls.

## 3. Pluggable Dependency Injection

- [ ] 3.1 Add a protected no-arg constructor to `LlmClient.java` to permit browserless instantiations without live config dependencies
- [ ] 3.2 Update `AiBrowser.java` constructors to align `aiStats` reference with the injected `LlmClient`'s stats reference
- [ ] 3.3 Update `AiBrowser.java` constructor to support constructor DI injecting `LlmClient`, `PageAnalyzer`, and `ActionExecutor`
- [ ] 3.4 Update `Neodymium.java` to expose `getLastAiExecutionResult()`, `getLastAiTestRunResult()`, and `setAiBrowser(AiBrowser)`

## 4. Execution Result Thread Aggregation & Logging

- [ ] 4.1 Update `AiBrowser.resolveTestDataToPrompt(...)` with an overloaded method accepting `List<LookupDetails> lookupsCollector`
- [ ] 4.2 Update `AiBrowser.execute(String)` to instantiate `AiExecutionResult` at start and pass it to `AiAgent.execute(...)`
- [ ] 4.3 Update `AiAgent.execute(...)` to both receive AND return `AiExecutionResult` and record step-level logs, durations, and details directly to it
- [ ] 4.4 Update `AiAgent.execute(...)` to calculate token delta snapshotted differences using the aligned `AiStats` instance
- [ ] 4.5 Cache the executed results on `AiBrowser` instance upon successful completion or when exceptions are caught/thrown

## 5. Documentation & Recipes

- [ ] 5.1 Create a comprehensive testing guide `doc/AI_TESTING.md` using the updated constructor DI recipes for browserless testing
- [ ] 5.2 Create the rich set of JUnit demo tests `src/test/java/com/xceptance/neodymium/ai/AiExecutionResultDemoTest.java` covering self-healing, vision escalations, and template resolutions fully offline
- [ ] 5.3 Add comprehensive documentation about the API key guard check and PESAP's default-enabled LLM queries under `doc/AI_TESTING.md`

## 6. Verification

- [ ] 6.1 Execute the test verification suite to ensure all assertions pass browserlessly:
  ```bash
  mvn clean test -Dtest=com.xceptance.neodymium.ai.AiExecutionResultDemoTest
  ```
