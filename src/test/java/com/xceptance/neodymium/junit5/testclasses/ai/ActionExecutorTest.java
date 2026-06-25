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
package com.xceptance.neodymium.junit5.testclasses.ai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionRegistry;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * Test class for {@link ActionExecutor}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ActionExecutorTest
{

    private void resetRegistry() throws Exception {
        Field initializedField = ActionRegistry.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.set(null, false);

        Field pluginsField = ActionRegistry.class.getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        ((Map<?, ?>) pluginsField.get(null)).clear();
    }

    @BeforeEach
    public void setup() throws Exception {
        resetRegistry();
    }

    @AfterEach
    public void teardown() throws Exception {
        resetRegistry();
    }

    public static class MockActionPlugin implements AiActionPlugin {
        public boolean executeCalled = false;
        public boolean preCheckCalled = false;

        @Override
        public String getActionName() { return "MOCK"; }

        @Override
        public List<Action> parseDirectInstruction(String instruction) { return null; }

        @Override
        public boolean requiresLlm(Action action) { return false; }

        @Override
        public String getPromptInstructions() { return "Mock prompt"; }

        @Override
        public void execute(Action action, Object testInstance, ActionExecutor executor) {
            executeCalled = true;
        }

        @Override
        public void preCheck(Action action, ActionExecutor executor) {
            preCheckCalled = true;
        }
    }

    @Test
    public void testSuccessfulExecutionDelegation() {
        ActionRegistry.init();
        MockActionPlugin mockPlugin = new MockActionPlugin();
        ActionRegistry.register(mockPlugin);

        ActionExecutor executor = new ActionExecutor(this);
        Action action = new Action();
        action.setType("MOCK");
        action.setDescription("Mock action description");
        action.setTarget("mockTarget");

        executor.execute(action);

        Assertions.assertTrue(mockPlugin.executeCalled, "Plugin execute() method should have been delegated to");
        Assertions.assertTrue(mockPlugin.preCheckCalled, "Plugin preCheck() method should have been delegated to");
    }

    @Test
    public void testHandlingUnknownActions() {
        ActionRegistry.init();
        ActionExecutor executor = new ActionExecutor(this);
        Action action = new Action();
        action.setType("UNKNOWN_ACTION");
        action.setDescription("Unknown action description");
        action.setTarget("unknownTarget");

        // Should not throw an exception, but should handle it gracefully (logs warning)
        Assertions.assertDoesNotThrow(() -> {
            executor.execute(action);
        }, "Executing an unknown action should not throw an exception, but handle gracefully");
    }

    @Test
    public void testCleanElementTextSingleQuotes()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "button 'Save this card'";
        final String expected = "Save this card";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextDoubleQuotes()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "link \"Log in\"";
        final String expected = "Log in";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextUnquoted()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "unquoted text";
        final String expected = "unquoted text";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextNull()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String actual = executor.cleanElementText(null);
        Assertions.assertNull(actual);
    }

    /**
     * Tracker object to collect interactions with the mocked Selenium WebDriver and TargetLocator.
     *
     * @author AI-generated: Gemini 3.5 Flash
     * @author Xceptance GmbH 2026
     */
    private static final class WebDriverInvocationTracker
    {
        final List<String> calls = new ArrayList<>();
        final List<String> switchedWindows = new ArrayList<>();
        final List<Object> switchedFrames = new ArrayList<>();
        final List<String> foundCssSelectors = new ArrayList<>();
        final WebElement mockWebElement;

        WebDriverInvocationTracker(final WebElement mockWebElement)
        {
            this.mockWebElement = mockWebElement;
        }
    }

    /**
     * Helper to invoke switchFrameContext on ActionExecutor with a mocked WebDriver setup.
     *
     * @param targetFrameId the raw target frame ID string
     * @param activeHandles the set of active window handles to be returned by WebDriver
     * @return the tracker recording all invocations
     * @throws Exception if reflection fails
     */
        private WebDriverInvocationTracker invokeSwitchFrameContext(final String targetFrameId, final Set<String> activeHandles) throws Exception
    {
        final ActionExecutor executor = new ActionExecutor(this);

        final Class<?>[] webElementInterfaces = new Class<?>[] { WebElement.class };
        final WebElement mockWebElement = (WebElement) Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            webElementInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> {
                return null;
            }
        );

        final WebDriverInvocationTracker tracker = new WebDriverInvocationTracker(mockWebElement);
        final WebDriver[] driverHolder = new WebDriver[1];

        final Class<?>[] locatorInterfaces = new Class<?>[] { WebDriver.TargetLocator.class };
        final WebDriver.TargetLocator mockLocator = (WebDriver.TargetLocator) Proxy.newProxyInstance(
            WebDriver.TargetLocator.class.getClassLoader(),
            locatorInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> {
                final String methodName = method.getName();
                if ("window".equals(methodName))
                {
                    tracker.calls.add("window");
                    tracker.switchedWindows.add((String) args[0]);
                    return driverHolder[0];
                }
                else if ("defaultContent".equals(methodName))
                {
                    tracker.calls.add("defaultContent");
                    return driverHolder[0];
                }
                else if ("frame".equals(methodName))
                {
                    tracker.calls.add("frame");
                    tracker.switchedFrames.add(args[0]);
                    return driverHolder[0];
                }
                return null;
            }
        );

        final Class<?>[] driverInterfaces = new Class<?>[] { WebDriver.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            driverInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> {
                final String methodName = method.getName();
                if ("getWindowHandles".equals(methodName))
                {
                    tracker.calls.add("getWindowHandles");
                    return activeHandles;
                }
                else if ("switchTo".equals(methodName))
                {
                    tracker.calls.add("switchTo");
                    return mockLocator;
                }
                else if ("findElement".equals(methodName))
                {
                    tracker.calls.add("findElement");
                    if (args[0] instanceof By)
                    {
                        final By by = (By) args[0];
                        final String byStr = by.toString();
                        if (byStr.startsWith("By.cssSelector: "))
                        {
                            tracker.foundCssSelectors.add(byStr.substring("By.cssSelector: ".length()));
                        }
                    }
                    return mockWebElement;
                }
                return null;
            }
        );
        driverHolder[0] = mockDriver;

        final WebDriver originalDriver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        try
        {
            WebDriverRunner.setWebDriver(mockDriver);
            final Method method = ActionExecutor.class.getDeclaredMethod("switchFrameContext", String.class);
            method.setAccessible(true);
            method.invoke(executor, targetFrameId);
        }
        finally
        {
            if (originalDriver != null)
            {
                WebDriverRunner.setWebDriver(originalDriver);
            }
            else
            {
                WebDriverRunner.closeWebDriver();
            }
        }

        return tracker;
    }

    /**
     * Verifies switching frame context with nested selector path containing colons, e.g. CSS :nth-of-type selector.
     */
    @Test
    public void testSwitchFrameContextWithColonInCssSelector() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("win_0:main >>> iframe:nth-of-type(1)", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(0));
        Assertions.assertEquals(1, tracker.foundCssSelectors.size());
        Assertions.assertEquals("iframe:nth-of-type(1)", tracker.foundCssSelectors.get(0));
        Assertions.assertEquals(1, tracker.switchedFrames.size());
        Assertions.assertSame(tracker.mockWebElement, tracker.switchedFrames.get(0));
    }

    /**
     * Verifies switching frame context with a single CSS selector (no nested parent contexts).
     */
    @Test
    public void testSwitchFrameContextWithSingleCssSelector() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("win_0:iframe:nth-of-type(1)", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(0));
        Assertions.assertEquals(1, tracker.foundCssSelectors.size());
        Assertions.assertEquals("iframe:nth-of-type(1)", tracker.foundCssSelectors.get(0));
        Assertions.assertEquals(1, tracker.switchedFrames.size());
        Assertions.assertSame(tracker.mockWebElement, tracker.switchedFrames.get(0));
    }

    /**
     * Verifies switching frame context with a target containing window and main context (no nested frames).
     */
    @Test
    public void testSwitchFrameContextWindowAndMainOnly() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("win_0:main", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(0));
        Assertions.assertTrue(tracker.switchedFrames.isEmpty());
        Assertions.assertTrue(tracker.foundCssSelectors.isEmpty());
    }

    /**
     * Verifies switching frame context with only a window identifier, defaulting the frame path to main.
     */
    @Test
    public void testSwitchFrameContextWindowOnly() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");
        activeHandles.add("real_win_1");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("win_1", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_1", tracker.switchedWindows.get(0));
        Assertions.assertTrue(tracker.switchedFrames.isEmpty());
        Assertions.assertTrue(tracker.foundCssSelectors.isEmpty());
    }

    /**
     * Verifies switching frame context with a custom mapped window handle name.
     */
    @Test
    public void testSwitchFrameContextWithCustomWindowMapping() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("custom_stale_handle", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(0));
        Assertions.assertTrue(tracker.switchedFrames.isEmpty());
        Assertions.assertTrue(tracker.foundCssSelectors.isEmpty());
    }

    /**
     * Verifies that blank target frame IDs return early and do not trigger any window switching operations.
     */
    @Test
    public void testSwitchFrameContextWithEmptyAndNullTarget() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker trackerNull = invokeSwitchFrameContext(null, activeHandles);
        Assertions.assertTrue(trackerNull.calls.isEmpty());

        final WebDriverInvocationTracker trackerEmpty = invokeSwitchFrameContext("", activeHandles);
        Assertions.assertTrue(trackerEmpty.calls.isEmpty());
    }

    /**
     * Verifies switching frame context when frame path specifies nested numeric frame indices.
     */
    @Test
    public void testSwitchFrameContextWithNumericFrameIndices() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("real_win_0");

        final WebDriverInvocationTracker tracker = invokeSwitchFrameContext("win_0:0.2", activeHandles);

        Assertions.assertEquals(1, tracker.switchedWindows.size());
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(0));
        Assertions.assertEquals(2, tracker.switchedFrames.size());
        Assertions.assertEquals(0, tracker.switchedFrames.get(0));
        Assertions.assertEquals(2, tracker.switchedFrames.get(1));
    }

    /**
     * Verifies that if switching to a target window throws NoSuchWindowException,
     * the executor falls back to the first available open window.
     */
    @Test
    public void testSwitchFrameContextToleratesClosedWindow() throws Exception
    {
        final Set<String> activeHandles = new LinkedHashSet<>();
        activeHandles.add("closed_win");
        activeHandles.add("real_win_0");

        final ActionExecutor executor = new ActionExecutor(this);

        final Class<?>[] webElementInterfaces = new Class<?>[] { WebElement.class };
        final WebElement mockWebElement = (WebElement) Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            webElementInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> null
        );

        final WebDriverInvocationTracker tracker = new WebDriverInvocationTracker(mockWebElement);
        final WebDriver[] driverHolder = new WebDriver[1];

        final Class<?>[] locatorInterfaces = new Class<?>[] { WebDriver.TargetLocator.class };
        final WebDriver.TargetLocator mockLocator = (WebDriver.TargetLocator) Proxy.newProxyInstance(
            WebDriver.TargetLocator.class.getClassLoader(),
            locatorInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> {
                final String methodName = method.getName();
                if ("window".equals(methodName))
                {
                    final String target = (String) args[0];
                    tracker.calls.add("window");
                    tracker.switchedWindows.add(target);
                    if ("closed_win".equals(target))
                    {
                        throw new NoSuchWindowException("target window already closed");
                    }
                    return driverHolder[0];
                }
                else if ("defaultContent".equals(methodName))
                {
                    tracker.calls.add("defaultContent");
                    return driverHolder[0];
                }
                return null;
            }
        );

        final Class<?>[] driverInterfaces = new Class<?>[] { WebDriver.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            driverInterfaces,
            (final Object proxy, final Method method, final Object[] args) -> {
                final String methodName = method.getName();
                if ("getWindowHandles".equals(methodName))
                {
                    tracker.calls.add("getWindowHandles");
                    final Set<String> currentOpen = new LinkedHashSet<>(activeHandles);
                    if (tracker.switchedWindows.contains("closed_win"))
                    {
                        currentOpen.remove("closed_win");
                    }
                    return currentOpen;
                }
                else if ("switchTo".equals(methodName))
                {
                    tracker.calls.add("switchTo");
                    return mockLocator;
                }
                return null;
            }
        );
        driverHolder[0] = mockDriver;

        final WebDriver originalDriver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        try
        {
            WebDriverRunner.setWebDriver(mockDriver);
            final Method method = ActionExecutor.class.getDeclaredMethod("switchFrameContext", String.class);
            method.setAccessible(true);
            method.invoke(executor, "closed_win:main");
        }
        finally
        {
            if (originalDriver != null)
            {
                WebDriverRunner.setWebDriver(originalDriver);
            }
            else
            {
                WebDriverRunner.closeWebDriver();
            }
        }

        Assertions.assertEquals(2, tracker.switchedWindows.size());
        Assertions.assertEquals("closed_win", tracker.switchedWindows.get(0));
        Assertions.assertEquals("real_win_0", tracker.switchedWindows.get(1));
    }
}
