/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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
     * Indicates whether attached screenshots were captured full-page ({@code true}),
     * viewport-only ({@code false}), or unspecified/absent ({@code null}).
     */
    private final Boolean fullPageScreenshot;

    /**
     * Constructs a MockSutState with text content and content hash.
     *
     * @param textContent the text representation of the state
     * @param contentHash the content hash
     */
    public MockSutState(final String textContent, final String contentHash)
    {
        this(textContent, Collections.emptyList(), contentHash, null);
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
        this(textContent, attachments, contentHash, null);
    }

    /**
     * Constructs a MockSutState with detailed text content, attachments list, content hash,
     * and explicit screenshot capture scope metadata.
     *
     * @param textContent the text representation of the state
     * @param attachments the list of attachments
     * @param contentHash the content hash
     * @param fullPageScreenshot {@code true} if full-page, {@code false} if viewport-only, {@code null} if unspecified
     */
    public MockSutState(
        final String textContent,
        final List<SutAttachment> attachments,
        final String contentHash,
        final Boolean fullPageScreenshot
    )
    {
        this.textContent = textContent;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
        this.contentHash = contentHash;
        this.fullPageScreenshot = fullPageScreenshot;
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

    /**
     * Returns whether attached screenshots were captured full-page or viewport-only.
     *
     * @return {@code Boolean.TRUE} for full-page captures, {@code Boolean.FALSE} for viewport captures,
     *         or {@code null} when unspecified or absent
     */
    @Override
    public Boolean isFullPageScreenshot()
    {
        return this.fullPageScreenshot;
    }
}
