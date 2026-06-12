## 1. Parser Implementation

- [ ] 1.1 Create `com.xceptance.neodymium.ai.core.parser.StepParser` class with a stack-based, nesting-aware parser.
- [ ] 1.2 Implement parsing and extraction of tags: `visual`, `glance`, `soft`, `optional`, `bug`, `timeout`, `hint`, `selector`.
- [ ] 1.3 Add syntax validation rules for unbalanced parentheses, empty tag parameters, and malformed timeouts.

## 2. Integration and Updates

- [ ] 2.1 Update `com.xceptance.neodymium.ai.core.StepLinter` to run step syntax validation upfront.
- [ ] 2.2 Update `com.xceptance.neodymium.ai.core.AiAgent` to use `StepParser` for extracting tags and stripping them from step instructions.

## 3. Testing and Verification

- [ ] 3.1 Create unit tests in `com.xceptance.neodymium.ai.core.StepParserTest` to verify all valid and invalid syntax scenarios.
- [ ] 3.2 Run the full suite of AI tests to ensure no regressions in tag handling.
