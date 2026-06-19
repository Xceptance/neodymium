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
package com.xceptance.neodymium.ai.testing;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Unit tests verifying dynamic playbook directory resolution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookDirectoryResolutionTest
{
    private static final String DEFAULT_PLAYBOOK_DIR = "src/test/resources/ai-playbooks/";

    @BeforeEach
    public final void setUp()
    {
        // Ensure clean state before each test
        clearSystemProperties();
        Neodymium.getData().clear();
    }

    @AfterEach
    public final void tearDown()
    {
        // Clean up state after each test
        clearSystemProperties();
        Neodymium.getData().clear();
    }

    private void clearSystemProperties()
    {
        System.clearProperty("playbook.directory.global");
        System.clearProperty("playbook.directory.package.com.xceptance.neodymium.ai");
        System.clearProperty("playbook.directory.class.com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest");
        System.clearProperty("playbook.directory.method.com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest::testMethod");
    }

    /**
     * Verifies that the global fallback defaults correctly.
     */
    @Test
    public final void testDefaultGlobalFallback()
    {
        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith(DEFAULT_PLAYBOOK_DIR));
    }

    /**
     * Verifies that the global property override is respected.
     */
    @Test
    public final void testGlobalOverride()
    {
        System.setProperty("playbook.directory.global", "custom/global/path");
        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith("custom/global/path/"));
    }

    /**
     * Verifies that package-level override works and takes precedence over global.
     */
    @Test
    public final void testPackageLevelOverride()
    {
        System.setProperty("playbook.directory.global", "custom/global/path");
        System.setProperty("playbook.directory.package.com.xceptance.neodymium.ai", "custom/package/path");

        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith("custom/package/path/"));
    }

    /**
     * Verifies that class-level override works and takes precedence over package-level and global.
     */
    @Test
    public final void testClassLevelOverride()
    {
        System.setProperty("playbook.directory.global", "custom/global/path");
        System.setProperty("playbook.directory.package.com.xceptance.neodymium.ai", "custom/package/path");
        System.setProperty("playbook.directory.class.com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest", "custom/class/path");

        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith("custom/class/path/"));
    }

    /**
     * Verifies that method-level override works and takes precedence over class-level.
     */
    @Test
    public final void testMethodLevelOverride()
    {
        System.setProperty("playbook.directory.class.com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest", "custom/class/path");
        System.setProperty("playbook.directory.method.com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest::testMethod", "custom/method/path");

        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith("custom/method/path/"));
    }

    /**
     * Verifies that Neodymium.getData() overrides are respected over System properties.
     */
    @Test
    public final void testNeodymiumDataOverride()
    {
        System.setProperty("playbook.directory.global", "system/global");
        Neodymium.getData().put("playbook.directory.global", "data/global");

        final File file = PlaybookManager.getPlaybookFile("com.xceptance.neodymium.ai.testing.PlaybookDirectoryResolutionTest :: testMethod");
        final String path = file.getPath();
        Assertions.assertTrue(path.startsWith("data/global/"));
    }
}
