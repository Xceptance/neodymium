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

import java.util.List;

/**
 * Protocol-neutral domain DTO representing a paginated subset of distilled failures
 * using cursor-based pagination to guarantee zero duplication when limit fluctuates.
 *
 * @param <T> failure detail type
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class FailurePageResultDto<T>
{
    private final String runId;
    private final String category;
    private final String cursor;
    private final String nextCursor;
    private final boolean hasMore;
    private final int limit;
    private final int totalCount;
    private final List<T> failures;

    public FailurePageResultDto(
        final String runId,
        final String category,
        final String cursor,
        final String nextCursor,
        final boolean hasMore,
        final int limit,
        final int totalCount,
        final List<T> failures)
    {
        this.runId = runId;
        this.category = category;
        this.cursor = cursor;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.limit = limit;
        this.totalCount = totalCount;
        this.failures = failures != null ? failures : List.of();
    }

    public String getRunId()
    {
        return runId;
    }

    public String getCategory()
    {
        return category;
    }

    public String getCursor()
    {
        return cursor;
    }

    public String getNextCursor()
    {
        return nextCursor;
    }

    public boolean isHasMore()
    {
        return hasMore;
    }

    public int getLimit()
    {
        return limit;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public List<T> getFailures()
    {
        return failures;
    }
}
