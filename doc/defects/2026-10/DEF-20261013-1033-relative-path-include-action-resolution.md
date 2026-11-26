# [DEF-20261013-1033] Relative Path Inclusion Failure in Dynamic Include Action

- **Status:** `Resolved`
- **Opened:** 2026-10-13 10:33
- **Closed:** 2026-10-13 10:45
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In execution runs such as `Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test` (`run_20261013_101103`), conditional steps containing relative include paths (e.g., `If the product has xpdp configurator features (hint: use java method) then _include: fragments/configure-xpdp-product-steps.steps, else _include: fragments/add-simple-product-to-cart.steps`) failed at runtime with `FileNotFoundException: Classpath resource not found: fragments/add-simple-product-to-cart.steps`. Because the `include` tool call errored, included sub-steps were never expanded or attached to `currentStep.getSubSteps()`. The LLM agent fell back to executing raw browser actions directly on the page without logging or nesting the planned fragment sub-steps. In contrast, in earlier test runs where absolute classpath paths (`tests/stokke/fragments/...`) were used, the steps were successfully resolved and logged as sub-steps.
- **Root Cause:**
  1. `IncludeAction.execute(Action action)` received the relative include path from the LLM tool call argument and passed it directly to `PlaybookParser.parse(path, new ClasspathResourceManager())`. `ClasspathResourceManager.read()` searched only root classpath paths, `ai-playbooks/`, or direct filenames at root. It lacked awareness of the active enclosing playbook file (e.g. `tests/stokke/stokkeOrderPayPalTest.yml`), which was stored in `ExecutionContext` transient data under `playbookFile` and `currentStep.getSourceFile()`.
  2. When resolving nested fragment inclusions in subdirectories (e.g., `fragments/nestedFirst.steps` including `fragments/nestedSecond.steps`), `ClasspathResourceManager.resolveInclude()` performed naive parent directory resolution (`parentDir.resolve(relativePath)`), producing duplicated directory paths (e.g., `com/.../fragments/fragments/nestedSecond.steps`) without collapsing matching directory segments or falling back to the grandparent directory.
- **Detection Gap ("What did we miss?"):** Existing test coverage in `IncludeActionTest` and `YamlPlaybookParserTest` only tested either static compile-time includes parsed from root paths or hardcoded full classpath identifiers (e.g., `com/xceptance/neodymium/ai/integration/fragments/...`). No automated test verified dynamic runtime `IncludeAction` execution with relative fragment paths (`fragments/...`) executed in the context of an enclosing playbook file registered in `ExecutionContext`.
- **Resolution:**
  1. Updated `IncludeAction`:
     - Added context-aware path resolution in `resolveIncludePath`: prioritizes parent directory candidates from the active include stack, `currentStep.getSourceFile()`, `currentStep.getParent().getSourceFile()`, `ExecutionContext` transient data `playbookFile` and `yamlSource`, and Neodymium configuration.
     - Associated parsed steps with the parent step via `subStep.setParent(currentStep)` and `subStep.setSourceFile(resolvedPath)` so that nested child steps maintain their true resource lineage for recursive includes and reporting.
     - Spliced parsed sub-steps directly into `currentStep.getSubSteps()` before pushing them to the execution pipeline.
     - Normalized path tracking in `RUNTIME_INCLUDE_STACK` to guard against circular include recursion.
  2. Updated `ClasspathResourceManager`:
     - Enhanced `resolveInclude()` to check if the direct resolved path exists. If not, it checks whether the parent directory name matches the relative path prefix to collapse redundant folder segments (e.g. `fragments/` inside `.../fragments`), and falls back to resolving against the grandparent directory.
- **Safety Net Added:** Added automated unit test suite in `neodymium-core/src/test/java/org/neodymium/ai/executor/selenide/plugins/IncludeActionTest.java`:
  - `testDirectClasspathInclude`: verifies direct classpath include execution and sub-step attachment.
  - `testRelativeIncludeResolvedViaPlaybookFile`: verifies relative include (`fragments/...`) resolved via active `playbookFile` in execution context.
  - `testNestedRelativeIncludes`: verifies nested relative includes across multiple hierarchy levels.
  - `testCircularInclusionDetection`: verifies circular include recursion throws `IOException`.
  - `testEmptyPathThrowsException`: verifies argument validation for missing or blank target paths.
