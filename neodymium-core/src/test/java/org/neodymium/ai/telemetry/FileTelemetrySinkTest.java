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
package org.neodymium.ai.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FileTelemetrySink}.
 * Validates writing telemetry metrics to disk in JSON format.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class FileTelemetrySinkTest
{
    @Test
    public void testConsumeWritesTelemetryToFile(@TempDir final Path tempDir) throws IOException
    {
        final Path outputPath = tempDir.resolve("telemetry/session-telemetry.json");
        final FileTelemetrySink sink = new FileTelemetrySink(outputPath);

        final SessionTelemetry telemetry = new SessionTelemetry(1, 0, 10, 5, 0, 100L, 0.001);

        sink.consume(telemetry);

        assertTrue(Files.exists(outputPath), "Telemetry sink should create telemetry output file on disk.");
        final String content = Files.readString(outputPath);
        assertTrue(content.contains("\"totalSteps\" : 1"), "File content should contain recorded total steps metric.");
    }
}
