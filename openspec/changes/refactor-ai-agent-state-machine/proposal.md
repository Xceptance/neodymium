## Why

The current `AiAgent` execution logic runs as a monolithic procedural loop that relies heavily on static `ThreadLocal` variables to manage state (e.g., inclusion stacks, condition caches, active agent references). This static thread-binding prevents concurrent parallel test execution and exposes the system to memory leaks and cross-test state pollution. Furthermore, the procedural loop directly mutates loop indices inside catch blocks to handle interactive HUD events (such as edit, add, skip, and rewind), making the execution flow brittle, difficult to debug, and impossible to unit-test.

## What Changes

- **ThreadLocal Elimination**: Remove all static `ThreadLocal` variables across the AI subsystem, replacing them with a thread-isolated, session-scoped `ExecutionContext`.
- **State Machine Execution Loop**: Refactor the procedural execution loop into a strongly-typed state machine. Execution steps, visual hash checking, direct regex bypasses, expected failures, and SUT recovery routines are modeled as discrete state transitions.
- **Event-Driven HUD Routing**: Replace exception-based HUD control flow with a structured event-driven `HudAction` handler, making interactive step modifications and cursor updates explicit.
- **Capability-Based LLM Registry**: Introduce pluggable capability-based routing for LLM providers. Allow registering specialized LLMs per browser session (e.g., local Ollama for text tasks, cloud Gemini for vision tasks) with a general-handler fallback mechanism.
- **Decoupled Playback and LLM Engines**: Keep the offline playback/replay engine and the live LLM generation/healing engine strictly decoupled. Successful playback executions run fully offline without validating API keys or initializing LLM providers. LLM provider lookup and validation are lazily deferred and triggered only when transitioning into the live LLM healing state.
- **Core Execution Concepts Preservation**: Ensure all existing AI execution concepts are fully integrated and mapped onto the state machine:
  - *Three-Phase Step Routing*: Playbook Replay -> Direct Action Bypass -> Live LLM Query.
  - *Historical Context Learning*: Executed context level is saved as `healedContextLevel` in the playbook to optimize starting context levels.
  - *Dynamic Visual Replay Caching*: Views compared locally via dHash/Hamming distance, with visual failure signatures cached for fast-fail.
  - *Two-Tier Retry Budgets*: Separate budgets for execution/WebDriver errors and empty action array loops.
  - *Programmatic Assertions*: Reflective execution of `@AiMethod` assertions under `JAVA_METHOD` actions.
  - *Expected Failures*: Support immediate playback fails for steps marked with `(bug: ID)`.


## Capabilities

### New Capabilities
- `ai-agent-state-machine`: The AI Agent's execution loop is driven by an explicit, strongly-typed state machine transitioning through discrete execution and recovery states. This includes event-driven interactive HUD command routing and explicit SUT state restoration.
- `ai-agent-capability-routing`: Pluggable capability-based LLM provider registry mapping specialized tasks (e.g., vision, step-splitting, structured json) to registered providers, complete with a general-handler default fallback chain.

### Modified Capabilities
<!-- None -->

## Impact

- **Affected Classes**: `AiAgent`, `AiBrowser`, `LlmClient`, `ActionExecutor`, `IncludeAction`, `BranchAction`, and testing mock infrastructures.
- **APIs**: Introduces `ExecutionContext`, `LlmRegistry`, `LlmEngine`, `State`, and `HudCommunicator` interfaces.
- **Dependencies**: No new external dependencies. Decouples the existing LangChain4j integration behind the `LlmEngine` interface.

