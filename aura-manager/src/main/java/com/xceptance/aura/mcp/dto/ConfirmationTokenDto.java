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
