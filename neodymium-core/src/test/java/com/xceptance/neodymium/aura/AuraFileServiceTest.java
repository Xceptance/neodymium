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
package com.xceptance.neodymium.aura;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AuraFileService.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AuraFileServiceTest
{
    @Test
    public void testCreateYamlFileCreatesEmptyFile() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String fileName = "test_empty_creation_unit.yaml";

        try
        {
            fileService.createYamlFile(fileName);

            final String content = fileService.readYamlFileContent(fileName);
            Assertions.assertNotNull(content);
            Assertions.assertEquals("", content);
        }
        finally
        {
            fileService.deleteYamlFile(fileName);
        }
    }

    @Test
    public void testCreateYmlFileCreatesEmptyFile() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String fileName = "test_empty_creation_unit.yml";

        try
        {
            fileService.createYamlFile(fileName);

            final String content = fileService.readYamlFileContent(fileName);
            Assertions.assertNotNull(content);
            Assertions.assertEquals("", content);
        }
        finally
        {
            fileService.deleteYamlFile(fileName);
        }
    }

    @Test
    public void testParsePlaybookSectionsWithEmptyContent()
    {
        final AuraFileService fileService = new AuraFileService();
        final Map<String, Object> sections = fileService.parsePlaybookSections("");

        Assertions.assertNotNull(sections);
        Assertions.assertEquals(List.of(), sections.get("beforeSteps"));
        Assertions.assertEquals(List.of(), sections.get("mainSteps"));
        Assertions.assertEquals(List.of(), sections.get("afterSteps"));
        Assertions.assertEquals(List.of(), sections.get("dataMatrix"));
        Assertions.assertEquals(List.of(), sections.get("varKeys"));
    }

    @Test
    public void testGetFilteredStepsFilesList() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String stepFileName = "unit_test_fragment_search.steps";
        try
        {
            fileService.createYamlFile(stepFileName);
            fileService.saveYamlFileContent(stepFileName, "steps:\n  - Click login button\n");

            final List<String> allSteps = fileService.getFilteredStepsFilesList("");
            Assertions.assertNotNull(allSteps);
            Assertions.assertTrue(allSteps.stream().anyMatch(s -> s.contains(stepFileName)));

            final List<String> filteredByName = fileService.getFilteredStepsFilesList("unit_test_fragment");
            Assertions.assertTrue(filteredByName.stream().anyMatch(s -> s.contains(stepFileName)));

            final List<String> filteredByContent = fileService.getFilteredStepsFilesList("Click login button");
            Assertions.assertTrue(filteredByContent.stream().anyMatch(s -> s.contains(stepFileName)));

            final List<String> filteredNoMatch = fileService.getFilteredStepsFilesList("non_existent_search_query_xyz");
            Assertions.assertFalse(filteredNoMatch.stream().anyMatch(s -> s.contains(stepFileName)));
        }
        finally
        {
            fileService.deleteYamlFile(stepFileName);
        }
    }
}
