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
package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * End-to-end tests for the Aura Manager workspace management.
 * 
 * // AI-generated: Gemini 2.5 Pro
 */
@DataFile("ai-test-pages/aura-manager-workspace-test.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuraManagerWorkspaceTest extends BaseAuraManagerUiTest
{
    public AuraManagerWorkspaceTest()
    {
        super(18150);
    }

    @BeforeEach
    public void prepareWorkspaceState(final TestInfo testInfo) throws IOException
    {
        final String methodName = testInfo.getTestMethod().get().getName();
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if ("testCreateTest".equals(methodName))
        {
            if (targetFile.exists())
            {
                Files.delete(targetFile.toPath());
            }
        }
        else if ("testDeleteTest".equals(methodName))
        {
            if (!targetFile.exists())
            {
                final String boilerplate = "# Neodymium YAML Test Data File\n" +
                                           "steps: |\n" +
                                           "  Open browser\n" +
                                           "data:\n" +
                                           "  - testId: \"Automated Workspace Test\"\n";
                Files.writeString(targetFile.toPath(), boilerplate, StandardCharsets.UTF_8);
            }
        }
    }

    @NeodymiumTest
    @DataSet(id = "Create_Test")
    @Order(1)
    public void testCreateTest()
    {
        assertMultiPhaseExecution();
    }

    @NeodymiumTest
    @DataSet(id = "Delete_Test")
    @Order(2)
    public void testDeleteTest()
    {
        assertMultiPhaseExecution();
    }
}
