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
package com.xceptance.aura.report.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Pluggable implementation mocking bucket storage using a local disk folder.
 *
 * @author Xceptance GmbH 2026
 */
@Service
@ConditionalOnProperty(name = "aura.report.storage.attachments.provider", havingValue = "local", matchIfMissing = true)
public class LocalFolderAttachmentStorageService implements AttachmentStorageService
{
    private static final Logger LOG = LoggerFactory.getLogger(LocalFolderAttachmentStorageService.class);

    @Value("${aura.report.storage.attachments.local.base-dir:storage/attachments/}")
    private String baseDir;

    @Override
    public String storeAttachment(final String runId, final String filename, final byte[] content, final String contentType)
    {
        try
        {
            final Path runDir = Paths.get(baseDir, "runs", runId);
            Files.createDirectories(runDir);
            final Path file = runDir.resolve(filename);
            Files.write(file, content);
            LOG.info("Saved local mock attachment for run {}: {}", runId, file.toAbsolutePath());
            return "/runs/" + runId + "/" + filename;
        }
        catch (final IOException e)
        {
            LOG.error("Failed to store local mock attachment for run {}: {}", runId, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public void deleteRunAttachments(final String runId)
    {
        try
        {
            final Path runDir = Paths.get(baseDir, "runs", runId);
            if (Files.exists(runDir))
            {
                Files.walk(runDir)
                    .map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
                LOG.info("Deleted local mock attachment folder for run {}", runId);
            }
        }
        catch (final IOException e)
        {
            LOG.warn("Failed to delete local mock attachments for run {}: {}", runId, e.getMessage());
        }
    }
}
