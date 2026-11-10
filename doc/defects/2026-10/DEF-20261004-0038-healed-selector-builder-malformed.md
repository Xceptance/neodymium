# [DEF-20261004-0038] Healed locator could be malformed or point at the wrong element

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:15
- **Closed:** 2026-10-04 01:12
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When vector healing picked a candidate, `PlaybookToolReplayer.resolveSelectorForCandidate` could produce a selector that was syntactically broken (an unescaped `"` or `\` in an `aria-label`, `name` or `data-testid` value, non-standard CSS IDs like `123btn` or `login:btn`, or Tailwind utility class tokens like `md:flex` or `w-1/2`) or one that was not an element locator at all (a `coord: x,y` string used as a selector).
- **Root Cause:** The method interpolated attribute values into `[attr="..."]` without escaping, added an `[aria-label="<accessibleName>"]` fallback that is weaker than the primary attributes, returned raw `#id` even for IDs requiring CSS escaping (digits, colons, slashes), joined CSS class names without filtering out invalid CSS identifier characters (colons, slashes), and returned a `coord:` string when only a position was known.
- **Detection Gap ("What did we miss?"):** The method was `private`, so no test could call it, and the replay tests only used candidates with plain ASCII ids. Because the selector is a string handed to the driver later, a malformed value only fails at the next lookup.
- **Resolution:** The method is now package-private static with Javadoc. The `aria-label` and `coord:` fallbacks were removed. Attribute values are escaped for backslash, double quote and newline. Non-standard CSS IDs are formatted as `tag[id="..."]` with escaped attribute values instead of invalid `#id`. Classes containing invalid CSS identifier characters (e.g. Tailwind `md:flex`, `w-1/2`) are filtered out. Text in `:has-text(...)` is escaped via `escapeAttributeValue`. The method returns `null` when it cannot build a locator, and the caller skips the heal on `null`.
- **Safety Net Added:** `HealedSelectorResolutionTest` (8 tests covering accessible name exclusion, aria-label preservation, coord exclusion, attribute escaping, ID preference, non-standard ID attribute selector formatting, Tailwind class filtering, and `:has-text` escaping); `PlaybookToolReplayTest` (9 tests) continues to pass.
