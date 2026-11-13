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
package com.xceptance.aura.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity representing a dimensional tag assigned to a Test Execution Run instance.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Entity
@Table(name = "test_run_tag", indexes = {
    @Index(name = "idx_trt_key_val", columnList = "tag_key, tag_value"),
    @Index(name = "idx_trt_run", columnList = "run_id")
})
@IdClass(TestRunTagId.class)
public class TestRunTagEntity
{
    @Id
    @Column(name = "run_id", nullable = false, length = 80)
    private String runId;

    @Id
    @Column(name = "tag_key", nullable = false, length = 80)
    private String tagKey;

    @Id
    @Column(name = "tag_value", nullable = false, length = 150)
    private String tagValue;

    public TestRunTagEntity()
    {
    }

    public TestRunTagEntity(final String runId, final String tagKey, final String tagValue)
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
}
