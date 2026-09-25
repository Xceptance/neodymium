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
        Assertions.assertEquals(Map.of(), sections.get("fragmentVarScopes"));
    }

    @Test
    public void testParsePlaybookSectionsWithFragmentVariables()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = "steps:\n  - Open ${neodymium.url}\nvariables:\n  neodymium.url: defined\n  user: required";
        final Map<String, Object> sections = fileService.parsePlaybookSections(content);

        Assertions.assertNotNull(sections);
        Assertions.assertEquals(List.of("Open ${neodymium.url}"), sections.get("mainSteps"));
        final Map<String, String> expectedScopes = Map.of("neodymium.url", "defined", "user", "required");
        Assertions.assertEquals(expectedScopes, sections.get("fragmentVarScopes"));
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

    @Test
    public void testParsePlaybookSectionsWithFragmentIncludes()
    {
        final AuraFileService fileService = new AuraFileService();

        final String listYaml = "- Step 1\n- _include: fragments/child.steps\n- include: fragments/other.steps";
        final Map<String, Object> listSections = fileService.parsePlaybookSections(listYaml);
        Assertions.assertNotNull(listSections);
        Assertions.assertEquals(List.of("Step 1", "_include: fragments/child.steps", "_include: fragments/other.steps"), listSections.get("mainSteps"));

        final String singleIncludeYaml = "_include: fragments/header.steps";
        final Map<String, Object> singleSections = fileService.parsePlaybookSections(singleIncludeYaml);
        Assertions.assertNotNull(singleSections);
        Assertions.assertEquals(List.of("_include: fragments/header.steps"), singleSections.get("mainSteps"));
    }

    @Test
    public void testGetRequiredVariablesForFragment() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String stepFileName = "unit_test_req_vars.steps";
        try
        {
            fileService.createYamlFile(stepFileName);
            final String yamlContent = "steps:\n  - Open ${neodymium.url}\n  - Type \"${username}\" into \"#user\"\n  - Type \"${password}\" into \"#pass\"\nvariables:\n  neodymium.url: defined\n  username: required\n";
            fileService.saveYamlFileContent(stepFileName, yamlContent);

            final List<String> reqVars = fileService.getRequiredVariablesForFragment(stepFileName);
            Assertions.assertNotNull(reqVars);
            Assertions.assertEquals(List.of("username", "password"), reqVars);

            final Map<String, String> reqMap = fileService.getFragmentRequiredVariablesMap();
            Assertions.assertNotNull(reqMap);
            Assertions.assertTrue(reqMap.containsKey(stepFileName));
            Assertions.assertEquals("username, password", reqMap.get(stepFileName));
        }
        finally
        {
            fileService.deleteYamlFile(stepFileName);
        }
    }

    @Test
    public void testParsePlaybookSectionsWithInlineColons()
    {
        final AuraFileService fileService = new AuraFileService();
        final String stepContent = "- Remove all non-numeric characters from ${numberOfAddedProducts} and store the result in numberOfAddedProducts (hint: use java method, no browser needed).\n"
            + "- Multiply ${numberOfAddedProducts} with ${quantityToAdd} and save the result in numberOfAddedProducts (hint: use java method, no browser needed).\n"
            + "- If ${addedProducts} is valid JSON, then merge ${productsAddedInTheRound} with ${addedProducts} and store in addedProducts (hint: use java method), else store ${productsAddedInTheRound} as addedProducts variable.";

        final Map<String, Object> sections = fileService.parsePlaybookSections(stepContent);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(3, mainSteps.size());

        Assertions.assertEquals("Remove all non-numeric characters from ${numberOfAddedProducts} and store the result in numberOfAddedProducts (hint: use java method, no browser needed).", mainSteps.get(0));
        Assertions.assertEquals("Multiply ${numberOfAddedProducts} with ${quantityToAdd} and save the result in numberOfAddedProducts (hint: use java method, no browser needed).", mainSteps.get(1));
        Assertions.assertEquals("If ${addedProducts} is valid JSON, then merge ${productsAddedInTheRound} with ${addedProducts} and store in addedProducts (hint: use java method), else store ${productsAddedInTheRound} as addedProducts variable.", mainSteps.get(2));
    }
}

