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
package com.xceptance.aura.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representation of a two-step confirmation required response.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmationTokenDto
{
    private String status = "CONFIRMATION_REQUIRED";

    private String confirmationToken;

    private String message;

    private int expiresInSeconds = 60;

    public ConfirmationTokenDto()
    {
    }

    public ConfirmationTokenDto(final String confirmationToken, final String message)
    {
        this.confirmationToken = confirmationToken;
        this.message = message;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public String getConfirmationToken()
    {
        return confirmationToken;
    }

    public void setConfirmationToken(final String confirmationToken)
    {
        this.confirmationToken = confirmationToken;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String message)
    {
        this.message = message;
    }

    public int getExpiresInSeconds()
    {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(final int expiresInSeconds)
    {
        this.expiresInSeconds = expiresInSeconds;
    }
}
