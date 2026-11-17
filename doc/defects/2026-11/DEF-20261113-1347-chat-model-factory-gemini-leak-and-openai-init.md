# [DEF-20261113-1347] ChatModelFactory Cross-Provider Gemini Model/Key Leakage and OpenAI Sync/Async Client Initialization Failure

- **Status:** `Resolved`
- **Opened:** 2026-11-13 13:15
- **Closed:** 2026-11-13 13:47
- **Component:** `aura-manager`
- **Scope:** `AI/Prompt & Config/Environment`
- **Symptom:**
  1. Selecting `neodymium.ai.chat.provider=openai` or `mistral` while `neodymium.ai.provider=gemini` inherited `neodymium.ai.model` (defaulting to Gemini's model name such as `gemini-2.5-flash`) via `config.getModel("chat")` and fell back to `config.aiApiKey()` (sending the Gemini API key to OpenAI/Mistral endpoints).
  2. Configuring OpenAI failed at runtime inside Spring AI 2.0.0's `OpenAiSetup.setupSyncClient` / `OpenAiChatModel$Builder.build` with:
     - `NullPointerException` when `httpClientCustomizers` (`null`) or `Duration timeout` (`null`) was passed to `OpenAiSetup.setupSyncClient`, and
     - `IllegalStateException: At least one credential source must be specified` when `OpenAiChatModel.builder()` lazily called `OpenAiSetup.setupAsyncClient` without receiving an explicitly configured `OpenAIClientAsync`.
  3. Every chat turn recreated heavyweight HTTP/SDK clients (`Client`, `OpenAIClient`, `MistralAiApi`) without caching, and omitted environment-variable fallback lookup for `GEMINI_API_KEY`, `OPENAI_API_KEY`, and `MISTRAL_API_KEY`.

- **Root Cause:**
  1. `AuraChatModelFactory.createChatModel` blindly called `config.getModel("chat")` (which falls back to global `neodymium.ai.model` / Gemini defaults in `AiConfiguration`) and `config.aiApiKey()` without verifying whether the resolved `chat` provider matched the global provider.
  2. `createOpenAiModel` passed `null` for `timeout` and `httpClientCustomizers` to `OpenAiSetup.setupSyncClient`, and omitted `.openAiClientAsync(...)` on `OpenAiChatModel.builder()`, causing Spring AI's builder to fall back to env-only `setupAsyncClient` credential resolution.

- **Detection Gap ("What did we miss?"):**
  - `AuraChatModelFactory` originally had zero dedicated unit tests exercising OpenAI, Mistral, or cross-provider configuration overrides; runtime client creation was only implicitly exercised against Gemini or mocked `ChatModel` beans.

- **Resolution:**
  1. Refactored `AuraChatModelFactory` to resolve `modelName`, `apiKey`, `baseUrl`, `temperature`, and `timeout` per canonical provider (`gemini`, `openai`, `mistral`), guarding global `neodymium.ai.model` and `config.aiApiKey()` fallback with `canonicalProvider.equals(globalProvider)` and adding injectable `envLookup` fallback (`GEMINI_API_KEY`, `OPENAI_API_KEY`, `MISTRAL_API_KEY`).
  2. Fixed `createOpenAiModel` to pass non-null `Duration timeout` (`getTimeoutSeconds("chat") > 0 ? ... : 180`), `List.of()` for `httpClientCustomizers`, and both `OpenAiSetup.setupSyncClient(...)` and `OpenAiSetup.setupAsyncClient(...)` on `OpenAiChatModel.builder()`.
  3. Added a `ConcurrentHashMap<ModelCacheKey, ChatModel>` cache (with `apiKey=***` masked in `ModelCacheKey.toString()`).

- **Safety Net Added:**
  - `AuraChatModelFactoryTest.testOpenAiProviderDoesNotInheritGeminiDefaultModel`
  - `AuraChatModelFactoryTest.testMistralProviderUsesExplicitModelAndEnvFallbackWithoutGeminiLeak`
  - `AuraChatModelFactoryTest.testModelInstanceIsCachedUntilConfigurationChanges`
  - `AuraChatModelFactoryTest.testUnsupportedProviderAndMissingKeyThrowIllegalStateException` (including explicit verification that a global Gemini key is never leaked to an `openai` chat provider override).
