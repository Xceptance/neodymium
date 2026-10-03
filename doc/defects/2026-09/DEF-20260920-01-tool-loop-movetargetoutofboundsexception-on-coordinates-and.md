# [DEF-20260920-01] Tool Loop MoveTargetOutOfBoundsException on Coordinates and Malformed Trailing Selector in Hybrid Clicks

- **Status:** Resolved
- **Opened:** 2026-09-20
- **Closed:** 2026-09-20
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`)
- **Scope:** `Framework`
- **Symptom:** AI tool loop failed with `org.openqa.selenium.interactions.MoveTargetOutOfBoundsException: move target out of bounds: (158, 893) is out of bounds of viewport width (1280) and height (857)` when executing `click({"selector": "article[data-ai=\"xcboo7um\"] button, text:", "x": 158, "y": 893})`.
- **Root Cause:**
  1. In `BrowserToolProvider.createClickTool`, Case 1 (coordinates provided) was prioritized over Case 3 (element selector/text resolution). Even though an element selector was provided, raw coordinates routed execution directly to raw Selenium `new Actions(driver).moveToLocation(x, y).click().perform()`, completely bypassing Selenide's auto-scrolling element click logic.
  2. Selenium's `Actions.moveToLocation(x, y)` operates strictly in viewport coordinates without auto-scrolling and throws `MoveTargetOutOfBoundsException` if coordinates are outside `[0, innerWidth] x [0, innerHeight]`.
  3. LLM generated a malformed trailing selector token (`button, text:`), which caused `document.querySelector` to fail with a `DOMException: SyntaxError` and prevented element bounding rect resolution.
- **Detection Gap ("What did we miss?"):** Tests in `BrowserToolProviderStabilityTest` verified coordinate clicks within bounds and basic selector resolution separately, but did not test hybrid calls where an LLM provides both an element selector and out-of-viewport coordinates, nor did they test selector sanitization or coordinate auto-scrolling.
- **Resolution:**
  1. Added `cleanSelector` utility to sanitize hallucinated trailing markers (such as `, text:`, `, text=`, trailing commas).
  2. In `createClickTool`: Prioritized element-based clicking via Selenide (`el.shouldBe(Condition.visible).click()`) when a selector or text is provided and target is not explicitly `coord:...`. If coordinates are within the element's bounding box (`0 <= x <= width`, `0 <= y <= height`), treated them as an in-element offset via `Actions.moveToElement(el, xOffset, yOffset)`.
  3. Added auto-scrolling and viewport clamping in `performSafeCoordinateClick`: if target coordinates are outside the viewport, `window.scrollBy(...)` is called to scroll the target coordinate into the center of the viewport, coordinate offsets are adjusted, clamped to viewport boundaries, and a JavaScript `document.elementFromPoint(x, y).click()` fallback is executed if `Actions.moveToLocation` fails.
  4. Updated `clickBadgeScript` to scroll the badge into view before querying bounding rect.
  5. Updated `createHoverTool` to call `SelenideElementFinder.scrollIntoViewIfNeeded(el)` before hovering.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest` verifying `cleanSelector`, hybrid click with selector prioritizing element click without out-of-bounds error, and coordinate auto-scroll & clamping.
