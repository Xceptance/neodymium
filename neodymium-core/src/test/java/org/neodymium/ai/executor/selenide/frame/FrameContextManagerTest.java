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
package org.neodymium.ai.executor.selenide.frame;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FrameContextManager}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class FrameContextManagerTest
{
    @AfterEach
    public void tearDown()
    {
        FrameContextManager.resetActiveFramePath();
    }

    @Test
    public void testIsDefaultContent()
    {
        Assertions.assertTrue(FrameContextManager.isDefaultContent(null));
        Assertions.assertTrue(FrameContextManager.isDefaultContent(""));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("   "));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("main"));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("default"));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("defaultContent"));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("win_0:main"));
        Assertions.assertTrue(FrameContextManager.isDefaultContent("win_2:main"));

        Assertions.assertFalse(FrameContextManager.isDefaultContent("#pact1"));
        Assertions.assertFalse(FrameContextManager.isDefaultContent("win_0:main >>> #pact1"));
    }

    @Test
    public void testNormalizeFramePath()
    {
        Assertions.assertEquals("", FrameContextManager.normalizeFramePath(null));
        Assertions.assertEquals("", FrameContextManager.normalizeFramePath(""));
        Assertions.assertEquals("", FrameContextManager.normalizeFramePath("main"));
        Assertions.assertEquals("", FrameContextManager.normalizeFramePath("win_0:main"));
        Assertions.assertEquals("#pact1", FrameContextManager.normalizeFramePath("#pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.normalizeFramePath("win_0:main >>> #pact1"));
        Assertions.assertEquals("#pact1 >>> #pact2", FrameContextManager.normalizeFramePath("win_0:main >>> #pact1 >>> #pact2"));
        Assertions.assertEquals("#pact1 >>> #pact2 >>> #pact3", FrameContextManager.normalizeFramePath("win_1:main >>> #pact1 >>> #pact2 >>> #pact3"));
    }

    @Test
    public void testParseFrameHierarchy()
    {
        Assertions.assertEquals(List.of(), FrameContextManager.parseFrameHierarchy(null));
        Assertions.assertEquals(List.of(), FrameContextManager.parseFrameHierarchy("win_0:main"));
        Assertions.assertEquals(List.of("#pact1"), FrameContextManager.parseFrameHierarchy("#pact1"));
        Assertions.assertEquals(List.of("#pact1"), FrameContextManager.parseFrameHierarchy("win_0:main >>> #pact1"));
        Assertions.assertEquals(
            List.of("#pact1", "#pact2", "#pact3"),
            FrameContextManager.parseFrameHierarchy("win_0:main >>> #pact1 >>> #pact2 >>> #pact3")
        );
    }

    @Test
    public void testActiveFramePathThreadLocal()
    {
        Assertions.assertNull(FrameContextManager.getActiveFramePath());

        FrameContextManager.setActiveFramePath("win_0:main >>> #pact1");
        Assertions.assertEquals("#pact1", FrameContextManager.getActiveFramePath());

        FrameContextManager.setActiveFramePath("win_0:main");
        Assertions.assertNull(FrameContextManager.getActiveFramePath());
    }

    @Test
    public void testNoOpFrameScopeForNullDriver()
    {
        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(null, "#pact1"))
        {
            Assertions.assertNotNull(scope);
        }
    }

    @Test
    public void testNoOpFrameScopeForDefaultContent()
    {
        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(null, "win_0:main"))
        {
            Assertions.assertNotNull(scope);
        }
    }

    @Test
    public void testCombinePreventsDuplication()
    {
        // Exact match deduplication
        Assertions.assertEquals("#pact1 >>> #pact2", FrameContextManager.combine("#pact1 >>> #pact2", "#pact1 >>> #pact2"));

        // Frame is prefix of prefix
        Assertions.assertEquals("#pact1 >>> #pact2 >>> #pact3", FrameContextManager.combine("#pact1", "#pact1 >>> #pact2 >>> #pact3"));

        // Prefix is prefix of frame
        Assertions.assertEquals("#pact1 >>> #pact2 >>> #pact3", FrameContextManager.combine("#pact1 >>> #pact2 >>> #pact3", "#pact1"));

        // Normal append
        Assertions.assertEquals("#pact1 >>> #pact2", FrameContextManager.combine("#pact1", "#pact2"));

        // Window prefix normalization and deduplication
        Assertions.assertEquals("#pact1", FrameContextManager.combine("win_0:main >>> #pact1", "#pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.combine("#pact1", "win_0:main >>> #pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.combine("win_0:main", "#pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.combine("#pact1", "win_0:main"));

        // Tail frame deduplication
        Assertions.assertEquals("#pact1 >>> #pact2", FrameContextManager.combine("#pact1 >>> #pact2", "#pact2"));

        // Multi-segment sliding-window overlap
        Assertions.assertEquals("#pact1 >>> #pact2 >>> #pact3", FrameContextManager.combine("#pact1 >>> #pact2", "#pact2 >>> #pact3"));
        Assertions.assertEquals("#a >>> #b >>> #c >>> #d", FrameContextManager.combine("#a >>> #b >>> #c", "#b >>> #c >>> #d"));
    }

    @Test
    public void testNormalizeAndParseWithFlexibleSpacing()
    {
        Assertions.assertEquals("#pact1", FrameContextManager.normalizeFramePath("main>>>#pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.normalizeFramePath("main >>> #pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.normalizeFramePath("win_0:main>>>#pact1"));

        Assertions.assertEquals(
            List.of("#pact1", "#pact2"),
            FrameContextManager.parseFrameHierarchy("#pact1>>>#pact2")
        );
        Assertions.assertEquals(
            List.of("#pact1", "#pact2"),
            FrameContextManager.parseFrameHierarchy("main >>> #pact1 >>> #pact2")
        );
    }

    @Test
    public void testSplitFrameTargetSupportsXPathWithAtSign()
    {
        final FrameContextManager.FrameTarget target1 = FrameContextManager.splitFrameTarget("#pact1 >>> //input[@id='inp_val']");
        Assertions.assertEquals("#pact1", target1.frame());
        Assertions.assertEquals("//input[@id='inp_val']", target1.target());

        final FrameContextManager.FrameTarget target2 = FrameContextManager.splitFrameTarget("//iframe[@id='frame1'] >>> //button[@name='login']");
        Assertions.assertEquals("//iframe[@id='frame1']", target2.frame());
        Assertions.assertEquals("//button[@name='login']", target2.target());
    }

    @Test
    public void testSplitFrameTargetSupportsCssAttributeWithAtSign()
    {
        final FrameContextManager.FrameTarget target = FrameContextManager.splitFrameTarget("#pact1 >>> a[href*='test@example.com']");
        Assertions.assertEquals("#pact1", target.frame());
        Assertions.assertEquals("a[href*='test@example.com']", target.target());
    }

    @Test
    public void testSplitFrameTargetProtectsQuotedDelimiterInElementSelector()
    {
        final FrameContextManager.FrameTarget singleQuoted = FrameContextManager.splitFrameTarget("#frame >>> //button[text()='Next >>>']");
        Assertions.assertEquals("#frame", singleQuoted.frame());
        Assertions.assertEquals("//button[text()='Next >>>']", singleQuoted.target());

        final FrameContextManager.FrameTarget doubleQuoted = FrameContextManager.splitFrameTarget("#frame >>> button[aria-label=\"Next >>>\"]");
        Assertions.assertEquals("#frame", doubleQuoted.frame());
        Assertions.assertEquals("button[aria-label=\"Next >>>\"]", doubleQuoted.target());
    }

    @Test
    public void testSplitFrameTargetFramedAnchoredCoordinates()
    {
        final FrameContextManager.FrameTarget target = FrameContextManager.splitFrameTarget("#pact1 >>> #canvas@50,100");
        Assertions.assertEquals("#pact1", target.frame());
        Assertions.assertEquals("#canvas@50,100", target.target());
    }
}
