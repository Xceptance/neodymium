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
package org.neodymium.ai.executor;

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.SutAttachment;

/**
 * Mock implementation of {@link SutState} used in unit testing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MockSutState implements SutState
{
    /**
     * The primary text content.
     */
    private final String textContent;

    /**
     * The list of attachments.
     */
    private final List<SutAttachment> attachments;

    /**
     * The state content hash.
     */
    private final String contentHash;

    /**
     * Constructs a MockSutState with text content and content hash.
     *
     * @param textContent the text representation of the state
     * @param contentHash the content hash
     */
    public MockSutState(final String textContent, final String contentHash)
    {
        this(textContent, Collections.emptyList(), contentHash);
    }

    /**
     * Constructs a MockSutState with detailed text content, attachments list, and content hash.
     *
     * @param textContent the text representation of the state
     * @param attachments the list of attachments
     * @param contentHash the content hash
     */
    public MockSutState(final String textContent, final List<SutAttachment> attachments, final String contentHash)
    {
        this.textContent = textContent;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
        this.contentHash = contentHash;
    }

    /**
     * Returns the text content.
     *
     * @return the text content string
     */
    @Override
    public String getTextContent()
    {
        return this.textContent;
    }

    /**
     * Returns the list of attachments.
     *
     * @return the list of attachments
     */
    @Override
    public List<SutAttachment> getAttachments()
    {
        return this.attachments;
    }

    /**
     * Returns the content hash.
     *
     * @return the content hash string
     */
    @Override
    public String getContentHash()
    {
        return this.contentHash;
    }
}
