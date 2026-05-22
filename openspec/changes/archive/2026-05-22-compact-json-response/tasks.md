## 1. Setup and Preparation

- [x] 1.1 Locate and review prompt files under `src/main/resources/ai-prompts/`
- [x] 1.2 Identify the exact test files verifying parser behavior

## 2. Parser Compact-Format Modification

- [x] 2.1 Update `ActionParser.java` to parse compact keys for root elements (`s`, `a`, `d`, `e`, `r`, `st`, `tc`)
- [x] 2.2 Update `ActionParser.java` to parse compact keys for action-level elements (`t`, `tg`, `v`, `d`, `ed`, `c`, `th`, `el`, `ad`, `ec`, `r`)
- [x] 2.3 Update `ActionParser.java` to pretty-print valid JSON response in debug logs
- [x] 2.4 Update `AiPromptGenerator.java` to parse and handle compact keys (`r`, `cs`, `dl`, `oia`, `a`/`action`) from LLM JSON responses

## 3. Prompt Template Updates

- [x] 3.1 Update `system-prompt.txt` to specify compact keys in its schema instructions and examples, including minified JSON instructions
- [x] 3.2 Update `system-healing-prompt.txt` to specify compact keys in its schema instructions and examples, including minified JSON instructions
- [x] 3.3 Update `system-exploration-prompt.txt` to specify compact keys in its schema instructions and examples, including minified JSON instructions
- [x] 3.4 Update `v2-system-exploration-prompt.txt` to specify compact keys in its schema instructions and examples, including minified JSON instructions
- [x] 3.5 Update `v2-extraction-prompt.txt` (checked and confirmed optimized)

## 4. Verification and Testing

- [x] 4.1 Write dedicated unit tests in `ActionParserEscalateTest.java` or `ActionParserTest.java` targeting compact key deserialization
- [x] 4.2 Run the entire test suite `mvn clean test` and verify that all tests pass seamlessly
