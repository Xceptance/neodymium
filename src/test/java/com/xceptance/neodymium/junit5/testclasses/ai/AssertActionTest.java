/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// AI-generated: Gemini 3.5 Flash

package com.xceptance.neodymium.junit5.testclasses.ai;

import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.plugins.AssertAction;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Unit/Integration test suite for {@link AssertAction} to verify regex and literal assertion behavior.
 */
@Browser("Chrome_headless")
public class AssertActionTest
{
    /**
     * Verifies that literal text assertions successfully match when the element text is identical.
     */
    @NeodymiumTest
    public final void testLiteralAssertionSuccess()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element'>Order ID: ORD-1779265279577</div></body></html>");

        final AssertAction assertAction = new AssertAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("ASSERT");
        action.setTarget("#test-element");
        action.setValue(List.of("ORD-1779265279577"));

        Assertions.assertDoesNotThrow(() ->
        {
            assertAction.execute(action, this, executor);
        });
    }

    /**
     * Verifies that regex assertions successfully match dynamic content on elements.
     */
    @NeodymiumTest
    public final void testRegexAssertionSuccess()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element'>Order ID: ORD-1779265279577</div></body></html>");

        final AssertAction assertAction = new AssertAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("ASSERT");
        action.setTarget("#test-element");
        action.setValue(List.of("ORD-\\d+"));

        Assertions.assertDoesNotThrow(() ->
        {
            assertAction.execute(action, this, executor);
        });
    }

    /**
     * Verifies that regex assertions successfully match dynamic content inside common attributes like value.
     */
    @NeodymiumTest
    public final void testRegexAssertionValueSuccess()
    {
        Selenide.open("data:text/html,<html><body><input id='test-input' value='ORD-1779265279577'/></body></html>");

        final AssertAction assertAction = new AssertAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("ASSERT");
        action.setTarget("#test-input");
        action.setValue(List.of("ORD-\\d+"));

        Assertions.assertDoesNotThrow(() ->
        {
            assertAction.execute(action, this, executor);
        });
    }

    /**
     * Verifies that regex assertions successfully match dynamic content in custom data-attributes.
     */
    @NeodymiumTest
    public final void testRegexAssertionDataAttributeSuccess()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element' data-neo-ref='xc_order' data-order='ORD-1779265279577'>Success</div></body></html>");

        final AssertAction assertAction = new AssertAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("ASSERT");
        action.setTarget("#test-element");
        action.setValue(List.of("ORD-\\d+"));

        Assertions.assertDoesNotThrow(() ->
        {
            assertAction.execute(action, this, executor);
        });
    }

    /**
     * Verifies that regex assertions throw AssertionError when the element content does not match the regex pattern.
     */
    @NeodymiumTest
    public final void testRegexAssertionFailure()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element'>Order ID: ORD-1779265279577</div></body></html>");

        final AssertAction assertAction = new AssertAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("ASSERT");
        action.setTarget("#test-element");
        action.setValue(List.of("ORD-[a-zA-Z]+"));

        Assertions.assertThrows(AssertionError.class, () ->
        {
            assertAction.execute(action, this, executor);
        });
    }
}
