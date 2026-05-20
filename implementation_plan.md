# Implementation Plan - Implement Robust Numeric Comparisons in AiAssertions

Implement a set of standard numerical comparison methods in `AiAssertions` to enable the AI agent to verify values like subtotals, tax rates, and quantities accurately and robustly across all localized formats (e.g. `14,96 €` vs `0.00`).

## User Review Required

> [!TIP]
> Based on your feedback, we've updated the plan to include a locale-aware version of the numeric parser `parseLocalizedDouble(String text, String locale)`. 
> 
> This version utilizes Java's native `NumberFormat` for the specified locale, with a fallback parser to guarantee absolute reliability. The parameterless `parseLocalizedDouble(String text)` dynamically retrieves the current locale from `Neodymium.configuration().locale()`.

## Proposed Changes

### neodymium-library

#### [MODIFY] [AiAssertions.java](file:///home/rschwietzke/projects/GIT/neodymium-library/src/main/java/com/xceptance/neodymium/ai/util/AiAssertions.java)
- Implement `parseLocalizedDouble(final String text, final String localeStr)`:
  - Clean the input by removing all non-numeric characters except digits, commas, dots, and minus signs.
  - Parse using JDK's standard `NumberFormat` initialized with the resolved `Locale`.
  - Provide a robust pure-regex fallback `parseLocalizedDoubleFallback(String clean)` to resolve the number if the locale-based parser fails or the locale is unknown/empty.
- Implement `parseLocalizedDouble(final String text)`:
  - Dynamically retrieve the current locale from `com.xceptance.neodymium.util.Neodymium.configuration().locale()`.
- Implement helper `parseTwoArguments(final String args, final String methodName)`:
  - Split comma-separated arguments, accounting for potential decimal commas in the first argument (e.g. `14,96 €, 0.00`).
- Implement the following new assertion methods:
  - `assertNumberGreaterThan(final String args)`
  - `assertNumberGreaterThanOrEqual(final String args)`
  - `assertNumberLessThan(final String args)`
  - `assertNumberLessThanOrEqual(final String args)`
  - `assertNumberEqual(final String args)`
- Ensure all public API methods are marked `final` for arguments and variables, use Allman bracing style, and are documented.

#### [NEW] [AiAssertionsTest.java](file:///home/rschwietzke/projects/GIT/neodymium-library/src/test/java/com/xceptance/neodymium/ai/util/AiAssertionsTest.java)
- Add comprehensive JUnit unit tests to cover all methods in `AiAssertions`:
  - `assertPriceGreaterThanZero`
  - `verifyLessOrEqual`
  - `assertNumberGreaterThan`
  - `assertNumberGreaterThanOrEqual`
  - `assertNumberLessThan`
  - `assertNumberLessThanOrEqual`
  - `assertNumberEqual`
  - Direct tests for `parseLocalizedDouble` with explicit locales (`sv-SE`, `de-DE`, `en-US`, `ja-JP`).
- Include test cases validating failure conditions (ensuring they throw `AssertionError`).
- Mark the file with `// AI-generated: Gemini 3.5 Flash`.

## Verification Plan

### Automated Tests
- Run `mvn clean test` in `neodymium-library` to verify that all unit tests, including the new unit tests for `AiAssertions`, pass successfully.
- Build the `neodymium-library` with `mvn clean install` to update the local Maven repository.
- Re-run `TC_CHK_001_GuestCheckout` in the `posters-demo-store` repository to verify that the guest checkout happy path test successfully executes and validates that the subtotal is greater than 0.00 without any failures.
