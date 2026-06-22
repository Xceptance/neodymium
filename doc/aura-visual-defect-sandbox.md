# Neodymium Aura AI: Aura AI Test Sandbox & Multi-Port Testing Environment

This document provides extensive architectural details, layout mappings, and usage instructions for the self-contained local testing sandbox ("Aura AI Test Sandbox") engineered specifically for **Neodymium Aura AI** (including the Observational Visual Auditor **Aura Glance**). 

---

## 1. Overview & Purpose

Headless mock page setups or static JVM unit tests cannot adequately validate visual AI layout scanners, perceptual hash comparison layers, secure cross-origin boundaries, or multimodal image audits. These advanced features require:
1. **Real Browser Rendering**: A real browser engine (Chrome, Firefox, Safari) compiling CSS grids, computing element dimensions, and executing scripts.
2. **Secure HTTPS & HTTP Contexts**: Validating that visual checkers, cookies, and selectors operate correctly under both encrypted (TLS) and non-encrypted environments.
3. **Toggleable Visual Anomalies**: Programmatic control to dynamically inject visual bugs (overlapping tags, clipped descriptions, alignment shift, poor contrast) so that automated suites can assert failures or record warning tags.
4. **Dynamic Data Parameterization**: Checking that dynamic variables (`${userFullName}`) inside test instructions are correctly resolved in SUT prompts.
5. **Double-Stage Replays**: Confirming that subsequent test runs matching baseline approved screens (dHash) bypass external LLM network latency entirely, executing offline in `< 10ms`.

The **Aura Test Suite Hub** fulfills these constraints 100% locally with zero external hosted server dependencies.

---

## 2. Directory Layout & Structure

All sandbox sub-applications and shared assets reside directly inside the library's test resources classpath:

```
src/test/resources/
├── keystore.p12                    # Self-signed PKCS12 localhost SSL certificate
├── ai-test-pages/
│   └── AuraGlanceTest/             # Unified Sandbox Root
│       ├── shared/
│       │   ├── style.css           # Core dark-mode glassmorphic design theme
│       │   ├── controls.css        # Anomaly control sliding drawer stylesheet
│       │   ├── controls.js         # Dynamic controls drawer injector script
│       │   └── placeholder.png     # Binary 1x1 raster image asset
│       ├── dashboard/              # Sub-App 1: SaaS Administration System
│       │   ├── index.html          # Dynamic SVG metrics charts & iframe host
│       │   └── iframe-content.html # Nested sub-document inside the iframe
│       ├── shop/                   # Sub-App 2: E-Commerce Storefront
│       │   ├── index.html          # Product listings, image scales, and flex grids
│       │   ├── homepage-perfect.html # Apparel Shop (Perfect HTML/CSS code quality)
│       │   ├── homepage-normal.html  # Apparel Shop (Average HTML/CSS code quality)
│       │   ├── homepage-bad.html     # Apparel Shop (Worst HTML/CSS code quality)
│       │   ├── plp-perfect.html      # Category Listing (Perfect HTML/CSS code quality)
│       │   ├── plp-normal.html       # Category Listing (Average HTML/CSS code quality)
│       │   ├── plp-bad.html          # Category Listing (Worst HTML/CSS code quality)
│       │   ├── forms.html          # Registrations, validation error, price calculators
│       │   ├── escalation.html     # AXTREE to LEAN context level escalation interactive challenge
│       │   ├── visual-escalation.html # STANDARD to VISUAL context level escalation interactive challenge
│       │   └── sandbox/            # Focused scenario playground
│       │       ├── svg-icons.html      # SVG icon-only naming challenge
│       │       ├── canvas-click.html   # Pixel coordinate click challenge
│       │       ├── shadow-dom.html     # Shadow DOM piercing challenge
│       │       ├── click-intercept.html# Overlay z-index block challenge
│       │       ├── dynamic-reveal.html # Dynamic DOM timing transition challenge
│       │       ├── hover-chain.html    # Nested hover hover chain challenge
│       │       ├── table-sorting.html  # AJAX sorting settling lag challenge
│       │       ├── scroll-list.html    # Viewport scroll overflow list challenge
│       │       ├── floating-labels.html# Text overlay visual collision challenge
│       │       ├── cross-origin-iframe.html # Context frame switching challenge
│       │       ├── mock-payment-iframe.html # Iframe page source
│       │       └── mock-oauth-login.html    # Authentication redirect challenge
│       └── a11y/                   # Sub-App 3: Accessibility Test Grounds
│           └── index.html          # Low contrast overrides, text clippings
```

---

## 3. Multi-Port HTTP & HTTPS Server Setup

The embedded test runner server (`EmbeddedHtmlServer.java`) spins up **two parallel server engines** listening on dynamic, OS-allocated random free ports to eliminate network collisions:

1. **HTTP Server (`http://localhost:<port>`)**: Plain HTTP instance.
2. **HTTPS Server (`https://localhost:<httpsPort>`)**: Encrypted SSL instance.

### Self-Signed Certificate Configuration
The secure server loads a self-signed PKCS12 certificate keystore (`keystore.p12`) from the classpath.
* **Keystore alias**: `localhost`
* **Keystore password**: `changeit`
* **MIME Mappings**: Upgraded to correctly serve standard browser media files (`.png`, `.jpg`, `.jpeg`, `.gif`, `.svg`) with their appropriate content types.

### Browser Configuration Integration
To prevent Chrome from blocking the self-signed certificate with secure interstitial warnings, Neodymium is configured via `config/browser.properties` to launch Chrome arguments:
```properties
browserprofile.Chrome_1024x768.arguments = -ignore-certificate-errors
```
This forces browser executors to safely bypass SSL warnings on `localhost`.

---

## 4. The Interactive Aura Chaos Drawer

Every sub-app page globally includes the shared drawer scripts:
```html
<link rel="stylesheet" href="../shared/style.css">
<link rel="stylesheet" href="../shared/controls.css">
<script src="../shared/controls.js"></script>
```

When DOM loading completes, `controls.js` dynamically builds and injects a floatable glassmorphic gear-wheel trigger button in the bottom right corner of the page viewport. Clicking it slides open the **Aura Chaos Panel** allowing programmatic or manual toggles to inject:

| Toggle Name | Element Triggered | CSS/DOM Shift Behavior | Targeted Testing Capability |
| :--- | :--- | :--- | :--- |
| **Inject Element Overlap** | `.overlap-target-btn` | Shifts target absolute over primary heading text tags. | Immediate Visual Assertion failures throwing `AssertionError` |
| **Inject Low Contrast** | `body` | Overrides structural text color tokens to near-invisible grays. | Multimodal contrast checks & accessibility audits |
| **Inject Clipped Text** | `.clipped-container` | Constraints card heights to `60px` with `overflow: hidden`. | Multimodal text truncation audits |
| **Inject Mobile Overlap** | `.columns-grid` | Force-squeezes grids horizontally, disabling wrapping rules. | Viewport responsive overlap tests |
| **Inject Layout Shift** | `.misaligned-element` | Offsets grid rows with asymmetrical margins and padding. | Alignment drift visual checking |
| **Show Promo Banner** | `#promo-banner-container` | Displays or hides a promotional banner on top of headers. | Conditional optional clicks bypass verification |

---

## 5. Writing & Running Automated Test Cases

The sandbox supports two complete test suites:

### 1. Unified Visual Integrity (`AuraGlanceTest.java`)
Validates core requirements (visual overlaps, text clippings, nested iframes, custom served images, conditional optional banners, AXTREE-to-LEAN/LEAN-to-STANDARD escalations, and STANDARD-to-VISUAL multimodal escalations).

### 2. Multi-Dimensional Matrix (`AuraFeatureMatrixTest.java`)
Validates advanced framework parameterization features:

* **Test Multiplication**: Uses repeatable annotations to duplicate and run identical steps across varying viewports:
  ```java
  @Browser("Chrome_1024x768")
  @Browser("Chrome_1500x1000")
  ```
* **Dynamic Data Injections**: Injects test parameters from `AuraFeatureMatrixTest.json` dynamically into natural language steps:
  ```java
  // AuraFeatureMatrixTest.json entry: "userFullName": "Alice Cooper"
  Neodymium.ai().execute("Type '${userFullName}' into the 'Full Name' field.");
  ```
* **AI Graphic audits**: Tells the auditor to evaluate pixel color details rather than simple box bounds:
  ```java
  Neodymium.ai().execute("Assert that the 'Aura Neon Gradient Poster' card displays an image showcasing deep ultraviolet and cyan shades.");
  ```
* **Offline Replay Checks**: Runs with the active LLM on the first execution to save approved baselines. The second run replays the step, matches the `dHash` baselines, and completes **instantly and offline** in `< 10ms`:
  ```java
  // Stage 1 (Establishes baseline dHash)
  Neodymium.ai().execute("Observe page visual consistency (visual)");
  
  // Stage 2 (Bypasses network, hits local cache cache)
  Neodymium.ai().execute("Observe page visual consistency (visual)");
  ```
* **Legacy Compatibility**: Replays legacy, older playbook instruction sets cleanly under the Aura observational visual auditor.

---

## 6. Commands Reference

### Compile Test Suites
Verify that your changes compile successfully without any JVM class errors:
```bash
mvn test-compile
```

### Run All AI Test Cases
Execute the JUnit 5 test classes:
```bash
mvn test -Pjunit-5 -Dtest=com.xceptance.neodymium.ai.Aura*Test
```

---

## 7. Context Level Escalation Challenges

The sandbox contains dedicated test pages designed to test the self-healing and context level escalation mechanisms of the Aura AI framework.

### A. Shop Escalation Challenge (`shop/escalation.html`)

This page contains elements that test standard text-based escalations:

#### 1. AXTREE to LEAN Challenge (AXTree-Hidden Targets)
- **The Elements**:
  1. **Custom Link**: A `<span>` element styled with a pointer cursor and click handler, but without a semantic link role.
  2. **Custom Button**: A `<div>` element styled as a pill button, but without an accessible button role.
  3. **Hidden Input**: A standard text `<input>` field placed inside a parent container marked with `aria-hidden="true"`.
- **Expected Flow**:
  - **Attempt 1 (AXTREE)**: The agent starts with the default `AXTREE` context level. Since AXTree omits non-semantic interactive divs/spans and aria-hidden nodes, the LLM prompt does not contain these target elements.
  - **Escalation**: The agent fails to find/interact with the elements and context escalates to `LEAN`.
  - **Attempt 2 (LEAN)**: The agent retries with `LEAN` context. Since the LEAN simplified DOM extractor retrieves all visible elements with pointer cursors/click handlers and form inputs, the agent successfully locates the elements and completes the steps.

#### 2. LEAN to STANDARD Challenge (Plain Text Verification)
- **The Element**:
  - **Plain Text Paragraph**: A standard `<p>` tag displaying the text: `Verification Token: AURA-9921-SECURE`.
- **Expected Flow**:
  - **Attempt 1 (LEAN)**: If asked to verify this token, the agent starts at `AXTREE`, escalates to `LEAN`, and retries. Since `LEAN` mode only extracts interactive elements, headings, and forms, the plain text paragraph is completely excluded from the LEAN prompt context.
  - **Escalation**: The LLM determines that it cannot verify the text content using the LEAN DOM, and returns an `ESCALATE` response status with the target context `STANDARD`.
  - **Attempt 2 (STANDARD)**: The agent escalates to `STANDARD` context level. In `STANDARD` mode, the DOM extractor appends all visible plain text contents (paragraphs, spans, table cells, etc.). The LLM now sees the token text in the prompt, successfully verifies its presence, and completes the step.

### B. Visual Escalation Challenge (`shop/visual-escalation.html`)

This page contains elements drawn purely graphically. Because their content does not exist in standard DOM text nodes, the agent must escalate to the `VISUAL` context level to see them in a screenshot.

#### 1. Canvas-Rendered Target
- **The Element**:
  - A `<canvas id="canvas-target">` element containing the text `VISUAL-ONLY-TOKEN` drawn purely via Canvas 2D context pixels.
- **Expected Flow**:
  - **Attempt 1 (STANDARD)**: If asked to verify or click this token, the agent starts at `AXTREE` and escalates to `STANDARD`. Since canvas text is purely graphical and not part of DOM text nodes, the LLM cannot see the text in the STANDARD DOM context.
  - **Escalation**: The LLM requests escalation to `VISUAL` context level so that it receives the viewport screenshot.
  - **Attempt 2 (VISUAL)**: The agent escalates to `VISUAL`. The LLM receives the screenshot, visually identifies `VISUAL-ONLY-TOKEN` on the canvas, maps it to the canvas element (`#canvas-target`), and returns a click or success status.

#### 2. CSS Pseudo-Element Target
- **The Element**:
  - An empty container `div` `#pseudo-container` with its text content `PSEUDO-ELEMENT-SECRET` injected purely via the CSS `:after` selector content rule.
- **Expected Flow**:
  - Similar to the canvas target, pseudo-element text does not exist in DOM text nodes. The agent must escalate to `VISUAL` to see the generated text in the screenshot and click or verify the container.

---

## 8. Unified Sandbox Scenario Challenges (`shop/sandbox/`)

This directory contains standalone pages designed to test isolated edge cases in AI web automation:

1. **SVG Icon Buttons (`svg-icons.html`):**
   * *Challenge:* Buttons enclosing only raw SVG elements with zero text content, labels, or title attributes.
   * *Aura AI Verification:* Tests the agent's ability to trigger the `VISUAL_LEAN` context level to analyze the SVG shape visually to identify "Edit", "Delete", or "Share".
2. **Canvas Interactive Hotspots (`canvas-click.html`):**
   * *Challenge:* A single `<canvas>` element containing targets drawn in pixels.
   * *Aura AI Verification:* Tests coordinate-based offset clicks targeting specific parts of a single DOM element.
3. **Shadow DOM Piercing (`shadow-dom.html`):**
   * *Challenge:* A form encapsulated within the open shadow root of a Web Component.
   * *Aura AI Verification:* Tests recursive shadow root traversal to locate inputs and execute typings.
4. **Click Interception (`click-intercept.html`):**
   * *Challenge:* A button overlayed by a transparent blocking element.
   * *Aura AI Verification:* Tests the agent's recovery and self-healing mechanisms when encountering `ElementClickInterceptedException`.
5. **Dynamic Reveal Timing (`dynamic-reveal.html`):**
   * *Challenge:* Inputs injected into the DOM after a 200ms asynchronous transition.
   * *Aura AI Verification:* Tests execution settling delays and checks that the runner doesn't fail on action batching.
6. **Hover Chain Menu (`hover-chain.html`):**
   * *Challenge:* Nested dropdown menus visible only via CSS hover states.
   * *Aura AI Verification:* Tests chaining sequential hover actions prior to a click.
7. **Table Price Sorting (`table-sorting.html`):**
   * *Challenge:* Sorting items in a table with a 400ms loading overlay delay.
   * *Aura AI Verification:* Tests waiting for DOM settling before performing sorted table assertions.
8. **Scroll-Overflow List (`scroll-list.html`):**
   * *Challenge:* A button placed at the bottom of a scrollable list, out of initial viewport view.
   * *Aura AI Verification:* Tests scroll-into-view commands within containers.
9. **Floating Label Overlaps (`floating-labels.html`):**
   * *Challenge:* Visual text overlaps generated by programmatically autofilling fields without triggering focus.
   * *Aura AI Verification:* Tests layout anomaly detection via the Visual Auditor (`(visual)`).
10. **Cross-Origin iFrame (`cross-origin-iframe.html`):**
    * *Challenge:* A parent HTTPS page loading an iframe hosted on an HTTP port (different origin).
    * *Aura AI Verification:* Tests switching WebDriver contexts inside cross-origin borders.
11. **Mock OAuth Login (`mock-oauth-login.html`):**
    * *Challenge:* An external auth provider login form that redirects back with token parameters.
    * *Aura AI Verification:* Tests cross-domain redirect flows, browser window redirection handles, and query token extraction.

---

## 9. Multi-Quality Apparel Store Sandbox (`shop/homepage-*.html` & `shop/plp-*.html`)

To benchmark and stress-test the robustness of test automation locators (IDs, CSS Selectors, and XPath) alongside visual regression auditors, the sandbox provides three visually identical variations of an Apparel Store Homepage and Product Listing Page (PLP) built with varying underlying HTML/CSS code quality:

### A. Perfect Quality (`homepage-perfect.html` & `plp-perfect.html`)
- **Characteristics:** Built following W3C standards and AAA accessibility best practices.
- **Locators:** Contains distinct, descriptive, and stable `id` and `class` attributes on all interactive elements (e.g. `id="nav-link-tops"`, `id="newsletter-email-input"`).
- **Accessibility:** Semantic HTML elements (`<header>`, `<nav>`, `<aside>`, `<main>`, `<section>`), explicit `<label>` bindings, and rich `aria-*` roles.
- **Auditing Value:** Validates the ideal baseline case for locator-based selector engines.

### B. Normal Quality (`homepage-normal.html` & `plp-normal.html`)
- **Characteristics:** Represents average commercial web development.
- **Locators:** Uses generic or slightly inconsistent naming structures (e.g. `id="inp_email"`, `class="cb-cat"`, `id="region_btn"`).
- **Accessibility:** Omits `aria-*` accessibility metadata. Inputs rely entirely on placeholder text rather than linked `<label>` tags. Semantic layout is replaced by generic `div`/`span` structures.
- **Auditing Value:** Serves as a standard, real-world baseline.

### C. Bad Quality (`homepage-bad.html` & `plp-bad.html`)
- **Characteristics:** Simulates legacy, obfuscated, or poorly generated websites.
- **Locators:** Uses duplicate IDs across elements (e.g. multiple `id="prod-info"`), random/obfuscated class names (e.g. `class="div-nest-3"`, `class="c-772x9"`), or completely lacks locators, forcing extremely long and brittle relative XPath references.
- **Accessibility:** Complete absence of semantic tags, alt text, labels, and roles. Form fields are styled divs, and checkboxes are toggled via styled click elements.
- **Auditing Value:** Validates the resilience of self-healing locators, visual locator mapping, and AI-driven selector healing algorithms under worst-case DOM conditions.

---

## 10. Dynamic VÉRLA Storefront Contexts (`/verla-*`)

The requirements, business features, and quality-level dimensions of the dynamic VÉRLA storefront have been extracted to a dedicated specification. 

Please refer to the complete documentation in [VERLA_DEMO_STORE.md](VERLA_DEMO_STORE.md).



