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

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideTargetLocator;
import com.codeborne.selenide.WebDriverRunner;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
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

        // Both 'hidden' and 'invisible' should pass via Condition.hidden / Condition.or
        plugin.execute(new Action("ASSERT", "#hidden-div", "hidden", "verify hidden", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "invisible", "verify invisible", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "[hidden]", "verify [hidden]", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "absent", "verify absent", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-div", "[absent]", "verify [absent]", "reasoning", false));
    }

    @Test
    public void testLegacyAssertUnfocusedWithAsyncFocusPolling() throws Exception
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
                        return "async-unfocused-input";
                    }
                    return null;
                }
        );

        final AtomicInteger jsCallCount = new AtomicInteger(0);

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
                            // First call reports focused, subsequent calls report unfocused
                            return jsCallCount.incrementAndGet() == 1 ? Boolean.TRUE : Boolean.FALSE;
                        }
                        return null;
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

        final long origTimeout = Configuration.timeout;
        final long origPoll = Configuration.pollingInterval;
        Configuration.timeout = 1000;
        Configuration.pollingInterval = 20;
        try
        {
            final AssertAction plugin = new AssertAction();
            // Polling should allow it to succeed when JS flips from true to false
            plugin.execute(new Action("ASSERT", "#async-unfocused-input", "unfocused", "verify unfocused", "reasoning", false));
            Assertions.assertTrue(jsCallCount.get() >= 2, "Polling must have retried JS check at least once");
        }
        finally
        {
            Configuration.timeout = origTimeout;
            Configuration.pollingInterval = origPoll;
        }
    }

    @Test
    public void testLegacyAssertFocusedWithAsyncFocusPolling() throws Exception
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
                        return "async-focused-input";
                    }
                    return null;
                }
        );

        final AtomicInteger jsCallCount = new AtomicInteger(0);

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
                            // First call reports false (not yet focused), subsequent calls report true (focused)
                            return jsCallCount.incrementAndGet() == 1 ? Boolean.FALSE : Boolean.TRUE;
                        }
                        return null;
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

        final long origTimeout = Configuration.timeout;
        final long origPoll = Configuration.pollingInterval;
        Configuration.timeout = 1000;
        Configuration.pollingInterval = 20;
        try
        {
            final AssertAction plugin = new AssertAction();
            // Polling should allow it to succeed when JS flips from false to true
            plugin.execute(new Action("ASSERT", "#async-focused-input", "focused", "verify focused", "reasoning", false));
            Assertions.assertTrue(jsCallCount.get() >= 2, "Polling must have retried JS check at least once");
        }
        finally
        {
            Configuration.timeout = origTimeout;
            Configuration.pollingInterval = origPoll;
        }
    }

    @Test
    public void testFocusedAndUnfocusedRethrowStaleElementReferenceExceptionFromSwitchTo()
    {
        final WebElement staleElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    throw new StaleElementReferenceException("Element is stale");
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("switchTo".equals(name))
                    {
                        return (WebDriver.TargetLocator) Proxy.newProxyInstance(
                                AssertActionPluginTest.class.getClassLoader(),
                                new Class<?>[]{WebDriver.TargetLocator.class},
                                (p, m, a) ->
                                {
                                    throw new StaleElementReferenceException("Active element is stale");
                                }
                        );
                    }
                    return null;
                }
        );

        final Driver selenideDriver = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    return null;
                }
        );

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.UNFOCUSED.check(selenideDriver, staleElement);
        });

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.FOCUSED.check(selenideDriver, staleElement);
        });
    }

    @Test
    public void testFocusedAndUnfocusedRethrowStaleElementReferenceExceptionFromExecuteJavaScript()
    {
        final WebElement normalElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return false;
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

        // Driver throwing direct StaleElementReferenceException from executeJavaScript
        final Driver selenideDriver = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    if ("executeJavaScript".equals(name))
                    {
                        throw new StaleElementReferenceException("Script execution element is stale");
                    }
                    return null;
                }
        );

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.UNFOCUSED.check(selenideDriver, normalElement);
        });

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.FOCUSED.check(selenideDriver, normalElement);
        });

        // Driver throwing wrapped JavascriptException with stale message
        final Driver selenideDriverWrapped = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    if ("executeJavaScript".equals(name))
                    {
                        throw new JavascriptException("javascript error: stale element reference in document");
                    }
                    return null;
                }
        );

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.UNFOCUSED.check(selenideDriverWrapped, normalElement);
        });

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.FOCUSED.check(selenideDriverWrapped, normalElement);
        });

        // Driver throwing wrapped JavascriptException with uppercase Stale message
        final Driver selenideDriverUppercaseStale = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    if ("executeJavaScript".equals(name))
                    {
                        throw new JavascriptException("JavaScript error: StaleElementReferenceError: The element is stale");
                    }
                    return null;
                }
        );

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.UNFOCUSED.check(selenideDriverUppercaseStale, normalElement);
        });

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.FOCUSED.check(selenideDriverUppercaseStale, normalElement);
        });

        // Driver throwing wrapped JavascriptException with "not attached" message
        final Driver selenideDriverNotAttached = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    if ("executeJavaScript".equals(name))
                    {
                        throw new JavascriptException("javascript error: element is not attached to the page document");
                    }
                    return null;
                }
        );

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.UNFOCUSED.check(selenideDriverNotAttached, normalElement);
        });

        Assertions.assertThrows(StaleElementReferenceException.class, () ->
        {
            AssertAction.FOCUSED.check(selenideDriverNotAttached, normalElement);
        });
    }

    @Test
    public void testFocusedConditionFailureMessageReturnsNotFocused()
    {
        final WebElement normalElement = (WebElement) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("equals".equals(name))
                    {
                        return false;
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

        final Driver selenideDriver = (Driver) Proxy.newProxyInstance(
                AssertActionPluginTest.class.getClassLoader(),
                new Class<?>[]{Driver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("getWebDriver".equals(name))
                    {
                        return mockDriver;
                    }
                    if ("config".equals(name))
                    {
                        return new SelenideConfig();
                    }
                    if ("switchTo".equals(name))
                    {
                        return new SelenideTargetLocator((Driver) proxy);
                    }
                    if ("executeJavaScript".equals(name))
                    {
                        return Boolean.FALSE;
                    }
                    return null;
                }
        );

        final CheckResult result = AssertAction.FOCUSED.check(selenideDriver, normalElement);
        Assertions.assertEquals(CheckResult.Verdict.REJECT, result.verdict(), "FOCUSED must be rejected when element is unfocused");
        Assertions.assertEquals("not focused", result.actualValue(), "Failure actual value must be 'not focused'");
    }
}
