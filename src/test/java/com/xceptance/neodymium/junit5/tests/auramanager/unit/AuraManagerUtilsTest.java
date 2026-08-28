package com.xceptance.neodymium.junit5.tests.auramanager.unit;

import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests utility and helper methods of the Aura Manager.
 */
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
