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

import org.neodymium.ai.pipeline.PipelineException;

/**
 * Specialized failure exception thrown when a playbook recording schema version is missing,
 * obsolete, or incompatible with the current runtime schema version.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class IncompatiblePlaybookSchemaException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    private final String recordedVersion;
    private final String expectedVersion;

    /**
     * Constructs an IncompatiblePlaybookSchemaException with formatted default message.
     *
     * @param recordedVersion the schema version found in the recording
     * @param expectedVersion the schema version expected by the runtime
     */
    public IncompatiblePlaybookSchemaException(final String recordedVersion, final String expectedVersion)
    {
        this(
            recordedVersion,
            expectedVersion,
            String.format(
                "Playbook recording schema version '%s' is obsolete or incompatible with current schema '%s'. "
                    + "Legacy recording formats are no longer supported. Re-record the playbook using ExecutionMode.FORCE_RECORDING.",
                recordedVersion != null ? recordedVersion : "null",
                expectedVersion
            )
        );
    }

    /**
     * Constructs an IncompatiblePlaybookSchemaException with custom message.
     *
     * @param recordedVersion the schema version found in the recording
     * @param expectedVersion the schema version expected by the runtime
     * @param message the detail error message
     */
    public IncompatiblePlaybookSchemaException(
        final String recordedVersion,
        final String expectedVersion,
        final String message
    )
    {
        super(message);
        this.recordedVersion = recordedVersion;
        this.expectedVersion = expectedVersion;
    }

    /**
     * Gets the schema version found in the recording.
     *
     * @return the recorded schema version string, or null if missing
     */
    public String getRecordedVersion()
    {
        return this.recordedVersion;
    }

    /**
     * Gets the schema version expected by the runtime.
     *
     * @return the expected schema version string
     */
    public String getExpectedVersion()
    {
        return this.expectedVersion;
    }
}
