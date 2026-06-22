## ADDED Requirements

### Requirement: Java method multi-parameter support
The JAVA_METHOD action SHALL support reflectively resolving and invoking public Java methods with multiple String parameters by parsing argument values as JSON arrays or splitting them by commas.

#### Scenario: Parse and invoke multi-parameter method
- **WHEN** the step instruction `java: assertNumbersEqual("[\"10.00\", \"10.00\"]")` is executed
- **THEN** the system parses the parameters as a JSON array and invokes the resolved `assertNumbersEqual(String)` method using reflection.

### Requirement: Java method return value assignment
The JAVA_METHOD action SHALL capture the return value of reflectively executed Java methods and assign it to the specified variable in the active Neodymium test data context when using the assignment prefix syntax.

#### Scenario: Variable assignment from Java method call
- **WHEN** the step instruction `java: ${myPrice} = parseLocalizedBigDecimal("14,96 €")` is executed
- **THEN** the system executes the method, retrieves the returned value, and stores `"14.96"` in the test data context under the key `myPrice`.

### Requirement: JShell dynamic expression evaluation
The system SHALL support dynamic evaluation of arbitrary mathematical or basic Java expressions via the JShell execution engine, strictly validating the input expression against an allowlist of safe characters (digits, dots, operators, parentheses, spaces) to prevent code injection.

#### Scenario: Evaluate math expression via JShell
- **WHEN** the step instruction `java: Math.min(10.5, 20.3)` is executed
- **THEN** the system validates the safety of the expression and evaluates it using JShell, returning the evaluated result.
