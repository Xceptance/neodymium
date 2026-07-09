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

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.xceptance.neodymium.util.InteractionLayer;
import com.xceptance.neodymium.util.layer.FoundElement;

/**
 * Smoke test for the desktop interaction layer using the Kate text editor.
 *
 * <p>
 * This test exercises the full flow of the desktop backend (java.awt.Robot):
 * <ol>
 *   <li>Launch Kate via {@code kate} CLI command</li>
 *   <li>Wait for the editor window to appear on screen</li>
 *   <li>Type "Hello World" into the active editor area</li>
 *   <li>Save the file to {@code /tmp/neo-desktop-test.txt} via the "Save As" dialog</li>
 *   <li>Verify the file was written to disk with the expected content</li>
 *   <li>Close Kate</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>Prerequisites:</b>
 * <ul>
 *   <li>Kate ({@code kate}) must be installed and available on {@code PATH}.</li>
 *   <li>A graphical display (X11/Wayland) must be available — {@code DISPLAY} must be set.</li>
 *   <li>The test must be run on the same physical desktop session that is currently active.</li>
 * </ul>
 * </p>
 *
 * <p>
 * All interactions use {@link InteractionLayer} constructed with {@code "desktop"} backend,
 * so no Selenide/WebDriver setup is required.
 * </p>
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class DesktopLayerKateSmokeTest
{
    /** Path where the test file will be saved. */
    private static final String OUTPUT_FILE_PATH = "/tmp/neo-desktop-test.txt";

    /** Text to be typed into Kate. */
    private static final String TEST_CONTENT = "Hello World";

    /** Desktop interaction layer under test — no browser driver involved. */
    private InteractionLayer desktop;

    /** The output file, cleaned up in {@link #tearDown()}. */
    private File outputFile;

    /**
     * Builds the desktop interaction layer before each test.
     * No WebDriver is started; the desktop backend uses java.awt.Robot directly.
     */
    @BeforeEach
    public void setUp()
    {
        desktop = new InteractionLayer("desktop");
        outputFile = new File(OUTPUT_FILE_PATH);
    }

    /**
     * Terminates Kate (if still running) and deletes the output file after each test.
     */
    @AfterEach
    public void tearDown()
    {
        // Best-effort: kill any remaining kate process
        try
        {
            Runtime.getRuntime().exec(new String[] { "pkill", "-x", "kate" });
        }
        catch (final IOException ignored)
        {
            // Ignore — process may already be gone
        }

        // Remove the output file so repeated runs start clean
        if (outputFile != null && outputFile.exists())
        {
            outputFile.delete();
        }
    }

    /**
     * Full smoke test: open Kate with target file as argument, type text, save, verify, close.
     *
     * <p>
     * By opening Kate as {@code kate /tmp/neo-desktop-test.txt} the file path is
     * pre-set — pressing Ctrl+S saves in-place without triggering the KDE file-chooser
     * dialog. This avoids all coordinate-based dialog navigation.
     * </p>
     *
     * @throws Exception if any Robot or file I/O operation fails
     */
    @Test
    public void testKateOpenTypeAndSave() throws Exception
    {
        // ── Step 1: Launch Kate with the target file path as argument ─────────
        // Kate opens and creates the file when Ctrl+S is pressed.
        // The desktop facade executes the command and sleeps 2 s for startup.
        desktop.open("kate " + OUTPUT_FILE_PATH);

        // Give Kate a bit more time to fully render its main window and load the
        // (empty) file buffer. Kate may show a session-restore dialog; we dismiss it.
        desktop.sleep(3000);

        // ── Step 2: Dismiss any session-restore prompt ─────────────────────────
        // If Kate offers to restore the previous session, Escape closes that dialog.
        pressEscape();
        desktop.sleep(500);

        // ── Step 3: Click the editor area to ensure focus ─────────────────────
        // Click near the center of the screen where the editor pane should be.
        final FoundElement editorCenter = desktop.findFirstElement(By.cssSelector("[620, 400]"));
        editorCenter.click();
        desktop.sleep(400);

        // ── Step 4: Type the test content ─────────────────────────────────────
        // sendKeys dispatches each character via java.awt.Robot.
        editorCenter.sendKeys(TEST_CONTENT);
        desktop.sleep(400);

        // ── Step 5: Save with Ctrl+S ──────────────────────────────────────────
        // Since the file path was given as a CLI argument, Kate knows where to
        // write — Ctrl+S goes directly to disk without any dialog.
        pressCtrlS();
        desktop.sleep(1500);

        // ── Step 6: Verify the file content on disk ───────────────────────────
        Assertions.assertTrue(outputFile.exists(),
            "The file '" + OUTPUT_FILE_PATH + "' must exist after Ctrl+S in Kate.");

        final String writtenContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        Assertions.assertTrue(writtenContent.contains(TEST_CONTENT),
            "File content should contain '" + TEST_CONTENT + "' but was: " + writtenContent);

        // ── Step 7: Close Kate with Ctrl+Q ────────────────────────────────────
        pressCtrlQ();
        desktop.sleep(800);

        // If Kate asks about unsaved changes, press Enter (accepts the default action)
        pressEnter();
        desktop.sleep(500);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers – Robot key chord shortcuts
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sends Ctrl+S ("Save") to the currently focused desktop window.
     * For a new unsaved Kate document this triggers the "Save As" dialog.
     */
    private void pressCtrlS()
    {
        sendChord(KeyEvent.VK_CONTROL, KeyEvent.VK_S);
    }

    /**
     * Sends Ctrl+L to the currently focused desktop window.
     * In the KDE file dialog, Ctrl+L activates the "Location:" URL bar so that
     * the next keyboard input goes into the path field, not the file list.
     */
    private void pressCtrlL()
    {
        sendChord(KeyEvent.VK_CONTROL, KeyEvent.VK_L);
    }

    /**
     * Sends Ctrl+A ("Select All") to the currently focused desktop window.
     */
    private void pressCtrlA()
    {
        sendChord(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
    }

    /**
     * Sends Enter to the currently focused desktop window.
     */
    private void pressEnter()
    {
        sendChord(KeyEvent.VK_ENTER);
    }

    /**
     * Sends Escape to the currently focused desktop window.
     * Used to dismiss session-restore prompts or other dialogs.
     */
    private void pressEscape()
    {
        sendChord(KeyEvent.VK_ESCAPE);
    }

    /**
     * Sends Ctrl+Q ("Quit") to the currently focused desktop window.
     */
    private void pressCtrlQ()
    {
        sendChord(KeyEvent.VK_CONTROL, KeyEvent.VK_Q);
    }

    /**
     * Presses and releases a sequence of key codes as a single chord using java.awt.Robot.
     *
     * @param keyCodes the key codes to press, in order; all are held while the last is typed
     * @throws RuntimeException if java.awt.Robot cannot be initialised
     */
    private void sendChord(final int... keyCodes)
    {
        try
        {
            final java.awt.Robot robot = new java.awt.Robot();
            robot.setAutoDelay(30);

            // Press all keys
            for (final int code : keyCodes)
            {
                robot.keyPress(code);
            }

            // Release all keys in reverse order
            for (int i = keyCodes.length - 1; i >= 0; i--)
            {
                robot.keyRelease(keyCodes[i]);
            }
        }
        catch (final java.awt.AWTException e)
        {
            throw new RuntimeException("Failed to initialise java.awt.Robot for key chord", e);
        }
    }
}
