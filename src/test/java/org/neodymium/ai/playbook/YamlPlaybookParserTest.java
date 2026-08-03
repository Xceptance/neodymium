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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * Unit tests for {@link YamlPlaybookParser}.
 * Validates parsing of step hierarchies and datasets from YAML string content.
 *
 * @author AI-generated: Gemini 3.5 Flash
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
        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
    }
}
