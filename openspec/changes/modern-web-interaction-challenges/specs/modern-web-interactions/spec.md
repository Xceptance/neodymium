## Purpose

Provides native browser tools and autonomous agent perception strategies for challenging modern web patterns including file dropzones, native dialogs, coordinate drag gestures, virtualized feeds, detached autocomplete portals, and contenteditable editors.

## ADDED Requirements

### Requirement: File upload and styled dropzone interaction
The Neodymium AI engine SHALL support uploading local files through tool `browser_upload_file(selector, filePath)` and automatically resolve associated hidden `<input type="file">` elements within or surrounding styled dropzone containers without triggering blocking OS-level file dialogs.

#### Scenario: Upload file via hidden input element
- **WHEN** tool `browser_upload_file` is executed with a file path targeting a visible or hidden `<input type="file">`
- **THEN** the engine SHALL set the file path on the input element and dispatch change events

#### Scenario: Upload file via styled dropzone wrapper
- **WHEN** tool `browser_upload_file` targets a styled container element (such as a dropzone `div`) that nests or references a hidden file input
- **THEN** the engine SHALL discover the associated file input and upload the file without clicking to open the OS file chooser

### Requirement: Native browser alert and dialog handling
The Neodymium AI engine SHALL detect native browser dialogs (`window.alert`, `window.confirm`, `window.prompt`) and provide the `browser_handle_alert` tool to inspect and resolve them before subsequent DOM queries fail.

#### Scenario: Accept native confirmation dialog
- **WHEN** tool `browser_handle_alert` is invoked with action `accept`
- **THEN** the engine SHALL accept the active modal browser alert dialog and return its message content

#### Scenario: Dismiss native confirmation dialog
- **WHEN** tool `browser_handle_alert` is invoked with action `dismiss`
- **THEN** the engine SHALL dismiss the active modal browser alert dialog

#### Scenario: Respond to native prompt dialog
- **WHEN** tool `browser_handle_alert` is invoked with action `accept` and non-empty `promptText`
- **THEN** the engine SHALL enter the text into the prompt dialog and accept it

### Requirement: Range slider and coordinate drag interactions
The Neodymium AI engine SHALL provide tool `browser_drag(selector, xOffset, yOffset)` to execute mouse drag-and-drop sequences across dual-thumb sliders, scrubbers, and reorderable elements.

#### Scenario: Drag slider thumb by horizontal pixel offset
- **WHEN** tool `browser_drag` is executed with target slider thumb selector and positive or negative `xOffset`
- **THEN** the engine SHALL perform a continuous mouse drag sequence from the element center to the offset position and dispatch mouse events

#### Scenario: Drag source element to target drop target
- **WHEN** tool `browser_drag` is executed with source and target selectors
- **THEN** the engine SHALL drag the source element and release it directly over the target element

### Requirement: Virtualized list and infinite feed discovery
The agent tool loop SHALL support autonomous discovery of items in virtualized lists and infinite feeds where offscreen items do not exist in the DOM snapshot.

#### Scenario: Iterative scroll and discovery of unmounted items
- **WHEN** an instruction targets an item not present in the current DOM snapshot of a scrollable feed
- **THEN** the agent SHALL scroll down, observe newly mounted items, and repeat until the target element is rendered

### Requirement: Debounced autocomplete with detached portals
The agent tool loop and browser tools SHALL support typing into search typeahead fields and interacting with suggestion options rendered into detached portals appended directly to `document.body`.

#### Scenario: Select suggestion from detached body portal
- **WHEN** text is typed into a search input triggering an asynchronous debounced suggestion dropdown
- **THEN** the engine SHALL capture the portal elements in subsequent DOM snapshots and allow selecting suggestions via click or keyboard navigation

### Requirement: Rich text and contenteditable element interaction
The `browser_type` tool and action execution engine SHALL detect `<div contenteditable="true">` elements and properly focus, set caret selection, and type text without relying on `<input>` value setters.

#### Scenario: Type formatted text into contenteditable container
- **WHEN** `browser_type` targets an element having `contenteditable="true"`
- **THEN** the engine SHALL focus the container, place the cursor, and input the text while triggering appropriate input and change events
