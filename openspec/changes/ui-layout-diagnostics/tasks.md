## 1. Context Routing & Registry

- [ ] 1.1 Update `AiAgent.java`'s `getInitialContextLevel` method to recognize case-insensitive `(layout)` tags, routing them directly to `ContextLevel.VISUAL`.
- [ ] 1.2 Add unit tests for routing logic to verify that `(layout)` tags successfully map to `ContextLevel.VISUAL`.
- [ ] 1.3 Create `LayoutDiagnosticsAction` class under `com.xceptance.neodymium.ai.action.plugins` implementing the AI playbook action interface.
- [ ] 1.4 Register the `LayoutDiagnosticsAction` in the AI action registry, parsing instructions with tags like `(layout)`.
- [ ] 1.5 Implement `getPromptInstructions()` in `LayoutDiagnosticsAction` returning the LLM contract (defining `type: "LAYOUT_DIAGNOSTICS"`, `target`, and `value` serialized expressions for relationships, alignment, overlaps, and container inner distances).
- [ ] 1.6 Declare global configuration properties in `NeodymiumConfiguration.java` for `neodymium.ai.layout.defaultTolerance` (default: `1`) and `neodymium.ai.layout.stabilityTimeout` (default: `2000`).

## 2. Spatial Relationship & Boundary Parsing

- [ ] 2.1 Implement step parsing inside `LayoutDiagnosticsAction` for spatial relationships: `left-of`, `right-of`, `above`, and `below`.
- [ ] 2.2 Implement parenthetical hint parser to extract expected distance, custom tolerance overrides (e.g. `(tolerance: 5px)`), and stability timeout overrides (e.g. `(timeout: 5000ms)`), falling back to global properties loaded from `NeodymiumConfiguration` if unspecified.
- [ ] 2.3 Support standard inline selectors or descriptive target resolution utilizing the pre-existing element mapping service (`ActionExecutor.findElement()`).
- [ ] 2.4 Implement subpixel rendering tolerance comparison logic inside the math evaluation helper.
- [ ] 2.5 Implement support for element overlap detection checks (`does not overlap`).
- [ ] 2.6 Implement support for container inner-boundary checks (`inner-distance`) verifying minimal spacing between container borders and child elements.

## 3. Element Alignment Verification

- [ ] 3.1 Implement alignment assertion parsing for multiple elements matching a selector, supporting alignment boundaries: `top`, `bottom`, `left`, `right`, `vertical-center`, and `horizontal-center`.
- [ ] 3.2 Implement uniform alignment validation math predicate to ensure that target boundary coordinates are equal within the given tolerance.
- [ ] 3.3 Check that the matched element collection contains at least two elements before running uniform alignment, throwing a descriptive `AssertionError` if not met.
- [ ] 3.4 Throw detailed `AssertionError` identifying the misaligned element, its bounding box, the target boundary edge, and the exact offset when alignment checks fail.

## 4. Layout Stability & Execution Engine

- [ ] 4.1 Implement layout stability check that waits until bounding boxes are stable (no changes over a 100ms window) before executing measurements. Limit the wait using the global stability timeout property, with support for step-level overrides (e.g. `(timeout: 5000ms)`).
- [ ] 4.2 Implement single-injection JavaScript execution to perform bounding box query, overlap checks, inner-distance checks, and math evaluation inside the browser to avoid WebDriver network roundtrip overhead.
- [ ] 4.3 Format descriptive `AssertionError` messages showing coordinates, expected/actual distances, element selectors, and visual deviation details.
- [ ] 4.4 Integrate visual overlay highlights drawn on a single temporary absolute-positioned HTML5 `<canvas>` element layered directly over the viewport prior to capturing screenshots for Allure reports.
- [ ] 4.5 Implement the `cleanup()` method on `LayoutDiagnosticsAction` to cleanly remove the temporary HTML5 canvas overlay from the browser DOM after step execution (whether it succeeds or fails).

## 5. Integration and Verification Tests

- [ ] 5.1 Create automated test suite with a variety of HTML test layouts to verify spatial relation and overlap checks (success, expected failures, tolerance limits).
- [ ] 5.2 Write automated tests to verify element alignment checks for different alignment edges (`top`, `vertical-center`, etc.) with customized tolerance parameters.
- [ ] 5.3 Write automated tests to verify container inner-distance checks along different boundaries (`top`, `bottom`, `left`, `right`).
- [ ] 5.4 Verify visual HTML5 canvas overlays are drawn correctly in test reports upon failures and cleaned up successfully afterwards without leaving any DOM residue.
- [ ] 5.5 Verify global configuration loading and step-level overrides (tolerance and stability timeouts) are parsed and applied correctly during execution.



