/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.aura.manager.ui.base.AuraManagerTestHelper;
import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the copy button in the Aura Manager terminal output view,
 * ensuring filtering (AI Only, Errors Only) is correctly respected when copying text.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public class AuraManagerTerminalCopyUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerTerminalCopyUiTest()
    {
        super(18898);
    }

    @NeodymiumTest
    public void testTerminalCopyButtonAndFiltering() throws IOException
    {
        AuraManagerTestHelper.startManager();
        final int port = AuraManagerTestHelper.getAuraServer().getAddress().getPort();
        Selenide.open("http://localhost:" + port + "/");

        // Make console panel open and visible
        executeJavaScript("window.consoleOpened = true; if (typeof updateCenterLayout === 'function') updateCenterLayout(); document.getElementById('consolePanel').style.display = 'flex'; document.getElementById('consolePanel').style.height = '300px';");

        // Verify terminal panel and copy button exist
        $("#consolePanel").shouldBe(Condition.visible);
        $("#copyTerminalBtn").shouldBe(Condition.visible);
        $("#copyTerminalIcon").shouldBe(Condition.visible);
        $("#copyTerminalText").shouldHave(Condition.exactText("Copy"));

        // Append log lines into terminalConsole via appendLog function
        executeJavaScript("appendLog('Line 1: Normal system log message');");
        executeJavaScript("appendLog('🤖 Line 2: AI test step executed');");
        executeJavaScript("appendLog('[ERROR] Line 3: Test step failed');");

        // Ensure checkboxes are unselected to verify copying all visible lines
        if ($("#aiOnlyCb").isSelected())
        {
            $("#aiOnlyCb").click();
        }
        if ($("#errorsOnlyCb").isSelected())
        {
            $("#errorsOnlyCb").click();
        }
        executeJavaScript("refreshLogFiltering();");

        // Click copy button
        $("#copyTerminalBtn").click();

        // Verify that visible lines contain all 3 lines when no filters are active
        final String copiedAll = executeJavaScript(
            "const logLines = document.querySelectorAll('#terminalConsole .log-line');"
            + "const visibleLines = [];"
            + "logLines.forEach(line => { if (line.style.display !== 'none') visibleLines.push(line.innerText || line.textContent); });"
            + "return visibleLines.join('\\n');"
        );
        Assertions.assertTrue(copiedAll.contains("Line 1: Normal system log message"), "Should contain line 1");
        Assertions.assertTrue(copiedAll.contains("🤖 Line 2: AI test step executed"), "Should contain line 2");
        Assertions.assertTrue(copiedAll.contains("[ERROR] Line 3: Test step failed"), "Should contain line 3");

        // Now enable "Errors Only" filter
        $("#errorsOnlyCb").click();

        // Click copy button again
        $("#copyTerminalBtn").click();

        // Verify only error line is visible and copied
        final String copiedErrorsOnly = executeJavaScript(
            "const logLines = document.querySelectorAll('#terminalConsole .log-line');"
            + "const visibleLines = [];"
            + "logLines.forEach(line => { if (line.style.display !== 'none') visibleLines.push(line.innerText || line.textContent); });"
            + "return visibleLines.join('\\n');"
        );
        Assertions.assertFalse(copiedErrorsOnly.contains("Line 1: Normal system log message"), "Should NOT contain normal log line when Errors Only is enabled");
        Assertions.assertFalse(copiedErrorsOnly.contains("🤖 Line 2: AI test step executed"), "Should NOT contain AI normal log line when Errors Only is enabled");
        Assertions.assertTrue(copiedErrorsOnly.contains("[ERROR] Line 3: Test step failed"), "Should contain error log line when Errors Only is enabled");
    }
}
