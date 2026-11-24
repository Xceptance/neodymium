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
package com.xceptance.aura.report.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key identifier for {@link TestRunTagEntity}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class TestRunTagId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String runId;
    private String tagKey;
    private String tagValue;

    public TestRunTagId()
    {
    }

    public TestRunTagId(final String runId, final String tagKey, final String tagValue)
    {
        this.runId = runId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
    }

    public String getRunId()
    {
        return runId;
    }

    public void setRunId(final String runId)
    {
        this.runId = runId;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public void setTagKey(final String tagKey)
    {
        this.tagKey = tagKey;
    }

    public String getTagValue()
    {
        return tagValue;
    }

    public void setTagValue(final String tagValue)
    {
        this.tagValue = tagValue;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        final TestRunTagId other = (TestRunTagId) obj;
        return Objects.equals(runId, other.runId)
            && Objects.equals(tagKey, other.tagKey)
            && Objects.equals(tagValue, other.tagValue);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(runId, tagKey, tagValue);
    }
}
