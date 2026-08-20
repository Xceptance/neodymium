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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openqa.selenium.JavascriptExecutor;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.aura.manager.ui.base.AuraManagerTestHelper;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Layout regression test for the Aura Dashboard 4-state history column layout.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public class DashboardHistoryLayoutUiTest extends BaseAuraManagerUiTest
{
    private static final double FLEX_TOLERANCE = 0.08;

    public DashboardHistoryLayoutUiTest()
    {
        super(18160);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

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

    private int offsetWidth(final String elementId)
    {
        final Object r = js().executeScript(
            "var e=document.getElementById(arguments[0]); return e ? e.offsetWidth : 0;",
            elementId);
        return r == null ? 0 : ((Number) r).intValue();
    }

    private int containerWidth()
    {
        final Object r = js().executeScript(
            "var e=document.getElementById('reportViewContainer'); return e ? e.clientWidth : 0;");
        return r == null ? 0 : ((Number) r).intValue();
    }

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

    private void assertHidden(final String id)
    {
        final int w = offsetWidth(id);
        Assertions.assertEquals(0, w,
            "#" + id + " must be display:none (offsetWidth 0), but got " + w + "px");
    }

    private void assertFlexShare(final String id, final int cw,
                                 final int idealFlex, final int totalFlex)
    {
        final int colW    = offsetWidth(id);
        final double ideal = (double) idealFlex / totalFlex;
        final double share = cw > 0 ? (double) colW / cw : 0;

        Assertions.assertTrue(colW > 0,
            "#" + id + " must be visible (offsetWidth > 0), but got " + colW + "px");

        Assertions.assertTrue(
            Math.abs(share - ideal) <= FLEX_TOLERANCE,
            String.format(
                "#%s expected ~%.0f%% of container (%dpx) but got %dpx (%.1f%%). "
                    + "Tolerance ±%.0f%%. This indicates the column is not flex-sizing correctly.",
                id, ideal * 100, cw, colW, share * 100, FLEX_TOLERANCE * 100));
    }

    private void waitForNavState(final int expected, final long timeoutMs)
    {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (navState() != expected && System.currentTimeMillis() < deadline)
        {
            sleep(100);
        }
    }

    private void assertState1()
    {
        waitForNavState(1, 2000);
        Assertions.assertEquals(1, navState(), "Expected historyNavState == 1 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns", cw, 1, 1);
        assertHidden("colTests");
        assertHidden("colReport");
    }

    private void assertState2()
    {
        waitForNavState(2, 2000);
        Assertions.assertEquals(2, navState(), "Expected historyNavState == 2 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns",  cw, 1, 2);
        assertFlexShare("colTests", cw, 1, 2);
        assertHidden("colReport");
    }

    private void assertState3()
    {
        waitForNavState(3, 2000);
        Assertions.assertEquals(3, navState(), "Expected historyNavState == 3 (waited up to 2 s)");
        final int cw = containerWidth();
        assertFlexShare("colRuns",   cw, 1, 3);
        assertFlexShare("colTests",  cw, 1, 3);
        assertFlexShare("colReport", cw, 1, 3);
    }

    private void assertState4()
    {
        final long deadline = System.currentTimeMillis() + 3000;
        while (navState() != 4 && System.currentTimeMillis() < deadline)
        {
            sleep(100);
        }

        Assertions.assertEquals(4, navState(), "Expected historyNavState == 4 (waited up to 3 s for postMessage delivery)");
        final int fullCw = containerWidth();
        final int miniBarOverhead = 48 + 16 + 8;
        final int effectiveCw = Math.max(1, fullCw - miniBarOverhead);

        assertHidden("colRuns");
        assertFlexShare("colTests", effectiveCw, 1, 3);
        assertFlexShare("colReport", effectiveCw, 2, 3);

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

    private void clickTestStepDetails()
    {
        $("#historyConsoleIframe").shouldBe(Condition.visible);

        Selenide.switchTo().frame($("#historyConsoleIframe"));
        js().executeScript("var card = document.querySelector('.step-card'); if (card) card.click();");
        sleep(200);
        Selenide.switchTo().defaultContent();
    }

    private void clickCurrentMiniRunChip()
    {
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
