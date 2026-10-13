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
}
