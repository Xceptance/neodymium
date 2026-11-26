# [DEF-20261005-2110] Missing java.util.Collections Import in AiConfiguration

- **Status:** `Resolved`
- **Opened:** 2026-10-05 21:10
- **Closed:** 2026-10-05 21:31
- **Component:** `neodymium-core`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** In `AiConfiguration.java` line 111, the compiler and IDE language server reported an unresolved symbol error: `Collections cannot be resolved` within `getAllProperties()`.
- **Root Cause:** When `getAllProperties()` was added to `AiConfiguration` to return an unmodifiable snapshot of loaded configuration properties via `Collections.unmodifiableMap(all)`, the explicit import `import java.util.Collections;` was omitted from the top-level import declarations.
- **Detection Gap ("What did we miss?"):** Prior test suite runs executed against cached or incremental compiler state for core configuration classes, allowing the missing import to manifest as an active IDE language server error before triggering a full clean build failure.
- **Resolution:** Added `import java.util.Collections;` to `AiConfiguration.java` and added explicit unit test `testGetAllProperties()` in `AiConfigurationTest.java` exercising `getAllProperties()` and asserting unmodifiable map immutability.
- **Safety Net Added:** `AiConfigurationTest.testGetAllProperties` in `neodymium-core/src/test/java/org/neodymium/ai/config/AiConfigurationTest.java`.
