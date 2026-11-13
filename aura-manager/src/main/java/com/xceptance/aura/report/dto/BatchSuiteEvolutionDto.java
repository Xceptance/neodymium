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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object encapsulating the cumulative suite evolution trend
 * from the initial run to the latest execution state S.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class BatchSuiteEvolutionDto
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<BatchSuiteEvolutionPointDto> points;
    private final int initialTestsCount;
    private final int finalTestsCount;
    private final int netTestsGrowth;
    private final double initialPassRate;
    private final double finalPassRate;
    private final double passRateDelta;
    private final boolean isPassRatePositive;
    private final String pointsJson;
    private final String activeRunId;

    public BatchSuiteEvolutionDto(final List<BatchSuiteEvolutionPointDto> points)
    {
        this(points, null);
    }

    public BatchSuiteEvolutionDto(final List<BatchSuiteEvolutionPointDto> points, final String activeRunId)
    {
        if (points == null || points.isEmpty())
        {
            this.points = Collections.emptyList();
            this.initialTestsCount = 0;
            this.finalTestsCount = 0;
            this.netTestsGrowth = 0;
            this.initialPassRate = 0.0;
            this.finalPassRate = 0.0;
            this.passRateDelta = 0.0;
            this.isPassRatePositive = true;
            this.pointsJson = "[]";
            this.activeRunId = (activeRunId != null) ? activeRunId : "";
        }
        else
        {
            this.points = List.copyOf(points);
            final BatchSuiteEvolutionPointDto first = this.points.get(0);
            final BatchSuiteEvolutionPointDto last = this.points.get(this.points.size() - 1);
            this.initialTestsCount = first.getTotalTestsKnown();
            this.finalTestsCount = last.getTotalTestsKnown();
            this.netTestsGrowth = this.finalTestsCount - this.initialTestsCount;
            this.initialPassRate = first.getPassRate();
            this.finalPassRate = last.getPassRate();
            this.passRateDelta = this.finalPassRate - this.initialPassRate;
            this.isPassRatePositive = this.passRateDelta >= 0.0;
            this.pointsJson = serializePoints(this.points);
            this.activeRunId = (activeRunId != null && !activeRunId.isBlank()) ? activeRunId : last.getRunId();
        }
    }

    private static String serializePoints(final List<BatchSuiteEvolutionPointDto> pointList)
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(pointList);
        }
        catch (final JsonProcessingException e)
        {
            return "[]";
        }
    }

    public List<BatchSuiteEvolutionPointDto> getPoints()
    {
        return points;
    }

    public int getInitialTestsCount()
    {
        return initialTestsCount;
    }

    public int getFinalTestsCount()
    {
        return finalTestsCount;
    }

    public int getNetTestsGrowth()
    {
        return netTestsGrowth;
    }

    public double getInitialPassRate()
    {
        return initialPassRate;
    }

    public double getFinalPassRate()
    {
        return finalPassRate;
    }

    public double getPassRateDelta()
    {
        return passRateDelta;
    }

    public boolean isPassRatePositive()
    {
        return isPassRatePositive;
    }

    public String getPointsJson()
    {
        return pointsJson;
    }

    public boolean hasEvolution()
    {
        return points.size() > 1;
    }

    public boolean hasPoints()
    {
        return !points.isEmpty();
    }

    public String getGrowthLabel()
    {
        if (netTestsGrowth > 0)
        {
            return "+" + netTestsGrowth + " tests";
        }
        else if (netTestsGrowth < 0)
        {
            return netTestsGrowth + " tests";
        }
        return "0 new";
    }

    public String getPassRateDeltaFormatted()
    {
        return String.format(Locale.ROOT, "%s%.1f%%", (passRateDelta >= 0.0 ? "+" : ""), passRateDelta);
    }

    public String getActiveRunId()
    {
        return activeRunId;
    }

    public boolean isHistoricalActive()
    {
        if (activeRunId == null || activeRunId.isBlank() || points.isEmpty())
        {
            return false;
        }
        final String latestRunId = points.get(points.size() - 1).getRunId();
        return !activeRunId.equals(latestRunId);
    }

    public BatchSuiteEvolutionPointDto getActivePoint()
    {
        if (points.isEmpty())
        {
            return null;
        }
        if (activeRunId != null && !activeRunId.isBlank())
        {
            for (final BatchSuiteEvolutionPointDto pt : points)
            {
                if (activeRunId.equals(pt.getRunId()))
                {
                    return pt;
                }
            }
        }
        return points.get(points.size() - 1);
    }
}
