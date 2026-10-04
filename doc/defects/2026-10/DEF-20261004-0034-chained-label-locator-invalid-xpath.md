# [DEF-20261004-0034] Chained `label=` locator produced invalid XPath

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:20
- **Closed:** 2026-10-04 00:34
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** A `label=` segment after `>>` (for example `#scope >> label=Email`) threw `InvalidSelectorException` / "A location step was expected following the '/' or '//' token" when evaluated. The standalone `label=Email` worked.
- **Root Cause:** `LocatorResolver.resolveChainedLocator` reused the standalone `label=` XPath, which is a union of three location paths (`label[@for]`, wrapping `label`, `aria-label`), stripped its leading `//`, and wrapped it as `//( a | b | c )`. A parenthesised union cannot follow `//` in XPath 1.0, so every chained `label=` was syntactically invalid.
- **Detection Gap ("What did we miss?"):** `LocatorResolverTest` asserts on locator strings (`toString().contains(...)`) and never evaluates the XPath, so a string that looks plausible but does not parse passes. No test combined `>>` with `label=`.
- **Resolution:** The chain branch now builds one predicate on the descendant axis, `//*[((self::input or select or textarea or button) and (@id=//label[...]/@for or ancestor::label[...])) or @aria-label=E]`, which has the same three associations and stays inside the scope of the previous segment. The standalone `label=` output is unchanged, so existing string assertions still hold.
- **Safety Net Added:** `LocatorResolverTest.testChainedLabelProducesValidXpathAndMatchesAllLabelStyles` and `LocatorResolverTest.testChainedLabelCanBeFollowedByFurtherSegments`. They evaluate the generated XPath with the JDK XPath 1.0 engine against a small XML fixture, including a same-named input outside the scope that must not match. Both failed with the XPath error before the fix.
- **Not Fixed (observed while probing):** unquoted `>> text=Submit` matches the outermost ancestor as well as the element (the quoted exact form is fine), and a `>>` inside quoted text is split by the naive regex in `resolveChainedLocator`.
