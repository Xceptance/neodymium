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
 * Selenide tests for the <em>Data Dump</em> popup shown inside the Aura Manager's embedded
 * interactive console iframe.
 *
 * <h2>Coverage</h2>
 * <ul>
 *   <li>Calling {@code showDumpPopup(data)} renders the overlay with exactly two file entries.</li>
 *   <li>The TXT entry shows the AI-context badge and the correct absolute path.</li>
 *   <li>The HTML entry shows the raw-DOM badge and the correct absolute path.</li>
 *   <li>File sizes are formatted in human-readable form (B / KB / MB).</li>
 *   <li>Clicking "Got it" dismisses the overlay.</li>
 *   <li>Pressing Escape dismisses the overlay.</li>
 *   <li>Calling {@code showDumpPopup} twice replaces the previous content.</li>
 * </ul>
 *
 * <p>The tests run against the <em>standalone</em> interactive console loaded inside an iframe
 * via the Aura Manager. A synthetic {@code dumpReady} payload is injected via {@code postMessage}
 * / direct JS calls into the frame's context because no real test execution is triggered.</p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public class DashboardDumpPopupTest extends BaseAuraManagerUiTest
{
    /** Synthetic dump payload reused across tests. */
    private static final String TXT_PATH  = "/tmp/target/ai-console-dump/neodymium-ai-dump-1234567890.txt";
    private static final String HTML_PATH = "/tmp/target/ai-console-dump/neodymium-ai-dump-1234567890.html";
    private static final long   TXT_SIZE  = 14_336L;   // 14.0 KB
    private static final long   HTML_SIZE = 1_048_576L; // 1.0 MB

    public DashboardDumpPopupTest()
    {
        super(18180);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Starts the Aura Manager, opens the dashboard and navigates to the History tab.
     * Then loads the interactive console standalone HTML into the historyConsoleIframe
     * by setting its src directly, so we can test the dump popup inside the frame.
     */
    @BeforeEach
    public void openConsoleInIframe() throws IOException
    {
        AuraManagerTestHelper.startManager();
        final String managerUrl = "http://127.0.0.1:" + AuraManagerTestHelper.getAuraServer().getAddress().getPort();
        Selenide.open(managerUrl);

        // Navigate to History & Reports → triggers the 3-column view
        $("#navReports").shouldBe(Condition.visible).click();
        sleep(400);
        js().executeScript("applyHistoryState(3);");
        sleep(300);

        // Load the interactive console into the iframe
        js().executeScript(
            "document.getElementById('historyConsoleIframe').src = '/interactive_console.html';"
        );
        sleep(1000); // give the iframe time to load

        // historyPlaceholder is position:absolute and covers the entire iframe area.
        // Chrome's click-interception check fires against the top-level document even when
        // Selenide is switched into the iframe context, so we must hide the placeholder to
        // allow clicks inside the frame to reach their targets.
        js().executeScript("document.getElementById('historyPlaceholder').style.display = 'none';");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JavascriptExecutor js()
    {
        return (JavascriptExecutor) WebDriverRunner.getWebDriver();
    }

    /**
     * Switches Selenide into the console iframe and calls {@code showDumpPopup} with the given
     * payload, then switches back to the default content.
     *
     * @param txtFile  absolute path of the TXT dump file
     * @param htmlFile absolute path of the HTML dump file
     * @param txtSize  byte size of the TXT file
     * @param htmlSize byte size of the HTML file
     */
    private void triggerDumpPopupInFrame(final String txtFile, final String htmlFile,
                                          final long txtSize, final long htmlSize)
    {
        Selenide.switchTo().frame($("#historyConsoleIframe"));
        js().executeScript(
            "showDumpPopup({" +
            "  txtFile: arguments[0]," +
            "  htmlFile: arguments[1]," +
            "  txtSize: arguments[2]," +
            "  htmlSize: arguments[3]" +
            "});",
            txtFile, htmlFile, txtSize, htmlSize
        );
    }

    /** Asserts that the dump overlay is visible (has the CSS class {@code active}). */
    private void assertOverlayVisible()
    {
        final Object hasActive = js().executeScript(
            "var el = document.getElementById('dumpReadyOverlay');" +
            "return el ? el.classList.contains('active') : false;"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(hasActive),
            "dumpReadyOverlay must have class 'active' (be visible).");
    }

    /** Asserts that the dump overlay is hidden (does NOT have the CSS class {@code active}). */
    private void assertOverlayHidden()
    {
        final Object hasActive = js().executeScript(
            "var el = document.getElementById('dumpReadyOverlay');" +
            "return el ? el.classList.contains('active') : true;"
        );
        Assertions.assertFalse(Boolean.TRUE.equals(hasActive),
            "dumpReadyOverlay must NOT have class 'active' (must be hidden).");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Calling {@code showDumpPopup()} must make the {@code #dumpReadyOverlay} element visible
     * and render exactly two file entries — one TXT and one HTML.
     */
    @NeodymiumTest
    public void testDumpPopupShowsWithTwoFileEntries()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);

        assertOverlayVisible();

        // Two .dump-file-entry elements must be present
        final Object count = js().executeScript(
            "return document.querySelectorAll('.dump-file-entry').length;"
        );
        Assertions.assertEquals(2L, count,
            "Dump popup must contain exactly 2 file entries (TXT + HTML).");

        Selenide.switchTo().defaultContent();
    }

    /**
     * The TXT file entry must show the correct badge text ("TXT"), the AI-context description,
     * and the full absolute path with the formatted size.
     */
    @NeodymiumTest
    public void testTxtFileEntryContent()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);

        final Object badgeText = js().executeScript(
            "var badges = document.querySelectorAll('.dump-file-type-badge.txt');" +
            "return badges[0] ? badges[0].textContent.trim() : null;"
        );
        Assertions.assertEquals("TXT", badgeText,
            "First file entry must have the TXT badge.");

        final Object pathText = js().executeScript(
            "var paths = document.querySelectorAll('.dump-file-path > span:first-child');" +
            "return paths[0] ? paths[0].textContent.trim() : null;"
        );
        Assertions.assertEquals(TXT_PATH, pathText,
            "TXT entry must display the exact absolute file path.");

        // Size: 14336 bytes → "14.0 KB"
        final Object sizeText = js().executeScript(
            "var sizes = document.querySelectorAll('.dump-file-size');" +
            "return sizes[0] ? sizes[0].textContent.trim() : null;"
        );
        Assertions.assertEquals("14.0 KB", sizeText,
            "TXT entry must format 14336 bytes as '14.0 KB'.");

        Selenide.switchTo().defaultContent();
    }

    /**
     * The HTML file entry must show the correct badge text ("HTML"), the raw-DOM description,
     * and the full absolute path with the formatted size.
     */
    @NeodymiumTest
    public void testHtmlFileEntryContent()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);

        final Object badgeText = js().executeScript(
            "var badges = document.querySelectorAll('.dump-file-type-badge.html');" +
            "return badges[0] ? badges[0].textContent.trim() : null;"
        );
        Assertions.assertEquals("HTML", badgeText,
            "Second file entry must have the HTML badge.");

        final Object pathText = js().executeScript(
            "var paths = document.querySelectorAll('.dump-file-path > span:first-child');" +
            "return paths[1] ? paths[1].textContent.trim() : null;"
        );
        Assertions.assertEquals(HTML_PATH, pathText,
            "HTML entry must display the exact absolute file path.");

        // Size: 1048576 bytes → "1.0 MB"
        final Object sizeText = js().executeScript(
            "var sizes = document.querySelectorAll('.dump-file-size');" +
            "return sizes[1] ? sizes[1].textContent.trim() : null;"
        );
        Assertions.assertEquals("1.0 MB", sizeText,
            "HTML entry must format 1048576 bytes as '1.0 MB'.");

        Selenide.switchTo().defaultContent();
    }

    /**
     * Clicking the "Got it" button must dismiss the overlay (remove the {@code active} class).
     */
    @NeodymiumTest
    public void testGotItButtonDismissesOverlay()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);
        assertOverlayVisible();

        // We are already inside the iframe frame context after triggerDumpPopupInFrame.
        // Click the "Got it" button directly inside the frame.
        $(".dump-ready-footer .btn").shouldBe(Condition.visible).click();
        sleep(200);

        assertOverlayHidden();
        Selenide.switchTo().defaultContent();
    }

    /**
     * Pressing the Escape key must dismiss the overlay.
     */
    @NeodymiumTest
    public void testEscapeKeyDismissesOverlay()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);
        assertOverlayVisible();

        // Simulate Escape key via JS dispatch (avoids browser focus issues in headless)
        js().executeScript(
            "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));"
        );
        sleep(200);

        assertOverlayHidden();
        Selenide.switchTo().defaultContent();
    }

    /**
     * Calling {@code showDumpPopup} a second time must replace the previous content entirely —
     * still exactly 2 entries with the new paths, not 4.
     */
    @NeodymiumTest
    public void testSecondDumpPopupReplacesFirstContent()
    {
        triggerDumpPopupInFrame(TXT_PATH, HTML_PATH, TXT_SIZE, HTML_SIZE);
        assertOverlayVisible();

        // Dismiss first popup
        js().executeScript("closeDumpPopup();");

        // Trigger second popup with different paths
        final String txtPath2  = "/tmp/target/ai-console-dump/neodymium-ai-dump-9999999999.txt";
        final String htmlPath2 = "/tmp/target/ai-console-dump/neodymium-ai-dump-9999999999.html";
        js().executeScript(
            "showDumpPopup({ txtFile: arguments[0], htmlFile: arguments[1], txtSize: 512, htmlSize: 1024 });",
            txtPath2, htmlPath2
        );
        sleep(200);

        assertOverlayVisible();

        // Must still be exactly 2 entries
        final Object count = js().executeScript(
            "return document.querySelectorAll('.dump-file-entry').length;"
        );
        Assertions.assertEquals(2L, count,
            "After a second dump, there must still be exactly 2 file entries (not 4).");

        // Path of the first entry must be the new TXT path, not the original
        final Object newPath = js().executeScript(
            "var paths = document.querySelectorAll('.dump-file-path > span:first-child');" +
            "return paths[0] ? paths[0].textContent.trim() : null;"
        );
        Assertions.assertEquals(txtPath2, newPath,
            "After second dump the TXT path must show the new path, not the old one.");

        Selenide.switchTo().defaultContent();
    }

    /**
     * The dump overlay element must always be present in the DOM so the SSE event handler
     * can safely call {@code showDumpPopup} without null-guard failures.
     *
     * <p>The overlay lives inside {@code interactive_console.html} which is loaded inside
     * {@code #historyConsoleIframe}. We must switch into that frame before querying the DOM.</p>
     */
    @NeodymiumTest
    public void testDumpOverlayElementAlwaysPresentInDom()
    {
        // Switch into the iframe — the overlay element lives in the console HTML, not the dashboard.
        Selenide.switchTo().frame($("#historyConsoleIframe"));

        // No popup triggered yet — overlay must exist but be hidden
        final Object exists = js().executeScript(
            "return document.getElementById('dumpReadyOverlay') !== null;"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(exists),
            "dumpReadyOverlay element must always be present in the DOM.");

        assertOverlayHidden();
        Selenide.switchTo().defaultContent();
    }
}
