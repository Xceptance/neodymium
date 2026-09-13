## Why

The Neodymium Aura AI test framework currently excels at static DOM structures, basic interactions, and synthetic delays (shadow DOM, iframes, SVG icons, hover chains, table sorting, canvas clicks, and timing delays). However, real-world modern web applications frequently employ interaction patterns that completely break AI test agents and automation drivers:
1. Multi-tab workflows and window popups (`target="_blank"`, `window.open`) desynchronize WebDriver from the active page.
2. File uploads are hidden behind styled dropzone elements, triggering native OS file dialogs that freeze test execution.
3. Virtualized feeds (AG Grid, virtual DOM windowing) unmount offscreen items, making them invisible in initial DOM snapshots.
4. Native browser dialogs (`alert`, `confirm`, `prompt`) suspend browser execution and throw unhandled alert exceptions.
5. Dual-thumb range sliders and reorderable lists rely on coordinate drag-and-drop sequences rather than text typing or discrete clicking.
6. Autocomplete typeaheads render detached option portals into `document.body` after debounce delays.
7. Rich text editors (`contenteditable="true"`) ignore standard input manipulation.

To make Neodymium Aura resilient across enterprise web applications, we must establish isolated sandbox test cases for each of these 7 challenges, stress-test the framework to identify failure modes, and implement the necessary browser tools and agent adaptations.

## What Changes

- **Multi-Tab & Window Popups**: Implement `browser_list_tabs`, `browser_switch_tab`, and `browser_close_tab` native browser tools in `BrowserToolProvider`. Add automatic tab detection and focus synchronization in `AgentToolLoopStep`. Build sandbox `sandbox/multi-window.html`.
- **File Upload & Styled Dropzones**: Implement `browser_upload_file(selector, filePath)` in `BrowserToolProvider` capable of locating hidden `<input type="file">` elements within or near styled dropzones, preventing native OS dialog blocking. Build sandbox `sandbox/file-upload.html`.
- **Infinite Virtualized Feeds**: Implement iterative viewport scrolling, DOM discovery, and settlement in `AgentToolLoopStep` for elements unmounted outside the viewport. Build sandbox `sandbox/virtualized-list.html`.
- **Native Browser Dialogs**: Implement `browser_handle_alert(action, promptText)` in `BrowserToolProvider` and add proactive alert detection in the agent loop to prevent `UnhandledAlertException`. Build sandbox `sandbox/native-alerts.html`.
- **Dual-Thumb Range Sliders & Drag Gestures**: Implement `browser_drag(selector, xOffset, yOffset)` and `browser_drag_to(sourceSelector, targetSelector)` in `BrowserToolProvider`. Build sandbox `sandbox/range-slider.html`.
- **Autocomplete & Floating Portals**: Enhance `AgentToolLoopStep` and `PageAnalyzer` to detect debounced typeahead mutations and detached portal containers attached to `document.body`. Build sandbox `sandbox/autocomplete.html`.
- **Rich Text & Contenteditable Editors**: Enhance `BrowserToolProvider.browser_type` to detect `contenteditable="true"` containers and emulate focus, selection range, and typing events. Build sandbox `sandbox/rich-editor.html`.
- **Live Integration Tests**: Add corresponding live/mock test classes in `org.neodymium.ai.integration.sandbox.live` and `org.neodymium.ai.integration.sandbox.mock` for each challenge.

## Capabilities

### New Capabilities
- `modern-web-interactions`: Specifications for handling file uploads (`browser_upload_file`), native browser dialogs (`browser_handle_alert`), drag-and-drop / range sliders (`browser_drag`), virtualized list discovery, debounced autocomplete portals, and contenteditable editors.

### Modified Capabilities
- `switch-window`: Migrate legacy `SWITCH_WINDOW` action specification to unified native browser tools (`browser_list_tabs`, `browser_switch_tab`, `browser_close_tab`) with multi-turn window tracking.

## Impact

- `neodymium-core/src/main/java/org/neodymium/ai/tool/browser/BrowserToolProvider.java`: Added tools for tabs, alerts, drag-and-drop, and file uploads.
- `neodymium-core/src/main/java/org/neodymium/ai/pipeline/steps/AgentToolLoopStep.java`: Multi-turn window tracking, alert interception, and virtualized feed scanning.
- `neodymium-core/src/test/resources/ai-test-pages/AuraGlanceTest/shop/sandbox/`: 7 new or updated HTML sandbox challenge pages.
- `neodymium-core/src/test/java/org/neodymium/ai/integration/sandbox/`: 7 new live integration tests and corresponding mock tests.
