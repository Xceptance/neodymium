## 1. Context Escalation Framework Refactoring

- [x] 1.1 Add `AXTREE` to the `ContextLevel` enum in `ContextLevel.java`.
- [x] 1.2 Refactor `ContextLevel.escalate()` to support: `HINT` -> `AXTREE` -> `LEAN` -> `STANDARD` -> `VISUAL`.
- [x] 1.3 Update `ContextLevelTest.java` to verify the new escalation chain, including standard validations.

## 2. PageAnalyzer Extension for AXTree and CDP Node Resolver

- [x] 2.1 Extend `PageAnalyzer.java` with a version-neutral CDP AXTree parser.
- [x] 2.2 Implement browser compatibility checks using `driver instanceof HasCdp`.
- [x] 2.3 Implement recursive serialization of AXNodes into compact token-optimized text outputs.
- [x] 2.4 Implement element reference stamping inside the browser via `DOM.resolveNode` and `Runtime.callFunctionOn` CDP commands.
- [x] 2.5 Implement graceful fallback to JS `LEAN` context extraction in `captureSimplifiedDom` if CDP is unsupported or throws an error.

## 3. Core Agent Logic Integration

- [x] 3.1 Modify `AiAgent.getInitialContextLevel()` to return `ContextLevel.AXTREE` by default instead of `ContextLevel.LEAN`.
- [x] 3.2 Ensure execution attempts correctly report the active context level in standard attempt logs.

## 4. Verification and Automated TDD Testing

- [x] 4.1 Write robust JUnit integration tests in `PageAnalyzerAXTreeTest` under Allman brace style and aggressive `final` modifiers.
- [x] 4.2 Validate Chrome CDP AXTree extraction on local Chrome instances.
- [x] 4.3 Validate cross-browser fallback behavior on mock drivers or non-Chromium browsers.
