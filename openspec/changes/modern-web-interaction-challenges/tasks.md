## 1. Multi-Tab & Window Popups

- [x] 1.1 Create `AuraGlanceTest/shop/sandbox/multi-window.html` and `popup.html` with `target="_blank"` links, `window.open` popups, and dynamic status updates
- [x] 1.2 Implement `browser_list_tabs`, `browser_switch_tab`, and `browser_close_tab` native tools in `BrowserToolProvider` and verify unit test passes in `BrowserToolsTest`
- [x] 1.3 Add active window handle synchronization in `AgentToolLoopStep` so subsequent DOM captures target the switched window
- [x] 1.4 Create `MultiWindowSandboxLiveTest` and `MultiWindowSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 2. File Upload & Styled Dropzones

- [ ] 2.1 Create `AuraGlanceTest/shop/sandbox/file-upload.html` with styled dropzone containers enclosing hidden `<input type="file">` elements
- [ ] 2.2 Implement `browser_upload_file(selector, filePath)` in `BrowserToolProvider` with smart ancestor/descendant file input resolution
- [ ] 2.3 Add file upload support to `BrowserToolsTest` and verify upload schema and execution
- [ ] 2.4 Create `FileUploadSandboxLiveTest` and `FileUploadSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 3. Dynamic Virtualized Lists & Infinite Feeds

- [ ] 3.1 Create `AuraGlanceTest/shop/sandbox/virtualized-list.html` where items are dynamically mounted and unmounted based on scroll position
- [ ] 3.2 Enhance `AgentToolLoopStep` with iterative scroll-and-scan perception when targeted elements are unmounted from the DOM
- [ ] 3.3 Create `VirtualizedListSandboxLiveTest` and `VirtualizedListSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 4. Native Browser Alerts & Dialogs

- [ ] 4.1 Create `AuraGlanceTest/shop/sandbox/native-alerts.html` with triggers for `window.alert`, `window.confirm`, and `window.prompt`
- [ ] 4.2 Implement `browser_handle_alert(action, promptText)` in `BrowserToolProvider` supporting accept, dismiss, and prompt entry
- [ ] 4.3 Add proactive alert detection in `AgentToolLoopStep` to notify the agent and avoid `UnhandledAlertException` during DOM capture
- [ ] 4.4 Create `NativeAlertSandboxLiveTest` and `NativeAlertSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 5. Dual-Thumb Range Sliders & Drag Gestures

- [ ] 5.1 Create `AuraGlanceTest/shop/sandbox/range-slider.html` featuring a custom dual-thumb price slider and reorderable drag list
- [ ] 5.2 Implement `browser_drag(selector, xOffset, yOffset)` and `browser_drag_to(source, target)` in `BrowserToolProvider` using Selenium `Actions`
- [ ] 5.3 Create `RangeSliderSandboxLiveTest` and `RangeSliderSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 6. Autocomplete & Debounced Typeahead with Floating Portals

- [ ] 6.1 Create `AuraGlanceTest/shop/sandbox/autocomplete.html` with debounced search-as-you-type rendering into detached `<body>` portals
- [ ] 6.2 Ensure `PageAnalyzer` captures floating portal containers in DOM snapshots and verify keyboard navigation in `BrowserToolProvider`
- [ ] 6.3 Create `AutocompleteSandboxLiveTest` and `AutocompleteSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes

## 7. Rich Text & Contenteditable Editors

- [ ] 7.1 Create `AuraGlanceTest/shop/sandbox/rich-editor.html` containing formatted `<div contenteditable="true">` editors
- [ ] 7.2 Update `BrowserToolProvider.browser_type` to detect `isContentEditable`, establish caret focus, and emulate typing without `.val()`
- [ ] 7.3 Create `RichEditorSandboxLiveTest` and `RichEditorSandboxMockTest` and verify `mvn test-compile -pl neodymium-core` passes
