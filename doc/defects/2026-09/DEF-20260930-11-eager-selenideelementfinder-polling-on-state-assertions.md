# [DEF-20260930-11] Eager `SelenideElementFinder` Polling on State Assertions and Blocking `interactable` Timeouts on Click

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In test executions (such as `AddToCartTest.executeAddToCart`), each `assert_element_state` call took 5.1s–5.3s (~76.5s across 14 assertions) even when elements were already visible, and `click` calls took 11.6s–22.8s on animated/overlay elements (~76.8s across 5 clicks).
- **Root Cause:**
  1. `BrowserToolProvider` resolved assertion targets via `SelenideElementFinder.findElement`, which executes an eager, custom 5,000ms polling loop requiring visible candidate matching rather than utilizing Selenide's native lazy element proxy (`Selenide.$(...)`). When asserting `hidden` or `absent` states, or when elements failed strict pre-visibility filters, it polled until the full 5,000ms timeout before falling back.
  2. `BrowserToolProvider.executeElementClick` asserted `el.shouldBe(Condition.interactable)` prior to click. When overlay wrappers, banners, or CSS animations were present, Selenide blocked for its condition timeout (5,000ms–10,000ms), generated attachment failure dumps to disk, and only then fell back to JavaScript click.
- **Detection Gap ("What did we miss?"):** Unit tests executed against mock WebDrivers or simple static fixtures where elements were instantly interactable and assertions were not timed against live multi-second timeouts.
- **Resolution:**
  1. Added `resolveLazyElement` to `BrowserToolProvider` using standard Selenide `Selenide.$(LocatorResolver.resolveLocator(selector))` for assertions (`assert_element_state`, `assert_attribute`, `assert_text`), delegating condition polling directly to Selenide's `shouldBe`.
  2. Refactored `executeElementClick` to attempt native `el.click()` directly after `shouldBe(Condition.visible)`, catching `ElementClickInterceptedException` / `ElementNotInteractableException` immediately and falling back to JS click in < 50ms without waiting out a multi-second interactable timeout.
- **Safety Net Added:** Unit tests verifying rapid assertion completion and immediate JS click fallback in `BrowserToolsTest`.
