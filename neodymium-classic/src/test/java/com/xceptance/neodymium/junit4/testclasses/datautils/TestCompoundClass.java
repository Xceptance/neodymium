package com.xceptance.neodymium.junit4.testclasses.datautils;

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
