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

/**
 * Interface defining pluggable binary attachment storage operations (Local mock folder vs S3/Azure Cloud Storage).
 *
 * @author Xceptance GmbH 2026
 */
public interface AttachmentStorageService
{
    /**
     * Uploads/saves a binary attachment (screenshot, video) and returns its accessible HTTP URL.
     *
     * @param runId the run ID
     * @param filename the attachment filename
     * @param content binary content bytes
     * @param contentType MIME content type
     * @return accessible HTTP URL
     */
    String storeAttachment(String runId, String filename, byte[] content, String contentType);

    /**
     * Deletes all attachments stored for a given run ID.
     *
     * @param runId the run ID
     */
    void deleteRunAttachments(String runId);
}
