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
package com.xceptance.neodymium.common.recording;

/**
 * @deprecated Use {@link org.neodymium.common.recording.TakeScreenshotsThread} instead.
 */
@Deprecated
public class TakeScreenshotsThread extends org.neodymium.common.recording.TakeScreenshotsThread
{
    public TakeScreenshotsThread(org.openqa.selenium.WebDriver driver, java.lang.Class<? extends org.neodymium.common.recording.writers.Writer> writerClass, org.neodymium.common.recording.config.RecordingConfigurations recordingConfigurations, java.lang.String testName) throws java.io.IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, java.lang.reflect.InvocationTargetException
    {
        super(driver, writerClass, recordingConfigurations, testName);
    }
}
