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
package com.xceptance.aura.mcp.tool;

import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.dto.ExecutionLaunchResultDto;
import com.xceptance.aura.core.dto.FailurePageResultDto;
import com.xceptance.aura.core.dto.PlaybookSummaryDto;
import com.xceptance.aura.core.service.AuraDiagnosticService;
import com.xceptance.aura.core.service.AuraExecutionService;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import com.xceptance.aura.mcp.security.TwoStepConfirmationManager;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes core Neodymium Aura execution, query, and management tools via Model Context Protocol (MCP).
 * Acts as a thin protocol adapter delegating domain operations to the shared Aura core services.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraMcpToolService
{
    private final AuraPlaybookService playbookService;
    private final AuraExecutionService executionService;
    private final AuraDiagnosticService diagnosticService;
    private final TwoStepConfirmationManager confirmationManager;

    public AuraMcpToolService(
        final AuraPlaybookService playbookService,
        final AuraExecutionService executionService,
        final AuraDiagnosticService diagnosticService,
        final TwoStepConfirmationManager confirmationManager)
    {
        this.playbookService = playbookService;
        this.executionService = executionService;
        this.diagnosticService = diagnosticService;
        this.confirmationManager = confirmationManager;
    }

    /**
     * Lists available test playbooks with metadata and steps count.
     */
    @McpTool(name = "list_tests", description = "List all available test playbooks with step counts and tags.")
    public Map<String, Object> listTests(
        @McpToolParam(description = "Optional filter string to match filename or tags") final String filter)
    {
        final List<PlaybookSummaryDto> playbooks = playbookService.listPlaybooks(filter);
        final List<Map<String, Object>> resultList = new ArrayList<>();

        for (final PlaybookSummaryDto p : playbooks)
        {
            final Map<String, Object> item = new HashMap<>();
            item.put("file", p.getFile());
            item.put("fileName", p.getFileName());
            item.put("datasetCount", p.getDatasetCount());
            item.put("hasError", p.isHasError());
            if (p.isHasError())
            {
                item.put("errorMessage", p.getErrorMessage());
            }
            resultList.add(item);
        }

        return Map.of(
            "total", resultList.size(),
            "tests", resultList
        );
    }

    /**
     * Dispatches an asynchronous test execution. Returns immediately (<100ms).
     */
    @McpTool(name = "run_test", description = "Dispatch an asynchronous test playbook run. Call this tool directly with the provided test name or path without querying list_tests or list_browser_profiles first. Returns immediately with RUNNING or ENQUEUED status.")
    public Map<String, Object> runTest(
        @McpToolParam(description = "Relative path or simple filename of YAML playbook (e.g. tests/Login.yaml or Login.yaml). Filenames are automatically resolved.") final String testPath,
        @McpToolParam(description = "Optional browser profile tag (e.g. Chrome_1500x1000). If omitted or null, defaults automatically to the configured default Chrome profile. Pass directly without prior validation.") final String browserProfile,
        @McpToolParam(description = "Optional dataset row identifier. Omit if not explicitly requested.") final String dataset)
    {
        final ExecutionLaunchResultDto launch = executionService.dispatchRun(testPath, browserProfile, dataset);
        final Map<String, Object> res = new HashMap<>();
        if (launch.getRunId() != null)
        {
            res.put("runId", launch.getRunId());
        }
        res.put("status", launch.getStatus());
        res.put("message", launch.getMessage());
        if (launch.getQueuePosition() > 0)
        {
            res.put("queuePosition", launch.getQueuePosition());
        }
        return res;
    }

    /**
     * Dispatches a suite of multiple test playbooks as a batched asynchronous test execution.
     */
    @McpTool(name = "run_suite", description = "Dispatch a batch of test playbooks as a unified test suite run. Returns immediately with RUNNING or ENQUEUED status.")
    public Map<String, Object> runSuite(
        @McpToolParam(description = "List of relative YAML playbook paths or filenames") final List<String> testPaths,
        @McpToolParam(description = "Optional browser profile tag (e.g. Chrome_1500x1000). If omitted or null, defaults automatically to the configured default Chrome profile. Pass directly without prior validation.") final String browserProfile)
    {
        final ExecutionLaunchResultDto launch = executionService.dispatchSuite(testPaths, browserProfile);
        final Map<String, Object> res = new HashMap<>();
        if (launch.getRunId() != null)
        {
            res.put("runId", launch.getRunId());
        }
        res.put("status", launch.getStatus());
        res.put("message", launch.getMessage());
        if (launch.getQueuePosition() > 0)
        {
            res.put("queuePosition", launch.getQueuePosition());
        }
        return res;
    }

    /**
     * Queries execution status and returns an adaptive failure payload with universal metrics envelope.
     */
    @McpTool(name = "get_status", description = "Query test execution status, universal metrics envelope, and adaptive failure clusters.")
    public RunStatusResponseDto getStatus(
        @McpToolParam(description = "The test run identifier (e.g. run_20260930_101500)") final String runId)
    {
        return diagnosticService.getRunStatus(runId);
    }

    /**
     * Inspects detailed stack traces for a run using cursor-based pagination.
     */
    @McpTool(name = "get_run_failures", description = "Cursor-paginated drill-down tool to inspect token-bounded stack traces across a test run.")
    public Map<String, Object> getRunFailures(
        @McpToolParam(description = "The test run identifier") final String runId,
        @McpToolParam(description = "Optional failure category filter (e.g. ELEMENT_NOT_FOUND)") final String category,
        @McpToolParam(description = "Zero-based integer offset index (e.g. '0', '5', '10') from nextCursor of previous call") final String cursor,
        @McpToolParam(description = "Page size between 1 and 10 (default: 5)") final Integer limit)
    {
        final FailurePageResultDto<DistilledFailureDto> pageResult = diagnosticService.getRunFailures(runId, category, cursor, limit);
        if (pageResult.getTotalCount() == 0 && (pageResult.getFailures() == null || pageResult.getFailures().isEmpty()))
        {
            final RunStatusResponseDto status = diagnosticService.getRunStatus(runId);
            if ("NOT_FOUND".equalsIgnoreCase(status.getStatus()))
            {
                return Map.of("error", "Run report not found for runId: " + runId);
            }
        }

        final Map<String, Object> res = new HashMap<>();
        res.put("runId", pageResult.getRunId());
        res.put("category", pageResult.getCategory());
        res.put("cursor", pageResult.getCursor());
        if (pageResult.getNextCursor() != null)
        {
            res.put("nextCursor", pageResult.getNextCursor());
        }
        res.put("hasMore", pageResult.isHasMore());
        res.put("limit", pageResult.getLimit());
        res.put("totalCount", pageResult.getTotalCount());
        res.put("failures", pageResult.getFailures());
        return res;
    }

    /**
     * Inspects failure details for a specific single test.
     */
    @McpTool(name = "get_execution_failure", description = "Inspect detailed failure stack trace and DOM context for an individual test execution.")
    public Map<String, Object> getExecutionFailure(
        @McpToolParam(description = "The test run identifier") final String runId,
        @McpToolParam(description = "Test name, title, or identifier") final String testName)
    {
        final DistilledFailureDto failure = diagnosticService.getExecutionFailure(runId, testName);
        if (failure == null)
        {
            return Map.of("error", "No failure found for test '" + testName + "' in run '" + runId + "'.");
        }

        return Map.of(
            "runId", runId,
            "testName", failure.getTestName(),
            "category", failure.getClusterCategory(),
            "stepText", failure.getStepText() != null ? failure.getStepText() : "",
            "locator", failure.getLocator() != null ? failure.getLocator() : "",
            "stackTrace", failure.getCleanedStackTrace() != null ? failure.getCleanedStackTrace() : "",
            "domContext", failure.getDomSnippet() != null ? failure.getDomSnippet() : ""
        );
    }

    /**
     * Creates or updates a test playbook YAML.
     */
    @McpTool(name = "create_test", description = "Create or update a test playbook YAML file.")
    public Map<String, Object> createTest(
        @McpToolParam(description = "Destination relative YAML path (e.g. tests/Checkout.yaml)") final String testPath,
        @McpToolParam(description = "YAML playbook file contents") final String content)
    {
        final AuraOperationResult<Void> result = playbookService.savePlaybook(testPath, content);
        return Map.of(
            "status", result.isSuccess() ? "SUCCESS" : "ERROR",
            "message", result.getMessage()
        );
    }

    /**
     * Deletes a test playbook with two-step confirmation token handshake.
     */
    @McpTool(name = "delete_test", description = "Delete a test playbook file. Enforces two-step confirmation handshake.")
    public Map<String, Object> deleteTest(
        @McpToolParam(description = "Relative path of test playbook to delete") final String testPath,
        @McpToolParam(description = "Confirmation token received from initial delete request") final String confirmationToken)
    {
        if (confirmationToken == null || confirmationToken.isBlank())
        {
            final String token = confirmationManager.issueToken("delete_test", testPath);
            return Map.of(
                "status", "CONFIRMATION_REQUIRED",
                "confirmationToken", token,
                "expiresInSeconds", 60,
                "message", "Destructive operation: To permanently delete '" + testPath + "', invoke delete_test with testPath and confirmationToken."
            );
        }

        final boolean valid = confirmationManager.validateAndConsumeToken(confirmationToken, "delete_test", testPath);
        if (!valid)
        {
            return Map.of("status", "ERROR", "message", "Invalid or expired confirmation token.");
        }

        final AuraOperationResult<Boolean> result = playbookService.deletePlaybook(testPath);
        return Map.of(
            "status", result.isSuccess() ? "SUCCESS" : (result.getStatus() == AuraOperationResult.Status.NOT_FOUND ? "NOT_FOUND" : "ERROR"),
            "message", result.getMessage()
        );
    }

    /**
     * Cancels an active test run with two-step confirmation token handshake.
     */
    @McpTool(name = "cancel_run", description = "Abort an active test run. Enforces two-step confirmation handshake.")
    public Map<String, Object> cancelRun(
        @McpToolParam(description = "Run identifier to cancel") final String runId,
        @McpToolParam(description = "Confirmation token received from initial cancel request") final String confirmationToken)
    {
        if (confirmationToken == null || confirmationToken.isBlank())
        {
            final String token = confirmationManager.issueToken("cancel_run", runId);
            return Map.of(
                "status", "CONFIRMATION_REQUIRED",
                "confirmationToken", token,
                "expiresInSeconds", 60,
                "message", "Disruptive operation: Aborting active test run '" + runId + "' will terminate running browser sessions. Call cancel_run with runId and confirmationToken to confirm."
            );
        }

        final boolean valid = confirmationManager.validateAndConsumeToken(confirmationToken, "cancel_run", runId);
        if (!valid)
        {
            return Map.of("status", "ERROR", "message", "Invalid or expired confirmation token.");
        }

        final AuraOperationResult<Void> result = executionService.cancelRun(runId);
        final String statusStr = switch (result.getStatus())
        {
            case CANCELLED -> "CANCELLED";
            case ALREADY_COMPLETED -> "ALREADY_COMPLETED";
            default -> "ERROR";
        };

        return Map.of(
            "status", statusStr,
            "message", result.getMessage()
        );
    }

    /**
     * Lists all configured browser profiles with their settings.
     */
    @McpTool(name = "list_browser_profiles", description = "List configured browser profiles (e.g. Chrome_1024x768, Firefox) with resolution and headless options.")
    public Map<String, Object> listBrowserProfiles()
    {
        final Map<String, BrowserConfiguration> profiles = MultibrowserConfiguration.getInstance().getBrowserProfiles();
        final List<Map<String, Object>> profileList = new ArrayList<>();

        if (profiles != null)
        {
            for (final Map.Entry<String, BrowserConfiguration> entry : profiles.entrySet())
            {
                final String tag = entry.getKey();
                final BrowserConfiguration config = entry.getValue();
                final Map<String, Object> item = new HashMap<>();
                item.put("browserTag", tag);
                item.put("name", config != null ? config.getName() : tag);
                item.put("headless", config != null && config.isHeadless());
                item.put("width", config != null ? config.getBrowserWidth() : 0);
                item.put("height", config != null ? config.getBrowserHeight() : 0);
                item.put("testEnvironment", config != null ? config.getTestEnvironment() : null);
                profileList.add(item);
            }
        }

        return Map.of(
            "total", profileList.size(),
            "browserProfiles", profileList
        );
    }

    /**
     * Reads the YAML content of a test playbook.
     */
    @McpTool(name = "get_test_content", description = "Read the raw YAML content of a test playbook file.")
    public Map<String, Object> getTestContent(
        @McpToolParam(description = "Relative path of the test playbook (e.g. tests/Login.yaml)") final String testPath)
    {
        final AuraOperationResult<String> result = playbookService.readPlaybook(testPath);
        if (!result.isSuccess())
        {
            return Map.of(
                "status", "ERROR",
                "message", result.getMessage()
            );
        }

        return Map.of(
            "status", "SUCCESS",
            "testPath", testPath,
            "content", result.getData() != null ? result.getData() : ""
        );
    }
}
