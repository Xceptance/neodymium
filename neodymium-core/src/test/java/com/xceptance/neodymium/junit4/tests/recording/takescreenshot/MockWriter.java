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
package com.xceptance.neodymium.junit4.tests.recording.takescreenshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.neodymium.common.recording.config.RecordingConfigurations;
import org.neodymium.common.recording.writers.Writer;

public class MockWriter implements Writer
{
    public static List<File> screenshots;

    public MockWriter(RecordingConfigurations recordingConfigurations, String gifFileName)
    {
    }

    @Override
    public void start() throws IOException
    {
        screenshots = new ArrayList<>();
    }

    @Override
    public void write(File image, long delay)
    {
        screenshots.add(image);
    }

    @Override
    public void stop()
    {
        Assert.assertNotNull("start method was probably not called", screenshots);
        Assert.assertTrue("no screenshots were taken", screenshots.size() > 0);
    }
}