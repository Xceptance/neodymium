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

// Data Transfer Object for the response data of a test run in the XTC API.
public class ResponseData
{
    private final String name;

    private final String index;

    private final String description;

    private final String startedAt;

    private final String finishedAt;

    private final String state;

    private final String summary;

    private final String rating;

    public ResponseData(String index, String name, String startedAt, String finishedAt, String state, String summary, String description,
                                 String rating)
    {
        this.index = index;
        this.name = name;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.state = state;
        this.summary = summary;
        this.description = description;
        this.rating = rating;
    }

    public String getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public String getFinishedAt()
    {
        return finishedAt;
    }

    public String getState()
    {
        return state;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getDescription()
    {
        return description;
    }

    public String getRating()
    {
        return rating;
    }
}
