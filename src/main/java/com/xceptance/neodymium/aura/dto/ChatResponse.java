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

import java.util.List;

/**
 * Data transfer object representing the structured AI Chat Response payload.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class ChatResponse
{
    public String message;
    public String thinking;
    public String action;
    public List<String> files;
    public String filename;
    public String content;
    public List<DatasetSelection> selectedDatasets;

    public ChatResponse()
    {
    }

    public ChatResponse(final String message, final String thinking, final String action, final List<String> files,
            final String filename, final String content, final List<DatasetSelection> selectedDatasets)
    {
        this.message = message;
        this.thinking = thinking;
        this.action = action;
        this.files = files;
        this.filename = filename;
        this.content = content;
        this.selectedDatasets = selectedDatasets;
    }
}
