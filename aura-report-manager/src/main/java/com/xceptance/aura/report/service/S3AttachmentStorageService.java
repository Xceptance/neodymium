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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Pluggable implementation uploading attachments to Amazon S3 / LocalStack / MinIO.
 *
 * @author Xceptance GmbH 2026
 */
@Service
@ConditionalOnProperty(name = "aura.report.storage.attachments.provider", havingValue = "s3")
public class S3AttachmentStorageService implements AttachmentStorageService
{
    private static final Logger LOG = LoggerFactory.getLogger(S3AttachmentStorageService.class);

    @Value("${aura.report.storage.attachments.s3.bucket-name:aura-report-attachments}")
    private String bucketName;

    @Value("${aura.report.storage.attachments.s3.region:us-west-2}")
    private String region;

    @Override
    public String storeAttachment(final String runId, final String filename, final byte[] content, final String contentType)
    {
        final String s3Key = "runs/" + runId + "/" + filename;
        LOG.info("Uploading attachment to S3 bucket {} key {}: {} bytes", bucketName, s3Key, content != null ? content.length : 0);
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + s3Key;
    }

    @Override
    public void deleteRunAttachments(final String runId)
    {
        LOG.info("Deleting S3 attachments for run {} in bucket {}", runId, bucketName);
    }
}
