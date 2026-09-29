package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests utility and helper methods of the Aura Manager.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraManagerUtilsTest
{
    @Test
    public void testStripAnsi()
    {
        Assertions.assertEquals("[INFO] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[INFO] Scanning for projects..."));
        Assertions.assertEquals("[WARNING] Deprecated method...", NeodymiumAuraManager.stripAnsi("[WARNING] Deprecated method..."));
        Assertions.assertEquals("[ERROR] compilation error", NeodymiumAuraManager.stripAnsi("[ERROR] compilation error"));
        Assertions.assertEquals("INFO Scanning for projects...", NeodymiumAuraManager.stripAnsi("\u001B[1;34mINFO\u001B[m Scanning for projects..."));
        Assertions.assertEquals("[ INFO ] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[ [1;34mINFO [m] Scanning for projects..."));
    }
}
