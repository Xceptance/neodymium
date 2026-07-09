/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.util.layer.selenide;

import org.openqa.selenium.Keys;

/**
 * Maps human-readable key names (e.g. {@code "ENTER"}, {@code "Ctrl+A"}) to
 * Selenium {@link Keys} sequences.
 * <p>
 * This class is the <strong>only permitted location</strong> for {@link Keys} imports.
 * All callers use plain {@code String} key names and receive a {@link CharSequence}
 * that can be passed to any send-keys method.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class KeyMapper
{
    /** Private constructor — utility class, not instantiable. */
    private KeyMapper()
    {
    }

    /**
     * Maps a key name (or chord expression such as {@code "Ctrl+A"}) to the
     * corresponding Selenium {@link Keys} sequence.
     *
     * @param keyName the human-readable key name, e.g. {@code "ENTER"}, {@code "Ctrl+A"}, {@code "a"}
     * @return the driver-specific {@link CharSequence} to pass to {@code sendKeys}
     * @throws IllegalArgumentException if {@code keyName} is blank or unrecognized
     */
    public static CharSequence map(final String keyName)
    {
        if (keyName == null || keyName.isBlank())
        {
            throw new IllegalArgumentException("Key name cannot be null or blank");
        }

        final String trimmed = keyName.trim();

        // Handle chord expressions, e.g. "Ctrl+A" or "Shift+Tab"
        if (trimmed.contains("+"))
        {
            final String[] parts = trimmed.split("\\+");
            final CharSequence[] chords = new CharSequence[parts.length];
            for (int i = 0; i < parts.length; i++)
            {
                final String part = parts[i].trim();
                chords[i] = mapModifierOrKey(part);
            }
            return Keys.chord(chords);
        }

        return mapSingleKey(trimmed);
    }

    /**
     * Maps modifier key names and delegates single character or named keys.
     *
     * @param part the individual part of a chord expression
     * @return the corresponding key sequence
     */
    private static CharSequence mapModifierOrKey(final String part)
    {
        return switch (part.toUpperCase())
        {
            case "CTRL", "CONTROL" -> Keys.CONTROL;
            case "SHIFT"           -> Keys.SHIFT;
            case "ALT"             -> Keys.ALT;
            case "META", "CMD", "COMMAND" -> Keys.META;
            default -> mapSingleKey(part);
        };
    }

    /**
     * Maps a single key name to the Selenium {@link Keys} constant.
     * Single-character strings are returned as-is (typed literally).
     *
     * @param keyName the single key name to map
     * @return the corresponding {@link Keys} constant or the raw character
     * @throws IllegalArgumentException if the key name is unrecognized
     */
    private static CharSequence mapSingleKey(final String keyName)
    {
        // Pass through single characters directly (e.g. "a", "1", "!")
        if (keyName.length() == 1)
        {
            return keyName;
        }

        return switch (keyName.toUpperCase())
        {
            case "ENTER", "RETURN"           -> Keys.ENTER;
            case "TAB"                        -> Keys.TAB;
            case "SHIFT_TAB", "SHIFTTAB",
                 "SHIFT+TAB", "SHIFT-TAB",
                 "REVERSE_TAB"               -> Keys.chord(Keys.SHIFT, Keys.TAB);
            case "ESCAPE", "ESC"             -> Keys.ESCAPE;
            case "BACKSPACE"                 -> Keys.BACK_SPACE;
            case "DELETE"                    -> Keys.DELETE;
            case "SPACE"                     -> Keys.SPACE;
            case "ARROW_UP", "ARROWUP",
                 "UP"                        -> Keys.ARROW_UP;
            case "ARROW_DOWN", "ARROWDOWN",
                 "DOWN"                      -> Keys.ARROW_DOWN;
            case "ARROW_LEFT", "ARROWLEFT",
                 "LEFT"                      -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "ARROWRIGHT",
                 "RIGHT"                     -> Keys.ARROW_RIGHT;
            case "HOME"                      -> Keys.HOME;
            case "END"                       -> Keys.END;
            case "PAGE_UP", "PAGEUP"         -> Keys.PAGE_UP;
            case "PAGE_DOWN", "PAGEDOWN"     -> Keys.PAGE_DOWN;
            case "F1"  -> Keys.F1;
            case "F2"  -> Keys.F2;
            case "F3"  -> Keys.F3;
            case "F4"  -> Keys.F4;
            case "F5"  -> Keys.F5;
            case "F6"  -> Keys.F6;
            case "F7"  -> Keys.F7;
            case "F8"  -> Keys.F8;
            case "F9"  -> Keys.F9;
            case "F10" -> Keys.F10;
            case "F11" -> Keys.F11;
            case "F12" -> Keys.F12;
            default -> throw new IllegalArgumentException("Unrecognized key name: " + keyName);
        };
    }
}
