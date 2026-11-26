# AI Agent Capability Routing Specification

Purpose: Support pluggable, capability-based LLM provider routing in the AI subsystem to configure specialized LLM models per browser session and fall back gracefully to a default provider.

## ADDED Requirements

### Requirement: Pluggable LLM Engine Registration
The system SHALL support registering multiple pluggable `LlmEngine` instances in a session-scoped `LlmRegistry`.

#### Scenario: Register multiple engines
- **WHEN** a session-scoped registry is initialized and multiple distinct engine instances are registered
- **THEN** the registry maintains references to all registered engines for the duration of the session

### Requirement: Engine Capability Announcement
Each `LlmEngine` MUST explicitly announce its supported capabilities via a set of `LlmCapability` enum values (including `TEXT_ONLY`, `VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`).

#### Scenario: Inspect engine capabilities
- **WHEN** an engine is queried for its capability set
- **THEN** it returns a set containing the exact capabilities it is qualified to perform

### Requirement: Capability-Based Task Routing
The `LlmRegistry` MUST route requests needing specific capabilities to a registered engine that supports them.

#### Scenario: Route vision task to vision engine
- **WHEN** a vision task is requested from the registry
- **THEN** the registry routes the request to a registered engine announcing `VISION` capability

### Requirement: Default Fallback Routing
If no registered engine supports the requested capability, the registry SHALL fall back to a designated default engine (General Handler). If no default is configured, it MUST throw an `IllegalStateException`.

#### Scenario: Fallback to default engine
- **WHEN** a capability not matched by any specialized engine is requested
- **THEN** the registry routes the request to the configured default engine

#### Scenario: Throw exception when no fallback matches
- **WHEN** a capability not matched by any engine is requested and no default engine is set
- **THEN** the registry throws an `IllegalStateException` outlining the missing capability

### Requirement: Bidirectional Engine Prompt Refinement
The `LlmEngine` interface SHALL support bidirectional communication. Instead of receiving statically compiled prompts, the active `LlmEngine` SHALL be passed the execution context and SHALL request or build its own model-specific prompts by calling back into a registered prompt-builder service.

#### Scenario: Engine requests customized prompts
- **WHEN** the active engine processes a step execution chat call
- **THEN** it calls back into the context prompt-builder to retrieve system and user prompts formatted specifically for its model type

### Requirement: Model-Specific Prompt Fallbacks
The prompt-builder registry SHALL support registering and resolving model-specific prompt templates (e.g. specialized schemas or system descriptions for Mistral vs. Gemini). If no model-specific template is registered for the active engine family, the registry MUST fall back to a standard default prompt template.

#### Scenario: Fallback to standard prompt template
- **WHEN** an engine type without a registered custom prompt template requests its prompt
- **THEN** the registry returns the standard default prompt template

### Requirement: Exception-Driven Error Communication
The LLM execution service SHALL communicate parsing errors, model-reported errors, and context escalation requests by throwing specific, structured subclasses of `LlmResponseException`. The thrown exception MUST carry an elaborate list of diagnostic messages (`ParserMessage` records) containing severity levels and exceptions.

#### Scenario: Throw exception on parse failure
- **WHEN** the LLM response fails to parse as valid JSON
- **THEN** the service throws an `LlmResponseParseFailureException` containing the raw response text and parser error details

#### Scenario: Throw exception on context escalation request
- **WHEN** the LLM response requests context escalation via `"st": "ESCALATE"` status
- **THEN** the service throws an `LlmResponseEscalateException` containing the requested target context level

### Requirement: Multi-Stage Response Repairing
The parser system SHALL support registering pluggable response fixers across multiple stages of parsing:
1. **String Response Stage**: Operates on raw response text.
2. **JSON Response Stage**: Operates on deserialized JSON elements.
3. **Model Response Stage**: Operates on the final parsed Java object representation.

#### Scenario: Repair malformed JSON successfully
- **WHEN** a response fixer (e.g. for unescaped newlines) is registered and a response with that anomaly is parsed
- **THEN** the fixer modifies the text, allowing the JSON to parse successfully, and records the repair trace in the execution results

### Requirement: Decoupled Registry Instances per Test Case
Each test case execution session MUST have its own isolated, non-shared `LlmRegistry` instance, preventing concurrent tests from sharing or polluting LLM configurations or engine instances.

#### Scenario: Verify isolated registry state
- **WHEN** concurrent test threads run separate test cases
- **THEN** each thread accesses its own registry instance inside its `ExecutionContext`, and registrations in one thread do not leak or propagate to others

### Requirement: Shared Connection Pooling and Pluggable Token Caching
Heavyweight SUT resources, LLM connection pools, and prompt token caches (e.g., Gemini context caching) SHALL be shared globally or session-wide to benefit from socket reuse and cached prompt tokens.

#### Scenario: Shared connection and prompt caching
- **WHEN** multiple isolated `LlmEngine` instances perform chat calls concurrently
- **THEN** they delegate to a shared socket connection client and query a pluggable, shared token cache to reuse pre-loaded prompt context and reduce visual query costs



