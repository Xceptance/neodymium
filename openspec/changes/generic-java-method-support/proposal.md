## Why

The current `JAVA_METHOD` AI action plugin only supports calling void methods that accept 0 or 1 String parameters. This requires test developers to write verbose custom boilerplate void assertion wrappers on their test classes or utility classes for every minor utility function they want to call (e.g., string formatting, localized price parsing). Allowing more generic method execution, multi-argument support, and variable assignment directly within the AI steps reduces boilerplate and makes AI test cases more compact, clean, and reusable.

## What Changes

- **Multi-Argument Support**: Reflectively resolve and invoke methods accepting multiple parameters by parsing argument values as JSON arrays or splitting comma-separated lists.
- **Variable Assignment**: Introduce assignment syntax (e.g. `java: ${myVar} = formatString("Welcome %s", "World")`) to capture the return value of Java methods and dynamically inject it into the active test data context.
- **JShell Expression Evaluation**: Securely evaluate arbitrary lightweight Java expressions (e.g. `java: Math.min(10.5, 20.3)`) directly through JDK's JShell engine, using strict sandboxing and character allowlisting.

## Capabilities

### New Capabilities
- `generic-java-method-support`: Covers multi-parameter Java method resolution, return value capture/assignment to context variables, and JShell-based expression evaluation.

### Modified Capabilities

## Impact

- **`JavaMethodAction.java`**: Regex pattern needs updating to support variable assignment syntax; parsing logic must handle comma-separated and JSON parameter arrays; invocation logic must support storing return values in Neodymium context.
- **`AiAssertions.java`**: Evaluation engine could be reused/exposed to support dynamic expressions.
- **Neodymium Test Runner / Context**: Ensure variables written by the action are properly resolved in subsequent steps.
