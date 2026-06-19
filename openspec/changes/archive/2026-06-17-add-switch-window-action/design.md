## Context

Neodymium AI's action execution loop (`ActionExecutor`) translates parsed actions into Selenide/WebDriver commands. Currently, the framework does not provide an action type to switch the active browser window/tab. To support multi-window verification and interactions, we need a standard `SWITCH_WINDOW` action that allows switching the active WebDriver window focus.

## Goals / Non-Goals

**Goals:**
- Add `SWITCH_WINDOW` action type to focus WebDriver on a different window or tab.
- Support index-based switching (e.g. `win_1` or `1` for the second window).
- Support title-based switching.
- Support automatic fallback to the newest/other window when no target or value is specified.
- Support direct parsing of natural language instructions matching the regex configuration.

**Non-Goals:**
- Automatically switching windows upon new window creation without an explicit instruction.

## Decisions

### 1. Add `SwitchWindowAction` as a core action plugin
- **Rationale**: Implementing the new action as an `AiActionPlugin` subclass (`com.xceptance.neodymium.ai.action.plugins.SwitchWindowAction`) fits the existing modular registry architecture.
- **Alternatives Considered**: Modifying `ActionExecutor` directly to intercept certain instructions. This was rejected to maintain clean separation of concerns and keep `ActionExecutor` generic.

### 2. Support index-based, title-based, and newest-window switching
- **Rationale**: 
  - Dynamic window handle IDs change per run, so the AI cannot hardcode them.
  - Index-based switching (e.g. `win_0` is first, `win_1` is second) is stable because handle order is usually stable.
  - Title-based switching is human-readable and matches typical Selenium testing practices.
  - Automatic fallback (switching to the next/newest window) makes natural instructions like "Switch to the new window" work out-of-the-box.

## Risks / Trade-offs

- **[Risk]** Dynamic/newest window logic might pick the wrong window if more than two windows are open simultaneously.
  - **[Mitigation]** Explicit window indexes (e.g. `win_2`) or titles can be used to resolve ambiguity in complex flows.
