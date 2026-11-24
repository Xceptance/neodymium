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
package com.xceptance.neodymium.aura.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying tag aggregation, root-tag separation, and tag counting on {@link YamlFileDto} and {@link DatasetDto}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class YamlFileDtoTest
{
    @Test
    public void testRootTagsAndDatasetOnlyTagsSeparation()
    {
        final Map<String, List<String>> rootTags = new LinkedHashMap<>();
        rootTags.put("area", List.of("Catalog"));
        rootTags.put("type", List.of("Regression"));

        final Map<String, List<String>> allTags = new LinkedHashMap<>();
        allTags.put("area", List.of("Catalog"));
        allTags.put("type", List.of("Regression", "Negative"));
        allTags.put("category", List.of("art", "outdoor", "wildlife"));

        final DatasetDto ds = new DatasetDto("ds1", "Art Search", true, false, null, Map.of("category", List.of("art", "outdoor")));
        final YamlFileDto dto = new YamlFileDto("tests/search.yaml", List.of(ds), false, null, false, allTags, rootTags);

        Assertions.assertEquals(rootTags, dto.getRootTags());
        Assertions.assertEquals(allTags, dto.getTags());
        Assertions.assertEquals(6, dto.getTotalTagCount());
        Assertions.assertEquals(4, dto.getDatasetOnlyTagCount());

        final Map<String, List<String>> datasetOnly = dto.getDatasetOnlyTags();
        Assertions.assertFalse(datasetOnly.containsKey("area"));
        Assertions.assertEquals(List.of("Negative"), datasetOnly.get("type"));
        Assertions.assertEquals(List.of("art", "outdoor", "wildlife"), datasetOnly.get("category"));
        Assertions.assertEquals(2, ds.getTotalTagCount());
    }

    @Test
    public void testLegacySixArgConstructorDefaultsRootTagsToTags()
    {
        final Map<String, List<String>> tags = Map.of("feature", List.of("login"));
        final YamlFileDto dto = new YamlFileDto("login.yaml", List.of(), false, null, false, tags);

        Assertions.assertEquals(tags, dto.getRootTags());
        Assertions.assertEquals(tags, dto.getTags());
        Assertions.assertTrue(dto.getDatasetOnlyTags().isEmpty());
        Assertions.assertEquals(1, dto.getTotalTagCount());
        Assertions.assertEquals(0, dto.getDatasetOnlyTagCount());
    }

    @Test
    public void testFileWithOnlyDatasetTags()
    {
        final Map<String, List<String>> allTags = Map.of("errorType", List.of("wrong_password", "wrong_domain"));
        final YamlFileDto dto = new YamlFileDto("negative.yaml", List.of(), false, null, false, allTags, Map.of());

        Assertions.assertTrue(dto.getRootTags().isEmpty());
        Assertions.assertEquals(2, dto.getTotalTagCount());
        Assertions.assertEquals(2, dto.getDatasetOnlyTagCount());
        Assertions.assertEquals(List.of("wrong_password", "wrong_domain"), dto.getDatasetOnlyTags().get("errorType"));
    }
}
