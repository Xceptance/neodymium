/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openqa.selenium.JavascriptExecutor;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.aura.manager.ui.base.AuraManagerTestHelper;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end UI regression tests for the live-run test-card column in the Aura Manager dashboard.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public class DashboardLiveRunUiTest extends BaseAuraManagerUiTest
{
    public DashboardLiveRunUiTest()
    {
        super(18170);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    public void openHistoryViewWithSyntheticRun() throws IOException
    {
        AuraManagerTestHelper.startManager();
        final String url = "http://127.0.0.1:" + AuraManagerTestHelper.getAuraServer().getAddress().getPort();
        Selenide.open(url);

        // Navigate to History & Reports tab
        $("#navReports").shouldBe(Condition.visible).click();
        sleep(500);

        injectSyntheticRunState(
            List.of("file-a.yaml", "file-b.yaml", "file-c.yaml"),
            "file-b.yaml"
        );

        // Transition to 3-column view and render the live test list
        js().executeScript("applyHistoryState(3); renderLiveTestList();");
        sleep(400);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JavascriptExecutor js()
    {
        return (JavascriptExecutor) WebDriverRunner.getWebDriver();
    }

    private void injectSyntheticRunState(final List<String> files, final String activeFile)
    {
        final StringBuilder testsJson = new StringBuilder("[");
        for (int i = 0; i < files.size(); i++)
        {
            if (i > 0) testsJson.append(',');
            testsJson.append("{\"file\":\"").append(files.get(i)).append("\",\"id\":null}");
        }
        testsJson.append("]");

        final int activeIdx = files.indexOf(activeFile);
        final StringBuilder completedJs = new StringBuilder("liveCompletedFiles.clear();");
        for (int i = 0; i < activeIdx; i++)
        {
            completedJs.append("liveCompletedFiles.add('").append(files.get(i)).append("');");
        }

        js().executeScript(
            "activeRunStats.running = true;" +
            "activeRunStats.tests = " + testsJson + ";" +
            "activeRunStats.activeFile = '" + activeFile + "';" +
            completedJs
        );
    }

    private double cardOpacity(final int index)
    {
        final Object opacity = js().executeScript(
            "var cards = document.querySelectorAll('.test-card');" +
            "if (!cards[arguments[0]]) return -1;" +
            "return parseFloat(window.getComputedStyle(cards[arguments[0]]).opacity);",
            (long) index
        );
        return opacity == null ? -1 : ((Number) opacity).doubleValue();
    }

    private String cardPointerEvents(final int index)
    {
        final Object val = js().executeScript(
            "var cards = document.querySelectorAll('.test-card');" +
            "if (!cards[arguments[0]]) return 'missing';" +
            "return window.getComputedStyle(cards[arguments[0]]).pointerEvents;",
            (long) index
        );
        return val == null ? "null" : val.toString();
    }

    private boolean cardHasIcon(final int index, final String iconClass)
    {
        final Object result = js().executeScript(
            "var cards = document.querySelectorAll('.test-card');" +
            "if (!cards[arguments[0]]) return false;" +
            "return cards[arguments[0]].querySelector('." + iconClass + "') !== null;",
            (long) index
        );
        return Boolean.TRUE.equals(result);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @NeodymiumTest
    public void testLiveTestListIsPopulatedImmediately()
    {
        final ElementsCollection cards = $$(".test-card");
        Assertions.assertEquals(3, cards.size(),
            "Expected 3 test cards for 3 datasets, but found " + cards.size() +
            ". The column was likely empty (renderLiveTestList not called on HUD entry).");
        cards.get(0).shouldBe(Condition.visible);
        cards.get(1).shouldBe(Condition.visible);
        cards.get(2).shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public void testDoneCardVisualState()
    {
        Assertions.assertTrue(cardHasIcon(0, "fa-circle-check"),
            "Done card (index 0) must contain fa-circle-check icon.");

        Assertions.assertFalse(cardHasIcon(0, "fa-circle-notch"),
            "Done card must NOT show the active spinner.");

        final double opacity = cardOpacity(0);
        Assertions.assertTrue(opacity > 0.70 && opacity <= 0.85,
            "Done card opacity must be in [0.70, 0.85] but was " + opacity);

        final String pe = cardPointerEvents(0);
        Assertions.assertNotEquals("none", pe,
            "Done card must have pointer-events != none (must be clickable).");
    }

    @NeodymiumTest
    public void testCurrentCardVisualState()
    {
        Assertions.assertTrue(cardHasIcon(1, "fa-circle-notch"),
            "Current card (index 1) must contain the fa-circle-notch spinner.");

        Assertions.assertFalse(cardHasIcon(1, "fa-circle-check"),
            "Current card must NOT show the done check icon.");

        final double opacity = cardOpacity(1);
        Assertions.assertTrue(opacity > 0.95,
            "Current card must have full opacity (>0.95) but got " + opacity);

        final String pe = cardPointerEvents(1);
        Assertions.assertNotEquals("none", pe,
            "Current card must have pointer-events != none.");

        final Object style = js().executeScript(
            "var cards = document.querySelectorAll('.test-card');" +
            "return cards[1] ? cards[1].getAttribute('style') : '';");
        Assertions.assertNotNull(style);
        Assertions.assertTrue(style.toString().contains("border-left"),
            "Current card must have a left-accent border in its inline style. Got: " + style);
    }

    @NeodymiumTest
    public void testPendingCardVisualState()
    {
        Assertions.assertTrue(cardHasIcon(2, "fa-clock"),
            "Pending card (index 2) must contain fa-clock icon.");

        final double opacity = cardOpacity(2);
        Assertions.assertTrue(opacity < 0.50,
            "Pending card opacity must be below 0.50 but was " + opacity);

        final String pe = cardPointerEvents(2);
        Assertions.assertEquals("none", pe,
            "Pending card must have pointer-events:none (not clickable). Got: " + pe);
    }

    @NeodymiumTest
    public void testCardLabelShowsFilenameNotFullPath()
    {
        final String labelText = $$(".test-card").get(0)
            .find(".test-card-label")
            .shouldBe(Condition.visible)
            .text();

        Assertions.assertFalse(labelText.contains("/"),
            "Card label must not contain path separators. Got: " + labelText);
        Assertions.assertFalse(labelText.contains("\\"),
            "Card label must not contain Windows path separators. Got: " + labelText);
    }

    @NeodymiumTest
    public void testCompletedFilesResetOnNewRun()
    {
        injectSyntheticRunState(
            List.of("file-a.yaml", "file-b.yaml", "file-c.yaml"),
            "file-a.yaml"
        );

        js().executeScript("liveCompletedFiles.clear(); liveLastActiveFile = null; renderLiveTestList();");
        sleep(300);

        Assertions.assertTrue(cardHasIcon(0, "fa-circle-notch"),
            "After reset, index 0 (file-a.yaml) should be CURRENT (spinner), not DONE.");
        Assertions.assertFalse(cardHasIcon(0, "fa-circle-check"),
            "After reset, index 0 must NOT show the done check icon.");

        Assertions.assertEquals("none", cardPointerEvents(1),
            "After reset, index 1 (file-b.yaml) should be PENDING (not clickable).");
        Assertions.assertEquals("none", cardPointerEvents(2),
            "After reset, index 2 (file-c.yaml) should be PENDING (not clickable).");
    }

    @NeodymiumTest
    public void testActiveFileTransitionAdvancesCompletedSet()
    {
        js().executeScript(
            "liveCompletedFiles.add('file-b.yaml');" +
            "activeRunStats.activeFile = 'file-c.yaml';" +
            "renderLiveTestList();"
        );
        sleep(300);

        Assertions.assertTrue(cardHasIcon(0, "fa-circle-check"),
            "file-a.yaml must be DONE after advancing to file-c.yaml.");
        Assertions.assertTrue(cardHasIcon(1, "fa-circle-check"),
            "file-b.yaml must be DONE after being superseded by file-c.yaml.");

        Assertions.assertTrue(cardHasIcon(2, "fa-circle-notch"),
            "file-c.yaml must be CURRENT (spinner) after advancing to it.");
        final String pe2 = cardPointerEvents(2);
        Assertions.assertNotEquals("none", pe2,
            "file-c.yaml (now current) must be clickable.");
    }

    @NeodymiumTest
    public void testHistoryState3IsAppliedWhenHudOpens()
    {
        final Object stateVal = js().executeScript(
            "return typeof historyNavState !== 'undefined' ? historyNavState : -1;");
        final int state = stateVal == null ? -1 : ((Number) stateVal).intValue();
        Assertions.assertEquals(3, state,
            "History nav state must be 3 when the live HUD is open (all columns visible).");

        final Object reportDisplay = js().executeScript(
            "var el = document.getElementById('colReport'); return el ? el.offsetWidth : 0;");
        final int reportWidth = reportDisplay == null ? 0 : ((Number) reportDisplay).intValue();
        Assertions.assertTrue(reportWidth > 0,
            "colReport must be visible (offsetWidth > 0) in State 3, but got " + reportWidth + "px.");
    }
}
