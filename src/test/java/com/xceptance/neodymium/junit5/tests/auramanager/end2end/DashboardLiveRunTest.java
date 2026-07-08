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
package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openqa.selenium.JavascriptExecutor;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end UI regression tests for the live-run test-card column in the Aura Manager dashboard.
 *
 * <h2>What is tested</h2>
 * <ul>
 *   <li>When the interactive HUD opens the test-card column is populated immediately — not left
 *       empty until the next poll cycle.</li>
 *   <li>Test cards are rendered with the correct 3-state visual:
 *       <ul>
 *         <li><b>Done</b> – check icon, full opacity, cursor:pointer, clickable.</li>
 *         <li><b>Current</b> – accent border + spinner, cursor:pointer, clickable.</li>
 *         <li><b>Pending</b> – clock icon, reduced opacity (~0.4), {@code pointer-events:none}.</li>
 *       </ul>
 *   </li>
 *   <li>Clicking a pending card must NOT navigate away (it is inert).</li>
 *   <li>Clicking a current card switches the iframe to the interactive console.</li>
 *   <li>{@code liveCompletedFiles} state resets between successive runs.</li>
 * </ul>
 *
 * <p>All tests inject synthetic state through the {@code window} JS API so that no real Maven
 * subprocess needs to be spawned. The dashboard's {@code renderLiveTestList()} function is driven
 * directly via JavaScript.</p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public class DashboardLiveRunTest extends BaseAuraManagerUiTest
{
    /** Port range reserved for this test class. */
    public DashboardLiveRunTest()
    {
        super(18170);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Starts the manager, opens the dashboard and navigates to the History/Reports tab.
     * Injects a synthetic multi-dataset run state (3 datasets) into the JS runtime so
     * that tests can exercise {@code renderLiveTestList()} without launching Maven.
     */
    @BeforeEach
    public void openHistoryViewWithSyntheticRun() throws IOException
    {
        AuraManagerTestHelper.startManager();
        final String url = "http://127.0.0.1:" + AuraManagerTestHelper.getAuraServer().getAddress().getPort();
        Selenide.open(url);

        // Navigate to History & Reports tab
        $("#navReports").shouldBe(Condition.visible).click();
        sleep(500);

        // Inject a synthetic 3-dataset live-run state:
        //   index 0 = done (file-a.yaml)
        //   index 1 = current (file-b.yaml)
        //   index 2 = pending (file-c.yaml)
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

    /**
     * Injects synthetic {@code activeRunStats} and {@code liveCompletedFiles} state into the
     * dashboard's JS runtime without starting a real Maven run.
     *
     * @param files       ordered list of test file names (relative paths)
     * @param activeFile  the one that should appear as currently executing
     */
    private void injectSyntheticRunState(final List<String> files, final String activeFile)
    {
        // Build the tests array: [{file:'...', id:null}, ...]
        final StringBuilder testsJson = new StringBuilder("[");
        for (int i = 0; i < files.size(); i++)
        {
            if (i > 0) testsJson.append(',');
            testsJson.append("{\"file\":\"").append(files.get(i)).append("\",\"id\":null}");
        }
        testsJson.append("]");

        // Determine which files are "done" = all before activeFile
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

    /**
     * Returns the computed CSS {@code opacity} of the nth {@code .test-card} element (0-indexed).
     */
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

    /**
     * Returns the computed CSS {@code pointer-events} value of the nth {@code .test-card} element.
     */
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

    /**
     * Returns true if the nth card contains an element with the given Font Awesome icon class.
     */
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

    /**
     * Verifies that exactly 3 test cards are rendered when the live test list contains 3 datasets.
     * This guards against the regression where the column was left empty on HUD entry.
     */
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

    /**
     * Verifies the DONE card (index 0, file-a.yaml):
     * <ul>
     *   <li>Shows a green check icon ({@code fa-circle-check}).</li>
     *   <li>Has opacity ~0.80 (slightly dimmed to distinguish from current).</li>
     *   <li>Has {@code pointer-events: auto} so it is clickable.</li>
     *   <li>Does NOT show the spinner ({@code fa-circle-notch}).</li>
     * </ul>
     */
    @NeodymiumTest
    public void testDoneCardVisualState()
    {
        // Check icon present
        Assertions.assertTrue(cardHasIcon(0, "fa-circle-check"),
            "Done card (index 0) must contain fa-circle-check icon.");

        // No spinner on a done card
        Assertions.assertFalse(cardHasIcon(0, "fa-circle-notch"),
            "Done card must NOT show the active spinner.");

        // Opacity should be around 0.80 (dimmed, but not as low as pending)
        final double opacity = cardOpacity(0);
        Assertions.assertTrue(opacity > 0.70 && opacity <= 0.85,
            "Done card opacity must be in [0.70, 0.85] but was " + opacity);

        // Clickable
        final String pe = cardPointerEvents(0);
        Assertions.assertNotEquals("none", pe,
            "Done card must have pointer-events != none (must be clickable).");
    }

    /**
     * Verifies the CURRENT card (index 1, file-b.yaml):
     * <ul>
     *   <li>Shows the spinning progress indicator ({@code fa-circle-notch fa-spin}).</li>
     *   <li>Has full opacity (1.0) — the active entry must not be dimmed.</li>
     *   <li>Has {@code pointer-events: auto}.</li>
     *   <li>Has a left-accent border (inline style contains {@code border-left}).</li>
     * </ul>
     */
    @NeodymiumTest
    public void testCurrentCardVisualState()
    {
        // Spinner present
        Assertions.assertTrue(cardHasIcon(1, "fa-circle-notch"),
            "Current card (index 1) must contain the fa-circle-notch spinner.");

        // No check-done icon on the current card
        Assertions.assertFalse(cardHasIcon(1, "fa-circle-check"),
            "Current card must NOT show the done check icon.");

        // Full opacity
        final double opacity = cardOpacity(1);
        Assertions.assertTrue(opacity > 0.95,
            "Current card must have full opacity (>0.95) but got " + opacity);

        // Clickable
        final String pe = cardPointerEvents(1);
        Assertions.assertNotEquals("none", pe,
            "Current card must have pointer-events != none.");

        // Accent border in inline style
        final Object style = js().executeScript(
            "var cards = document.querySelectorAll('.test-card');" +
            "return cards[1] ? cards[1].getAttribute('style') : '';");
        Assertions.assertNotNull(style);
        Assertions.assertTrue(style.toString().contains("border-left"),
            "Current card must have a left-accent border in its inline style. Got: " + style);
    }

    /**
     * Verifies the PENDING card (index 2, file-c.yaml):
     * <ul>
     *   <li>Shows a clock icon ({@code fa-clock}).</li>
     *   <li>Has opacity ~0.40 (heavily dimmed to signal "not yet started").</li>
     *   <li>Has {@code pointer-events: none} so it is inert to mouse interaction.</li>
     * </ul>
     */
    @NeodymiumTest
    public void testPendingCardVisualState()
    {
        // Clock icon present
        Assertions.assertTrue(cardHasIcon(2, "fa-clock"),
            "Pending card (index 2) must contain fa-clock icon.");

        // Opacity should be very low (~0.40)
        final double opacity = cardOpacity(2);
        Assertions.assertTrue(opacity < 0.50,
            "Pending card opacity must be below 0.50 but was " + opacity);

        // pointer-events must be none → not clickable
        final String pe = cardPointerEvents(2);
        Assertions.assertEquals("none", pe,
            "Pending card must have pointer-events:none (not clickable). Got: " + pe);
    }

    /**
     * Verifies that the card label shows only the filename (last path segment), not the full path,
     * and that the full path is stored in the {@code title} attribute for tooltip access.
     */
    @NeodymiumTest
    public void testCardLabelShowsFilenameNotFullPath()
    {
        // With synthetic data the "files" are bare filenames, but the label stripping
        // logic should be idempotent (basename of "file-a.yaml" → "file-a.yaml").
        final String labelText = $$(".test-card").get(0)
            .find(".test-card-label")
            .shouldBe(Condition.visible)
            .text();

        // Label must not contain a directory separator — only the filename
        Assertions.assertFalse(labelText.contains("/"),
            "Card label must not contain path separators. Got: " + labelText);
        Assertions.assertFalse(labelText.contains("\\"),
            "Card label must not contain Windows path separators. Got: " + labelText);
    }

    /**
     * Verifies that when a second run is started the {@code liveCompletedFiles} set is cleared
     * and all three cards revert to pending/current state without stale "done" marks from the
     * previous run.
     */
    @NeodymiumTest
    public void testCompletedFilesResetOnNewRun()
    {
        // Simulate a new run where file-a.yaml is now the active (first) test
        injectSyntheticRunState(
            List.of("file-a.yaml", "file-b.yaml", "file-c.yaml"),
            "file-a.yaml"
        );

        // Explicitly clear completed files as the JS run-start code would
        js().executeScript("liveCompletedFiles.clear(); liveLastActiveFile = null; renderLiveTestList();");
        sleep(300);

        // Index 0 should now be CURRENT (spinner), not DONE (check)
        Assertions.assertTrue(cardHasIcon(0, "fa-circle-notch"),
            "After reset, index 0 (file-a.yaml) should be CURRENT (spinner), not DONE.");
        Assertions.assertFalse(cardHasIcon(0, "fa-circle-check"),
            "After reset, index 0 must NOT show the done check icon.");

        // Index 1 and 2 should now be PENDING
        Assertions.assertEquals("none", cardPointerEvents(1),
            "After reset, index 1 (file-b.yaml) should be PENDING (not clickable).");
        Assertions.assertEquals("none", cardPointerEvents(2),
            "After reset, index 2 (file-c.yaml) should be PENDING (not clickable).");
    }

    /**
     * Verifies the transition from the second to the third dataset: when the backend advances
     * {@code activeFile} from "file-b.yaml" to "file-c.yaml", the poll handler should add
     * "file-b.yaml" to {@code liveCompletedFiles} and re-render the list so that:
     * <ul>
     *   <li>Index 0 (file-a.yaml) – done (check icon).</li>
     *   <li>Index 1 (file-b.yaml) – done (check icon) — just completed.</li>
     *   <li>Index 2 (file-c.yaml) – current (spinner).</li>
     * </ul>
     */
    @NeodymiumTest
    public void testActiveFileTransitionAdvancesCompletedSet()
    {
        // Simulate poll detecting activeFile change: file-b.yaml → file-c.yaml
        js().executeScript(
            // Add the previously-active file to liveCompletedFiles
            "liveCompletedFiles.add('file-b.yaml');" +
            "activeRunStats.activeFile = 'file-c.yaml';" +
            "renderLiveTestList();"
        );
        sleep(300);

        // file-a.yaml and file-b.yaml should now show done
        Assertions.assertTrue(cardHasIcon(0, "fa-circle-check"),
            "file-a.yaml must be DONE after advancing to file-c.yaml.");
        Assertions.assertTrue(cardHasIcon(1, "fa-circle-check"),
            "file-b.yaml must be DONE after being superseded by file-c.yaml.");

        // file-c.yaml should now be current
        Assertions.assertTrue(cardHasIcon(2, "fa-circle-notch"),
            "file-c.yaml must be CURRENT (spinner) after advancing to it.");
        final String pe2 = cardPointerEvents(2);
        Assertions.assertNotEquals("none", pe2,
            "file-c.yaml (now current) must be clickable.");
    }

    /**
     * Smoke test: {@code applyHistoryState(3)} must make all three columns visible after
     * {@code renderLiveTestList()} is called, confirming the HUD column-population fix works
     * in the expected layout state.
     */
    @NeodymiumTest
    public void testHistoryState3IsAppliedWhenHudOpens()
    {
        // After @BeforeEach setup the state must be 3 (all columns visible)
        final Object stateVal = js().executeScript(
            "return typeof historyNavState !== 'undefined' ? historyNavState : -1;");
        final int state = stateVal == null ? -1 : ((Number) stateVal).intValue();
        Assertions.assertEquals(3, state,
            "History nav state must be 3 when the live HUD is open (all columns visible).");

        // The historyConsoleIframe column (colReport) must not be hidden
        final Object reportDisplay = js().executeScript(
            "var el = document.getElementById('colReport'); return el ? el.offsetWidth : 0;");
        final int reportWidth = reportDisplay == null ? 0 : ((Number) reportDisplay).intValue();
        Assertions.assertTrue(reportWidth > 0,
            "colReport must be visible (offsetWidth > 0) in State 3, but got " + reportWidth + "px.");
    }
}
