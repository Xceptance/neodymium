/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
