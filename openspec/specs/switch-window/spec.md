# switch-window Specification

## Purpose
TBD - created by archiving change add-switch-window-action. Update Purpose after archive.
## Requirements
### Requirement: SWITCH_WINDOW action type
The Neodymium AI engine MUST support a browser action type named `SWITCH_WINDOW` that switches the focus of the WebDriver/browser instance to another window or tab.

#### Scenario: Switch to newest window automatically when no parameter is specified
- **WHEN** a `SWITCH_WINDOW` action is executed with empty `target` and `value` fields
- **THEN** the AI engine SHALL find all open window handles, determine which window is not the current active window, and switch the WebDriver's focus to the newest window

#### Scenario: Switch to window by index
- **WHEN** a `SWITCH_WINDOW` action is executed with a `target` or `value` specifying a window index (such as `win_1`, `1`, `win_0`, or `0`)
- **THEN** the AI engine SHALL switch the WebDriver's focus to the window handle at that index in the list of open window handles

#### Scenario: Switch to window by title
- **WHEN** a `SWITCH_WINDOW` action is executed with a `target` or `value` specifying a window title
- **THEN** the AI engine SHALL switch the WebDriver's focus to the window whose title matches or contains the specified title


