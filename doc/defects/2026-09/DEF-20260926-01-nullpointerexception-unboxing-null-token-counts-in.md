# [DEF-20260926-01] NullPointerException Unboxing Null Token Counts in GeminiLlmProvider and LLM Providers

- **Status:** Resolved
- **Opened:** 2026-09-26
- **Closed:** 2026-09-26
- **Component:** `neodymium-core` (`ai-client`)
- **Scope:** `Framework`
- **Symptom:** In live agent execution (e.g., `IncludeIntegrationTest.testIncludeConditionalIfThenFalse`), the request fails with `ConclusiveFailureException` caused by `java.io.IOException: Failed to execute Gemini chat request: Cannot invoke "java.lang.Integer.intValue()" because the return value of "dev.langchain4j.model.output.TokenUsage.outputTokenCount()" is null`.
- **Root Cause:** In `GeminiLlmProvider.java` (and similarly in `OpenAiLlmProvider`, `VertexAiLlamaProvider`, and `MistralLlmProvider`), boxed `Integer` counts from LangChain4j's `dev.langchain4j.model.output.TokenUsage` were directly passed into primitive `int` constructor parameters of `TokenUsage(int, int, int, int)`. When LLM responses omit candidate/output token metadata (typical for tool calls or specific finish reasons), LangChain4j yields `null`, triggering an NPE during implicit auto-unboxing.
- **Detection Gap ("What did we miss?"):** Mock LLM unit tests construct `TokenUsage` with explicit non-null primitive integers (`new TokenUsage(10, 10, 20)`). Provider unit tests did not simulate LangChain4j responses with `null` token counters.
- **Resolution:** Introduced null-safe factory methods `TokenUsage.of(Integer, Integer, Integer, Integer)` and `TokenUsage.of(Integer, Integer, Integer)` in `TokenUsage.java` that coalesce `null` values to 0 and derive `totalTokenCount` if absent. Updated all four LLM providers (`GeminiLlmProvider`, `OpenAiLlmProvider`, `VertexAiLlamaProvider`, `MistralLlmProvider`) to use `TokenUsage.of(...)`.
- **Safety Net Added:** Unit tests in `TokenUsageTest` testing null and partial-null permutations for `TokenUsage.of(...)`.
