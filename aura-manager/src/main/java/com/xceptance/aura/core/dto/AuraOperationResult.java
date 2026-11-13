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
package com.xceptance.aura.core.dto;

/**
 * Universal operation result container capturing execution state, error classification,
 * and structured diagnostic messages for both programmatic and AI agent consumption.
 *
 * @param <T> data payload type
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraOperationResult<T>
{
    public enum Status
    {
        SUCCESS,
        ERROR,
        CONFIRMATION_REQUIRED,
        NOT_FOUND,
        ALREADY_COMPLETED,
        CANCELLED
    }

    private final Status status;
    private final T data;
    private final String message;
    private final String confirmationToken;
    private final Integer expiresInSeconds;

    private AuraOperationResult(
        final Status status,
        final T data,
        final String message,
        final String confirmationToken,
        final Integer expiresInSeconds)
    {
        this.status = status;
        this.data = data;
        this.message = message;
        this.confirmationToken = confirmationToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public static <T> AuraOperationResult<T> success(final T data, final String message)
    {
        return new AuraOperationResult<>(Status.SUCCESS, data, message, null, null);
    }

    public static <T> AuraOperationResult<T> error(final String message)
    {
        return new AuraOperationResult<>(Status.ERROR, null, message, null, null);
    }

    public static <T> AuraOperationResult<T> notFound(final String message)
    {
        return new AuraOperationResult<>(Status.NOT_FOUND, null, message, null, null);
    }

    public static <T> AuraOperationResult<T> cancelled(final String message)
    {
        return new AuraOperationResult<>(Status.CANCELLED, null, message, null, null);
    }

    public static <T> AuraOperationResult<T> alreadyCompleted(final String message)
    {
        return new AuraOperationResult<>(Status.ALREADY_COMPLETED, null, message, null, null);
    }

    public static <T> AuraOperationResult<T> confirmationRequired(
        final String token,
        final int expiresInSeconds,
        final String message)
    {
        return new AuraOperationResult<>(Status.CONFIRMATION_REQUIRED, null, message, token, expiresInSeconds);
    }

    public Status getStatus()
    {
        return status;
    }

    public T getData()
    {
        return data;
    }

    public String getMessage()
    {
        return message;
    }

    public String getConfirmationToken()
    {
        return confirmationToken;
    }

    public Integer getExpiresInSeconds()
    {
        return expiresInSeconds;
    }

    public boolean isSuccess()
    {
        return status == Status.SUCCESS;
    }
}
