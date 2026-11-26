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
package com.xceptance.aura.report.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xceptance.aura.report.dto.TestExecutionDto;

/**
 * Unit and integration tests verifying that test diagnostics attachment paths,
 * file URIs, and inlined text payloads are properly populated during execution details resolution.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportAttachmentDiagnosticsTest
{
    @Autowired
    private AuraReportDataService dataService;

    @AfterEach
    public void tearDown() throws IOException
    {
        final Path runsDir = Path.of("storage/runs");
        if (Files.exists(runsDir))
        {
            Files.list(runsDir)
                .filter(p -> p.getFileName().toString().startsWith("diag-test-run-"))
                .forEach(runDir -> {
                    try
                    {
                        Files.walk(runDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    }
                    catch (final IOException ignored)
                    {
                    }
                });
        }
    }

    @Test
    public void testExecutionAttachmentsPopulateRelativePathFileUriAndInlinedContent() throws Exception
    {
        final String runId = "diag-test-run-101";
        final Path runDir = Path.of("storage", "runs", runId);
        final Path classDir = runDir.resolve("com.example.DiagnosticTest");
        Files.createDirectories(classDir);

        final String logContent = "[INFO] Starting DiagnosticTest\n[ERROR] Assertion failed on step 4";
        Files.writeString(classDir.resolve("execution.log"), logContent);

        final String jsonContent = "{\"status\": \"error\", \"code\": 500}";
        Files.writeString(classDir.resolve("api-response.json"), jsonContent);

        final String execJson = """
            {
                "id": "diag-exec-101",
                "testClass": "com.example.DiagnosticTest",
                "testMethod": "testApiFailure",
                "status": "failed",
                "failureReason": "API responded with status 500",
                "attachments": [
                    {
                        "name": "api-response.json",
                        "path": "api-response.json",
                        "type": "json",
                        "contentType": "application/json"
                    }
                ]
            }
            """;
        Files.writeString(classDir.resolve("execution-diag-exec-101.json"), execJson);

        final TestExecutionDto dto = dataService.getExecutionDetails(runId, "diag-exec-101");

        Assertions.assertNotNull(dto, "Execution details DTO must be resolved");
        Assertions.assertNotNull(dto.getAttachments(), "Attachments list must not be null");
        Assertions.assertTrue(dto.getAttachments().size() >= 2, "Must resolve both raw and auto-discovered attachments");

        final TestExecutionDto.ExecutionAttachmentDto rawJsonAtt = dto.getAttachments().stream()
            .filter(a -> "api-response.json".equals(a.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(rawJsonAtt, "api-response.json attachment must be present");
        Assertions.assertNotNull(rawJsonAtt.getRelativePath(), "Relative path must be populated");
        Assertions.assertTrue(rawJsonAtt.getRelativePath().replace('\\', '/').endsWith("com.example.DiagnosticTest/api-response.json"));
        Assertions.assertNotNull(rawJsonAtt.getFileUri(), "File URI must be populated");
        Assertions.assertTrue(rawJsonAtt.getFileUri().startsWith("file:/"));
        Assertions.assertEquals(jsonContent, rawJsonAtt.getContent(), "Small JSON content must be inlined");

        final TestExecutionDto.ExecutionAttachmentDto autoLogAtt = dto.getAttachments().stream()
            .filter(a -> "execution.log".equals(a.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(autoLogAtt, "execution.log attachment must be auto-discovered");
        Assertions.assertNotNull(autoLogAtt.getRelativePath(), "Relative path must be populated");
        Assertions.assertTrue(autoLogAtt.getRelativePath().replace('\\', '/').endsWith("com.example.DiagnosticTest/execution.log"));
        Assertions.assertNotNull(autoLogAtt.getFileUri(), "File URI must be populated");
        Assertions.assertTrue(autoLogAtt.getFileUri().startsWith("file:/"));
        Assertions.assertEquals(logContent, autoLogAtt.getContent(), "Small log content must be inlined");
    }
}
