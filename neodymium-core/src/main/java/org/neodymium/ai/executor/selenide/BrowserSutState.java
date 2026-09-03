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
package org.neodymium.ai.executor.selenide;

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;

/**
 * Concrete browser implementation of {@link SutState} representing the state of
 * the System Under Test (SUT) driven via Selenide/WebDriver.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class BrowserSutState implements SutState
{
    /**
     * The primary DOM HTML source code representation.
     */
    private final String htmlSource;

    /**
     * The list of visual screenshots or attachments captured from the browser.
     */
    private final List<SutAttachment> attachments;

    /**
     * Calculated perceptual layout or MD5 text hash of the SUT state.
     */
    private final String contentHash;

    /**
     * Constructs a BrowserSutState.
     *
     * @param htmlSource the raw DOM HTML source code
     * @param attachments the visual screenshots or binary attachments list
     * @param contentHash the content hash
     */
    public BrowserSutState(
        final String htmlSource,
        final List<SutAttachment> attachments,
        final String contentHash
    )
    {
        this.htmlSource = htmlSource;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
        this.contentHash = contentHash;
    }

    /**
     * Returns the DOM HTML source text representation.
     *
     * @return the raw DOM HTML string
     */
    @Override
    public String getTextContent()
    {
        return this.htmlSource;
    }

    /**
     * Returns the unmodifiable list of visual screenshots/attachments.
     *
     * @return the list of visual attachments
     */
    @Override
    public List<SutAttachment> getAttachments()
    {
        return this.attachments;
    }

    /**
     * Returns the calculated layout hash of the page state.
     *
     * @return the content hash string
     */
    @Override
    public String getContentHash()
    {
        return this.contentHash;
    }
}
