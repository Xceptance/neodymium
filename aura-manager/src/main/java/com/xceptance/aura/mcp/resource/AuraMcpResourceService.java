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
package com.xceptance.aura.mcp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.service.AuraDiagnosticService;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

/**
 * Exposes MCP resources for reading test run summaries and raw playbook definitions.
 * Delegates data retrieval to shared Aura core services.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraMcpResourceService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraMcpResourceService.class);

    private final AuraDiagnosticService diagnosticService;
    private final AuraPlaybookService playbookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuraMcpResourceService(
        final AuraDiagnosticService diagnosticService,
        final AuraPlaybookService playbookService)
    {
        this.diagnosticService = diagnosticService;
        this.playbookService = playbookService;
    }

    /**
     * Compact JSON run report summary.
     */
    @McpResource(uri = "test://runs/{runId}/summary", name = "Test Run Summary", mimeType = "application/json")
    public String getRunSummary(final String runId)
    {
        final RunStatusResponseDto dto = diagnosticService.getRunStatus(runId);
        if ("NOT_FOUND".equalsIgnoreCase(dto.getStatus()))
        {
            return "{\"error\": \"Run not found: " + runId + "\"}";
        }

        try
        {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to serialize run summary for runId: {}", runId, e);
            return "{\"error\": \"Serialization failure\"}";
        }
    }

    /**
     * Raw YAML playbook contents.
     */
    @McpResource(uri = "test://playbooks/{path}", name = "Playbook Definition", mimeType = "text/yaml")
    public String getPlaybookContent(final String path)
    {
        final AuraOperationResult<String> result = playbookService.readPlaybook(path);
        if (result.isSuccess())
        {
            return result.getData();
        }
        return "# Error: " + result.getMessage();
    }
}
