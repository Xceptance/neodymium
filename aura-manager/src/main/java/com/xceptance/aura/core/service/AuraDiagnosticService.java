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
package com.xceptance.aura.core.service;

import com.xceptance.aura.core.dto.FailurePageResultDto;
import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;

/**
 * Service managing execution diagnostics, status resolution, universal metrics,
 * cursor-based failure pagination, and single execution drilldown.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public interface AuraDiagnosticService
{
    /**
     * Retrieves the status, universal metrics envelope, and adaptive failures for a run.
     *
     * @param runId run identifier
     * @return status response DTO
     */
    RunStatusResponseDto getRunStatus(String runId);

    /**
     * Retrieves cursor-paginated distilled failures across an entire test run.
     *
     * @param runId run identifier
     * @param category optional failure category filter
     * @param cursor optional offset cursor (starting index)
     * @param limit maximum items to return (capped at 10, default 5)
     * @return cursor-paginated failure page result
     */
    FailurePageResultDto<DistilledFailureDto> getRunFailures(String runId, String category, String cursor, Integer limit);

    /**
     * Retrieves detailed failure information for a specific individual test within a run.
     *
     * @param runId run identifier
     * @param testName test name or title
     * @return distilled failure details or null if not found
     */
    DistilledFailureDto getExecutionFailure(String runId, String testName);
}
