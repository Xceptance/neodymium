/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying browser profile classification and mapping.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraBrowserSelectionTest
{
    private AuraQueueService queueService;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp()
    {
        final AuraInteractiveService interactiveService = new AuraInteractiveService();
        this.queueService = new AuraQueueService(interactiveService);
    }

    @Test
    public void testBrowserProfilesClassification()
    {
        final List<BrowserProfileDto> profiles = queueService.getAvailableBrowserProfiles();
        Assertions.assertNotNull(profiles);
        Assertions.assertFalse(profiles.isEmpty());

        for (final BrowserProfileDto p : profiles)
        {
            Assertions.assertNotNull(p.id);
            Assertions.assertNotNull(p.browser);
            Assertions.assertTrue(List.of("chrome", "firefox", "safari", "edge", "mobile", "other").contains(p.browser),
                    "Browser type '" + p.browser + "' should be one of the known browser categories");
        }
    }

    @Test
    public void testBrowserProfilesAvailability()
    {
        final List<BrowserProfileDto> profiles = queueService.getAvailableBrowserProfiles();
        Assertions.assertNotNull(profiles);
        Assertions.assertFalse(profiles.isEmpty());

        final String osName = System.getProperty("os.name", "").toLowerCase();
        for (final BrowserProfileDto p : profiles)
        {
            if ("safari".equalsIgnoreCase(p.browser) && !osName.contains("mac"))
            {
                Assertions.assertFalse(p.available, "Safari should not be available on non-macOS platforms");
                Assertions.assertNotNull(p.unavailableReason);
                Assertions.assertTrue(p.unavailableReason.contains("macOS"),
                        "Expected explanation mentioning macOS, got: " + p.unavailableReason);
            }
            if (!p.available)
            {
                Assertions.assertNotNull(p.unavailableReason, "Unavailable profiles must provide an explanation");
                Assertions.assertFalse(p.unavailableReason.isBlank(), "Unavailable reason cannot be blank");
            }
        }
    }

    @Test
    public void testEffectiveBrowserProfilesFiltersUnavailable()
    {
        final String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac"))
        {
            final DatasetSelection selection = new DatasetSelection();
            selection.browserProfiles = List.of("Safari_1920x1080");

            final List<String> effective = queueService.getEffectiveBrowserProfiles(selection, null);
            Assertions.assertNotNull(effective);
            Assertions.assertFalse(effective.isEmpty());
            Assertions.assertFalse(effective.contains("Safari_1920x1080"),
                    "Unavailable Safari profile must be filtered out on non-macOS platforms");
        }
    }

    @Test
    public void testEffectiveBrowserProfilesDefaultsToChrome()
    {
        final List<String> effective = queueService.getEffectiveBrowserProfiles(null, null);
        Assertions.assertNotNull(effective);
        Assertions.assertFalse(effective.isEmpty());

        final String defaultProfile = effective.get(0).toLowerCase();
        Assertions.assertTrue(defaultProfile.contains("chrome"),
                "Default browser profile should be a Chrome profile, but got: " + effective.get(0));
    }
}
