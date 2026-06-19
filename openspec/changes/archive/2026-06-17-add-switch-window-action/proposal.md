## Why

Neodymium AI currently lacks a direct action type to switch browser windows or tabs during automated test execution. In complex web applications (e.g., payment redirections, third-party authentication, or popup dialogs), actions might open a new window or tab. Without the ability to switch the browser context to the new window/tab, the AI agent is unable to interact with or verify elements on the newly opened pages, leading to test failures.

## What Changes

- Introduce a new core AI action type: `SWITCH_WINDOW`.
- Add a new action plugin `SwitchWindowAction` that handles switching browser focus to a target window (using window index, title, handle name, or switching to the newest window when no target is provided).
- Support direct parsing of "Switch to window..." natural language instructions without invoking the LLM (zero latency/token cost).
- Add natural language regex configuration for window switching to `config/ai.properties`.

## Capabilities

### New Capabilities

- `switch-window`: Support switching active focus to another window or tab based on index, title, handle name, or switching automatically to the newest window when no target is specified.

### Modified Capabilities

None.

## Impact

- **Core AI Engine**: Registers a new action type in `ActionRegistry`.
- **Configuration**: Extends `config/ai.properties` with a new regex property.
- **Selenium/Selenide integration**: Leverages WebDriver's window handles to focus the driver on target windows/tabs.
