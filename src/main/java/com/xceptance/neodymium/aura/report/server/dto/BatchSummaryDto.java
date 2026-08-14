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
package com.xceptance.neodymium.aura.report.server.dto;

import java.util.List;

/**
 * Data transfer object representing batch directory summary information.
 *
 * @author AI-generated: Gemini Advanced
 * @author Xceptance GmbH 2026
 */
public final class BatchSummaryDto
{
    private final String id;
    private final String name;
    private final String environment;
    private final String description;
    private final String lastExecuted;
    private final String latestPassRate;
    private final String knownBugs;
    private final List<String> locales;
    private final List<String> browsers;

    public BatchSummaryDto(
        final String id,
        final String name,
        final String environment,
        final String description,
        final String lastExecuted,
        final String latestPassRate,
        final String knownBugs,
        final List<String> locales,
        final List<String> browsers)
    {
        this.id = id;
        this.name = name;
        this.environment = environment;
        this.description = description;
        this.lastExecuted = lastExecuted;
        this.latestPassRate = latestPassRate;
        this.knownBugs = knownBugs;
        this.locales = locales;
        this.browsers = browsers;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public String getDescription()
    {
        return description;
    }

    public String getLastExecuted()
    {
        return lastExecuted;
    }

    public String getLatestPassRate()
    {
        return latestPassRate;
    }

    public String getKnownBugs()
    {
        return knownBugs;
    }

    public List<String> getLocales()
    {
        return locales;
    }

    public List<String> getBrowsers()
    {
        return browsers;
    }

    public String getLocalesCsv()
    {
        return locales != null ? String.join(",", locales) : "";
    }

    public String getBrowsersCsv()
    {
        return browsers != null ? String.join(",", browsers) : "";
    }
}
