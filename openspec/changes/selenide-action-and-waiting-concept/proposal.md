## Why

When executing UI actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`, `ASSERT`, `HOVER`, `SCROLL`, `WAIT`), real-world web applications frequently render asynchronous DOM updates, CSS transitions, AJAX requests, and dynamic single-page application (SPA) re-renders. If action executor plugins interact with low-level `WebElement` instances directly or omit proper condition-driven waiting, tests become susceptible to `StaleElementReferenceException`, element-not-interactable errors, or race conditions on slow-rendering components.

A comprehensive review and hardening of all Selenide action plugins (`org.neodymium.ai.executor.selenide.plugins`) ensures that every action consistently leverages Selenide's declarative condition-driven polling paradigm (`Condition.visible`, `Condition.interactable`, `Condition.editable`, `Condition.enabled`, `Condition.text`), re-evaluates stale elements dynamically, and provides precise timeout and diagnostic information upon failure.

## What Changes

- **Selenide Action Plugin Audit**: Systematically audit all action plugins (`ClickAction`, `TypeAction`, `ClearAction`, `SelectAction`, `AssertAction`, `HoverAction`, `ScrollAction`, `WaitAction`, `KeyPressAction`) in `org.neodymium.ai.executor.selenide.plugins`.
- **Standardized Condition-Driven Waiting**:
  - Enforce explicit, appropriate Selenide pre-conditions before executing operations (e.g. `$(selector).shouldBe(visible, interactable).click()`, `$(selector).shouldBe(visible, editable).setValue(...)`, `$(selector).shouldBe(visible, enabled).selectOption(...)`).
- **Stale Element & Dynamic DOM Resilience**: Ensure no cached raw `WebElement` references bypass Selenide's proxy re-query mechanism during retries and animations.
- **Scroll & Viewport Visibility Handling**: Standardize smart auto-scrolling into view (`scrollIntoView(true)`) when elements are rendered offscreen or behind sticky headers.
- **Diagnostic Timeouts & Error Messages**: Improve exception reporting when Selenide wait conditions time out, highlighting the target selector, expected condition, actual element state, and elapsed duration.

## Capabilities

### New Capabilities
- `selenide-action-and-waiting-concept`: Covers the standardized condition-driven waiting, dynamic element resilience, auto-scroll handling, and hardened execution across all Selenide action executor plugins.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components**: `org.neodymium.ai.executor.selenide.plugins` (`ClickAction`, `TypeAction`, `ClearAction`, `SelectAction`, `AssertAction`, `HoverAction`, `ScrollAction`, `WaitAction`, `KeyPressAction`, `SelenideTargetExecutor`).
- **Dependencies**: Uses Selenide 7.x native condition APIs (`Condition.visible`, `Condition.interactable`, `Condition.editable`, `Condition.enabled`, `Condition.text`, `Condition.attribute`).
- **Compatibility**: 100% backward-compatible; enhances execution stability across all live recording and replay tests.
