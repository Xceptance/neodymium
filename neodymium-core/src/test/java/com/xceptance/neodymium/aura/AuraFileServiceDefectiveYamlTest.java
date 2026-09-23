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
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests verifying graceful handling of defective and malformed YAML files in AuraFileService.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraFileServiceDefectiveYamlTest
{
    private AuraFileService fileService;

    @BeforeEach
    public void setUp()
    {
        fileService = new AuraFileService();
    }

    @Test
    public void testParsePlaybookSectionsWithMalformedYaml()
    {
        final String malformedYaml = "steps: |\n"
                + "    Open ${homepageUrl} in the browser\n"
                + "  Close cookie consent banner\n";

        final Map<String, Object> result = fileService.parsePlaybookSections(malformedYaml);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(Boolean.TRUE, result.get("hasError"));
        Assertions.assertNotNull(result.get("error"));

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) result.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertTrue(mainSteps.isEmpty(), "Main steps should be empty when YAML parsing fails");
    }

    @Test
    public void testParsePlaybookSectionsWithValidYaml()
    {
        final String validYaml = "steps: |\n"
                + "  Open ${homepageUrl}\n"
                + "  Click button\n";

        final Map<String, Object> result = fileService.parsePlaybookSections(validYaml);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(Boolean.FALSE, result.get("hasError"));
        Assertions.assertNull(result.get("error"));

        @SuppressWarnings("unchecked")
        final List<String> mainSteps = (List<String>) result.get("mainSteps");
        Assertions.assertNotNull(mainSteps);
        Assertions.assertEquals(2, mainSteps.size());
        Assertions.assertEquals("Open ${homepageUrl}", mainSteps.get(0));
        Assertions.assertEquals("Click button", mainSteps.get(1));
    }

    @Test
    public void testGetFileDetailsWithMalformedYamlFile(@TempDir final File tempDir) throws IOException
    {
        final File defectiveFile = new File(tempDir, "broken.yaml");
        final String defectiveContent = "steps: |\n"
                + "    Open url\n"
                + "  Close banner\n";
        Files.writeString(defectiveFile.toPath(), defectiveContent, StandardCharsets.UTF_8);

        final Map<String, Object> details = fileService.getFileDetails(defectiveFile);

        Assertions.assertNotNull(details);
        Assertions.assertEquals(Boolean.TRUE, details.get("hasError"));
        Assertions.assertNotNull(details.get("error"));

        @SuppressWarnings("unchecked")
        final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
        Assertions.assertNotNull(datasets);
        Assertions.assertTrue(datasets.isEmpty());
    }

    @Test
    public void testYamlFileDtoErrorProperties()
    {
        final YamlFileDto errorDto = new YamlFileDto("broken.yaml", List.of(), true, "YAML syntax error");
        Assertions.assertTrue(errorDto.isHasError());
        Assertions.assertEquals("YAML syntax error", errorDto.getErrorMessage());

        final YamlFileDto validDto = new YamlFileDto("valid.yaml", List.of());
        Assertions.assertFalse(validDto.isHasError());
        Assertions.assertNull(validDto.getErrorMessage());
    }
}
