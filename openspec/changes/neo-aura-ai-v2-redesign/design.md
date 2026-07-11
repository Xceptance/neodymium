## Context

Neo Aura AI v1 is a coupled, browser-bound AI execution engine. The redesigned v2 engine separates the orchestration runner from browser actions, driver configuration, and log sinks by introducing a decoupled, event-driven state machine. This design focuses on creating domain-blind execution models, thread isolation for parallel test runs, and a composable pipeline architecture.

---

## Goals / Non-Goals

**Goals:**
* Define a domain-blind `StateMachineRunner` that operates on abstract `TargetExecutor` and `SutState` interfaces.
* Implement thread-isolated session instances (`AiSession`) allowing parallel execution of multiple AI runs without `ThreadLocal` sharing.
* Implement typed exception-based flow control (`PipelineException` hierarchy) to handle pipeline routing, healing retries, and compound step splitting.
* Support format-preserving secret masking and on-the-fly action parameterization during runtime recording.
* Implement protocol-level driver interception for Basic Auth (Selenium 4 `HasAuthentication`) and REST Bearer token injection.
* Support offline browserless test execution using `MockLlmProvider` and `MockTargetExecutor` fixtures.
* Implement a custom JUnit 5 runner extension and suite of annotations (`@NeodymiumAiTest`, `@AiPlaybook`, `@AiMode`, `@AiDataSet`, etc.) supporting convention-based playbook resolution, parameterization, and lifecycle sharing.
* Implement dynamic, model-family-optimized prompt compilation via `PromptBuilderService` and output repairing via a Multi-Stage Response Repairer.
* Implement pluggable lifecycle hooks (`PreExecutionHook`, `PostExecutionHook`) and standard audit implementations (`LlmExecutionAuditor`, `DataConsistencyAuditor`, `AuraVisualAuditor`).
* Implement a Visual Root Cause Analysis (RCA) diagnostic loop and a Two-Stage Semantic Healing comparison pattern (Semantic Divergence Analysis).

**Non-Goals:**
* Implementing concrete executors for non-browser/non-REST interfaces (such as Database or CLI) in this phase.

---

## Decisions

### 1. Exception-Based Flow Control & Composable Pipeline Steps
* **Decision**: We model the runner execution sequence as composable pipeline steps (`PipelineStep`) that throw typed `PipelineException` instances (e.g. `EscalationException`, `StepSplitException`, `HealingRequiredException`) to communicate routing directives. These exceptions are caught by `TryCatchStep` nodes to route to appropriate catch subpipelines (analogous to Java `try-catch` blocks).
* **Rationale**: Custom flow-control enums (`FlowControl`) wrapped in result records cannot carry rich diagnostic metadata (like failed actions or target escalation levels). Leveraging native Java exception types keeps signatures simple, type-safe, and highly extensible.
* **Alternatives Considered**: Enum-based state mapping inside the runner loop. Rejected due to the risk of creating a complex procedural loop inside the main runner class.

### 2. On-the-Fly Sanitization & Parameterization
* **Decision**: Masking of secrets in prompts and DOM structures (using format-preserving mock patterns or user stand-ins) occurs immediately before sending data to the LLM. Parameterization of recorded actions (converting raw typed strings to variables like `${userPassword}`) occurs on-the-fly as the actions are executed.
* **Rationale**: Identifying which raw strings in a finished `PlaybookRecording` correspond to variables is extremely difficult after the run is complete. Doing this on the fly when the execution context has full variable mapping preserves correctness.
* **Alternatives Considered**: Post-execution sanitization pass. Rejected due to the high risk of false positives/negatives in matching credentials after execution data is serialized.

### 3. Protocol-Level Authentication Interception
* **Decision**: Native website Basic Auth is intercepted at the driver layer using Selenium 4's `HasAuthentication` CDP registration. API authorization (e.g. Bearer tokens) is injected directly using HTTP Client request interceptors.
* **Rationale**: Basic auth challenges are OS-level dialogs that do not exist in the DOM, making them completely invisible to the LLM. Intercepting them at the protocol level ensures the LLM only interacts with the fully rendered, authenticated web application.
* **Alternatives Considered**: Simulated UI interaction steps. Rejected because browser-native authentication prompts are not interactable via DOM-based click/type selectors.

### 4. JUnit 5 Extension & Annotation Mapping
* **Decision**: We build a custom JUnit 5 extension (`NeodymiumAiRunner`) implementing `TestTemplateInvocationContextProvider`. `@NeodymiumAiTest` marks the class, resolving playbooks by package package/convention, and `@AiPlaybook` marks test methods. `@AiDataSet` filters data sets, and `@AiMode` configures sequential/single execution modes.
* **Rationale**: `@Test` is always required to maintain IDE integration, while the custom template provider handles spawning parameterized invocations for multiple data sets dynamically.
* **Alternatives Considered**: A custom JUnit runner class extending old JUnit 4 runner concepts. Rejected in favor of modern JUnit 5 extension APIs.

### 5. Two-Stage Semantic Healing & Visual RCA
* **Decision**: When divergence or failure occurs, the runner performs a Semantic Divergence Analysis comparison of the baseline state and current state before action generation, storing a Semantic Diff Summary in the context. Upon conclusive failure, a Vision-based LLM query captures a Visual RCA diagnostic.
* **Rationale**: Decoupling the diagnostic task (finding what changed) from generation improves LLM success rates and reduces trial-and-error, while Visual RCA yields plain-English failure descriptions in test reports.
* **Alternatives Considered**: Direct live generation of corrective actions without pre-comparison. Rejected because it frequently results in incorrect LLM assumptions and wasted tokens.

### 6. Model-Tailored Prompt Compilation & Repairing
* **Decision**: We introduce a `PromptBuilderService` accessed by `LlmProvider` to format prompt messages based on target model family syntax (e.g. Gemini headers vs Mistral messages). Raw responses pass through a Multi-Stage Response Repairer (Raw string stage, JSON Element stage, Java Object stage).
* **Rationale**: LLM models react very differently to system message placement. Repairing JSON at multiple stages prevents minor model formatting errors from throwing parsing exceptions.
* **Alternatives Considered**: Static prompt string interpolation in pipeline steps. Rejected due to model syntax divergence.

### 7. Post-Execution Audit Verification
* **Decision**: Pluggable post-run audit hooks (`LlmExecutionAuditor`, `DataConsistencyAuditor`, `AuraVisualAuditor`) are executed sequentially at session completion, publishing diagnostic events or throwing exceptions to fail the run.
* **Rationale**: This allows post-run validation of logical correctness, variable structures, and visual regressions without cluttering the main state machine runner.
* **Alternatives Considered**: Verification inside the main runner steps. Rejected to maintain strict single-responsibility separation.

### 8. Immutable Playbook & Defensive SessionData Copying
* **Decision**: The parsed `Playbook` and its associated dataset parameter lists are strictly immutable. When the session initializes, it copies the active dataset map defensively to construct the thread-isolated `SessionData` instance.
* **Rationale**: This prevents dynamic runtime variable modifications (e.g. dynamic value extractions or HUD edits) from mutating the original static dataset, keeping it clean and consistent for subsequent replays or parallel execution threads.
* **Alternatives Considered**: Direct mapping of the parsed dataset map. Rejected due to the high risk of cross-thread contamination during parallel runs.

### 9. Parallel Implementation in `org.neodymium.ai`
* **Decision**: All redesigned v2 engine classes and interfaces will be implemented from scratch under a new parallel package structure `org.neodymium.ai.*` (removing the legacy `com.xceptance` prefix). Required core utility helper classes (e.g. config loaders, tag extractors, and element selectors) will be copied or rewritten directly into the new package namespace rather than importing from legacy packages.
* **Rationale**: This decouples v2 implementation from legacy v1 classes (`com.xceptance.neodymium.ai.*`), enabling side-by-side verification and testing without breaking existing classic AI test suites. Furthermore, removing the company prefix makes the library open-source ready.
* **Alternatives Considered**: Modifying existing v1 classes in-place or importing legacy utilities directly. Rejected because it would instantly break the existing 22 action plugins and couple the new package to legacy company-namespaced classes.

---

## Risks / Trade-offs

* **[Risk] File Locking Conflicts**: Running concurrent session threads writing playbooks or recordings to disk via a shared `LocalFileResourceManager` could result in write collisions or file corruption.
  * *Mitigation*: Ensure `LocalFileResourceManager` isolates file writes by appending unique session or thread identifiers, or implements light file locking.
* **[Risk] Infinite Healing Loops**: In-page self-healing could get stuck in an infinite retry loop if SUT states cycle indefinitely.
  * *Mitigation*: Enforce a strict retry budget ceiling (default `maxRetries = 3`) within the `LoopStep` execution predicate.
