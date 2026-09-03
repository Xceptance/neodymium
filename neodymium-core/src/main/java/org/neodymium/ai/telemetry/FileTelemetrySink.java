/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Telemetry sink implementation that writes formatted audit JSON files to disk.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class FileTelemetrySink implements TelemetrySink
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTelemetrySink.class);

    /**
     * Target output file path.
     */
    private final Path outputPath;

    /**
     * Jackson ObjectMapper instance configured with pretty printing.
     */
    private final ObjectMapper mapper;

    /**
     * Constructs a FileTelemetrySink with target output path.
     *
     * @param outputPath target path to write session-telemetry.json
     */
    public FileTelemetrySink(final Path outputPath)
    {
        this.outputPath = outputPath;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Consumes telemetry and writes pretty-printed JSON to disk.
     *
     * @param telemetry current session telemetry
     */
    @Override
    public void consume(final SessionTelemetry telemetry)
    {
        if (this.outputPath == null)
        {
            return;
        }

        try
        {
            final Path parent = this.outputPath.getParent();
            if (parent != null && !Files.exists(parent))
            {
                Files.createDirectories(parent);
            }

            final String json = this.mapper.writeValueAsString(telemetry);
            Files.writeString(
                this.outputPath,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            LOGGER.debug("Session telemetry successfully written to {}", this.outputPath.toAbsolutePath());
        }
        catch (final IOException e)
        {
            LOGGER.warn("Failed to write session telemetry audit file to {}: {}", this.outputPath, e.getMessage(), e);
        }
    }

    /**
     * Gets the output path for this file sink.
     *
     * @return the Path
     */
    public Path getOutputPath()
    {
        return this.outputPath;
    }
}
