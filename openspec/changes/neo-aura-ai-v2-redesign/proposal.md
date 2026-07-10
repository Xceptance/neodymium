## Why

The current Neo Aura AI orchestration engine is tightly coupled to Selenide browser drivers, procedural loops, static ThreadLocals, and specific log output channels. This coupling prevents the engine from running tests on non-browser interfaces (such as REST APIs, CLI tools, or databases), blocks running test sessions concurrently in parallel execution threads, makes unit testing complex, and complicates dynamic credential masking, step splitting, and interactive debugger breaks.

---

## What Changes

* **Domain-Agnostic State Machine**: Replaces the coupled procedural execution loop with a clean `StateMachineRunner` operating on abstract SUT interfaces (`TargetExecutor` and `SutState`).
* **Typed Exception Flow Control**: Replaces the custom `FlowControl` and `StepResult` enums with a robust `PipelineException` hierarchy (e.g. `EscalationException`, `StepSplitException`, `HealingRequiredException`), utilizing a multi-catch `TryCatchStep` wrapper.
* **On-the-Fly Sanitization**: Replaces the post-execution sanitization pass with on-the-fly credential masking (using format-preserving mock patterns or user stand-ins) and dynamic action parameterization (`ActionSanitizer`) as actions are executed.
* **Protocol-Level Authentication**: Implements browser-native Basic Auth interception (via Selenium 4 CDP) and REST Bearer/Basic header injection before step execution starts.
* **Abstract Resource Management**: Delegates playbook and recording reads/writes to `PlaybookResourceManager` (introducing `InMemoryResourceManager` to support direct next replay without disk I/O).
* **Decoupled Lifecycle Events**: Uncouples logging, Allure reporting, trace viewer, and HUD listeners from the runner via a synchronous `ExecutionEventBus`.

---

## Capabilities

### New Capabilities
- `decoupled-state-runner`: Domain-blind state machine running on abstract target executors and composable pipeline steps.
- `adaptive-context-escalation`: Adaptive context detail levels (LEAN, SEMANTIC, FULL) based on failure feedback and typed exceptions.
- `on-the-fly-sanitization`: Runtime secret masking and action parameterization before actions are recorded in the playbook recording.
- `protocol-authentication-interception`: Native session-level authentication configuration (Basic Auth CDP interception, REST header token injection).

### Modified Capabilities
- `includes-resolution`: Updated to resolve relative include paths dynamically via `PlaybookResourceManager` rather than assuming local filesystem paths.

---

## Impact

* **Affected Packages**: Refactors and extends `com.xceptance.neodymium.ai.*`.
* **APIs**: Introduces clean static factories on abstract `AiSession` (e.g., `AiSession.selenide()`, `AiSession.rest()`) and makes concrete session classes package-private.
* **Dependencies**: Extends Selenium 4 API usage (for CDP `HasAuthentication` interception).
* **Testing**: Replaces mock browser frameworks with local `MockLlmProvider` and `MockTargetExecutor` unit test fixtures.
