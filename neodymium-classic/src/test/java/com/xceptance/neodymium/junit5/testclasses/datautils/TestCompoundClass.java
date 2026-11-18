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
package com.xceptance.neodymium.junit5.testclasses.datautils;

import java.util.List;
import java.util.Map;

public class TestCompoundClass
{
    enum Level
    {
        LOW,
        MEDIUM,
        HIGH
    }

    private String clubCardNumber;

    private String description;

    private Object notSet;

    private Double numberValue;

    private Object nullValue = "notNullString";

    private int age;

    private TestCreditCard creditCard;

    private List<String> names;

    private List<TestPerson> persons;

    private Map<String, String> keyValueMap;

    private Level level;

    public List<TestPerson> getPersons()
    {
        return persons;
    }

    public List<String> getNames()
    {
        return names;
    }

    public int getAge()
    {
        return age;
    }

    public String getClubCardNumber()
    {
        return clubCardNumber;
    }

    public TestCreditCard getCreditCard()
    {
        return creditCard;
    }

    public Map<String, String> getKeyValueMap()
    {
        return keyValueMap;
    }

    public Level getLevel()
    {
        return level;
    }

    public String getDescription()
    {
        return description;
    }

    public Double getNumberValue()
    {
        return numberValue;
    }

    public Object getNullValue()
    {
        return nullValue;
    }

    public Object getNotSet()
    {
        return notSet;
    }
}
