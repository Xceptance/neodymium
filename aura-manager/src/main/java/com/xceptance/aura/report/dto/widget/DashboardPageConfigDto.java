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
package com.xceptance.aura.report.dto.widget;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Data transfer object encapsulating the persistent configuration of a custom dashboard page.
 * Each page has a user-defined name and an independent set of configured dashboard widgets.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DashboardPageConfigDto
{
    private final String id;
    private final String name;
    private final List<DashboardWidgetConfigDto> widgets;
    private final int sortOrder;
    private final long createdTimestamp;
    private final List<String> executionIdentityDimensions;
    private final WidgetFilterConfigDto runFilters;
    private final String parentPageId;
    private final boolean isGroupParent;
    private final String groupType;
    private final String groupValue;
    private final WidgetPointScope groupCardScope;
    private final List<DashboardPageConfigDto> subPages;

    public DashboardPageConfigDto()
    {
        this(UUID.randomUUID().toString(), "New Page", Collections.emptyList(), 0, System.currentTimeMillis(), Collections.singletonList("browser"), null);
    }

    public DashboardPageConfigDto(final String name)
    {
        this(UUID.randomUUID().toString(), name, Collections.emptyList(), 0, System.currentTimeMillis(), Collections.singletonList("browser"), null);
    }

    public DashboardPageConfigDto(
        final String id,
        final String name,
        final List<DashboardWidgetConfigDto> widgets,
        final int sortOrder,
        final long createdTimestamp)
    {
        this(id, name, widgets, sortOrder, createdTimestamp, Collections.singletonList("browser"), null);
    }

    public DashboardPageConfigDto(
        final String id,
        final String name,
        final List<DashboardWidgetConfigDto> widgets,
        final int sortOrder,
        final long createdTimestamp,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters)
    {
        this(id, name, widgets, sortOrder, createdTimestamp, executionIdentityDimensions, runFilters, null, false, null, null, WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList());
    }

    public DashboardPageConfigDto(
        final String id,
        final String name,
        final List<DashboardWidgetConfigDto> widgets,
        final int sortOrder,
        final long createdTimestamp,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters,
        final String parentPageId,
        final Boolean isGroupParent,
        final String groupType,
        final String groupValue,
        final WidgetPointScope groupCardScope)
    {
        this(id, name, widgets, sortOrder, createdTimestamp, executionIdentityDimensions, runFilters, parentPageId, isGroupParent, groupType, groupValue, groupCardScope, Collections.emptyList());
    }

    @JsonCreator
    public DashboardPageConfigDto(
        @JsonProperty("id") final String id,
        @JsonProperty("name") final String name,
        @JsonProperty("widgets") final List<DashboardWidgetConfigDto> widgets,
        @JsonProperty("sortOrder") final int sortOrder,
        @JsonProperty("createdTimestamp") final long createdTimestamp,
        @JsonProperty("executionIdentityDimensions") final List<String> executionIdentityDimensions,
        @JsonProperty("runFilters") final WidgetFilterConfigDto runFilters,
        @JsonProperty("parentPageId") final String parentPageId,
        @JsonProperty("isGroupParent") @JsonAlias("groupParent") final Boolean isGroupParent,
        @JsonProperty("groupType") final String groupType,
        @JsonProperty("groupValue") final String groupValue,
        @JsonProperty("groupCardScope") final WidgetPointScope groupCardScope,
        @JsonProperty("subPages") final List<DashboardPageConfigDto> subPages)
    {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name.trim() : "Custom Dashboard";
        this.widgets = (widgets != null)
            ? Collections.unmodifiableList(new ArrayList<>(widgets))
            : Collections.emptyList();
        this.sortOrder = sortOrder;
        this.createdTimestamp = (createdTimestamp > 0) ? createdTimestamp : System.currentTimeMillis();
        this.executionIdentityDimensions = (executionIdentityDimensions != null && !executionIdentityDimensions.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(executionIdentityDimensions))
            : Collections.singletonList("browser");
        this.runFilters = runFilters;
        this.parentPageId = (parentPageId != null && !parentPageId.isBlank()) ? parentPageId.trim() : null;
        this.isGroupParent = Boolean.TRUE.equals(isGroupParent);
        this.groupType = (groupType != null && !groupType.isBlank()) ? groupType.trim() : null;
        this.groupValue = (groupValue != null && !groupValue.isBlank()) ? groupValue.trim() : null;
        this.groupCardScope = (groupCardScope != null) ? groupCardScope : WidgetPointScope.LATEST_TEST_STATUS;
        this.subPages = (subPages != null)
            ? Collections.unmodifiableList(new ArrayList<>(subPages))
            : Collections.emptyList();
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public List<DashboardWidgetConfigDto> getWidgets()
    {
        return widgets;
    }

    public int getSortOrder()
    {
        return sortOrder;
    }

    public long getCreatedTimestamp()
    {
        return createdTimestamp;
    }

    public List<String> getExecutionIdentityDimensions()
    {
        return executionIdentityDimensions;
    }

    public List<String> getEffectiveExecutionIdentityDimensions(final List<String> fallback)
    {
        if (executionIdentityDimensions != null && !executionIdentityDimensions.isEmpty())
        {
            return executionIdentityDimensions;
        }
        return (fallback != null && !fallback.isEmpty())
            ? fallback
            : Collections.singletonList("browser");
    }

    public WidgetFilterConfigDto getRunFilters()
    {
        return runFilters;
    }

    public boolean hasRunFilters()
    {
        return runFilters != null && !runFilters.isEmpty();
    }

    public String getParentPageId()
    {
        return parentPageId;
    }

    @JsonProperty("isGroupParent")
    public boolean isGroupParent()
    {
        return isGroupParent;
    }

    public String getGroupType()
    {
        return groupType;
    }

    public String getGroupValue()
    {
        return groupValue;
    }

    public WidgetPointScope getGroupCardScope()
    {
        return (groupCardScope != null) ? groupCardScope : WidgetPointScope.LATEST_TEST_STATUS;
    }

    public boolean isSubPage()
    {
        return parentPageId != null && !parentPageId.isBlank();
    }

    public List<DashboardPageConfigDto> getSubPages()
    {
        return subPages;
    }

    public DashboardPageConfigDto withName(final String newName)
    {
        return new DashboardPageConfigDto(this.id, newName, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withWidgets(final List<DashboardWidgetConfigDto> newWidgets)
    {
        return new DashboardPageConfigDto(this.id, this.name, newWidgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withSortOrder(final int newSortOrder)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, newSortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withExecutionIdentityDimensions(final List<String> newIdentity)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, newIdentity, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withRunFilters(final WidgetFilterConfigDto newRunFilters)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, newRunFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withParentPageId(final String newParentPageId)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, newParentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withGroupParent(final boolean newIsGroupParent)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, newIsGroupParent, this.groupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withGroupType(final String newGroupType)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, newGroupType, this.groupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withGroupValue(final String newGroupValue)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, newGroupValue, this.groupCardScope, this.subPages);
    }

    public DashboardPageConfigDto withGroupCardScope(final WidgetPointScope newScope)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, newScope, this.subPages);
    }

    public DashboardPageConfigDto withSubPages(final List<DashboardPageConfigDto> newSubPages)
    {
        return new DashboardPageConfigDto(this.id, this.name, this.widgets, this.sortOrder, this.createdTimestamp, this.executionIdentityDimensions, this.runFilters, this.parentPageId, this.isGroupParent, this.groupType, this.groupValue, this.groupCardScope, newSubPages);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final DashboardPageConfigDto that = (DashboardPageConfigDto) o;
        return sortOrder == that.sortOrder
            && createdTimestamp == that.createdTimestamp
            && isGroupParent == that.isGroupParent
            && Objects.equals(id, that.id)
            && Objects.equals(name, that.name)
            && Objects.equals(widgets, that.widgets)
            && Objects.equals(executionIdentityDimensions, that.executionIdentityDimensions)
            && Objects.equals(runFilters, that.runFilters)
            && Objects.equals(parentPageId, that.parentPageId)
            && Objects.equals(groupType, that.groupType)
            && Objects.equals(groupValue, that.groupValue)
            && groupCardScope == that.groupCardScope
            && Objects.equals(subPages, that.subPages);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name, widgets, sortOrder, createdTimestamp, executionIdentityDimensions, runFilters, parentPageId, isGroupParent, groupType, groupValue, groupCardScope, subPages);
    }

    @Override
    public String toString()
    {
        return "DashboardPageConfigDto{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", widgetsCount=" + widgets.size() +
            ", sortOrder=" + sortOrder +
            ", createdTimestamp=" + createdTimestamp +
            ", executionIdentityDimensions=" + executionIdentityDimensions +
            ", hasRunFilters=" + hasRunFilters() +
            ", parentPageId='" + parentPageId + '\'' +
            ", isGroupParent=" + isGroupParent +
            ", groupType='" + groupType + '\'' +
            ", groupValue='" + groupValue + '\'' +
            ", groupCardScope=" + groupCardScope +
            ", subPagesCount=" + subPages.size() +
            '}';
    }
}
