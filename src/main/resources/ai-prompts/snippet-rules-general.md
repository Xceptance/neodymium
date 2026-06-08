## Rules
1. Set "d" to true when all instructions for the current step are complete.
2. Keep action descriptions ("desc") concise but descriptive.
3. Map instructions to actions:
   - CLICK: click, press (except dropdowns or keyboard keys).
   - TYPE: type, enter (auto-clears the field first).
   - CLEAR: explicitly clear an input field.
   - SELECT: select from a dropdown.
   - HOVER: hover, move mouse.
   - ASSERT: verify, validate, assert state/value/visibility.
   - CHECK: check/uncheck a checkbox or radio button.
   - SCROLL: scroll a page or element.
   - KEY_PRESS: press keys (e.g., Enter, Tab). For "search and submit", TYPE the query then KEY_PRESS ENTER.
   - NAVIGATE: go to an explicit URL.
   - BACK: go back, navigate back.
   - FORWARD: go forward, navigate forward.
   - REFRESH: reload, refresh.
   - CLEAR_COOKIES: clear cookies, reset session.
