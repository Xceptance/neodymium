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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import com.xceptance.neodymium.aura.AuraFileService.IssueSeverity;
import com.xceptance.neodymium.aura.AuraFileService.ValidationIssue;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
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
    public final void testParsePlaybookSectionsWithPropertiesAndMeta()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            _properties:
              neodymium.ai.tokenBudget.input: 1500000
              skipReplay: true

            _meta:
              author: "QA Team"
              tags:
                - smoke
                - checkout

            description: "Smoke test verifying guest checkout"

            promptAddon:
              visual: "Do not dismiss the modal before asserting banner"

            steps: |
              Open https://verla.xceptance.com
              Click checkout button
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);
        Assertions.assertEquals(Boolean.FALSE, sections.get("hasError"));

        final String propertiesYaml = (String) sections.get("propertiesYaml");
        Assertions.assertNotNull(propertiesYaml, "Must extract propertiesYaml");
        Assertions.assertTrue(propertiesYaml.contains("neodymium.ai.tokenBudget.input"), "Should contain tokenBudget property");
        Assertions.assertTrue(propertiesYaml.contains("skipReplay"), "Should contain skipReplay property");
        Assertions.assertFalse(propertiesYaml.startsWith("{"), "propertiesYaml must use clean block style, not flow JSON: " + propertiesYaml);

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> propertiesList = (List<Map<String, String>>) sections.get("propertiesList");
        Assertions.assertNotNull(propertiesList, "Must extract propertiesList");
        Assertions.assertEquals(2, propertiesList.size());
        Assertions.assertEquals("neodymium.ai.tokenBudget.input", propertiesList.get(0).get("key"));
        Assertions.assertEquals("1500000", propertiesList.get(0).get("value"));
        Assertions.assertEquals("skipReplay", propertiesList.get(1).get("key"));
        Assertions.assertEquals("true", propertiesList.get(1).get("value"));

        final String metaYaml = (String) sections.get("metaYaml");
        Assertions.assertNotNull(metaYaml, "Must extract metaYaml");
        Assertions.assertTrue(metaYaml.contains("author"), "Should contain author");
        Assertions.assertTrue(metaYaml.contains("QA Team"), "Should contain QA Team");

        Assertions.assertEquals("Smoke test verifying guest checkout", sections.get("description"));

        final String promptAddonYaml = (String) sections.get("promptAddonYaml");
        Assertions.assertNotNull(promptAddonYaml, "Must extract promptAddonYaml");
        Assertions.assertTrue(promptAddonYaml.contains("visual"), "Should contain visual promptAddon");
    }

    @Test
    public final void testParsePlaybookSectionsWithRootVariablesAndLifecycleBlocks()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            baseUrl: "https://verla.xceptance.com"
            browser: "Chrome_1920x1080"

            beforeAll: |
              Set up test database

            beforeEach: |
              Clear browser cookies

            steps: |
              Open ${baseUrl}
              Click search

            afterEach: |
              Log out user

            afterAll: |
              Tear down test database
            """;

        final Map<String, Object> sections = fileService.parsePlaybookSections(content);
        Assertions.assertNotNull(sections);
        Assertions.assertEquals(Boolean.FALSE, sections.get("hasError"));

        @SuppressWarnings("unchecked")
        final List<String> beforeSteps = (List<String>) sections.get("beforeSteps");
        Assertions.assertNotNull(beforeSteps);
        Assertions.assertTrue(beforeSteps.contains("Clear browser cookies"), "beforeSteps should contain beforeEach step");

        @SuppressWarnings("unchecked")
        final List<String> afterSteps = (List<String>) sections.get("afterSteps");
        Assertions.assertNotNull(afterSteps);
        Assertions.assertTrue(afterSteps.contains("Log out user"), "afterSteps should contain afterEach step");

        @SuppressWarnings("unchecked")
        final Map<String, Object> rootVariables = (Map<String, Object>) sections.get("rootVariables");
        Assertions.assertNotNull(rootVariables, "Must extract rootVariables");
        Assertions.assertEquals("https://verla.xceptance.com", rootVariables.get("baseUrl"));
        Assertions.assertEquals("Chrome_1920x1080", rootVariables.get("browser"));
    }

    @Test
    public final void testValidateFileContentRecognizesRootVariablesAndProperties()
    {
        final AuraFileService fileService = new AuraFileService();
        final String content = """
            baseUrl: "https://verla.xceptance.com"
            _properties:
              neodymium.ai.tokenBudget.input: 1000000

            steps: |
              Open ${baseUrl}
              Observe consistency (visual)
            """;

        final List<ValidationIssue> issues = fileService.validateFileContent("test.yaml", content);
        Assertions.assertNotNull(issues);
        final boolean hasUndefinedVarError = issues.stream().anyMatch(issue ->
                issue.severity() == IssueSeverity.ERROR && issue.message().contains("Undefined variable"));
        Assertions.assertFalse(hasUndefinedVarError, "baseUrl defined at root level should not be flagged as undefined variable");
    }

    @Test
    public final void testFilteredYamlFilesListWithListArg()
    {
        final AuraFileService fileService = new AuraFileService();
        final YamlFileDto file1 = new YamlFileDto("tests/alpha/Login.yaml", List.of());
        final YamlFileDto file2 = new YamlFileDto("tests/beta/Checkout.yaml", List.of());

        final List<YamlFileDto> list = List.of(file1, file2);

        final List<YamlFileDto> filtered = fileService.getFilteredYamlFilesList(list, "login");
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("tests/alpha/Login.yaml", filtered.get(0).file);

        final List<YamlFileDto> allWhenEmpty = fileService.getFilteredYamlFilesList(list, "");
        Assertions.assertEquals(2, allWhenEmpty.size());
    }

    @Test
    public final void testYamlFilesListCachingAndInvalidation() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String fileName = "test_caching_unit.yaml";

        try
        {
            fileService.createYamlFile(fileName);
            fileService.saveYamlFileContent(fileName, "steps:\n  - Step One");

            final List<YamlFileDto> firstScan = fileService.getYamlFilesList();
            final YamlFileDto firstEntry = firstScan.stream()
                    .filter(f -> fileName.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(firstEntry, "First scan should contain created file");

            final List<YamlFileDto> secondScan = fileService.getYamlFilesList();
            final YamlFileDto secondEntry = secondScan.stream()
                    .filter(f -> fileName.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(secondEntry, "Second scan should contain created file");
            Assertions.assertSame(firstEntry, secondEntry, "Second scan should return the cached YamlFileDto instance");

            fileService.saveYamlFileContent(fileName, "steps:\n  - Step Two");
            final List<YamlFileDto> thirdScan = fileService.getYamlFilesList();
            final YamlFileDto thirdEntry = thirdScan.stream()
                    .filter(f -> fileName.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(thirdEntry);
            Assertions.assertNotSame(firstEntry, thirdEntry, "Third scan should return a freshly parsed instance after save");
        }
        finally
        {
            fileService.deleteYamlFile(fileName);
        }
    }

    @Test
    public final void testUpdateFragmentReferencesInvalidatesCache() throws IOException
    {
        final AuraFileService fileService = new AuraFileService();
        final String testFile = "test_fragment_cache_invalidation.yaml";
        final String fragmentOld = "fragments/old.steps";
        final String fragmentNew = "fragments/new.steps";

        try
        {
            fileService.createYamlFile(testFile);
            fileService.saveYamlFileContent(testFile, "steps:\n  - _include: " + fragmentOld);

            final List<YamlFileDto> firstScan = fileService.getYamlFilesList();
            final YamlFileDto firstEntry = firstScan.stream()
                    .filter(f -> testFile.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(firstEntry);

            final List<YamlFileDto> secondScan = fileService.getYamlFilesList();
            final YamlFileDto secondEntry = secondScan.stream()
                    .filter(f -> testFile.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertSame(firstEntry, secondEntry);

            fileService.updateFragmentReferences(fragmentOld, fragmentNew);

            final List<YamlFileDto> thirdScan = fileService.getYamlFilesList();
            final YamlFileDto thirdEntry = thirdScan.stream()
                    .filter(f -> testFile.equals(f.file))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(thirdEntry);
            Assertions.assertNotSame(firstEntry, thirdEntry, "Cache should be invalidated after updating fragment references");
        }
        finally
        {
            fileService.deleteYamlFile(testFile);
        }
    }
}
