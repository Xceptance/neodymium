## MODIFIED Requirements

### Requirement: SWITCH_WINDOW action type
The Neodymium AI engine MUST support tab and window management via native browser tools (`browser_list_tabs`, `browser_switch_tab`, and `browser_close_tab`) and maintain backwards compatibility for the legacy `SWITCH_WINDOW` action type.

#### Scenario: List open browser tabs and windows
- **WHEN** tool `browser_list_tabs` is invoked
- **THEN** the engine SHALL return an array of open window/tab handles with their corresponding titles, URLs, and active status

#### Scenario: Switch to newest window automatically when no parameter is specified
- **WHEN** a `SWITCH_WINDOW` action or `browser_switch_tab` tool call is executed with empty target parameters
- **THEN** the AI engine SHALL find all open window handles, determine which window is not the current active window, and switch the WebDriver focus to the newest window

#### Scenario: Switch to window by index
- **WHEN** `browser_switch_tab` or `SWITCH_WINDOW` is executed specifying a window index (such as `win_1`, `1`, `win_0`, or `0`)
- **THEN** the AI engine SHALL switch the WebDriver focus to the window handle at that index in the list of open window handles

#### Scenario: Switch to window by title
- **WHEN** `browser_switch_tab` or `SWITCH_WINDOW` is executed specifying a window title or URL substring
- **THEN** the AI engine SHALL switch the WebDriver focus to the first window whose title or current URL matches or contains the specified string

#### Scenario: Close active tab and refocus parent
- **WHEN** `browser_close_tab` is invoked
- **THEN** the AI engine SHALL close the current window/tab and switch WebDriver focus back to the primary/parent window handle
