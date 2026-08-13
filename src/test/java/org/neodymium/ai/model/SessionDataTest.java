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
package org.neodymium.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * TDD test suite validating the {@link SessionData} layered data hierarchy,
 * lookup precedence, snapshot rollback, and sensitive parameter masking.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionDataTest
{
    /**
     * Constructs a default test instance.
     */
    public SessionDataTest()
    {
    }

    /**
     * Verifies that modifying the original map passed to the constructor does not
     * affect the SessionData's static data (defensive copy check).
     */
    @Test
    public void testStaticDataImmutability()
    {
        final Map<String, SessionData.DataEntry> staticMap = new HashMap<>();
        staticMap.put("host", new SessionData.DataEntry("localhost", false));

        final SessionData session = new SessionData(staticMap);

        // Modify the original map
        staticMap.put("host", new SessionData.DataEntry("remote", false));
        staticMap.put("port", new SessionData.DataEntry(8080, false));

        // Session data must remain unaffected
        assertEquals("localhost", session.get("host"));
        assertNull(session.get("port"));
    }

    /**
     * Verifies that dynamic variables take precedence over static variables during lookups.
     */
    @Test
    public void testLookupPrecedence()
    {
        final Map<String, SessionData.DataEntry> staticMap = new HashMap<>();
        staticMap.put("username", new SessionData.DataEntry("alice", false));
        staticMap.put("role", new SessionData.DataEntry("admin", false));

        final SessionData session = new SessionData(staticMap);

        // Put overriding dynamic variable
        session.putDynamic("username", "bob", false);

        // Precedence check: username should be bob (dynamic), role should remain admin (static fallback)
        assertEquals("bob", session.get("username"));
        assertEquals("admin", session.get("role"));
    }

    /**
     * Verifies that snapshots of dynamic states are captured and correctly rolled back.
     */
    @Test
    public void testSnapshotAndRollback()
    {
        final SessionData session = new SessionData(new HashMap<>());

        session.putDynamic("var1", "value1", false);
        
        // 1. Capture snapshot at step 1
        session.captureSnapshot(1);

        // 2. Modify state during step 1 execution
        session.putDynamic("var1", "value1-modified", false);
        session.putDynamic("var2", "value2", false);

        assertEquals("value1-modified", session.get("var1"));
        assertEquals("value2", session.get("var2"));

        // 3. Rollback to step 1
        session.rollbackToStep(1);

        // var1 should revert to value1, and var2 (added after snapshot) should be discarded
        assertEquals("value1", session.get("var1"));
        assertNull(session.get("var2"));
    }

    /**
     * Verifies that sensitive variables are correctly masked with the placeholder in the guarded map.
     */
    @Test
    public void testGuardedDataMapMasking()
    {
        final Map<String, SessionData.DataEntry> staticMap = new HashMap<>();
        staticMap.put("publicVar", new SessionData.DataEntry("public", false));
        staticMap.put("password", new SessionData.DataEntry("secretPassword", true));

        final SessionData session = new SessionData(staticMap);
        session.putDynamic("token", "secretToken", true);
        session.putDynamic("dynamicPublic", "publicDynamicVal", false);

        final Map<String, Object> guardedMap = session.getGuardedDataMap();

        // Non-sensitive variables must preserve their raw values
        assertEquals("public", guardedMap.get("publicVar"));
        assertEquals("publicDynamicVal", guardedMap.get("dynamicPublic"));

        // Sensitive variables must be replaced with the standard masking placeholder
        assertEquals("[SENSITIVE_VALUE]", guardedMap.get("password"));
        assertEquals("[SENSITIVE_VALUE]", guardedMap.get("token"));
    }

    /**
     * Verifies that resolveVariables falls back to Neodymium.getData() when not in SessionData.
     */
    @Test
    public void testResolveVariablesFallbackToNeodymiumData()
    {
        com.xceptance.neodymium.util.Neodymium.getData().put("globalUrl", "http://localhost:8080");
        final SessionData session = new SessionData();

        final String resolved = session.resolveVariables("Open ${globalUrl}/home");
        assertEquals("Open http://localhost:8080/home", resolved);
    }

    /**
     * Verifies that resolveVariables fails hard with IllegalArgumentException when a placeholder cannot be resolved.
     */
    @Test
    public void testResolveVariablesFailsHardOnUnresolvablePlaceholder()
    {
        final SessionData session = new SessionData();
        final IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> session.resolveVariables("Open ${unresolvableVariable}/home")
        );

        org.junit.jupiter.api.Assertions.assertTrue(
            ex.getMessage().contains("Unresolvable variable placeholder '${unresolvableVariable}'"),
            "Exception message should mention the missing variable name"
        );
    }
}
