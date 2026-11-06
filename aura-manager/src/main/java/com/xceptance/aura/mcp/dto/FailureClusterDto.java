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

import java.util.List;

/**
 * Representation of a grouped failure cluster.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailureClusterDto
{
    private String category;

    private int count;

    private String sampleMessage;

    private List<String> affectedSuites;

    private String representativeFailure;

    public FailureClusterDto()
    {
    }

    public FailureClusterDto(final String category, final int count, final String sampleMessage,
                             final List<String> affectedSuites, final String representativeFailure)
    {
        this.category = category;
        this.count = count;
        this.sampleMessage = sampleMessage;
        this.affectedSuites = affectedSuites;
        this.representativeFailure = representativeFailure;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(final String category)
    {
        this.category = category;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(final int count)
    {
        this.count = count;
    }

    public String getSampleMessage()
    {
        return sampleMessage;
    }

    public void setSampleMessage(final String sampleMessage)
    {
        this.sampleMessage = sampleMessage;
    }

    public List<String> getAffectedSuites()
    {
        return affectedSuites;
    }

    public void setAffectedSuites(final List<String> affectedSuites)
    {
        this.affectedSuites = affectedSuites;
    }

    public String getRepresentativeFailure()
    {
        return representativeFailure;
    }

    public void setRepresentativeFailure(final String representativeFailure)
    {
        this.representativeFailure = representativeFailure;
    }
}
