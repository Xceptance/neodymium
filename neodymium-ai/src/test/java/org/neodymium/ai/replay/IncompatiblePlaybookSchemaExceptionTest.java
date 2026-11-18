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
package org.neodymium.ai.replay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolRegistry;

/**
 * Unit tests for {@link IncompatiblePlaybookSchemaException} and schema version enforcement in {@link PlaybookToolReplayer}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class IncompatiblePlaybookSchemaExceptionTest
{
    @Test
    public void testDefaultMessageAndAccessors()
    {
        final IncompatiblePlaybookSchemaException ex = new IncompatiblePlaybookSchemaException("3.0", "4.0");

        Assertions.assertTrue(ex instanceof PipelineException);
        Assertions.assertEquals("3.0", ex.getRecordedVersion());
        Assertions.assertEquals("4.0", ex.getExpectedVersion());
        Assertions.assertTrue(ex.getMessage().contains("schema version '3.0' is obsolete or incompatible with current schema '4.0'"));
        Assertions.assertTrue(ex.getMessage().contains("FORCE_RECORDING"));
    }

    @Test
    public void testNullRecordedVersionHandledGracefully()
    {
        final IncompatiblePlaybookSchemaException ex = new IncompatiblePlaybookSchemaException(null, "4.0");

        Assertions.assertNull(ex.getRecordedVersion());
        Assertions.assertEquals("4.0", ex.getExpectedVersion());
        Assertions.assertTrue(ex.getMessage().contains("schema version 'null' is obsolete"));
    }

    @Test
    public void testCustomMessageConstructor()
    {
        final IncompatiblePlaybookSchemaException ex = new IncompatiblePlaybookSchemaException("2.0", "4.0", "Custom schema mismatch error");

        Assertions.assertEquals("2.0", ex.getRecordedVersion());
        Assertions.assertEquals("4.0", ex.getExpectedVersion());
        Assertions.assertEquals("Custom schema mismatch error", ex.getMessage());
    }

    @Test
    public void testReplayerThrowsIncompatiblePlaybookSchemaExceptionOnLegacyVersion()
    {
        final PlaybookStep step = new PlaybookStep("Click legacy element");
        step.setSchemaVersion("3.0");

        final ToolRegistry registry = new ToolRegistry();
        final SimpleToolContext context = new SimpleToolContext(registry);

        final IncompatiblePlaybookSchemaException ex = Assertions.assertThrows(
            IncompatiblePlaybookSchemaException.class,
            () -> PlaybookToolReplayer.replayStep(step, registry, context)
        );

        Assertions.assertEquals("3.0", ex.getRecordedVersion());
        Assertions.assertEquals(PlaybookStep.CURRENT_SCHEMA_VERSION, ex.getExpectedVersion());
    }

    @Test
    public void testReplayerThrowsIncompatiblePlaybookSchemaExceptionOnMissingVersion()
    {
        final PlaybookStep step = new PlaybookStep("Click element with null schema");
        step.setSchemaVersion(null);

        final ToolRegistry registry = new ToolRegistry();
        final SimpleToolContext context = new SimpleToolContext(registry);

        final IncompatiblePlaybookSchemaException ex = Assertions.assertThrows(
            IncompatiblePlaybookSchemaException.class,
            () -> PlaybookToolReplayer.replayStep(step, registry, context)
        );

        Assertions.assertNull(ex.getRecordedVersion());
        Assertions.assertEquals(PlaybookStep.CURRENT_SCHEMA_VERSION, ex.getExpectedVersion());
    }
}
