# [DEF-20261020-0705] Custom Utility Tools Not Registered in ToolRegistry During Agent Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-20 07:05
- **Closed:** 2026-10-20 07:58
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Issue:** [GitLab #83](https://gitlab.xceptance.de/testautomation/test-automation-team-tasks/-/work_items/83)
- **Symptom:** During automated AI playbook execution (e.g. `tests/ifolor/TGifts.yaml`), custom methods annotated with `@Tool` and configured in `neodymium.ai.agent.methods.classes` (such as `IfolorEditorHelper.uploadImage`) were never registered in `ToolRegistry`. Consequently, the LLM agent could not discover or invoke the custom tool, falling back to standard `upload_file` (which failed on custom drag-and-drop dropzones without standard `<input type='file'>`) until timing out after 60 seconds.
- **Root Cause:** While `JavaToolFactory.createToolsFromClass(...)` existed in `neodymium-core`, `ToolRegistry` and `BrowserToolProvider` never read `neodymium.ai.agent.methods.classes` or invoked `JavaToolFactory` during registry population. Custom `@Tool` methods were completely omitted from runtime `ToolRegistry` instances. In addition, parameter mapping required exact case matches and failed if the LLM emitted snake_case arguments or single-argument objects under alternative key names.
- **Detection Gap ("What did we miss?"):** `JavaToolFactoryTest` tested tool instantiation in complete isolation with mock classes, but no integration test verified that configured classes in `ai.properties` / `neodymium.properties` were loaded and registered into `ToolRegistry` or `BrowserToolProvider` when initializing the execution pipeline.
- **Resolution:**
  1. Implemented `ToolRegistry.registerConfiguredCustomTools(ToolRegistry)` to dynamically read `neodymium.ai.agent.methods.classes`, reflectively load annotated classes, create `JavaTool` instances via `JavaToolFactory`, and register them into the registry.
  2. Added call to `ToolRegistry.registerConfiguredCustomTools(registry)` inside `BrowserToolProvider.registerBrowserTools(registry)`.
  3. Enhanced `ToolRegistry.getTool(...)` to support case-insensitive and snake_case vs camelCase name resolution.
  4. Enhanced `JavaTool.execute(...)` with case-insensitive and snake_case parameter name matching, plus single-argument fallback mapping when a single-parameter method receives a single JSON argument.
- **Safety Net Added:** Added `CustomToolRegistryConfigurationTest` with:
  - `testCustomToolsAutoRegisteredFromConfiguration`: Verifies automatic registration from `neodymium.ai.agent.methods.classes`, snake_case lookup (`upload_image`), and execution.
  - `testResilientSingleArgumentParameterResolution`: Verifies execution when single argument key differs from method parameter name.
