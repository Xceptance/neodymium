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
package com.xceptance.neodymium.ai.core;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import org.junit.jupiter.api.Assertions;

/**
 * Integration tests for PageAnalyzer checkable input logic.
 * Verifies checked state detection, value retrieval, custom ARIA roles, and label-visibility fallbacks.
 *
 * @author AI-generated: Gemini 3.5 Flash (Medium)
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1024x768")
public class PageAnalyzerCheckableTest extends BaseAiTest
{
    /**
     * Verifies that standard checkable inputs have their 'checked' state and 'value'
     * correctly captured, while text inputs preserve privacy (value is null).
     */
    @NeodymiumTest
    public final void testStandardCheckableInputs()
    {
        Selenide.open(currentTestUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);

        Assertions.assertNotNull(dom);

        // 1. Text input should have value attribute captured, except for passwords
        Assertions.assertTrue(dom.contains("<input id=\"username\" name=\"username\" type=\"text\" value=\"mySecretUsername\""),
            "DOM should capture text input value");
        Assertions.assertTrue(dom.contains("<input id=\"password\" name=\"password\" type=\"password\" value=\"***\""),
            "DOM should obscure password input value");
        Assertions.assertTrue(dom.contains("<textarea id=\"comments\" name=\"comments\" value=\"Some multiline comment\""),
            "DOM should capture textarea value");

        // 2. Checkboxes should show proper checked and value attributes
        Assertions.assertTrue(dom.contains("<input id=\"subscribe\" name=\"subscribe\" type=\"checkbox\" checked=\"true\" value=\"newsletter\""),
            "DOM should capture checked status and value for checked checkbox");
        Assertions.assertTrue(dom.contains("<input id=\"terms\" name=\"terms\" type=\"checkbox\" value=\"agreed\""),
            "DOM should capture value but not checked status for unchecked checkbox");
        Assertions.assertFalse(dom.contains("id=\"terms\" name=\"terms\" type=\"checkbox\" checked="),
            "Unchecked checkbox must not contain checked attribute");

        // 3. Radios should show proper checked status and value
        Assertions.assertTrue(dom.contains("<input id=\"gender-male\" name=\"gender\" type=\"radio\" checked=\"true\" value=\"M\""),
            "DOM should capture checked status and value for checked radio button");
        Assertions.assertTrue(dom.contains("<input id=\"gender-female\" name=\"gender\" type=\"radio\" value=\"F\""),
            "DOM should capture value but not checked status for unchecked radio button");

        // 4. Submit button should show its value
        Assertions.assertTrue(dom.contains("<input id=\"submit-btn\" name=\"submit-btn\" type=\"submit\" value=\"Submit Form\""),
            "DOM should capture value for submit input button");
    }

    /**
     * Verifies that custom controls with role="radio" and data-checked or class="checked"
     * are correctly parsed as checked and have their roles exposed.
     */
    @NeodymiumTest
    public final void testCustomCheckableControls()
    {
        Selenide.open(currentTestUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);

        Assertions.assertNotNull(dom);

        // 1. Custom checked radio element
        Assertions.assertTrue(dom.contains("<clickable id=\"option-dhl\" role=\"radio\" checked=\"true\""),
            "DOM should identify custom checked element as checked and expose its role");

        // 2. Custom unchecked radio element
        Assertions.assertTrue(dom.contains("<clickable id=\"option-ups\" role=\"radio\""),
            "DOM should capture custom unchecked element and expose its role");
        Assertions.assertFalse(dom.contains("id=\"option-ups\" role=\"radio\" checked="),
            "Unchecked custom radio must not contain checked attribute");
    }

    /**
     * Verifies that visually hidden checkable inputs (display:none or 0x0) are still captured
     * when their associated labels are visible.
     */
    @NeodymiumTest
    public final void testVisuallyHiddenCheckableInputs()
    {
        Selenide.open(currentTestUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);

        Assertions.assertNotNull(dom);

        // 1. Visually hidden checkbox inside a visible wrapping label
        Assertions.assertTrue(dom.contains("<input id=\"subscribe-hidden\" name=\"subscribe\" type=\"checkbox\" checked=\"true\" value=\"yes\""),
            "DOM should capture display:none checkbox when its parent label is visible");

        // 2. Visually hidden radio associated with a sibling label via for="id"
        Assertions.assertTrue(dom.contains("<input id=\"gender-hidden\" name=\"gender\" type=\"radio\" checked=\"true\" value=\"male\""),
            "DOM should capture 0x0 radio input when its sibling label is visible");
    }
}
