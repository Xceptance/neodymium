# [DEF-20261113-1645] Thymeleaf Preprocessing Syntax Collision Corrupts Settings Modal Form Input Names

- **Status:** `Resolved`
- **Opened:** 2026-11-13 16:45
- **Closed:** 2026-11-13 16:50
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `UI & Presentation` / `Framework & Config/Environment`
- **Symptom:** Saving settings via the Aura Manager Settings modal did not persist property updates into their target configuration files (such as `neodymium.ai.apiKey` in `ai.properties`), even when Spring MVC form binding succeeded.
- **Root Cause:** In Thymeleaf templates, double underscores (`__${expression}__`) denote template pre-processing. In `settings-modal.html`, form inputs were declared as `th:name="'prop__' + ${group.fileName} + '__' + ${entry.key}"`. Thymeleaf interpreted `'prop__'` and `'__'` as opening and closing delimiters for pre-processing, stripping the underscores and concatenating expressions, yielding client HTML `name="prop + ${group.fileName} + ${entry.key}"` (e.g. `name="prop + ai.properties + neodymium.ai.apiKey"`). Because `AuraTestSettingsController` specifically filtered for parameter names starting with `prop__` (or `prop--`), all property file entries were completely ignored.
- **Detection Gap:** `AuraTestSettingsControllerTest` mocked/simulated incoming parameter maps directly without evaluating Thymeleaf template rendering of `settings-modal.html`.
- **Resolution:** Replaced the double-underscore prefix and separator with hyphen delimiters `th:name="|prop--${group.fileName}--${entry.key}|"` in `settings-modal.html` across `aura-manager` and `neodymium-core`. Updated `AuraTestSettingsController.java` to support both `prop--` and `prop__` prefixes and splitters.
- **Safety Net Added:** Added unit test `testSaveSettingsSupportsHyphenDelimiter` in `AuraTestSettingsControllerTest`.
