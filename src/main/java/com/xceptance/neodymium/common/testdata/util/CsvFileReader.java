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
