# Spec Delta

## Purpose

Defines how Neodymium parses, validates, and executes element selectors written in the Playwright selector language (plus a
small set of Neodymium shorthands), so that every selector an agent or playbook may write is either executed with Playwright
semantics or rejected with an actionable error.

## ADDED Requirements

### Requirement: Selectors are parsed structurally
The system SHALL parse selectors with awareness of quoted strings, escape sequences, attribute brackets, and pseudo-class
parentheses, so that characters inside a quoted value, attribute value, or pseudo-class argument never change how the
surrounding selector is interpreted.

#### Scenario: Dot inside an attribute value is not a class
- **WHEN** the selector `header.top-nav-bar a[href="index.html"]:has-text("VÉRLA")` is resolved on a page containing `<header class="top-nav-bar"><a href="index.html">VÉRLA.</a></header>`
- **THEN** the anchor element is matched and no `html` class condition is applied

#### Scenario: Hash inside an attribute value is not an id
- **WHEN** the selector `a[href="#top"]` is resolved
- **THEN** elements with `href="#top"` are matched regardless of their `id`

#### Scenario: Chain separator inside quotes is literal text
- **WHEN** the selector `text="A >> B"` is resolved
- **THEN** it is treated as a single text selector for the literal text `A >> B`

#### Scenario: Shorthand-looking text inside quotes is literal
- **WHEN** the selector `text="Open #xcode"` or `[title="mode:hidden"]` is resolved
- **THEN** the quoted content is matched literally and no automation-ID rewrite or visibility validation is applied to it

### Requirement: Unsupported or invalid selectors are rejected with an actionable error
The system SHALL reject any selector it cannot parse or execute with an invalid-selector error naming the offending token, its
position, and a supported alternative where one exists. It SHALL NOT silently ignore parts of a selector and SHALL NOT retry a
rejected selector as a plain page-text search.

#### Scenario: Unknown pseudo-class
- **WHEN** the selector `button:frobnicate` is resolved
- **THEN** an invalid-selector error is raised that names `:frobnicate` and its position

#### Scenario: Syntax error
- **WHEN** the selector `div[data-x="1"` (unclosed bracket) is resolved
- **THEN** an invalid-selector error is raised describing the unclosed bracket

#### Scenario: No text-search fallback
- **WHEN** an element lookup is performed with a selector that raises an invalid-selector error
- **THEN** the error is surfaced to the caller and no search for the selector string as page text is performed

### Requirement: CSS selectors follow CSS semantics
The system SHALL evaluate standard CSS (type, class, id, attribute selectors with all operators and the `i`/`s` flags,
descendant/child/sibling combinators, selector lists, `:is()`, `:where()`, `:not()`, `:has()`, and structural pseudo-classes)
with the semantics defined by the CSS specification, including when combined with Playwright extensions.

#### Scenario: Attribute suffix operator
- **WHEN** the selector `a[href$=".pdf"]` is resolved
- **THEN** only anchors whose `href` ends with `.pdf` are matched

#### Scenario: nth-child counts all siblings
- **WHEN** the selector `li:first-child:has-text("Item")` is resolved on a list whose first child is an `h3` followed by `li` elements
- **THEN** no element is matched, because the first `li` is not the first child

#### Scenario: Negation excludes only the element itself
- **WHEN** the selector `div:not(span)` is resolved on a page with a `div` that contains a `span`
- **THEN** that `div` is matched

#### Scenario: Selector list combined with extensions
- **WHEN** the selector `a:has-text("Buy"), button:has-text("Buy")` is resolved
- **THEN** the union of matching anchors and buttons is returned in document order

#### Scenario: Unsupported CSS combined with extensions is not dropped
- **WHEN** the selector `button:enabled:has-text("Pay")` is resolved on a page with an enabled and a disabled "Pay" button
- **THEN** only the enabled button is matched

### Requirement: Text matching follows Playwright text semantics
The system SHALL match text against the element's DOM text content (not rendered text), collapsing whitespace including
non-breaking spaces. Unquoted text matches case-insensitive substrings, quoted text matches case-sensitive full strings, and
`/pattern/flags` matches a regular expression. Button and submit inputs SHALL be matched by their value.

#### Scenario: Unquoted text is case-insensitive
- **WHEN** the selector `text=add to cart` is resolved on a page with `<button>ADD TO CART</button>`
- **THEN** the button is matched

#### Scenario: Case-insensitive matching is Unicode-aware
- **WHEN** the selector `:has-text("vérla")` is resolved on a page containing `VÉRLA`
- **THEN** the element containing `VÉRLA` is matched

#### Scenario: Quoted text is exact and case-sensitive
- **WHEN** the selector `text="Log in"` is resolved on a page with `<a>Log in</a>` and `<a>Log in now</a>` and `<a>log in</a>`
- **THEN** only the first anchor is matched

#### Scenario: Regular expression text
- **WHEN** the selector `text=/total:\s*\$\d+/i` is resolved
- **THEN** elements whose text matches the pattern case-insensitively are matched

#### Scenario: CSS text-transform does not affect matching
- **WHEN** the selector `text="Add to cart"` is resolved on `<button style="text-transform:uppercase">Add to cart</button>`
- **THEN** the button is matched

#### Scenario: Non-breaking spaces are normalized
- **WHEN** the selector `text="$ 19.99"` is resolved on an element whose content is `$&nbsp;19.99`
- **THEN** the element is matched

#### Scenario: Submit input matched by value
- **WHEN** the selector `text=Place order` is resolved on `<input type="submit" value="Place order">`
- **THEN** the input is matched

### Requirement: Text engine and text pseudo-classes select the right element depth
The system SHALL resolve `text=`, `:text()`, `:text-is()`, and `:text-matches()` to the smallest elements containing the
matching text, and SHALL resolve `:has-text()` to every element whose text contains a match, including ancestors.

#### Scenario: text= inside a chain selects the innermost element
- **WHEN** the selector `form >> text=Submit` is resolved on `<form><div><button>Submit</button></div></form>`
- **THEN** the button is returned, not the wrapping `div`

#### Scenario: has-text matches ancestors
- **WHEN** the selector `div:has-text("Submit")` is resolved on the same page
- **THEN** the wrapping `div` is matched

### Requirement: Selector engines and chaining follow Playwright semantics
The system SHALL support the `css=`, `xpath=`, `text=`, `id=`, `data-testid=`, `data-test-id=`, `data-test=`, `role=`, `nth=`,
and `visible=` engines, legacy shorthands (leading `//` or `..` is XPath, a leading quote is text), `>>` chaining relative to
the previous part's results, and `*` capture of an intermediate part.

#### Scenario: Chained parts are scoped
- **WHEN** the selector `#cart >> button:has-text("Remove")` is resolved
- **THEN** only Remove buttons inside `#cart` are matched

#### Scenario: Zero-based and negative nth
- **WHEN** the selectors `li >> nth=0` and `li >> nth=-1` are resolved
- **THEN** the first and the last `li` are returned respectively

#### Scenario: Capture an intermediate part
- **WHEN** the selector `*css=article >> text=Hello` is resolved
- **THEN** the `article` elements containing a `Hello` text match are returned

#### Scenario: Legacy XPath shorthand
- **WHEN** the selector `//button[@type='submit']` is resolved
- **THEN** it is evaluated as XPath

### Requirement: Playwright internal engines are supported
The system SHALL support the `internal:` engines emitted by Playwright codegen and locator APIs: `has`, `has-not`, `and`, `or`,
`chain`, `has-text`, `has-not-text`, `text`, `label`, `attr`, `testid`, and `role`, including the `i` (case-insensitive
substring) and `s` (case-sensitive exact) suffixes and regex values. `internal:control=enter-frame` SHALL be rejected.

#### Scenario: has filter
- **WHEN** the selector `li >> internal:has="button:has-text(\"Delete\")"` is resolved
- **THEN** only `li` elements containing a Delete button are matched

#### Scenario: Case flags on text
- **WHEN** the selectors `internal:text="sign in"i` and `internal:text="Sign in"s` are resolved on `<button>Sign In</button>`
- **THEN** the first matches the button and the second does not

#### Scenario: Frame entry is rejected
- **WHEN** a selector containing `internal:control=enter-frame` is resolved
- **THEN** an invalid-selector error is raised explaining that frames must be entered with an explicit frame switch

### Requirement: Role selectors use computed accessibility semantics
The system SHALL resolve `role=` and `internal:role=` using each element's explicit or implicit ARIA role and its accessible
name as computed by the browser, support the `name`, `checked`, `disabled`, `expanded`, `level`, `pressed`, `selected`, and
`include-hidden` options, and exclude hidden elements unless `include-hidden` is set.

#### Scenario: Implicit role and name
- **WHEN** the selector `role=button[name="Checkout"]` is resolved on `<button aria-label="Checkout">🛒</button>`
- **THEN** the button is matched

#### Scenario: Name matching modes
- **WHEN** the selectors `role=link[name="docs"]` and `role=link[name="docs"s]` are resolved on `<a href="/d">Read the Docs</a>`
- **THEN** the first matches and the second does not

#### Scenario: State options
- **WHEN** the selector `role=checkbox[checked=true]` is resolved
- **THEN** only checked checkboxes, native or ARIA, are matched

#### Scenario: Heading level
- **WHEN** the selector `role=heading[level=2]` is resolved
- **THEN** `h2` elements and elements with `role="heading" aria-level="2"` are matched

#### Scenario: Hidden elements excluded by default
- **WHEN** the selector `role=button` is resolved on a page with a button inside an `aria-hidden="true"` container
- **THEN** that button is not matched unless `[include-hidden]` is given

#### Scenario: Same result standalone and chained
- **WHEN** `role=button[name="Pay"]` and `body >> role=button[name="Pay"]` are resolved on the same page
- **THEN** both return the same elements

### Requirement: Visibility filtering is supported
The system SHALL support the `:visible` pseudo-class and the `visible=true` / `visible=false` engine, treating an element as
visible when it has a non-empty bounding box and is not hidden by `visibility: hidden`.

#### Scenario: Visible pseudo-class
- **WHEN** the selector `button:visible:has-text("Close")` is resolved on a page with one visible and one `display:none` Close button
- **THEN** only the visible button is matched

#### Scenario: Visible engine in a chain
- **WHEN** the selector `.modal >> visible=false` is resolved
- **THEN** only hidden `.modal` elements are returned

### Requirement: Layout pseudo-classes are supported
The system SHALL support `:left-of()`, `:right-of()`, `:above()`, `:below()`, and `:near()` with an optional maximum pixel
distance (default 50 px for `:near()`), returning matches ordered by distance to the closest anchor element.

#### Scenario: Input to the right of a label
- **WHEN** the selector `input:right-of(:text("Username"))` is resolved
- **THEN** inputs located to the right of the Username text are returned, closest first

### Requirement: Open shadow DOM is always searched
The system SHALL search open shadow roots for CSS, text, role, and attribute engines without requiring the caller to name shadow
hosts. XPath selectors SHALL NOT pierce shadow roots. Closed shadow roots are not supported.

#### Scenario: CSS pierces open shadow roots
- **WHEN** the selector `button.buy` is resolved on a page where the only matching button lives inside an open shadow root
- **THEN** the button is matched

#### Scenario: Text pierces open shadow roots
- **WHEN** the selector `text=Accept cookies` is resolved and the text lives inside an open shadow root
- **THEN** the containing element is matched

#### Scenario: XPath does not pierce
- **WHEN** the selector `//button[@class='buy']` is resolved on the same page
- **THEN** no element is matched

### Requirement: Results are returned in a deterministic order
The system SHALL return matched elements in document order, except for layout pseudo-classes, which order by distance.
Elements inside shadow roots SHALL be ordered by the document position of their host.

#### Scenario: Document order across a selector list
- **WHEN** the selector `h2, h1` is resolved on a page where an `h1` precedes an `h2`
- **THEN** the `h1` is returned first

### Requirement: Neodymium shorthands remain available in selector positions
The system SHALL keep the automation-ID shorthands (`#xc…`, bare `xc…`, `data-ai=`) and the `testid=`, `placeholder=`, `alt=`,
`title=`, and `label=` engines, applying them only where the parser identifies an id token or engine prefix.

#### Scenario: Automation ID shorthand
- **WHEN** the selector `div > #xc4o48u` is resolved
- **THEN** it matches elements with `data-ai="xc4o48u"` that are children of a `div`

#### Scenario: Label engine
- **WHEN** the selector `label=Email Address` is resolved on a form with `<label for="e">Email Address</label><input id="e">`
- **THEN** the input is matched

### Requirement: All selector consumers share one resolution
The system SHALL resolve selectors identically for element actions, assertions, DOM discovery queries, locator quality checks,
and playbook replay.

#### Scenario: Discovery and action agree
- **WHEN** the agent runs a DOM discovery query and then a click with the same selector `role=button[name="Pay"]`
- **THEN** the click targets the first element reported by the discovery query
