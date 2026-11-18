/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.recording.config;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Mutable;

/**
 * Configuration interface, which contains the variables needed for both gif and video recording.
 * 
 * @author Xceptance Software Technologies
 */
@LoadPolicy(LoadType.MERGE)
public abstract interface RecordingConfigurations extends Mutable
{
    public boolean enableFilming();

    public boolean filmAutomatically();

    public int oneImagePerMilliseconds();

    public String tempFolderToStoreRecording();

    public boolean deleteRecordingsAfterAddingToAllureReport();

    public boolean deleteTempRecordings();

    public boolean appendAllRecordingsToAllureReport();

    public double imageQuality();

    public double imageScaleFactor();

    public String format();

    public boolean logInformationAboutRecording();
}
