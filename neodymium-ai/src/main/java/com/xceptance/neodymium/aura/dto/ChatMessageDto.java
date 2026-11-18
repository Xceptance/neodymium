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
package com.xceptance.neodymium.aura.dto;

/**
 * Data transfer object representing an LLM chat message role and content.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class ChatMessageDto
{
    public String role;
    public String content;
    public String thinking;

    public ChatMessageDto()
    {
    }

    public ChatMessageDto(final String role, final String content)
    {
        this.role = role;
        this.content = content;
    }

    public ChatMessageDto(final String role, final String content, final String thinking)
    {
        this.role = role;
        this.content = content;
        this.thinking = thinking;
    }
}
