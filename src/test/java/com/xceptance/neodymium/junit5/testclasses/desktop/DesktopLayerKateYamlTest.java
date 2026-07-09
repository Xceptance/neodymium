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
package com.xceptance.neodymium.junit5.testclasses.desktop;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * YAML-driven AI test for the Kate desktop text editor using natural language steps.
 *
 * <p>
 * This test demonstrates how the Neodymium AI agent can drive a native desktop
 * application using the same {@code Neodymium.ai().execute()} pattern as browser
 * tests — no custom Java step methods or manual YAML parsing required. The YAML
 * playbook contains human-readable step descriptions; the AI agent translates them
 * into coordinate-based desktop actions via {@code java.awt.Robot}.
 * </p>
 *
 * <p>
 * The companion YAML file ({@code DesktopLayerKateYamlTest.yaml}) carries a
 * {@code _steps} block with natural language instructions, for example:
 * <pre>
 *   - Launch the Kate text editor with the file path /tmp/… as a command-line argument
 *   - Click in the center of the editor area to ensure keyboard focus
 *   - Type the text: Hello World
 *   - Save the file using Ctrl+S
 *   - Verify that the file exists and contains the expected content
 *   - Close Kate using Ctrl+Q
 * </pre>
 * </p>
 *
 * <p>
 * There is no {@code @Browser} annotation because no Selenide / WebDriver session is
 * started. The driver backend is set to {@code desktop} before the Neodymium AI agent
 * is initialised, wiring all interactions to {@code DesktopBrowserFacade} /
 * {@code java.awt.Robot}.
 * </p>
 *
 * <p>
 * <b>Prerequisites:</b>
 * <ul>
 *   <li>Kate ({@code kate}) must be installed and on {@code PATH}.</li>
 *   <li>A graphical display (X11/Wayland) must be available.</li>
 *   <li>An AI API key must be configured via the {@code GEMINI_API_KEY} environment
 *       variable or the {@code neodymium.ai.apiKey} system property, or the test must
 *       be run with a recorded offline replay.</li>
 * </ul>
 * </p>
 *
 * @see DesktopLayerKateSmokeTest
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SuppressBrowsers
@DataFile("com/xceptance/neodymium/junit5/testclasses/desktop/DesktopLayerKateYamlTest.yaml")
public class DesktopLayerKateYamlTest
{
    static
    {
        System.setProperty("neodymium.driver.backend", "desktop");
    }

    /** Absolute path of the file Kate will write to disk. */
    private static final String OUTPUT_FILE_PATH = "/tmp/neo-desktop-yaml-test.txt";

    /**
     * Switches the driver backend to {@code desktop} so that the Neodymium AI agent
     * uses {@code DesktopBrowserFacade} / {@code java.awt.Robot} instead of Selenide,
     * and resets the output file to ensure a clean run.
     *
     * <p>
     * The system property must be set here (before {@code NeodymiumBeforeTestExecutionCallback}
     * runs) so that the {@code AiBrowser} is initialised with the desktop layer already active.
     * </p>
     */
    @BeforeEach
    public void setUp()
    {
        // Tell the Neodymium interaction layer to use the desktop backend.
        System.setProperty("neodymium.driver.backend", "desktop");
        Neodymium.recreateInteractionLayer("desktop");

        // Ensure a clean slate for each run
        final File outputFile = new File(OUTPUT_FILE_PATH);
        if (outputFile.exists())
        {
            outputFile.delete();
        }
    }

    /**
     * Kills any lingering Kate process, removes the output file, and restores the
     * driver backend configuration after each test.
     */
    @AfterEach
    @SuppressBrowsers
    public void tearDown()
    {
        // Best-effort: terminate Kate if it is still running
        try
        {
            Runtime.getRuntime().exec(new String[] { "pkill", "-x", "kate" });
            Thread.sleep(500);
        }
        catch (final IOException | InterruptedException ignored)
        {
            // Kate may already be gone — that is fine
        }

        final File outputFile = new File(OUTPUT_FILE_PATH);
        if (outputFile.exists())
        {
            outputFile.delete();
        }

        // Restore driver backend so other tests in the same JVM are unaffected
        System.clearProperty("neodymium.driver.backend");
    }

    /**
     * Runs the YAML-defined natural language steps via the Neodymium AI agent.
     * <p>
     * {@code Neodymium.ai().execute()} reads the {@code _steps} block from the YAML dataset injected by
     * {@link DataFile}, then dispatches each natural language instruction to the AI agent. The agent translates each
     * step into {@code DesktopBrowserFacade} actions (coordinate-based click, type, key chord) using
     * {@code java.awt.Robot} — no browser or WebDriver is involved.
     * </p>
     * 
     * @throws Throwable
     */
    @NeodymiumTest
    public void testKateViaYamlPlaybook() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
