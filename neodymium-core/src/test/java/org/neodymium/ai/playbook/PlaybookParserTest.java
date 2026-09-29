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
package org.neodymium.ai.playbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * TDD validation tests for {@link YamlPlaybookParser}.
 * Covers simple step lists, data parameter parsing, nested includes, and cycle detection.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookParserTest
{
    /**
     * Verifies that the YamlPlaybookParser correctly parses a simple YAML playbook
     * with steps and datasets.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testParseSimpleYamlPlaybook() throws IOException
    {
        final PlaybookResourceManager manager = new InMemoryResourceManager();
        
        final String yaml = """
            steps: |
              Click login button
              Type username
            data:
              - username: testuser
                password: secretpassword
            """;
        
        manager.write("main.yaml", yaml);

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("main.yaml", manager);

        assertNotNull(playbook);
        
        // 1. Verify steps
        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(2, steps.size());
        assertEquals("Click login button", steps.get(0).getInstruction());
        assertEquals("Type username", steps.get(1).getInstruction());
        assertFalse(steps.get(0).isComposite());

        // 2. Verify dataset parameters
        final List<Map<String, SessionData.DataEntry>> datasets = playbook.getDataSets();
        assertEquals(1, datasets.size());
        
        final Map<String, SessionData.DataEntry> map = datasets.get(0);
        assertEquals("testuser", map.get("username").value());
        assertFalse(map.get("username").sensitive());
        assertEquals("secretpassword", map.get("password").value());
    }

    /**
     * Verifies that the YamlPlaybookParser resolves nested includes recursively.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testParseNestedIncludes() throws IOException
    {
        final PlaybookResourceManager manager = new InMemoryResourceManager();

        final String mainYaml = """
            steps: |
              include: common/setup.yaml
              Click continue
            """;

        final String setupYaml = """
            steps: |
              Open homepage
              include: login.yaml
            """;

        final String loginYaml = """
            steps: |
              Enter credentials
            """;

        manager.write("main.yaml", mainYaml);
        manager.write("common/setup.yaml", setupYaml);
        manager.write("common/login.yaml", loginYaml);

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("main.yaml", manager);

        assertNotNull(playbook);
        
        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(2, steps.size());

        // First step should be the include step: 'common/setup.yaml'
        final PlaybookStep setupInclude = steps.get(0);
        assertEquals("_include: common/setup.yaml", setupInclude.getInstruction());
        assertTrue(setupInclude.isComposite());
        assertEquals(2, setupInclude.getSubSteps().size());

        // Sub-steps of setup.yaml
        assertEquals("Open homepage", setupInclude.getSubSteps().get(0).getInstruction());
        
        final PlaybookStep loginInclude = setupInclude.getSubSteps().get(1);
        assertEquals("_include: login.yaml", loginInclude.getInstruction());
        assertTrue(loginInclude.isComposite());
        assertEquals(1, loginInclude.getSubSteps().size());
        assertEquals("Enter credentials", loginInclude.getSubSteps().get(0).getInstruction());

        // Second step of main.yaml
        assertEquals("Click continue", steps.get(1).getInstruction());
    }

    /**
     * Verifies that the YamlPlaybookParser detects cyclic inclusion loops
     * and throws an IOException describing the circular reference.
     *
     * @throws IOException if writing content fails
     */
    @Test
    public void testCircularIncludeDetection() throws IOException
    {
        final PlaybookResourceManager manager = new InMemoryResourceManager();

        final String aYaml = """
            steps: |
              include: b.yaml
            """;

        final String bYaml = """
            steps: |
              include: a.yaml
            """;

        manager.write("a.yaml", aYaml);
        manager.write("b.yaml", bYaml);

        final PlaybookParser parser = new YamlPlaybookParser();
        
        // Assert that parsing a.yaml results in an IOException due to circular dependency
        final Exception exception = assertThrows(IOException.class, () -> {
            parser.parse("a.yaml", manager);
        });

        assertTrue(exception.getMessage().contains("a.yaml"));
        assertTrue(exception.getMessage().contains("b.yaml"));
    }

    /**
     * Verifies that the YamlPlaybookParser parses a raw multi-line string
     * with plain instructions correctly via parseString.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testParseStringWithPlainInstructions() throws IOException
    {
        final String content = """
            Open homepage
            Search for product
            Add to cart
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(content);

        assertNotNull(playbook);
        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(3, steps.size());
        assertEquals("Open homepage", steps.get(0).getInstruction());
        assertEquals("Search for product", steps.get(1).getInstruction());
        assertEquals("Add to cart", steps.get(2).getInstruction());
        assertTrue(playbook.getDataSets().isEmpty());
    }

    /**
     * Verifies that YamlPlaybookParser correctly detects and parses structured YAML
     * multiline string content with steps and data sections.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testParseStringWithStructuredYaml() throws IOException
    {
        final String content = """
            steps: |
              Open homepage in browser
              Type '${searchTerm}' into input
            data:
              - searchTerm: "Minimalist"
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(content);

        assertNotNull(playbook);
        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(2, steps.size());
        assertEquals("Open homepage in browser", steps.get(0).getInstruction());
        assertEquals("Type '${searchTerm}' into input", steps.get(1).getInstruction());
        assertEquals(1, playbook.getDataSets().size());
        assertEquals("Minimalist", playbook.getDataSets().get(0).get("searchTerm").value());
    }

    /**
     * Verifies that single Map datasets and metadata sections are parsed correctly by YamlPlaybookParser.
     */
    @Test
    public void testParseSingleMapDataAndMetadata() throws IOException
    {
        final PlaybookResourceManager manager = new InMemoryResourceManager();
        final String yaml = """
            _meta:
              author: "AI QA Team"
              version: "1.2.0"
            data:
              user: "testuser"
              password: "secretpassword"
            steps:
              - "Login with user"
            """;

        manager.write("auth_playbook.yaml", yaml);

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("auth_playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());
        assertEquals(1, playbook.getDataSets().size());

        final Map<String, SessionData.DataEntry> dataset = playbook.getDataSets().get(0);
        assertEquals("testuser", dataset.get("user").value());
        assertEquals("secretpassword", dataset.get("password").value());
        assertEquals("auth_playbook.yaml", dataset.get("_meta.sourceFile").value());
        assertEquals("auth_playbook.yaml", dataset.get("_meta.classpathResourcePath").value());
        assertEquals("AI QA Team", dataset.get("_meta.author").value());
        assertEquals("1.2.0", dataset.get("_meta.version").value());
    }

    /**
     * Verifies that YamlPlaybookParser detects YAML when starting with data: or before: blocks.
     */
    @Test
    public void testParseStringStartingWithDataOrBefore() throws IOException
    {
        final String dataFirstYaml = """
            data:
              searchTerm: "Desk Lamp"
            steps:
              - "Search for ${searchTerm}"
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(dataFirstYaml);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());
        assertEquals(1, playbook.getDataSets().size());
        assertEquals("Desk Lamp", playbook.getDataSets().get(0).get("searchTerm").value());

        final String beforeYaml = """
            before:
              - "Open login page"
            steps:
              - "Submit form"
            """;
        final Playbook beforePlaybook = parser.parseString(beforeYaml);

        assertNotNull(beforePlaybook);
        assertEquals(2, beforePlaybook.getSteps().size());
        assertEquals("Open login page", beforePlaybook.getSteps().get(0).getInstruction());
        assertEquals("Submit form", beforePlaybook.getSteps().get(1).getInstruction());
    }

    /**
     * Verifies that parseString recognizes promptAddon blocks and parses prompt add-ons,
     * while plain instruction lines starting with 'addon:' are not treated as YAML blocks.
     */
    @Test
    public void testParseStringWithPromptAddon() throws IOException
    {
        final String promptAddonYaml = """
            promptAddon:
              general: "Always be concise."
            steps:
              - "Search for item"
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(promptAddonYaml);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());
        assertEquals("Search for item", playbook.getSteps().get(0).getInstruction());
        assertEquals(1, playbook.getPromptAddons().size());
        assertEquals("Always be concise.", playbook.getPromptAddons().get("general"));

        // Verify that 'addon:' is not recognized as a YAML block and treated as a plain step
        final String plainInstruction = """
            addon: check if something exists
            click button
            """;
        final Playbook plainPlaybook = parser.parseString(plainInstruction);
        assertNotNull(plainPlaybook);
        assertEquals(2, plainPlaybook.getSteps().size());
        assertEquals("addon: check if something exists", plainPlaybook.getSteps().get(0).getInstruction());
        assertEquals("click button", plainPlaybook.getSteps().get(1).getInstruction());
        assertTrue(plainPlaybook.getPromptAddons().isEmpty());
    }
}
