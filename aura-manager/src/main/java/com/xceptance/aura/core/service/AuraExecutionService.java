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

import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.dto.ExecutionLaunchResultDto;

import java.util.List;

/**
 * Service managing test playbook dispatching, suite execution, process lifecycle, and cancellations.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public interface AuraExecutionService
{
    /**
     * Submits a single test playbook for execution.
     *
     * @param testPath playbook file path
     * @param browserProfile optional browser profile key
     * @param dataset optional dataset ID
     * @return launch result with runId and queue status
     */
    ExecutionLaunchResultDto dispatchRun(String testPath, String browserProfile, String dataset);

    /**
     * Submits a suite of multiple test playbooks for batched execution.
     *
     * @param testPaths list of relative playbook paths
     * @param browserProfile optional browser profile key
     * @return launch result with runId and queue status
     */
    ExecutionLaunchResultDto dispatchSuite(List<String> testPaths, String browserProfile);

    /**
     * Cancels an active or queued execution run.
     *
     * @param runId target run ID
     * @return operation result indicating status
     */
    AuraOperationResult<Void> cancelRun(String runId);

    /**
     * Checks if a run is currently registered or active.
     *
     * @param runId run identifier
     * @return status string or null
     */
    String getCoordinatorRunStatus(String runId);
}
