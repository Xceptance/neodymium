package com.xceptance.neodymium.junit5.testclasses.datautils;

import java.text.MessageFormat;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

@SuppressBrowsers
public class DataUtilsTestsXml
{
    private static final String NIL = "not in list";

    @NeodymiumTest
    @DataSet(id = "asString")
    public void testExists() throws Exception
    {
        Assertions.assertTrue(DataUtils.exists("value"));
        Assertions.assertFalse(DataUtils.exists("notInDataSet"));
    }

    @NeodymiumTest
    @DataSet(id = "asString")
    public void testAsString() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asString(null);
        });
        expectIAE(() -> {
            DataUtils.asString("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asString("");
        });
        expectIAE(() -> {
            DataUtils.asString(NIL);
        });

        Assertions.assertEquals("", DataUtils.asString("empty"));
        Assertions.assertEquals("value", DataUtils.asString("value"));
        Assertions.assertEquals("containing strange things like spaces and äüø", DataUtils.asString("sentence"));

        Assertions.assertEquals(null, DataUtils.asString(null, null));
        Assertions.assertEquals(null, DataUtils.asString("", null));
        Assertions.assertEquals(null, DataUtils.asString("nullValue", null));
        Assertions.assertEquals(null, DataUtils.asString(NIL, null));
    }

    @NeodymiumTest
    @DataSet(id = "asInt")
    public void testAsInt() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asInt(null);
        });
        expectIAE(() -> {
            DataUtils.asInt("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asInt("");
        });
        expectIAE(() -> {
            DataUtils.asInt(NIL);
        });

        expectNFE(() -> {
            DataUtils.asInt("empty");
        });

        Assertions.assertEquals(3, DataUtils.asInt("positiveValue"));
        Assertions.assertEquals(-3, DataUtils.asInt("negativeValue"));
        Assertions.assertEquals(0, DataUtils.asInt("zeroValue"));

        Assertions.assertEquals(3, DataUtils.asInt(null, 3));
        Assertions.assertEquals(3, DataUtils.asInt("", 3));
        Assertions.assertEquals(3, DataUtils.asInt("nullValue", 3));
        Assertions.assertEquals(3, DataUtils.asInt(NIL, 3));
    }

    @NeodymiumTest
    @DataSet(id = "asLong")
    public void testAsLong() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asLong(null);
        });
        expectIAE(() -> {
            DataUtils.asLong("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asLong("");
        });
        expectIAE(() -> {
            DataUtils.asLong(NIL);
        });

        expectNFE(() -> {
            DataUtils.asLong("empty");
        });

        Assertions.assertEquals(3, DataUtils.asLong("positiveValue"));
        Assertions.assertEquals(-3, DataUtils.asLong("negativeValue"));
        Assertions.assertEquals(0, DataUtils.asLong("zeroValue"));

        Assertions.assertEquals(3, DataUtils.asLong(null, 3));
        Assertions.assertEquals(3, DataUtils.asLong("", 3));
        Assertions.assertEquals(3, DataUtils.asLong("nullValue", 3));
        Assertions.assertEquals(3, DataUtils.asLong(NIL, 3));
    }

    @NeodymiumTest
    @DataSet(id = "asFloat")
    public void testAsFloat() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asFloat(null);
        });
        expectIAE(() -> {
            DataUtils.asFloat("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asFloat("");
        });
        expectIAE(() -> {
            DataUtils.asFloat(NIL);
        });

        expectNFE(() -> {
            DataUtils.asFloat("empty");
        });

        Assertions.assertEquals(3.3, DataUtils.asFloat("positiveValue"), 0.000001);
        Assertions.assertEquals(-3.3, DataUtils.asFloat("negativeValue"), 0.000001);
        Assertions.assertEquals(0, DataUtils.asFloat("zeroValue"), 0.000001);

        Assertions.assertEquals(3, DataUtils.asFloat(null, 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asFloat("", 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asFloat("nullValue", 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asFloat(NIL, 3), 0.000001);
    }

    @NeodymiumTest
    @DataSet(id = "asDouble")
    public void testAsDouble() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asDouble(null);
        });
        expectIAE(() -> {
            DataUtils.asDouble("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asDouble("");
        });
        expectIAE(() -> {
            DataUtils.asDouble(NIL);
        });

        expectNFE(() -> {
            DataUtils.asDouble("empty");
        });

        Assertions.assertEquals(3.3, DataUtils.asDouble("positiveValue"), 0.000001);
        Assertions.assertEquals(-3.3, DataUtils.asDouble("negativeValue"), 0.000001);
        Assertions.assertEquals(0, DataUtils.asDouble("zeroValue"), 0.000001);

        Assertions.assertEquals(3, DataUtils.asDouble(null, 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asDouble("", 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asDouble("nullValue", 3), 0.000001);
        Assertions.assertEquals(3, DataUtils.asDouble(NIL, 3), 0.000001);
    }

    @NeodymiumTest
    @DataSet(id = "asBoolean")
    public void testAsBoolean() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            DataUtils.asBool(null);
        });
        expectIAE(() -> {
            DataUtils.asBool("nullValue");
        });
        expectIAE(() -> {
            DataUtils.asBool("");
        });
        expectIAE(() -> {
            DataUtils.asBool(NIL);
        });

        Assertions.assertEquals(false, DataUtils.asBool("empty"));
        Assertions.assertEquals(true, DataUtils.asBool("positiveValue"));
        Assertions.assertEquals(false, DataUtils.asBool("negativeValue"));

        Assertions.assertEquals(true, DataUtils.asBool(null, true));
        Assertions.assertEquals(true, DataUtils.asBool("", true));
        Assertions.assertEquals(true, DataUtils.asBool("nullValue", true));
        Assertions.assertEquals(true, DataUtils.asBool(NIL, true));
    }

    @NeodymiumTest
    @DataSet(id = "asObject")
    public void testGetClass() throws Exception
    {
        TestCompoundClass testCompound = DataUtils.get(TestCompoundClass.class);

        Assertions.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assertions.assertEquals(null, testCompound.getNotSet());
        // our XML parer does not support explicit null value
        // Assertions.assertEquals(null, testCompound.getNullValue());
        Assertions.assertEquals(Double.valueOf(12.34), testCompound.getNumberValue());
        Assertions.assertEquals("containing strange things like spaces and äüø", testCompound.getDescription());
        Assertions.assertEquals("4111111111111111", testCompound.getCreditCard().getCardNumber());
        Assertions.assertEquals("123", testCompound.getCreditCard().getCcv());
        Assertions.assertEquals(10, testCompound.getCreditCard().getMonth());
        Assertions.assertEquals(2018, testCompound.getCreditCard().getYear());
        Assertions.assertEquals(23, testCompound.getAge());
        Assertions.assertEquals(3, testCompound.getNames().size());
        Assertions.assertEquals("abc", testCompound.getNames().get(0));
        Assertions.assertEquals("def", testCompound.getNames().get(1));
        Assertions.assertEquals("ghi", testCompound.getNames().get(2));
        Assertions.assertEquals(2, testCompound.getPersons().size());
        Assertions.assertEquals("a", testCompound.getPersons().get(0).getFirstName());
        Assertions.assertEquals("b", testCompound.getPersons().get(0).getLastName());
        Assertions.assertEquals("c", testCompound.getPersons().get(1).getFirstName());
        Assertions.assertEquals("d", testCompound.getPersons().get(1).getLastName());
        Assertions.assertEquals("value", testCompound.getKeyValueMap().get("key"));
        Assertions.assertEquals(TestCompoundClass.Level.HIGH, testCompound.getLevel());
    }

    @NeodymiumTest
    @DataSet(id = "asObject")
    public void testGetByPath() throws Exception
    {
        Double numberValue = DataUtils.get("$.numberValue", Double.class);
        Assertions.assertEquals(Double.valueOf(12.34), numberValue);

        String description = DataUtils.get("$.description", String.class);
        Assertions.assertEquals("containing strange things like spaces and äüø", description);

        TestCreditCard creditCard = DataUtils.get("$.creditCard", TestCreditCard.class);
        Assertions.assertEquals("4111111111111111", creditCard.getCardNumber());
        Assertions.assertEquals("123", creditCard.getCcv());
        Assertions.assertEquals(10, creditCard.getMonth());
        Assertions.assertEquals(2018, creditCard.getYear());

        String name = DataUtils.get("$.names[2]", String.class);
        Assertions.assertEquals("ghi", name);

        String lastName = DataUtils.get("$.persons[1].lastName", String.class);
        Assertions.assertEquals("d", lastName);

        TestCompoundClass.Level level = DataUtils.get("$.level", TestCompoundClass.Level.class);
        Assertions.assertEquals(TestCompoundClass.Level.HIGH, level);

        @SuppressWarnings("unchecked")
        List<String> firstNames = DataUtils.get("$.persons[*].firstName", List.class);
        Assertions.assertEquals("a", firstNames.get(0));
        Assertions.assertEquals("c", firstNames.get(1));

        Object nullValue = DataUtils.get("$.nullValue", Object.class);
        Assertions.assertEquals(null, nullValue);

        Object notSet = DataUtils.get("$.notSet", Object.class);
        Assertions.assertEquals(null, notSet);
    }

    @NeodymiumTest
    @DataSet(id = "asObject")
    public void testGetClassByPath() throws Exception
    {
        TestCompoundClass testCompound = DataUtils.get("$", TestCompoundClass.class);

        Assertions.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assertions.assertEquals(null, testCompound.getNotSet());
        // our XML parer does not support explicit null value
        // Assertions.assertEquals(null, testCompound.getNullValue());
        Assertions.assertEquals(Double.valueOf(12.34), testCompound.getNumberValue());
        Assertions.assertEquals("containing strange things like spaces and äüø", testCompound.getDescription());
        Assertions.assertEquals("4111111111111111", testCompound.getCreditCard().getCardNumber());
        Assertions.assertEquals("123", testCompound.getCreditCard().getCcv());
        Assertions.assertEquals(10, testCompound.getCreditCard().getMonth());
        Assertions.assertEquals(2018, testCompound.getCreditCard().getYear());
        Assertions.assertEquals(23, testCompound.getAge());
        Assertions.assertEquals(3, testCompound.getNames().size());
        Assertions.assertEquals("abc", testCompound.getNames().get(0));
        Assertions.assertEquals("def", testCompound.getNames().get(1));
        Assertions.assertEquals("ghi", testCompound.getNames().get(2));
        Assertions.assertEquals(2, testCompound.getPersons().size());
        Assertions.assertEquals("a", testCompound.getPersons().get(0).getFirstName());
        Assertions.assertEquals("b", testCompound.getPersons().get(0).getLastName());
        Assertions.assertEquals("c", testCompound.getPersons().get(1).getFirstName());
        Assertions.assertEquals("d", testCompound.getPersons().get(1).getLastName());
        Assertions.assertEquals("value", testCompound.getKeyValueMap().get("key"));
        Assertions.assertEquals(TestCompoundClass.Level.HIGH, testCompound.getLevel());
    }

    private void expectIAE(Runnable function)
    {
        expectException(function, IllegalArgumentException.class);
    }

    private void expectNFE(Runnable function)
    {
        expectException(function, NumberFormatException.class);
    }

    private void expectException(Runnable function, Class<? extends Throwable> expectedException)
    {
        Throwable caughtException = null;

        try
        {
            function.run();
        }
        catch (Throwable e)
        {
            caughtException = e;
        }

        String caughtExceptionName = "no exception!";
        if (caughtException != null)
            caughtExceptionName = caughtException.getClass().toString();

        if (caughtException == null || caughtException.getClass() != expectedException)
        {
            Assertions.fail(MessageFormat.format("Expected exception {0} but caught {1}", expectedException.toString(), caughtExceptionName));
        }
    }

}
