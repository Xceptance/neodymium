## Context

See [proposal.md](proposal.md) for motivation. Neodymium delegates low-level browser operations to Selenide (`SelenideTargetExecutor` and individual action plugins under `org.neodymium.ai.executor.selenide.plugins`). Selenide provides rich, lazy-evaluated proxy elements (`SelenideElement`) and polling condition checks (`shouldBe`, `shouldHave`). However, inconsistent use of conditions across action plugins (e.g. relying on standard Selenium clicks vs Selenide condition chaining, or inconsistent timeout overrides) can expose tests to timing-dependent flakes.

## Goals / Non-Goals

**Goals:**
- Unify the waiting and interaction protocol across all action plugins using standard Selenide conditions (`Condition.visible`, `Condition.interactable`, `Condition.editable`, `Condition.enabled`, `Condition.text`, `Condition.exactText`, `Condition.attribute`).
- Ensure complete immunity against `StaleElementReferenceException` during rapid SPA state transitions.
- Standardize automatic scrolling into view for elements hidden under sticky navigation or below the fold.
- Provide consistent timeout and retry boundaries configured via `neodymium.ai.action.timeout`.

**Non-Goals:**
- Replace Selenide with raw Selenium WebDriver.
- Add hardcoded fixed `Thread.sleep()` pauses (all waiting must be condition-driven).

## Decisions

### 1. Lazy Proxy Resolution vs Raw WebElement
- **Decision:** Never extract or store `WebElement` instances across action lifecycles. Always perform operations directly on `SelenideElement` proxies (e.g. `$(selector)`).
- **Rationale:** Selenide's proxy automatically re-executes `findElement` on each condition check or interaction, making it inherently resilient to DOM replacement.

### 2. Standardized Action Pre-Condition Matrix
- **Decision:** Each action plugin defines its required pre-conditions:
  - `ClickAction`: `shouldBe(Condition.visible).shouldBe(Condition.interactable)`
  - `TypeAction`: `shouldBe(Condition.visible).shouldBe(Condition.editable)`
  - `ClearAction`: `shouldBe(Condition.visible).shouldBe(Condition.editable)`
  - `SelectAction`: `shouldBe(Condition.visible).shouldBe(Condition.enabled)`
  - `HoverAction`: `shouldBe(Condition.visible)`
  - `AssertAction`: Uses Selenide's declarative `shouldHave(Condition)` or `shouldBe(Condition)`.
- **Rationale:** Prevents attempting interactions on disabled, hidden, or animating elements before they are ready.

### 3. Smart Scrolling Integration
- **Decision:** When an element is visible in DOM but positioned outside the current viewport or occluded, use Selenide's `scrollIntoView("{behavior: 'smooth', block: 'center'}")` prior to interaction.
- **Rationale:** Ensures clean clickability without triggering browser occlusion errors.

## Risks / Trade-offs

- **[Risk: Slow Failures on Missing Elements]** → Mitigation: Use configurable action timeout (`neodymium.ai.action.timeout`, default 4000ms) to ensure fast failures when elements truly do not exist.
