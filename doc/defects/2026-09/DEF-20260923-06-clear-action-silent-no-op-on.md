# [DEF-20260923-06] Clear Action Silent No-Op on Checkbox Elements Leaving Checkboxes Selected

- **Status:** Resolved
- **Opened:** 2026-09-23
- **Closed:** 2026-09-23
- **Component:** `neodymium-core` (`ClearAction`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Executing `Clear #checkbox` on a checked `<input type="checkbox">` executed without error but left the checkbox in a checked/selected state (`isSelected() == true`). Additionally, negative tests expecting an exception failed with `AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown` because ChromeDriver permits `clear()` on any `<input>` element without error.
- **Root Cause:** Both `ClearAction` and `BrowserToolProvider#createClearTool()` delegated directly to `element.clear()`. While W3C WebDriver / ChromeDriver allows `clear()` on `<input type="checkbox">` by resetting its value string attribute, it does not toggle or uncheck the checkbox. Neither component inspected element types to uncheck checkboxes or assert editability constraints on checkboxes.
- **Detection Gap ("What did we miss?"):** No tests previously verified `ClearAction` or `BrowserToolProvider` against checkbox inputs. Tests had only focused on text inputs, textareas, and contenteditable elements.
- **Resolution:** Enhanced `ClearAction` and `createClearTool` in `BrowserToolProvider` to detect checkbox elements (`type="checkbox"` or `role="checkbox"`), assert editability (`element.shouldBe(Condition.editable)`), and if selected (`element.isSelected()`), click the element to uncheck it (preserving idempotency when already unchecked).
- **Safety Net Added:** Added unit test `testClearActionOnCheckbox` in `SelenideActionPluginsTest` and live/recorded integration test `testClearCheckbox` in `ClearIntegrationTest`.
