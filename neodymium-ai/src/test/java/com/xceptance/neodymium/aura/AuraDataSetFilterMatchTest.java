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
package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.aura.dto.DatasetDto;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for dataset ID parsing in AuraFileService and dataset matching.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraDataSetFilterMatchTest
{
    @Test
    public final void testDatasetIdExtractionWithCustomIdKey(@TempDir final File tempDir) throws Exception
    {
        final File yamlFile = new File(tempDir, "test_dataset_id.yml");
        try (final FileWriter writer = new FileWriter(yamlFile, StandardCharsets.UTF_8))
        {
            writer.write("steps: |\n" +
                    "  Open \"https://www.example.com\"\n" +
                    "data:\n" +
                    "  - id: \"custom_dataset_id_123\"\n");
        }

        final AuraFileService fileService = new AuraFileService();
        final Map<String, Object> details = fileService.getFileDetails(yamlFile);

        Assertions.assertNotNull(details);
        @SuppressWarnings("unchecked")
        final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
        Assertions.assertNotNull(datasets);
        Assertions.assertEquals(1, datasets.size());

        final DatasetDto dto = datasets.get(0);
        Assertions.assertEquals("custom_dataset_id_123", dto.id);
        Assertions.assertTrue(dto.hasTestId);
    }

    @Test
    public final void testDatasetFilterRegexEscaping()
    {
        final List<String> ids = List.of("1", "wikipedia_search");
        final StringBuilder idFilterBuilder = new StringBuilder();
        boolean hasIds = false;
        for (int j = 0; j < ids.size(); j++)
        {
            if (ids.get(j) != null)
            {
                if (hasIds)
                {
                    idFilterBuilder.append("|");
                }
                else
                {
                    idFilterBuilder.append("^(");
                }
                idFilterBuilder.append(ids.get(j).replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1"));
                hasIds = true;
            }
        }
        if (hasIds)
        {
            idFilterBuilder.append(")$");
        }

        final String filterRegex = idFilterBuilder.toString();
        Assertions.assertEquals("^(1|wikipedia_search)$", filterRegex);
        Assertions.assertFalse(filterRegex.contains("\\Q"));
        Assertions.assertFalse(filterRegex.contains("\\E"));

        final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(filterRegex);
        Assertions.assertTrue(pattern.matcher("1").find());
        Assertions.assertTrue(pattern.matcher("wikipedia_search").find());
        Assertions.assertFalse(pattern.matcher("2").find());
    }
}
