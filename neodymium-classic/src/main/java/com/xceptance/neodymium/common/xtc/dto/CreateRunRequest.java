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
package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for creating a new test run in the XTC API.
public class CreateRunRequest
{
    private final String startedAt;

    private final Integer estimatedDuration;

    private final String name;

    private final String testInstance;

    private final String profile;

    private final String link;

    private final String buildNumber;

    private final String description;

    public CreateRunRequest(String startedAt, Integer estimatedDuration, String name, String testInstance, String profile, String link, String buildNumber,
                            String description)
    {
        this.startedAt = startedAt;
        this.estimatedDuration = estimatedDuration;
        this.name = name;
        this.testInstance = testInstance;
        this.profile = profile;
        this.link = link;
        this.buildNumber = buildNumber;
        this.description = description;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public Integer getEstimatedDuration()
    {
        return estimatedDuration;
    }

    public String getName()
    {
        return name;
    }

    public String getTestInstance()
    {
        return testInstance;
    }

    public String getProfile()
    {
        return profile;
    }

    public String getLink()
    {
        return link;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public String getDescription()
    {
        return description;
    }
}
