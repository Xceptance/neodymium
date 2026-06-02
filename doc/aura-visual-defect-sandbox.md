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
│       │   └── forms.html          # Registrations, validation error, price calculators
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
Validates core requirements (visual overlaps, text clippings, nested iframes, custom served images, and conditional optional banners).

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
  Neodymium.ai().execute("Observe page visual consistency (glance)");
  
  // Stage 2 (Bypasses network, hits local cache cache)
  Neodymium.ai().execute("Observe page visual consistency (glance)");
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
