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

package org.neodymium.ai.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlaybookStep} visual and full-page instruction detection methods.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class PlaybookStepTest
{
    @Test
    public void testIsVisualStep()
    {
        final PlaybookStep standardStep = new PlaybookStep();
        standardStep.setInstruction("Click on the login button");
        Assertions.assertFalse(standardStep.isVisualStep());

        final PlaybookStep visualStep = new PlaybookStep();
        visualStep.setInstruction("Verify header logo position (visual)");
        Assertions.assertTrue(visualStep.isVisualStep());

        final PlaybookStep layoutStep = new PlaybookStep();
        layoutStep.setInstruction("Check sidebar layout (layout)");
        Assertions.assertTrue(layoutStep.isVisualStep());
    }

    @Test
    public void testIsFullPageVisualStep()
    {
        final PlaybookStep standardVisualStep = new PlaybookStep();
        standardVisualStep.setInstruction("Verify footer links (visual)");
        Assertions.assertTrue(standardVisualStep.isVisualStep());
        Assertions.assertFalse(standardVisualStep.isFullPageVisualStep());

        final PlaybookStep fullPageVisualStepHyphen = new PlaybookStep();
        fullPageVisualStepHyphen.setInstruction("Inspect footer copyright and legal notice (visual-full)");
        Assertions.assertTrue(fullPageVisualStepHyphen.isVisualStep());
        Assertions.assertTrue(fullPageVisualStepHyphen.isFullPageVisualStep());

        final PlaybookStep fullPageVisualStepUnderscore = new PlaybookStep();
        fullPageVisualStepUnderscore.setInstruction("Inspect full page overview (visual_full)");
        Assertions.assertTrue(fullPageVisualStepUnderscore.isVisualStep());
        Assertions.assertTrue(fullPageVisualStepUnderscore.isFullPageVisualStep());
    }

    @Test
    public void testDurationAndDelayFields() throws Exception
    {
        final PlaybookStep step = new PlaybookStep();
        step.setInstruction("Click submit");
        step.setDurationMs(350L);
        step.setDelayMs(700L);

        Assertions.assertEquals(350L, step.getDurationMs());
        Assertions.assertEquals(700L, step.getDelayMs());

        final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        final String json = mapper.writeValueAsString(step);
        Assertions.assertTrue(json.contains("\"durationMs\":350") || json.contains("\"durationMs\" : 350"));
        Assertions.assertTrue(json.contains("\"delayMs\":700") || json.contains("\"delayMs\" : 700"));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(350L, deserialized.getDurationMs());
        Assertions.assertEquals(700L, deserialized.getDelayMs());
    }
}
