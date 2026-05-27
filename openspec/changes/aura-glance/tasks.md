## 1. Setup and Configuration

- [ ] 1.1 Create `AuraGlanceConfig` class to parse visual consistency configuration properties.
- [ ] 1.2 Add the visual glance configuration keys (e.g., `neodymium.visual.glance.enabled`, `neodymium.visual.glance.captureScreenshots`) to the standard `neodymium.properties` templates.

## 2. DOM Style and Layout Extraction (Aura AST)

- [ ] 2.1 Develop the high-performance layout scanner JavaScript file (`layout-scanner.js`) that captures visible leaf text and interactive control styles and coordinates, returning a compressed JSON "Aura AST" payload.
- [ ] 2.2 Implement `JavaScriptResourceLoader` to load and cache the JS script resource from classpath.
- [ ] 2.3 Create `AuraAstNode` and `AuraPageState` models in Java representing the extracted DOM metadata.
- [ ] 2.4 Implement `AuraAstExtractor` which executes the script and parses the JSON response into `AuraPageState` alongside a viewport screenshot.

## 3. Background WebDriver Capture

- [ ] 3.1 Create `AuraGlanceListener` implementing the WebDriver listener interface (hooked in background via the native `AuraCaptureListener`).
- [ ] 3.2 Register the listener in `AuraCaptureListener` depending on config settings.
- [ ] 3.3 Add asynchronous processing capability to process the captured screenshots and Aura AST on a separate thread to maintain sub-30ms test execution latency.

## 4. Multimodal AI Glance Auditor

- [ ] 4.1 Implement `AuraGlanceAuditor` that prepares the multimodal prompts (Screenshot PNG + Aura AST layout text) and queries Gemini.
- [ ] 4.2 Define the structured JSON schema for visual anomalies returned by the LLM (type, severity, description, coordinates).
- [ ] 4.3 Implement local dHash visual playbook caching (`VisualPlaybookCache`) that stores approved baselines and computes local Hamming distance to bypass Gemini calls offline for identical screens.
- [ ] 4.4 Update `AiAgent.java` step router to recognize the case-insensitive `(glance)` tag and route steps directly to `ContextLevel.VISUAL`.
- [ ] 4.5 Refactor `AiAgent.java` tag compiler to separate `OPTIONAL_TAG_PATTERN` and `SOFT_TAG_PATTERN`.
- [ ] 4.6 Implement decoupled handlers in `AiAgent.executeStep()`:
  - If `(optional)`: skip silently without warning, logging `status: "SKIPPED"` (or `"PASSED"`).
  - If `(soft)`: record anomalies with `status: "WARNING"`, write a warning log to developer console, and safely continue execution.
- [ ] 4.7 Implement the on-demand API `NeodymiumVisualGlancer.assertVisualConsistency()` throwing `AssertionError` upon rule or heuristic violations (unless a soft gate is active).

## 5. Report Integration & Testing

- [ ] 5.1 Implement the report aggregator compiling all captured visual states and anomalies into standard `run-info.json` and `<testCase>.json` traces.
- [ ] 5.2 Create the visual lab HTML/JS comparison dashboard components inside the Aura Server Thymeleaf UI.
- [ ] 5.3 Write unit tests for Aura AST extraction, dHash Hamming distance logic, and tag decoupling (soft vs optional step execution).
- [ ] 5.4 Create integration tests running a mock browser session to verify complete end-to-end background capture, linter auditing, and soft warning gates.
