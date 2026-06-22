## Context

The current `JavaMethodAction` is limited to reflecting methods with 0 or 1 `String` parameters and discards all return values. Extending this to support multi-parameter methods, return value variable assignment, and safe JShell expression evaluation will significantly reduce test boilerplate.

## Goals / Non-Goals

**Goals:**
- Update `JavaMethodAction` parsing to support variable assignment syntax (`java: ${myVar} = ...`).
- Resolve and invoke public methods with multiple `String` parameters.
- Provide a secure character-allowlisted JShell evaluation pathway for inline expressions.

**Non-Goals:**
- Supporting arbitrary class loading or execution of non-classpath code.
- Support for complex multiline Java scripting within step instructions.

## Decisions

### 1. Parsing and Resolution of Multi-Parameter Methods
- **Decision**: Update `JavaMethodAction`'s regex and parameter splitting to extract list of arguments by parsing the parameter block as a JSON array (e.g. `["arg1", "arg2"]`) or splitting by commas.
- **Rationale**: Reuses the parsing pattern established in `AiAssertions` for two-argument assertions.

### 2. Variable Assignment Syntax and Storage
- **Decision**: Introduce a pattern match for assignments: `java:\s*(?:\$\{([a-zA-Z0-9_.]+)\}\s*=)?\s*([a-zA-Z_][a-zA-Z0-9_]*)...`
- **Rationale**: Explicitly maps to Neodymium test data context `Neodymium.getData().put(varName, result)`.

### 3. Dynamic JShell Evaluation Guardrails
- **Decision**: Reuse the strict regex filter `^[0-9.+\\-*/()\\s]+$` before passing expression to JShell.
- **Rationale**: Prevents execution of arbitrary code (like `System.exit` or filesystem access) while allowing complex math and basic expressions.

## Risks / Trade-offs

- **Risk**: String conversion loss when mapping arbitrary return objects.
- **Mitigation**: Invoke `.toString()` on returned values before storing in Neodymium context.
- **Risk**: Remote Code Execution (RCE) via JShell.
- **Mitigation**: Enforce the regex allowlist on all dynamic expressions evaluated in JShell.
