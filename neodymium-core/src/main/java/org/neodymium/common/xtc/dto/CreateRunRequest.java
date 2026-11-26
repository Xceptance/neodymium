/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common.xtc.dto;

// Data Transfer Object for creating a new test run in the XTC API.
public class CreateRunRequest
{
    private final String startedAt;

    private final Integer estimatedDuration;

    private final String name;

    private final String testInstance;

    private final String profile;

    private final String link;

    private final String buildNumber;

    private final String description;

    public CreateRunRequest(String startedAt, Integer estimatedDuration, String name, String testInstance, String profile, String link, String buildNumber,
                            String description)
    {
        this.startedAt = startedAt;
        this.estimatedDuration = estimatedDuration;
        this.name = name;
        this.testInstance = testInstance;
        this.profile = profile;
        this.link = link;
        this.buildNumber = buildNumber;
        this.description = description;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public Integer getEstimatedDuration()
    {
        return estimatedDuration;
    }

    public String getName()
    {
        return name;
    }

    public String getTestInstance()
    {
        return testInstance;
    }

    public String getProfile()
    {
        return profile;
    }

    public String getLink()
    {
        return link;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public String getDescription()
    {
        return description;
    }
}
