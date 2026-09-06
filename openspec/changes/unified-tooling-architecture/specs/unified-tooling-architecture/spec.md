## Purpose

Provides a unified in-process tooling architecture for Neodymium that models browser interactions, Java helper methods, assertions, and domain plugins as typed, schema-validated tools with deterministic playback recording.

## ADDED Requirements

### Requirement: Standard in-process tool contract
The system SHALL define a standard tool contract where every executable capability exposes a unique name, a description, and an OpenAPI/JSON Schema defining its input parameters, and executes to produce a structured result containing text, data, and execution status.

#### Scenario: Discover and inspect tool definition
- **WHEN** a tool is registered in the tool registry
- **THEN** the system provides its tool definition containing name, description, and valid JSON Schema describing expected parameters and required fields.

### Requirement: Java tool schema generation and typed invocation
The system SHALL reflectively inspect public Java methods annotated with tool annotations, automatically generate their parameter JSON Schema using standard types, deserialize incoming structured arguments into the target Java types, and execute the method.

#### Scenario: Execute multi-argument Java tool with typed conversion
- **WHEN** a tool call for an annotated Java method is received with structured arguments
- **THEN** the system converts the arguments into the declared method parameter types and executes the method without requiring manual string parsing.

### Requirement: Return value capture and context variable assignment
The system SHALL capture the return value of an executed tool and, when a target variable name is configured, store the result in the active test execution session context.

#### Scenario: Assign tool return value to session variable
- **WHEN** a tool call specifies a target variable name and executes successfully
- **THEN** the return value is saved into the session data context under the specified variable key for subsequent step resolution.


### Requirement: Browser tools encapsulation
The system SHALL expose standard browser automation operations (including click, type, navigate, select, hover, assert text, and execute script) as standard tools, isolating browser driver interactions behind the tool contract.

#### Scenario: Execute browser action via tool contract
- **WHEN** a browser click tool call is dispatched with a target element selector
- **THEN** the system executes the click interaction on the matching element in the active browser session and returns a success status.

### Requirement: Element metadata and offline similarity healing in playbooks
The system SHALL record tool calls in the recorded JSON playbook and preserve target element metadata (including DOM feature vectors, candidate locators, and visual hashes) to enable deterministic offline execution and sub-millisecond similarity healing without invoking LLMs during replay.

#### Scenario: Replay recorded browser tool call offline
- **WHEN** a recorded playbook containing browser tool calls is executed in replay mode
- **THEN** the system executes the recorded tool calls directly without LLM invocation, applying DOM feature vector similarity matching if the target selector has shifted.

### Requirement: Composite plugin execution via tool context
The system SHALL provide an execution context to tools allowing them to invoke other registered tools, access or set session variables, and attach structured artifacts to the test report without accessing global driver singletons.

#### Scenario: Composite WCAG tool executes script and attaches report
- **WHEN** a composite accessibility tool is executed
- **THEN** it invokes the browser script execution tool via its tool context, evaluates compliance, and attaches the resulting report artifact to the test outcome.
