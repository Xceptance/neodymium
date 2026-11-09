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
package com.xceptance.aura.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.report.controller.AuraReportViewController;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.ReportSettingEntity;
import com.xceptance.aura.report.repository.ReportSettingRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.neodymium.ai.util.AtomicFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service managing persistent report preferences and dashboard configurations (such as pinned test grouping).
 * Uses H2 database storage backed by local JSON file synchronization for resilience across server restarts.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Service
public class ReportDashboardSettingService
{
    private static final Logger LOG = LoggerFactory.getLogger(ReportDashboardSettingService.class);

    public static final String PINNED_GROUP_BY_KEY = "pinned_group_by";
    public static final String PINNED_INNER_GROUP_BY_KEY = "pinned_inner_group_by";
    public static final String PINNED_INNER_GROUP_BY_PREFIX = "pinned_inner_group_by:";
    public static final String EXECUTION_IDENTITY_DIMENSIONS_KEY = "execution_identity_dimensions";
    public static final String VARIATIONS_APPLY_IDENTITY_KEY = "variations_apply_identity";
    public static final String DASHBOARD_WIDGETS_KEY = "dashboard_widgets_config";
    public static final String CUSTOM_DASHBOARD_PAGES_KEY = "custom_dashboard_pages";
    public static final List<String> DEFAULT_EXECUTION_IDENTITY_DIMENSIONS = List.of("browser");

    /**
     * @deprecated Use {@link #EXECUTION_IDENTITY_DIMENSIONS_KEY} instead.
     */
    @Deprecated
    public static final String UNIQUENESS_DIMENSIONS_KEY = "uniqueness_dimensions";

    /**
     * @deprecated Use {@link #VARIATIONS_APPLY_IDENTITY_KEY} instead.
     */
    @Deprecated
    public static final String VARIATIONS_APPLY_UNIQUENESS_KEY = "variations_apply_uniqueness";

    /**
     * @deprecated Use {@link #DEFAULT_EXECUTION_IDENTITY_DIMENSIONS} instead.
     */
    @Deprecated
    public static final List<String> DEFAULT_UNIQUENESS_DIMENSIONS = DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;

    public static final String FLAKY_MIN_UNKNOWN_FAILURES_KEY = "flaky_min_unknown_failures";
    public static final int DEFAULT_FLAKY_MIN_UNKNOWN_FAILURES = 2;
    public static final String FLAKY_HISTORY_WINDOW_KEY = "flaky_history_window";
    public static final int DEFAULT_FLAKY_HISTORY_WINDOW = 10;
    public static final String FLAKY_MIN_FAILURE_RATE_KEY = "flaky_min_failure_rate";
    public static final double DEFAULT_FLAKY_MIN_FAILURE_RATE = 0.20;

    private final ReportSettingRepository repository;
    private final Path settingsFile;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportDashboardSettingService(
        final ReportSettingRepository repository,
        @Value("${aura.report.storage.settings.file:storage/report-settings.json}") final String settingsFilePath)
    {
        this.repository = repository;
        this.settingsFile = Paths.get(settingsFilePath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    public ReportDashboardSettingService(final ReportSettingRepository repository)
    {
        this(repository, "storage/report-settings.json");
    }

    /**
     * Idempotently expands the setting_value column in the report_setting database table to TEXT
     * to ensure large dashboard layout JSON strings are never truncated.
     */
    @PostConstruct
    public void ensureColumnCapacity()
    {
        if (repository != null)
        {
            try
            {
                repository.alterSettingValueColumnToText();
            }
            catch (final Exception e)
            {
                LOG.debug("Setting value column already TEXT or database does not require alter: {}", e.getMessage());
            }
        }
    }

    /**
     * Retrieves the pinned grouping dimensions string (e.g. "batch" or "batch,environment"),
     * or null if no grouping is currently pinned.
     *
     * @return comma-separated grouping dimensions, or null
     */
    public synchronized String getPinnedGrouping()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(PINNED_GROUP_BY_KEY);
                if (entityOpt.isPresent())
                {
                    final String val = entityOpt.get().getValue();
                    final String normalized = normalizeGrouping(val);
                    if (normalized != null)
                    {
                        return normalized;
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read pinned grouping from database: {}", e.getMessage());
            }
        }

        // Fallback to local settings JSON file
        final Map<String, String> fileSettings = readFileSettings();
        final String fileVal = fileSettings.get(PINNED_GROUP_BY_KEY);
        final String normalized = normalizeGrouping(fileVal);
        if (normalized != null && repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(PINNED_GROUP_BY_KEY, normalized));
            }
            catch (final Exception e)
            {
                LOG.debug("Could not backfill database setting from file: {}", e.getMessage());
            }
        }
        return normalized;
    }

    /**
     * Persists the given grouping string as the pinned default grouping.
     *
     * @param groupBy grouping string (e.g. "batch", "batch,environment")
     */
    public synchronized void setPinnedGrouping(final String groupBy)
    {
        final String normalized = normalizeGrouping(groupBy);
        if (normalized == null)
        {
            clearPinnedGrouping();
            return;
        }

        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(PINNED_GROUP_BY_KEY, normalized));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist pinned grouping to database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        map.put(PINNED_GROUP_BY_KEY, normalized);
        writeFileSettings(map);
    }

    /**
     * Clears the pinned grouping, returning the default dashboard view to un-grouped runs.
     */
    public synchronized void clearPinnedGrouping()
    {
        if (repository != null)
        {
            try
            {
                repository.deleteById(PINNED_GROUP_BY_KEY);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to delete pinned grouping from database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        if (map.containsKey(PINNED_GROUP_BY_KEY))
        {
            map.remove(PINNED_GROUP_BY_KEY);
            writeFileSettings(map);
        }
    }

    /**
     * Toggles the pinned state for the given grouping:
     * if already pinned, it unpins; otherwise, it pins it.
     *
     * @param groupBy grouping string to toggle
     * @return the new pinned grouping string, or null if unpinned
     */
    public synchronized String togglePinnedGrouping(final String groupBy)
    {
        if (isGroupingPinned(groupBy))
        {
            clearPinnedGrouping();
            return null;
        }
        setPinnedGrouping(groupBy);
        return getPinnedGrouping();
    }

    /**
     * Checks whether the given grouping matches the active pinned grouping.
     * Dimension ordering is canonicalized so order does not cause false negatives.
     *
     * @param groupBy grouping string to check
     * @return true if currently pinned to these criteria, false otherwise
     */
    public boolean isGroupingPinned(final String groupBy)
    {
        final String pinned = getPinnedGrouping();
        if (pinned == null || pinned.isBlank())
        {
            return false;
        }

        final Set<String> pinnedDims = new LinkedHashSet<>(AuraReportViewController.parseGroupDimensions(pinned));
        final Set<String> currentDims = new LinkedHashSet<>(AuraReportViewController.parseGroupDimensions(groupBy));

        return !pinnedDims.isEmpty() && pinnedDims.equals(currentDims);
    }

    /**
     * Retrieves the pinned inner grouping for the specified parent group type.
     *
     * @param groupType the parent group type (e.g. "environment", "batch")
     * @return pinned inner grouping dimensions string, or null
     */
    public synchronized String getPinnedInnerGrouping(final String groupType)
    {
        final String normGroupType = normalizeGrouping(groupType);
        final String specificKey = (normGroupType != null) ? (PINNED_INNER_GROUP_BY_PREFIX + normGroupType) : null;

        if (repository != null && specificKey != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(specificKey);
                if (entityOpt.isPresent())
                {
                    final String val = entityOpt.get().getValue();
                    final String normalized = normalizeGrouping(val);
                    if (normalized != null && !isRedundantInnerGrouping(normGroupType, normalized))
                    {
                        return normalized;
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read specific pinned inner grouping for '{}' from database: {}", normGroupType, e.getMessage());
            }
        }

        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(PINNED_INNER_GROUP_BY_KEY);
                if (entityOpt.isPresent())
                {
                    final String val = entityOpt.get().getValue();
                    final String normalized = normalizeGrouping(val);
                    if (normalized != null && !isRedundantInnerGrouping(normGroupType, normalized))
                    {
                        return normalized;
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read global pinned inner grouping from database: {}", e.getMessage());
            }
        }

        final Map<String, String> fileSettings = readFileSettings();
        if (specificKey != null && fileSettings.containsKey(specificKey))
        {
            final String normalized = normalizeGrouping(fileSettings.get(specificKey));
            if (normalized != null && !isRedundantInnerGrouping(normGroupType, normalized))
            {
                if (repository != null)
                {
                    try
                    {
                        repository.save(new ReportSettingEntity(specificKey, normalized));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                return normalized;
            }
        }
        if (fileSettings.containsKey(PINNED_INNER_GROUP_BY_KEY))
        {
            final String normalized = normalizeGrouping(fileSettings.get(PINNED_INNER_GROUP_BY_KEY));
            if (normalized != null && !isRedundantInnerGrouping(normGroupType, normalized))
            {
                if (repository != null)
                {
                    try
                    {
                        repository.save(new ReportSettingEntity(PINNED_INNER_GROUP_BY_KEY, normalized));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                return normalized;
            }
        }

        return null;
    }

    /**
     * Persists the given inner grouping for the specified parent group type.
     *
     * @param groupType the parent group type
     * @param groupBy the inner grouping dimensions string
     */
    public synchronized void setPinnedInnerGrouping(final String groupType, final String groupBy)
    {
        final String normGroupType = normalizeGrouping(groupType);
        final String normalized = normalizeGrouping(groupBy);
        if (normalized == null)
        {
            clearPinnedInnerGrouping(groupType);
            return;
        }

        final String targetKey = (normGroupType != null) ? (PINNED_INNER_GROUP_BY_PREFIX + normGroupType) : PINNED_INNER_GROUP_BY_KEY;
        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(targetKey, normalized));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist pinned inner grouping to database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        map.put(targetKey, normalized);
        writeFileSettings(map);
    }

    /**
     * Clears the pinned inner grouping for the specified parent group type.
     *
     * @param groupType the parent group type
     */
    public synchronized void clearPinnedInnerGrouping(final String groupType)
    {
        final String normGroupType = normalizeGrouping(groupType);
        final String targetKey = (normGroupType != null) ? (PINNED_INNER_GROUP_BY_PREFIX + normGroupType) : PINNED_INNER_GROUP_BY_KEY;

        if (repository != null)
        {
            try
            {
                repository.deleteById(targetKey);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to delete pinned inner grouping from database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        map.remove(targetKey);
        writeFileSettings(map);
    }

    /**
     * Toggles the pinned inner grouping state for the given parent group type.
     *
     * @param groupType the parent group type
     * @param groupBy the inner grouping to toggle
     * @return the active pinned inner grouping or null if unpinned
     */
    public synchronized String togglePinnedInnerGrouping(final String groupType, final String groupBy)
    {
        if (isInnerGroupingPinned(groupType, groupBy))
        {
            clearPinnedInnerGrouping(groupType);
            return null;
        }
        setPinnedInnerGrouping(groupType, groupBy);
        return getPinnedInnerGrouping(groupType);
    }

    /**
     * Checks if the given inner grouping is currently pinned for the specified parent group type.
     *
     * @param groupType the parent group type
     * @param groupBy the inner grouping to check
     * @return true if pinned, false otherwise
     */
    public boolean isInnerGroupingPinned(final String groupType, final String groupBy)
    {
        final String pinned = getPinnedInnerGrouping(groupType);
        if (pinned == null || pinned.isBlank())
        {
            return false;
        }

        final Set<String> pinnedDims = new LinkedHashSet<>(AuraReportViewController.parseGroupDimensions(pinned));
        final Set<String> currentDims = new LinkedHashSet<>(AuraReportViewController.parseGroupDimensions(groupBy));

        return !pinnedDims.isEmpty() && pinnedDims.equals(currentDims);
    }

    /**
     * Context-aware check whether grouping is pinned for a given groupType.
     *
     * @param groupType the parent group type (or null/empty/"all" for top-level)
     * @param groupBy grouping string to check
     * @return true if currently pinned
     */
    public boolean isGroupingPinned(final String groupType, final String groupBy)
    {
        if (groupType == null || groupType.isBlank() || "all".equalsIgnoreCase(groupType))
        {
            return isGroupingPinned(groupBy);
        }
        return isInnerGroupingPinned(groupType, groupBy);
    }

    /**
     * Context-aware toggle for pinned grouping.
     *
     * @param groupType the parent group type (or null/empty/"all" for top-level)
     * @param groupBy grouping string to toggle
     * @return new pinned grouping string or null if unpinned
     */
    public synchronized String togglePinnedGrouping(final String groupType, final String groupBy)
    {
        if (groupType == null || groupType.isBlank() || "all".equalsIgnoreCase(groupType))
        {
            return togglePinnedGrouping(groupBy);
        }
        return togglePinnedInnerGrouping(groupType, groupBy);
    }

    /**
     * Context-aware getter for pinned grouping.
     *
     * @param groupType the parent group type (or null/empty/"all" for top-level)
     * @return active pinned grouping or null
     */
    public synchronized String getPinnedGrouping(final String groupType)
    {
        if (groupType == null || groupType.isBlank() || "all".equalsIgnoreCase(groupType))
        {
            return getPinnedGrouping();
        }
        return getPinnedInnerGrouping(groupType);
    }

    private static boolean isRedundantInnerGrouping(final String groupType, final String innerGroupBy)
    {
        if (groupType == null || groupType.isBlank() || innerGroupBy == null || innerGroupBy.isBlank())
        {
            return false;
        }
        final Set<String> parentDims = new LinkedHashSet<>(AuraReportViewController.parseGroupDimensions(groupType));
        final List<String> innerDims = AuraReportViewController.parseGroupDimensions(innerGroupBy);
        return innerDims.stream().anyMatch(parentDims::contains);
    }

    /**
     * Canonicalizes the given grouping string into a comma-separated list of recognized dimensions.
     *
     * @param groupBy raw grouping string
     * @return normalized grouping string, or null if no recognized grouping dimensions exist
     */
    public static String normalizeGrouping(final String groupBy)
    {
        if (groupBy == null || groupBy.isBlank())
        {
            return null;
        }
        final List<String> dims = AuraReportViewController.parseGroupDimensions(groupBy);
        if (dims.isEmpty())
        {
            return null;
        }
        return String.join(",", dims);
    }

    /**
     * Retrieves the active execution identity dimensions list governing Latest Test Status and Trend diagrams.
     * Defaults to ["browser"] if not explicitly configured.
     * Falls back to legacy {@link #UNIQUENESS_DIMENSIONS_KEY} if present.
     *
     * @return list of dimension names defining execution identity
     */
    public synchronized List<String> getExecutionIdentityDimensions()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(EXECUTION_IDENTITY_DIMENSIONS_KEY);
                if (entityOpt.isPresent())
                {
                    final String val = entityOpt.get().getValue();
                    final List<String> parsed = parseExecutionIdentityDimensions(val);
                    if (!parsed.isEmpty())
                    {
                        return parsed;
                    }
                }

                // Fallback to legacy uniqueness key
                final Optional<ReportSettingEntity> legacyOpt = repository.findById(UNIQUENESS_DIMENSIONS_KEY);
                if (legacyOpt.isPresent())
                {
                    final String val = legacyOpt.get().getValue();
                    final List<String> parsed = parseExecutionIdentityDimensions(val);
                    if (!parsed.isEmpty())
                    {
                        return parsed;
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read execution identity dimensions from database: {}", e.getMessage());
            }
        }

        // Fallback to local settings JSON file
        final Map<String, String> fileSettings = readFileSettings();
        String fileVal = fileSettings.get(EXECUTION_IDENTITY_DIMENSIONS_KEY);
        if (fileVal == null || fileVal.isBlank())
        {
            fileVal = fileSettings.get(UNIQUENESS_DIMENSIONS_KEY);
        }

        final List<String> parsed = parseExecutionIdentityDimensions(fileVal);
        if (fileVal != null && !fileVal.isBlank() && repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(EXECUTION_IDENTITY_DIMENSIONS_KEY, String.join(",", parsed)));
            }
            catch (final Exception e)
            {
                LOG.debug("Could not backfill execution identity dimensions setting from file: {}", e.getMessage());
            }
        }
        return parsed;
    }

    /**
     * @deprecated Use {@link #getExecutionIdentityDimensions()} instead.
     */
    @Deprecated
    public synchronized List<String> getUniquenessDimensions()
    {
        return getExecutionIdentityDimensions();
    }

    /**
     * Persists the given execution identity dimensions list to database and local settings file.
     * Also mirrors to legacy {@link #UNIQUENESS_DIMENSIONS_KEY} for backward compatibility.
     *
     * @param dimensions list of dimensions (e.g. ["browser", "locale", "environment"])
     */
    public synchronized void setExecutionIdentityDimensions(final List<String> dimensions)
    {
        final List<String> clean = (dimensions != null) ? parseExecutionIdentityDimensions(String.join(",", dimensions)) : DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
        final List<String> effective = new ArrayList<>(clean);
        if (effective.stream().noneMatch("browser"::equalsIgnoreCase))
        {
            effective.add(0, "browser");
        }
        final String csv = String.join(",", effective);

        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(EXECUTION_IDENTITY_DIMENSIONS_KEY, csv));
                repository.save(new ReportSettingEntity(UNIQUENESS_DIMENSIONS_KEY, csv));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist execution identity dimensions to database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        map.put(EXECUTION_IDENTITY_DIMENSIONS_KEY, csv);
        map.put(UNIQUENESS_DIMENSIONS_KEY, csv);
        writeFileSettings(map);
    }

    /**
     * @deprecated Use {@link #setExecutionIdentityDimensions(List)} instead.
     */
    @Deprecated
    public synchronized void setUniquenessDimensions(final List<String> dimensions)
    {
        setExecutionIdentityDimensions(dimensions);
    }

    /**
     * Toggles the presence of a dimension in the active execution identity dimensions list.
     * If currently active, removes it; if inactive, appends it.
     * Browser is locked as a mandatory base coordinate alongside Test and cannot be removed.
     *
     * @param dimension dimension name to toggle (e.g. "environment", "release")
     * @return updated list of active execution identity dimensions
     */
    public synchronized List<String> toggleExecutionIdentityDimension(final String dimension)
    {
        if (dimension == null || dimension.isBlank())
        {
            return getExecutionIdentityDimensions();
        }
        final String cleanDim = normalizeDimensionName(dimension);
        if ("browser".equalsIgnoreCase(cleanDim))
        {
            return getExecutionIdentityDimensions();
        }
        final List<String> current = new ArrayList<>(getExecutionIdentityDimensions());
        if (current.contains(cleanDim))
        {
            current.remove(cleanDim);
        }
        else
        {
            current.add(cleanDim);
        }
        setExecutionIdentityDimensions(current);
        return Collections.unmodifiableList(current);
    }

    /**
     * @deprecated Use {@link #toggleExecutionIdentityDimension(String)} instead.
     */
    @Deprecated
    public synchronized List<String> toggleUniquenessDimension(final String dimension)
    {
        return toggleExecutionIdentityDimension(dimension);
    }

    /**
     * Checks whether a dimension is currently included in the execution identity definition.
     *
     * @param dimension dimension name (e.g. "browser", "environment")
     * @return true if active, false otherwise
     */
    public boolean isExecutionIdentityDimensionActive(final String dimension)
    {
        if (dimension == null || dimension.isBlank())
        {
            return false;
        }
        final String cleanDim = normalizeDimensionName(dimension);
        return getExecutionIdentityDimensions().contains(cleanDim);
    }

    /**
     * @deprecated Use {@link #isExecutionIdentityDimensionActive(String)} instead.
     */
    @Deprecated
    public boolean isUniquenessDimensionActive(final String dimension)
    {
        return isExecutionIdentityDimensionActive(dimension);
    }

    /**
     * Checks whether the active execution identity dimensions should be applied to split Known Test Base Variations.
     * Defaults to false (canonical variations unaffected).
     *
     * @return true if identity dimensions should split variations, false otherwise
     */
    public synchronized boolean isVariationsApplyIdentity()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(VARIATIONS_APPLY_IDENTITY_KEY);
                if (entityOpt.isPresent())
                {
                    return Boolean.parseBoolean(entityOpt.get().getValue());
                }

                // Fallback to legacy key
                final Optional<ReportSettingEntity> legacyOpt = repository.findById(VARIATIONS_APPLY_UNIQUENESS_KEY);
                if (legacyOpt.isPresent())
                {
                    return Boolean.parseBoolean(legacyOpt.get().getValue());
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read variations apply identity setting from database: {}", e.getMessage());
            }
        }

        final Map<String, String> fileSettings = readFileSettings();
        String fileVal = fileSettings.get(VARIATIONS_APPLY_IDENTITY_KEY);
        if (fileVal == null || fileVal.isBlank())
        {
            fileVal = fileSettings.get(VARIATIONS_APPLY_UNIQUENESS_KEY);
        }

        if (fileVal != null && !fileVal.isBlank())
        {
            final boolean val = Boolean.parseBoolean(fileVal);
            if (repository != null)
            {
                try
                {
                    repository.save(new ReportSettingEntity(VARIATIONS_APPLY_IDENTITY_KEY, String.valueOf(val)));
                }
                catch (final Exception e)
                {
                    LOG.debug("Could not backfill database setting from file: {}", e.getMessage());
                }
            }
            return val;
        }
        return false;
    }

    /**
     * @deprecated Use {@link #isVariationsApplyIdentity()} instead.
     */
    @Deprecated
    public synchronized boolean isVariationsApplyUniqueness()
    {
        return isVariationsApplyIdentity();
    }

    /**
     * Persists the setting controlling whether Known Test Base Variations are split by the active execution identity dimensions.
     *
     * @param apply true to apply identity dimensions to variations, false to keep canonical
     */
    public synchronized void setVariationsApplyIdentity(final boolean apply)
    {
        final String str = String.valueOf(apply);
        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(VARIATIONS_APPLY_IDENTITY_KEY, str));
                repository.save(new ReportSettingEntity(VARIATIONS_APPLY_UNIQUENESS_KEY, str));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist variations apply identity to database: {}", e.getMessage());
            }
        }

        final Map<String, String> map = readFileSettings();
        map.put(VARIATIONS_APPLY_IDENTITY_KEY, str);
        map.put(VARIATIONS_APPLY_UNIQUENESS_KEY, str);
        writeFileSettings(map);
    }

    /**
     * @deprecated Use {@link #setVariationsApplyIdentity(boolean)} instead.
     */
    @Deprecated
    public synchronized void setVariationsApplyUniqueness(final boolean apply)
    {
        setVariationsApplyIdentity(apply);
    }

    /**
     * Toggles the variations apply identity setting between true and false.
     *
     * @return updated boolean value
     */
    public synchronized boolean toggleVariationsApplyIdentity()
    {
        final boolean updated = !isVariationsApplyIdentity();
        setVariationsApplyIdentity(updated);
        return updated;
    }

    /**
     * @deprecated Use {@link #toggleVariationsApplyIdentity()} instead.
     */
    @Deprecated
    public synchronized boolean toggleVariationsApplyUniqueness()
    {
        return toggleVariationsApplyIdentity();
    }

    /**
     * Returns the minimum number of unknown failures required in previous runs
     * to consider a test variation flaky.
     *
     * @return minimum unknown failure count (defaults to 2)
     */
    public synchronized int getFlakyMinUnknownFailures()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(FLAKY_MIN_UNKNOWN_FAILURES_KEY);
                if (entityOpt.isPresent())
                {
                    return Integer.parseInt(entityOpt.get().getValue().trim());
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read flaky min unknown failures from database: {}", e.getMessage());
            }
        }

        final Map<String, String> fileSettings = readFileSettings();
        final String fileVal = fileSettings.get(FLAKY_MIN_UNKNOWN_FAILURES_KEY);
        if (fileVal != null && !fileVal.isBlank())
        {
            try
            {
                final int val = Integer.parseInt(fileVal.trim());
                if (repository != null)
                {
                    try
                    {
                        repository.save(new ReportSettingEntity(FLAKY_MIN_UNKNOWN_FAILURES_KEY, String.valueOf(val)));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                return val;
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        return DEFAULT_FLAKY_MIN_UNKNOWN_FAILURES;
    }

    /**
     * Persists the minimum number of unknown failures required to mark a test as flaky.
     *
     * @param minFailures minimum failure count (at least 1)
     */
    public synchronized void setFlakyMinUnknownFailures(final int minFailures)
    {
        final int val = Math.max(1, minFailures);
        final String str = String.valueOf(val);
        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(FLAKY_MIN_UNKNOWN_FAILURES_KEY, str));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist flaky min unknown failures to database: {}", e.getMessage());
            }
        }
        final Map<String, String> map = readFileSettings();
        map.put(FLAKY_MIN_UNKNOWN_FAILURES_KEY, str);
        writeFileSettings(map);
    }

    /**
     * Returns the sliding window size of previous completed runs to inspect for flakiness.
     *
     * @return historical run window size (defaults to 10)
     */
    public synchronized int getFlakyHistoryWindow()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(FLAKY_HISTORY_WINDOW_KEY);
                if (entityOpt.isPresent())
                {
                    return Integer.parseInt(entityOpt.get().getValue().trim());
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read flaky history window from database: {}", e.getMessage());
            }
        }

        final Map<String, String> fileSettings = readFileSettings();
        final String fileVal = fileSettings.get(FLAKY_HISTORY_WINDOW_KEY);
        if (fileVal != null && !fileVal.isBlank())
        {
            try
            {
                final int val = Integer.parseInt(fileVal.trim());
                if (repository != null)
                {
                    try
                    {
                        repository.save(new ReportSettingEntity(FLAKY_HISTORY_WINDOW_KEY, String.valueOf(val)));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                return val;
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        return DEFAULT_FLAKY_HISTORY_WINDOW;
    }

    /**
     * Persists the sliding window size of previous completed runs to inspect for flakiness.
     *
     * @param window window size (at least 1)
     */
    public synchronized void setFlakyHistoryWindow(final int window)
    {
        final int val = Math.max(1, window);
        final String str = String.valueOf(val);
        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(FLAKY_HISTORY_WINDOW_KEY, str));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist flaky history window to database: {}", e.getMessage());
            }
        }
        final Map<String, String> map = readFileSettings();
        map.put(FLAKY_HISTORY_WINDOW_KEY, str);
        writeFileSettings(map);
    }

    /**
     * Returns the minimum unknown failure rate (failures / totalRuns) required to consider a test flaky.
     *
     * @return minimum failure rate ratio between 0.0 and 1.0 (defaults to 0.20)
     */
    public synchronized double getFlakyMinFailureRate()
    {
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(FLAKY_MIN_FAILURE_RATE_KEY);
                if (entityOpt.isPresent())
                {
                    return Double.parseDouble(entityOpt.get().getValue().trim());
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read flaky min failure rate from database: {}", e.getMessage());
            }
        }

        final Map<String, String> fileSettings = readFileSettings();
        final String fileVal = fileSettings.get(FLAKY_MIN_FAILURE_RATE_KEY);
        if (fileVal != null && !fileVal.isBlank())
        {
            try
            {
                final double val = Double.parseDouble(fileVal.trim());
                if (repository != null)
                {
                    try
                    {
                        repository.save(new ReportSettingEntity(FLAKY_MIN_FAILURE_RATE_KEY, String.valueOf(val)));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                return val;
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        return DEFAULT_FLAKY_MIN_FAILURE_RATE;
    }

    /**
     * Persists the minimum unknown failure rate required to mark a test as flaky.
     *
     * @param rate failure rate ratio (clamped between 0.0 and 1.0)
     */
    public synchronized void setFlakyMinFailureRate(final double rate)
    {
        final double clamped = Math.max(0.0, Math.min(1.0, rate));
        final String str = String.valueOf(clamped);
        if (repository != null)
        {
            try
            {
                repository.save(new ReportSettingEntity(FLAKY_MIN_FAILURE_RATE_KEY, str));
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to persist flaky min failure rate to database: {}", e.getMessage());
            }
        }
        final Map<String, String> map = readFileSettings();
        map.put(FLAKY_MIN_FAILURE_RATE_KEY, str);
        writeFileSettings(map);
    }

    /**
     * Parses a comma-separated string into a list of normalized execution identity dimension names.
     * If the input is empty or null, returns {@link #DEFAULT_EXECUTION_IDENTITY_DIMENSIONS}.
     *
     * @param csv comma-separated string
     * @return non-empty list of unique normalized dimension names
     */
    public static List<String> parseExecutionIdentityDimensions(final String csv)
    {
        if (csv == null || csv.isBlank())
        {
            return DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
        }
        final Set<String> result = new LinkedHashSet<>();
        final String[] tokens = csv.split(",");
        for (final String t : tokens)
        {
            final String clean = normalizeDimensionName(t);
            if (!clean.isEmpty())
            {
                result.add(clean);
            }
        }
        return result.isEmpty() ? DEFAULT_EXECUTION_IDENTITY_DIMENSIONS : new ArrayList<>(result);
    }

    /**
     * @deprecated Use {@link #parseExecutionIdentityDimensions(String)} instead.
     */
    @Deprecated
    public static List<String> parseUniquenessDimensions(final String csv)
    {
        return parseExecutionIdentityDimensions(csv);
    }

    /**
     * Checks whether the given list of dimensions matches the standard default ["browser"].
     *
     * @param dims identity dimensions
     * @return true if default execution identity dimensions
     */
    public static boolean isDefaultExecutionIdentity(final List<String> dims)
    {
        if (dims == null || dims.size() != 1)
        {
            return false;
        }
        final Set<String> set = dims.stream()
            .map(ReportDashboardSettingService::normalizeDimensionName)
            .collect(Collectors.toSet());
        return set.contains("browser");
    }

    /**
     * @deprecated Use {@link #isDefaultExecutionIdentity(List)} instead.
     */
    @Deprecated
    public static boolean isDefaultUniqueness(final List<String> dims)
    {
        return isDefaultExecutionIdentity(dims);
    }

    public static String normalizeDimensionName(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        final String clean = raw.trim().toLowerCase(Locale.ROOT);
        if ("env".equals(clean) || "instance".equals(clean))
        {
            return "environment";
        }
        if ("browsers".equals(clean))
        {
            return "browser";
        }
        if ("locales".equals(clean) || "location".equals(clean))
        {
            return "locale";
        }
        if ("areas".equals(clean))
        {
            return "area";
        }
        if ("batch".equals(clean) || "suite".equals(clean))
        {
            return "batch";
        }
        return clean;
    }

    private Map<String, String> readFileSettings()
    {
        if (settingsFile == null || !Files.exists(settingsFile))
        {
            return new LinkedHashMap<>();
        }
        try
        {
            final byte[] bytes = Files.readAllBytes(settingsFile);
            if (bytes.length == 0)
            {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(bytes, new TypeReference<Map<String, String>>()
            {
            });
        }
        catch (final IOException e)
        {
            LOG.warn("Failed to read settings from {}: {}", settingsFile, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private void writeFileSettings(final Map<String, String> settings)
    {
        if (settingsFile == null)
        {
            return;
        }
        try
        {
            final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
            AtomicFileUtils.writeStringAtomic(settingsFile, json);
        }
        catch (final IOException e)
        {
            LOG.warn("Failed to write settings to {}: {}", settingsFile, e.getMessage());
        }
    }

    /**
     * Canonical default widgets matching the default dashboard layout.
     *
     * @return unmodifiable list of default widgets
     */
    public static List<DashboardWidgetConfigDto> getDefaultWidgets()
    {
        return List.of(
            new DashboardWidgetConfigDto(
                "default-status-pie",
                "Latest Test Status",
                WidgetMetric.STATUS,
                "",
                WidgetPresentation.PIE_CHART,
                WidgetTrendMode.LATEST_APPROACH,
                WidgetValueFormat.PERCENT,
                "half",
                0
            ),
            new DashboardWidgetConfigDto(
                "default-token-cost",
                "Token Cost & Trend",
                WidgetMetric.COSTS,
                "",
                WidgetPresentation.TREND_GRAPH,
                WidgetTrendMode.DAILY,
                WidgetValueFormat.ABSOLUTE,
                "full",
                1
            ),
            new DashboardWidgetConfigDto(
                "default-browser-ratio",
                "Browser Ratio",
                WidgetMetric.TAG,
                "browser",
                WidgetPresentation.RATIO_BAR,
                WidgetTrendMode.BY_RUNS,
                WidgetValueFormat.PERCENT,
                "half",
                2
            ),
            new DashboardWidgetConfigDto(
                "default-suite-evolution",
                "Suite Evolution Trend",
                WidgetMetric.STATUS,
                "",
                WidgetPresentation.TREND_GRAPH,
                WidgetTrendMode.LATEST_APPROACH,
                WidgetValueFormat.PERCENT,
                "full",
                3
            )
        );
    }

    /**
     * Checks whether custom dashboard widgets are persisted in database or file.
     *
     * @return true if customized, false if using defaults
     */
    public synchronized boolean isCustomWidgetsConfigured()
    {
        if (repository != null)
        {
            try
            {
                if (repository.existsById(DASHBOARD_WIDGETS_KEY))
                {
                    return true;
                }
            }
            catch (final Exception e)
            {
                LOG.debug("Could not check widget settings existence in database: {}", e.getMessage());
            }
        }
        final Map<String, String> fileSettings = readFileSettings();
        return fileSettings.containsKey(DASHBOARD_WIDGETS_KEY);
    }

    /**
     * Retrieves the list of configured dashboard widgets, or defaults if not configured.
     *
     * @return list of widget configurations
     */
    public synchronized List<DashboardWidgetConfigDto> getDashboardWidgets()
    {
        String json = null;
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(DASHBOARD_WIDGETS_KEY);
                if (entityOpt.isPresent())
                {
                    json = entityOpt.get().getValue();
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read dashboard widgets from database: {}", e.getMessage());
            }
        }

        if (json == null || json.isBlank())
        {
            final Map<String, String> fileSettings = readFileSettings();
            json = fileSettings.get(DASHBOARD_WIDGETS_KEY);
        }

        if (json != null && !json.isBlank())
        {
            try
            {
                final List<DashboardWidgetConfigDto> parsed = objectMapper.readValue(
                    json,
                    new TypeReference<List<DashboardWidgetConfigDto>>()
                    {
                    }
                );
                if (parsed != null)
                {
                    final List<DashboardWidgetConfigDto> sorted = new ArrayList<>(parsed);
                    sorted.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
                    return Collections.unmodifiableList(sorted);
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to parse dashboard widgets JSON: {}", e.getMessage());
            }
        }

        return getDefaultWidgets();
    }

    /**
     * Persists the list of configured dashboard widgets.
     *
     * @param widgets widgets to persist
     */
    public synchronized void setDashboardWidgets(final List<DashboardWidgetConfigDto> widgets)
    {
        final List<DashboardWidgetConfigDto> list = (widgets != null) ? new ArrayList<>(widgets) : new ArrayList<>();
        final List<DashboardWidgetConfigDto> ordered = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            ordered.add(list.get(i).withSortOrder(i));
        }

        try
        {
            final String json = objectMapper.writeValueAsString(ordered);
            if (repository != null)
            {
                try
                {
                    repository.save(new ReportSettingEntity(DASHBOARD_WIDGETS_KEY, json));
                }
                catch (final Exception e)
                {
                    LOG.warn("Failed to persist dashboard widgets to database: {}", e.getMessage());
                }
            }
            final Map<String, String> fileSettings = readFileSettings();
            fileSettings.put(DASHBOARD_WIDGETS_KEY, json);
            writeFileSettings(fileSettings);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to serialize dashboard widgets: {}", e.getMessage(), e);
        }
    }

    /**
     * Adds or updates a single dashboard widget by ID.
     *
     * @param widget widget to add or update
     */
    public synchronized void addOrUpdateWidget(final DashboardWidgetConfigDto widget)
    {
        if (widget == null)
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++)
        {
            if (current.get(i).getId().equals(widget.getId()))
            {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0)
        {
            current.set(existingIndex, widget);
        }
        else
        {
            boolean updatedChild = false;
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.isContainer() && w.hasChildren())
                {
                    final List<DashboardWidgetConfigDto> children = new ArrayList<>(w.getChildren());
                    for (int j = 0; j < children.size(); j++)
                    {
                        if (children.get(j).getId().equals(widget.getId()))
                        {
                            children.set(j, widget);
                            current.set(i, w.withChildren(children));
                            updatedChild = true;
                            break;
                        }
                    }
                    if (updatedChild)
                    {
                        break;
                    }
                }
            }
            if (!updatedChild)
            {
                current.add(widget);
            }
        }
        setDashboardWidgets(current);
    }

    /**
     * Deletes a dashboard widget by its ID.
     * If the widget is a child inside a container, removes it from that container.
     *
     * @param widgetId ID of widget to delete
     */
    public synchronized void deleteWidget(final String widgetId)
    {
        if (widgetId == null || widgetId.isBlank())
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        final boolean removedTopLevel = current.removeIf(w -> widgetId.equals(w.getId()));
        if (!removedTopLevel)
        {
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.isContainer() && w.hasChildren())
                {
                    final boolean containsChild = w.getChildren().stream().anyMatch(c -> c.getId().equals(widgetId));
                    if (containsChild)
                    {
                        final DashboardWidgetConfigDto updated = w.removeChild(widgetId);
                        if (updated.getChildren().size() <= 1)
                        {
                            if (updated.getChildren().isEmpty())
                            {
                                current.remove(i);
                            }
                            else
                            {
                                current.set(i, updated.getChildren().get(0));
                            }
                        }
                        else
                        {
                            current.set(i, updated);
                        }
                        break;
                    }
                }
            }
        }
        setDashboardWidgets(current);
    }

    /**
     * Unites two dashboard widgets into a single container widget.
     *
     * @param targetWidgetId ID of the target widget (or container)
     * @param sourceWidgetId ID of the source widget being dropped
     * @return the combined container widget configuration, or null if not found
     */
    public synchronized DashboardWidgetConfigDto combineWidgets(final String targetWidgetId, final String sourceWidgetId)
    {
        if (targetWidgetId == null || sourceWidgetId == null || targetWidgetId.equals(sourceWidgetId))
        {
            return null;
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        DashboardWidgetConfigDto target = null;
        DashboardWidgetConfigDto source = null;
        int targetIdx = -1;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(targetWidgetId))
            {
                target = w;
                targetIdx = i;
            }
            if (w.getId().equals(sourceWidgetId))
            {
                source = w;
            }
        }

        if (target == null || source == null || target.isRuns() || source.isRuns())
        {
            return null;
        }

        current.remove(source);
        targetIdx = current.indexOf(target);

        final DashboardWidgetConfigDto container;
        if (target.isContainer())
        {
            container = target.addChild(source);
        }
        else
        {
            final String containerId = "container-" + UUID.randomUUID().toString().substring(0, 8);
            final String title = target.getTitle() + " & " + source.getTitle();
            container = DashboardWidgetConfigDto.createContainer(
                containerId,
                title,
                List.of(target, source),
                target.getSortOrder()
            );
        }

        current.set(targetIdx, container);
        setDashboardWidgets(current);
        return container;
    }

    /**
     * Separates a container widget back into its individual child widgets.
     *
     * @param containerId ID of the container widget to separate
     * @return list of separated child widgets, or empty list if not found
     */
    public synchronized List<DashboardWidgetConfigDto> separateContainer(final String containerId)
    {
        if (containerId == null || containerId.isBlank())
        {
            return Collections.emptyList();
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(containerId))
            {
                container = w;
                targetIdx = i;
                break;
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return Collections.emptyList();
        }

        final List<DashboardWidgetConfigDto> children = new ArrayList<>(container.getChildren());
        current.remove(targetIdx);
        for (int j = 0; j < children.size(); j++)
        {
            current.add(targetIdx + j, children.get(j));
        }

        setDashboardWidgets(current);
        return Collections.unmodifiableList(children);
    }

    /**
     * Separates a single child widget out of a container into an independent top-level widget.
     * If the container originally had 3 or more children, the remaining children remain together in the container.
     * If the container only had 2 children, separating one child causes the other child to also become an independent widget,
     * cleanly dissolving the 2-item container into two standalone widgets.
     *
     * @param containerId optional container ID (if null or blank, resolved by searching for the container containing childWidgetId)
     * @param childWidgetId ID of the child widget to extract
     * @return the extracted child widget configuration, or null if not found
     */
    public synchronized DashboardWidgetConfigDto separateChildWidget(final String containerId, final String childWidgetId)
    {
        if (childWidgetId == null || childWidgetId.isBlank())
        {
            return null;
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        if (containerId != null && !containerId.isBlank())
        {
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.getId().equals(containerId))
                {
                    container = w;
                    targetIdx = i;
                    break;
                }
            }
        }

        if (container == null)
        {
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.isContainer() && w.hasChildren())
                {
                    final boolean hasChild = w.getChildren().stream().anyMatch(c -> c.getId().equals(childWidgetId));
                    if (hasChild)
                    {
                        container = w;
                        targetIdx = i;
                        break;
                    }
                }
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return null;
        }

        DashboardWidgetConfigDto childToExtract = null;
        for (final DashboardWidgetConfigDto c : container.getChildren())
        {
            if (c.getId().equals(childWidgetId))
            {
                childToExtract = c;
                break;
            }
        }

        if (childToExtract == null)
        {
            return null;
        }

        final DashboardWidgetConfigDto updatedContainer = container.removeChild(childWidgetId);
        if (updatedContainer.getChildren().size() <= 1)
        {
            if (updatedContainer.getChildren().isEmpty())
            {
                current.set(targetIdx, childToExtract);
            }
            else
            {
                final DashboardWidgetConfigDto remainingChild = updatedContainer.getChildren().get(0);
                current.set(targetIdx, remainingChild);
                current.add(targetIdx + 1, childToExtract);
            }
        }
        else
        {
            final String updatedTitle;
            if (container.getTitle() != null && container.getTitle().contains(childToExtract.getTitle()))
            {
                final List<String> remainingTitles = updatedContainer.getChildren().stream()
                    .map(DashboardWidgetConfigDto::getTitle)
                    .collect(Collectors.toList());
                updatedTitle = String.join(" & ", remainingTitles);
            }
            else
            {
                updatedTitle = container.getTitle();
            }

            final DashboardWidgetConfigDto updatedWithTitle = updatedContainer.withTitle(updatedTitle);
            current.set(targetIdx, updatedWithTitle);
            current.add(targetIdx + 1, childToExtract);
        }

        setDashboardWidgets(current);
        return childToExtract;
    }

    /**
     * Reorders dashboard widgets according to the provided ID sequence.
     *
     * @param widgetIdsInOrder sequence of widget IDs
     */
    public synchronized void reorderWidgets(final List<String> widgetIdsInOrder)
    {
        if (widgetIdsInOrder == null || widgetIdsInOrder.isEmpty())
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = getDashboardWidgets();
        final Map<String, DashboardWidgetConfigDto> byId = new LinkedHashMap<>();
        for (final DashboardWidgetConfigDto w : current)
        {
            byId.put(w.getId(), w);
        }

        final List<DashboardWidgetConfigDto> reordered = new ArrayList<>();
        for (final String id : widgetIdsInOrder)
        {
            if (byId.containsKey(id))
            {
                reordered.add(byId.remove(id));
            }
        }
        reordered.addAll(byId.values());
        setDashboardWidgets(reordered);
    }

    /**
     * Reorders the child widgets of a container widget according to the provided child ID sequence on the default dashboard.
     *
     * @param containerId ID of the container widget
     * @param childIdsInOrder sequence of child widget IDs
     * @return true if the container was found and reordered, false otherwise
     */
    public synchronized boolean reorderContainerChildren(final String containerId, final List<String> childIdsInOrder)
    {
        if (containerId == null || containerId.isBlank() || childIdsInOrder == null || childIdsInOrder.isEmpty())
        {
            return false;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(containerId))
            {
                container = w;
                targetIdx = i;
                break;
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return false;
        }

        final DashboardWidgetConfigDto updatedContainer = container.reorderChildren(childIdsInOrder);
        current.set(targetIdx, updatedContainer);
        setDashboardWidgets(current);
        return true;
    }

    /**
     * Renames a dashboard widget or container group by its ID on the default dashboard.
     * Supports renaming top-level widgets, container groups, or child widgets inside a container group.
     *
     * @param widgetId ID of widget or container to rename
     * @param newTitle new title for the widget
     * @return true if widget was found and renamed, false otherwise
     */
    public synchronized boolean renameWidget(final String widgetId, final String newTitle)
    {
        if (widgetId == null || widgetId.isBlank() || newTitle == null || newTitle.isBlank())
        {
            return false;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getDashboardWidgets());
        final boolean updated = updateWidgetTitleInList(current, widgetId, newTitle.trim());
        if (updated)
        {
            setDashboardWidgets(current);
        }
        return updated;
    }

    /**
     * Renames a dashboard widget or container group by its ID on the specified page.
     * Supports renaming top-level widgets, container groups, or child widgets inside a container group.
     *
     * @param pageId optional page ID (if null or blank, targets default dashboard)
     * @param widgetId ID of widget or container to rename
     * @param newTitle new title for the widget
     * @return true if widget was found and renamed, false otherwise
     */
    public synchronized boolean renameWidget(final String pageId, final String widgetId, final String newTitle)
    {
        if (pageId == null || pageId.isBlank())
        {
            return renameWidget(widgetId, newTitle);
        }
        if (widgetId == null || widgetId.isBlank() || newTitle == null || newTitle.isBlank())
        {
            return false;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        final boolean updated = updateWidgetTitleInList(current, widgetId, newTitle.trim());
        if (updated)
        {
            setWidgetsForPage(pageId, current);
        }
        return updated;
    }

    private static boolean updateWidgetTitleInList(
        final List<DashboardWidgetConfigDto> list,
        final String widgetId,
        final String newTitle)
    {
        for (int i = 0; i < list.size(); i++)
        {
            final DashboardWidgetConfigDto w = list.get(i);
            if (widgetId.equals(w.getId()))
            {
                list.set(i, w.withTitle(newTitle));
                return true;
            }
            if (w.isContainer() && w.hasChildren())
            {
                final List<DashboardWidgetConfigDto> children = new ArrayList<>(w.getChildren());
                for (int j = 0; j < children.size(); j++)
                {
                    final DashboardWidgetConfigDto child = children.get(j);
                    if (widgetId.equals(child.getId()))
                    {
                        children.set(j, child.withTitle(newTitle));
                        list.set(i, w.withChildren(children));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Resets dashboard widgets to canonical defaults.
     */
    public synchronized void resetDefaultWidgets()
    {
        if (repository != null)
        {
            try
            {
                repository.deleteById(DASHBOARD_WIDGETS_KEY);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to delete dashboard widgets setting from database: {}", e.getMessage());
            }
        }
        final Map<String, String> fileSettings = readFileSettings();
        if (fileSettings.containsKey(DASHBOARD_WIDGETS_KEY))
        {
            fileSettings.remove(DASHBOARD_WIDGETS_KEY);
            writeFileSettings(fileSettings);
        }
    }

    /**
     * Retrieves all configured custom dashboard pages in sort order.
     *
     * @return list of custom dashboard pages
     */
    public synchronized List<DashboardPageConfigDto> getCustomPages()
    {
        String json = null;
        if (repository != null)
        {
            try
            {
                final Optional<ReportSettingEntity> entityOpt = repository.findById(CUSTOM_DASHBOARD_PAGES_KEY);
                if (entityOpt.isPresent())
                {
                    json = entityOpt.get().getValue();
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read custom dashboard pages from database: {}", e.getMessage());
            }
        }

        if (json == null || json.isBlank())
        {
            final Map<String, String> fileSettings = readFileSettings();
            json = fileSettings.get(CUSTOM_DASHBOARD_PAGES_KEY);
        }

        if (json != null && !json.isBlank())
        {
            try
            {
                final List<DashboardPageConfigDto> parsed = objectMapper.readValue(
                    json,
                    new TypeReference<List<DashboardPageConfigDto>>()
                    {
                    }
                );
                if (parsed != null)
                {
                    final List<DashboardPageConfigDto> sorted = new ArrayList<>(parsed);
                    sorted.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
                    return Collections.unmodifiableList(sorted);
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to parse custom dashboard pages JSON: {}", e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    /**
     * Persists the list of custom dashboard pages.
     *
     * @param pages list of pages to persist
     */
    public synchronized void setCustomPages(final List<DashboardPageConfigDto> pages)
    {
        final List<DashboardPageConfigDto> list = (pages != null) ? new ArrayList<>(pages) : new ArrayList<>();
        final List<DashboardPageConfigDto> ordered = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            ordered.add(list.get(i).withSortOrder(i));
        }

        try
        {
            final String json = objectMapper.writeValueAsString(ordered);
            if (repository != null)
            {
                try
                {
                    repository.save(new ReportSettingEntity(CUSTOM_DASHBOARD_PAGES_KEY, json));
                }
                catch (final Exception e)
                {
                    LOG.warn("Failed to persist custom dashboard pages to database: {}", e.getMessage());
                }
            }
            final Map<String, String> fileSettings = readFileSettings();
            fileSettings.put(CUSTOM_DASHBOARD_PAGES_KEY, json);
            writeFileSettings(fileSettings);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to serialize custom dashboard pages: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves a single custom dashboard page by its ID.
     *
     * @param pageId ID of the page
     * @return custom dashboard page, or null if not found
     */
    public synchronized DashboardPageConfigDto getCustomPage(final String pageId)
    {
        if (pageId == null || pageId.isBlank())
        {
            return null;
        }
        for (final DashboardPageConfigDto p : getCustomPages())
        {
            if (pageId.equals(p.getId()))
            {
                return p;
            }
        }
        return null;
    }

    /**
     * Creates and persists a new custom dashboard page with the given name, execution identity dimensions,
     * run filters, and empty widgets.
     *
     * @param name display name for the new page
     * @param executionIdentityDimensions default execution identity dimensions for this page
     * @param runFilters default run filters for this page
     * @return newly created DashboardPageConfigDto
     */
    /**
     * Creates and persists a new custom dashboard page with the given name, execution identity dimensions,
     * run filters, and optionally cloned widgets from an existing source dashboard.
     *
     * @param name display name for the new page
     * @param executionIdentityDimensions default execution identity dimensions for this page
     * @param runFilters default run filters for this page
     * @param copyFromPageId optional source dashboard ID to clone widgets from
     * @return newly created DashboardPageConfigDto
     */
    public synchronized DashboardPageConfigDto createCustomPage(
        final String name,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters,
        final String copyFromPageId)
    {
        final String effectiveName = (name != null && !name.isBlank()) ? name.trim() : "Custom Dashboard";
        final String pageId = "page-" + UUID.randomUUID().toString().substring(0, 8);
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        final List<String> effectiveIdentity = (executionIdentityDimensions != null && !executionIdentityDimensions.isEmpty())
            ? new ArrayList<>(executionIdentityDimensions)
            : new ArrayList<>(DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        if (effectiveIdentity.stream().noneMatch("browser"::equalsIgnoreCase))
        {
            effectiveIdentity.add(0, "browser");
        }

        final List<DashboardWidgetConfigDto> initialWidgets;
        if (copyFromPageId != null && !copyFromPageId.isBlank())
        {
            final String normCopyId = "default".equalsIgnoreCase(copyFromPageId.trim()) ? null : copyFromPageId.trim();
            final List<DashboardWidgetConfigDto> sourceWidgets = getWidgetsForPage(normCopyId);
            if (sourceWidgets != null && !sourceWidgets.isEmpty())
            {
                initialWidgets = new ArrayList<>();
                for (int i = 0; i < sourceWidgets.size(); i++)
                {
                    initialWidgets.add(sourceWidgets.get(i).cloneWithNewId().withSortOrder(i));
                }
            }
            else
            {
                initialWidgets = Collections.emptyList();
            }
        }
        else
        {
            initialWidgets = Collections.emptyList();
        }

        final DashboardPageConfigDto newPage = new DashboardPageConfigDto(
            pageId,
            effectiveName,
            initialWidgets,
            pages.size(),
            System.currentTimeMillis(),
            effectiveIdentity,
            runFilters
        );
        pages.add(newPage);
        setCustomPages(pages);
        return newPage;
    }

    /**
     * Creates and persists a new custom dashboard page with the given name, execution identity dimensions,
     * run filters, and empty widgets.
     *
     * @param name display name for the new page
     * @param executionIdentityDimensions default execution identity dimensions for this page
     * @param runFilters default run filters for this page
     * @return newly created DashboardPageConfigDto
     */
    public synchronized DashboardPageConfigDto createCustomPage(
        final String name,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters)
    {
        return createCustomPage(name, executionIdentityDimensions, runFilters, null);
    }

    /**
     * Creates and persists a new custom dashboard page with default execution identity and empty widgets.
     *
     * @param name display name for the new page
     * @return newly created DashboardPageConfigDto
     */
    public synchronized DashboardPageConfigDto createCustomPage(final String name)
    {
        return createCustomPage(name, DEFAULT_EXECUTION_IDENTITY_DIMENSIONS, null);
    }

    /**
     * Creates a main group dashboard and child sub-dashboards for each grouping.
     *
     * @param name display name for the group dashboard
     * @param groupType grouping dimension (e.g. "browser", "environment", "batch")
     * @param groupCardScope evaluation scope for group cards (LATEST_TEST_STATUS or LAST_RUN)
     * @param executionIdentityDimensions execution identity dimensions
     * @param runFilters optional base run filters
     * @param groupValues initial group values for which to create sub-dashboards
     * @return newly created parent DashboardPageConfigDto
     */
    public synchronized DashboardPageConfigDto createGroupDashboard(
        final String name,
        final String groupType,
        final WidgetPointScope groupCardScope,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters,
        final List<String> groupValues)
    {
        return createGroupDashboard(name, groupType, groupCardScope, executionIdentityDimensions, runFilters, groupValues, null);
    }

    /**
     * Creates a main group dashboard and child sub-dashboards for each grouping,
     * optionally initializing parent widgets from a source dashboard.
     *
     * @param name display name for the group dashboard
     * @param groupType grouping dimension (e.g. "browser", "environment", "batch")
     * @param groupCardScope evaluation scope for group cards (LATEST_TEST_STATUS or LAST_RUN)
     * @param executionIdentityDimensions execution identity dimensions
     * @param runFilters optional base run filters
     * @param groupValues initial group values for which to create sub-dashboards
     * @param copyFromPageId optional source dashboard ID to clone widgets from
     * @return newly created parent DashboardPageConfigDto
     */
    public synchronized DashboardPageConfigDto createGroupDashboard(
        final String name,
        final String groupType,
        final WidgetPointScope groupCardScope,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters,
        final List<String> groupValues,
        final String copyFromPageId)
    {
        final String effectiveGroupType = normalizeGroupType(groupType);
        final String effectiveName = (name != null && !name.isBlank())
            ? name.trim()
            : formatGroupDashboardName(effectiveGroupType);
        final String parentPageId = "page-group-" + UUID.randomUUID().toString().substring(0, 8);
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        final List<String> effectiveIdentity = (executionIdentityDimensions != null && !executionIdentityDimensions.isEmpty())
            ? new ArrayList<>(executionIdentityDimensions)
            : new ArrayList<>(DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        if (effectiveIdentity.stream().noneMatch("browser"::equalsIgnoreCase))
        {
            effectiveIdentity.add(0, "browser");
        }
        final WidgetPointScope effectiveScope = (groupCardScope != null) ? groupCardScope : WidgetPointScope.LATEST_TEST_STATUS;

        final List<DashboardWidgetConfigDto> initialWidgets;
        if (copyFromPageId != null && !copyFromPageId.isBlank())
        {
            final String normCopyId = "default".equalsIgnoreCase(copyFromPageId.trim()) ? null : copyFromPageId.trim();
            final List<DashboardWidgetConfigDto> sourceWidgets = getWidgetsForPage(normCopyId);
            if (sourceWidgets != null && !sourceWidgets.isEmpty())
            {
                initialWidgets = new ArrayList<>();
                for (int i = 0; i < sourceWidgets.size(); i++)
                {
                    initialWidgets.add(sourceWidgets.get(i).cloneWithNewId().withSortOrder(i));
                }
            }
            else
            {
                initialWidgets = Collections.emptyList();
            }
        }
        else
        {
            initialWidgets = Collections.emptyList();
        }

        final DashboardPageConfigDto parentPage = new DashboardPageConfigDto(
            parentPageId,
            effectiveName,
            initialWidgets,
            pages.size(),
            System.currentTimeMillis(),
            effectiveIdentity,
            runFilters,
            null,
            true,
            effectiveGroupType,
            null,
            effectiveScope
        );
        pages.add(parentPage);

        if (groupValues != null)
        {
            for (final String gVal : groupValues)
            {
                if (gVal == null || gVal.isBlank())
                {
                    continue;
                }
                final String cleanVal = gVal.trim();
                final String subPageId = "page-sub-" + UUID.randomUUID().toString().substring(0, 8);
                final WidgetFilterConfigDto subFilters = createGroupRunFilters(effectiveGroupType, cleanVal, runFilters);
                final DashboardPageConfigDto subPage = new DashboardPageConfigDto(
                    subPageId,
                    cleanVal,
                    Collections.emptyList(),
                    pages.size(),
                    System.currentTimeMillis(),
                    effectiveIdentity,
                    subFilters,
                    parentPageId,
                    false,
                    effectiveGroupType,
                    cleanVal,
                    effectiveScope
                );
                pages.add(subPage);
            }
        }

        setCustomPages(pages);
        return parentPage;
    }

    /**
     * Normalizes a potentially compound (comma-separated) group type string into a canonical
     * comma-separated list of dimension names (e.g. "batch,browser").
     *
     * @param groupType raw grouping type string
     * @return normalized comma-separated grouping type, or "browser" if empty
     */
    public static String normalizeGroupType(final String groupType)
    {
        if (groupType == null || groupType.isBlank())
        {
            return "browser";
        }
        final Set<String> normalized = new LinkedHashSet<>();
        final String[] tokens = groupType.split(",");
        for (final String t : tokens)
        {
            final String norm = normalizeDimensionName(t);
            if (!norm.isEmpty())
            {
                normalized.add(norm);
            }
        }
        return normalized.isEmpty() ? "browser" : String.join(",", normalized);
    }

    /**
     * Formats a canonical group dashboard display name from a normalized group type string.
     * For example, "batch,browser" becomes "Batch & Browser Groups Dashboard".
     *
     * @param normalizedGroupType normalized group type string
     * @return human-readable group dashboard name
     */
    public static String formatGroupDashboardName(final String normalizedGroupType)
    {
        final String effective = (normalizedGroupType != null && !normalizedGroupType.isBlank())
            ? normalizedGroupType
            : "browser";
        final String[] tokens = effective.split(",");
        final List<String> formatted = new ArrayList<>();
        for (final String t : tokens)
        {
            final String trimmed = t.trim();
            if (!trimmed.isEmpty())
            {
                formatted.add(trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1));
            }
        }
        return String.join(" & ", formatted) + " Groups Dashboard";
    }

    /**
     * Constructs a scoped {@link WidgetFilterConfigDto} for a specific grouping value.
     * Supports compound dimensions (e.g. "batch,browser") and compound values (e.g. "SmokeBatch • Chrome").
     *
     * @param groupType grouping dimension (e.g. "browser", "environment", "locale", "area", "batch", or comma-separated compound)
     * @param groupValue value of the grouping (e.g. "Chrome", "Staging", or bullet-separated compound)
     * @param baseFilters base run filters to inherit limit and timestamps from
     * @return scoped WidgetFilterConfigDto
     */
    public static WidgetFilterConfigDto createGroupRunFilters(
        final String groupType,
        final String groupValue,
        final WidgetFilterConfigDto baseFilters)
    {
        final Set<String> browsers = new LinkedHashSet<>();
        final Set<String> locales = new LinkedHashSet<>();
        final Set<String> envs = new LinkedHashSet<>();
        final Set<String> areas = new LinkedHashSet<>();
        final Set<String> tags = new LinkedHashSet<>();
        final Map<String, Set<String>> tagsByType = new LinkedHashMap<>();

        if (baseFilters != null)
        {
            if (baseFilters.getBrowsers() != null) browsers.addAll(baseFilters.getBrowsers());
            if (baseFilters.getLocales() != null) locales.addAll(baseFilters.getLocales());
            if (baseFilters.getEnvs() != null) envs.addAll(baseFilters.getEnvs());
            if (baseFilters.getAreas() != null) areas.addAll(baseFilters.getAreas());
            if (baseFilters.getTags() != null) tags.addAll(baseFilters.getTags());
            if (baseFilters.getTagsByType() != null)
            {
                for (final Map.Entry<String, Set<String>> e : baseFilters.getTagsByType().entrySet())
                {
                    tagsByType.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
                }
            }
        }

        final List<String> dims = new ArrayList<>();
        if (groupType != null && !groupType.isBlank())
        {
            final String[] rawDims = groupType.split(",");
            for (final String rd : rawDims)
            {
                final String norm = normalizeDimensionName(rd);
                if (!norm.isEmpty())
                {
                    dims.add(norm);
                }
            }
        }

        final List<String> vals = new ArrayList<>();
        if (groupValue != null && !groupValue.isBlank())
        {
            final String trimmed = groupValue.trim();
            final String[] rawVals;
            if (trimmed.contains(" • "))
            {
                rawVals = trimmed.split(" • ");
            }
            else if (trimmed.contains(" / "))
            {
                rawVals = trimmed.split(" / ");
            }
            else if (trimmed.contains(" | "))
            {
                rawVals = trimmed.split(" \\| ");
            }
            else
            {
                rawVals = new String[] { trimmed };
            }
            for (final String rv : rawVals)
            {
                vals.add(rv.trim());
            }
        }

        for (int i = 0; i < dims.size(); i++)
        {
            final String cleanDim = dims.get(i);
            final String cleanVal = (i < vals.size()) ? vals.get(i) : "";

            if ("browser".equals(cleanDim))
            {
                browsers.clear();
                if (!cleanVal.isEmpty())
                {
                    browsers.add(cleanVal);
                }
            }
            else if ("locale".equals(cleanDim))
            {
                locales.clear();
                if (!cleanVal.isEmpty())
                {
                    locales.add(cleanVal);
                }
            }
            else if ("environment".equals(cleanDim))
            {
                envs.clear();
                if (!cleanVal.isEmpty())
                {
                    envs.add(cleanVal);
                }
            }
            else if ("area".equals(cleanDim))
            {
                areas.clear();
                if (!cleanVal.isEmpty())
                {
                    areas.add(cleanVal);
                }
            }
            else if ("batch".equals(cleanDim))
            {
                tags.clear();
                if (!cleanVal.isEmpty())
                {
                    tags.add(cleanVal);
                }
            }
            else if (!cleanDim.isEmpty())
            {
                if (!cleanVal.isEmpty())
                {
                    tagsByType.put(cleanDim, Set.of(cleanVal));
                }
            }
        }

        final Integer limit = (baseFilters != null) ? baseFilters.getLimit() : null;
        final String startDate = (baseFilters != null) ? baseFilters.getStartDate() : null;
        final String endDate = (baseFilters != null) ? baseFilters.getEndDate() : null;
        final Set<String> exBrowsers = (baseFilters != null && baseFilters.getExcludeBrowsers() != null) ? baseFilters.getExcludeBrowsers() : Collections.emptySet();
        final Set<String> exLocales = (baseFilters != null && baseFilters.getExcludeLocales() != null) ? baseFilters.getExcludeLocales() : Collections.emptySet();
        final Set<String> exEnvs = (baseFilters != null && baseFilters.getExcludeEnvs() != null) ? baseFilters.getExcludeEnvs() : Collections.emptySet();
        final Set<String> exAreas = (baseFilters != null && baseFilters.getExcludeAreas() != null) ? baseFilters.getExcludeAreas() : Collections.emptySet();
        final Set<String> exTags = (baseFilters != null && baseFilters.getExcludeTags() != null) ? baseFilters.getExcludeTags() : Collections.emptySet();
        final Map<String, Set<String>> exTagsByType = (baseFilters != null && baseFilters.getExcludeTagsByType() != null) ? baseFilters.getExcludeTagsByType() : Collections.emptyMap();

        return new WidgetFilterConfigDto(
            browsers, exBrowsers,
            locales, exLocales,
            envs, exEnvs,
            areas, exAreas,
            tags, exTags,
            tagsByType, exTagsByType,
            limit, startDate, endDate
        );
    }

    /**
     * Retrieves all sub-pages for a specific parent group dashboard.
     *
     * @param parentPageId parent dashboard page ID
     * @return list of sub-pages
     */
    public synchronized List<DashboardPageConfigDto> getSubPages(final String parentPageId)
    {
        if (parentPageId == null || parentPageId.isBlank())
        {
            return Collections.emptyList();
        }
        return getCustomPages().stream()
            .filter(p -> parentPageId.equals(p.getParentPageId()))
            .collect(Collectors.toList());
    }

    /**
     * Determines the set of grouping dimensions that are prohibited for a given page.
     * Any dimension already used in this page's own grouping or in any of its ancestor
     * dashboards' groupings is prohibited to prevent redundant or conflicting re-grouping.
     *
     * @param pageId custom dashboard page ID
     * @return set of normalized prohibited grouping dimensions (e.g. "batch", "browser")
     */
    public synchronized Set<String> getProhibitedGroupDimensions(final String pageId)
    {
        final Set<String> prohibited = new LinkedHashSet<>();
        if (pageId == null || pageId.isBlank())
        {
            return prohibited;
        }

        final DashboardPageConfigDto page = getCustomPage(pageId);
        if (page == null)
        {
            return prohibited;
        }

        if (page.getGroupType() != null && !page.getGroupType().isBlank())
        {
            final String[] tokens = page.getGroupType().split(",");
            for (final String t : tokens)
            {
                final String norm = normalizeDimensionName(t);
                if (!norm.isEmpty())
                {
                    prohibited.add(norm);
                }
            }
        }

        String parentId = page.getParentPageId();
        final Set<String> visited = new LinkedHashSet<>();
        while (parentId != null && !parentId.isBlank() && visited.add(parentId))
        {
            final DashboardPageConfigDto parent = getCustomPage(parentId);
            if (parent == null)
            {
                break;
            }
            if (parent.getGroupType() != null && !parent.getGroupType().isBlank())
            {
                final String[] tokens = parent.getGroupType().split(",");
                for (final String t : tokens)
                {
                    final String norm = normalizeDimensionName(t);
                    if (!norm.isEmpty())
                    {
                        prohibited.add(norm);
                    }
                }
            }
            parentId = parent.getParentPageId();
        }

        return Collections.unmodifiableSet(prohibited);
    }

    /**
     * Retrieves all top-level custom dashboard pages (excluding child sub-pages).
     *
     * @return list of root custom pages
     */
    public synchronized List<DashboardPageConfigDto> getRootCustomPages()
    {
        final List<DashboardPageConfigDto> all = getCustomPages();
        final List<DashboardPageConfigDto> roots = new ArrayList<>();
        final Set<String> visited = new LinkedHashSet<>();
        for (final DashboardPageConfigDto p : all)
        {
            if (p.getParentPageId() == null || p.getParentPageId().isBlank())
            {
                roots.add(buildSubPageTree(p, all, visited));
            }
        }
        return Collections.unmodifiableList(roots);
    }

    /**
     * Recursively builds the hierarchical sub-page tree for a given dashboard page,
     * protecting against circular parent references.
     *
     * @param page dashboard page configuration
     * @param allPages list of all dashboard pages
     * @param visited set of already visited page IDs
     * @return page configured with its sub-pages tree
     */
    private DashboardPageConfigDto buildSubPageTree(
        final DashboardPageConfigDto page,
        final List<DashboardPageConfigDto> allPages,
        final Set<String> visited)
    {
        if (page == null || !page.isGroupParent() || !visited.add(page.getId()))
        {
            return page;
        }
        final List<DashboardPageConfigDto> children = allPages.stream()
            .filter(c -> page.getId().equals(c.getParentPageId()))
            .map(c -> buildSubPageTree(c, allPages, visited))
            .collect(Collectors.toList());
        return page.withSubPages(children);
    }

    /**
     * Ensures that a sub-dashboard page exists for the specified group value under the given parent page.
     * If absent, creates and persists a new child sub-dashboard and returns it.
     *
     * @param parentPageId parent dashboard page ID
     * @param groupType grouping dimension
     * @param groupValue value of the grouping
     * @param groupCardScope evaluation scope
     * @return existing or newly created DashboardPageConfigDto for this grouping
     */
    public synchronized DashboardPageConfigDto ensureSubDashboardForGroup(
        final String parentPageId,
        final String groupType,
        final String groupValue,
        final WidgetPointScope groupCardScope)
    {
        if (parentPageId == null || parentPageId.isBlank() || groupValue == null || groupValue.isBlank())
        {
            return null;
        }
        final String cleanVal = groupValue.trim();
        for (final DashboardPageConfigDto p : getCustomPages())
        {
            if (parentPageId.equals(p.getParentPageId()) && cleanVal.equalsIgnoreCase(p.getGroupValue()))
            {
                return p;
            }
        }

        final DashboardPageConfigDto parent = getCustomPage(parentPageId);
        final List<String> identity = (parent != null) ? parent.getExecutionIdentityDimensions() : DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
        final WidgetFilterConfigDto baseFilters = (parent != null) ? parent.getRunFilters() : null;
        final WidgetPointScope scope = (groupCardScope != null) ? groupCardScope : (parent != null ? parent.getGroupCardScope() : WidgetPointScope.LATEST_TEST_STATUS);
        final String effectiveGroupType = (groupType != null && !groupType.isBlank())
            ? normalizeGroupType(groupType)
            : (parent != null && parent.getGroupType() != null ? normalizeGroupType(parent.getGroupType()) : "browser");

        final String subPageId = "page-sub-" + UUID.randomUUID().toString().substring(0, 8);
        final WidgetFilterConfigDto subFilters = createGroupRunFilters(effectiveGroupType, cleanVal, baseFilters);
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        final DashboardPageConfigDto subPage = new DashboardPageConfigDto(
            subPageId,
            cleanVal,
            Collections.emptyList(),
            pages.size(),
            System.currentTimeMillis(),
            identity,
            subFilters,
            parentPageId,
            false,
            effectiveGroupType,
            cleanVal,
            scope
        );
        pages.add(subPage);
        setCustomPages(pages);
        return subPage;
    }

    /**
     * Converts an existing custom dashboard page (e.g. a sub grouping dashboard or standard page)
     * into a group dashboard page with its own grouping dimension, status evaluation scope,
     * execution identity dimensions, run filters, and auto-generated child sub-dashboards.
     *
     * @param pageId ID of the page to convert
     * @param groupType grouping dimension (e.g. "browser", "locale", "environment", "area", "batch", or custom tag)
     * @param groupCardScope evaluation scope for group cards
     * @param executionIdentityDimensions optional custom execution identity dimensions
     * @param runFilters optional custom run filters
     * @param groupValues discovered group values to generate sub-dashboards for
     * @return updated DashboardPageConfigDto, or null if page not found
     */
    public synchronized DashboardPageConfigDto convertToGroupDashboard(
        final String pageId,
        final String groupType,
        final WidgetPointScope groupCardScope,
        final List<String> executionIdentityDimensions,
        final WidgetFilterConfigDto runFilters,
        final List<String> groupValues)
    {
        if (pageId == null || pageId.isBlank())
        {
            return null;
        }
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        int targetIdx = -1;
        DashboardPageConfigDto target = null;
        for (int i = 0; i < pages.size(); i++)
        {
            if (pageId.equals(pages.get(i).getId()))
            {
                targetIdx = i;
                target = pages.get(i);
                break;
            }
        }
        if (target == null)
        {
            return null;
        }

        final String cleanDim = (groupType != null && !groupType.isBlank())
            ? normalizeGroupType(groupType)
            : "browser";
        final WidgetPointScope scope = (groupCardScope != null) ? groupCardScope : WidgetPointScope.LATEST_TEST_STATUS;

        final List<String> rawIdentity = (executionIdentityDimensions != null && !executionIdentityDimensions.isEmpty())
            ? executionIdentityDimensions
            : ((target.getExecutionIdentityDimensions() != null && !target.getExecutionIdentityDimensions().isEmpty())
                ? target.getExecutionIdentityDimensions()
                : DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        final List<String> effectiveIdentity = new ArrayList<>(rawIdentity);
        if (effectiveIdentity.stream().noneMatch("browser"::equalsIgnoreCase))
        {
            effectiveIdentity.add(0, "browser");
        }

        final WidgetFilterConfigDto effectiveFilters = (runFilters != null)
            ? runFilters
            : target.getRunFilters();

        final DashboardPageConfigDto converted = new DashboardPageConfigDto(
            target.getId(),
            target.getName(),
            target.getWidgets(),
            target.getSortOrder(),
            target.getCreatedTimestamp(),
            effectiveIdentity,
            effectiveFilters,
            target.getParentPageId(),
            true,
            cleanDim,
            target.getGroupValue(),
            scope
        );
        pages.set(targetIdx, converted);

        if (groupValues != null)
        {
            for (final String gVal : groupValues)
            {
                if (gVal == null || gVal.isBlank())
                {
                    continue;
                }
                final String cleanVal = gVal.trim();
                boolean exists = false;
                for (final DashboardPageConfigDto p : pages)
                {
                    if (target.getId().equals(p.getParentPageId()) && cleanVal.equalsIgnoreCase(p.getGroupValue()))
                    {
                        exists = true;
                        break;
                    }
                }
                if (!exists)
                {
                    final String subPageId = "page-sub-" + UUID.randomUUID().toString().substring(0, 8);
                    final WidgetFilterConfigDto subFilters = createGroupRunFilters(cleanDim, cleanVal, effectiveFilters);
                    final DashboardPageConfigDto subPage = new DashboardPageConfigDto(
                        subPageId,
                        cleanVal,
                        Collections.emptyList(),
                        pages.size(),
                        System.currentTimeMillis(),
                        effectiveIdentity,
                        subFilters,
                        target.getId(),
                        false,
                        cleanDim,
                        cleanVal,
                        scope
                    );
                    pages.add(subPage);
                }
            }
        }

        setCustomPages(pages);
        return converted;
    }

    /**
     * Backward-compatible overload for {@link #convertToGroupDashboard(String, String, WidgetPointScope, List, WidgetFilterConfigDto, List)}.
     *
     * @param pageId ID of the page to convert
     * @param groupType grouping dimension
     * @param groupCardScope evaluation scope for group cards
     * @param groupValues discovered group values to generate sub-dashboards for
     * @return updated DashboardPageConfigDto, or null if page not found
     */
    public synchronized DashboardPageConfigDto convertToGroupDashboard(
        final String pageId,
        final String groupType,
        final WidgetPointScope groupCardScope,
        final List<String> groupValues)
    {
        return convertToGroupDashboard(pageId, groupType, groupCardScope, null, null, groupValues);
    }

    /**
     * Deletes a custom dashboard page by ID.
     * If the page is a group parent, also cascades deletion recursively to all descendant sub-dashboards.
     *
     * @param pageId page ID to delete
     * @return true if page was deleted, false if not found
     */
    public synchronized boolean deleteCustomPage(final String pageId)
    {
        if (pageId == null || pageId.isBlank())
        {
            return false;
        }
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        final Set<String> idsToDelete = new LinkedHashSet<>();
        collectDescendantIds(pageId, pages, idsToDelete);
        idsToDelete.add(pageId);
        final boolean removed = pages.removeIf(p -> idsToDelete.contains(p.getId()));
        if (removed)
        {
            setCustomPages(pages);
        }
        return removed;
    }

    private void collectDescendantIds(final String parentId, final List<DashboardPageConfigDto> all, final Set<String> result)
    {
        for (final DashboardPageConfigDto p : all)
        {
            if (parentId.equals(p.getParentPageId()) && result.add(p.getId()))
            {
                collectDescendantIds(p.getId(), all, result);
            }
        }
    }

    /**
     * Retrieves the widgets configured for the given page ID.
     * If pageId is null or blank, retrieves the default dashboard widgets.
     *
     * @param pageId optional page ID
     * @return list of widget configurations
     */
    public synchronized List<DashboardWidgetConfigDto> getWidgetsForPage(final String pageId)
    {
        if (pageId == null || pageId.isBlank() || "default".equalsIgnoreCase(pageId.trim()))
        {
            return getDashboardWidgets();
        }
        final DashboardPageConfigDto page = getCustomPage(pageId);
        return (page != null && page.getWidgets() != null) ? page.getWidgets() : Collections.emptyList();
    }

    /**
     * Persists widgets for a specific page.
     * If pageId is null, blank, or "default", persists to the default dashboard widgets.
     *
     * @param pageId optional page ID
     * @param widgets widgets to persist
     */
    public synchronized void setWidgetsForPage(final String pageId, final List<DashboardWidgetConfigDto> widgets)
    {
        if (pageId == null || pageId.isBlank() || "default".equalsIgnoreCase(pageId.trim()))
        {
            setDashboardWidgets(widgets);
            return;
        }
        final List<DashboardPageConfigDto> pages = new ArrayList<>(getCustomPages());
        for (int i = 0; i < pages.size(); i++)
        {
            if (pageId.equals(pages.get(i).getId()))
            {
                final List<DashboardWidgetConfigDto> list = (widgets != null) ? new ArrayList<>(widgets) : new ArrayList<>();
                final List<DashboardWidgetConfigDto> ordered = new ArrayList<>();
                for (int j = 0; j < list.size(); j++)
                {
                    ordered.add(list.get(j).withSortOrder(j));
                }
                pages.set(i, pages.get(i).withWidgets(ordered));
                setCustomPages(pages);
                return;
            }
        }
    }

    /**
     * Copies widgets configuration from a source dashboard to a target dashboard.
     * Generates fresh unique widget IDs (and recursive IDs for container children)
     * to keep dashboards completely independent.
     *
     * @param sourcePageId ID of the source dashboard page (or null/"default" for default dashboard)
     * @param targetPageId ID of the target custom dashboard page (or null/"default" for default dashboard)
     * @param append true to append to target's existing widgets, false to replace them
     * @return the updated list of widgets on the target page
     */
    public synchronized List<DashboardWidgetConfigDto> copyWidgets(
        final String sourcePageId,
        final String targetPageId,
        final boolean append)
    {
        final String normalizedSourceId = (sourcePageId != null && !sourcePageId.isBlank() && !"default".equalsIgnoreCase(sourcePageId.trim()))
            ? sourcePageId.trim()
            : null;
        final String normalizedTargetId = (targetPageId != null && !targetPageId.isBlank() && !"default".equalsIgnoreCase(targetPageId.trim()))
            ? targetPageId.trim()
            : null;

        if (Objects.equals(normalizedSourceId, normalizedTargetId))
        {
            throw new IllegalArgumentException("Source and target dashboard cannot be identical");
        }

        final List<DashboardWidgetConfigDto> sourceWidgets = getWidgetsForPage(normalizedSourceId);
        if (sourceWidgets == null || sourceWidgets.isEmpty())
        {
            throw new IllegalArgumentException("Source dashboard has no widgets to copy");
        }

        if (normalizedTargetId != null && getCustomPage(normalizedTargetId) == null)
        {
            throw new IllegalArgumentException("Target dashboard not found: " + normalizedTargetId);
        }

        final List<DashboardWidgetConfigDto> clonedSourceWidgets = sourceWidgets.stream()
            .map(DashboardWidgetConfigDto::cloneWithNewId)
            .collect(Collectors.toList());

        final List<DashboardWidgetConfigDto> finalWidgets = new ArrayList<>();
        if (append)
        {
            final List<DashboardWidgetConfigDto> existingTargetWidgets = getWidgetsForPage(normalizedTargetId);
            if (existingTargetWidgets != null)
            {
                finalWidgets.addAll(existingTargetWidgets);
            }
        }

        final int startIndex = finalWidgets.size();
        for (int i = 0; i < clonedSourceWidgets.size(); i++)
        {
            finalWidgets.add(clonedSourceWidgets.get(i).withSortOrder(startIndex + i));
        }

        setWidgetsForPage(normalizedTargetId, finalWidgets);
        return Collections.unmodifiableList(finalWidgets);
    }

    /**
     * Backward-compatible overload for {@link #copyWidgets(String, String, boolean)},
     * defaulting to Replace mode (replaces target's existing widgets).
     *
     * @param sourcePageId ID of the source dashboard page
     * @param targetPageId ID of the target dashboard page
     * @return the updated list of widgets on the target page
     */
    public synchronized List<DashboardWidgetConfigDto> copyWidgets(
        final String sourcePageId,
        final String targetPageId)
    {
        return copyWidgets(sourcePageId, targetPageId, false);
    }

    /**
     * Adds or updates a widget on the given page.
     *
     * @param pageId optional page ID
     * @param widget widget to add or update
     */
    public synchronized void addOrUpdateWidget(final String pageId, final DashboardWidgetConfigDto widget)
    {
        if (pageId == null || pageId.isBlank())
        {
            addOrUpdateWidget(widget);
            return;
        }
        if (widget == null)
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++)
        {
            if (current.get(i).getId().equals(widget.getId()))
            {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0)
        {
            current.set(existingIndex, widget.withSortOrder(existingIndex));
        }
        else
        {
            boolean updatedChild = false;
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.isContainer() && w.hasChildren())
                {
                    final List<DashboardWidgetConfigDto> children = new ArrayList<>(w.getChildren());
                    for (int j = 0; j < children.size(); j++)
                    {
                        if (children.get(j).getId().equals(widget.getId()))
                        {
                            children.set(j, widget);
                            current.set(i, w.withChildren(children));
                            updatedChild = true;
                            break;
                        }
                    }
                    if (updatedChild)
                    {
                        break;
                    }
                }
            }
            if (!updatedChild)
            {
                current.add(widget.withSortOrder(current.size()));
            }
        }
        setWidgetsForPage(pageId, current);
    }

    /**
     * Deletes a widget from the given page.
     *
     * @param pageId optional page ID
     * @param widgetId widget ID to delete
     */
    public synchronized void deleteWidget(final String pageId, final String widgetId)
    {
        if (pageId == null || pageId.isBlank())
        {
            deleteWidget(widgetId);
            return;
        }
        if (widgetId == null || widgetId.isBlank())
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        current.removeIf(w -> w.getId().equals(widgetId));
        setWidgetsForPage(pageId, current);
    }

    /**
     * Reorders widgets on the given page.
     *
     * @param pageId optional page ID
     * @param widgetIds sequence of widget IDs
     */
    public synchronized void reorderWidgets(final String pageId, final List<String> widgetIds)
    {
        if (pageId == null || pageId.isBlank())
        {
            reorderWidgets(widgetIds);
            return;
        }
        if (widgetIds == null || widgetIds.isEmpty())
        {
            return;
        }
        final List<DashboardWidgetConfigDto> current = getWidgetsForPage(pageId);
        final Map<String, DashboardWidgetConfigDto> byId = new LinkedHashMap<>();
        for (final DashboardWidgetConfigDto w : current)
        {
            byId.put(w.getId(), w);
        }
        final List<DashboardWidgetConfigDto> reordered = new ArrayList<>();
        for (final String id : widgetIds)
        {
            if (byId.containsKey(id))
            {
                reordered.add(byId.remove(id));
            }
        }
        reordered.addAll(byId.values());
        setWidgetsForPage(pageId, reordered);
    }

    /**
     * Reorders the child widgets of a container widget on the given page according to the provided child ID sequence.
     *
     * @param pageId optional page ID
     * @param containerId ID of the container widget
     * @param childIds sequence of child widget IDs
     * @return true if the container was found and reordered, false otherwise
     */
    public synchronized boolean reorderContainerChildren(final String pageId, final String containerId, final List<String> childIds)
    {
        if (pageId == null || pageId.isBlank())
        {
            return reorderContainerChildren(containerId, childIds);
        }
        if (containerId == null || containerId.isBlank() || childIds == null || childIds.isEmpty())
        {
            return false;
        }
        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(containerId))
            {
                container = w;
                targetIdx = i;
                break;
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return false;
        }

        final DashboardWidgetConfigDto updatedContainer = container.reorderChildren(childIds);
        current.set(targetIdx, updatedContainer);
        setWidgetsForPage(pageId, current);
        return true;
    }

    /**
     * Resets widgets on the given page.
     * For custom pages, clears all widgets (returns to empty page).
     * For default page (pageId is null/blank), resets to default widgets.
     *
     * @param pageId optional page ID
     */
    public synchronized void resetWidgetsForPage(final String pageId)
    {
        if (pageId == null || pageId.isBlank())
        {
            resetDefaultWidgets();
            return;
        }
        setWidgetsForPage(pageId, Collections.emptyList());
    }

    /**
     * Unites two dashboard widgets on the specified page into a single container widget.
     *
     * @param pageId optional page ID
     * @param targetWidgetId ID of the target widget (or container)
     * @param sourceWidgetId ID of the source widget being dropped
     * @return the combined container widget configuration, or null if not found
     */
    public synchronized DashboardWidgetConfigDto combineWidgets(final String pageId, final String targetWidgetId, final String sourceWidgetId)
    {
        if (pageId == null || pageId.isBlank())
        {
            return combineWidgets(targetWidgetId, sourceWidgetId);
        }
        if (targetWidgetId == null || sourceWidgetId == null || targetWidgetId.equals(sourceWidgetId))
        {
            return null;
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        DashboardWidgetConfigDto target = null;
        DashboardWidgetConfigDto source = null;
        int targetIdx = -1;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(targetWidgetId))
            {
                target = w;
                targetIdx = i;
            }
            if (w.getId().equals(sourceWidgetId))
            {
                source = w;
            }
        }

        if (target == null || source == null || target.isRuns() || source.isRuns())
        {
            return null;
        }

        current.remove(source);
        targetIdx = current.indexOf(target);

        final DashboardWidgetConfigDto container;
        if (target.isContainer())
        {
            container = target.addChild(source);
        }
        else
        {
            final String containerId = "container-" + UUID.randomUUID().toString().substring(0, 8);
            final String title = target.getTitle() + " & " + source.getTitle();
            container = DashboardWidgetConfigDto.createContainer(
                containerId,
                title,
                List.of(target, source),
                target.getSortOrder()
            );
        }

        current.set(targetIdx, container);
        setWidgetsForPage(pageId, current);
        return container;
    }

    /**
     * Separates a container widget on the specified page back into its individual child widgets.
     *
     * @param pageId optional page ID
     * @param containerId ID of the container widget to separate
     * @return list of separated child widgets, or empty list if not found
     */
    public synchronized List<DashboardWidgetConfigDto> separateContainer(final String pageId, final String containerId)
    {
        if (pageId == null || pageId.isBlank())
        {
            return separateContainer(containerId);
        }
        if (containerId == null || containerId.isBlank())
        {
            return Collections.emptyList();
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        for (int i = 0; i < current.size(); i++)
        {
            final DashboardWidgetConfigDto w = current.get(i);
            if (w.getId().equals(containerId))
            {
                container = w;
                targetIdx = i;
                break;
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return Collections.emptyList();
        }

        final List<DashboardWidgetConfigDto> children = new ArrayList<>(container.getChildren());
        current.remove(targetIdx);
        for (int j = 0; j < children.size(); j++)
        {
            current.add(targetIdx + j, children.get(j));
        }

        setWidgetsForPage(pageId, current);
        return Collections.unmodifiableList(children);
    }

    /**
     * Separates a single child widget out of a container on the specified page.
     *
     * @param pageId optional page ID
     * @param containerId optional container ID
     * @param childWidgetId ID of the child widget to extract
     * @return the extracted child widget configuration, or null if not found
     */
    public synchronized DashboardWidgetConfigDto separateChildWidget(final String pageId, final String containerId, final String childWidgetId)
    {
        if (pageId == null || pageId.isBlank())
        {
            return separateChildWidget(containerId, childWidgetId);
        }
        if (childWidgetId == null || childWidgetId.isBlank())
        {
            return null;
        }

        final List<DashboardWidgetConfigDto> current = new ArrayList<>(getWidgetsForPage(pageId));
        int targetIdx = -1;
        DashboardWidgetConfigDto container = null;

        if (containerId != null && !containerId.isBlank())
        {
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.getId().equals(containerId))
                {
                    container = w;
                    targetIdx = i;
                    break;
                }
            }
        }

        if (container == null)
        {
            for (int i = 0; i < current.size(); i++)
            {
                final DashboardWidgetConfigDto w = current.get(i);
                if (w.isContainer() && w.hasChildren())
                {
                    final boolean hasChild = w.getChildren().stream().anyMatch(c -> c.getId().equals(childWidgetId));
                    if (hasChild)
                    {
                        container = w;
                        targetIdx = i;
                        break;
                    }
                }
            }
        }

        if (container == null || !container.isContainer() || !container.hasChildren())
        {
            return null;
        }

        DashboardWidgetConfigDto childToExtract = null;
        for (final DashboardWidgetConfigDto c : container.getChildren())
        {
            if (c.getId().equals(childWidgetId))
            {
                childToExtract = c;
                break;
            }
        }

        if (childToExtract == null)
        {
            return null;
        }

        final DashboardWidgetConfigDto updatedContainer = container.removeChild(childWidgetId);
        if (updatedContainer.getChildren().size() <= 1)
        {
            if (updatedContainer.getChildren().isEmpty())
            {
                current.set(targetIdx, childToExtract);
            }
            else
            {
                final DashboardWidgetConfigDto remainingChild = updatedContainer.getChildren().get(0);
                current.set(targetIdx, remainingChild);
                current.add(targetIdx + 1, childToExtract);
            }
        }
        else
        {
            final String updatedTitle;
            if (container.getTitle() != null && container.getTitle().contains(childToExtract.getTitle()))
            {
                final List<String> remainingTitles = updatedContainer.getChildren().stream()
                    .map(DashboardWidgetConfigDto::getTitle)
                    .collect(Collectors.toList());
                updatedTitle = String.join(" & ", remainingTitles);
            }
            else
            {
                updatedTitle = container.getTitle();
            }

            final DashboardWidgetConfigDto updatedWithTitle = updatedContainer.withTitle(updatedTitle);
            current.set(targetIdx, updatedWithTitle);
            current.add(targetIdx + 1, childToExtract);
        }

        setWidgetsForPage(pageId, current);
        return childToExtract;
    }
}
