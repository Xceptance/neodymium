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

import java.util.List;
import org.neodymium.ai.client.SutAttachment;

/**
 * Interface representing the state of the System Under Test (SUT) in an extensible format.
 * Encapsulates the primary text representation, binary attachments (e.g. screenshots),
 * and dynamic hash values.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface SutState
{
    /**
     * Retrieves the primary text representation representing the state (DOM, JSON, CLI printout).
     *
     * @return the raw state text content
     */
    String getTextContent();

    /**
     * Retrieves optional visual or binary attachments (like screenshots or console logs).
     *
     * @return the list of SUT state attachments
     */
    List<SutAttachment> getAttachments();

    /**
     * Retrieves the perceptual visual hash or content hash of the state.
     *
     * @return the state content hash string
     */
    String getContentHash();
}
