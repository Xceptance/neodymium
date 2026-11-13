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

import com.xceptance.aura.report.service.AuraReportDataService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JPA entity representing a unique Test Base variation / signature.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
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

    @Column(name = "sub_area_tag", length = 80)
    private String subAreaTag;

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

    @Column(name = "healed")
    private Boolean healed = false;

    @Column(name = "execution_mode", length = 40)
    private String executionMode;

    @Column(name = "last_run_id", length = 120)
    private String lastRunId;

    @Column(name = "last_environment", length = 80)
    private String lastEnvironment;

    @Column(name = "last_batch", length = 120)
    private String lastBatch;

    @Column(name = "flaky")
    private Boolean flaky = false;

    @Column(name = "flaky_failure_count")
    private Integer flakyFailureCount = 0;

    @Column(name = "flaky_total_runs")
    private Integer flakyTotalRuns = 0;

    @Transient
    private String flakyReason;

    @Transient
    private Map<String, String> uniquenessProperties = new LinkedHashMap<>();

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
        this.areaTag = cleanAreaValue(areaTag);
        this.location = AuraReportDataService.normalizeLocation(location);
        this.browser = AuraReportDataService.normalizeBrowser(browser);
    }

    private static String cleanAreaValue(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "Default";
        }
        String trimmed = raw.trim();
        while (trimmed.startsWith("@"))
        {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()
            || "Browsing (default)".equalsIgnoreCase(trimmed)
            || "General".equalsIgnoreCase(trimmed)
            || "Default".equalsIgnoreCase(trimmed))
        {
            return "Default";
        }
        return trimmed;
    }

    private static String cleanSubAreaValue(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return null;
        }
        String trimmed = raw.trim();
        while (trimmed.startsWith("@"))
        {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
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
        return cleanAreaValue(this.areaTag);
    }

    public void setAreaTag(final String areaTag)
    {
        this.areaTag = cleanAreaValue(areaTag);
    }

    public String getSubAreaTag()
    {
        return cleanSubAreaValue(this.subAreaTag);
    }

    public void setSubAreaTag(final String subAreaTag)
    {
        this.subAreaTag = cleanSubAreaValue(subAreaTag);
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(final String location)
    {
        this.location = AuraReportDataService.normalizeLocation(location);
    }

    public String getBrowser()
    {
        return browser;
    }

    public void setBrowser(final String browser)
    {
        this.browser = AuraReportDataService.normalizeBrowser(browser);
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

    public Boolean isHealed()
    {
        return healed != null && healed;
    }

    public Boolean getHealed()
    {
        return healed;
    }

    public void setHealed(final Boolean healed)
    {
        this.healed = healed != null ? healed : false;
    }

    public Boolean isFlaky()
    {
        return flaky != null && flaky;
    }

    public Boolean getFlaky()
    {
        return flaky != null ? flaky : false;
    }

    public void setFlaky(final Boolean flaky)
    {
        this.flaky = flaky != null ? flaky : false;
    }

    public Integer getFlakyFailureCount()
    {
        return flakyFailureCount != null ? flakyFailureCount : 0;
    }

    public void setFlakyFailureCount(final Integer flakyFailureCount)
    {
        this.flakyFailureCount = flakyFailureCount != null ? flakyFailureCount : 0;
    }

    public Integer getFlakyTotalRuns()
    {
        return flakyTotalRuns != null ? flakyTotalRuns : 0;
    }

    public void setFlakyTotalRuns(final Integer flakyTotalRuns)
    {
        this.flakyTotalRuns = flakyTotalRuns != null ? flakyTotalRuns : 0;
    }

    public String getFlakyReason()
    {
        return flakyReason;
    }

    public void setFlakyReason(final String flakyReason)
    {
        this.flakyReason = flakyReason;
    }

    public String getExecutionMode()
    {
        return executionMode;
    }

    public void setExecutionMode(final String executionMode)
    {
        this.executionMode = executionMode;
    }

    public String getLastRunId()
    {
        return lastRunId;
    }

    public void setLastRunId(final String lastRunId)
    {
        this.lastRunId = lastRunId;
    }

    public String getLastEnvironment()
    {
        return lastEnvironment;
    }

    public void setLastEnvironment(final String lastEnvironment)
    {
        this.lastEnvironment = lastEnvironment;
    }

    public String getLastBatch()
    {
        return lastBatch;
    }

    public void setLastBatch(final String lastBatch)
    {
        this.lastBatch = lastBatch;
    }

    /**
     * Resolves the display execution mode string (AI, HEALED, RECORDING, or JAVA).
     *
     * @return display mode string
     */
    public String getDisplayMode()
    {
        final String rawMode = this.executionMode != null ? this.executionMode.trim() : "";
        if ("AI".equalsIgnoreCase(rawMode) || "LLM_RECORDING".equalsIgnoreCase(rawMode) || "LLM_ONLY".equalsIgnoreCase(rawMode) || "FORCE_RECORDING".equalsIgnoreCase(rawMode))
        {
            return "AI";
        }
        if ("HEALED".equalsIgnoreCase(rawMode))
        {
            return "HEALED";
        }
        if ("RECORDING".equalsIgnoreCase(rawMode) || "PLAYBOOK".equalsIgnoreCase(rawMode) || "REPLAY_STRICT".equalsIgnoreCase(rawMode) || "REPLAY_WITH_HEALING".equalsIgnoreCase(rawMode))
        {
            return "RECORDING";
        }
        if ("JAVA".equalsIgnoreCase(rawMode))
        {
            return "JAVA";
        }
        if (!rawMode.isEmpty())
        {
            return rawMode.toUpperCase();
        }
        if (isHealed())
        {
            return "HEALED";
        }
        if (testClassName != null && (testClassName.contains("_yaml_") || testClassName.contains("_yml_") || testClassName.endsWith("PlaybookTest")))
        {
            return "RECORDING";
        }
        return "JAVA";
    }

    public Map<String, String> getIdentityProperties()
    {
        return uniquenessProperties != null ? uniquenessProperties : Collections.emptyMap();
    }

    public void setIdentityProperties(final Map<String, String> identityProperties)
    {
        this.uniquenessProperties = identityProperties;
    }

    public String getIdentityProperty(final String key)
    {
        if (uniquenessProperties == null || key == null)
        {
            return null;
        }
        if (uniquenessProperties.containsKey(key))
        {
            return uniquenessProperties.get(key);
        }
        for (final Map.Entry<String, String> e : uniquenessProperties.entrySet())
        {
            if (key.equalsIgnoreCase(e.getKey()))
            {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * @deprecated Use {@link #getIdentityProperties()} instead.
     */
    @Deprecated
    public Map<String, String> getUniquenessProperties()
    {
        return getIdentityProperties();
    }

    /**
     * @deprecated Use {@link #setIdentityProperties(Map)} instead.
     */
    @Deprecated
    public void setUniquenessProperties(final Map<String, String> uniquenessProperties)
    {
        setIdentityProperties(uniquenessProperties);
    }

    /**
     * @deprecated Use {@link #getIdentityProperty(String)} instead.
     */
    @Deprecated
    public String getUniquenessProperty(final String key)
    {
        return getIdentityProperty(key);
    }

    public String getTagsJson()
    {
        final StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (lastBatch != null && !lastBatch.isBlank())
        {
            first = false;
            sb.append("\"batch\":\"").append(escapeJson(lastBatch.trim())).append("\"");
        }
        if (uniquenessProperties != null)
        {
            for (final Map.Entry<String, String> e : uniquenessProperties.entrySet())
            {
                if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null)
                {
                    if (!first)
                    {
                        sb.append(",");
                    }
                    first = false;
                    sb.append("\"").append(escapeJson(e.getKey().trim().toLowerCase(Locale.ROOT))).append("\":\"")
                      .append(escapeJson(e.getValue().trim())).append("\"");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(final String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
