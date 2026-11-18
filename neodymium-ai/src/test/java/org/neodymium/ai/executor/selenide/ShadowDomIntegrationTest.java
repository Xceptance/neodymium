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
package org.neodymium.ai.executor.selenide;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.ContextLevel;

/**
 * Integration tests verifying that {@link PageAnalyzer} and {@link SelenideElementFinder}
 * seamlessly traverse and interact with open Shadow DOM boundaries and custom Web Components.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public class ShadowDomIntegrationTest
{
    @BeforeEach
    public void setUp()
    {
        Configuration.headless = true;
        Configuration.browser = "chrome";

        final String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Shadow DOM Test</title></head>
            <body>
              <div id="regular-container">
                <h2>Regular Light DOM Title</h2>
                <button id="regular-button">Regular Button</button>
              </div>

              <!-- Custom Web Component with Open Shadow Root -->
              <c-custom-login id="login-comp"></c-custom-login>

              <!-- Nested Custom Web Component -->
              <c-nested-outer id="nested-outer"></c-nested-outer>

              <script>
                class CustomLogin extends HTMLElement {
                  constructor() {
                    super();
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = `
                      <div class="login-wrapper">
                        <c-custom-input id="email-comp"></c-custom-input>
                        <button class="shadow-login-btn is-visible" type="button">Log In</button>
                      </div>
                    `;
                  }
                }
                customElements.define('c-custom-login', CustomLogin);

                class CustomInput extends HTMLElement {
                  constructor() {
                    super();
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = `
                      <div class="input-field-group">
                        <label for="inner-email-input">Email Address</label>
                        <input id="inner-email-input" type="email" name="user_email" placeholder="name@example.com" class="c-input">
                      </div>
                    `;
                  }
                }
                customElements.define('c-custom-input', CustomInput);

                class NestedOuter extends HTMLElement {
                  constructor() {
                    super();
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = `
                      <div class="outer-box">
                        <c-nested-inner id="nested-inner"></c-nested-inner>
                      </div>
                    `;
                  }
                }
                customElements.define('c-nested-outer', NestedOuter);

                class NestedInner extends HTMLElement {
                  constructor() {
                    super();
                    const shadow = this.attachShadow({mode: 'open'});
                    shadow.innerHTML = `
                      <div class="inner-box">
                        <input id="nested-secret" type="password" name="secret_code" placeholder="Secret Key">
                      </div>
                    `;
                  }
                }
                customElements.define('c-nested-inner', NestedInner);
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
    public void testPageAnalyzerCapturesShadowDomElements()
    {
        final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());
        final String domOutput = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

        Assertions.assertNotNull(domOutput);
        // Verify custom components and shadow elements are represented
        Assertions.assertTrue(domOutput.contains("c-custom-login") || domOutput.contains("user_email"),
            "DOM output should contain shadow DOM elements or custom component tags: " + domOutput);
        Assertions.assertTrue(domOutput.contains("name@example.com") || domOutput.contains("inner-email-input"),
            "DOM output should contain shadow DOM email input field: " + domOutput);
    }

    @Test
    public void testFindElementWithCompoundSelectorAcrossShadowBoundary()
    {
        // Compound selector piercing across custom element boundary
        final SelenideElement emailInput = SelenideElementFinder.findElement("c-custom-input input");
        Assertions.assertNotNull(emailInput);

        emailInput.setValue("testuser@canyon.com");
        Assertions.assertEquals("testuser@canyon.com", emailInput.getValue());
    }

    @Test
    public void testFindElementWithDirectClassInShadowDom()
    {
        // Direct selector inside shadow root
        final SelenideElement loginBtn = SelenideElementFinder.findElement(".shadow-login-btn");
        Assertions.assertNotNull(loginBtn);
        Assertions.assertEquals("Log In", loginBtn.getText().trim());

        // Compound selector matching host and inner class
        final SelenideElement loginBtnCompound = SelenideElementFinder.findElement("c-custom-login .shadow-login-btn.is-visible");
        Assertions.assertNotNull(loginBtnCompound);
        Assertions.assertEquals("Log In", loginBtnCompound.getText().trim());
    }

    @Test
    public void testFindElementWithNestedShadowRoots()
    {
        // Compound selector piercing through two levels of open shadow roots: c-nested-outer -> c-nested-inner -> input
        final SelenideElement secretInput = SelenideElementFinder.findElement("c-nested-outer c-nested-inner input");
        Assertions.assertNotNull(secretInput);

        secretInput.setValue("supersecret123");
        Assertions.assertEquals("supersecret123", secretInput.getValue());
    }

    @Test
    public void testFindElementWithActionFallback()
    {
        final Action action = new Action(
            "TYPE",
            "c-custom-login c-custom-input input",
            List.of("fallback@test.com"),
            "Enter email address",
            "Targeting shadow DOM input via action",
            false);

        final SelenideElement element = SelenideElementFinder.findElement(action);
        Assertions.assertNotNull(element);
        element.setValue("action@example.com");
        Assertions.assertEquals("action@example.com", element.getValue());
    }

    @Test
    public void testFindElementsCollectionInShadowDom()
    {
        final Action action = new Action(
            "ASSERT",
            "c-custom-login .shadow-login-btn",
            List.of("1"),
            "Verify login button exists",
            "Asserting button presence in shadow DOM",
            false);

        final ElementsCollection collection = SelenideElementFinder.findElements(action);
        Assertions.assertFalse(collection.isEmpty(), "Collection should not be empty for shadow DOM element");
        Assertions.assertEquals("Log In", collection.first().getText().trim());
    }
}
