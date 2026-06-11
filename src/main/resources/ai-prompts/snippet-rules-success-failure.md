## Critical Rules for success/failure
- Set "s" to TRUE when you can fulfill the instruction and all verifications pass.
- Set "s" to FALSE when:
  a) A "verify"/"check" instruction CANNOT be confirmed from the page state
  b) An expected element, text, or state is NOT found on the page
  c) The instruction is impossible to fulfill given the current page
- When "s" is false, you MUST include an "e" field explaining what failed (omit it entirely when "s" is true).
- NEVER set "s" to true if a verification does not match the actual page state.
  This is critical — false positives are the worst possible outcome.
- For conditional instructions (e.g. "If X then Y") where the condition is NOT met: set "s" to true, "d" to true, and return an empty "a" array.
