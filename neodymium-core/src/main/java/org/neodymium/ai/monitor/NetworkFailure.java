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
package org.neodymium.ai.monitor;

/**
 * Immutable record representing a captured network failure (HTTP 4xx, 5xx, or blocked/aborted request).
 *
 * @param url the requested URL
 * @param statusCode HTTP response status code (or 0 for aborted/blocked requests)
 * @param method HTTP method (GET, POST, etc.)
 * @param errorDescription description or status text of the error
 * @param timestamp epoch millisecond when the event was recorded
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public record NetworkFailure(
    String url,
    int statusCode,
    String method,
    String errorDescription,
    long timestamp
)
{
    /**
     * Checks if this failure is an HTTP 4xx client error.
     *
     * @return true if status is between 400 and 499 inclusive
     */
    public boolean is4xx()
    {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Checks if this failure is an HTTP 5xx server error.
     *
     * @return true if status is between 500 and 599 inclusive
     */
    public boolean is5xx()
    {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Checks if this failure is a network abort, blocked request, or CORS rejection without HTTP status.
     *
     * @return true if status is 0 or negative
     */
    public boolean isBlocked()
    {
        return statusCode <= 0;
    }

    /**
     * Checks whether this network failure matches the specified status filter.
     * Supported filters: {@code "all_failures"}, {@code "4xx"}, {@code "5xx"}, {@code "blocked"}.
     *
     * @param statusFilter the filter criteria (case-insensitive, null or empty defaults to all failures)
     * @return true if this failure satisfies the filter criteria
     */
    public boolean matchesFilter(final String statusFilter)
    {
        if (statusFilter == null || statusFilter.isBlank() || "all_failures".equalsIgnoreCase(statusFilter.trim()))
        {
            return true;
        }

        final String normalized = statusFilter.trim().toLowerCase();
        return switch (normalized)
        {
            case "4xx" -> is4xx();
            case "5xx" -> is5xx();
            case "blocked" -> isBlocked();
            default -> true;
        };
    }
}
