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
package org.neodymium.common.xtc.dto;

// Data Transfer Object for the response data of a test run in the XTC API.
public class ResponseData
{
    private final String name;

    private final String index;

    private final String description;

    private final String startedAt;

    private final String finishedAt;

    private final String state;

    private final String summary;

    private final String rating;

    public ResponseData(String index, String name, String startedAt, String finishedAt, String state, String summary, String description,
                                 String rating)
    {
        this.index = index;
        this.name = name;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.state = state;
        this.summary = summary;
        this.description = description;
        this.rating = rating;
    }

    public String getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public String getFinishedAt()
    {
        return finishedAt;
    }

    public String getState()
    {
        return state;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getDescription()
    {
        return description;
    }

    public String getRating()
    {
        return rating;
    }
}
