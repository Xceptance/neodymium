# [DEF-20261113-1630] Settings Modal Form Binding Drop in AuraTestSettingsController

- **Status:** `Resolved`
- **Opened:** 2026-11-13 16:30
- **Closed:** 2026-11-13 16:32
- **Component:** `aura-manager`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Submitting configuration updates (e.g., `neodymium.ai.apiKey` or dev override keys) in the Aura Manager Settings dialog displayed a "Settings saved successfully!" banner, but changes were never saved to property files and `dev-neodymium.properties` was rewritten as an empty header. Subsequent test executions failed with `Gemini API key is missing`.
- **Root Cause:** `AuraTestSettingsController.saveSettings` declared `@RequestBody(required = false) final MultiValueMap<String, String> formData`. Because HTMX form submissions transmit `application/x-www-form-urlencoded` payloads and standard Servlet container parameter parsing consumes the request stream, Spring MVC's request body message converters resolved `formData` as an empty map (or null). Consequently, `filePropertyUpdates` was empty (skipping file saves), and `saveDevNeoProperties` was passed an empty list, clearing all existing overrides.
- **Detection Gap:** Existing unit tests in `AuraTestSettingsControllerTest` covered modal attribute rendering, MCP sessions, and self-tests, but completely lacked tests for POST requests to `/api/settings/save`.
- **Resolution:** Replaced `@RequestBody(required = false)` with `@RequestParam(required = false)` on `saveSettings(...)` in `AuraTestSettingsController.java`, enabling standard URL-encoded form data binding across property files and dev overrides.
- **Safety Net Added:** Added `@Test` `AuraTestSettingsControllerTest.testSaveSettingsPersistsPropertyFileAndDevNeoOverrides` and `testSaveSettingsHandlesNullOrEmptyFormDataGracefully`.
