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
import com.xceptance.aura.core.dto.PlaybookSummaryDto;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.mcp.security.HostAllowlistValidator;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of AuraPlaybookService providing safe, validated playbook management.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraPlaybookServiceImpl implements AuraPlaybookService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraPlaybookServiceImpl.class);

    private final AuraFileService fileService;
    private final HostAllowlistValidator allowlistValidator;

    public AuraPlaybookServiceImpl(
        final AuraFileService fileService,
        final HostAllowlistValidator allowlistValidator)
    {
        this.fileService = fileService;
        this.allowlistValidator = allowlistValidator;
    }

    @Override
    public List<PlaybookSummaryDto> listPlaybooks(final String filter)
    {
        final List<YamlFileDto> files;
        try
        {
            files = fileService.getYamlFilesList();
        }
        catch (final Exception e)
        {
            LOGGER.error("Error retrieving YAML files list: {}", e.getMessage(), e);
            return Collections.emptyList();
        }

        if (files == null)
        {
            return Collections.emptyList();
        }

        final List<PlaybookSummaryDto> result = new ArrayList<>();
        final String lowerFilter = (filter != null) ? filter.toLowerCase().trim() : "";

        for (final YamlFileDto f : files)
        {
            final String filePath = (f.file != null) ? f.file : "";
            final String fileName = (f.getFileName() != null) ? f.getFileName() : "";
            if (!lowerFilter.isEmpty() && !filePath.toLowerCase().contains(lowerFilter) && !fileName.toLowerCase().contains(lowerFilter))
            {
                continue;
            }

            final int datasetCount = (f.datasets != null) ? f.datasets.size() : 0;
            result.add(new PlaybookSummaryDto(
                filePath,
                fileName,
                datasetCount,
                f.isHasError(),
                f.getErrorMessage()
            ));
        }

        return result;
    }

    @Override
    public String resolvePlaybookPath(final String testPath)
    {
        if (testPath == null || testPath.isBlank())
        {
            return null;
        }

        final String cleanPath = testPath.trim().replace('\\', '/');
        if (!allowlistValidator.isSafeTestPath(cleanPath))
        {
            return null;
        }

        // Fast path: if exact relative path exists directly on disk, return immediately without directory traversal
        try
        {
            final File resourcesDir = fileService.getResourcesDirectory();
            if (resourcesDir != null && resourcesDir.isDirectory())
            {
                final File direct = new File(resourcesDir, cleanPath);
                if (direct.exists() && direct.isFile())
                {
                    return cleanPath;
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Direct file check failed for path '{}': {}", cleanPath, e.getMessage());
        }

        final List<YamlFileDto> files;
        try
        {
            files = fileService.getYamlFilesList();
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to retrieve YAML files for playbook resolution: {}", e.getMessage());
            return cleanPath;
        }

        if (files == null || files.isEmpty())
        {
            return cleanPath;
        }

        // 1. Direct exact path match
        for (final YamlFileDto f : files)
        {
            if (f.file != null && f.file.replace('\\', '/').equalsIgnoreCase(cleanPath))
            {
                return f.file.replace('\\', '/');
            }
        }

        // Extract filename from input (e.g. "tests/examples/google.yaml" -> "google.yaml")
        final String inputFileName = cleanPath.contains("/") ? cleanPath.substring(cleanPath.lastIndexOf('/') + 1) : cleanPath;

        // 2. Exact filename match (case-sensitive)
        for (final YamlFileDto f : files)
        {
            final String fileName = f.getFileName();
            if (fileName != null && fileName.equals(inputFileName) && f.file != null)
            {
                return f.file.replace('\\', '/');
            }
        }

        // 3. Exact filename match (case-insensitive)
        for (final YamlFileDto f : files)
        {
            final String fileName = f.getFileName();
            if (fileName != null && fileName.equalsIgnoreCase(inputFileName) && f.file != null)
            {
                return f.file.replace('\\', '/');
            }
        }

        // Fall back to cleanPath if no better match found
        return cleanPath;
    }

    @Override
    public AuraOperationResult<String> readPlaybook(final String testPath)
    {
        if (!allowlistValidator.isSafeTestPath(testPath))
        {
            return AuraOperationResult.error("Invalid or unsafe test path: " + testPath);
        }

        try
        {
            final String content = fileService.readYamlFileContent(testPath);
            if (content == null)
            {
                return AuraOperationResult.notFound("Playbook not found: " + testPath);
            }
            return AuraOperationResult.success(content, "Playbook loaded successfully.");
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to read playbook at path '{}'", testPath, e);
            return AuraOperationResult.error("Failed to read file: " + e.getMessage());
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected error reading playbook at path '{}'", testPath, e);
            return AuraOperationResult.error("Unexpected error reading playbook: " + e.getMessage());
        }
    }

    @Override
    public AuraOperationResult<Void> savePlaybook(final String testPath, final String content)
    {
        if (!allowlistValidator.isSafeTestPath(testPath))
        {
            return AuraOperationResult.error("Invalid or unsafe test path: " + testPath);
        }

        if (content == null || content.trim().isEmpty())
        {
            return AuraOperationResult.error("Playbook content cannot be empty.");
        }

        try
        {
            final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            yaml.load(content);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Rejected saving malformed YAML playbook '{}': {}", testPath, e.getMessage());
            return AuraOperationResult.error("Malformed YAML syntax: " + e.getMessage());
        }

        try
        {
            fileService.saveYamlFileContent(testPath, content);
            return AuraOperationResult.success(null, "Test playbook saved successfully: " + testPath);
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to save playbook '{}'", testPath, e);
            return AuraOperationResult.error("Failed to save file: " + e.getMessage());
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected error saving playbook '{}'", testPath, e);
            return AuraOperationResult.error("Unexpected error saving playbook: " + e.getMessage());
        }
    }

    @Override
    public AuraOperationResult<Boolean> deletePlaybook(final String testPath)
    {
        if (!allowlistValidator.isSafeTestPath(testPath))
        {
            return AuraOperationResult.error("Invalid or unsafe test path: " + testPath);
        }

        try
        {
            final boolean deleted = fileService.deleteYamlFile(testPath);
            if (!deleted)
            {
                return AuraOperationResult.notFound("Test playbook did not exist: " + testPath);
            }
            return AuraOperationResult.success(true, "Test playbook deleted successfully.");
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to delete playbook '{}'", testPath, e);
            return AuraOperationResult.error("Failed to delete file: " + e.getMessage());
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected error deleting playbook '{}'", testPath, e);
            return AuraOperationResult.error("Unexpected error deleting playbook: " + e.getMessage());
        }
    }
}
