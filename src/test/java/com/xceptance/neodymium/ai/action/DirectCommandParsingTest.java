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
package com.xceptance.neodymium.ai.action;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import com.xceptance.neodymium.ai.action.plugins.ClickAction;
import com.xceptance.neodymium.ai.action.plugins.TypeAction;
import com.xceptance.neodymium.ai.action.plugins.SelectAction;
import com.xceptance.neodymium.ai.action.plugins.ClearAction;
import com.xceptance.neodymium.ai.action.plugins.HoverAction;
import com.xceptance.neodymium.ai.action.plugins.WaitAction;
import com.xceptance.neodymium.ai.action.plugins.ScrollAction;
import com.xceptance.neodymium.ai.action.plugins.KeyPressAction;
import com.xceptance.neodymium.ai.action.plugins.NavigateAction;
import com.xceptance.neodymium.ai.action.plugins.BackAction;
import com.xceptance.neodymium.ai.action.plugins.ForwardAction;
import com.xceptance.neodymium.ai.action.plugins.RefreshAction;
import com.xceptance.neodymium.ai.action.plugins.ClearCookiesAction;

/**
 * Pure unit tests verifying direct command shortcut parsing logic across all interactive plugins
 * and the shared SelectorParser utility.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class DirectCommandParsingTest
{
    @Test
    public void testSelectorParser()
    {
        final SelectorParser.ParsedSelector cssBare = SelectorParser.parse("#btn");
        assertEquals(SelectorParser.SelectorType.CSS, cssBare.getType());
        assertEquals("#btn", cssBare.getExpression());

        final SelectorParser.ParsedSelector cssPrefixed = SelectorParser.parse("css=.button");
        assertEquals(SelectorParser.SelectorType.CSS, cssPrefixed.getType());
        assertEquals(".button", cssPrefixed.getExpression());

        final SelectorParser.ParsedSelector xpathBare = SelectorParser.parse("xpath=//div[@id='foo']");
        assertEquals(SelectorParser.SelectorType.XPATH, xpathBare.getType());
        assertEquals("//div[@id='foo']", xpathBare.getExpression());

        final SelectorParser.ParsedSelector xpathAutoWrap = SelectorParser.parse("xpath=div[@id='foo']");
        assertEquals(SelectorParser.SelectorType.XPATH, xpathAutoWrap.getType());
        assertEquals("(div[@id='foo'])", xpathAutoWrap.getExpression());

        assertThrows(IllegalArgumentException.class, () -> 
        {
            SelectorParser.parse(null);
        });
        assertThrows(IllegalArgumentException.class, () -> 
        {
            SelectorParser.parse("   ");
        });
        assertThrows(IllegalArgumentException.class, () -> 
        {
            SelectorParser.parse("xpath=");
        });
        assertThrows(IllegalArgumentException.class, () -> 
        {
            SelectorParser.parse("css=");
        });
    }

    @Test
    public void testClickAction()
    {
        final ClickAction plugin = new ClickAction();

        final List<Action> actions = plugin.parseDirectInstruction("CLICK #btn");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("CLICK", actions.get(0).getType());
        assertEquals("#btn", actions.get(0).getTarget());

        final List<Action> actionsWhitespace = plugin.parseDirectInstruction("  CLICK   xpath=//button   ");
        assertNotNull(actionsWhitespace);
        assertEquals(1, actionsWhitespace.size());
        assertEquals("CLICK", actionsWhitespace.get(0).getType());
        assertEquals("//button", actionsWhitespace.get(0).getTarget());

        assertNull(plugin.parseDirectInstruction("Click #btn"));
        assertNull(plugin.parseDirectInstruction("CLICK"));
        assertNull(plugin.parseDirectInstruction("CLICK "));
    }

    @Test
    public void testTypeAction()
    {
        final TypeAction plugin = new TypeAction();

        final List<Action> actionsQuoted = plugin.parseDirectInstruction("TYPE \"john@example.com\" into #email");
        assertNotNull(actionsQuoted);
        assertEquals(1, actionsQuoted.size());
        assertEquals("TYPE", actionsQuoted.get(0).getType());
        assertEquals("john@example.com", actionsQuoted.get(0).getValue());
        assertEquals("#email", actionsQuoted.get(0).getTarget());

        final List<Action> actionsUnquoted = plugin.parseDirectInstruction("TYPE hello into css=#id");
        assertNotNull(actionsUnquoted);
        assertEquals(1, actionsUnquoted.size());
        assertEquals("TYPE", actionsUnquoted.get(0).getType());
        assertEquals("hello", actionsUnquoted.get(0).getValue());
        assertEquals("#id", actionsUnquoted.get(0).getTarget());

        final List<Action> actionsCaseInsensitiveSeparator = plugin.parseDirectInstruction("TYPE \"val\" INTO #id");
        assertNotNull(actionsCaseInsensitiveSeparator);
        assertEquals(1, actionsCaseInsensitiveSeparator.size());
        assertEquals("val", actionsCaseInsensitiveSeparator.get(0).getValue());
        assertEquals("#id", actionsCaseInsensitiveSeparator.get(0).getTarget());

        assertNull(plugin.parseDirectInstruction("Type \"val\" into #id"));
        assertNull(plugin.parseDirectInstruction("TYPE"));
        assertNull(plugin.parseDirectInstruction("TYPE \"val\" into"));
    }

    @Test
    public void testSelectAction()
    {
        final SelectAction plugin = new SelectAction();

        final List<Action> actionsQuoted = plugin.parseDirectInstruction("SELECT \"United States\" in #country");
        assertNotNull(actionsQuoted);
        assertEquals(1, actionsQuoted.size());
        assertEquals("SELECT", actionsQuoted.get(0).getType());
        assertEquals("United States", actionsQuoted.get(0).getValue());
        assertEquals("#country", actionsQuoted.get(0).getTarget());

        final List<Action> actionsUnquoted = plugin.parseDirectInstruction("SELECT red in #color");
        assertNotNull(actionsUnquoted);
        assertEquals(1, actionsUnquoted.size());
        assertEquals("SELECT", actionsUnquoted.get(0).getType());
        assertEquals("red", actionsUnquoted.get(0).getValue());
        assertEquals("#color", actionsUnquoted.get(0).getTarget());

        final List<Action> actionsCaseInsensitiveSeparator = plugin.parseDirectInstruction("SELECT \"val\" IN #id");
        assertNotNull(actionsCaseInsensitiveSeparator);
        assertEquals(1, actionsCaseInsensitiveSeparator.size());
        assertEquals("val", actionsCaseInsensitiveSeparator.get(0).getValue());
        assertEquals("#id", actionsCaseInsensitiveSeparator.get(0).getTarget());

        assertNull(plugin.parseDirectInstruction("Select \"val\" in #id"));
        assertNull(plugin.parseDirectInstruction("SELECT"));
        assertNull(plugin.parseDirectInstruction("SELECT \"val\" in"));
    }

    @Test
    public void testClearAction()
    {
        final ClearAction plugin = new ClearAction();

        final List<Action> actions = plugin.parseDirectInstruction("CLEAR #input");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("CLEAR", actions.get(0).getType());
        assertEquals("#input", actions.get(0).getTarget());

        assertNull(plugin.parseDirectInstruction("Clear #input"));
        assertNull(plugin.parseDirectInstruction("CLEAR"));
        assertNull(plugin.parseDirectInstruction("CLEAR "));
    }

    @Test
    public void testHoverAction()
    {
        final HoverAction plugin = new HoverAction();

        final List<Action> actions = plugin.parseDirectInstruction("HOVER #menu");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("HOVER", actions.get(0).getType());
        assertEquals("#menu", actions.get(0).getTarget());

        assertNull(plugin.parseDirectInstruction("Hover #menu"));
        assertNull(plugin.parseDirectInstruction("HOVER"));
        assertNull(plugin.parseDirectInstruction("HOVER "));
    }

    @Test
    public void testWaitAction()
    {
        final WaitAction plugin = new WaitAction();

        final List<Action> actionsMs = plugin.parseDirectInstruction("WAIT 500");
        assertNotNull(actionsMs);
        assertEquals(1, actionsMs.size());
        assertEquals("WAIT", actionsMs.get(0).getType());
        assertEquals("500", actionsMs.get(0).getValue());

        final List<Action> actionsSec = plugin.parseDirectInstruction("WAIT 2.5s");
        assertNotNull(actionsSec);
        assertEquals(1, actionsSec.size());
        assertEquals("WAIT", actionsSec.get(0).getType());
        assertEquals("2500", actionsSec.get(0).getValue());

        assertNull(plugin.parseDirectInstruction("Wait 500"));
        assertNull(plugin.parseDirectInstruction("WAIT abc"));
    }

    @Test
    public void testScrollAction()
    {
        final ScrollAction plugin = new ScrollAction();

        final List<Action> actionsTarget = plugin.parseDirectInstruction("SCROLL #footer");
        assertNotNull(actionsTarget);
        assertEquals(1, actionsTarget.size());
        assertEquals("SCROLL", actionsTarget.get(0).getType());
        assertEquals("#footer", actionsTarget.get(0).getTarget());

        final List<Action> actionsDirection = plugin.parseDirectInstruction("SCROLL down");
        assertNotNull(actionsDirection);
        assertEquals(1, actionsDirection.size());
        assertEquals("SCROLL", actionsDirection.get(0).getType());
        assertEquals("down", actionsDirection.get(0).getTarget());
        assertNull(actionsDirection.get(0).getValue());

        assertNull(plugin.parseDirectInstruction("Scroll #footer"));
        assertNull(plugin.parseDirectInstruction("SCROLL"));
        assertNull(plugin.parseDirectInstruction("SCROLL "));
    }

    @Test
    public void testKeyPressAction()
    {
        final KeyPressAction plugin = new KeyPressAction();

        final List<Action> actionsSingle = plugin.parseDirectInstruction("KEYPRESS Enter");
        assertNotNull(actionsSingle);
        assertEquals(1, actionsSingle.size());
        assertEquals("KEY_PRESS", actionsSingle.get(0).getType());
        assertEquals("Enter", actionsSingle.get(0).getValue());

        final List<Action> actionsCombo = plugin.parseDirectInstruction("KEYPRESS Ctrl+A");
        assertNotNull(actionsCombo);
        assertEquals(1, actionsCombo.size());
        assertEquals("KEY_PRESS", actionsCombo.get(0).getType());
        assertEquals("Ctrl+A", actionsCombo.get(0).getValue());

        final List<Action> actionsList = plugin.parseDirectInstruction("KEYPRESS Tab, Tab, Enter");
        assertNotNull(actionsList);
        assertEquals(3, actionsList.size());
        assertEquals("KEY_PRESS", actionsList.get(0).getType());
        assertEquals("Tab", actionsList.get(0).getValue());
        assertEquals("KEY_PRESS", actionsList.get(1).getType());
        assertEquals("Tab", actionsList.get(1).getValue());
        assertEquals("KEY_PRESS", actionsList.get(2).getType());
        assertEquals("Enter", actionsList.get(2).getValue());

        assertNull(plugin.parseDirectInstruction("KeyPress Enter"));
        assertNull(plugin.parseDirectInstruction("KEYPRESS"));
        assertNull(plugin.parseDirectInstruction("KEYPRESS "));
    }

    @Test
    public void testNavigateAction()
    {
        final NavigateAction plugin = new NavigateAction();

        final List<Action> actionsOpen = plugin.parseDirectInstruction("OPEN http://example.com");
        assertNotNull(actionsOpen);
        assertEquals(1, actionsOpen.size());
        assertEquals("NAVIGATE", actionsOpen.get(0).getType());
        assertEquals("http://example.com", actionsOpen.get(0).getValue());

        final List<Action> actionsNavigate = plugin.parseDirectInstruction("NAVIGATE http://example.com");
        assertNotNull(actionsNavigate);
        assertEquals(1, actionsNavigate.size());
        assertEquals("NAVIGATE", actionsNavigate.get(0).getType());
        assertEquals("http://example.com", actionsNavigate.get(0).getValue());

        final List<Action> actionsBasicAuth = plugin.parseDirectInstruction(
            "OPEN http://example.com with basic auth user \"admin\" password \"secret\"");
        assertNotNull(actionsBasicAuth);
        assertEquals(1, actionsBasicAuth.size());
        assertEquals("NAVIGATE", actionsBasicAuth.get(0).getType());
        assertEquals("http://example.com", actionsBasicAuth.get(0).getTarget());
        assertEquals("admin", actionsBasicAuth.get(0).getValues().get(0));
        assertEquals("secret", actionsBasicAuth.get(0).getValues().get(1));

        assertNull(plugin.parseDirectInstruction("Open http://example.com"));
        assertNull(plugin.parseDirectInstruction("OPEN"));
    }

    @Test
    public void testBackAction()
    {
        final BackAction plugin = new BackAction();
        final List<Action> actions = plugin.parseDirectInstruction("BACK");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("BACK", actions.get(0).getType());

        assertNull(plugin.parseDirectInstruction("Back"));
    }

    @Test
    public void testForwardAction()
    {
        final ForwardAction plugin = new ForwardAction();
        final List<Action> actions = plugin.parseDirectInstruction("FORWARD");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("FORWARD", actions.get(0).getType());

        assertNull(plugin.parseDirectInstruction("Forward"));
    }

    @Test
    public void testRefreshAction()
    {
        final RefreshAction plugin = new RefreshAction();
        final List<Action> actions = plugin.parseDirectInstruction("REFRESH");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("REFRESH", actions.get(0).getType());

        assertNull(plugin.parseDirectInstruction("Refresh"));
    }

    @Test
    public void testClearCookiesAction()
    {
        final ClearCookiesAction plugin = new ClearCookiesAction();
        final List<Action> actions = plugin.parseDirectInstruction("CLEAR_COOKIES");
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("CLEAR_COOKIES", actions.get(0).getType());

        assertNull(plugin.parseDirectInstruction("Clear_Cookies"));
    }
}
