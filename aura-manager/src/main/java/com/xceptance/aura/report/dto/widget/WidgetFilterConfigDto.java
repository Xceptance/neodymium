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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data transfer object encapsulating dedicated run filter criteria for a dashboard widget.
 * When enabled, the widget is evaluated against test runs and variations matching these filters,
 * independent of global dashboard filters.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WidgetFilterConfigDto
{
    private final Set<String> browsers;
    private final Set<String> excludeBrowsers;
    private final Set<String> locales;
    private final Set<String> excludeLocales;
    private final Set<String> envs;
    private final Set<String> excludeEnvs;
    private final Set<String> areas;
    private final Set<String> excludeAreas;
    private final Set<String> tags;
    private final Set<String> excludeTags;
    private final Map<String, Set<String>> tagsByType;
    private final Map<String, Set<String>> excludeTagsByType;
    private final Integer limit;
    private final String startDate;
    private final String endDate;

    public WidgetFilterConfigDto()
    {
        this(
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            null, null, null
        );
    }

    public WidgetFilterConfigDto(
        final Set<String> browsers,
        final Set<String> excludeBrowsers,
        final Set<String> locales,
        final Set<String> excludeLocales,
        final Set<String> envs,
        final Set<String> excludeEnvs,
        final Set<String> areas,
        final Set<String> excludeAreas,
        final Set<String> tags,
        final Set<String> excludeTags,
        final Map<String, Set<String>> tagsByType,
        final Map<String, Set<String>> excludeTagsByType)
    {
        this(
            browsers, excludeBrowsers,
            locales, excludeLocales,
            envs, excludeEnvs,
            areas, excludeAreas,
            tags, excludeTags,
            tagsByType, excludeTagsByType,
            null, null, null
        );
    }

    @JsonCreator
    public WidgetFilterConfigDto(
        @JsonProperty("browsers") final Set<String> browsers,
        @JsonProperty("excludeBrowsers") final Set<String> excludeBrowsers,
        @JsonProperty("locales") final Set<String> locales,
        @JsonProperty("excludeLocales") final Set<String> excludeLocales,
        @JsonProperty("envs") final Set<String> envs,
        @JsonProperty("excludeEnvs") final Set<String> excludeEnvs,
        @JsonProperty("areas") final Set<String> areas,
        @JsonProperty("excludeAreas") final Set<String> excludeAreas,
        @JsonProperty("tags") final Set<String> tags,
        @JsonProperty("excludeTags") final Set<String> excludeTags,
        @JsonProperty("tagsByType") final Map<String, Set<String>> tagsByType,
        @JsonProperty("excludeTagsByType") final Map<String, Set<String>> excludeTagsByType,
        @JsonProperty("limit") final Integer limit,
        @JsonProperty("startDate") final String startDate,
        @JsonProperty("endDate") final String endDate)
    {
        this.browsers = (browsers != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(browsers)) : Collections.emptySet();
        this.excludeBrowsers = (excludeBrowsers != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludeBrowsers)) : Collections.emptySet();
        this.locales = (locales != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(locales)) : Collections.emptySet();
        this.excludeLocales = (excludeLocales != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludeLocales)) : Collections.emptySet();
        this.envs = (envs != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(envs)) : Collections.emptySet();
        this.excludeEnvs = (excludeEnvs != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludeEnvs)) : Collections.emptySet();
        this.areas = (areas != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(areas)) : Collections.emptySet();
        this.excludeAreas = (excludeAreas != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludeAreas)) : Collections.emptySet();
        this.tags = (tags != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(tags)) : Collections.emptySet();
        this.excludeTags = (excludeTags != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludeTags)) : Collections.emptySet();

        if (tagsByType != null && !tagsByType.isEmpty())
        {
            final Map<String, Set<String>> copy = new LinkedHashMap<>();
            tagsByType.forEach((k, v) -> {
                if (k != null && v != null && !v.isEmpty())
                {
                    copy.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v)));
                }
            });
            this.tagsByType = Collections.unmodifiableMap(copy);
        }
        else
        {
            this.tagsByType = Collections.emptyMap();
        }

        if (excludeTagsByType != null && !excludeTagsByType.isEmpty())
        {
            final Map<String, Set<String>> copy = new LinkedHashMap<>();
            excludeTagsByType.forEach((k, v) -> {
                if (k != null && v != null && !v.isEmpty())
                {
                    copy.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v)));
                }
            });
            this.excludeTagsByType = Collections.unmodifiableMap(copy);
        }
        else
        {
            this.excludeTagsByType = Collections.emptyMap();
        }

        this.limit = (limit != null && limit > 0) ? limit : null;
        this.startDate = (startDate != null && !startDate.isBlank()) ? startDate.trim() : null;
        this.endDate = (endDate != null && !endDate.isBlank()) ? endDate.trim() : null;
    }

    @JsonIgnore
    public boolean hasAnyFilters()
    {
        return (limit != null && limit > 0)
            || (startDate != null && !startDate.isBlank())
            || (endDate != null && !endDate.isBlank())
            || !browsers.isEmpty() || !excludeBrowsers.isEmpty()
            || !locales.isEmpty() || !excludeLocales.isEmpty()
            || !envs.isEmpty() || !excludeEnvs.isEmpty()
            || !areas.isEmpty() || !excludeAreas.isEmpty()
            || !tags.isEmpty() || !excludeTags.isEmpty()
            || !tagsByType.isEmpty() || !excludeTagsByType.isEmpty();
    }

    @JsonIgnore
    public boolean isEmpty()
    {
        return !hasAnyFilters();
    }

    public Set<String> getBrowsers()
    {
        return browsers;
    }

    public Set<String> getExcludeBrowsers()
    {
        return excludeBrowsers;
    }

    public Set<String> getLocales()
    {
        return locales;
    }

    public Set<String> getExcludeLocales()
    {
        return excludeLocales;
    }

    public Set<String> getEnvs()
    {
        return envs;
    }

    public Set<String> getExcludeEnvs()
    {
        return excludeEnvs;
    }

    public Set<String> getAreas()
    {
        return areas;
    }

    public Set<String> getExcludeAreas()
    {
        return excludeAreas;
    }

    public Set<String> getTags()
    {
        return tags;
    }

    public Set<String> getExcludeTags()
    {
        return excludeTags;
    }

    public Map<String, Set<String>> getTagsByType()
    {
        return tagsByType;
    }

    public Map<String, Set<String>> getExcludeTagsByType()
    {
        return excludeTagsByType;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public String getStartDate()
    {
        return startDate;
    }

    public String getEndDate()
    {
        return endDate;
    }

    @JsonIgnore
    public Long getFromTimestampMs()
    {
        if (startDate == null || startDate.isBlank())
        {
            return null;
        }
        try
        {
            return LocalDate.parse(startDate.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        catch (final DateTimeParseException e)
        {
            return null;
        }
    }

    @JsonIgnore
    public Long getToTimestampMs()
    {
        if (endDate == null || endDate.isBlank())
        {
            return null;
        }
        try
        {
            return LocalDate.parse(endDate.trim()).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        catch (final DateTimeParseException e)
        {
            return null;
        }
    }

    /**
     * Builds a human-readable summary of active filters for tooltips and badges.
     *
     * @return readable summary string
     */
    @JsonIgnore
    public String getFilterSummary()
    {
        if (!hasAnyFilters())
        {
            return "No active filters";
        }
        final List<String> parts = new ArrayList<>();
        if (limit != null && limit > 0)
        {
            parts.add("Limit: Last " + limit + " Runs");
        }
        if ((startDate != null && !startDate.isBlank()) || (endDate != null && !endDate.isBlank()))
        {
            if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank())
            {
                parts.add("Date: " + startDate + " to " + endDate);
            }
            else if (startDate != null && !startDate.isBlank())
            {
                parts.add("Date: From " + startDate);
            }
            else
            {
                parts.add("Date: Until " + endDate);
            }
        }
        if (!browsers.isEmpty())
        {
            parts.add("Browsers: " + String.join(", ", browsers));
        }
        if (!excludeBrowsers.isEmpty())
        {
            parts.add("Excl. Browsers: " + String.join(", ", excludeBrowsers));
        }
        if (!locales.isEmpty())
        {
            parts.add("Locales: " + String.join(", ", locales));
        }
        if (!excludeLocales.isEmpty())
        {
            parts.add("Excl. Locales: " + String.join(", ", excludeLocales));
        }
        if (!envs.isEmpty())
        {
            parts.add("Envs: " + String.join(", ", envs));
        }
        if (!excludeEnvs.isEmpty())
        {
            parts.add("Excl. Envs: " + String.join(", ", excludeEnvs));
        }
        if (!areas.isEmpty())
        {
            parts.add("Areas: " + String.join(", ", areas));
        }
        if (!excludeAreas.isEmpty())
        {
            parts.add("Excl. Areas: " + String.join(", ", excludeAreas));
        }
        if (!tags.isEmpty())
        {
            parts.add("Tags: " + String.join(", ", tags));
        }
        if (!excludeTags.isEmpty())
        {
            parts.add("Excl. Tags: " + String.join(", ", excludeTags));
        }
        tagsByType.forEach((k, v) -> {
            if (!v.isEmpty())
            {
                parts.add(k + ": " + String.join(", ", v));
            }
        });
        excludeTagsByType.forEach((k, v) -> {
            if (!v.isEmpty())
            {
                parts.add("Excl. " + k + ": " + String.join(", ", v));
            }
        });
        return String.join(" | ", parts);
    }

    /**
     * Returns an ordered list of structured filter chips for display on the widget card.
     *
     * @return list of active filter chips
     */
    @JsonIgnore
    public List<WidgetFilterChipDto> getFilterChips()
    {
        if (!hasAnyFilters())
        {
            return Collections.emptyList();
        }
        final List<WidgetFilterChipDto> chips = new ArrayList<>();
        if (limit != null && limit > 0)
        {
            chips.add(new WidgetFilterChipDto("limit", "history", "Last " + limit + " Runs", false));
        }
        if ((startDate != null && !startDate.isBlank()) || (endDate != null && !endDate.isBlank()))
        {
            final String dateLabel;
            if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank())
            {
                dateLabel = startDate + " \u2013 " + endDate;
            }
            else if (startDate != null && !startDate.isBlank())
            {
                dateLabel = "From " + startDate;
            }
            else
            {
                dateLabel = "Until " + endDate;
            }
            chips.add(new WidgetFilterChipDto("date", "calendar_month", dateLabel, false));
        }
        for (final String b : browsers)
        {
            chips.add(new WidgetFilterChipDto("browser", "web", b, false));
        }
        for (final String eb : excludeBrowsers)
        {
            chips.add(new WidgetFilterChipDto("browser", "block", "Excl. " + eb, true));
        }
        for (final String l : locales)
        {
            chips.add(new WidgetFilterChipDto("locale", "language", l, false));
        }
        for (final String el : excludeLocales)
        {
            chips.add(new WidgetFilterChipDto("locale", "block", "Excl. " + el, true));
        }
        for (final String e : envs)
        {
            chips.add(new WidgetFilterChipDto("env", "dns", e, false));
        }
        for (final String ee : excludeEnvs)
        {
            chips.add(new WidgetFilterChipDto("env", "block", "Excl. " + ee, true));
        }
        for (final String a : areas)
        {
            chips.add(new WidgetFilterChipDto("area", "category", a, false));
        }
        for (final String ea : excludeAreas)
        {
            chips.add(new WidgetFilterChipDto("area", "block", "Excl. " + ea, true));
        }
        for (final String t : tags)
        {
            chips.add(new WidgetFilterChipDto("tag", "sell", t, false));
        }
        for (final String et : excludeTags)
        {
            chips.add(new WidgetFilterChipDto("tag", "block", "Excl. " + et, true));
        }
        tagsByType.forEach((k, vals) -> {
            for (final String v : vals)
            {
                chips.add(new WidgetFilterChipDto("tag", "sell", k + ": " + v, false));
            }
        });
        excludeTagsByType.forEach((k, vals) -> {
            for (final String ev : vals)
            {
                chips.add(new WidgetFilterChipDto("tag", "block", "Excl. " + k + ": " + ev, true));
            }
        });
        return Collections.unmodifiableList(chips);
    }

    /**
     * Returns the total count of active filter criteria.
     *
     * @return active filter count
     */
    @JsonIgnore
    public int getActiveFiltersCount()
    {
        int count = browsers.size() + excludeBrowsers.size()
            + locales.size() + excludeLocales.size()
            + envs.size() + excludeEnvs.size()
            + areas.size() + excludeAreas.size()
            + tags.size() + excludeTags.size();
        for (final Set<String> vals : tagsByType.values())
        {
            count += vals.size();
        }
        for (final Set<String> vals : excludeTagsByType.values())
        {
            count += vals.size();
        }
        if (limit != null && limit > 0)
        {
            count++;
        }
        if ((startDate != null && !startDate.isBlank()) || (endDate != null && !endDate.isBlank()))
        {
            count++;
        }
        return count;
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
        final WidgetFilterConfigDto that = (WidgetFilterConfigDto) o;
        return Objects.equals(browsers, that.browsers)
            && Objects.equals(excludeBrowsers, that.excludeBrowsers)
            && Objects.equals(locales, that.locales)
            && Objects.equals(excludeLocales, that.excludeLocales)
            && Objects.equals(envs, that.envs)
            && Objects.equals(excludeEnvs, that.excludeEnvs)
            && Objects.equals(areas, that.areas)
            && Objects.equals(excludeAreas, that.excludeAreas)
            && Objects.equals(tags, that.tags)
            && Objects.equals(excludeTags, that.excludeTags)
            && Objects.equals(tagsByType, that.tagsByType)
            && Objects.equals(excludeTagsByType, that.excludeTagsByType)
            && Objects.equals(limit, that.limit)
            && Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(browsers, excludeBrowsers, locales, excludeLocales, envs, excludeEnvs, areas, excludeAreas, tags, excludeTags, tagsByType, excludeTagsByType, limit, startDate, endDate);
    }
}
