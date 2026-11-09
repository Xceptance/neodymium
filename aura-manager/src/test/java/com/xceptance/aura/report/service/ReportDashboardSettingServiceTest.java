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

import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterChipDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.ReportSettingEntity;
import com.xceptance.aura.report.repository.ReportSettingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ReportDashboardSettingService}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class ReportDashboardSettingServiceTest
{
    private ReportSettingRepository repository;
    private Path tempSettingsFile;
    private ReportDashboardSettingService service;

    @BeforeEach
    public void setUp(@TempDir final Path tempDir)
    {
        repository = Mockito.mock(ReportSettingRepository.class);
        tempSettingsFile = tempDir.resolve("report-settings.json");
        service = new ReportDashboardSettingService(repository, tempSettingsFile.toString());
    }

    @Test
    public void testGetPinnedGroupingFromDatabase()
    {
        final ReportSettingEntity entity = new ReportSettingEntity(
            ReportDashboardSettingService.PINNED_GROUP_BY_KEY,
            "batch,environment"
        );
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.of(entity));

        final String pinned = service.getPinnedGrouping();
        Assertions.assertEquals("batch,environment", pinned);
    }

    @Test
    public void testGetPinnedGroupingFallbackToFile() throws IOException
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.empty());

        Files.writeString(tempSettingsFile, "{\"pinned_group_by\":\"locale,browser\"}");

        final String pinned = service.getPinnedGrouping();
        Assertions.assertEquals("locale,browser", pinned);

        // Verify it backfills the database
        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals("pinned_group_by", captor.getValue().getKey());
        Assertions.assertEquals("locale,browser", captor.getValue().getValue());
    }

    @Test
    public void testSetPinnedGroupingPersistsToDbAndFile()
    {
        service.setPinnedGrouping("batch,environment");

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals("pinned_group_by", captor.getValue().getKey());
        Assertions.assertEquals("batch,environment", captor.getValue().getValue());

        Assertions.assertTrue(Files.exists(tempSettingsFile));
    }

    @Test
    public void testClearPinnedGroupingRemovesFromDbAndFile()
    {
        service.setPinnedGrouping("batch");
        service.clearPinnedGrouping();

        Mockito.verify(repository).deleteById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY);
    }

    @Test
    public void testTogglePinnedGrouping()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.empty());

        // Initial toggle -> sets pin
        final String newPinned = service.togglePinnedGrouping("environment");
        Assertions.assertEquals("environment", newPinned);

        // Mock repository returning the newly pinned value
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.PINNED_GROUP_BY_KEY, "environment")));

        // Second toggle on same grouping -> unpins
        final String afterSecondToggle = service.togglePinnedGrouping("environment");
        Assertions.assertNull(afterSecondToggle);
        Mockito.verify(repository).deleteById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY);
    }

    @Test
    public void testIsGroupingPinnedOrderIndependent()
    {
        final ReportSettingEntity entity = new ReportSettingEntity(
            ReportDashboardSettingService.PINNED_GROUP_BY_KEY,
            "batch,environment"
        );
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.of(entity));

        Assertions.assertTrue(service.isGroupingPinned("batch,environment"));
        Assertions.assertTrue(service.isGroupingPinned("environment,batch"));
        Assertions.assertFalse(service.isGroupingPinned("batch"));
        Assertions.assertFalse(service.isGroupingPinned("locale"));
        Assertions.assertFalse(service.isGroupingPinned("none"));
    }

    @Test
    public void testNormalizeGrouping()
    {
        Assertions.assertEquals("batch", ReportDashboardSettingService.normalizeGrouping("batch"));
        Assertions.assertEquals("batch,environment", ReportDashboardSettingService.normalizeGrouping("batch, env"));
        Assertions.assertNull(ReportDashboardSettingService.normalizeGrouping(null));
        Assertions.assertNull(ReportDashboardSettingService.normalizeGrouping(""));
        Assertions.assertNull(ReportDashboardSettingService.normalizeGrouping("none"));
        Assertions.assertNull(ReportDashboardSettingService.normalizeGrouping("all"));
    }

    @Test
    public void testGetExecutionIdentityDimensionsDefault()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());

        final List<String> dimensions = service.getExecutionIdentityDimensions();
        Assertions.assertEquals(List.of("browser"), dimensions);
        Assertions.assertTrue(service.isExecutionIdentityDimensionActive("browser"));
        Assertions.assertFalse(service.isExecutionIdentityDimensionActive("locale"));
        Assertions.assertFalse(service.isExecutionIdentityDimensionActive("environment"));
    }

    @Test
    public void testGetExecutionIdentityDimensionsFromDatabase()
    {
        final ReportSettingEntity entity = new ReportSettingEntity(
            ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY,
            "browser,locale,environment,release"
        );
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.of(entity));

        final List<String> dimensions = service.getExecutionIdentityDimensions();
        Assertions.assertEquals(List.of("browser", "locale", "environment", "release"), dimensions);
        Assertions.assertTrue(service.isExecutionIdentityDimensionActive("environment"));
        Assertions.assertTrue(service.isExecutionIdentityDimensionActive("release"));
    }

    @Test
    public void testGetExecutionIdentityDimensionsFallbackToLegacyKey()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        final ReportSettingEntity legacyEntity = new ReportSettingEntity(
            ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY,
            "browser,locale,environment"
        );
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.of(legacyEntity));

        final List<String> dimensions = service.getExecutionIdentityDimensions();
        Assertions.assertEquals(List.of("browser", "locale", "environment"), dimensions);
        Assertions.assertTrue(service.isExecutionIdentityDimensionActive("environment"));
    }

    @Test
    public void testSetExecutionIdentityDimensionsPersistsBothNewAndLegacyKeys()
    {
        service.setExecutionIdentityDimensions(List.of("browser", "locale", "environment"));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final List<ReportSettingEntity> saved = captor.getAllValues();
        Assertions.assertTrue(saved.stream().anyMatch(e -> ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY.equals(e.getKey()) && "browser,locale,environment".equals(e.getValue())));
        Assertions.assertTrue(saved.stream().anyMatch(e -> ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY.equals(e.getKey()) && "browser,locale,environment".equals(e.getValue())));

        Assertions.assertTrue(Files.exists(tempSettingsFile));
    }

    @Test
    public void testToggleExecutionIdentityDimension()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());

        // Default has browser. Toggle environment -> adds environment
        final List<String> withEnv = service.toggleExecutionIdentityDimension("environment");
        Assertions.assertEquals(List.of("browser", "environment"), withEnv);

        // Toggling browser must be a no-op because browser is locked as a mandatory base coordinate
        final List<String> afterBrowserToggle = service.toggleExecutionIdentityDimension("browser");
        Assertions.assertTrue(afterBrowserToggle.contains("browser"));
        Assertions.assertEquals(List.of("browser", "environment"), afterBrowserToggle);
    }

    @Test
    public void testParseExecutionIdentityDimensions()
    {
        Assertions.assertEquals(List.of("browser", "locale"), ReportDashboardSettingService.parseExecutionIdentityDimensions("browser, locale"));
        Assertions.assertEquals(List.of("browser", "locale", "environment"), ReportDashboardSettingService.parseExecutionIdentityDimensions("browser, locale, environment, env"));
        Assertions.assertEquals(List.of("browser"), ReportDashboardSettingService.parseExecutionIdentityDimensions(null));
        Assertions.assertEquals(List.of("browser"), ReportDashboardSettingService.parseExecutionIdentityDimensions(""));
    }

    @Test
    public void testIsDefaultExecutionIdentity()
    {
        Assertions.assertTrue(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of("browser")));
        Assertions.assertTrue(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of("Browser")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of("browser", "locale")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of("locale")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of("browser", "environment")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultExecutionIdentity(List.of()));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultExecutionIdentity(null));
    }

    @Test
    public void testVariationsApplyIdentityDefaultsToFalse()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_IDENTITY_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_UNIQUENESS_KEY))
            .thenReturn(Optional.empty());

        Assertions.assertFalse(service.isVariationsApplyIdentity());
    }

    @Test
    public void testSetAndToggleVariationsApplyIdentity()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_IDENTITY_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_UNIQUENESS_KEY))
            .thenReturn(Optional.empty());

        final boolean toggled = service.toggleVariationsApplyIdentity();
        Assertions.assertTrue(toggled);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final List<ReportSettingEntity> saved = captor.getAllValues();
        Assertions.assertTrue(saved.stream().anyMatch(e -> ReportDashboardSettingService.VARIATIONS_APPLY_IDENTITY_KEY.equals(e.getKey()) && "true".equals(e.getValue())));
    }

    @Test
    public void testGetUniquenessDimensionsDefault()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());

        final List<String> dimensions = service.getUniquenessDimensions();
        Assertions.assertEquals(List.of("browser"), dimensions);
        Assertions.assertTrue(service.isUniquenessDimensionActive("browser"));
        Assertions.assertFalse(service.isUniquenessDimensionActive("locale"));
        Assertions.assertFalse(service.isUniquenessDimensionActive("environment"));
    }

    @Test
    public void testGetUniquenessDimensionsFromDatabase()
    {
        final ReportSettingEntity entity = new ReportSettingEntity(
            ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY,
            "browser,locale,environment,release"
        );
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.of(entity));

        final List<String> dimensions = service.getUniquenessDimensions();
        Assertions.assertEquals(List.of("browser", "locale", "environment", "release"), dimensions);
        Assertions.assertTrue(service.isUniquenessDimensionActive("environment"));
        Assertions.assertTrue(service.isUniquenessDimensionActive("release"));
    }

    @Test
    public void testGetUniquenessDimensionsFallbackToFile() throws IOException
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());

        Files.writeString(tempSettingsFile, "{\"uniqueness_dimensions\":\"browser,locale,environment\"}");

        final List<String> dimensions = service.getUniquenessDimensions();
        Assertions.assertEquals(List.of("browser", "locale", "environment"), dimensions);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals("execution_identity_dimensions", captor.getValue().getKey());
        Assertions.assertEquals("browser,locale,environment", captor.getValue().getValue());
    }

    @Test
    public void testSetUniquenessDimensionsPersistsToDbAndFile()
    {
        service.setUniquenessDimensions(List.of("browser", "locale", "environment"));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final List<ReportSettingEntity> saved = captor.getAllValues();
        Assertions.assertTrue(saved.stream().anyMatch(e -> ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY.equals(e.getKey()) && "browser,locale,environment".equals(e.getValue())));

        Assertions.assertTrue(Files.exists(tempSettingsFile));
    }

    @Test
    public void testToggleUniquenessDimension()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.UNIQUENESS_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());

        // Default has browser. Toggle environment -> adds environment
        final List<String> withEnv = service.toggleUniquenessDimension("environment");
        Assertions.assertEquals(List.of("browser", "environment"), withEnv);

        // Mock repository returning the updated value
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY, "browser,environment")));

        // Toggle environment again -> removes environment
        final List<String> withoutEnv = service.toggleUniquenessDimension("environment");
        Assertions.assertEquals(List.of("browser"), withoutEnv);
    }

    @Test
    public void testParseUniquenessDimensions()
    {
        Assertions.assertEquals(List.of("browser", "locale"), ReportDashboardSettingService.parseUniquenessDimensions("browser, locale"));
        Assertions.assertEquals(List.of("browser", "locale", "environment"), ReportDashboardSettingService.parseUniquenessDimensions("browser, locale, environment, env"));
        Assertions.assertEquals(List.of("browser"), ReportDashboardSettingService.parseUniquenessDimensions(null));
        Assertions.assertEquals(List.of("browser"), ReportDashboardSettingService.parseUniquenessDimensions(""));
    }

    @Test
    public void testIsDefaultUniqueness()
    {
        Assertions.assertTrue(ReportDashboardSettingService.isDefaultUniqueness(List.of("browser")));
        Assertions.assertTrue(ReportDashboardSettingService.isDefaultUniqueness(List.of("Browser")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultUniqueness(List.of("browser", "locale")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultUniqueness(List.of("locale")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultUniqueness(List.of("browser", "environment")));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultUniqueness(List.of()));
        Assertions.assertFalse(ReportDashboardSettingService.isDefaultUniqueness(null));
    }

    @Test
    public void testVariationsApplyUniquenessDefaultsToFalse()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_UNIQUENESS_KEY))
            .thenReturn(Optional.empty());

        Assertions.assertFalse(service.isVariationsApplyUniqueness());
    }

    @Test
    public void testSetAndToggleVariationsApplyUniqueness()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.EXECUTION_IDENTITY_DIMENSIONS_KEY))
            .thenReturn(Optional.empty());
        Mockito.when(repository.findById(ReportDashboardSettingService.VARIATIONS_APPLY_UNIQUENESS_KEY))
            .thenReturn(Optional.empty());

        final boolean toggled = service.toggleVariationsApplyUniqueness();
        Assertions.assertTrue(toggled);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final List<ReportSettingEntity> saved = captor.getAllValues();
        Assertions.assertTrue(saved.stream().anyMatch(e -> ReportDashboardSettingService.VARIATIONS_APPLY_UNIQUENESS_KEY.equals(e.getKey()) && "true".equals(e.getValue())));
    }

    @Test
    public void testGetPinnedInnerGroupingFromDatabase()
    {
        final ReportSettingEntity entity = new ReportSettingEntity(
            "pinned_inner_group_by:environment",
            "browser"
        );
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.of(entity));

        final String pinned = service.getPinnedInnerGrouping("environment");
        Assertions.assertEquals("browser", pinned);
    }

    @Test
    public void testGetPinnedInnerGroupingFallbackToFile() throws IOException
    {
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.empty());

        Files.writeString(tempSettingsFile, "{\"pinned_inner_group_by:environment\":\"locale\"}");

        final String pinned = service.getPinnedInnerGrouping("environment");
        Assertions.assertEquals("locale", pinned);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals("pinned_inner_group_by:environment", captor.getValue().getKey());
        Assertions.assertEquals("locale", captor.getValue().getValue());
    }

    @Test
    public void testSetPinnedInnerGroupingPersistsToDbAndFile()
    {
        service.setPinnedInnerGrouping("environment", "browser");

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals("pinned_inner_group_by:environment", captor.getValue().getKey());
        Assertions.assertEquals("browser", captor.getValue().getValue());

        Assertions.assertTrue(Files.exists(tempSettingsFile));
    }

    @Test
    public void testClearPinnedInnerGroupingRemovesFromDbAndFile() throws IOException
    {
        Files.writeString(tempSettingsFile, "{\"pinned_inner_group_by:environment\":\"browser\"}");

        service.clearPinnedInnerGrouping("environment");

        Mockito.verify(repository).deleteById("pinned_inner_group_by:environment");
        Assertions.assertTrue(Files.exists(tempSettingsFile));
        Assertions.assertFalse(Files.readString(tempSettingsFile).contains("pinned_inner_group_by:environment"));
    }

    @Test
    public void testTogglePinnedInnerGrouping()
    {
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.empty());

        // Currently unpinned -> pins "browser"
        final String pinned = service.togglePinnedInnerGrouping("environment", "browser");
        Assertions.assertEquals("browser", pinned);

        // Currently pinned to "browser" -> unpins (returns null)
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.of(new ReportSettingEntity("pinned_inner_group_by:environment", "browser")));

        final String unpinned = service.togglePinnedInnerGrouping("environment", "browser");
        Assertions.assertNull(unpinned);

        // Currently unpinned -> toggle with "locale" pins "locale"
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.empty());

        final String switched = service.togglePinnedInnerGrouping("environment", "locale");
        Assertions.assertEquals("locale", switched);
    }

    @Test
    public void testIsInnerGroupingPinned()
    {
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.of(new ReportSettingEntity("pinned_inner_group_by:environment", "browser")));

        Assertions.assertTrue(service.isInnerGroupingPinned("environment", "browser"));
        Assertions.assertFalse(service.isInnerGroupingPinned("environment", "locale"));
        Assertions.assertFalse(service.isInnerGroupingPinned("environment", null));
    }

    @Test
    public void testContextAwareIsGroupingPinnedAndToggle()
    {
        Mockito.when(repository.findById("pinned_inner_group_by:environment"))
            .thenReturn(Optional.of(new ReportSettingEntity("pinned_inner_group_by:environment", "browser")));

        Assertions.assertTrue(service.isGroupingPinned("environment", "browser"));
        Assertions.assertFalse(service.isGroupingPinned("environment", "locale"));

        // When groupType is null or "all", it checks top-level grouping
        Mockito.when(repository.findById(ReportDashboardSettingService.PINNED_GROUP_BY_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.PINNED_GROUP_BY_KEY, "environment")));

        Assertions.assertTrue(service.isGroupingPinned(null, "environment"));
        Assertions.assertTrue(service.isGroupingPinned("all", "environment"));
        Assertions.assertFalse(service.isGroupingPinned(null, "browser"));
    }

    @Test
    public void testDefaultWidgetsWhenUnconfigured()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.empty());

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(4, widgets.size());
        Assertions.assertEquals(WidgetMetric.STATUS, widgets.get(0).getMetric());
        Assertions.assertEquals(WidgetPresentation.PIE_CHART, widgets.get(0).getPresentation());
        Assertions.assertFalse(service.isCustomWidgetsConfigured());
    }

    @Test
    public void testGetDashboardWidgetsFromDatabase()
    {
        final String json = "[{\"id\":\"w-1\",\"title\":\"Bugs Pie\",\"metric\":\"BUGS\",\"presentation\":\"PIE_CHART\",\"trendMode\":\"BY_RUNS\",\"valueFormat\":\"PERCENT\",\"width\":\"half\",\"sortOrder\":0}]";
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, json)));

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("w-1", widgets.get(0).getId());
        Assertions.assertEquals("Bugs Pie", widgets.get(0).getTitle());
        Assertions.assertEquals(WidgetMetric.BUGS, widgets.get(0).getMetric());
    }

    @Test
    public void testSetDashboardWidgetsPersists()
    {
        final List<DashboardWidgetConfigDto> widgets = List.of(
            new DashboardWidgetConfigDto("w-custom-1", "Pass Rate Perlenkette", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE, WidgetTrendMode.DAILY, WidgetValueFormat.PERCENT, "full", 0)
        );

        service.setDashboardWidgets(widgets);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertEquals(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, captor.getValue().getKey());
        Assertions.assertTrue(captor.getValue().getValue().contains("w-custom-1"));
        Assertions.assertTrue(Files.exists(tempSettingsFile));
    }

    @Test
    public void testAddOrUpdateWidget()
    {
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.empty());

        final DashboardWidgetConfigDto newWidget = new DashboardWidgetConfigDto(
            "w-add-1",
            "Costs Hourly Trend",
            WidgetMetric.COSTS,
            "",
            WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.HOURLY,
            WidgetValueFormat.ABSOLUTE,
            "half",
            99
        );

        service.addOrUpdateWidget(newWidget);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertTrue(captor.getValue().getValue().contains("w-add-1"));
    }

    @Test
    public void testDeleteWidget()
    {
        final String json = "[{\"id\":\"w-1\",\"title\":\"W1\",\"metric\":\"BUGS\",\"presentation\":\"PIE_CHART\",\"trendMode\":\"BY_RUNS\",\"valueFormat\":\"PERCENT\",\"width\":\"half\",\"sortOrder\":0},{\"id\":\"w-2\",\"title\":\"W2\",\"metric\":\"STATUS\",\"presentation\":\"RATIO_BAR\",\"trendMode\":\"BY_RUNS\",\"valueFormat\":\"PERCENT\",\"width\":\"half\",\"sortOrder\":1}]";
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, json)));

        service.deleteWidget("w-1");

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertFalse(captor.getValue().getValue().contains("\"id\":\"w-1\""));
        Assertions.assertTrue(captor.getValue().getValue().contains("\"id\":\"w-2\""));
    }

    @Test
    public void testReorderWidgets()
    {
        final String json = "[{\"id\":\"w-1\",\"title\":\"W1\",\"metric\":\"BUGS\",\"presentation\":\"PIE_CHART\",\"trendMode\":\"BY_RUNS\",\"valueFormat\":\"PERCENT\",\"width\":\"half\",\"sortOrder\":0},{\"id\":\"w-2\",\"title\":\"W2\",\"metric\":\"STATUS\",\"presentation\":\"RATIO_BAR\",\"trendMode\":\"BY_RUNS\",\"valueFormat\":\"PERCENT\",\"width\":\"half\",\"sortOrder\":1}]";
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, json)));

        service.reorderWidgets(List.of("w-2", "w-1"));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        final String saved = captor.getValue().getValue();
        final int idxW2 = saved.indexOf("w-2");
        final int idxW1 = saved.indexOf("w-1");
        Assertions.assertTrue(idxW2 < idxW1, "w-2 should precede w-1 in reordered output");
    }

    @Test
    public void testResetDefaultWidgets() throws IOException
    {
        Files.writeString(tempSettingsFile, "{\"dashboard_widgets_config\":\"[]\"}");

        service.resetDefaultWidgets();

        Mockito.verify(repository).deleteById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY);
        final String content = Files.readString(tempSettingsFile);
        Assertions.assertFalse(content.contains(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY));
    }

    @Test
    public void testDashboardWidgetsJson_IgnoresUnknownPropertiesAndFullWidth()
    {
        // JSON containing legacy 'fullWidth' and arbitrary unknown future properties
        final String legacyJson = "[{"
            + "\"id\":\"w-legacy-1\","
            + "\"title\":\"Legacy Full Width Widget\","
            + "\"metric\":\"STATUS\","
            + "\"tagKey\":\"\","
            + "\"presentation\":\"TREND_GRAPH\","
            + "\"trendMode\":\"DAILY\","
            + "\"valueFormat\":\"PERCENT\","
            + "\"width\":\"full\","
            + "\"sortOrder\":0,"
            + "\"fullWidth\":true,"
            + "\"unknownFutureProperty\":\"unexpected\""
            + "}]";

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, legacyJson)));

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("w-legacy-1", widgets.get(0).getId());
        Assertions.assertTrue(widgets.get(0).isFullWidth());

        // Now test serialization output does NOT emit fullWidth
        service.setDashboardWidgets(widgets);
        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        final String savedJson = captor.getValue().getValue();
        Assertions.assertFalse(savedJson.contains("\"fullWidth\""), "Serialized JSON must not contain calculated fullWidth property");
        Assertions.assertTrue(savedJson.contains("\"width\":\"full\""));

        // Verify deleteWidget operates cleanly without deserialization failure
        service.deleteWidget("w-legacy-1");
        final ArgumentCaptor<ReportSettingEntity> deleteCaptor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeast(2)).save(deleteCaptor.capture());
        Assertions.assertFalse(deleteCaptor.getValue().getValue().contains("w-legacy-1"));
    }

    @Test
    public void testDashboardWidgetsJson_HandlesMatrixPresentationAndUnknownEnumValuesGracefully()
    {
        final String json = "[{"
            + "\"id\":\"widget-valid-1\","
            + "\"title\":\"Status Pie\","
            + "\"metric\":\"STATUS\","
            + "\"presentation\":\"PIE_CHART\","
            + "\"sortOrder\":0"
            + "},{"
            + "\"id\":\"widget-matrix-22\","
            + "\"title\":\"Area Matrix\","
            + "\"metric\":\"TAG\","
            + "\"tagKey\":\"Area\","
            + "\"presentation\":\"MATRIX\","
            + "\"width\":\"full\","
            + "\"sortOrder\":1"
            + "},{"
            + "\"id\":\"widget-unknown-enum\","
            + "\"title\":\"Fallback Unknown\","
            + "\"metric\":\"UNKNOWN_METRIC\","
            + "\"presentation\":\"UNKNOWN_PRESENTATION\","
            + "\"sortOrder\":2"
            + "}]";

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, json)));

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(3, widgets.size(), "All 3 widgets must be loaded without discarding list");

        Assertions.assertEquals("widget-valid-1", widgets.get(0).getId());
        Assertions.assertEquals(WidgetPresentation.PIE_CHART, widgets.get(0).getPresentation());

        Assertions.assertEquals("widget-matrix-22", widgets.get(1).getId());
        Assertions.assertEquals(WidgetPresentation.TEST_VARIATIONS, widgets.get(1).getPresentation());

        Assertions.assertEquals("widget-unknown-enum", widgets.get(2).getId());
        Assertions.assertEquals(WidgetPresentation.PIE_CHART, widgets.get(2).getPresentation());
        Assertions.assertEquals(WidgetMetric.STATUS, widgets.get(2).getMetric());
    }

    @Test
    public void testDashboardWidgetsJson_QuarterWidthSerializationAndDeserialization()
    {
        final DashboardWidgetConfigDto widget1 = new DashboardWidgetConfigDto(
            "w-quarter-1", "Quarter Widget 1", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "quarter", 0, false
        );

        final DashboardWidgetConfigDto widget2 = new DashboardWidgetConfigDto(
            "w-quarter-2", "Quarter Widget 2", WidgetMetric.BUGS, "",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN, "25%", 1, false
        );

        Assertions.assertTrue(widget1.isQuarterWidth());
        Assertions.assertFalse(widget1.isHalfWidth());
        Assertions.assertFalse(widget1.isFullWidth());
        Assertions.assertEquals("quarter", widget1.getWidth());

        Assertions.assertTrue(widget2.isQuarterWidth(), "25% width must normalize to quarter");
        Assertions.assertFalse(widget2.isHalfWidth());
        Assertions.assertFalse(widget2.isFullWidth());
        Assertions.assertEquals("quarter", widget2.getWidth());

        service.setDashboardWidgets(List.of(widget1, widget2));
        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        final String savedJson = captor.getValue().getValue();

        Assertions.assertTrue(savedJson.contains("\"width\":\"quarter\""));
        Assertions.assertFalse(savedJson.contains("\"quarterWidth\""), "Serialized JSON must not contain quarterWidth getter");
        Assertions.assertFalse(savedJson.contains("\"halfWidth\""), "Serialized JSON must not contain halfWidth getter");
        Assertions.assertFalse(savedJson.contains("\"fullWidth\""), "Serialized JSON must not contain fullWidth getter");

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, savedJson)));

        final List<DashboardWidgetConfigDto> reloaded = service.getDashboardWidgets();
        Assertions.assertNotNull(reloaded);
        Assertions.assertEquals(2, reloaded.size());
        Assertions.assertTrue(reloaded.get(0).isQuarterWidth());
        Assertions.assertTrue(reloaded.get(1).isQuarterWidth());
    }

    @Test
    public void testEnsureColumnCapacityCallsRepositoryAlterQuery()
    {
        service.ensureColumnCapacity();
        Mockito.verify(repository).alterSettingValueColumnToText();
    }

    @Test
    public void testSetDashboardWidgetsWithLongJsonOver500Characters()
    {
        // 5 widgets which together produce JSON well exceeding 500 chars (approx 1000 chars)
        final List<DashboardWidgetConfigDto> longList = List.of(
            new DashboardWidgetConfigDto("w-long-1", "Pass Rate Perlenkette 1", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE, WidgetTrendMode.DAILY, WidgetValueFormat.PERCENT, "full", 0),
            new DashboardWidgetConfigDto("w-long-2", "Pass Rate Perlenkette 2", WidgetMetric.BUGS, "", WidgetPresentation.TREND_GRAPH, WidgetTrendMode.HOURLY, WidgetValueFormat.ABSOLUTE, "half", 1),
            new DashboardWidgetConfigDto("w-long-3", "Pass Rate Perlenkette 3", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "half", 2),
            new DashboardWidgetConfigDto("w-long-4", "Pass Rate Perlenkette 4", WidgetMetric.TAG, "browser", WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 3),
            new DashboardWidgetConfigDto("w-long-5", "Pass Rate Perlenkette 5", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 4)
        );

        service.setDashboardWidgets(longList);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository).save(captor.capture());
        final String savedJson = captor.getValue().getValue();
        Assertions.assertTrue(savedJson.length() > 500, "Serialized JSON length should be > 500 characters, was: " + savedJson.length());
        Assertions.assertTrue(savedJson.contains("w-long-5"));
    }

    @Test
    public void testDashboardWidgetsJson_PointScopeSerializationAndDeserialization()
    {
        // 1. Deserializing legacy JSON without pointScope defaults gracefully to LATEST_TEST_STATUS
        final String legacyJson = "[{"
            + "\"id\":\"w-legacy-ps\","
            + "\"title\":\"Status Donut\","
            + "\"metric\":\"STATUS\","
            + "\"tagKey\":\"\","
            + "\"presentation\":\"PIE_CHART\","
            + "\"trendMode\":\"LATEST_APPROACH\","
            + "\"valueFormat\":\"PERCENT\","
            + "\"width\":\"half\","
            + "\"sortOrder\":0"
            + "}]";

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, legacyJson)));

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, widgets.get(0).getPointScope());

        // 2. Custom pointScope LAST_RUN is serialized and deserialized properly
        final DashboardWidgetConfigDto customConfig = new DashboardWidgetConfigDto(
            "w-custom-ps",
            "Latest Run Only",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN,
            "half",
            0
        );

        service.setDashboardWidgets(List.of(customConfig));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final String savedJson = captor.getValue().getValue();
        Assertions.assertTrue(savedJson.contains("\"pointScope\":\"LAST_RUN\""), "Saved JSON must include pointScope LAST_RUN");

        // 3. Round-trip deserialization with explicit pointScope
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, savedJson)));

        final List<DashboardWidgetConfigDto> roundTrip = service.getDashboardWidgets();
        Assertions.assertEquals(1, roundTrip.size());
        Assertions.assertEquals(WidgetPointScope.LAST_RUN, roundTrip.get(0).getPointScope());
    }

    @Test
    public void testDashboardWidgets_ShowHealedAiSerializationAndDeserialization()
    {
        final DashboardWidgetConfigDto widgetWithHealed = new DashboardWidgetConfigDto(
            "w-healed-roundtrip",
            "Status With Healed",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            true
        );

        service.setDashboardWidgets(List.of(widgetWithHealed));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final String savedJson = captor.getValue().getValue();
        Assertions.assertTrue(savedJson.contains("\"showHealedAi\":true"), "Saved JSON must include showHealedAi:true");

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, savedJson)));

        final List<DashboardWidgetConfigDto> roundTrip = service.getDashboardWidgets();
        Assertions.assertEquals(1, roundTrip.size());
        Assertions.assertTrue(roundTrip.get(0).isShowHealedAi());
    }

    @Test
    public void testDashboardWidgets_OwnExecutionIdentityAndRunFiltersSerializationAndDeserialization()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"),
            Set.of("Safari"),
            Set.of("US"),
            Set.of("FR"),
            Set.of("Prod"),
            Set.of("Dev"),
            Set.of("Checkout"),
            Set.of("Cart"),
            Set.of("smoke"),
            Set.of("flaky"),
            Map.of("tier", Set.of("p1")),
            Map.of("tier", Set.of("p3"))
        );

        final DashboardWidgetConfigDto customConfig = new DashboardWidgetConfigDto(
            "w-custom-isolated",
            "Isolated Widget",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            true,
            List.of("browser", "locale", "tag:tier"),
            true,
            runFilters
        );

        Assertions.assertTrue(customConfig.hasOwnExecutionIdentity());
        Assertions.assertTrue(customConfig.hasOwnRunFilters());
        Assertions.assertEquals(List.of("browser", "locale", "tag:tier"), customConfig.getExecutionIdentityDimensions());
        Assertions.assertEquals(List.of("browser", "locale", "tag:tier"), customConfig.getEffectiveExecutionIdentityDimensions(List.of("browser")));

        Assertions.assertEquals(12, runFilters.getActiveFiltersCount());
        Assertions.assertEquals(12, runFilters.getFilterChips().size());
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "browser".equals(c.getCategory()) && "Chrome".equals(c.getLabel()) && !c.isExcluded()));
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "browser".equals(c.getCategory()) && "Excl. Safari".equals(c.getLabel()) && c.isExcluded()));
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "locale".equals(c.getCategory()) && "US".equals(c.getLabel()) && !c.isExcluded()));
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "locale".equals(c.getCategory()) && "Excl. FR".equals(c.getLabel()) && c.isExcluded()));
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "tag".equals(c.getCategory()) && "tier: p1".equals(c.getLabel()) && !c.isExcluded()));
        Assertions.assertTrue(runFilters.getFilterChips().stream().anyMatch(c -> "tag".equals(c.getCategory()) && "Excl. tier: p3".equals(c.getLabel()) && c.isExcluded()));

        service.setDashboardWidgets(List.of(customConfig));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final String savedJson = captor.getValue().getValue();

        Assertions.assertTrue(savedJson.contains("\"overrideExecutionIdentity\":true"), "JSON must include overrideExecutionIdentity:true");
        Assertions.assertTrue(savedJson.contains("\"tag:tier\""), "JSON must include custom identity dimension tag:tier");
        Assertions.assertTrue(savedJson.contains("\"overrideRunFilters\":true"), "JSON must include overrideRunFilters:true");
        Assertions.assertTrue(savedJson.contains("\"runFilters\":"), "JSON must include runFilters object");
        Assertions.assertTrue(savedJson.contains("\"Chrome\""), "JSON must include filter browser Chrome");
        Assertions.assertTrue(savedJson.contains("\"Safari\""), "JSON must include exclude browser Safari");
        Assertions.assertFalse(savedJson.contains("\"filterChips\""), "Serialized JSON must not contain calculated filterChips");
        Assertions.assertFalse(savedJson.contains("\"ownRunFilters\""), "Serialized JSON must not contain calculated ownRunFilters");
        Assertions.assertFalse(savedJson.contains("\"ownExecutionIdentity\""), "Serialized JSON must not contain calculated ownExecutionIdentity");

        // Round-trip deserialization
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, savedJson)));

        final List<DashboardWidgetConfigDto> roundTrip = service.getDashboardWidgets();
        Assertions.assertEquals(1, roundTrip.size());
        final DashboardWidgetConfigDto deserialized = roundTrip.get(0);
        Assertions.assertTrue(deserialized.hasOwnExecutionIdentity());
        Assertions.assertTrue(deserialized.hasOwnRunFilters());
        Assertions.assertEquals(List.of("browser", "locale", "tag:tier"), deserialized.getExecutionIdentityDimensions());
        Assertions.assertNotNull(deserialized.getRunFilters());
        Assertions.assertEquals(12, deserialized.getRunFilters().getActiveFiltersCount());
        Assertions.assertEquals(12, deserialized.getRunFilters().getFilterChips().size());
        Assertions.assertTrue(deserialized.getRunFilters().getBrowsers().contains("Chrome"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeBrowsers().contains("Safari"));
        Assertions.assertTrue(deserialized.getRunFilters().getLocales().contains("US"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeLocales().contains("FR"));
        Assertions.assertTrue(deserialized.getRunFilters().getEnvs().contains("Prod"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeEnvs().contains("Dev"));
        Assertions.assertTrue(deserialized.getRunFilters().getAreas().contains("Checkout"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeAreas().contains("Cart"));
        Assertions.assertTrue(deserialized.getRunFilters().getTags().contains("smoke"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeTags().contains("flaky"));
        Assertions.assertTrue(deserialized.getRunFilters().getTagsByType().containsKey("tier"));
        Assertions.assertTrue(deserialized.getRunFilters().getTagsByType().get("tier").contains("p1"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeTagsByType().containsKey("tier"));
        Assertions.assertTrue(deserialized.getRunFilters().getExcludeTagsByType().get("tier").contains("p3"));

        // Verify hasTagValue and getTagValuesCsv helper methods
        Assertions.assertTrue(deserialized.getRunFilters().hasTagValue("tier", "p1"));
        Assertions.assertTrue(deserialized.getRunFilters().hasTagValue("TIER", "P1"), "Matching must be case-insensitive");
        Assertions.assertFalse(deserialized.getRunFilters().hasTagValue("tier", "nonexistent"));
        Assertions.assertFalse(deserialized.getRunFilters().hasTagValue("unknownKey", "p1"));
        Assertions.assertEquals("p1", deserialized.getRunFilters().getTagValuesCsv("tier"));
        Assertions.assertEquals("p1", deserialized.getRunFilters().getTagValuesCsv("TIER"));
        Assertions.assertEquals("", deserialized.getRunFilters().getTagValuesCsv("unknownKey"));
    }

    @Test
    public void testDashboardWidgets_LegacyJsonWithoutIdentityOrRunFilters_DefaultsSafely()
    {
        final String legacyJson = "[{"
            + "\"id\":\"w-legacy-default\","
            + "\"title\":\"Legacy Widget\","
            + "\"metric\":\"STATUS\","
            + "\"presentation\":\"PIE_CHART\","
            + "\"trendMode\":\"LATEST_APPROACH\","
            + "\"valueFormat\":\"PERCENT\","
            + "\"width\":\"half\","
            + "\"sortOrder\":0"
            + "}]";

        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, legacyJson)));

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        final DashboardWidgetConfigDto w = widgets.get(0);
        Assertions.assertFalse(w.hasOwnExecutionIdentity());
        Assertions.assertFalse(w.isOverrideExecutionIdentity());
        Assertions.assertTrue(w.getExecutionIdentityDimensions().isEmpty());
        Assertions.assertFalse(w.hasOwnRunFilters());
        Assertions.assertFalse(w.isOverrideRunFilters());
        Assertions.assertEquals(List.of("browser"), w.getEffectiveExecutionIdentityDimensions(List.of("browser")));
    }

    @Test
    public void testDashboardWidgets_LimitAndTimestampFiltersSerializationAndDeserialization()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            10,
            "2026-10-01",
            "2026-10-31"
        );

        Assertions.assertTrue(runFilters.hasAnyFilters());
        Assertions.assertEquals(3, runFilters.getActiveFiltersCount());
        Assertions.assertEquals(10, runFilters.getLimit());
        Assertions.assertEquals("2026-10-01", runFilters.getStartDate());
        Assertions.assertEquals("2026-10-31", runFilters.getEndDate());
        Assertions.assertNotNull(runFilters.getFromTimestampMs());
        Assertions.assertNotNull(runFilters.getToTimestampMs());
        Assertions.assertTrue(runFilters.getToTimestampMs() > runFilters.getFromTimestampMs());

        final List<WidgetFilterChipDto> chips = runFilters.getFilterChips();
        Assertions.assertEquals(3, chips.size());
        Assertions.assertTrue(chips.stream().anyMatch(c -> "limit".equals(c.getCategory()) && "Last 10 Runs".equals(c.getLabel())));
        Assertions.assertTrue(chips.stream().anyMatch(c -> "date".equals(c.getCategory()) && "2026-10-01 \u2013 2026-10-31".equals(c.getLabel())));
        Assertions.assertTrue(chips.stream().anyMatch(c -> "browser".equals(c.getCategory()) && "Chrome".equals(c.getLabel())));

        final DashboardWidgetConfigDto customConfig = new DashboardWidgetConfigDto(
            "w-limit-date",
            "Limited Date Widget",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            false,
            Collections.emptyList(),
            true,
            runFilters
        );

        service.setDashboardWidgets(List.of(customConfig));

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        final String savedJson = captor.getValue().getValue();

        Assertions.assertTrue(savedJson.contains("\"limit\":10"), "JSON must include limit:10");
        Assertions.assertTrue(savedJson.contains("\"startDate\":\"2026-10-01\""), "JSON must include startDate");
        Assertions.assertTrue(savedJson.contains("\"endDate\":\"2026-10-31\""), "JSON must include endDate");
        Assertions.assertFalse(savedJson.contains("\"fromTimestampMs\""), "Calculated getFromTimestampMs must not be serialized");
        Assertions.assertFalse(savedJson.contains("\"toTimestampMs\""), "Calculated getToTimestampMs must not be serialized");

        // Round-trip deserialization
        Mockito.when(repository.findById(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY))
            .thenReturn(Optional.of(new ReportSettingEntity(ReportDashboardSettingService.DASHBOARD_WIDGETS_KEY, savedJson)));

        final List<DashboardWidgetConfigDto> roundTrip = service.getDashboardWidgets();
        Assertions.assertEquals(1, roundTrip.size());
        final DashboardWidgetConfigDto deserialized = roundTrip.get(0);
        Assertions.assertNotNull(deserialized.getRunFilters());
        Assertions.assertEquals(10, deserialized.getRunFilters().getLimit());
        Assertions.assertEquals("2026-10-01", deserialized.getRunFilters().getStartDate());
        Assertions.assertEquals("2026-10-31", deserialized.getRunFilters().getEndDate());
        Assertions.assertEquals(3, deserialized.getRunFilters().getActiveFiltersCount());
        Assertions.assertEquals(3, deserialized.getRunFilters().getFilterChips().size());
    }

    @Test
    public void testFlakySettingsDefaults()
    {
        Mockito.when(repository.findById(Mockito.anyString())).thenReturn(Optional.empty());

        Assertions.assertEquals(ReportDashboardSettingService.DEFAULT_FLAKY_MIN_UNKNOWN_FAILURES, service.getFlakyMinUnknownFailures());
        Assertions.assertEquals(ReportDashboardSettingService.DEFAULT_FLAKY_HISTORY_WINDOW, service.getFlakyHistoryWindow());
        Assertions.assertEquals(ReportDashboardSettingService.DEFAULT_FLAKY_MIN_FAILURE_RATE, service.getFlakyMinFailureRate());
    }

    @Test
    public void testFlakySettingsPersist()
    {
        service.setFlakyMinUnknownFailures(3);
        service.setFlakyHistoryWindow(15);
        service.setFlakyMinFailureRate(0.30);

        final ArgumentCaptor<ReportSettingEntity> captor = ArgumentCaptor.forClass(ReportSettingEntity.class);
        Mockito.verify(repository, Mockito.atLeast(3)).save(captor.capture());

        final List<ReportSettingEntity> savedEntities = captor.getAllValues();
        Assertions.assertTrue(savedEntities.stream().anyMatch(e -> ReportDashboardSettingService.FLAKY_MIN_UNKNOWN_FAILURES_KEY.equals(e.getKey()) && "3".equals(e.getValue())));
        Assertions.assertTrue(savedEntities.stream().anyMatch(e -> ReportDashboardSettingService.FLAKY_HISTORY_WINDOW_KEY.equals(e.getKey()) && "15".equals(e.getValue())));
        Assertions.assertTrue(savedEntities.stream().anyMatch(e -> ReportDashboardSettingService.FLAKY_MIN_FAILURE_RATE_KEY.equals(e.getKey()) && "0.3".equals(e.getValue())));
    }

    @Test
    public void testCombineWidgetsCreatesNewContainer()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Failure List", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        service.setDashboardWidgets(List.of(w1, w2));

        service.combineWidgets("w-target", "w-source");

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size(), "Two combined widgets should form a single container");
        final DashboardWidgetConfigDto container = widgets.get(0);
        Assertions.assertTrue(container.isContainer());
        Assertions.assertEquals(2, container.getChildren().size());
        Assertions.assertEquals("w-target", container.getChildren().get(0).getId());
        Assertions.assertEquals("w-source", container.getChildren().get(1).getId());
    }

    @Test
    public void testCombineWidgetsAppendsToExistingContainer()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(c1, c2);

        final DashboardWidgetConfigDto standalone = new DashboardWidgetConfigDto(
            "w-third", "Ratio Bar", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        service.setDashboardWidgets(List.of(container, standalone));

        service.combineWidgets(container.getId(), "w-third");

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        final DashboardWidgetConfigDto updatedContainer = widgets.get(0);
        Assertions.assertEquals(3, updatedContainer.getChildren().size());
        Assertions.assertEquals("w-third", updatedContainer.getChildren().get(2).getId());
    }

    @Test
    public void testSeparateContainerRestoresWidgetsInPlace()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(c1, c2);

        service.setDashboardWidgets(List.of(container));

        service.separateContainer(container.getId());

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(2, widgets.size(), "Container should be unpacked into 2 individual widgets");
        Assertions.assertEquals("c1", widgets.get(0).getId());
        Assertions.assertEquals("c2", widgets.get(1).getId());
        Assertions.assertFalse(widgets.get(0).isContainer());
        Assertions.assertFalse(widgets.get(1).isContainer());
    }

    @Test
    public void testDeleteWidgetFromContainerUnwrapsWhenSingleChildRemains()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(c1, c2);

        service.setDashboardWidgets(List.of(container));

        service.deleteWidget("c2");

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("c1", widgets.get(0).getId(), "Remaining child should be unwrapped to standalone widget");
        Assertions.assertFalse(widgets.get(0).isContainer());
    }

    @Test
    public void testSeparateChildWidgetFromMultiChildContainer()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c3 = new DashboardWidgetConfigDto(
            "c3", "Child 3", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(
            "container-1", "Child 1 & Child 2 & Child 3", List.of(c1, c2, c3), 0
        );

        service.setDashboardWidgets(List.of(container));

        final DashboardWidgetConfigDto separated = service.separateChildWidget(container.getId(), "c2");
        Assertions.assertNotNull(separated, "Separated child widget should not be null");
        Assertions.assertEquals("c2", separated.getId());

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(2, widgets.size(), "Container with 3 children separated by 1 child should yield 1 container and 1 standalone widget");

        final DashboardWidgetConfigDto remainingContainer = widgets.get(0);
        Assertions.assertTrue(remainingContainer.isContainer(), "Remaining container must still be a container");
        Assertions.assertEquals(2, remainingContainer.getChildren().size(), "Remaining container must retain 2 children");
        Assertions.assertEquals("c1", remainingContainer.getChildren().get(0).getId());
        Assertions.assertEquals("c3", remainingContainer.getChildren().get(1).getId());

        final DashboardWidgetConfigDto standalone = widgets.get(1);
        Assertions.assertEquals("c2", standalone.getId());
        Assertions.assertFalse(standalone.isContainer(), "Separated child widget must be a standalone card");
    }

    @Test
    public void testSeparateChildWidgetFromTwoChildContainer()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(c1, c2);

        service.setDashboardWidgets(List.of(container));

        final DashboardWidgetConfigDto separated = service.separateChildWidget(container.getId(), "c2");
        Assertions.assertNotNull(separated);
        Assertions.assertEquals("c2", separated.getId());

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(2, widgets.size(), "Separating 1 child from a 2-child container unwraps both children into standalone widgets");
        Assertions.assertEquals("c1", widgets.get(0).getId());
        Assertions.assertFalse(widgets.get(0).isContainer(), "Child 1 must be unwrapped to standalone widget");
        Assertions.assertEquals("c2", widgets.get(1).getId());
        Assertions.assertFalse(widgets.get(1).isContainer(), "Child 2 must be unwrapped to standalone widget");
    }

    @Test
    public void testReorderContainerChildrenDefaultDashboard()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c3 = new DashboardWidgetConfigDto(
            "c3", "Child 3", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(
            "cont-1", "Container 1", List.of(c1, c2, c3), 0
        );

        service.setDashboardWidgets(List.of(container));

        // Reorder children to: c3, c1, c2
        final boolean reordered = service.reorderContainerChildren("cont-1", List.of("c3", "c1", "c2"));
        Assertions.assertTrue(reordered);

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        final DashboardWidgetConfigDto updated = widgets.get(0);
        Assertions.assertEquals(3, updated.getChildren().size());
        Assertions.assertEquals("c3", updated.getChildren().get(0).getId());
        Assertions.assertEquals("c1", updated.getChildren().get(1).getId());
        Assertions.assertEquals("c2", updated.getChildren().get(2).getId());

        // Non-existent container returns false
        Assertions.assertFalse(service.reorderContainerChildren("unknown-container", List.of("c1", "c2")));
        // Null or empty list returns false
        Assertions.assertFalse(service.reorderContainerChildren("cont-1", Collections.emptyList()));
        Assertions.assertFalse(service.reorderContainerChildren("cont-1", null));
        Assertions.assertFalse(service.reorderContainerChildren(null, List.of("c1")));
    }

    @Test
    public void testReorderContainerChildrenCustomPage()
    {
        final DashboardPageConfigDto page = service.createCustomPage("Custom Analytics");
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(
            "cont-custom", "Custom Container", List.of(c1, c2), 0
        );

        service.setWidgetsForPage(page.getId(), List.of(container));

        final boolean reordered = service.reorderContainerChildren(page.getId(), "cont-custom", List.of("c2", "c1"));
        Assertions.assertTrue(reordered);

        final List<DashboardWidgetConfigDto> pageWidgets = service.getWidgetsForPage(page.getId());
        Assertions.assertEquals(1, pageWidgets.size());
        final DashboardWidgetConfigDto updated = pageWidgets.get(0);
        Assertions.assertEquals("c2", updated.getChildren().get(0).getId());
        Assertions.assertEquals("c1", updated.getChildren().get(1).getId());

        // Null pageId delegates to default dashboard
        Assertions.assertFalse(service.reorderContainerChildren(null, "unknown", List.of("c1")));
    }

    @Test
    public void testCreateCustomPage()
    {
        final DashboardPageConfigDto created = service.createCustomPage("Executive Summary");
        Assertions.assertNotNull(created, "Created page should not be null");
        Assertions.assertNotNull(created.getId(), "Page ID should be generated");
        Assertions.assertEquals("Executive Summary", created.getName());
        Assertions.assertNotNull(created.getWidgets(), "New custom page widgets list should not be null");
        Assertions.assertTrue(created.getWidgets().isEmpty(), "New custom page should start completely empty");

        final List<DashboardPageConfigDto> pages = service.getCustomPages();
        Assertions.assertEquals(1, pages.size());
        Assertions.assertEquals(created.getId(), pages.get(0).getId());
        Assertions.assertEquals("Executive Summary", pages.get(0).getName());
    }

    @Test
    public void testAddWidgetToCustomPageIsIsolatedFromMainDashboard()
    {
        final DashboardPageConfigDto page = service.createCustomPage("Custom Tab");
        final String pageId = page.getId();

        // Main dashboard initially has default widgets
        final List<DashboardWidgetConfigDto> initialMainWidgets = service.getDashboardWidgets();
        Assertions.assertFalse(initialMainWidgets.isEmpty(), "Main dashboard should have default widgets");

        // Custom page starts empty
        final List<DashboardWidgetConfigDto> pageWidgetsInitial = service.getWidgetsForPage(pageId);
        Assertions.assertTrue(pageWidgetsInitial.isEmpty(), "Custom page widgets should initially be empty");

        // Add a widget to the custom page
        final DashboardWidgetConfigDto customWidget = new DashboardWidgetConfigDto(
            "custom-w1", "Page Specific Widget", WidgetMetric.BUGS, null,
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false, false,
            Collections.emptyList(), true, null
        );
        service.addOrUpdateWidget(pageId, customWidget);

        // Verify widget is present on the custom page
        final List<DashboardWidgetConfigDto> pageWidgetsAfter = service.getWidgetsForPage(pageId);
        Assertions.assertEquals(1, pageWidgetsAfter.size());
        Assertions.assertEquals("custom-w1", pageWidgetsAfter.get(0).getId());
        Assertions.assertEquals("Page Specific Widget", pageWidgetsAfter.get(0).getTitle());

        // Verify main dashboard widgets were NOT modified
        final List<DashboardWidgetConfigDto> mainWidgetsAfter = service.getDashboardWidgets();
        Assertions.assertEquals(initialMainWidgets.size(), mainWidgetsAfter.size());
        Assertions.assertTrue(mainWidgetsAfter.stream().noneMatch(w -> "custom-w1".equals(w.getId())),
            "Main dashboard must not contain custom page widgets");
    }

    @Test
    public void testDeleteWidgetFromCustomPage()
    {
        final DashboardPageConfigDto page = service.createCustomPage("Deletions Test Page");
        final String pageId = page.getId();

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-del-1", "Widget 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false, false,
            Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-del-2", "Widget 2", WidgetMetric.COSTS, null,
            WidgetPresentation.TEXT, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 1, false, false,
            Collections.emptyList(), true, null
        );

        service.addOrUpdateWidget(pageId, w1);
        service.addOrUpdateWidget(pageId, w2);
        Assertions.assertEquals(2, service.getWidgetsForPage(pageId).size());

        service.deleteWidget(pageId, "w-del-1");
        final List<DashboardWidgetConfigDto> remaining = service.getWidgetsForPage(pageId);
        Assertions.assertEquals(1, remaining.size());
        Assertions.assertEquals("w-del-2", remaining.get(0).getId());

        service.deleteWidget(pageId, "w-del-2");
        final List<DashboardWidgetConfigDto> emptyAfter = service.getWidgetsForPage(pageId);
        Assertions.assertTrue(emptyAfter.isEmpty(), "After deleting all widgets, custom page should be empty again");
    }

    @Test
    public void testDeleteCustomPage()
    {
        final DashboardPageConfigDto page1 = service.createCustomPage("Page One");
        final DashboardPageConfigDto page2 = service.createCustomPage("Page Two");
        Assertions.assertEquals(2, service.getCustomPages().size());

        service.deleteCustomPage(page1.getId());
        final List<DashboardPageConfigDto> remainingPages = service.getCustomPages();
        Assertions.assertEquals(1, remainingPages.size());
        Assertions.assertEquals(page2.getId(), remainingPages.get(0).getId());

        // Verify getCustomPage returns null for deleted page
        Assertions.assertNull(service.getCustomPage(page1.getId()));
        Assertions.assertNotNull(service.getCustomPage(page2.getId()));
    }

    @Test
    public void testMultipleCustomPagesIsolation()
    {
        final DashboardPageConfigDto pageA = service.createCustomPage("Page Alpha");
        final DashboardPageConfigDto pageB = service.createCustomPage("Page Beta");

        final DashboardWidgetConfigDto widgetA = new DashboardWidgetConfigDto(
            "widget-a", "Widget For Alpha", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false, false,
            Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto widgetB = new DashboardWidgetConfigDto(
            "widget-b", "Widget For Beta", WidgetMetric.COSTS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false, false,
            Collections.emptyList(), true, null
        );

        service.addOrUpdateWidget(pageA.getId(), widgetA);
        service.addOrUpdateWidget(pageB.getId(), widgetB);

        final List<DashboardWidgetConfigDto> widgetsA = service.getWidgetsForPage(pageA.getId());
        final List<DashboardWidgetConfigDto> widgetsB = service.getWidgetsForPage(pageB.getId());

        Assertions.assertEquals(1, widgetsA.size());
        Assertions.assertEquals("widget-a", widgetsA.get(0).getId());

        Assertions.assertEquals(1, widgetsB.size());
        Assertions.assertEquals("widget-b", widgetsB.get(0).getId());
    }

    @Test
    public void testCombineWidgetsPreservesCustomExecutionIdentityAndSeparationRestoresIt()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-pie", "Status Pie Chart", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );

        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-ratio", "Browser ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("environment"), false, null
        );

        service.setDashboardWidgets(List.of(w1, w2));

        final DashboardWidgetConfigDto container = service.combineWidgets("w-pie", "w-ratio");
        Assertions.assertNotNull(container, "Combined container must not be null");
        Assertions.assertTrue(container.isContainer(), "Resulting widget must be a container");
        Assertions.assertTrue(container.hasOwnExecutionIdentity(), "Container must inherit custom execution identity");
        Assertions.assertEquals(List.of("environment"), container.getExecutionIdentityDimensions());

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertTrue(widgets.get(0).hasOwnExecutionIdentity());
        Assertions.assertEquals(List.of("environment"), widgets.get(0).getExecutionIdentityDimensions());

        // Separate back
        final List<DashboardWidgetConfigDto> separated = service.separateContainer(container.getId());
        Assertions.assertEquals(2, separated.size(), "Separation must restore 2 standalone widgets");
        Assertions.assertTrue(separated.get(0).hasOwnExecutionIdentity(), "First widget must retain custom execution identity");
        Assertions.assertEquals(List.of("environment"), separated.get(0).getExecutionIdentityDimensions());
        Assertions.assertTrue(separated.get(1).hasOwnExecutionIdentity(), "Second widget must retain custom execution identity");
        Assertions.assertEquals(List.of("environment"), separated.get(1).getExecutionIdentityDimensions());
    }

    @Test
    public void testCreateCustomPageWithExecutionIdentityAndRunFilters()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Set.of("en_US"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            10, "2026-10-01", "2026-10-31"
        );
        final List<String> identityDims = List.of("browser", "locale");

        final DashboardPageConfigDto created = service.createCustomPage(
            "QA Performance", identityDims, runFilters
        );

        Assertions.assertNotNull(created);
        Assertions.assertEquals("QA Performance", created.getName());
        Assertions.assertEquals(identityDims, created.getExecutionIdentityDimensions());
        Assertions.assertTrue(created.hasRunFilters());
        Assertions.assertEquals(runFilters, created.getRunFilters());

        final List<DashboardPageConfigDto> pages = service.getCustomPages();
        Assertions.assertEquals(1, pages.size());
        Assertions.assertEquals("QA Performance", pages.get(0).getName());
        Assertions.assertEquals(identityDims, pages.get(0).getExecutionIdentityDimensions());
        Assertions.assertTrue(pages.get(0).hasRunFilters());
        Assertions.assertEquals(Integer.valueOf(10), pages.get(0).getRunFilters().getLimit());
    }

    @Test
    public void testCreateCustomPageDefaults()
    {
        final DashboardPageConfigDto created = service.createCustomPage("Default Page");

        Assertions.assertNotNull(created);
        Assertions.assertEquals("Default Page", created.getName());
        Assertions.assertEquals(List.of("browser"), created.getExecutionIdentityDimensions());
        Assertions.assertFalse(created.hasRunFilters());
        Assertions.assertNull(created.getRunFilters());
    }

    @Test
    public void testCreateCustomPageEnforcesBrowserInExecutionIdentity()
    {
        final DashboardPageConfigDto created = service.createCustomPage(
            "Custom Page Without Explicit Browser",
            List.of("locale"),
            null
        );

        Assertions.assertNotNull(created);
        Assertions.assertTrue(created.getExecutionIdentityDimensions().contains("browser"),
            "createCustomPage must enforce browser as mandatory base dimension");
        Assertions.assertEquals(List.of("browser", "locale"), created.getExecutionIdentityDimensions());
    }

    @Test
    public void testCreateGroupDashboardEnforcesBrowserInExecutionIdentity()
    {
        final DashboardPageConfigDto created = service.createGroupDashboard(
            "Group Without Browser",
            "browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("environment"),
            null,
            List.of("Chrome"),
            null
        );

        Assertions.assertNotNull(created);
        Assertions.assertTrue(created.getExecutionIdentityDimensions().contains("browser"),
            "createGroupDashboard must enforce browser as mandatory base dimension");
        Assertions.assertEquals(List.of("browser", "environment"), created.getExecutionIdentityDimensions());
    }

    @Test
    public void testRenameWidgetGroupContainer()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Latest Test Status", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        service.setDashboardWidgets(List.of(w1, w2));
        final DashboardWidgetConfigDto container = service.combineWidgets("w-target", "w-source");
        Assertions.assertNotNull(container);
        Assertions.assertEquals("Latest Test Status & Browser Ratio", container.getTitle());

        final boolean renamed = service.renameWidget(container.getId(), "Core QA Suite");
        Assertions.assertTrue(renamed);

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        final DashboardWidgetConfigDto updatedContainer = widgets.get(0);
        Assertions.assertEquals("Core QA Suite", updatedContainer.getTitle());
        Assertions.assertEquals(2, updatedContainer.getChildren().size());
        Assertions.assertEquals("Latest Test Status", updatedContainer.getChildren().get(0).getTitle());
        Assertions.assertEquals("Browser Ratio", updatedContainer.getChildren().get(1).getTitle());
    }

    @Test
    public void testRenameSingleChildWidgetInGroup()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-child1", "Status Donut", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-child2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        service.setDashboardWidgets(List.of(w1, w2));
        final DashboardWidgetConfigDto container = service.combineWidgets("w-child1", "w-child2");
        Assertions.assertNotNull(container);

        final boolean renamed = service.renameWidget("w-child1", "Overall Pass Breakdown");
        Assertions.assertTrue(renamed);

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size());
        final DashboardWidgetConfigDto updatedContainer = widgets.get(0);
        Assertions.assertEquals(2, updatedContainer.getChildren().size());
        Assertions.assertEquals("Overall Pass Breakdown", updatedContainer.getChildren().get(0).getTitle());
        Assertions.assertEquals("Browser Ratio", updatedContainer.getChildren().get(1).getTitle());
    }

    @Test
    public void testRenameWidgetOnCustomPage()
    {
        final DashboardPageConfigDto page = service.createCustomPage("My Dashboard");
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-page1", "Initial Name", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );
        service.setWidgetsForPage(page.getId(), List.of(w1));

        final boolean renamed = service.renameWidget(page.getId(), "w-page1", "Custom Name");
        Assertions.assertTrue(renamed);

        final List<DashboardWidgetConfigDto> widgets = service.getWidgetsForPage(page.getId());
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("Custom Name", widgets.get(0).getTitle());
    }

    @Test
    public void testRenameWidgetInvalidInputs()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-test", "Valid Title", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );
        service.setDashboardWidgets(List.of(w1));

        Assertions.assertFalse(service.renameWidget(null, "New Title"));
        Assertions.assertFalse(service.renameWidget("w-test", null));
        Assertions.assertFalse(service.renameWidget("w-test", "   "));
        Assertions.assertFalse(service.renameWidget("non-existent", "New Title"));

        Assertions.assertEquals("Valid Title", service.getDashboardWidgets().get(0).getTitle());
    }

    @Test
    public void testAddOrUpdateWidgetContainerAware()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-c1", "Widget 1", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-c2", "Widget 2", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        service.setDashboardWidgets(List.of(w1, w2));
        service.combineWidgets("w-c1", "w-c2");

        final DashboardWidgetConfigDto updatedChild = new DashboardWidgetConfigDto(
            "w-c1", "Updated Widget 1", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            "half", 0
        );

        service.addOrUpdateWidget(updatedChild);

        final List<DashboardWidgetConfigDto> widgets = service.getDashboardWidgets();
        Assertions.assertEquals(1, widgets.size(), "Should remain a single container without adding top-level duplicate");
        final DashboardWidgetConfigDto container = widgets.get(0);
        Assertions.assertEquals("Updated Widget 1", container.getChildren().get(0).getTitle());
    }

    @Test
    public void testCreateGroupDashboardWithSubDashboards()
    {
        final List<String> groupValues = List.of("Chrome", "Firefox", "Safari");
        final DashboardPageConfigDto groupParent = service.createGroupDashboard(
            "Browser Matrix",
            "browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            groupValues
        );

        Assertions.assertNotNull(groupParent);
        Assertions.assertTrue(groupParent.isGroupParent());
        Assertions.assertEquals("browser", groupParent.getGroupType());
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, groupParent.getGroupCardScope());

        final List<DashboardPageConfigDto> subPages = service.getSubPages(groupParent.getId());
        Assertions.assertEquals(3, subPages.size());

        for (final DashboardPageConfigDto sub : subPages)
        {
            Assertions.assertTrue(sub.isSubPage());
            Assertions.assertEquals(groupParent.getId(), sub.getParentPageId());
            Assertions.assertEquals("browser", sub.getGroupType());
            Assertions.assertTrue(groupValues.contains(sub.getGroupValue()));
            Assertions.assertNotNull(sub.getRunFilters());
            Assertions.assertTrue(sub.getRunFilters().getBrowsers().contains(sub.getGroupValue()));
        }
    }

    @Test
    public void testGetRootCustomPagesNestsSubPages()
    {
        final DashboardPageConfigDto standardPage = service.createCustomPage("Standard Overview", List.of("browser"), null);
        final DashboardPageConfigDto groupParent = service.createGroupDashboard(
            "Environments",
            "environment",
            WidgetPointScope.LAST_RUN,
            List.of("environment"),
            null,
            List.of("Production", "Staging")
        );

        final List<DashboardPageConfigDto> roots = service.getRootCustomPages();
        Assertions.assertEquals(2, roots.size(), "Should contain exactly 2 root pages");

        final DashboardPageConfigDto foundGroup = roots.stream()
            .filter(r -> r.getId().equals(groupParent.getId()))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(foundGroup);
        Assertions.assertTrue(foundGroup.isGroupParent());
        Assertions.assertEquals(2, foundGroup.getSubPages().size());
        Assertions.assertEquals(WidgetPointScope.LAST_RUN, foundGroup.getGroupCardScope());
    }

    @Test
    public void testDeleteCustomPageCascadesToSubPages()
    {
        final DashboardPageConfigDto groupParent = service.createGroupDashboard(
            "Locales",
            "locale",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("locale"),
            null,
            List.of("en_US", "de_DE")
        );

        final String parentId = groupParent.getId();
        final List<DashboardPageConfigDto> initialSubPages = service.getSubPages(parentId);
        Assertions.assertEquals(2, initialSubPages.size());

        service.deleteCustomPage(parentId);

        Assertions.assertNull(service.getCustomPage(parentId), "Parent page should be deleted");
        for (final DashboardPageConfigDto sub : initialSubPages)
        {
            Assertions.assertNull(service.getCustomPage(sub.getId()), "Child sub-page should be cascade deleted");
        }
        Assertions.assertTrue(service.getSubPages(parentId).isEmpty());
    }

    @Test
    public void testEnsureSubDashboardForGroup()
    {
        final DashboardPageConfigDto groupParent = service.createGroupDashboard(
            "Areas",
            "area",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("area"),
            null,
            List.of("Checkout")
        );

        final DashboardPageConfigDto existing = service.ensureSubDashboardForGroup(
            groupParent.getId(), "area", "Checkout", WidgetPointScope.LATEST_TEST_STATUS
        );
        Assertions.assertNotNull(existing);

        final DashboardPageConfigDto newlyCreated = service.ensureSubDashboardForGroup(
            groupParent.getId(), "area", "Catalog", WidgetPointScope.LATEST_TEST_STATUS
        );
        Assertions.assertNotNull(newlyCreated);
        Assertions.assertEquals("Catalog", newlyCreated.getGroupValue());
        Assertions.assertEquals("Catalog", newlyCreated.getName());
        Assertions.assertTrue(newlyCreated.getRunFilters().getAreas().contains("Catalog"));

        final List<DashboardPageConfigDto> allSubs = service.getSubPages(groupParent.getId());
        Assertions.assertEquals(2, allSubs.size());
    }

    @Test
    public void testConvertToGroupDashboardCreatesSubMainGroupAndChildDashboards()
    {
        final DashboardPageConfigDto groupParent = service.createGroupDashboard(
            "Batches",
            "batch",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("TEST", "CHECKOUT")
        );
        final List<DashboardPageConfigDto> rootSubs = service.getSubPages(groupParent.getId());
        Assertions.assertEquals(2, rootSubs.size());

        final DashboardPageConfigDto testSub = rootSubs.stream()
            .filter(s -> "TEST".equals(s.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertFalse(testSub.isGroupParent());
        Assertions.assertEquals(groupParent.getId(), testSub.getParentPageId());

        // Convert the "TEST" sub-dashboard to a group dashboard grouped by browser
        final DashboardPageConfigDto converted = service.convertToGroupDashboard(
            testSub.getId(),
            "browser",
            WidgetPointScope.LAST_RUN,
            List.of("Chrome", "Firefox")
        );

        Assertions.assertNotNull(converted);
        Assertions.assertTrue(converted.isGroupParent(), "Converted sub-page must now be a group parent");
        Assertions.assertEquals("browser", converted.getGroupType());
        Assertions.assertEquals(WidgetPointScope.LAST_RUN, converted.getGroupCardScope());
        Assertions.assertEquals(groupParent.getId(), converted.getParentPageId(), "Parent page ID must be preserved");

        // Verify child sub-dashboards generated under converted page
        final List<DashboardPageConfigDto> subSubs = service.getSubPages(converted.getId());
        Assertions.assertEquals(2, subSubs.size(), "Should have created 2 child sub-dashboards under converted page");
        for (final DashboardPageConfigDto child : subSubs)
        {
            Assertions.assertEquals(converted.getId(), child.getParentPageId());
            Assertions.assertFalse(child.isGroupParent());
            Assertions.assertNotNull(child.getRunFilters());
            Assertions.assertTrue(child.getRunFilters().getTags().contains("TEST"), "Inherits parent batch filter");
            Assertions.assertTrue(child.getRunFilters().getBrowsers().contains(child.getGroupValue()), "Has browser filter");
        }
    }

    @Test
    public void testRecursiveTreeAndMultiLevelCascadeDeletion()
    {
        final DashboardPageConfigDto rootGroup = service.createGroupDashboard(
            "Root Group",
            "batch",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("SUITE_A")
        );
        final String suiteAId = service.getSubPages(rootGroup.getId()).get(0).getId();

        // Convert SUITE_A to intermediate group with Chrome and Firefox
        service.convertToGroupDashboard(suiteAId, "browser", WidgetPointScope.LATEST_TEST_STATUS, List.of("Chrome", "Firefox"));

        // Verify getRootCustomPages recursively embeds the nested sub-pages
        final List<DashboardPageConfigDto> roots = service.getRootCustomPages();
        Assertions.assertEquals(1, roots.size());
        final DashboardPageConfigDto root = roots.get(0);
        Assertions.assertEquals(1, root.getSubPages().size());
        final DashboardPageConfigDto suiteA = root.getSubPages().get(0);
        Assertions.assertTrue(suiteA.isGroupParent());
        Assertions.assertEquals(2, suiteA.getSubPages().size());

        // Deleting root page cascades to suiteA and grandchildren Chrome, Firefox
        service.deleteCustomPage(rootGroup.getId());
        Assertions.assertNull(service.getCustomPage(rootGroup.getId()));
        Assertions.assertNull(service.getCustomPage(suiteAId));
        Assertions.assertTrue(service.getCustomPages().isEmpty(), "All descendants must be deleted recursively");
    }

    @Test
    public void testConvertToGroupDashboardWithCustomIdentityAndFilters()
    {
        final DashboardPageConfigDto standardPage = service.createCustomPage("Standard Overview", List.of("browser"), null);
        Assertions.assertFalse(standardPage.isGroupParent());

        final WidgetFilterConfigDto customFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Set.of("en_US"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            10, null, null
        );

        final DashboardPageConfigDto converted = service.convertToGroupDashboard(
            standardPage.getId(),
            "environment",
            WidgetPointScope.LAST_RUN,
            List.of("browser", "locale"),
            customFilters,
            List.of("Production", "Staging")
        );

        Assertions.assertNotNull(converted);
        Assertions.assertTrue(converted.isGroupParent());
        Assertions.assertEquals("environment", converted.getGroupType());
        Assertions.assertEquals(List.of("browser", "locale"), converted.getExecutionIdentityDimensions());
        Assertions.assertEquals(customFilters, converted.getRunFilters());

        final List<DashboardPageConfigDto> subPages = service.getSubPages(converted.getId());
        Assertions.assertEquals(2, subPages.size());
        for (final DashboardPageConfigDto sub : subPages)
        {
            Assertions.assertEquals(List.of("browser", "locale"), sub.getExecutionIdentityDimensions());
            Assertions.assertTrue(sub.getRunFilters().getEnvs().contains(sub.getGroupValue()));
            Assertions.assertTrue(sub.getRunFilters().getBrowsers().contains("Chrome"));
            Assertions.assertTrue(sub.getRunFilters().getLocales().contains("en_US"));
            Assertions.assertEquals(10, sub.getRunFilters().getLimit());
        }
    }

    @Test
    public void testCreateGroupRunFiltersWithCompoundDimensions()
    {
        final WidgetFilterConfigDto baseFilters = new WidgetFilterConfigDto(
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            25, "2026-01-01", "2026-01-31"
        );

        final WidgetFilterConfigDto compoundFilters = ReportDashboardSettingService.createGroupRunFilters(
            "batch,browser",
            "SmokeBatch • Chrome",
            baseFilters
        );

        Assertions.assertNotNull(compoundFilters);
        Assertions.assertTrue(compoundFilters.getTags().contains("SmokeBatch"));
        Assertions.assertTrue(compoundFilters.getBrowsers().contains("Chrome"));
        Assertions.assertEquals(25, compoundFilters.getLimit());
        Assertions.assertEquals("2026-01-01", compoundFilters.getStartDate());
        Assertions.assertEquals("2026-01-31", compoundFilters.getEndDate());
    }

    @Test
    public void testCreateGroupRunFiltersWithMultipleCompoundDelimiters()
    {
        final WidgetFilterConfigDto slashFilters = ReportDashboardSettingService.createGroupRunFilters(
            "environment,browser",
            "Staging / Safari",
            null
        );
        Assertions.assertTrue(slashFilters.getEnvs().contains("Staging"));
        Assertions.assertTrue(slashFilters.getBrowsers().contains("Safari"));

        final WidgetFilterConfigDto pipeFilters = ReportDashboardSettingService.createGroupRunFilters(
            "area,environment",
            "@Checkout | Production",
            null
        );
        Assertions.assertTrue(pipeFilters.getAreas().contains("@Checkout"));
        Assertions.assertTrue(pipeFilters.getEnvs().contains("Production"));
    }

    @Test
    public void testNormalizeGroupTypeAndFormatDashboardName()
    {
        final String normalized = ReportDashboardSettingService.normalizeGroupType("browsers, env");
        Assertions.assertEquals("browser,environment", normalized);

        final String formatted = ReportDashboardSettingService.formatGroupDashboardName("batch,browser");
        Assertions.assertEquals("Batch & Browser Groups Dashboard", formatted);
    }

    @Test
    public void testGetProhibitedGroupDimensionsHierarchy()
    {
        final DashboardPageConfigDto standard = service.createCustomPage("Root Standard");
        final Set<String> standardProhibited = service.getProhibitedGroupDimensions(standard.getId());
        Assertions.assertTrue(standardProhibited.isEmpty(), "Root standard dashboard should have no prohibited dimensions");

        final DashboardPageConfigDto batchGroup = service.createGroupDashboard(
            "Batch Group",
            "batch",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("SmokeSuite")
        );
        final Set<String> rootProhibited = service.getProhibitedGroupDimensions(batchGroup.getId());
        Assertions.assertEquals(Set.of("batch"), rootProhibited);

        final List<DashboardPageConfigDto> subPages = service.getSubPages(batchGroup.getId());
        Assertions.assertEquals(1, subPages.size());
        final DashboardPageConfigDto smokeSubPage = subPages.get(0);
        final Set<String> subProhibited = service.getProhibitedGroupDimensions(smokeSubPage.getId());
        Assertions.assertEquals(Set.of("batch"), subProhibited, "Sub-page should inherit batch as prohibited");

        // Convert sub-page to group by browser
        final DashboardPageConfigDto convertedSub = service.convertToGroupDashboard(
            smokeSubPage.getId(),
            "browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("Chrome")
        );
        Assertions.assertNotNull(convertedSub);

        final List<DashboardPageConfigDto> childSubPages = service.getSubPages(convertedSub.getId());
        Assertions.assertEquals(1, childSubPages.size());
        final DashboardPageConfigDto chromeChild = childSubPages.get(0);

        final Set<String> childProhibited = service.getProhibitedGroupDimensions(chromeChild.getId());
        Assertions.assertTrue(childProhibited.contains("batch"), "Should prohibit batch from grandparent");
        Assertions.assertTrue(childProhibited.contains("browser"), "Should prohibit browser from parent");
    }

    @Test
    public void testConvertToGroupDashboardWithCompoundDimensions()
    {
        final DashboardPageConfigDto standard = service.createCustomPage("Multi Matrix");
        final DashboardPageConfigDto converted = service.convertToGroupDashboard(
            standard.getId(),
            "environment,browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("Production • Chrome", "Staging • Firefox")
        );

        Assertions.assertNotNull(converted);
        Assertions.assertEquals("environment,browser", converted.getGroupType());

        final List<DashboardPageConfigDto> subPages = service.getSubPages(converted.getId());
        Assertions.assertEquals(2, subPages.size());

        final DashboardPageConfigDto prodChrome = subPages.stream()
            .filter(p -> "Production • Chrome".equals(p.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(prodChrome);
        Assertions.assertTrue(prodChrome.getRunFilters().getEnvs().contains("Production"));
        Assertions.assertTrue(prodChrome.getRunFilters().getBrowsers().contains("Chrome"));
    }

    @Test
    public void testCopyWidgets_ReplaceMode()
    {
        final DashboardPageConfigDto sourcePage = service.createCustomPage("Source Page");
        final DashboardPageConfigDto targetPage = service.createCustomPage("Target Page");

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-src-1", "Source Status", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-src-2", "Source Costs", WidgetMetric.COSTS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1
        );
        service.addOrUpdateWidget(sourcePage.getId(), w1);
        service.addOrUpdateWidget(sourcePage.getId(), w2);

        final DashboardWidgetConfigDto existingTargetWidget = new DashboardWidgetConfigDto(
            "w-tgt-old", "Old Target Widget", WidgetMetric.BUGS, null,
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );
        service.addOrUpdateWidget(targetPage.getId(), existingTargetWidget);

        // Copy in replace mode (append = false)
        final List<DashboardWidgetConfigDto> copied = service.copyWidgets(sourcePage.getId(), targetPage.getId(), false);

        Assertions.assertEquals(2, copied.size());
        Assertions.assertEquals("Source Status", copied.get(0).getTitle());
        Assertions.assertEquals("Source Costs", copied.get(1).getTitle());
        Assertions.assertNotEquals("w-src-1", copied.get(0).getId());
        Assertions.assertNotEquals("w-src-2", copied.get(1).getId());
        Assertions.assertEquals(0, copied.get(0).getSortOrder());
        Assertions.assertEquals(1, copied.get(1).getSortOrder());

        // Target page should only have the 2 copied widgets (old widget replaced)
        final List<DashboardWidgetConfigDto> targetWidgets = service.getWidgetsForPage(targetPage.getId());
        Assertions.assertEquals(2, targetWidgets.size());
        Assertions.assertTrue(targetWidgets.stream().noneMatch(w -> "w-tgt-old".equals(w.getId())));
    }

    @Test
    public void testCopyWidgets_AppendMode()
    {
        final DashboardPageConfigDto sourcePage = service.createCustomPage("Append Source");
        final DashboardPageConfigDto targetPage = service.createCustomPage("Append Target");

        final DashboardWidgetConfigDto srcWidget = new DashboardWidgetConfigDto(
            "w-src-append", "Appended Widget", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        service.addOrUpdateWidget(sourcePage.getId(), srcWidget);

        final DashboardWidgetConfigDto tgtWidget = new DashboardWidgetConfigDto(
            "w-tgt-keep", "Existing Widget", WidgetMetric.BUGS, null,
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );
        service.addOrUpdateWidget(targetPage.getId(), tgtWidget);

        // Copy in append mode (append = true)
        final List<DashboardWidgetConfigDto> result = service.copyWidgets(sourcePage.getId(), targetPage.getId(), true);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("w-tgt-keep", result.get(0).getId());
        Assertions.assertEquals(0, result.get(0).getSortOrder());

        Assertions.assertEquals("Appended Widget", result.get(1).getTitle());
        Assertions.assertNotEquals("w-src-append", result.get(1).getId());
        Assertions.assertEquals(1, result.get(1).getSortOrder());
    }

    @Test
    public void testCopyWidgets_ContainerRecursion()
    {
        final DashboardPageConfigDto sourcePage = service.createCustomPage("Container Source");
        final DashboardPageConfigDto targetPage = service.createCustomPage("Container Target");

        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "Child 2", WidgetMetric.COSTS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer("c-orig", "Container Group", List.of(child1, child2), 0);
        service.addOrUpdateWidget(sourcePage.getId(), container);

        final List<DashboardWidgetConfigDto> copied = service.copyWidgets(sourcePage.getId(), targetPage.getId(), false);

        Assertions.assertEquals(1, copied.size());
        final DashboardWidgetConfigDto copiedContainer = copied.get(0);
        Assertions.assertTrue(copiedContainer.isContainer());
        Assertions.assertNotEquals("c-orig", copiedContainer.getId());
        Assertions.assertEquals(2, copiedContainer.getChildren().size());
        Assertions.assertNotEquals("child-1", copiedContainer.getChildren().get(0).getId());
        Assertions.assertNotEquals("child-2", copiedContainer.getChildren().get(1).getId());
    }

    @Test
    public void testCopyWidgets_SameSourceAndTarget_ThrowsException()
    {
        final DashboardPageConfigDto page = service.createCustomPage("Self Copy Page");
        final DashboardWidgetConfigDto w = new DashboardWidgetConfigDto(
            "w-self", "Status", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        service.addOrUpdateWidget(page.getId(), w);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.copyWidgets(page.getId(), page.getId(), false);
        });
    }

    @Test
    public void testCopyWidgets_SourceNotFound_ThrowsException()
    {
        final DashboardPageConfigDto target = service.createCustomPage("Target Missing Source");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.copyWidgets("non-existent-page-id", target.getId(), false);
        });
    }

    @Test
    public void testCopyWidgets_FromDefaultDashboard()
    {
        final DashboardPageConfigDto targetPage = service.createCustomPage("Target From Default");

        // Copy from default dashboard
        final List<DashboardWidgetConfigDto> copied = service.copyWidgets("default", targetPage.getId(), false);

        Assertions.assertFalse(copied.isEmpty());
        final List<DashboardWidgetConfigDto> defaultWidgets = service.getDashboardWidgets();
        Assertions.assertEquals(defaultWidgets.size(), copied.size());

        for (int i = 0; i < defaultWidgets.size(); i++)
        {
            Assertions.assertEquals(defaultWidgets.get(i).getTitle(), copied.get(i).getTitle());
            Assertions.assertNotEquals(defaultWidgets.get(i).getId(), copied.get(i).getId());
            Assertions.assertEquals(i, copied.get(i).getSortOrder());
        }
    }

    @Test
    public void testCreateCustomPage_WithCopyFromPageId()
    {
        final DashboardPageConfigDto templatePage = service.createCustomPage("Template Page");
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-tmpl-1", "Template Status", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        service.addOrUpdateWidget(templatePage.getId(), w1);

        final DashboardPageConfigDto newPage = service.createCustomPage(
            "Instantiated Page",
            List.of("browser"),
            null,
            templatePage.getId()
        );

        Assertions.assertNotNull(newPage);
        Assertions.assertEquals("Instantiated Page", newPage.getName());
        Assertions.assertEquals(1, newPage.getWidgets().size());
        Assertions.assertEquals("Template Status", newPage.getWidgets().get(0).getTitle());
        Assertions.assertNotEquals("w-tmpl-1", newPage.getWidgets().get(0).getId());
    }
}



