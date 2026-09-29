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
package com.xceptance.aura.report.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing historical execution trends for a test area across a batch's test runs.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class BatchAreaTrendDto
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String areaName;
    private final String areaGroup;
    private final int latestExecutionsCount;
    private final String latestStatusBadgeClass;
    private final String latestStatusLabel;
    private final List<AreaRunPointDto> runPoints;

    public BatchAreaTrendDto(
        final String areaName,
        final String areaGroup,
        final int latestExecutionsCount,
        final String latestStatusBadgeClass,
        final String latestStatusLabel,
        final List<AreaRunPointDto> runPoints)
    {
        this.areaName = areaName;
        this.areaGroup = areaGroup;
        this.latestExecutionsCount = latestExecutionsCount;
        this.latestStatusBadgeClass = latestStatusBadgeClass;
        this.latestStatusLabel = latestStatusLabel;
        this.runPoints = runPoints != null ? runPoints : new ArrayList<>();
    }

    public String getAreaName()
    {
        return areaName;
    }

    public String getAreaGroup()
    {
        return areaGroup;
    }

    public int getLatestExecutionsCount()
    {
        return latestExecutionsCount;
    }

    public String getLatestStatusBadgeClass()
    {
        return latestStatusBadgeClass;
    }

    public String getLatestStatusLabel()
    {
        return latestStatusLabel;
    }

    public List<AreaRunPointDto> getRunPoints()
    {
        return runPoints;
    }

    public String getRunPointsJson()
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(runPoints);
        }
        catch (final JsonProcessingException e)
        {
            return "[]";
        }
    }
}
