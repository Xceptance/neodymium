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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openqa.selenium.JavascriptExecutor;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Layout regression test for the Aura Dashboard 4-state history column layout.
 *
 * <h2>Design Intent</h2>
 * Each state defines the intended flex proportions:
 * <ul>
 *   <li><b>State 1</b> – {@code colRuns} (flex:1) takes the full container.
 *       {@code colTests} and {@code colReport} are {@code display:none}.</li>
 *   <li><b>State 2</b> – {@code colRuns} (flex:1) and {@code colTests} (flex:1)
 *       share the container equally (~50/50).</li>
 *   <li><b>State 3</b> – All three columns (flex:1 each) share the container
 *       equally (~33/33/33).</li>
 *   <li><b>State 4</b> – {@code colRuns} is replaced by the 48 px mini-bar.
 *       {@code colTests} (flex:1) and {@code colReport} (flex:2) share the
 *       remaining space in a 1:2 ratio.</li>
 * </ul>
 *
 * <p>The test navigates back and forth across these states repeatedly to
 * guard against the regression where {@code colTests} got stuck at a tiny
 * pixel width after returning from State 4.</p>
 *
 * <p>No AI execution is involved — this is a pure Selenide layout test.</p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public class DashboardHistoryLayoutTest extends BaseAuraManagerUiTest
{
    /**
     * Allowed deviation from the ideal flex proportion expressed as a fraction
     * of the container width. Covers the gap (16 px) and resizer (8 px) overhead
     * that the flex container distributes unevenly near the edges.
     *
     * <p>At 1200 px container width, 0.08 = 96 px of slack, which is generous
     * but still tight enough to catch a column that has collapsed to near-zero.</p>
     */
    private static final double FLEX_TOLERANCE = 0.08;

    public DashboardHistoryLayoutTest()
    {
        super(18160);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Starts the Aura Manager server and navigates to the History tab so that
     * the browser is in State 1 before each test method runs.
     */
    @BeforeEach
    public void openHistoryView() throws IOException
    {
        AuraManagerTestHelper.startManager();
        final String url = "http://127.0.0.1:" + AuraManagerTestHelper.getAuraServer().getAddress().getPort();
        Selenide.open(url);

        // Click the History & Reports nav item → triggers State 1
        $("#navReports").shouldBe(Condition.visible).click();
        sleep(600);
    }

    // ── Low-level measurement helpers ─────────────────────────────────────────

    /**
     * Returns the rendered {@code offsetWidth} of the element with the given
     * HTML id, evaluated via JavaScript to bypass any CSS shorthand ambiguity.
     * Returns 0 if the element is {@code display:none} or absent.
     */
    private int offsetWidth(final String elementId)
    {
        final Object r = js().executeScript(
            "var e=document.getElementById(arguments[0]); return e ? e.offsetWidth : 0;",
            elementId);
        return r == null ? 0 : ((Number) r).intValue();
    }

    /**
     * Returns the {@code clientWidth} of {@code #reportViewContainer} — the
     * total width available for the flex children (excluding scrollbar).
     */
    private int containerWidth()
    {
        final Object r = js().executeScript(
            "var e=document.getElementById('reportViewContainer'); return e ? e.clientWidth : 0;");
        return r == null ? 0 : ((Number) r).intValue();
    }

    /**
     * Returns the current value of the JS variable {@code historyNavState}.
     */
    private int navState()
    {
        final Object r = js().executeScript(
            "return typeof historyNavState!=='undefined' ? historyNavState : -1;");
        return r == null ? -1 : ((Number) r).intValue();
    }

    private JavascriptExecutor js()
    {
        return (JavascriptExecutor) WebDriverRunner.getWebDriver();
    }

    // ── Column state assertions ───────────────────────────────────────────────

    /**
     * Asserts that the column is fully hidden ({@code offsetWidth == 0}).
     */
    private void assertHidden(final String id)
    {
        final int w = offsetWidth(id);
        Assertions.assertEquals(0, w,
            "#" + id + " must be display:none (offsetWidth 0), but got " + w + "px");
    }

    /**
     * Asserts that a column has a width close to its expected flex share.
     *
     * <p>The expected share is {@code idealFlex / totalFlex}. For example,
     * in State 2 both columns have flex:1 so {@code idealFlex=1, totalFlex=2}
     * → each should be ~50 % of the container.</p>
     *
     * @param id          element id
     * @param cw          container width in pixels
     * @param idealFlex   this column's flex value (1, 2, …)
     * @param totalFlex   sum of all visible columns' flex values
     */
    private void assertFlexShare(final String id, final int cw,
                                 final int idealFlex, final int totalFlex)
    {
        final int colW    = offsetWidth(id);
        final double ideal = (double) idealFlex / totalFlex;
        final double share = cw > 0 ? (double) colW / cw : 0;

        // Must be visible
        Assertions.assertTrue(colW > 0,
            "#" + id + " must be visible (offsetWidth > 0), but got " + colW + "px");

        // Must be within ±FLEX_TOLERANCE of the ideal proportion
        Assertions.assertTrue(
            Math.abs(share - ideal) <= FLEX_TOLERANCE,
            String.format(
                "#%s expected ~%.0f%% of container (%dpx) but got %dpx (%.1f%%). "
                    + "Tolerance ±%.0f%%. This indicates the column is not flex-sizing correctly.",
                id, ideal * 100, cw, colW, share * 100, FLEX_TOLERANCE * 100));
    }

    // ── Per-state assertion methods ───────────────────────────────────────────

    /**
     * Polls {@code historyNavState} up to {@code timeoutMs} milliseconds, waking every
     * {@code intervalMs}, until it reaches {@code expected}. This absorbs the inherent
     * async gap between the browser dispatching a click event, the JS onclick handler
     * executing synchronously within that event, and the Java thread reading the result.
     *
     * <p>Using a tight poll (100 ms intervals) rather than a fixed {@code sleep} makes
     * the tests run as fast as possible in the happy path while still being robust on
     * slow CI machines or heavily loaded browsers.</p>
     *
     * @param expected   the expected state number (1–4)
     * @param timeoutMs  maximum wait time in milliseconds
     */
    private void waitForNavState(final int expected, final long timeoutMs)
    {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (navState() != expected && System.currentTimeMillis() < deadline)
        {
            sleep(100);
        }
    }

    /**
     * State 1: colRuns occupies 100 % (flex:1, solo); colTests and colReport hidden.
     */
    private void assertState1()
    {
        waitForNavState(1, 2000);
        Assertions.assertEquals(1, navState(), "Expected historyNavState == 1 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns", cw, 1, 1);
        assertHidden("colTests");
        assertHidden("colReport");
    }

    /**
     * State 2: colRuns and colTests each get ~50 % (both flex:1); colReport hidden.
     */
    private void assertState2()
    {
        waitForNavState(2, 2000);
        Assertions.assertEquals(2, navState(), "Expected historyNavState == 2 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns",  cw, 1, 2);
        assertFlexShare("colTests", cw, 1, 2);
        assertHidden("colReport");
    }

    /**
     * State 3: all three columns each get ~33 % (all flex:1).
     */
    private void assertState3()
    {
        waitForNavState(3, 2000);
        Assertions.assertEquals(3, navState(), "Expected historyNavState == 3 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns",   cw, 1, 3);
        assertFlexShare("colTests",  cw, 1, 3);
        assertFlexShare("colReport", cw, 1, 3);
    }

    /**
     * o State 4: colRuns full panel hidden; colTests (flex:1) and colReport (flex:2) share the remaining space in a 1:2
     * ratio. The mini-runs bar is 48 px wide.
     * <p>
     * We calculate the effective container as the total container minus the mini-bar width (48 px) and its gap (16 px)
     * to get the width actually distributed between colTests and colReport.
     * </p>
     */
    /**
     * State 4: colRuns full panel hidden; colTests (flex:1) and colReport (flex:2) share the remaining space in a 1:2
     * ratio. The mini-runs bar is 48 px wide.
     *
     * <p>State 4 is triggered by a {@code postMessage} from the iframe step card click.
     * The parent window listener is async, so we poll {@code navState()} up to 3 s
     * before asserting to avoid a race where the assertion fires before the JS has run.</p>
     *
     * <p>
     * We calculate the effective container as the total container minus the mini-bar width (48 px) and its gap (16 px)
     * to get the width actually distributed between colTests and colReport.
     * </p>
     */
    private void assertState4()
    {
        // Poll until the JS state machine has transitioned, or time out after 3 s.
        final long deadline = System.currentTimeMillis() + 3000;
        while (navState() != 4 && System.currentTimeMillis() < deadline)
        {
            sleep(100);
        }

        Assertions.assertEquals(4, navState(), "Expected historyNavState == 4 (waited up to 3 s for postMessage delivery)");
        final int fullCw = containerWidth();
        // Deduct the mini-bar (48 px) and one gap (16 px) and the resizer (8 px)
        final int miniBarOverhead = 48 + 16 + 8;
        final int effectiveCw = Math.max(1, fullCw - miniBarOverhead);

        assertHidden("colRuns");
        assertFlexShare("colTests", effectiveCw, 1, 3);   // flex:1 of 1+2=3 total
        assertFlexShare("colReport", effectiveCw, 2, 3);   // flex:2 of 1+2=3 total

        // Additional sanity: colReport must be roughly twice colTests
        final int testsW = offsetWidth("colTests");
        final int reportW = offsetWidth("colReport");
        Assertions.assertTrue(
            reportW > testsW,
            "In State 4, colReport (flex:2) must be wider than colTests (flex:1). "
                + "colReport=" + reportW + "px, colTests=" + testsW + "px");
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private void clickFirstRun()
    {
        $$(".history-row").first().shouldBe(Condition.visible).click();
        sleep(400);
    }

    private void clickFirstTestCase()
    {
        $$(".test-card").first().shouldBe(Condition.visible).click();
        sleep(400);
    }

    /**
     * Enters the details iframe and clicks the first step card, which fires
     * {@code postMessage({action:'stepSelected'})} to the parent window and triggers
     * the State-3 → State-4 transition.
     *
     * <p>We wait explicitly for the iframe to be attached and for at least one
     * {@code .step-card} to be visible before clicking. The parent's message listener
     * is async, so after returning to the default context we poll {@code navState()}
     * rather than sleeping a fixed amount.</p>
     */
    private void clickTestStepDetails()
    {
        // Wait for the iframe element to be present and src to be set
        $("#historyConsoleIframe").shouldBe(Condition.visible);

        // Switch into the iframe and click the first step card to fire stepSelected postMessage
        Selenide.switchTo().frame($("#historyConsoleIframe"));
        js().executeScript("var card = document.querySelector('.step-card'); if (card) card.click();");
        sleep(200); // brief wait so the postMessage has time to be dispatched
        Selenide.switchTo().defaultContent();
    }

    /**
     * Clicks the <em>selected</em> mini-run chip in the {@code colRunsMini} bar (State 4).
     *
     * <p>The selected chip is the one whose run ID matches {@code currentReportId}.
     * {@code refreshMiniRunsBar()} marks it with the CSS class {@code .selected}. We must
     * click <em>this specific chip</em> (not blindly the first chip) because the chips are
     * ordered by {@code historyCached} iteration order, which may place the current run at
     * any position. Clicking a non-current chip would call {@code selectHistoryRun()} and
     * transition to State 2 instead of the expected State 3.</p>
     */
    private void clickCurrentMiniRunChip()
    {
        // The selected chip corresponds to the currently loaded run.
        // Clicking it calls onMiniRunChipClick(currentReportId) → applyHistoryState(3).
        $(".mini-run-chip.selected").shouldBe(Condition.visible).click();
        sleep(400);
    }


    private void clickSecondOrFirstRun()
    {
        final var rows = $$(".history-row .run-row-header");
        (rows.size() >= 2 ? rows.get(1) : rows.first()).shouldBe(Condition.visible).click();
        sleep(400);
    }

    // ── Test method ──────────────────────────────────────────────────────────

    /**
     * Navigates through the full State 1 → 2 → 3 → 4 → 3 → 2 → 3 sequence
     * multiple times and verifies that column widths match their designed flex
     * proportions at every checkpoint.
     *
     * <p>The regression being guarded: after returning from State 4, {@code colTests}
     * was previously stuck at a tiny pixel width in State 3 because:
     * <ol>
     *   <li>An old duplicate resize system wrote {@code colTests.style.width}</li>
     *   <li>The CSS class {@code .left-area { min-width: 300px }} survived the
     *       JS inline-style reset and created a hard floor that broke flex sizing</li>
     *   <li>The state machine was not explicitly setting {@code display} on
     *       {@code colRuns} — relying on the CSS class default instead</li>
     * </ol></p>
     */
    @NeodymiumTest
    public void testHistoryLayoutStateMachineColumnWidths()
    {
        // ── Round 0: initial State 1 (full-width runs) ───────────────────
        assertState1();

        // ── Round 1: 1 → 2 → 3 → 4 → 3 → 2 ─────────────────────────────
        clickFirstRun();
        assertState2();

        clickFirstTestCase();
        assertState3();

        clickTestStepDetails();
        assertState4();

        // Return from State 4 → State 3 (core regression path)
        clickCurrentMiniRunChip();
        assertState3();

        clickSecondOrFirstRun();
        assertState2();

        // ── Round 2: 2 → 3 → 4 → 3 (regression path repeated) ──────────
        clickFirstTestCase();
        assertState3();

        clickTestStepDetails();
        assertState4();

        clickCurrentMiniRunChip();
        assertState3();

        // ── Round 3: 3 → 4 → 3 → 4 → 3 (double bounce) ─────────────────
        clickTestStepDetails();
        assertState4();

        clickCurrentMiniRunChip();
        assertState3();

        // ── Round 4: back to State 2, then reset to State 1 ─────────────
        clickSecondOrFirstRun();
        assertState2();

        js().executeScript("applyHistoryState(1);");
        sleep(300);
        assertState1();

        // ── Round 5: fresh 1 → 2 → 3 pass ──────────────────────────────
        clickFirstRun();
        assertState2();

        clickFirstTestCase();
        assertState3();
    }
}
