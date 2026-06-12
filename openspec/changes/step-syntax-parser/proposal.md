## Why

Natural language AI steps contain critical special directives/metadata tagged in brackets, such as `(visual)`, `(glance)`, `(soft)`, `(bug: ...)` and `(hint: ...)`. Currently, these tags are either parsed using simple string contains or regex patterns inside execution flow components, which is error-prone, lacks robust token/syntax verification, doesn't validate syntax validity (e.g., mismatched brackets, empty arguments, malformed hints), and doesn't run upfront. By introducing a dedicated parser and syntax validator (scanner/parser/lexer) before step execution, we can catch syntax errors upfront, validate parameters, and ensure that instructions are clean and well-structured before running.

## What Changes

- Introduce a lexer and parser (a proper scanner and token parser, not regex-based) to extract and validate special directives/tags (e.g., `(visual)`, `(hint: ...)`, `(selector: ...)`, `(glance)`, `(soft)`, `(bug: ...)`) in natural language AI steps.
- Expose an upfront validation/linting API that checks steps for malformed syntax (unbalanced parentheses, missing parameters inside tag directives, invalid tags inside parentheses) and raises clear pre-execution validation errors.
- Integrate the parser with the existing step runner and linter pipeline to replace the ad-hoc regex/contains calls.

## Capabilities

### New Capabilities
- `step-syntax-parser`: A robust step syntax lexer/parser that parses step metadata directives and performs upfront validation.

### Modified Capabilities

## Impact

- Pre-execution validation layer for steps.
- `com.xceptance.neodymium.ai.core.StepLinter` or a new validation component.
- The step execution pipeline in `com.xceptance.neodymium.ai.core.AiAgent`.
