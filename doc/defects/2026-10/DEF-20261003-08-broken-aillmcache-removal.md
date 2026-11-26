# [DEF-20261003-08] Broken @AiLlmCache Drops Native Tool Calls and Causes Agent Desynchronization

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`CachingLlmProvider`, `AiLlmCache`, `LlmCacheHelper`, `InMemoryLlmCache`, `NeodymiumAiRunner`)
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  Tests annotated with `@AiLlmCache` fail or emit warnings during agent execution when replaying cached responses because tool calls and reasoning blocks are missing, leading to "no valid tool call" warnings or runner divergence.
- **Root Cause:**
  1. `CachingLlmProvider` instantiated cached responses via `new LlmResponse(cached.content(), new TokenUsage(0, 0, cachedTokens, cachedTokens), ...)`, completely dropping `toolCalls` and `thinking`. In modern agent execution with native tool calling, `content` is frequently empty and actions reside in `toolCalls`. The agent therefore saw empty responses with zero tool calls.
  2. `buildCacheKey` computed keys strictly from `systemMessage` and `userMessage`, omitting conversational history, tool results, tool schemas, and screenshot payloads, resulting in cross-turn and cross-test cache collisions.
  3. `LlmCacheHelper` walked `Thread.currentThread().getStackTrace()` performing reflection lookups on every session initialization, while `InMemoryLlmCache` maintained state across tests in a global static map.
- **Detection Gap ("What did we miss?"):**
  The original unit test (`LlmCacheTest`) tested only synthetic single-turn text responses using a mock delegate, never exercising native tool calls, multi-turn agent loops, or vision payloads.
- **Resolution:**
  1. Deleted `AiLlmCache.java`, `CachingLlmProvider.java`, `InMemoryLlmCache.java`, `LlmCacheHelper.java`, and `LlmCacheTest.java`.
  2. Removed `BeforeAllCallback`, `AfterAllCallback`, and cache lifecycle clearing hooks from `NeodymiumAiRunner.java`.
  3. Removed `LlmCacheHelper.wrapRegistryIfActive` call from `SelenideBrowserSession.java`.
  4. Removed dead `wrapProviders` method from `LlmRegistry.java`.
  5. Stripped `@AiLlmCache` annotations and imports from demo tests (`ProgrammaticDemoTest.java`, `ProgrammaticCustomRecordingDirectoryTest.java`, `ProgrammaticIncludesDemoTest.java`).
  6. Updated `doc/DOCUMENTATION.md` removing Section 7.1 and renumbering Section 7.
- **Safety Net Added:**
  Verified build and test compilation across all modules (`mvn clean test-compile`).
