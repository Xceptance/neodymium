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
package com.xceptance.aura.core.service.impl;

import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.dto.ExecutionLaunchResultDto;
import com.xceptance.aura.core.service.AuraExecutionService;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.mcp.security.HostAllowlistValidator;
import com.xceptance.aura.mcp.service.McpExecutionCoordinator;
import com.xceptance.neodymium.aura.AuraQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AuraExecutionService coordinating test runs and queue cancellations.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraExecutionServiceImpl implements AuraExecutionService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraExecutionServiceImpl.class);

    private final McpExecutionCoordinator executionCoordinator;
    private final AuraQueueService queueService;
    private final HostAllowlistValidator allowlistValidator;
    private final AuraPlaybookService playbookService;

    public AuraExecutionServiceImpl(
        final McpExecutionCoordinator executionCoordinator,
        final AuraQueueService queueService,
        final HostAllowlistValidator allowlistValidator,
        final AuraPlaybookService playbookService)
    {
        this.executionCoordinator = executionCoordinator;
        this.queueService = queueService;
        this.allowlistValidator = allowlistValidator;
        this.playbookService = playbookService;
    }

    @Override
    public ExecutionLaunchResultDto dispatchRun(final String testPath, final String browserProfile, final String dataset)
    {
        if (!allowlistValidator.isSafeTestPath(testPath))
        {
            return ExecutionLaunchResultDto.error("Invalid or unsafe test path: " + testPath);
        }

        final String resolvedPath = (playbookService != null) ? playbookService.resolvePlaybookPath(testPath) : testPath;
        if (resolvedPath == null)
        {
            return ExecutionLaunchResultDto.error("Test playbook not found: " + testPath);
        }

        try
        {
            final McpExecutionCoordinator.DispatchResult dispatch = executionCoordinator.submitRun(resolvedPath, browserProfile, dataset);
            return new ExecutionLaunchResultDto(
                dispatch.getRunId(),
                dispatch.getStatus(),
                dispatch.getQueuePosition(),
                dispatch.getMessage()
            );
        }
        catch (final Exception e)
        {
            LOGGER.error("Execution dispatch failed for playbook '{}'", testPath, e);
            return ExecutionLaunchResultDto.error("Failed to dispatch test execution: " + e.getMessage());
        }
    }

    @Override
    public ExecutionLaunchResultDto dispatchSuite(final List<String> testPaths, final String browserProfile)
    {
        if (testPaths == null || testPaths.isEmpty())
        {
            return ExecutionLaunchResultDto.error("Suite testPaths cannot be null or empty.");
        }

        final List<String> resolvedPaths = new ArrayList<>();
        for (final String path : testPaths)
        {
            if (!allowlistValidator.isSafeTestPath(path))
            {
                return ExecutionLaunchResultDto.error("Invalid or unsafe test path in suite: " + path);
            }
            final String resolved = (playbookService != null) ? playbookService.resolvePlaybookPath(path) : path;
            if (resolved == null)
            {
                return ExecutionLaunchResultDto.error("Test playbook not found in suite: " + path);
            }
            resolvedPaths.add(resolved);
        }

        try
        {
            final McpExecutionCoordinator.DispatchResult dispatch = executionCoordinator.submitSuite(resolvedPaths, browserProfile);
            return new ExecutionLaunchResultDto(
                dispatch.getRunId(),
                dispatch.getStatus(),
                dispatch.getQueuePosition(),
                dispatch.getMessage()
            );
        }
        catch (final Exception e)
        {
            LOGGER.error("Execution dispatch failed for suite ({} tests)", testPaths.size(), e);
            return ExecutionLaunchResultDto.error("Failed to dispatch suite execution: " + e.getMessage());
        }
    }

    @Override
    public AuraOperationResult<Void> cancelRun(final String runId)
    {
        try
        {
            final boolean cancelledInCoordinator = executionCoordinator.cancelRun(runId);
            if (cancelledInCoordinator)
            {
                return AuraOperationResult.cancelled("Test run cancelled successfully.");
            }

            final String activeRunId = queueService.getCurrentRunId();
            if (queueService.isRunningQueue() && runId != null && runId.equals(activeRunId))
            {
                queueService.stopProcess();
                return AuraOperationResult.cancelled("Test run cancelled successfully.");
            }

            final String coordinatorStatus = executionCoordinator.getRunStatus(runId);
            if ("COMPLETED".equalsIgnoreCase(coordinatorStatus) || "CANCELLED".equalsIgnoreCase(coordinatorStatus))
            {
                return AuraOperationResult.alreadyCompleted("The run has already completed or was previously cancelled.");
            }

            if (coordinatorStatus == null && !queueService.isRunningQueue())
            {
                return AuraOperationResult.notFound("Run '" + runId + "' was not found.");
            }

            if (!queueService.isRunningQueue())
            {
                return AuraOperationResult.alreadyCompleted("The run has already completed or is not currently active.");
            }

            return AuraOperationResult.notFound("Run '" + runId + "' is not active (current active run is '" + activeRunId + "').");
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to cancel run '{}'", runId, e);
            return AuraOperationResult.error("Error occurred while cancelling run: " + e.getMessage());
        }
    }

    @Override
    public String getCoordinatorRunStatus(final String runId)
    {
        try
        {
            return executionCoordinator.getRunStatus(runId);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Error querying coordinator run status for '{}': {}", runId, e.getMessage());
            return null;
        }
    }
}
