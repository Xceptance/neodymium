/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.executor.selenide.plugins;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Unit test for {@link AssertAction} verifying support for all DOM element states.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public class AssertActionTest extends BaseAiTest
{
    @Test
    @DisplayName("AssertAction handles all DOM element state assertions cleanly")
    public void testAssertActionDomStates() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // 1. Visible & Hidden
        plugin.execute(new Action("ASSERT", "#visible-btn", "visible", "check visible", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-btn", "hidden", "check hidden", "reasoning", false));

        // 2. Checked & Unchecked
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "checked", "check checked", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "unchecked", "check unchecked", "reasoning", false));

        // 3. Disabled & Enabled
        plugin.execute(new Action("ASSERT", "#disabled-input", "disabled", "check disabled", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "enabled", "check enabled", "reasoning", false));

        // 4. Selected
        plugin.execute(new Action("ASSERT", "#opt-user", "selected", "check selected", "reasoning", false));

        // 5. Readonly & Editable
        plugin.execute(new Action("ASSERT", "#readonly-input", "readonly", "check readonly", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "editable", "check editable", "reasoning", false));

        // 6. Focused
        $("#username").click();
        plugin.execute(new Action("ASSERT", "#username", "focused", "check focused", "reasoning", false));

        // 7. Boolean "true" / "false" shortcuts & attribute expressions
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "true", "check true on checkbox", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "false", "check false on checkbox", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "checked=true", "check checked=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "checked=false", "check checked=false", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#disabled-input", "disabled=true", "check disabled=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "disabled=false", "check disabled=false", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#opt-user", "selected=true", "check selected=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#readonly-input", "readonly=true", "check readonly=true", "reasoning", false));
    }

    @Test
    @DisplayName("AssertAction with isRegex=false matches literal text containing dollar signs cleanly")
    public void testAssertActionLiteralWithDollarSignPasses() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        // #welcome-message contains text: "Welcome to our web store!"
        plugin.execute(new Action("ASSERT", "#welcome-message", "Welcome", "check literal Welcome", "reasoning", false));

        // #total-price contains text: "Total Amount: CAD $ 120.00"
        plugin.execute(new Action("ASSERT", "#total-price", "CAD $", "check literal CAD $", "reasoning", false));
        plugin.execute(new Action("ASSERT", "[data-ai=\"xcuunj33\"]", "CAD $", "check data-ai literal CAD $", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#price-usd", "$ 45.00", "check literal $ 45.00", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#price-compact", "$100", "check literal $100", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#price-eur", "50 €", "check literal 50 €", "reasoning", false));
    }

    @Test
    @DisplayName("AssertAction with isRegex=true handles CAD $ and dollar signs without failing on end anchor")
    public void testAssertActionRegexWithDollarSignAndCurrency() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        // Even when LLM sets isRegex=true for "CAD $" or "$", it matches cleanly
        plugin.execute(new Action("ASSERT", "[data-ai=\"xcuunj33\"]", "CAD $", "check regex CAD $", "reasoning", true));
        plugin.execute(new Action("ASSERT", "#total-price", "CAD \\$ 120\\.00", "check fully escaped regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "#total-price", "CAD $", "check unescaped CAD $ regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "#price-usd", "$ 45.00", "check unescaped $ 45.00 regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "#price-compact", "$100", "check unescaped $100 regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "#price-eur", "50 €", "check 50 € regex", "reasoning", true));
    }

    @Test
    @DisplayName("AssertAction handles URL and Title assertions with isRegex=true cleanly")
    public void testAssertActionUrlAndTitleRegexMatching() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // 1. URL regex match
        plugin.execute(new Action("ASSERT", "url", ".*AssertActionTest/testAssertHappyPath\\.html", "check url regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "currentUrl", ".*/testAssertHappyPath\\.html.*", "check currentUrl regex", "reasoning", true));

        // 2. Title regex match
        plugin.execute(new Action("ASSERT", "title", ".*Assert.*Action.*Test.*", "check title regex", "reasoning", true));
        plugin.execute(new Action("ASSERT", "pageTitle", "^Assert.*Test$", "check pageTitle regex", "reasoning", true));
    }

    @Test
    @DisplayName("AssertAction handles URL and Title assertions with isRegex=false literal contains cleanly")
    public void testAssertActionUrlAndTitleLiteralContains() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // 1. URL literal substring contains
        plugin.execute(new Action("ASSERT", "url", "testAssertHappyPath.html", "check url contains", "reasoning", false));

        // 2. Title literal substring contains
        plugin.execute(new Action("ASSERT", "title", "Assert Action", "check title contains", "reasoning", false));
    }

    @Test
    @DisplayName("AssertAction gracefully handles malformed regex patterns by falling back to literal matching")
    public void testAssertActionMalformedRegexFallback() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        // Regex with surrounding slashes cleaned cleanly
        plugin.execute(new Action("ASSERT", "#welcome-message", "/Welcome.*store!/", "regex with slashes", "reasoning", true));
        // Regex on title with surrounding slashes
        plugin.execute(new Action("ASSERT", "title", "/Assert Action Test/", "regex title with slashes", "reasoning", true));
        // Regex on URL with surrounding slashes
        plugin.execute(new Action("ASSERT", "url", "/testAssertHappyPath\\.html/", "regex url with slashes", "reasoning", true));
        // Unclosed regex bracket on an element where text does not match throws AssertionError cleanly (not unhandled PatternSyntaxException)
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#welcome-message", "[unclosed-bracket", "malformed regex non-matching", "reasoning", true));
        });
    }

    @Test
    @DisplayName("AssertAction throws RuntimeException when URL or Title assertion has null value")
    public void testAssertActionNullValueOnUrlAndTitleThrows() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        Assertions.assertThrows(RuntimeException.class, () ->
        {
            plugin.execute(new Action("ASSERT", "url", null, "check null url", "reasoning", false));
        });
        Assertions.assertThrows(RuntimeException.class, () ->
        {
            plugin.execute(new Action("ASSERT", "title", null, "check null title", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles asserting selected state on direct SELECT elements")
    public void testAssertActionDirectSelectElement() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        plugin.execute(new Action("ASSERT", "#role-select", "selected", "check select element has selected option", "reasoning", false));
    }

    @Test
    @DisplayName("AssertAction throws AssertionError on failed state assertions")
    public void testAssertActionNegativeStates() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // 1. Asserting disabled element is enabled
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#disabled-input", "enabled", "check enabled on disabled element", "reasoning", false));
        });

        // 2. Asserting unchecked box is checked
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#terms-opt", "checked", "check checked on unchecked box", "reasoning", false));
        });

        // 3. Asserting readonly input is editable
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#readonly-input", "editable", "check editable on readonly element", "reasoning", false));
        });

        // 4. Asserting unselected option is selected
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#opt-admin", "selected", "check selected on unselected option", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles distinct ASSERT_EXISTS and ASSERT_ABSENT cleanly")
    public void testDistinctAssertExistsAndAbsent() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        plugin.execute(new Action("ASSERT_EXISTS", "#visible-btn", "", "check exists", "reasoning", false));
        plugin.execute(new Action("ASSERT_VISIBLE", "#visible-btn", "", "check visible", "reasoning", false));
        plugin.execute(new Action("ASSERT_ABSENT", "#hidden-btn", "", "check absent", "reasoning", false));
        plugin.execute(new Action("ASSERT_HIDDEN", "#hidden-btn", "", "check hidden", "reasoning", false));
        plugin.execute(new Action("ASSERT_ABSENT", "#non-existent-element-xyz", "", "check non-existent is absent", "reasoning", false));

        // Negative: asserting non-existent element exists or is visible
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_EXISTS", "#non-existent-element-xyz", "", "check non-existent exists", "reasoning", false));
        });
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_VISIBLE", "#non-existent-element-xyz", "", "check non-existent visible", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles distinct ASSERT_TEXT and ASSERT_VALUE cleanly without keyword collision")
    public void testDistinctAssertTextAndValue() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        plugin.execute(new Action("ASSERT_TEXT", "#welcome-message", "Welcome", "check text Welcome", "reasoning", false));
        plugin.execute(new Action("ASSERT_TEXT", "#total-price", "CAD $", "check text CAD $", "reasoning", false));
        plugin.execute(new Action("ASSERT_TEXT", "#price-usd", "$ 45.00", "check text $ 45.00", "reasoning", false));

        // Test with value input
        plugin.execute(new Action("ASSERT_VALUE", "#username", "JohnDoe", "check username initial value", "reasoning", false));
        $("#username").setValue("AntigravityUser");
        plugin.execute(new Action("ASSERT_VALUE", "#username", "AntigravityUser", "check username value", "reasoning", false));

        // Negative: asserting wrong text or value
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_TEXT", "#welcome-message", "NonExistentText123", "wrong text", "reasoning", false));
        });
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_VALUE", "#username", "WrongValue", "wrong value", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles distinct element state actions (ASSERT_CHECKED, ASSERT_DISABLED, etc.)")
    public void testDistinctAssertElementStates() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // Checkbox states
        plugin.execute(new Action("ASSERT_CHECKED", "#newsletter-opt", "", "check checked", "reasoning", false));
        plugin.execute(new Action("ASSERT_UNCHECKED", "#terms-opt", "", "check unchecked", "reasoning", false));

        // Disabled / Enabled
        plugin.execute(new Action("ASSERT_DISABLED", "#disabled-input", "", "check disabled", "reasoning", false));
        plugin.execute(new Action("ASSERT_ENABLED", "#enabled-input", "", "check enabled", "reasoning", false));

        // Focus
        $("#username").click();
        plugin.execute(new Action("ASSERT_FOCUSED", "#username", "", "check focused", "reasoning", false));

        // Selected
        plugin.execute(new Action("ASSERT_SELECTED", "#opt-user", "", "check option selected", "reasoning", false));
        plugin.execute(new Action("ASSERT_SELECTED", "#role-select", "", "check select has selected option", "reasoning", false));

        // Readonly / Editable
        plugin.execute(new Action("ASSERT_READONLY", "#readonly-input", "", "check readonly", "reasoning", false));
        plugin.execute(new Action("ASSERT_EDITABLE", "#enabled-input", "", "check editable", "reasoning", false));

        // Negative checks
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_ENABLED", "#disabled-input", "", "check enabled on disabled", "reasoning", false));
        });
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_DISABLED", "#enabled-input", "", "check disabled on enabled", "reasoning", false));
        });
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_CHECKED", "#terms-opt", "", "check checked on unchecked", "reasoning", false));
        });
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_UNCHECKED", "#newsletter-opt", "", "check unchecked on checked", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles distinct ASSERT_URL and ASSERT_TITLE cleanly")
    public void testDistinctAssertUrlAndTitle() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        plugin.execute(new Action("ASSERT_URL", "url", "testAssertHappyPath.html", "check url contains", "reasoning", false));
        plugin.execute(new Action("ASSERT_TITLE", "title", "Assert Action Test", "check title contains", "reasoning", false));
        plugin.execute(new Action("ASSERT_URL", "url", ".*AssertActionTest.*", "check url regex", "reasoning", true));
        plugin.execute(new Action("ASSERT_TITLE", "title", "^Assert Action.*", "check title regex", "reasoning", true));
    }

    @Test
    @DisplayName("AssertAction distinguishes ASSERT_EXISTS (in DOM) vs ASSERT_VISIBLE (on screen) on hidden elements")
    public void testDistinctAssertExistsVsVisibleOnHiddenElement() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // #hidden-btn is display:none in the DOM
        // 1. ASSERT_EXISTS succeeds because the element is in the DOM
        plugin.execute(new Action("ASSERT_EXISTS", "#hidden-btn", "", "check hidden element exists in DOM", "reasoning", false));

        // 2. ASSERT_HIDDEN succeeds
        plugin.execute(new Action("ASSERT_HIDDEN", "#hidden-btn", "", "check hidden element is hidden", "reasoning", false));

        // 3. ASSERT_VISIBLE must fail on display:none element
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_VISIBLE", "#hidden-btn", "", "check hidden is visible (should fail)", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles ASSERT_VALUE with isRegex=true correctly")
    public void testDistinctAssertValueRegex() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();
        $("#username").setValue("INV-98765-DE");

        // Dynamic regex pattern matching on input value
        plugin.execute(new Action("ASSERT_VALUE", "#username", "^INV-\\d+-DE$", "check invoice pattern", "reasoning", true));

        // Negative: pattern mismatch
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_VALUE", "#username", "^ORD-\\d+$", "mismatched pattern", "reasoning", true));
        });
    }

    @Test
    @DisplayName("AssertAction handles ASSERT_ATTRIBUTE with name=value, name, and regex")
    public void testDistinctAssertAttribute() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // Attribute existence
        plugin.execute(new Action("ASSERT_ATTRIBUTE", "#username", "placeholder", "check placeholder attribute exists", "reasoning", false));
        plugin.execute(new Action("ASSERT_ATTRIBUTE", "#disabled-input", "disabled", "check disabled attribute exists", "reasoning", false));

        // Attribute name=value match
        plugin.execute(new Action("ASSERT_ATTRIBUTE", "#username", "placeholder=Enter username", "check placeholder value", "reasoning", false));
        plugin.execute(new Action("ASSERT_ATTRIBUTE", "#total-price", "data-ai=\"xcuunj33\"", "check quoted data-ai attribute", "reasoning", false));

        // Attribute regex match
        plugin.execute(new Action("ASSERT_ATTRIBUTE", "#username", "placeholder=.*username", "check placeholder regex", "reasoning", true));

        // Negative: wrong attribute value
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_ATTRIBUTE", "#username", "placeholder=WrongPlaceholder", "wrong placeholder", "reasoning", false));
        });
    }

    @Test
    @DisplayName("AssertAction handles ASSERT_COUNT on element collections (exact, >=, >, <, <=)")
    public void testDistinctAssertCount() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // Exact count
        plugin.execute(new Action("ASSERT_COUNT", "input[type='checkbox']", "2", "check exact 2 checkboxes", "reasoning", false));
        plugin.execute(new Action("ASSERT_COUNT", "input[type='radio']", "2", "check exact 2 radios", "reasoning", false));

        // Comparison operators
        plugin.execute(new Action("ASSERT_COUNT", "input", ">=5", "check at least 5 inputs", "reasoning", false));
        plugin.execute(new Action("ASSERT_COUNT", "input", ">4", "check more than 4 inputs", "reasoning", false));
        plugin.execute(new Action("ASSERT_COUNT", "input[type='checkbox']", "<5", "check fewer than 5 checkboxes", "reasoning", false));
        plugin.execute(new Action("ASSERT_COUNT", "input[type='checkbox']", "<=2", "check at most 2 checkboxes", "reasoning", false));

        // Negative: count mismatch
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_COUNT", "input[type='checkbox']", "5", "wrong count", "reasoning", false));
        });
    }
}
