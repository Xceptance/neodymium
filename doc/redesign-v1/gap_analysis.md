# Neodymium AI Architecture: Gap Analysis & Potential Issues

Following up on the initial architectural rewrite plan, I have conducted a deep dive into the current implementation (`AiAgent`, `ActionExecutor`, `PlaybookManager`, `ActionRegistry`, `LlmClient`, etc.) to identify concrete technical gaps and hidden potential issues. 

This analysis serves as a foundation for planning the rewrite without making immediate code changes.

## 1. Concurrency & Thread-Safety Gaps

The current execution model heavily relies on implicit `ThreadLocal` state, which is a major anti-pattern for concurrent test execution (especially when runner threads are pooled and reused).

*   **`AiAgent` & `AiBrowser` Leakage**: `AiAgent` maintains `ThreadLocal<AiAgent> activeAgent` and `ThreadLocal<AiExecutionResult> activeResult`. Crucially, `AiBrowser.close()` does not explicitly clear these, meaning stale states will leak into subsequent test executions on the same thread.
*   **Action Plugins**: Plugins like `IncludeAction` use `ThreadLocal<List<String>> RUNTIME_INCLUDE_STACK` to prevent infinite loops. `BranchAction` uses `ThreadLocal` for condition evaluation. This binds execution logic directly to the thread rather than the test session, preventing asynchronous or multi-threaded step processing in the future.
*   **`LlmClient` State**: Maintains a `ThreadLocal<LlmMode> currentCallMode`, which couples the client to the executing thread rather than the context of the specific request.
*   **`ActionRegistry` Initialization**: The registry uses a non-thread-safe `LinkedHashMap` for storing plugins and a non-volatile `boolean initialized` flag. In a highly parallel startup environment, this causes race conditions resulting in plugins being registered multiple times or threads observing a partially initialized map.

## 2. Resource Management & Performance Bottlenecks

*   **`PlaybookManager` File I/O Overhead**: The `getPlaybookDirectory()` method recursively falls back across method, class, and global configurations. At each level, it calls `PropertiesUtil.loadPropertiesFromFile("config/ai.properties")`. This triggers massive, repeated disk I/O on *every* playbook load and save, completely un-cached.
*   **Shadow DOM Resolution Penalties**: `ActionExecutor` dynamically injects massive JavaScript strings (e.g., `SHADOW_DOM_FINDER_PREFIX`) to traverse shadow roots on every interaction strategy fallback. On complex SPAs, running this heavy JS execution loop iteratively severely degrades execution speed. Elements and coordinates should be cached within a single step turn.

## 3. Structural Coupling & API Boundaries

*   **Global Configuration Singletons**: Core classes (like `ActionRegistry` and `LlmClient`) reach out globally to `Neodymium.aiConfiguration()` instead of accepting configuration via constructors or initialization parameters. This prevents isolated unit testing of these components.
*   **Lack of Playbook Atomicity**: As identified in the architecture proposal, `PlaybookManager.savePlaybook()` opens a `FileWriter` directly to the target file. If the JVM halts during serialization, the JSON file is truncated and corrupted permanently.
*   **Exception Pollution**: `AiAgent.java` houses multiple inner exception classes (like `DefinitiveAssertionError`, which is over 100 lines long). Moreover, exceptions are used heavily for standard flow control (e.g., `HudActionException` thrown specifically to mutate loop indices).

## 4. LLM Abstraction & Security Gaps

*   **Hardcoded Gemini Dependency**: `LlmClient` directly references LangChain4j Google Gemini classes (`GoogleAiGeminiChatModel`). There is no interface-based abstraction for injecting local models (like Ollama) or alternative cloud providers.
*   **Missing Data Redaction**: The codebase currently lacks a `ContextAnonymizer`. The raw DOM (which includes inputs populated with test data like passwords or PII) is sent entirely in the clear to the LLM during `PageAnalyzer` execution.
*   **Hardcoded Magic Numbers**: The visual validation relies on a hardcoded Hamming distance of `15` sprinkled in four different locations inside `AiAgent`, making it impossible to tune the strictness of visual regression per project.

## Next Steps

With these gaps identified, the previously outlined **Phased Verification & Implementation Plan** (moving to a State Machine, introducing `ExecutionContext`, and building the `LlmProvider` SPI) is technically sound and directly addresses these structural vulnerabilities. 

*No implementation has been performed at this stage. Please review this gap analysis to confirm alignment before we begin code modifications.*
