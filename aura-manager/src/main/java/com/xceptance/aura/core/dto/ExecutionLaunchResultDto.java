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
package com.xceptance.aura.core.dto;

/**
 * Standardized result DTO for dispatching a test execution run.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecutionLaunchResultDto
{
    private final String runId;
    private final String status;
    private final int queuePosition;
    private final String message;

    public ExecutionLaunchResultDto(
        final String runId,
        final String status,
        final int queuePosition,
        final String message)
    {
        this.runId = runId;
        this.status = status;
        this.queuePosition = queuePosition;
        this.message = message;
    }

    public static ExecutionLaunchResultDto error(final String message)
    {
        return new ExecutionLaunchResultDto(null, "ERROR", 0, message);
    }

    public String getRunId()
    {
        return runId;
    }

    public String getStatus()
    {
        return status;
    }

    public int getQueuePosition()
    {
        return queuePosition;
    }

    public String getMessage()
    {
        return message;
    }
}
