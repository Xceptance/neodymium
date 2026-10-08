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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * Unit tests for {@link YamlPlaybookParser}.
 * Validates parsing of step hierarchies and datasets from YAML string content,
 * including strict format validation and empty playbook rejection.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class YamlPlaybookParserTest
{
    @Test
    public void testParseValidYamlPlaybook() throws IOException
    {
        final String yamlContent = """
            steps:
              - instruction: "Open demo store homepage"
                actions:
                  - type: "NAVIGATE"
                    target: "http://localhost:8080"
              - instruction: "Click login button"
                actions:
                  - type: "CLICK"
                    target: "#login-btn"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("test-playbook.yaml", manager);

        assertNotNull(playbook, "Parsed playbook should not be null.");
        assertEquals(2, playbook.getSteps().size(), "Should parse 2 top-level playbook steps.");
        assertEquals("Open demo store homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals(1, playbook.getSteps().get(0).getActions().size());
        assertEquals("NAVIGATE", playbook.getSteps().get(0).getActions().get(0).getType());
        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
        assertEquals(1, playbook.getSteps().get(1).getActions().size());
        assertEquals("CLICK", playbook.getSteps().get(1).getActions().get(0).getType());
    }

    @Test
    public void testParseStringSteps() throws IOException
    {
        final String yamlContent = """
            steps:
              - "Open homepage"
              - "Click button"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-string-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("test-string-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(2, playbook.getSteps().size());
        assertEquals("Open homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals("Click button", playbook.getSteps().get(1).getInstruction());
    }

    @Test
    public void testParseEmptyPlaybookThrowsException() throws Exception
    {
        final String yamlContent = """
            steps: []
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("empty-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("empty-playbook.yaml", manager);
        });

        assertEquals("Playbook cannot be empty: empty-playbook.yaml parsed to 0 executable steps.", ex.getMessage());
    }

    @Test
    public void testParseInvalidStepMapShapeThrowsException() throws Exception
    {
        final String yamlContent = """
            steps:
              - invalid_key1: "value 1"
                invalid_key2: "value 2"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("invalid-map-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("invalid-map-playbook.yaml", manager);
        });

        assertTrue(ex.getMessage().contains("Invalid playbook step format in file: invalid-map-playbook.yaml"));
    }

    @Test
    public void testParseEmptyJsonPlaybookThrowsException() throws Exception
    {
        final String jsonContent = "[]";

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("empty-recording.json", jsonContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("empty-recording.json", manager);
        });

        assertEquals("Playbook cannot be empty: empty-recording.json parsed to 0 executable steps.", ex.getMessage());
    }

    @Test
    public void testParsePlaybookWithBeforeAndSteps() throws IOException
    {
        final String yamlContent = """
            before: |
              Open https://www.example.com
              Select English
            steps: |
              Click button
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("before-steps.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("before-steps.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());
        assertEquals("Open https://www.example.com", playbook.getSteps().get(0).getInstruction());
        assertEquals("Select English", playbook.getSteps().get(1).getInstruction());
        assertEquals("Click button", playbook.getSteps().get(2).getInstruction());
    }

    @Test
    public void testParsePlaybookWithBeforeStepsAndAfter() throws IOException
    {
        final String yamlContent = """
            before: |
              Open https://www.example.com
              Accept cookies
            steps: |
              Search for product
              Add to cart
            after: |
              Clear cookies
              Close banner
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("lifecycle-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("lifecycle-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(6, playbook.getSteps().size());
        assertEquals("Open https://www.example.com", playbook.getSteps().get(0).getInstruction());
        assertEquals("Accept cookies", playbook.getSteps().get(1).getInstruction());
        assertEquals("Search for product", playbook.getSteps().get(2).getInstruction());
        assertEquals("Add to cart", playbook.getSteps().get(3).getInstruction());
        assertEquals("Clear cookies", playbook.getSteps().get(4).getInstruction());
        assertEquals("Close banner", playbook.getSteps().get(5).getInstruction());
    }

    @Test
    public void testParsePlaybookWithAllBeforeAliases() throws IOException
    {
        final String yamlContent = """
            before: |
              Step before
            beforeEach: |
              Step beforeEach
            _beforeEach: |
              Step _beforeEach
            _beforeAll: |
              Step _beforeAll
            steps: |
              Main step
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("before-aliases.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("before-aliases.yaml", manager);

        assertNotNull(playbook);
        assertEquals(5, playbook.getSteps().size());
        assertEquals("Step before", playbook.getSteps().get(0).getInstruction());
        assertEquals("Step beforeEach", playbook.getSteps().get(1).getInstruction());
        assertEquals("Step _beforeEach", playbook.getSteps().get(2).getInstruction());
        assertEquals("Step _beforeAll", playbook.getSteps().get(3).getInstruction());
        assertEquals("Main step", playbook.getSteps().get(4).getInstruction());
    }

    @Test
    public void testParsePlaybookWithAllAfterAliases() throws IOException
    {
        final String yamlContent = """
            steps: |
              Main step
            after: |
              Step after
            afterEach: |
              Step afterEach
            _afterEach: |
              Step _afterEach
            _afterAll: |
              Step _afterAll
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("after-aliases.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("after-aliases.yaml", manager);

        assertNotNull(playbook);
        assertEquals(5, playbook.getSteps().size());
        assertEquals("Main step", playbook.getSteps().get(0).getInstruction());
        assertEquals("Step after", playbook.getSteps().get(1).getInstruction());
        assertEquals("Step afterEach", playbook.getSteps().get(2).getInstruction());
        assertEquals("Step _afterEach", playbook.getSteps().get(3).getInstruction());
        assertEquals("Step _afterAll", playbook.getSteps().get(4).getInstruction());
    }

    @Test
    public void testParsePlaybookWithStructuredActionsInBeforeAndAfter() throws IOException
    {
        final String yamlContent = """
            before:
              - instruction: "Open home page"
                actions:
                  - type: "NAVIGATE"
                    target: "https://example.com"
            steps:
              - instruction: "Click login button"
                actions:
                  - type: "CLICK"
                    target: "#login-btn"
            after:
              - instruction: "Clear session"
                actions:
                  - type: "CLEAR_COOKIES"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("structured-lifecycle.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("structured-lifecycle.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());
        assertEquals("Open home page", playbook.getSteps().get(0).getInstruction());
        assertEquals(1, playbook.getSteps().get(0).getActions().size());
        assertEquals("NAVIGATE", playbook.getSteps().get(0).getActions().get(0).getType());

        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
        assertEquals(1, playbook.getSteps().get(1).getActions().size());
        assertEquals("CLICK", playbook.getSteps().get(1).getActions().get(0).getType());

        assertEquals("Clear session", playbook.getSteps().get(2).getInstruction());
        assertEquals(1, playbook.getSteps().get(2).getActions().size());
        assertEquals("CLEAR_COOKIES", playbook.getSteps().get(2).getActions().get(0).getType());
    }

    @Test
    public void testParsePlaybookWithIncludesInBeforeAndAfter() throws IOException
    {
        final String mainYaml = """
            before: |
              _include: setup.yaml
            steps: |
              Main step
            after: |
              _include: teardown.yaml
            """;

        final String setupYaml = """
            steps: |
              Setup step 1
              Setup step 2
            """;

        final String teardownYaml = """
            steps: |
              Teardown step 1
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("main.yaml", mainYaml);
        manager.write("setup.yaml", setupYaml);
        manager.write("teardown.yaml", teardownYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("main.yaml", manager);

        assertNotNull(playbook);
        assertEquals(4, playbook.getSteps().size(), "Should parse 4 inlined steps across before, steps, and after blocks");

        assertEquals("Setup step 1", playbook.getSteps().get(0).getInstruction());
        assertEquals("setup.yaml", playbook.getSteps().get(0).getSourceFile());

        assertEquals("Setup step 2", playbook.getSteps().get(1).getInstruction());
        assertEquals("setup.yaml", playbook.getSteps().get(1).getSourceFile());

        assertEquals("Main step", playbook.getSteps().get(2).getInstruction());
        assertEquals("main.yaml", playbook.getSteps().get(2).getSourceFile());

        assertEquals("Teardown step 1", playbook.getSteps().get(3).getInstruction());
        assertEquals("teardown.yaml", playbook.getSteps().get(3).getSourceFile());
    }

    @Test
    public void testParseYamlAnchorList() throws IOException
    {
        final String yamlContent = """
            search: &search
              - "Enter query into search box"
              - "Click submit search"
              - "Verify results page is shown"

            steps:
              - "Open homepage"
              - *search
              - "Open another page"
              - *search
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("anchor-list-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("anchor-list-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(8, playbook.getSteps().size(), "Should expand nested list anchors into sequential steps");
        assertEquals("Open homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals("Enter query into search box", playbook.getSteps().get(1).getInstruction());
        assertEquals("Click submit search", playbook.getSteps().get(2).getInstruction());
        assertEquals("Verify results page is shown", playbook.getSteps().get(3).getInstruction());
        assertEquals("Open another page", playbook.getSteps().get(4).getInstruction());
        assertEquals("Enter query into search box", playbook.getSteps().get(5).getInstruction());
        assertEquals("Click submit search", playbook.getSteps().get(6).getInstruction());
        assertEquals("Verify results page is shown", playbook.getSteps().get(7).getInstruction());
    }

    @Test
    public void testParseYamlAnchorBlockScalar() throws IOException
    {
        final String yamlContent = """
            search: &search |
              Enter query into search box
              Click submit search

            steps:
              - "Open homepage"
              - *search
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("anchor-block-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("anchor-block-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size(), "Should split multiline block scalar in list into individual steps");
        assertEquals("Open homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals("Enter query into search box", playbook.getSteps().get(1).getInstruction());
        assertEquals("Click submit search", playbook.getSteps().get(2).getInstruction());
    }

    /**
     * Anchor/alias reuse across three site variants with data sets. This mirrors the structure of
     * {@code verla/SearchTest_German.yaml} in the e2e module, but is kept inline because the core module
     * must not depend on resources that live in {@code neodymium-e2e-tests}.
     */
    @Test
    public void testParseSearchTestGermanYaml() throws IOException
    {
        final String yamlContent = """
            search: &search
              - Gib "${searchQuery}" in das Suchfeld ein.
              - Ein Suchvorschlag-Dropdown ist sichtbar und enthält mindestens 6 Einträge.
              - Klicke auf "View All", um alle Suchergebnisse zu sehen.
              - Eine Suchergebnisseite wird angezeigt und zeigt die Suchergebnisse für "${searchQuery}".
              - Ein Feld für die Anzahl der Suchergebnisse wird angezeigt.
              - Die Anzahl der Ergebnisse '${resultCount}' ist zu sehen.
              - Auf der linken Seite wird eine Box mit Kategorien, Farben, Preis und Angebot angezeigt (visual-full).

            steps:
              - Öffne ${verla.url}/verla-perfect/index.html
              - Öffne das Länder-Dropdown.
              - Klicke "${country}".
              - Die gezeigte Landesflagge ist die Flagge von "${country}" (visual).
              - *search

              - Öffne ${verla.url}/verla-normal/index.html
              - *search

              - Öffne ${verla.url}/verla-bad/index.html
              - *search

            data:
              - testId: "US"
                country: "United States"
                searchQuery: "minimalist"
                resultCount: 33
              - testId: "DE"
                country: "Germany"
                searchQuery: "sandfarben"
                resultCount: 41
              - testId: "FIN"
                country: "Finland"
                searchQuery: "moderni"
                resultCount: 35
            """;
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("SearchTest_German.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("SearchTest_German.yaml", manager);

        assertNotNull(playbook);
        // 4 initial steps + 7 search steps + 1 open step + 7 search steps + 1 open step + 7 search steps = 27 steps
        assertEquals(27, playbook.getSteps().size());
        assertEquals(3, playbook.getDataSets().size());
        assertEquals("US", playbook.getDataSets().get(0).get("testId").value());
    }

    @Test
    public void testParseGroupedStepsMultilineBlock() throws IOException
    {
        final String yamlContent = """
            steps: |
              1. Open demo store homepage
              Add first product to cart:
                - Locate the first product card and hover over it
                - Click its 'Add to Cart' button
                - When this string '${testId}' is not equal 'bad', click the size 'S'
              Verify cart count is '1'
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("grouped-multiline.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("grouped-multiline.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size(), "Should parse 3 top-level steps");
        assertEquals("1. Open demo store homepage", playbook.getSteps().get(0).getInstruction());
        assertFalse(playbook.getSteps().get(0).hasSubSteps());

        final PlaybookStep groupStep = playbook.getSteps().get(1);
        assertEquals("Add first product to cart", groupStep.getInstruction(), "Trailing colon should be stripped from goal header");
        assertTrue(groupStep.hasSubSteps(), "Group step must have sub-steps");
        assertEquals(3, groupStep.getSubSteps().size(), "Should have 3 child milestones");
        assertEquals("Locate the first product card and hover over it", groupStep.getSubSteps().get(0).getInstruction());
        assertEquals("Click its 'Add to Cart' button", groupStep.getSubSteps().get(1).getInstruction());
        assertEquals("When this string '${testId}' is not equal 'bad', click the size 'S'", groupStep.getSubSteps().get(2).getInstruction());
        assertEquals(groupStep, groupStep.getSubSteps().get(0).getParent(), "Child step parent reference must point to group step");

        assertEquals("Verify cart count is '1'", playbook.getSteps().get(2).getInstruction());
        assertFalse(playbook.getSteps().get(2).hasSubSteps());
    }

    @Test
    public void testParseGroupedStepsStructuredYaml() throws IOException
    {
        final String yamlContent = """
            steps:
              - "Open demo store homepage"
              - Add first product to cart:
                  - "Locate the first product card and hover over it"
                  - "Click its 'Add to Cart' button"
              - "Verify cart count is '1'"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("grouped-structured.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("grouped-structured.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());
        assertEquals("Open demo store homepage", playbook.getSteps().get(0).getInstruction());

        final PlaybookStep groupStep = playbook.getSteps().get(1);
        assertEquals("Add first product to cart", groupStep.getInstruction());
        assertTrue(groupStep.hasSubSteps());
        assertEquals(2, groupStep.getSubSteps().size());
        assertEquals("Locate the first product card and hover over it", groupStep.getSubSteps().get(0).getInstruction());
        assertEquals("Click its 'Add to Cart' button", groupStep.getSubSteps().get(1).getInstruction());
        assertEquals(groupStep, groupStep.getSubSteps().get(0).getParent());

        assertEquals("Verify cart count is '1'", playbook.getSteps().get(2).getInstruction());
    }

    @Test
    public void testParseGroupedStepsWithInstructionAndSubSteps() throws IOException
    {
        final String yamlContent = """
            steps:
              - instruction: "Select product variant and purchase"
                subSteps:
                  - "Select size M"
                  - "Select color Blue"
                  - "Click Add to Cart"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("instruction-substeps.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("instruction-substeps.yaml", manager);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());

        final PlaybookStep groupStep = playbook.getSteps().get(0);
        assertEquals("Select product variant and purchase", groupStep.getInstruction());
        assertTrue(groupStep.hasSubSteps());
        assertEquals(3, groupStep.getSubSteps().size());
        assertEquals("Select size M", groupStep.getSubSteps().get(0).getInstruction());
        assertEquals("Select color Blue", groupStep.getSubSteps().get(1).getInstruction());
        assertEquals("Click Add to Cart", groupStep.getSubSteps().get(2).getInstruction());
    }

    @Test
    public void testDuplicateStepInstructionsResolveDistinctLineNumbers() throws IOException
    {
        final String yamlContent = """
            steps: |
              Locate the promo code input field:
                - and type '10p-off' into it.
                - Submit the form.

              Locate the promo code input field:
                - clear its content
                - type 'FREEGIFT' into it
                - Assert promo line item is shown (bug).
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("duplicate-steps.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("duplicate-steps.yaml", manager);

        assertNotNull(playbook);
        assertEquals(2, playbook.getSteps().size());

        final PlaybookStep firstOccurrence = playbook.getSteps().get(0);
        final PlaybookStep secondOccurrence = playbook.getSteps().get(1);

        assertEquals("Locate the promo code input field", firstOccurrence.getInstruction());
        assertEquals("Locate the promo code input field", secondOccurrence.getInstruction());

        assertEquals(2, firstOccurrence.getLineNumber());
        assertEquals(6, secondOccurrence.getLineNumber());

        assertTrue(secondOccurrence.isBug());
        assertFalse(firstOccurrence.isBug());
    }

    @Test
    public void testLegacyJsonRecordingNormalizationWithParentBugFlag() throws Exception
    {
        final String legacyJson = """
            [
              {
                "instruction": "Locate the promo code input field",
                "bug": true,
                "subSteps": [
                  {
                    "instruction": "clear its content",
                    "bug": false
                  },
                  {
                    "instruction": "Assert promo line item is shown",
                    "bug": true
                  }
                ]
              }
            ]
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("recording.json", legacyJson);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("recording.json", manager);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());

        final PlaybookStep parent = playbook.getSteps().get(0);
        assertTrue(parent.isBug(), "Parent should still evaluate to true when queried dynamically");

        final PlaybookStep sub0 = parent.getSubSteps().get(0);
        assertFalse(sub0.isBug(), "sub0 must be normalized to false and not inherit bug from parent");

        final PlaybookStep sub1 = parent.getSubSteps().get(1);
        assertTrue(sub1.isBug(), "sub1 must preserve its own bug flag");
    }

    @Test
    public void testParsePlaybookWithRootProperties() throws IOException
    {
        final String yamlContent = """
            _properties:
              neodymium.ai.tokenBudget.input: 1500000
              neodymium:
                ai:
                  tokenBudget:
                    output: 75000
              skipReplay: true

            steps:
              - "Open homepage"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("properties-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("properties-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(1, playbook.getSteps().size());
        assertEquals(1, playbook.getDataSets().size(), "Should generate a default dataset when _properties is present without data");

        final Map<String, SessionData.DataEntry> dataset = playbook.getDataSets().get(0);
        assertEquals("1500000", dataset.get("neodymium.ai.tokenBudget.input").value());
        assertEquals("75000", dataset.get("neodymium.ai.tokenBudget.output").value());
        assertEquals("true", dataset.get("neodymium.ai.skipReplay").value());
    }

    @Test
    public void testParsePlaybookWithPropertiesAndDataRows() throws IOException
    {
        final String yamlContent = """
            _properties:
              neodymium.ai.tokenBudget.input: 1000000

            steps:
              - "Open homepage"

            data:
              - testId: "row1"
                searchTerm: "Vitamin"
              - testId: "row2"
                searchTerm: "Moisturizer"
                _properties:
                  neodymium.ai.tokenBudget.input: 2500000
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("properties-data-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("properties-data-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(2, playbook.getDataSets().size());

        final Map<String, SessionData.DataEntry> row1 = playbook.getDataSets().get(0);
        assertEquals("row1", row1.get("testId").value());
        assertEquals("Vitamin", row1.get("searchTerm").value());
        assertEquals("1000000", row1.get("neodymium.ai.tokenBudget.input").value(), "Row 1 should inherit root _properties");

        final Map<String, SessionData.DataEntry> row2 = playbook.getDataSets().get(1);
        assertEquals("row2", row2.get("testId").value());
        assertEquals("Moisturizer", row2.get("searchTerm").value());
        assertEquals("2500000", row2.get("neodymium.ai.tokenBudget.input").value(), "Row 2 should override root _properties");
    }

    @Test
    public void testParsePlaybookWithPropertiesKeywordAndUnderscoreData() throws IOException
    {
        final String yamlContent = """
            properties:
              tokenBudget.input: 1200000

            steps:
              - "Open homepage"

            _data:
              - testId: "rowA"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("properties-alt-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("properties-alt-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(1, playbook.getDataSets().size());

        final Map<String, SessionData.DataEntry> row = playbook.getDataSets().get(0);
        assertEquals("rowA", row.get("testId").value());
        assertEquals("1200000", row.get("tokenBudget.input").value());
    }

    /**
     * Verifies that steps resolved from a statically-parsed {@code _include:} directive are inlined
     * directly into the top-level playbook steps list without synthetic container wrapper nodes,
     * retaining their original source file name and line number, and having no parent.
     */
    @Test
    public void testStaticIncludeStepsAreInlinedWithOriginalSourceLocation() throws IOException
    {
        final String hostYaml = """
            steps:
              - "Navigate to homepage"
              - include: shared-login.yaml
              - "Verify dashboard"
            """;

        final String includedYaml = """
            steps:
              - "Enter username"
              - "Enter password"
              - "Click login"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("host.yaml", hostYaml);
        manager.write("shared-login.yaml", includedYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("host.yaml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();

        // We expect 5 steps: "Navigate to homepage", 3 login steps, "Verify dashboard"
        assertEquals(5, steps.size(), "Parsed step count should equal inlined steps without include wrappers");

        // Step 0: "Navigate to homepage" — native host step
        assertEquals("host.yaml", steps.get(0).getSourceFile());
        assertNull(steps.get(0).getParent());

        // Steps 1..3: Included login steps from shared-login.yaml
        assertEquals("Enter username", steps.get(1).getInstruction());
        assertEquals("shared-login.yaml", steps.get(1).getSourceFile());
        assertNull(steps.get(1).getParent());
        assertFalse(steps.get(1).hasSubSteps());

        assertEquals("Enter password", steps.get(2).getInstruction());
        assertEquals("shared-login.yaml", steps.get(2).getSourceFile());
        assertNull(steps.get(2).getParent());
        assertFalse(steps.get(2).hasSubSteps());

        assertEquals("Click login", steps.get(3).getInstruction());
        assertEquals("shared-login.yaml", steps.get(3).getSourceFile());
        assertNull(steps.get(3).getParent());
        assertFalse(steps.get(3).hasSubSteps());

        // Step 4: "Verify dashboard" — native host step
        assertEquals("Verify dashboard", steps.get(4).getInstruction());
        assertEquals("host.yaml", steps.get(4).getSourceFile());
        assertNull(steps.get(4).getParent());
    }

    /**
     * Verifies that nested static includes (include inside include) inline recursively all the way up
     * to the top level, with each step preserving its origin file and line number.
     */
    @Test
    public void testNestedStaticIncludeStepsAreInlinedRecursively() throws IOException
    {
        final String hostYaml = """
            steps:
              - "Setup"
              - include: middle.yaml
            """;

        final String middleYaml = """
            steps:
              - "Middle step"
              - include: leaf.yaml
            """;

        final String leafYaml = """
            steps:
              - "Leaf step A"
              - "Leaf step B"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("host.yaml", hostYaml);
        manager.write("middle.yaml", middleYaml);
        manager.write("leaf.yaml", leafYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("host.yaml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();

        // "Setup" (host.yaml), "Middle step" (middle.yaml), "Leaf step A" (leaf.yaml), "Leaf step B" (leaf.yaml)
        assertEquals(4, steps.size(), "Nested static includes should expand to 4 top-level inlined steps");

        assertEquals("host.yaml", steps.get(0).getSourceFile());
        assertEquals("middle.yaml", steps.get(1).getSourceFile());
        assertEquals("leaf.yaml", steps.get(2).getSourceFile());
        assertEquals("leaf.yaml", steps.get(3).getSourceFile());

        for (final PlaybookStep step : steps)
        {
            assertNull(step.getParent());
            assertFalse(step.hasSubSteps());
        }
    }

    @Test
    public void testParseIncludedStepListFragment() throws IOException
    {
        final String hostYaml = """
            steps: |
              Navigate to homepage
              Accept cookies
              _include: checkout.steps
            """;

        final String checkoutSteps = """
            - Select payment method
            - Enter payment credentials
            - Confirm order
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("stokkeTest.yml", hostYaml);
        manager.write("checkout.steps", checkoutSteps);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("stokkeTest.yml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(5, steps.size(), "Should expand included list fragment steps into 5 total steps");
        assertEquals("Navigate to homepage", steps.get(0).getInstruction());
        assertEquals("Accept cookies", steps.get(1).getInstruction());
        assertEquals("Select payment method", steps.get(2).getInstruction());
        assertEquals("Enter payment credentials", steps.get(3).getInstruction());
        assertEquals("Confirm order", steps.get(4).getInstruction());
    }

    @Test
    public void testParseYamlListStepWithInlineColonAndInclude() throws IOException
    {
        final String hostYaml = """
            steps:
              - Start checkout.
              - If checkout page is not loaded, click retry and _include: fragments/restart.steps
              - Validate checkout page is loaded.
            """;

        final String restartSteps = """
            - Click start checkout again
            - Save error message
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("host.yml", hostYaml);
        manager.write("fragments/restart.steps", restartSteps);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("host.yml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(3, steps.size());
        assertEquals("Start checkout.", steps.get(0).getInstruction());
        assertEquals("If checkout page is not loaded, click retry and _include: fragments/restart.steps", steps.get(1).getInstruction());
        assertEquals("Validate checkout page is loaded.", steps.get(2).getInstruction());
    }

    @Test
    public void testParseYamlListStepWithParentheticalHintColon() throws IOException
    {
        final String yamlContent = """
            steps:
              - Generate random email address (hint: use java method)
              - Enter email ${randomEmail} address and continue.
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("hint-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("hint-playbook.yaml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(2, steps.size());
        assertEquals("Generate random email address (hint: use java method)", steps.get(0).getInstruction());
        assertEquals("Enter email ${randomEmail} address and continue.", steps.get(1).getInstruction());
    }

    @Test
    public void testParseMultilineStringWithBulletedInclude() throws IOException
    {
        final String hostYaml = """
            steps: |
              - _include: fragments/open-pdp.yaml
              - Verify the product name headline is visible.
            """;

        final String fragmentYaml = """
            steps:
              - Open homepage in the browser
              - Click on the first product card.
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("host.yaml", hostYaml);
        manager.write("fragments/open-pdp.yaml", fragmentYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("host.yaml", manager);

        final List<PlaybookStep> steps = playbook.getSteps();
        assertEquals(3, steps.size(), "Should expand bulleted include inside multiline string scalar into 3 inlined steps");
        assertEquals("Open homepage in the browser", steps.get(0).getInstruction());
        assertEquals("Click on the first product card.", steps.get(1).getInstruction());
        assertEquals("Verify the product name headline is visible.", steps.get(2).getInstruction());
    }

    @Test
    public void testUnresolvedIncludeDirectiveThrowsException() throws IOException
    {
        final String badIncludeYaml = """
            steps:
              - "_include: fragments/missing-file.yaml"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("bad-include.yaml", badIncludeYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Exception ex = assertThrows(Exception.class, () -> {
            parser.parse("bad-include.yaml", manager);
        });

        assertNotNull(ex.getMessage());
    }

    @Test
    public void testJsonRecordedStepsStatusResetToPendingOnParse() throws IOException
    {
        final String jsonContent = """
            [
              {
                "instruction": "Step 1: Open page",
                "status": "SUCCESS",
                "durationMs": 5432,
                "startTimeMs": 1700000000000
              },
              {
                "instruction": "Step 2: Click button",
                "status": "HEALED",
                "durationMs": 1234,
                "startTimeMs": 1700000005432
              }
            ]
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("recorded.json", jsonContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("recorded.json", manager);

        assertNotNull(playbook);
        assertEquals(2, playbook.getSteps().size());

        final PlaybookStep s1 = playbook.getSteps().get(0);
        assertEquals(PlaybookStepStatus.PENDING, s1.getStatus(), "Step 1 status must be reset to PENDING");
        assertNull(s1.getDurationMs(), "Step 1 duration must be reset to null");
        assertNull(s1.getStartTimeMs(), "Step 1 startTimeMs must be reset to null");

        final PlaybookStep s2 = playbook.getSteps().get(1);
        assertEquals(PlaybookStepStatus.PENDING, s2.getStatus(), "Step 2 status must be reset to PENDING");
        assertNull(s2.getDurationMs(), "Step 2 duration must be reset to null");
        assertNull(s2.getStartTimeMs(), "Step 2 startTimeMs must be reset to null");
    }
}
