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
package org.neodymium.ai.client;

import java.io.IOException;
import java.util.Set;

/**
 * Interface representing an LLM connector capable of executing chat prompts
 * and declaring its supported execution capabilities.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface LlmProvider
{
    /**
     * Executes the chat request against the LLM provider.
     *
     * @param request the chat prompt and attachment context payload
     * @return the LLM response including raw content text and token usage
     * @throws IOException if network or provider-side execution fails
     */
    LlmResponse chat(final LlmRequest request) throws IOException;

    /**
     * Declares the specialized features/capabilities this provider is qualified to perform.
     *
     * @return the set of supported LlmCapabilities
     */
    Set<LlmCapability> getCapabilities();
}
