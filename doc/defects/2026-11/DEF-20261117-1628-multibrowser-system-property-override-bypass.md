# [DEF-20261117-1628] MultibrowserConfiguration ignores JVM system properties and archetype hardcodes headless browser profiles

- **Status:** `Resolved`
- **Opened:** 2026-11-17 16:28
- **Closed:** 2026-11-17 16:33
- **Component:** `neodymium-classic`, `neodymium-archetype`
- **Scope:** `Framework & Config/Environment`
- **Symptom:**
  - Running `mvn test -Dbrowserprofile.global.headless=true` (or `false`) has no effect on browser execution.
  - Archetype generated projects run headless by default with no CLI mechanism to disable or enable headless mode, preventing first-time users from seeing the browser window and preventing CI from toggling headless mode via CLI without editing files.
- **Root Cause:**
  1. `MultibrowserConfiguration` parses `./config/browser.properties`, `./config/dev-browser.properties`, and an optional temp file using `FileInputStream`. It never inspected `System.getProperties()` for `browserprofile.*` entries.
  2. `neodymium-archetype/config/browser.properties` explicitly configured `browserprofile.global.headless = true`, `browserprofile.Chrome_1500x1000.headless = true`, and `browserprofile.Firefox_1500x1000.headless = true`. Because profile-level properties take precedence over global properties in `BrowserConfigurationMapper`, even if global headless were parsed, the profile-level setting would suppress it.
- **Detection Gap ("What did we miss?"):**
  - Existing tests for `MultibrowserConfiguration` (such as `EnvironmentAndBrowserConfigurationTest`) verified properties loaded from temporary files via `writeMapToPropertiesFile`, but never asserted that JVM arguments (`-Dbrowserprofile...`) passed via command line or `System.setProperty` take precedence over file-based configuration.
  - Archetype integration tests (`archetype-it`) executed `test-compile` without running browser test execution in headless/headed modes.
- **Resolution:**
  1. In `MultibrowserConfiguration.java`, introduced `loadSystemProperties` to iterate over `System.getProperties()` and overlay any properties starting with `browserprofile.` onto `browserProfileProperties` and `testEnvironmentProperties`.
  2. In `neodymium-archetype/src/main/resources/archetype-resources/config/browser.properties`, set `browserprofile.global.headless = false` as default and removed the redundant profile-level `headless = true` declarations so individual profiles inherit the global setting.
  3. In `neodymium-archetype/src/main/resources/archetype-resources/README.md`, updated documentation to explain that desktop tests run headed by default and documented `mvn test -Dbrowserprofile.global.headless=true` for CI and headless environments.
- **Safety Net Added:**
  - Created `MultibrowserSystemPropertyOverrideTest` in `neodymium-classic/src/test/java/com/xceptance/neodymium/junit5/tests/` asserting that `System.setProperty("browserprofile.global.headless", "true")`, `System.setProperty("browserprofile.Chrome_Test.headless", "true")`, and `System.setProperty("browserprofile.testEnvironment.myEnv.url", ...)` correctly override values loaded from `browser.properties`.
