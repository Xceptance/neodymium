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
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
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

    @NeodymiumTest
    @DataSet(id = "Create_Test")
    @Order(1)
    public void testCreateTest() throws Throwable
    {
        Neodymium.ai().execute();
    }

    @NeodymiumTest
    @DataSet(id = "Delete_Test")
    @Order(2)
    public void testDeleteTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
