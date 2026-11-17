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
package com.xceptance.aura.chat.dto;

import java.util.List;

/**
 * Data transfer object representing an incoming dashboard chat prompt and conversation history.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public final class ChatRequest
{
    /**
     * The user's current prompt.
     */
    public String prompt;

    /**
     * Optional currently active playbook file in the editor.
     */
    public String activeFile;

    /**
     * Prior messages in the active chat session.
     */
    public List<ChatMessageDto> history;

    /**
     * Optional chat session identifier for scoped workflows such as two-turn confirmations.
     */
    public String sessionId;

    /**
     * Optional passive UI context describing the currently open dashboard page or run view.
     */
    public String viewContext;

    /**
     * Default constructor for deserialization.
     */
    public ChatRequest()
    {
    }

    /**
     * Creates a chat request.
     *
     * @param prompt user prompt text
     * @param activeFile optional active editor file
     * @param history conversation history preceding this prompt
     */
    public ChatRequest(final String prompt, final String activeFile, final List<ChatMessageDto> history)
    {
        this(prompt, activeFile, history, null, null);
    }

    /**
     * Creates a chat request bound to a specific chat session.
     *
     * @param prompt user prompt text
     * @param activeFile optional active editor file
     * @param history conversation history preceding this prompt
     * @param sessionId optional session identifier
     */
    public ChatRequest(final String prompt,
                       final String activeFile,
                       final List<ChatMessageDto> history,
                       final String sessionId)
    {
        this(prompt, activeFile, history, sessionId, null);
    }

    /**
     * Creates a chat request bound to a chat session and active UI view context.
     *
     * @param prompt user prompt text
     * @param activeFile optional active editor file
     * @param history conversation history preceding this prompt
     * @param sessionId optional session identifier
     * @param viewContext optional passive dashboard or report view context
     */
    public ChatRequest(final String prompt,
                       final String activeFile,
                       final List<ChatMessageDto> history,
                       final String sessionId,
                       final String viewContext)
    {
        this.prompt = prompt;
        this.activeFile = activeFile;
        this.history = history;
        this.sessionId = sessionId;
        this.viewContext = viewContext;
    }
}
