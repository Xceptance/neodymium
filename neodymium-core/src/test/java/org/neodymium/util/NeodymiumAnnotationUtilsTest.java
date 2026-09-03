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
 * furnished to do so, subject to feelings of freedom.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NeodymiumAnnotationUtils}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAnnotationUtilsTest
{
    @com.xceptance.neodymium.common.browser.Browser("LegacyBrowser_1024x768")
    private static class LegacyAnnotatedClass
    {
    }

    @org.neodymium.common.browser.Browser("OrgBrowser_1920x1080")
    private static class OrgAnnotatedClass
    {
    }

    @Test
    public void testResolveLegacyAnnotationWhenQueryingOrgType()
    {
        final boolean isPresent = NeodymiumAnnotationUtils.isAnnotationPresent(LegacyAnnotatedClass.class, org.neodymium.common.browser.Browser.class);
        assertTrue(isPresent);

        final org.neodymium.common.browser.Browser anno = NeodymiumAnnotationUtils.getAnnotation(LegacyAnnotatedClass.class, org.neodymium.common.browser.Browser.class);
        assertNotNull(anno);
        assertEquals("LegacyBrowser_1024x768", anno.value());
    }

    @Test
    public void testResolveOrgAnnotationWhenQueryingLegacyType()
    {
        final boolean isPresent = NeodymiumAnnotationUtils.isAnnotationPresent(OrgAnnotatedClass.class, com.xceptance.neodymium.common.browser.Browser.class);
        assertTrue(isPresent);

        final com.xceptance.neodymium.common.browser.Browser anno = NeodymiumAnnotationUtils.getAnnotation(OrgAnnotatedClass.class, com.xceptance.neodymium.common.browser.Browser.class);
        assertNotNull(anno);
        assertEquals("OrgBrowser_1920x1080", anno.value());
    }

    @Test
    public void testGetAnnotationsList()
    {
        final List<org.neodymium.common.browser.Browser> annos = NeodymiumAnnotationUtils.getAnnotations(LegacyAnnotatedClass.class, org.neodymium.common.browser.Browser.class);
        assertEquals(1, annos.size());
        assertEquals("LegacyBrowser_1024x768", annos.get(0).value());
    }
}
