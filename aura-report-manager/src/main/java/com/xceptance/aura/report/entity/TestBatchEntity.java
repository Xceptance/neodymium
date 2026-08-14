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
import jakarta.persistence.Table;

/**
 * JPA entity representing a Test Batch definition.
 *
 * @author Xceptance GmbH 2026
 */
@Entity
@Table(name = "test_batch")
public class TestBatchEntity
{
    @Id
    @Column(name = "batch_name", nullable = false, length = 255)
    private String batchName;

    @Column(name = "environment", length = 255)
    private String environment;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "locales_csv", length = 500)
    private String localesCsv;

    @Column(name = "browsers_csv", length = 500)
    private String browsersCsv;

    @Column(name = "latest_run_id", length = 80)
    private String latestRunId;

    public TestBatchEntity()
    {
    }

    public TestBatchEntity(final String batchName, final String environment, final String description, final String localesCsv, final String browsersCsv, final String latestRunId)
    {
        this.batchName = batchName;
        this.environment = environment;
        this.description = description;
        this.localesCsv = localesCsv;
        this.browsersCsv = browsersCsv;
        this.latestRunId = latestRunId;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public void setBatchName(final String batchName)
    {
        this.batchName = batchName;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(final String environment)
    {
        this.environment = environment;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public String getLocalesCsv()
    {
        return localesCsv;
    }

    public void setLocalesCsv(final String localesCsv)
    {
        this.localesCsv = localesCsv;
    }

    public String getBrowsersCsv()
    {
        return browsersCsv;
    }

    public void setBrowsersCsv(final String browsersCsv)
    {
        this.browsersCsv = browsersCsv;
    }

    public String getLatestRunId()
    {
        return latestRunId;
    }

    public void setLatestRunId(final String latestRunId)
    {
        this.latestRunId = latestRunId;
    }
}
