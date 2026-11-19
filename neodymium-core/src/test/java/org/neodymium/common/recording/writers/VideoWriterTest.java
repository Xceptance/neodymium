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
package org.neodymium.common.recording.writers;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.util.TestImageGenerator;

/**
 * Unit tests for {@link VideoWriter} verifying FFmpeg encoding flags and framerate adjustment logic.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class VideoWriterTest
{
    @Test
    public void testFfmpegCommandIncludesFaststartFlag() throws Exception
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        final VideoWriter videoWriter = new VideoWriter(config, "target/test-faststart-check.mp4");

        final List<String> command = videoWriter.getProcessBuilder().command();
        Assertions.assertTrue(command.contains("-movflags"), "ProcessBuilder command should contain -movflags");
        final int index = command.indexOf("-movflags");
        Assertions.assertTrue(command.size() > index + 1 && "+faststart".equals(command.get(index + 1)),
            "Parameter following -movflags should be +faststart");
    }

    @Test
    public void testFramerateAdjustmentIncludesFaststartFlag() throws Exception
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        final File testVideoFile = new File("target/test-adjustment-check-" + System.currentTimeMillis() + ".mp4");
        final VideoWriter videoWriter = new VideoWriter(config, testVideoFile.getAbsolutePath());

        try
        {
            videoWriter.start();
            videoWriter.write(TestImageGenerator.generateImage(), 100);
            videoWriter.write(TestImageGenerator.generateImage(), 100);
            videoWriter.stop();

            final List<String> command = videoWriter.getProcessBuilder().command();
            Assertions.assertTrue(command.contains("-movflags"), "Adjusted ProcessBuilder command should contain -movflags");
            final int index = command.indexOf("-movflags");
            Assertions.assertTrue(command.size() > index + 1 && "+faststart".equals(command.get(index + 1)),
                "Parameter following -movflags should be +faststart");
        }
        finally
        {
            if (testVideoFile.exists())
            {
                testVideoFile.delete();
            }
        }
    }

    @Test
    public void testStopWithZeroScreenshotsDeletesEmptyVideoFile() throws Exception
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        final File testVideoFile = new File("target/test-zero-screenshots-" + System.currentTimeMillis() + ".mp4");
        final VideoWriter videoWriter = new VideoWriter(config, testVideoFile.getAbsolutePath());

        try
        {
            videoWriter.start();
            // Stop immediately without capturing any frames (e.g. early failure in @BeforeEach)
            videoWriter.stop();

            Assertions.assertFalse(testVideoFile.exists(), "Empty video file should be discarded and deleted on zero screenshots");
        }
        finally
        {
            if (testVideoFile.exists())
            {
                testVideoFile.delete();
            }
        }
    }
}
