/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.executor.selenide.selector;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Browser-backed integration tests for {@link PlaywrightSelectorEvaluatorLoader},
 * verifying transparent Shadow DOM piercing, text matching, and role evaluation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaywrightSelectorEvaluatorBrowserTest
{
    @BeforeEach
    public void setUp()
    {
        Configuration.headless = true;
        Configuration.browser = "chrome";

        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Playwright Selector Evaluator Test</title></head>
            <body>
              <div id="regular-container">
                <h2>Regular Light DOM Title</h2>
                <button id="regular-button" aria-label="Regular Action">Submit Order</button>
                <a href="index.html">VÉRLA.</a>
              </div>

              <!-- Custom Web Component with Open Shadow Root -->
              <c-custom-login id="login-comp"></c-custom-login>

              <script>
                class CustomLogin extends HTMLElement {
                  constructor() {
                    super();
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = `
                      <div class="login-wrapper">
                        <label for="shadow-user">Shadow User</label>
                        <input id="shadow-user" type="text" placeholder="Enter username">
                        <button class="shadow-login-btn" type="button">Log In Now</button>
                      </div>
                    `;
                  }
                }
                customElements.define('c-custom-login', CustomLogin);
              </script>
            </body>
            </html>
            """;

        Selenide.open("data:text/html;charset=utf-8," + html);
    }

    @AfterEach
    public void tearDown()
    {
        Selenide.closeWebDriver();
    }

    @Test
    public void testCssPiercesOpenShadowDom()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button.shadow-login-btn");

        Assertions.assertEquals(1, results.size(), "Should find the button inside the shadow root");
        Assertions.assertEquals("Log In Now", results.get(0).getText().trim());
    }

    @Test
    public void testTextPiercesOpenShadowDom()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "text=Log In Now");

        Assertions.assertEquals(1, results.size(), "Should find button via text= inside shadow root");
        Assertions.assertEquals("button", results.get(0).getTagName().toLowerCase());
    }

    @Test
    public void testXPathDoesNotPierceShadowDom()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "xpath=//button[@class='shadow-login-btn']");

        Assertions.assertTrue(results.isEmpty(), "XPath must NOT pierce open shadow roots");
    }

    @Test
    public void testRoleSelectorInsideShadowDom()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "role=button[name=\"Log In Now\"]");

        Assertions.assertEquals(1, results.size(), "Should find button via role and computed accessible name in shadow DOM");
    }

    @Test
    public void testRoleHeadingInLightDom()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "role=heading[level=2]");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("Regular Light DOM Title", results.get(0).getText().trim());
    }

    @Test
    public void testVerlaLogoSelectorMatches()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "a[href=\"index.html\"]:contains(\"VÉRLA\")");

        Assertions.assertEquals(1, results.size(), "Should match VÉRLA link without bogus class errors");
        Assertions.assertEquals("VÉRLA.", results.get(0).getText().trim());
    }

    @Test
    public void testChainedSelectorWithCapture()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "*#login-comp >> button.shadow-login-btn");

        Assertions.assertEquals(1, results.size(), "Should capture the intermediate #login-comp element");
        Assertions.assertEquals("c-custom-login", results.get(0).getTagName().toLowerCase());
    }

    @Test
    public void testHasPseudoClass()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "div:has(#regular-button)");

        Assertions.assertEquals(1, results.size(), "Should find #regular-container containing the button");
        Assertions.assertEquals("regular-container", results.get(0).getAttribute("id"));
    }

    @Test
    public void testVisiblePseudoClass()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button:visible");

        Assertions.assertEquals(2, results.size(), "Both the regular button and shadow login button are visible");
    }

    @Test
    public void testRightOfLayoutPseudoClass()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "a:right-of(#regular-button)");

        Assertions.assertEquals(1, results.size(), "Link should be located to the right of the button");
        Assertions.assertEquals("VÉRLA.", results.get(0).getText().trim());
    }

    @Test
    public void testTextIsExactMatchExcludesSubstrings()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body>
              <div class="dropdown">
                <button class="size-btn" type="button" data-size="S">S</button>
                <button class="size-btn" type="button" data-size="L">L</button>
                <button class="size-btn" type="button" data-size="XL">XL</button>
              </div>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<WebElement> resultsL = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button.size-btn:text-is(\"L\")");
        Assertions.assertEquals(1, resultsL.size(), "button.size-btn:text-is(\"L\") must match only 'L' and not 'XL'");
        Assertions.assertEquals("L", resultsL.get(0).getText().trim());

        final List<WebElement> resultsXL = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button.size-btn:text-is(\"XL\")");
        Assertions.assertEquals(1, resultsXL.size(), "button.size-btn:text-is(\"XL\") must match 'XL'");
        Assertions.assertEquals("XL", resultsXL.get(0).getText().trim());

        final List<WebElement> resultsSingleQuote = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button.size-btn:text-is('L')");
        Assertions.assertEquals(1, resultsSingleQuote.size(), "Single-quoted :text-is('L') must match 'L'");

        final List<WebElement> resultsExactText = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button.size-btn:exact-text(\"L\")");
        Assertions.assertEquals(1, resultsExactText.size(), ":exact-text(\"L\") must match 'L'");
    }

    @Test
    public void testTextIsWithChildWrapper()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body>
              <button id="wrapped-btn" type="button"><span>Submit Order</span></button>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "button:text-is(\"Submit Order\")");
        Assertions.assertEquals(1, results.size(), "button:text-is must return button even when text is inside child <span>");
        Assertions.assertEquals("wrapped-btn", results.get(0).getAttribute("id"));
    }

    @Test
    public void testChainedHasTextNestedContainersExcludeParent()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body>
              <div id="virtual-list-container" class="virtual-scroll-viewport">
                <div id="virtual-content" class="virtual-content">
                  <div class="virtual-item" id="item-38">
                    <div class="item-title">Product #38: Plasma Conduit</div>
                    <button id="select-product-38" class="select-btn">Select</button>
                  </div>
                  <div class="virtual-item" id="item-42">
                    <div class="item-title">Product #42: Quantum Sensor</div>
                    <button id="select-product-42" class="select-btn">Select</button>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;base64," + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)));
        final WebDriver driver = WebDriverRunner.getWebDriver();

        // 1. Verify specific class container matches item-42
        final List<WebElement> containerResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "div.virtual-item:has-text(\"Product #42\")");
        Assertions.assertEquals(1, containerResults.size(), "div.virtual-item:has-text should match only item-42");
        Assertions.assertEquals("item-42", containerResults.get(0).getAttribute("id"));

        // 2. Verify chained selector selects only item-42's Select button, never item-38 from ancestor container
        final List<WebElement> buttonResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "div:has-text(\"Product #42\") >> button:has-text(\"Select\")");
        Assertions.assertEquals(1, buttonResults.size(), "Chained selector must resolve only Product #42's button");
        Assertions.assertEquals("select-product-42", buttonResults.get(0).getAttribute("id"));

        // 3. Verify captured clause *div:has-text(...) prunes outer virtual-list-container and captures item-42
        final List<WebElement> capturedResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "*div:has-text(\"Product #42\") >> button:has-text(\"Select\")");
        Assertions.assertEquals(1, capturedResults.size(), "Captured container selector must resolve to item-42");
        Assertions.assertEquals("item-42", capturedResults.get(0).getAttribute("id"));
    }

    @Test
    public void testCssDescendantCombinatorPiercesShadowBoundary()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();

        // 1. Host in light DOM, descendant in shadow DOM
        final List<WebElement> results = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "c-custom-login .shadow-login-btn");
        Assertions.assertEquals(1, results.size(), "Should find button via host descendant combinator");
        Assertions.assertEquals("Log In Now", results.get(0).getText().trim());

        // 2. Host ID in light DOM, input in shadow DOM
        final List<WebElement> inputResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "#login-comp input");
        Assertions.assertEquals(1, inputResults.size(), "Should find input via host ID descendant combinator");
        Assertions.assertEquals("shadow-user", inputResults.get(0).getAttribute("id"));

        // 3. Child combinator across shadow boundary: host > direct shadow child
        final List<WebElement> wrapperResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "c-custom-login > .login-wrapper");
        Assertions.assertEquals(1, wrapperResults.size(), "Should find direct shadow child via child combinator");
        Assertions.assertEquals("login-wrapper", wrapperResults.get(0).getAttribute("class"));

        // 4. Multi-level combinator: host > wrapper button
        final List<WebElement> chainedResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "c-custom-login > .login-wrapper button");
        Assertions.assertEquals(1, chainedResults.size(), "Should find button through multi-level combinator across shadow root");
        Assertions.assertEquals("Log In Now", chainedResults.get(0).getText().trim());
    }

    @Test
    public void testTripleNestedShadowRootsCombinator()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body>
              <c-portal id="portal-app"></c-portal>
              <script>
                class CInput extends HTMLElement {
                  connectedCallback() {
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = '<div class="wrap"><input id="field-input" type="email"></div>';
                  }
                }
                customElements.define('c-input', CInput);

                class CStepper extends HTMLElement {
                  connectedCallback() {
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = '<div class="form"><c-input id="email-field"></c-input></div>';
                  }
                }
                customElements.define('c-stepper', CStepper);

                class CPortal extends HTMLElement {
                  connectedCallback() {
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = '<div class="card"><c-stepper id="auth-stepper"></c-stepper></div>';
                  }
                }
                customElements.define('c-portal', CPortal);
              </script>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;base64," + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)));
        final WebDriver driver = WebDriverRunner.getWebDriver();

        // Query deep nested component and input inside it
        final List<WebElement> inputResults = PlaywrightSelectorEvaluatorLoader.evaluate(driver, "c-input#email-field input");
        Assertions.assertEquals(1, inputResults.size(), "Should find input in triple nested shadow roots");
        Assertions.assertEquals("field-input", inputResults.get(0).getAttribute("id"));
    }
}
