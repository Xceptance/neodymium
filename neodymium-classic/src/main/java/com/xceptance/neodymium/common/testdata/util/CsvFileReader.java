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
package com.xceptance.neodymium.common.testdata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvFileReader
{
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader().withCommentMarker('#');

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /**
     * Get test data from CSV input stream
     * 
     * @param inputStream
     * @return
     */
    public static List<Map<String, String>> readFile(InputStream inputStream)
    {
        List<Map<String, String>> data = new LinkedList<>();
        CSVParser csvParser;
        try
        {
            csvParser = CSVParser.parse(inputStream, CHARSET_UTF8, CSV_FORMAT);
            for (CSVRecord record : csvParser.getRecords())
            {
                data.add(record.toMap());
            }

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return data;
    }

    /**
     * Get test data from CSV file
     * 
     * @param file
     * @return
     */
    public static List<Map<String, String>> readFile(File file)
    {
        try
        {
            return readFile(new FileInputStream(file));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
