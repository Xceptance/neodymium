/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.client;

/**
 * Immutable record representing binary or media attachments (such as screenshots or API request traces)
 * associated with a specific System Under Test (SUT) state.
 *
 * @param mediaType the MIME type of the attachment (e.g., "image/png", "application/json")
 * @param filePath the path reference to the external file storage for HTML reporting
 * @param base64Data the base64-encoded raw binary data of the attachment (may be null if only used for reporting)
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record SutAttachment(
    String mediaType,
    String filePath,
    String base64Data
)
{
}
