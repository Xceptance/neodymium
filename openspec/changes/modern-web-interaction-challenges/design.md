## Context

Neodymium Aura AI currently runs an agent tool loop (`AgentToolLoopStep`) communicating with an LLM via native tool calling. Actions are executed through `BrowserToolProvider`, which interacts with the browser via Selenide and WebDriver, while `PageAnalyzer` extracts simplified DOM representations for model perception.

Modern web architectures employ dynamic patterns (multi-window handles, hidden file inputs inside styled dropzones, unmounted virtual DOM nodes, native browser dialogs, drag gestures, detached body portals, and contenteditable nodes) that fall outside basic DOM query/click primitives. See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Establish 7 isolated, standalone HTML sandbox challenge pages under `AuraGlanceTest/shop/sandbox/` reproducing each pattern.
- Implement native browser tools in `BrowserToolProvider` for multi-tab management, file uploads, native alerts, and coordinate drag actions.
- Enhance `AgentToolLoopStep` to handle unmounted virtual DOM discovery, detached body portals, and native alert presence.
- Support typing into `<div contenteditable="true">` elements in `browser_type`.
- Write corresponding live (`@Tag("LiveLlm")`) and mock integration test classes for each challenge.

**Non-Goals:**
- Solving live third-party CAPTCHAs (reCAPTCHA v3 / Cloudflare Turnstile).
- Automating native OS desktop windows outside the browser boundary.
- Mobile touch multi-finger gestures (e.g. pinch-to-zoom).

## Decisions

### 1. Explicit Tab/Window Tools Over Automatic Window Switching
- **Choice**: Implement explicit tools `browser_list_tabs`, `browser_switch_tab`, and `browser_close_tab`.
- **Rationale**: Automatic window switching can be unpredictable when background tracking popups or advertisement tabs open. Giving the model explicit visibility into open tabs (`index`, `title`, `url`, `isActive`) allows deliberate multi-tab workflows and reliable returns to parent windows.
- **Alternative Considered**: Auto-switching to newest window handle on every action. Rejected because it breaks when a page opens secondary utility windows or downloads.

### 2. Bypass OS File Dialogs via Ancestor/Descendant File Input Resolution
- **Choice**: `browser_upload_file(selector, filePath)` resolves the real `<input type="file">` directly (searching children, siblings, or nearby DOM trees of a dropzone), making it interactable if needed, and calls `input.sendKeys(resolvedPath)`.
- **Rationale**: Clicking a dropzone triggers an OS-level file chooser dialog which completely blocks WebDriver and the browser JS engine. Setting the file path directly on the input element avoids OS blocking while correctly firing HTML5 `change` and `drop` events.
- **Alternative Considered**: Robot/Sikuli OS click automation. Rejected as platform-dependent, headless-incompatible, and brittle.

### 3. Proactive Alert Detection in Agent Loop
- **Choice**: Check for native browser alert presence in `AgentToolLoopStep` before attempting DOM capture or screenshots.
- **Rationale**: When `window.alert()` or `window.confirm()` is active, any call to execute JavaScript or take a screenshot throws `UnhandledAlertException`. Detecting the alert upfront injects an alert notice to the agent, enabling it to invoke `browser_handle_alert(action="accept"|"dismiss")`.
- **Alternative Considered**: Setting WebDriver capability `UNHANDLED_PROMPT_BEHAVIOUR=ACCEPT`. Rejected because tests must explicitly verify alert messages and test dismiss paths.

### 4. Continuous Actions Sequence for Range Sliders & Drag
- **Choice**: Implement `browser_drag` using Selenium's `Actions` class (`moveToElement -> clickAndHold -> moveByOffset -> release`).
- **Rationale**: Custom dual-thumb sliders listen for `mousedown`, `mousemove` on `document`, and `mouseup`. Discrete clicks cannot move continuous slider handles to target values.
- **Alternative Considered**: Direct JS property manipulation. Rejected because custom web component sliders often encapsulate internal state that only responds to real mouse/pointer events.

### 5. Selection-Aware Typing for Contenteditable
- **Choice**: In `browser_type`, check if the element has `contenteditable="true"` or `isContentEditable`. If so, focus the element, place the selection caret at the end of the content, and use `sendKeys` / simulated input events.
- **Rationale**: Selenide's `.val(...)` attempts to set the `value` property, which does not exist on `<div>` or `<p>` elements.

## Risks / Trade-offs

- **[Risk] Agent clicks dropzone before calling upload tool** → *Mitigation*: Ensure sandbox dropzone labels clearly indicate upload instructions, and PESAP prompt identifies dropzone elements as file upload targets.
- **[Risk] Virtualized feeds cause infinite scroll loops** → *Mitigation*: Impose a strict maximum scroll count (e.g. 5 scroll attempts) before declaring target element unfindable.
- **[Risk] Detached portals pollute DOM size** → *Mitigation*: `PageAnalyzer` prunes invisible portal wrappers while preserving active floating poppers attached to `document.body`.
