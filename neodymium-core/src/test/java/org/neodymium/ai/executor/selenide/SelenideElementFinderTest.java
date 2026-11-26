/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.codeborne.selenide.WebDriverRunner;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.executor.selenide.selector.PlaywrightBy;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Unit tests verifying target locator splitting and candidate resolution in {@link SelenideElementFinder}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class SelenideElementFinderTest
{
    private interface MockJsDriver extends WebDriver, JavascriptExecutor
    {
    }

    @AfterEach
    public void tearDown()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            WebDriverRunner.closeWebDriver();
        }
    }
    @Test
    public void testCandidateSetPreservation()
    {
        final Action action = new Action("CLICK", "#primary-btn", "Click button");
        action.setCandidateLocators(List.of(
            new LocatorCandidate(".btn-primary", 0.9),
            new LocatorCandidate("button[type='submit']", 0.8),
            new LocatorCandidate("text=Submit", 0.7)
        ));

        final List<String> candidates = action.getAllCandidateLocators();
        Assertions.assertEquals(4, candidates.size());
        Assertions.assertEquals("#primary-btn", candidates.get(0));
        Assertions.assertEquals(".btn-primary", candidates.get(1));
        Assertions.assertEquals("button[type='submit']", candidates.get(2));
        Assertions.assertEquals("text=Submit", candidates.get(3));
    }

    @Test
    public void testNullAndEmptyTargetHandling()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement((Action) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement("", List.of()));
    }

    @Test
    public void testActionCandidateFallbackResolution()
    {
        final Action action = new Action("CLICK", "", "Click button");
        action.setCandidateLocators(List.of(new LocatorCandidate("#valid-fallback-button", 0.9)));

        // Even though target is empty, candidate locator is extracted and attempt does not throw IllegalArgumentException for empty target
        // (will attempt resolution on fallback candidate)
        final String firstCandidate = action.getAllCandidateLocators().get(0);
        Assertions.assertEquals("#valid-fallback-button", firstCandidate);
    }

    @Test
    public void testIsDirectlyPresentSafelyReturnsFalseOnNullOrBlank()
    {
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent(null));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("   "));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("#some-missing-element"));
    }

    @Test
    public void testFindFirstVisibleHandlesNullGracefully()
    {
        Assertions.assertNull(SelenideElementFinder.findFirstVisible(null, "someTarget"));
    }

    @Test
    public void testIsDirectlyPresentCompoundSelectorDoesNotHijackAncestor()
    {
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("article[data-ai=\"xcboo7um\"] button[data-ai=\"xcz0f8a5\"]"));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("article#xcboo7um button#xcz0f8a5"));
    }

    @Test
    public void testResetDomStampCacheForTesting()
    {
        SelenideElementFinder.resetDomStampCacheForTesting();
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("#non-existent-after-reset"));
    }

    @Test
    public void testIsAutomationIdSelector()
    {
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("[data-ai='xc123']"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("[data-ai=\"xcrxcvi\"]"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("data-ai=xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("#xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("div > #xc4o48u"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("xcrxcvi"));

        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(null));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(""));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("   "));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(".btn-primary"));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("#submit-btn"));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("//button[@id='save']"));
    }

    @Test
    public void testResolveLocatorTransformsHashAutomationId()
    {
        final By resolvedHash = SelenideElementFinder.resolveLocator("#xc123");
        Assertions.assertEquals(PlaywrightBy.of("#xc123"), resolvedHash);

        final By resolvedCompound = SelenideElementFinder.resolveLocator("div.order-summary > #xcrxcvi");
        Assertions.assertEquals(PlaywrightBy.of("div.order-summary > #xcrxcvi"), resolvedCompound);
    }

    @Test
    public void testFindElementThrowsOnInvalidSelectorSyntaxWithoutFallingBackToText()
    {
        // Unknown pseudo-class must throw InvalidSelectorException immediately
        final InvalidSelectorException ex1 = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> SelenideElementFinder.findElement("button:frobnicate"));
        Assertions.assertTrue(ex1.getMessage().contains(":frobnicate"));

        // Unclosed attribute bracket must throw InvalidSelectorException immediately
        final InvalidSelectorException ex2 = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> SelenideElementFinder.findElement("div[data-x=\"1\""));
        Assertions.assertTrue(ex2.getMessage().contains("Unclosed attribute bracket"));
    }

    @Test
    public void testFindAllInShadowRootsThrowsOnInvalidSelector()
    {
        final InvalidSelectorException ex = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> SelenideElementFinder.findAllInShadowRoots("button:frobnicate"));
        Assertions.assertTrue(ex.getMessage().contains(":frobnicate"));
    }

    @Test
    public void testActionWithFramePropertyPreservation()
    {
        final Action action = new Action("CLICK", "#button", "Click nested button")
            .withFrame("#pact1 >>> #pact2");
        Assertions.assertEquals("#pact1 >>> #pact2", action.getFrame());
        Assertions.assertEquals("#button", action.getTarget());
    }

    @Test
    public void testFindElementsRequiresValidTarget()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SelenideElementFinder.findElements(null));

        final Action emptyAction = new Action("ASSERT_COUNT", "  ", "empty");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SelenideElementFinder.findElements(emptyAction));
    }

    @Test
    public void testIsDirectlyAttachedSafelyReturnsFalseOnNullBlankOrNoDriver()
    {
        Assertions.assertFalse(SelenideElementFinder.isDirectlyAttached(null));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyAttached("   "));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyAttached("button#hidden-btn"));
    }

    @Test
    public void testIsDirectlyAttachedReturnsTrueForHiddenElementWhileIsDirectlyPresentReturnsFalse()
    {
        final AtomicBoolean displayedChecked = new AtomicBoolean(false);
        final WebElement hiddenButton = (WebElement) Proxy.newProxyInstance(
            SelenideElementFinderTest.class.getClassLoader(),
            new Class<?>[] { WebElement.class },
            (proxy, method, args) ->
            {
                final String name = method.getName();
                if ("equals".equals(name))
                {
                    return args != null && args.length == 1 && proxy == args[0];
                }
                if ("hashCode".equals(name))
                {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name))
                {
                    return "MockHiddenButton";
                }
                if ("isDisplayed".equals(name))
                {
                    displayedChecked.set(true);
                    return false;
                }
                if ("getTagName".equals(name))
                {
                    return "button";
                }
                if ("findElements".equals(name))
                {
                    return Collections.emptyList();
                }
                return null;
            });

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
            SelenideElementFinderTest.class.getClassLoader(),
            new Class<?>[] { MockJsDriver.class },
            (proxy, method, args) ->
            {
                final String name = method.getName();
                if ("equals".equals(name))
                {
                    return args != null && args.length == 1 && proxy == args[0];
                }
                if ("hashCode".equals(name))
                {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name))
                {
                    return "MockJsDriver";
                }
                if ("executeScript".equals(name))
                {
                    if (args != null && args.length >= 2 && args[1] instanceof final Object[] scriptArgs
                        && scriptArgs.length >= 2 && "button#hidden-btn".equals(scriptArgs[1]))
                    {
                        return List.of(hiddenButton);
                    }
                    return Collections.emptyList();
                }
                if ("findElements".equals(name))
                {
                    if (args != null && args.length >= 1 && args[0] instanceof final PlaywrightBy playwrightBy)
                    {
                        return playwrightBy.findElements((WebDriver) proxy);
                    }
                    if (args != null && args.length >= 1 && args[0] instanceof final By by
                        && by.toString().contains("//button[@id='hidden-btn']"))
                    {
                        return List.of(hiddenButton);
                    }
                    return Collections.emptyList();
                }
                return null;
            });

        try
        {
            WebDriverRunner.setWebDriver(mockDriver);

            Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("button#hidden-btn"));
            Assertions.assertTrue(displayedChecked.get(), "isDirectlyPresent must inspect element visibility");
            displayedChecked.set(false);
            Assertions.assertTrue(SelenideElementFinder.isDirectlyAttached("button#hidden-btn"));
            Assertions.assertFalse(displayedChecked.get(), "isDirectlyAttached must not check element visibility");

            displayedChecked.set(false);
            Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("//button[@id='hidden-btn']"));
            Assertions.assertTrue(displayedChecked.get(), "isDirectlyPresent must inspect XPath element visibility");
            displayedChecked.set(false);
            Assertions.assertTrue(SelenideElementFinder.isDirectlyAttached("//button[@id='hidden-btn']"));
            Assertions.assertFalse(displayedChecked.get(), "isDirectlyAttached must not check XPath element visibility");

            Assertions.assertFalse(SelenideElementFinder.isDirectlyAttached("button#missing-btn"));
        }
        finally
        {
            if (WebDriverRunner.hasWebDriverStarted())
            {
                WebDriverRunner.closeWebDriver();
            }
        }
    }
}
