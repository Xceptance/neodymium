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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SemanticIntent} parsing, assertions, and category classification.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class SemanticIntentTest
{
    @Test
    public void testIsAssertion()
    {
        assertTrue(SemanticIntent.ASSERT.isAssertion());
        assertTrue(SemanticIntent.ASSERT_METADATA.isAssertion());

        assertFalse(SemanticIntent.CLICK.isAssertion());
        assertFalse(SemanticIntent.TYPE.isAssertion());
        assertFalse(SemanticIntent.SELECT.isAssertion());
        assertFalse(SemanticIntent.HOVER_SCROLL.isAssertion());
        assertFalse(SemanticIntent.NAVIGATE.isAssertion());
        assertFalse(SemanticIntent.WAIT.isAssertion());
        assertFalse(SemanticIntent.STORE.isAssertion());
        assertFalse(SemanticIntent.BRANCH.isAssertion());
    }

    @Test
    public void testIsInteraction()
    {
        assertTrue(SemanticIntent.CLICK.isInteraction());
        assertTrue(SemanticIntent.TYPE.isInteraction());
        assertTrue(SemanticIntent.SELECT.isInteraction());
        assertTrue(SemanticIntent.HOVER_SCROLL.isInteraction());

        assertFalse(SemanticIntent.ASSERT.isInteraction());
        assertFalse(SemanticIntent.ASSERT_METADATA.isInteraction());
        assertFalse(SemanticIntent.NAVIGATE.isInteraction());
        assertFalse(SemanticIntent.WAIT.isInteraction());
        assertFalse(SemanticIntent.STORE.isInteraction());
        assertFalse(SemanticIntent.BRANCH.isInteraction());
    }

    @Test
    public void testIsMutating()
    {
        assertTrue(SemanticIntent.CLICK.isMutating());
        assertTrue(SemanticIntent.TYPE.isMutating());
        assertTrue(SemanticIntent.SELECT.isMutating());
        assertTrue(SemanticIntent.NAVIGATE.isMutating());

        assertFalse(SemanticIntent.ASSERT.isMutating());
        assertFalse(SemanticIntent.ASSERT_METADATA.isMutating());
        assertFalse(SemanticIntent.HOVER_SCROLL.isMutating());
        assertFalse(SemanticIntent.WAIT.isMutating());
        assertFalse(SemanticIntent.STORE.isMutating());
        assertFalse(SemanticIntent.BRANCH.isMutating());
    }

    @Test
    public void testFromCodeAndAliases()
    {
        assertEquals(SemanticIntent.ASSERT, SemanticIntent.fromCode("ASSERT"));
        assertEquals(SemanticIntent.ASSERT, SemanticIntent.fromCode("assert"));
        assertEquals(SemanticIntent.ASSERT, SemanticIntent.fromCode("VERIFY"));
        assertEquals(SemanticIntent.ASSERT, SemanticIntent.fromCode("check"));
        assertEquals(SemanticIntent.ASSERT, SemanticIntent.fromCode("validate"));

        assertEquals(SemanticIntent.ASSERT_METADATA, SemanticIntent.fromCode("ASSERT_METADATA"));
        assertEquals(SemanticIntent.ASSERT_METADATA, SemanticIntent.fromCode("assert-metadata"));
        assertEquals(SemanticIntent.ASSERT_METADATA, SemanticIntent.fromCode("url"));
        assertEquals(SemanticIntent.ASSERT_METADATA, SemanticIntent.fromCode("title"));

        assertEquals(SemanticIntent.CLICK, SemanticIntent.fromCode("CLICK"));
        assertEquals(SemanticIntent.CLICK, SemanticIntent.fromCode("button"));
        assertEquals(SemanticIntent.CLICK, SemanticIntent.fromCode("link"));

        assertEquals(SemanticIntent.TYPE, SemanticIntent.fromCode("TYPE"));
        assertEquals(SemanticIntent.TYPE, SemanticIntent.fromCode("input"));
        assertEquals(SemanticIntent.TYPE, SemanticIntent.fromCode("fill"));
        assertEquals(SemanticIntent.TYPE, SemanticIntent.fromCode("enter"));

        assertEquals(SemanticIntent.SELECT, SemanticIntent.fromCode("SELECT"));
        assertEquals(SemanticIntent.SELECT, SemanticIntent.fromCode("dropdown"));
        assertEquals(SemanticIntent.SELECT, SemanticIntent.fromCode("option"));
        assertEquals(SemanticIntent.SELECT, SemanticIntent.fromCode("radio"));

        assertEquals(SemanticIntent.HOVER_SCROLL, SemanticIntent.fromCode("HOVER_SCROLL"));
        assertEquals(SemanticIntent.HOVER_SCROLL, SemanticIntent.fromCode("hover"));
        assertEquals(SemanticIntent.HOVER_SCROLL, SemanticIntent.fromCode("scroll"));

        assertEquals(SemanticIntent.NAVIGATE, SemanticIntent.fromCode("NAVIGATE"));
        assertEquals(SemanticIntent.NAVIGATE, SemanticIntent.fromCode("open"));
        assertEquals(SemanticIntent.NAVIGATE, SemanticIntent.fromCode("goto"));

        assertEquals(SemanticIntent.WAIT, SemanticIntent.fromCode("WAIT"));
        assertEquals(SemanticIntent.WAIT, SemanticIntent.fromCode("pause"));
        assertEquals(SemanticIntent.WAIT, SemanticIntent.fromCode("sleep"));

        assertEquals(SemanticIntent.STORE, SemanticIntent.fromCode("STORE"));
        assertEquals(SemanticIntent.STORE, SemanticIntent.fromCode("extract"));
        assertEquals(SemanticIntent.STORE, SemanticIntent.fromCode("save"));

        assertEquals(SemanticIntent.BRANCH, SemanticIntent.fromCode("BRANCH"));
        assertEquals(SemanticIntent.BRANCH, SemanticIntent.fromCode("condition"));
        assertEquals(SemanticIntent.BRANCH, SemanticIntent.fromCode("if"));

        assertNull(SemanticIntent.fromCode(null));
        assertNull(SemanticIntent.fromCode("   "));
        assertNull(SemanticIntent.fromCode("unknown_intent_xyz"));
        assertEquals(SemanticIntent.CLICK, SemanticIntent.fromString("unknown_intent_xyz", SemanticIntent.CLICK));
    }
}
