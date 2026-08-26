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
package org.neodymium.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying serialization, deserialization, and evaluation of fullPage on {@link PlaybookStep}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaybookStepFullPagePersistenceTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Verify PlaybookStep serializes and deserializes fullPage property")
    public void testPlaybookStepSerializationWithFullPage() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Verify registration success (visual)");
        step.setFullPage(true);
        step.setScreenshotHash("a1b2c3d4e5f6");

        final String json = MAPPER.writeValueAsString(step);
        Assertions.assertTrue(json.contains("\"fullPage\":true") || json.contains("\"fullPage\" : true"), "JSON should include fullPage property");

        final PlaybookStep deserialized = MAPPER.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(Boolean.TRUE, deserialized.isFullPage());
        Assertions.assertTrue(deserialized.isFullPageVisualStep(), "Step with fullPage=true must evaluate isFullPageVisualStep() as true");
    }

    @Test
    @DisplayName("Verify PlaybookStep backwards compatibility when fullPage is missing")
    public void testPlaybookStepDeserializationWithoutFullPage() throws Exception
    {
        final String jsonWithoutFullPage = "{\"instruction\":\"Verify dashboard (visual)\",\"screenshotHash\":\"hash123\"}";
        final PlaybookStep deserialized = MAPPER.readValue(jsonWithoutFullPage, PlaybookStep.class);
        Assertions.assertNull(deserialized.isFullPage());
        Assertions.assertFalse(deserialized.isFullPageVisualStep(), "(visual) without fullPage flag must be false");

        final String jsonWithFullTag = "{\"instruction\":\"Verify dashboard (visual: full)\",\"screenshotHash\":\"hash123\"}";
        final PlaybookStep deserializedFull = MAPPER.readValue(jsonWithFullTag, PlaybookStep.class);
        Assertions.assertNull(deserializedFull.isFullPage());
        Assertions.assertTrue(deserializedFull.isFullPageVisualStep(), "(visual: full) must be true even if fullPage flag is null");
    }

    @Test
    @DisplayName("Verify isFullPageVisualStep returns true when fullPage is true regardless of instruction tag")
    public void testIsFullPageVisualStepWithFlag()
    {
        final PlaybookStep step = new PlaybookStep("Check order summary");
        Assertions.assertFalse(step.isFullPageVisualStep());

        step.setFullPage(true);
        Assertions.assertTrue(step.isFullPageVisualStep());

        step.setFullPage(false);
        Assertions.assertFalse(step.isFullPageVisualStep());
    }
}
