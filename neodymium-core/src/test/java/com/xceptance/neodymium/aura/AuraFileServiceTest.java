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

import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public void testParsePlaybookSectionsWithRawTextAndVariables()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = "- Open ${neodymium.url}\n- Type ${user}\nvariables:\n  neodymium.url: defined\n  user: required";
        final Map<String, Object> sections = fileService.parsePlaybookSections(content);

        Assertions.assertNotNull(sections);
        Assertions.assertEquals(List.of("Open ${neodymium.url}", "Type ${user}"), sections.get("mainSteps"));
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

    @Test
    public void testRenameOrMoveYamlFile() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String oldName = "test_rename_source.yaml";
        final String newName = "test_rename_target.yaml";

        try
        {
            fileService.createYamlFile(oldName);
            fileService.saveYamlFileContent(oldName, "steps:\n  - Open ${url}\n");
            fileService.setActiveEditingFile(oldName);

            final boolean success = fileService.renameOrMoveFile(oldName, newName);
            Assertions.assertTrue(success);
            Assertions.assertNull(fileService.readYamlFileContent(oldName));
            Assertions.assertNotNull(fileService.readYamlFileContent(newName));
            Assertions.assertEquals(newName, fileService.getActiveEditingFile());
        }
        finally
        {
            fileService.deleteYamlFile(oldName);
            fileService.deleteYamlFile(newName);
        }
    }

    @Test
    public void testRenameOrMoveStepsFileToSubfolder() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String oldName = "test_move_fragment.steps";
        final String newName = "subfolder_test/moved_fragment.steps";

        try
        {
            fileService.createYamlFile(oldName);
            fileService.saveYamlFileContent(oldName, "steps:\n  - Click button\n");

            final boolean success = fileService.renameOrMoveFile(oldName, newName);
            Assertions.assertTrue(success);
            Assertions.assertNull(fileService.readYamlFileContent(oldName));
            Assertions.assertNotNull(fileService.readYamlFileContent(newName));
        }
        finally
        {
            fileService.deleteYamlFile(oldName);
            fileService.deleteYamlFile(newName);
        }
    }

    @Test
    public void testRenameOrMoveFileDestinationExistsThrowsException() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String file1 = "test_exists_1.yaml";
        final String file2 = "test_exists_2.yaml";

        try
        {
            fileService.createYamlFile(file1);
            fileService.createYamlFile(file2);

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                fileService.renameOrMoveFile(file1, file2);
            });
        }
        finally
        {
            fileService.deleteYamlFile(file1);
            fileService.deleteYamlFile(file2);
        }
    }

    @Test
    public void testRenameOrMoveFileDirectoryTraversalThrowsException() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String source = "test_traversal.yaml";

        try
        {
            fileService.createYamlFile(source);

            Assertions.assertThrows(SecurityException.class, () -> {
                fileService.renameOrMoveFile(source, "../../forbidden.yaml");
            });
        }
        finally
        {
            fileService.deleteYamlFile(source);
        }
    }

    @Test
    public void testUpdateFragmentReferences_updatesMatchingFiles() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String fragmentOld = "fragments/login.steps";
        final String fragmentNew = "auth/login.steps";
        final String testFile = "test_ref_update_host.yaml";

        try
        {
            // Create a YAML test file that includes the fragment via _include:.
            fileService.createYamlFile(testFile);
            fileService.saveYamlFileContent(testFile,
                    "steps:\n  - _include: fragments/login.steps\n  - Open ${url}\n");

            final AuraFileService.UpdateFragmentRefsResult result =
                    fileService.updateFragmentReferences(fragmentOld, fragmentNew);

            Assertions.assertEquals(1, result.updatedFileCount());
            Assertions.assertEquals(1, result.updatedFiles().size());

            // The actual file content must contain the new path, not the old one.
            final String updated = fileService.readYamlFileContent(testFile);
            Assertions.assertNotNull(updated);
            Assertions.assertTrue(updated.contains(fragmentNew),
                    "Expected new fragment path in content");
            Assertions.assertFalse(updated.contains(fragmentOld),
                    "Expected old fragment path to be replaced");
        }
        finally
        {
            fileService.deleteYamlFile(testFile);
        }
    }

    @Test
    public void testUpdateFragmentReferences_noMatchingFiles_returnsZero() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String testFile = "test_ref_update_nomatch.yaml";

        try
        {
            // Create a YAML file that does NOT reference the fragment.
            fileService.createYamlFile(testFile);
            fileService.saveYamlFileContent(testFile, "steps:\n  - Open ${url}\n");

            final AuraFileService.UpdateFragmentRefsResult result =
                    fileService.updateFragmentReferences("fragments/nonexistent.steps", "auth/nonexistent.steps");

            Assertions.assertEquals(0, result.updatedFileCount());
            Assertions.assertTrue(result.updatedFiles().isEmpty());
        }
        finally
        {
            fileService.deleteYamlFile(testFile);
        }
    }

    @Test
    public void testUpdateFragmentReferences_blankPaths_returnsZero() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();

        final AuraFileService.UpdateFragmentRefsResult result =
                fileService.updateFragmentReferences("", null);

        Assertions.assertEquals(0, result.updatedFileCount());
        Assertions.assertTrue(result.updatedFiles().isEmpty());
    }

    @Test
    public void testFindAndLoadPlaybookDetails()
    {
        final AuraFileService fileService = new AuraFileService();
        final java.io.File pbFile = fileService.findPlaybookFile("verla/SearchTest.yaml", "normal");
        Assertions.assertNotNull(pbFile, "Playbook JSON file for verla/SearchTest.yaml [normal] should be found");
        Assertions.assertTrue(fileService.hasPlaybook("verla/SearchTest.yaml", "normal"));

        final Map<String, Object> details = fileService.loadPlaybookDetails("verla/SearchTest.yaml", "normal");
        Assertions.assertNotNull(details);
        Assertions.assertEquals("verla/SearchTest.yaml", details.get("file"));
        Assertions.assertEquals("normal", details.get("datasetId"));
        Assertions.assertNotNull(details.get("steps"));
        Assertions.assertTrue(((Integer) details.get("totalSteps")) > 0);
        Assertions.assertNotNull(details.get("generatedAt"));
        Assertions.assertNotNull(details.get("lastModifiedEpoch"));
        Assertions.assertNotNull(details.get("testFileLastEdited"));
        Assertions.assertNotNull(details.get("testFileLastModifiedEpoch"));
    }

    @Test
    public void testLoadPlaybookDetailsWithHealedStepReconcilesOriginalTarget() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final File resourcesDir = fileService.getResourcesDirectory();
        final String testFileName = "test_healed_reconcile.yaml";
        final File testFile = new File(resourcesDir, testFileName);
        final File jsonFile = new File(resourcesDir, "test_healed_reconcile.json");

        try
        {
            Files.writeString(testFile.toPath(), "test:\n  - step: Click button\n", StandardCharsets.UTF_8);
            final String samplePlaybook = """
                [
                  {
                    "instruction": "Click button",
                    "status": "HEALED",
                    "actions": [
                      {
                        "type": "CLICK",
                        "target": "#healedButton",
                        "originalTarget": null,
                        "expectedTarget": null,
                        "healed": false
                      }
                    ],
                    "toolCalls": [
                      {
                        "callId": "c1",
                        "toolName": "click",
                        "arguments": {
                          "selector": "#brokenButton"
                        }
                      }
                    ]
                  }
                ]
                """;
            Files.writeString(jsonFile.toPath(), samplePlaybook, StandardCharsets.UTF_8);

            final Map<String, Object> details = fileService.loadPlaybookDetails(testFileName, null);
            Assertions.assertNotNull(details);
            Assertions.assertEquals(1, details.get("healedSteps"));
            Assertions.assertEquals(1, details.get("totalSteps"));

            final List<?> steps = (List<?>) details.get("steps");
            Assertions.assertEquals(1, steps.size());
            final Map<?, ?> step = (Map<?, ?>) steps.get(0);
            final List<?> actions = (List<?>) step.get("actions");
            Assertions.assertEquals(1, actions.size());

            final Map<?, ?> action = (Map<?, ?>) actions.get(0);
            Assertions.assertEquals("#healedButton", action.get("target"));
            Assertions.assertEquals("#brokenButton", action.get("expectedTarget"));
            Assertions.assertEquals("#brokenButton", action.get("originalTarget"));
            Assertions.assertEquals(true, action.get("healed"));
        }
        finally
        {
            if (testFile.exists())
            {
                testFile.delete();
            }
            if (jsonFile.exists())
            {
                jsonFile.delete();
            }
        }
    }

    @Test
    public final void testAcceptPlaybookHealingSingleStep() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final File resourcesDir = fileService.getResourcesDirectory();
        final String testFileName = "test_accept_healing.yaml";
        final File testFile = new File(resourcesDir, testFileName);
        final File jsonFile = new File(resourcesDir, "test_accept_healing.json");

        try
        {
            Files.writeString(testFile.toPath(), "test:\n  - step: Click button\n", StandardCharsets.UTF_8);
            final String samplePlaybook = """
                [
                  {
                    "instruction": "Click button",
                    "status": "HEALED",
                    "actions": [
                      {
                        "type": "CLICK",
                        "target": "#newButton",
                        "originalTarget": "#oldButton",
                        "expectedTarget": "#oldButton",
                        "healed": true
                      }
                    ],
                    "toolCalls": [
                      {
                        "callId": "c1",
                        "toolName": "click",
                        "arguments": {
                          "selector": "#oldButton"
                        }
                      }
                    ]
                  }
                ]
                """;
            Files.writeString(jsonFile.toPath(), samplePlaybook, StandardCharsets.UTF_8);

            final boolean accepted = fileService.acceptPlaybookHealing(testFileName, null, 0);
            Assertions.assertTrue(accepted);

            final Map<String, Object> details = fileService.loadPlaybookDetails(testFileName, null);
            Assertions.assertNotNull(details);
            Assertions.assertEquals(0, details.get("healedSteps"));
            Assertions.assertEquals(1, details.get("passedSteps"));

            final List<?> steps = (List<?>) details.get("steps");
            Assertions.assertEquals(1, steps.size());
            final Map<?, ?> step = (Map<?, ?>) steps.get(0);
            Assertions.assertEquals("SUCCESS", step.get("status"));

            final List<?> actions = (List<?>) step.get("actions");
            final Map<?, ?> action = (Map<?, ?>) actions.get(0);
            Assertions.assertEquals("#newButton", action.get("target"));
            Assertions.assertNull(action.get("expectedTarget"));
            Assertions.assertNull(action.get("healed"));

            // Check JSON content directly on disk
            final String updatedDiskContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            Assertions.assertTrue(updatedDiskContent.contains("\"selector\" : \"#newButton\"") || updatedDiskContent.contains("\"selector\": \"#newButton\""));
            Assertions.assertFalse(updatedDiskContent.contains("#oldButton"));
        }
        finally
        {
            if (testFile.exists())
            {
                testFile.delete();
            }
            if (jsonFile.exists())
            {
                jsonFile.delete();
            }
        }
    }

    @Test
    public final void testAcceptPlaybookHealingAllSteps() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final File resourcesDir = fileService.getResourcesDirectory();
        final String testFileName = "test_accept_healing_all.yaml";
        final File testFile = new File(resourcesDir, testFileName);
        final File jsonFile = new File(resourcesDir, "test_accept_healing_all.json");

        try
        {
            Files.writeString(testFile.toPath(), "test:\n  - step: Click\n  - step: Type\n", StandardCharsets.UTF_8);
            final String samplePlaybook = """
                [
                  {
                    "instruction": "Click",
                    "status": "HEALED",
                    "actions": [
                      {
                        "type": "CLICK",
                        "target": "#newBtn",
                        "originalTarget": "#oldBtn",
                        "expectedTarget": "#oldBtn",
                        "healed": true
                      }
                    ],
                    "toolCalls": [
                      {
                        "callId": "c1",
                        "toolName": "click",
                        "arguments": {
                          "selector": "#oldBtn"
                        }
                      }
                    ]
                  },
                  {
                    "instruction": "Type",
                    "status": "SUCCESS",
                    "actions": [
                      {
                        "type": "TYPE",
                        "target": "#inputField",
                        "value": "hello"
                      }
                    ]
                  }
                ]
                """;
            Files.writeString(jsonFile.toPath(), samplePlaybook, StandardCharsets.UTF_8);

            final boolean accepted = fileService.acceptPlaybookHealing(testFileName, null, null);
            Assertions.assertTrue(accepted);

            final Map<String, Object> details = fileService.loadPlaybookDetails(testFileName, null);
            Assertions.assertNotNull(details);
            Assertions.assertEquals(0, details.get("healedSteps"));
            Assertions.assertEquals(2, details.get("passedSteps"));

            final List<?> steps = (List<?>) details.get("steps");
            Assertions.assertEquals(2, steps.size());
            Assertions.assertEquals("SUCCESS", ((Map<?, ?>) steps.get(0)).get("status"));
            Assertions.assertEquals("SUCCESS", ((Map<?, ?>) steps.get(1)).get("status"));
        }
        finally
        {
            if (testFile.exists())
            {
                testFile.delete();
            }
            if (jsonFile.exists())
            {
                jsonFile.delete();
            }
        }
    }

    @Test
    public void testParsePlaybookSectionsWithFoldedBlockScalarSteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps:
              - Click the "Add to Cart" button.
              - >
                Verify the cart header is displayed.
                Verify the cart items section is displayed.
                Verify the product is displayed in the cart.
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(4, mainSteps.size(), "Folded block scalar lines must be parsed as individual discrete steps");
        Assertions.assertEquals("Click the \"Add to Cart\" button.", mainSteps.get(0));
        Assertions.assertEquals("Verify the cart header is displayed.", mainSteps.get(1));
        Assertions.assertEquals("Verify the cart items section is displayed.", mainSteps.get(2));
        Assertions.assertEquals("Verify the product is displayed in the cart.", mainSteps.get(3));
    }

    @Test
    public void testParsePlaybookSectionsWithFoldedBlockScalarChompedSteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps:
              - >-
                Verify the cart header is displayed.
                Verify the cart items section is displayed.
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(2, mainSteps.size());
        Assertions.assertEquals("  - Verify the cart header is displayed.", mainSteps.get(0));
        Assertions.assertEquals("  - Verify the cart items section is displayed.", mainSteps.get(1));
    }

    @Test
    public void testParsePlaybookSectionsWithLiteralBlockScalarSteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps:
              - |
                Verify subtotal is displayed.
                Verify total is displayed.
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(2, mainSteps.size());
        Assertions.assertEquals("Verify subtotal is displayed.", mainSteps.get(0));
        Assertions.assertEquals("Verify total is displayed.", mainSteps.get(1));
    }

    @Test
    public void testParsePlaybookSectionsWithHierarchicalMapSubSteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps:
              - Detailed verification of the cart page:
                - Verify the cart header is displayed.
                - Verify the product is displayed in the cart.
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(3, mainSteps.size());
        Assertions.assertEquals("Detailed verification of the cart page:", mainSteps.get(0));
        Assertions.assertEquals("Verify the cart header is displayed.", mainSteps.get(1));
        Assertions.assertEquals("Verify the product is displayed in the cart.", mainSteps.get(2));
    }

    @Test
    public void testParsePlaybookSectionsPreservesCommentsAndSubSteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps:
              # Add product to cart
              - Click the "Add to Cart" button.
              - Close bonus product modal or added to cart confirmation if displayed (optional)

              # Open cart
              - Click on the shopping cart icon in the header to open the cart and wait for cart loading to finish.

              # Detailed verification of the cart page
              - >-
                Verify the cart header is displayed.
                Verify the cart items section is displayed.
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(8, mainSteps.size(), "Must preserve comments and parse substeps");
        Assertions.assertEquals("# Add product to cart", mainSteps.get(0));
        Assertions.assertEquals("Click the \"Add to Cart\" button.", mainSteps.get(1));
        Assertions.assertEquals("Close bonus product modal or added to cart confirmation if displayed (optional)", mainSteps.get(2));
        Assertions.assertEquals("# Open cart", mainSteps.get(3));
        Assertions.assertEquals("Click on the shopping cart icon in the header to open the cart and wait for cart loading to finish.", mainSteps.get(4));
        Assertions.assertEquals("# Detailed verification of the cart page", mainSteps.get(5));
        Assertions.assertEquals("  - Verify the cart header is displayed.", mainSteps.get(6));
        Assertions.assertEquals("  - Verify the cart items section is displayed.", mainSteps.get(7));
    }

    @Test
    public void testParsePlaybookSectionsWithMultilineStepsAndFoldedSubsteps()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            steps: |
              Open https://verla.xceptance.com/index.html
              Locate the first product card and click its 'Add to Cart' or 'Add' button.
              If the page is an PDP layout then _include: fragments/configure-pdp-product.steps, else _include: fragments/add-simple-product-to-cart.steps
              # Detailed verification of the cart page
              - >-
                Verify the cart header is displayed.
                Verify the cart items section is displayed.
              Click checkout button
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);
        Assertions.assertEquals(Boolean.FALSE, sections.get("hasError"));

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(7, mainSteps.size(), "Must preserve multiline natural steps without hyphens and folded substeps");
        Assertions.assertEquals("Open https://verla.xceptance.com/index.html", mainSteps.get(0));
        Assertions.assertEquals("Locate the first product card and click its 'Add to Cart' or 'Add' button.", mainSteps.get(1));
        Assertions.assertEquals("If the page is an PDP layout then _include: fragments/configure-pdp-product.steps, else _include: fragments/add-simple-product-to-cart.steps", mainSteps.get(2));
        Assertions.assertEquals("# Detailed verification of the cart page", mainSteps.get(3));
        Assertions.assertEquals("  - Verify the cart header is displayed.", mainSteps.get(4));
        Assertions.assertEquals("  - Verify the cart items section is displayed.", mainSteps.get(5));
        Assertions.assertEquals("Click checkout button", mainSteps.get(6));
    }

    @Test
    public void testExtractAreaTagFromContent()
    {
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag("#@Checkout\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag("  #@Checkout\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag("# @Checkout\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag("@Checkout\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag("  @Checkout\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Area_123", AuraFileService.extractAreaTag("#@Area_123\nsteps:\n  - step 1"));
        Assertions.assertEquals("@Area-tag", AuraFileService.extractAreaTag("#@Area-tag\nsteps:\n  - step 1"));

        // Null and blank content
        Assertions.assertNull(AuraFileService.extractAreaTag(null));
        Assertions.assertNull(AuraFileService.extractAreaTag(""));
        Assertions.assertNull(AuraFileService.extractAreaTag("   \n\n  "));

        // Content without area tag
        Assertions.assertNull(AuraFileService.extractAreaTag("steps:\n  - step 1"));

        // Tag occurring inside or after section body should be ignored
        final String tagAfterSteps = "steps:\n  - step 1\n#@Checkout\n  - step 2";
        Assertions.assertNull(AuraFileService.extractAreaTag(tagAfterSteps));
    }

    @Test
    public void testExtractAreaTagFromFileSystem() throws IOException
    {
        final File tempFile = File.createTempFile("playbook-area-", ".yaml");
        try
        {
            Files.writeString(tempFile.toPath(), "#@Search\n# Test file\nsteps:\n  - Open url\n", StandardCharsets.UTF_8);
            final String tag = AuraFileService.extractAreaTagFromFileSystem(tempFile.getAbsolutePath());
            Assertions.assertEquals("@Search", tag);

            // Non-existent file
            Assertions.assertNull(AuraFileService.extractAreaTagFromFileSystem("/invalid/path/nonexistent.yaml"));
        }
        finally
        {
            tempFile.delete();
        }
    }

    @Test
    public void testExtractAreaTagFromStructuredYamlTags() throws IOException
    {
        final String yamlWithTagList = """
            tags:
              Area:
                - Search
                - Browsing
            steps: |
              Select the language
            """;
        Assertions.assertEquals("@Search", AuraFileService.extractAreaTag(yamlWithTagList));
        Assertions.assertEquals(List.of("@Search", "@Browsing"), AuraFileService.extractAreaTags(yamlWithTagList));

        final String yamlWithTagScalar = """
            tags:
              area: Checkout
            steps: |
              Proceed to payment
            """;
        Assertions.assertEquals("@Checkout", AuraFileService.extractAreaTag(yamlWithTagScalar));
        Assertions.assertEquals(List.of("@Checkout"), AuraFileService.extractAreaTags(yamlWithTagScalar));

        final String yamlWithPrefixedTag = """
            tags:
              Area:
                - @Account
            steps: |
              Open profile
            """;
        Assertions.assertEquals("@Account", AuraFileService.extractAreaTag(yamlWithPrefixedTag));
        Assertions.assertEquals(List.of("@Account"), AuraFileService.extractAreaTags(yamlWithPrefixedTag));

        final File tempFile = File.createTempFile("playbook-structured-area-", ".yaml");
        try
        {
            Files.writeString(tempFile.toPath(), yamlWithTagList, StandardCharsets.UTF_8);
            final String tag = AuraFileService.extractAreaTagFromFileSystem(tempFile.getAbsolutePath());
            Assertions.assertEquals("@Search", tag);
            Assertions.assertEquals(List.of("@Search", "@Browsing"), AuraFileService.extractAreaTagsFromFileSystem(tempFile.getAbsolutePath()));
        }
        finally
        {
            tempFile.delete();
        }
    }

    @Test
    public void testParsePlaybookSectionsWithAreaTag()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            #@Checkout
            # Neodymium YAML Test Data File
            steps: |
              Open https://verla.xceptance.com
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);
        Assertions.assertEquals("@Checkout", sections.get("areaTag"));
        Assertions.assertEquals(Boolean.FALSE, sections.get("hasError"));

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) sections.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(1, mainSteps.size());
        Assertions.assertEquals("Open https://verla.xceptance.com", mainSteps.get(0));
    }

    @Test
    public void testParsePlaybookSectionsWithRootAndDatasetTags()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            tags:
              tag3: value
              tag4:
                - value2
                - value3
            steps: |
              Open https://verla.xceptance.com
            data:
              -
                tags:
                  tag1: val1
                testId: "en"
                searchPhrase: "Neodymium magnet"
              -
                tags:
                  tag2:
                    - vA
                    - vB
                testId: "de"
                searchPhrase: "Magnet"
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);
        Assertions.assertEquals(Boolean.FALSE, sections.get("hasError"));

        @SuppressWarnings("unchecked")
        final Map<String, List<String>> rootTags = (Map<String, List<String>>) sections.get("rootTags");
        Assertions.assertNotNull(rootTags);
        Assertions.assertEquals(List.of("value"), rootTags.get("tag3"));
        Assertions.assertEquals(List.of("value2", "value3"), rootTags.get("tag4"));

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> datasetTagsList = (List<Map<String, List<String>>>) sections.get("datasetTagsList");
        Assertions.assertNotNull(datasetTagsList);
        Assertions.assertEquals(2, datasetTagsList.size());
        Assertions.assertEquals(List.of("val1"), datasetTagsList.get(0).get("tag1"));
        Assertions.assertEquals(List.of("vA", "vB"), datasetTagsList.get(1).get("tag2"));

        @SuppressWarnings("unchecked")
        final List<String> varKeys = (List<String>) sections.get("varKeys");
        Assertions.assertFalse(varKeys.contains("tags"));
        Assertions.assertTrue(varKeys.contains("testId"));
        Assertions.assertTrue(varKeys.contains("searchPhrase"));
    }

    @Test
    public void testExtractTagsInFileDetails() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String fileName = "test_tags_extract.yaml";
        final String yamlContent = """
            tags:
              feature: checkout
              smoke: true
            steps:
              - Open https://example.com
            data:
              - testId: "us"
                tags:
                  tier: p1
              - testId: "de"
                tags:
                  tier: p2
            """;

        try
        {
            fileService.saveYamlFileContent(fileName, yamlContent);
            final File file = new File(fileService.getResourcesDirectory(), fileName);
            final Map<String, Object> details = fileService.getFileDetails(file);

            Assertions.assertNotNull(details);
            @SuppressWarnings("unchecked")
            final Map<String, List<String>> rootTags = (Map<String, List<String>>) details.get("rootTags");
            Assertions.assertNotNull(rootTags);
            Assertions.assertEquals(List.of("checkout"), rootTags.get("feature"));
            Assertions.assertEquals(List.of("true"), rootTags.get("smoke"));

            @SuppressWarnings("unchecked")
            final Map<String, List<String>> allTags = (Map<String, List<String>>) details.get("tags");
            Assertions.assertNotNull(allTags);
            Assertions.assertEquals(List.of("checkout"), allTags.get("feature"));
            Assertions.assertEquals(List.of("true"), allTags.get("smoke"));
            Assertions.assertEquals(List.of("p1", "p2"), allTags.get("tier"));

            @SuppressWarnings("unchecked")
            final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
            Assertions.assertEquals(2, datasets.size());
            Assertions.assertEquals(List.of("p1"), datasets.get(0).tags.get("tier"));
            Assertions.assertEquals(List.of("p2"), datasets.get(1).tags.get("tier"));
        }
        finally
        {
            fileService.deleteYamlFile(fileName);
        }
    }

    @Test
    public void testMatchesTagQuery()
    {
        final Map<String, List<String>> tags = Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true"),
            "area", List.of("@Checkout"),
            "tier", List.of("p1", "critical")
        );

        // Exact key:value matches
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "feature:checkout"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "feature: checkout"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "feature=checkout"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "tag:feature:checkout"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "tags:smoke:true"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "tier:critical"));

        // Key only
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "feature"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "smoke"));

        // Value only
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "checkout"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "p1"));
        Assertions.assertTrue(AuraFileService.matchesTagQuery(tags, "@Checkout"));

        // Non-matches
        Assertions.assertFalse(AuraFileService.matchesTagQuery(tags, "feature:login"));
        Assertions.assertFalse(AuraFileService.matchesTagQuery(tags, "tier:p3"));
        Assertions.assertFalse(AuraFileService.matchesTagQuery(tags, "unrelated"));
        Assertions.assertFalse(AuraFileService.matchesTagQuery(null, "checkout"));
        Assertions.assertFalse(AuraFileService.matchesTagQuery(tags, ""));
    }

    @Test
    public void testExtractAvailableTestTags()
    {
        final YamlFileDto file1 = new YamlFileDto("test1.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true")
        ));
        final YamlFileDto file2 = new YamlFileDto("test2.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("login"),
            "smoke", List.of("true")
        ));

        final List<String> available = AuraFileService.extractAvailableTestTags(List.of(file1, file2));
        Assertions.assertEquals(3, available.size());
        Assertions.assertEquals(List.of("feature:checkout", "feature:login", "smoke:true"), available);
    }

    @Test
    public void testParseSearchTokens()
    {
        Assertions.assertTrue(AuraFileService.parseSearchTokens(null).isEmpty());
        Assertions.assertTrue(AuraFileService.parseSearchTokens("").isEmpty());
        Assertions.assertTrue(AuraFileService.parseSearchTokens("   ").isEmpty());

        Assertions.assertEquals(List.of("checkout"), AuraFileService.parseSearchTokens("checkout"));
        Assertions.assertEquals(List.of("feature:checkout", "priority:high"), AuraFileService.parseSearchTokens("feature:checkout, priority:high"));
        Assertions.assertEquals(List.of("feature:checkout", "priority:high", "smoke"), AuraFileService.parseSearchTokens("feature:checkout; priority:high, smoke"));
        Assertions.assertEquals(List.of("tag:smoke", "env:staging"), AuraFileService.parseSearchTokens("tag:smoke env:staging"));
    }

    @Test
    public void testGetFilteredYamlFilesListWithMultipleTags()
    {
        final DatasetDto ds1 = new DatasetDto("ds1", "Dataset 1", false, false, null, Map.of(
            "feature", List.of("checkout"),
            "priority", List.of("high")
        ));
        final DatasetDto ds2 = new DatasetDto("ds2", "Dataset 2", false, false, null, Map.of(
            "feature", List.of("login"),
            "priority", List.of("low")
        ));
        final YamlFileDto bothTagsFile = new YamlFileDto("checkout_both.yaml", List.of(ds1, ds2), false, null, false, Map.of());

        final DatasetDto ds3 = new DatasetDto("ds3", "Dataset 3", false, false, null, Map.of("priority", List.of("high")));
        final YamlFileDto inheritedTagsFile = new YamlFileDto("checkout_inherited.yaml", List.of(ds3), false, null, false, Map.of(
            "feature", List.of("checkout")
        ));

        final DatasetDto ds4 = new DatasetDto("ds4", "Dataset 4", false, false, null, Map.of("feature", List.of("checkout")));
        final DatasetDto ds5 = new DatasetDto("ds5", "Dataset 5", false, false, null, Map.of("priority", List.of("high")));
        final YamlFileDto disjointTagsFile = new YamlFileDto("disjoint.yaml", List.of(ds4, ds5), false, null, false, Map.of());

        final List<YamlFileDto> files = List.of(bothTagsFile, inheritedTagsFile, disjointTagsFile);

        // Single tag query
        final List<YamlFileDto> singleMatch = AuraFileService.filterYamlFiles(files, "feature:checkout");
        Assertions.assertEquals(3, singleMatch.size());

        // Multi-tag conjunction query: bothTagsFile and inheritedTagsFile should match; disjointTagsFile should NOT match
        final List<YamlFileDto> multiMatch = AuraFileService.filterYamlFiles(files, "feature:checkout, priority:high");
        Assertions.assertEquals(2, multiMatch.size());
        Assertions.assertTrue(multiMatch.stream().anyMatch(f -> "checkout_both.yaml".equals(f.file)));
        Assertions.assertTrue(multiMatch.stream().anyMatch(f -> "checkout_inherited.yaml".equals(f.file)));
        Assertions.assertFalse(multiMatch.stream().anyMatch(f -> "disjoint.yaml".equals(f.file)));
    }
}
