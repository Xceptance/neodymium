## Context

Natural language AI steps in Neodymium use inline tags in parentheses to specify execution directives, locator hints, expected failures, timeouts, and soft assertions. Currently, these tags are parsed using ad-hoc regular expressions or basic string checks (`contains`). This has several limitations:
1. CSS or XPath selectors in `(hint: ...)` or `(selector: ...)` can contain nested parentheses, which regular expressions cannot parse reliably.
2. Malformed syntax (such as unbalanced parentheses or empty arguments) is not caught upfront, leading to runtime failures or incorrect execution context.
3. No unified pre-execution validator exists to lint all steps in a playbook before executing the test suite.

## Goals / Non-Goals

**Goals:**
- Implement a robust, handcrafted upfront parser (Lexer/Parser) in Java to parse step syntax and extract metadata tags without relying on regular expressions for parenthetical balancing.
- Correctly handle nested parentheses within parameter values (e.g. selectors).
- Provide a validation API that checks for syntax issues (unbalanced parentheses, empty tag parameters, malformed timeouts, etc.) before test execution.
- Integrate the parser with `StepLinter` and the step runner/agent pipeline.

**Non-Goals:**
- Parsing or validating the semantic meaning of the natural language text outside the parenthesized tags.
- Changing the behavior or meaning of the existing tags.

## Decisions

### 1. Parsing Strategy: Stack-Based/Nesting-Aware Single-Pass Scanner
We will implement a custom scanner that processes the step string character by character. 
- When a `(` is encountered, it tracks the nesting depth.
- It scans forward until the matching `)` is found (where nesting depth returns to 0).
- This ensures that selector expressions like `(hint: div:not(.active))` are parsed as a single directive containing `div:not(.active)` rather than being cut short at the first `)`.
- If the end of the string is reached with a nesting depth > 0, it raises a syntax exception.

*Alternatives considered:*
- *Regex matching:* Rejected because standard Java regex cannot easily match arbitrary nested structures without complex and unmaintainable patterns.
- *Parser generators (like ANTLR):* Rejected as it introduces heavy dependencies and complexity for a relatively simple grammar. A hand-written scanner is fast, lightweight, and easy to maintain.

### 2. Lexical Validation Rules
For each extracted parenthesized group:
- Trim the contents and check if it starts with a known directive prefix (case-insensitively): `visual`, `layout`, `soft`, `optional`, `bug`, `timeout`, `hint`, `selector`.
- If it starts with one of these prefixes, it is validated:
  - If a colon is expected (e.g., `hint:`, `selector:`, `timeout:`, and optionally `bug:`), it checks that the value is present and non-empty.
  - For `timeout`, it verifies that the parameter value is a valid integer optionally followed by `ms` or `s`.
  - For `bug`, it allows either `(bug)` or `(bug: ID)`.
- If a parenthesis does not start with a known prefix, it is treated as regular parenthesized text (e.g. comments/descriptions), unless it is close to a known prefix (which can be warned as a typo).

### 3. Integration with Existing Pipeline
- Introduce `com.xceptance.neodymium.ai.core.parser.StepParser` and related classes.
- Update `StepLinter` to use the new parser for upfront validation and syntax warning detection.
- Update `AiAgent` to use the new parser for stripping tags and extracting step metadata (like `isVisual`, `bugId`, etc.).

## Risks / Trade-offs

- [Risk] Arbitrary parentheses in natural language step text (e.g. comments) might be mistaken for directives if they happen to start with a tag name.
  - [Mitigation] The parser will only treat parenthesized expressions as directives if they match known tag names exactly (case-insensitively). Other parentheses are ignored and treated as normal text.
