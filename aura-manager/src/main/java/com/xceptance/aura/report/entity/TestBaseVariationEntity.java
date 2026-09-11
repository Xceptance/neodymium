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
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * JPA entity representing a unique Test Base variation / signature.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Entity
@Table(name = "test_base_variation", indexes = {
    @Index(name = "idx_tb_class", columnList = "test_class_name"),
    @Index(name = "idx_tb_location", columnList = "location"),
    @Index(name = "idx_tb_browser", columnList = "browser")
})
public class TestBaseVariationEntity
{
    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id; // SHA256 or string hash of testClass|dataSet|location|browser

    @Column(name = "test_class_name", nullable = false, length = 150)
    private String testClassName;

    @Column(name = "test_method_name", length = 150)
    private String testMethodName;

    @Column(name = "data_set_label", length = 150)
    private String dataSetLabel;

    @Column(name = "area_tag", length = 80)
    private String areaTag;

    @Column(name = "location", length = 20)
    private String location;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "total_executions_count")
    private Integer totalExecutionsCount = 0;

    @Column(name = "last_status", length = 40)
    private String lastStatus;

    @Column(name = "last_executed_at")
    private Long lastExecutedAt;

    @Lob
    @Column(name = "history_links", columnDefinition = "CLOB")
    private String historyLinks;

    public TestBaseVariationEntity()
    {
    }

    public TestBaseVariationEntity(final String id, final String testClassName, final String dataSetLabel, final String areaTag, final String location, final String browser)
    {
        this(id, testClassName, null, dataSetLabel, areaTag, location, browser);
    }

    public TestBaseVariationEntity(final String id, final String testClassName, final String testMethodName, final String dataSetLabel, final String areaTag, final String location, final String browser)
    {
        this.id = id;
        this.testClassName = testClassName;
        this.testMethodName = (testMethodName != null && !testMethodName.isBlank()) ? testMethodName.trim() : null;
        this.dataSetLabel = (dataSetLabel != null && !dataSetLabel.isBlank()) ? dataSetLabel.trim() : "Default";
        this.areaTag = areaTag;
        this.location = location;
        this.browser = browser;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getTestClassName()
    {
        return testClassName;
    }

    public void setTestClassName(final String testClassName)
    {
        this.testClassName = testClassName;
    }

    public String getTestMethodName()
    {
        return testMethodName != null ? testMethodName : "";
    }

    public void setTestMethodName(final String testMethodName)
    {
        this.testMethodName = (testMethodName != null && !testMethodName.isBlank()) ? testMethodName.trim() : null;
    }

    public String getDataSetLabel()
    {
        return (dataSetLabel != null && !dataSetLabel.isBlank()) ? dataSetLabel : "Default";
    }

    public void setDataSetLabel(final String dataSetLabel)
    {
        this.dataSetLabel = (dataSetLabel != null && !dataSetLabel.isBlank()) ? dataSetLabel.trim() : "Default";
    }

    public String getAreaTag()
    {
        return areaTag;
    }

    public void setAreaTag(final String areaTag)
    {
        this.areaTag = areaTag;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(final String location)
    {
        this.location = location;
    }

    public String getBrowser()
    {
        return browser;
    }

    public void setBrowser(final String browser)
    {
        this.browser = browser;
    }

    public Integer getTotalExecutionsCount()
    {
        return totalExecutionsCount;
    }

    public void setTotalExecutionsCount(final Integer totalExecutionsCount)
    {
        this.totalExecutionsCount = totalExecutionsCount;
    }

    public String getLastStatus()
    {
        return lastStatus;
    }

    public void setLastStatus(final String lastStatus)
    {
        this.lastStatus = lastStatus;
    }

    public Long getLastExecutedAt()
    {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(final Long lastExecutedAt)
    {
        this.lastExecutedAt = lastExecutedAt;
    }

    public String getHistoryLinks()
    {
        return historyLinks;
    }

    public void setHistoryLinks(final String historyLinks)
    {
        this.historyLinks = historyLinks;
    }
}
