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

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;

/**
 * Unit tests for {@link AssertAction} verifying legacy state assertions,
 * compound state negation inversions (including editable vs readonly),
 * and space-separated state normalizations.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class AssertActionPluginTest
{
    private interface MockJsDriver extends WebDriver, JavascriptExecutor, Interactive
    {
    }

    private long originalTimeout;

    @BeforeEach
    public void setUp()
    {
        this.originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
    }

    @AfterEach
    public void tearDown()
    {
        Configuration.timeout = this.originalTimeout;
        if (WebDriverRunner.hasWebDriverStarted())
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testLegacyAssertExistsAndPresent() throws Exception
    {
        final WebElement existingElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("getAttribute".equals(name) && "id".equals(args[0]))
                    {
                        return "test-btn";
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElement".equals(name))
                    {
                        return existingElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(existingElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AssertAction plugin = new AssertAction();

        // Both 'present' and 'exists' should pass via Condition.exist
        plugin.execute(new Action("ASSERT", "#test-btn", "present", "verify present", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#test-btn", "exists", "verify exists", "reasoning", false));
    }

    @Test
    public void testCompoundStateNegationEditableInvertsToReadonly() throws Exception
    {
        final WebElement readonlyElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "input";
                    }
                    if ("getAttribute".equals(name))
                    {
                        final String attr = (String) args[0];
                        if ("readonly".equals(attr) || "readOnly".equals(attr))
                        {
                            return "true";
                        }
                        if ("id".equals(attr))
                        {
                            return "locked-input";
                        }
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElement".equals(name))
                    {
                        return readonlyElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(readonlyElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AssertAction plugin = new AssertAction();

        // Asserting 'editable' with isNegated=true must invert to 'readonly' and SUCCEED
        plugin.execute(new Action("ASSERT_ELEMENT_STATE", "#locked-input", List.of("editable"), "verify not editable", "reasoning", false, true));

        // Asserting 'readonly' with isNegated=true must invert to 'editable' and FAIL on readonly element
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT_ELEMENT_STATE", "#locked-input", List.of("readonly"), "verify not readonly", "fail", false, true));
        });
    }

    @Test
    public void testLegacyAssertUnfocused() throws Exception
    {
        final WebElement unfocusedElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "input";
                    }
                    if ("getAttribute".equals(name) && "id".equals(args[0]))
                    {
                        return "unfocused-input";
                    }
                    return null;
                }
        );

        final WebElement otherActiveElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("getTagName".equals(name))
                    {
                        return "body";
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElement".equals(name))
                    {
                        return unfocusedElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(unfocusedElement);
                    }
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("==="))
                        {
                            return Boolean.FALSE;
                        }
                        return otherActiveElement;
                    }
                    if ("switchTo".equals(name))
                    {
                        return (WebDriver.TargetLocator) Proxy.newProxyInstance(
                                AssertActionPluginTest.class.getClassLoader(),
                                new Class<?>[]{WebDriver.TargetLocator.class},
                                (p, m, a) -> null
                        );
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AssertAction plugin = new AssertAction();
        plugin.execute(new Action("ASSERT", "#unfocused-input", "unfocused", "verify unfocused", "reasoning", false));
    }

    @Test
    public void testLegacyAssertUnfocusedFailsWhenFocused() throws Exception
    {
        final WebElement focusedElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "input";
                    }
                    if ("getAttribute".equals(name) && "id".equals(args[0]))
                    {
                        return "focused-input";
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElement".equals(name))
                    {
                        return focusedElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(focusedElement);
                    }
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("==="))
                        {
                            return Boolean.TRUE;
                        }
                        return focusedElement;
                    }
                    if ("switchTo".equals(name))
                    {
                        return (WebDriver.TargetLocator) Proxy.newProxyInstance(
                                AssertActionPluginTest.class.getClassLoader(),
                                new Class<?>[]{WebDriver.TargetLocator.class},
                                (p, m, a) -> null
                        );
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AssertAction plugin = new AssertAction();
        Assertions.assertThrows(AssertionError.class, () ->
        {
            plugin.execute(new Action("ASSERT", "#focused-input", "unfocused", "verify unfocused", "reasoning", false));
        });
    }

    @Test
    public void testLegacyAssertHiddenAndInvisible() throws Exception
    {
        final WebElement hiddenElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("isDisplayed".equals(name))
                    {
                        // Element is hidden
                        return false;
                    }
                    if ("isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "div";
                    }
                    if ("getAttribute".equals(name) && "id".equals(args[0]))
                    {
                        return "hidden-div";
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElement".equals(name))
                    {
                        return hiddenElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(hiddenElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AssertAction plugin = new AssertAction();

        // Both 'hidden' and 'invisible' should pass via Condition.hidden
        plugin.execute(new Action("ASSERT", "#hidden-div", "hidden", "verify hidden", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "invisible", "verify invisible", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "[hidden]", "verify [hidden]", "reasoning", false));
    }
}
