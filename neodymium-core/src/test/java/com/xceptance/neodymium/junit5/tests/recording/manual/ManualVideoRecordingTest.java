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
package com.xceptance.neodymium.junit5.tests.recording.manual;

import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit5.tests.recording.AbstractRecordingTest;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.UUID;

public class ManualVideoRecordingTest extends AbstractRecordingTest
{
    public ManualVideoRecordingTest()
    {
        super(false);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("video", false);
        configurationsClass.put(Thread.currentThread(), VideoRecordingConfigurations.class);
    }

    @BeforeEach
    public void startFilming()
    {
        FilmTestExecution.startVideoRecording(UUID.randomUUID().toString());
    }

    @AfterEach
    public void finishFilming()
    {
        FilmTestExecution.finishVideoFilming(uuid.get(Thread.currentThread()), false);
    }

    @AfterAll
    public static void assertLogFileExists()
    {
        File logFile = new File(logFilePath.get(Thread.currentThread()));
        Assert.assertTrue("the logfile for the manual video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the manual video recording test wasn't deleted", logFile.exists());
    }
}
