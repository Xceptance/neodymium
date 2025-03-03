package com.xceptance.neodymium.junit4.testclasses.datautils;

import java.text.MessageFormat;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
public class DataUtilsTests
{
    private static final String NIL = "not in list";

    @Test
    @DataSet(id = "asString")
    public void testExists() throws Exception
    {
        Assert.assertTrue(DataUtils.exists("value"));
        Assert.assertFalse(DataUtils.exists("notInDataSet"));
    }

    @Test
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

        Assert.assertEquals("", DataUtils.asString("empty"));
        Assert.assertEquals("value", DataUtils.asString("value"));
        Assert.assertEquals("containing strange things like spaces and äüø", DataUtils.asString("sentence"));

        Assert.assertEquals(null, DataUtils.asString(null, null));
        Assert.assertEquals(null, DataUtils.asString("", null));
        Assert.assertEquals(null, DataUtils.asString("nullValue", null));
        Assert.assertEquals(null, DataUtils.asString(NIL, null));

    }

    @Test
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

        Assert.assertEquals(3, DataUtils.asInt("positiveValue"));
        Assert.assertEquals(-3, DataUtils.asInt("negativeValue"));
        Assert.assertEquals(0, DataUtils.asInt("zeroValue"));

        Assert.assertEquals(3, DataUtils.asInt(null, 3));
        Assert.assertEquals(3, DataUtils.asInt("", 3));
        Assert.assertEquals(3, DataUtils.asInt("nullValue", 3));
        Assert.assertEquals(3, DataUtils.asInt(NIL, 3));
    }

    @Test
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

        Assert.assertEquals(3, DataUtils.asLong("positiveValue"));
        Assert.assertEquals(-3, DataUtils.asLong("negativeValue"));
        Assert.assertEquals(0, DataUtils.asLong("zeroValue"));

        Assert.assertEquals(3, DataUtils.asLong(null, 3));
        Assert.assertEquals(3, DataUtils.asLong("", 3));
        Assert.assertEquals(3, DataUtils.asLong("nullValue", 3));
        Assert.assertEquals(3, DataUtils.asLong(NIL, 3));
    }

    @Test
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

        Assert.assertEquals(3.3, DataUtils.asFloat("positiveValue"), 0.000001);
        Assert.assertEquals(-3.3, DataUtils.asFloat("negativeValue"), 0.000001);
        Assert.assertEquals(0, DataUtils.asFloat("zeroValue"), 0.000001);

        Assert.assertEquals(3, DataUtils.asFloat(null, 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asFloat("", 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asFloat("nullValue", 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asFloat(NIL, 3), 0.000001);
    }

    @Test
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

        Assert.assertEquals(3.3, DataUtils.asDouble("positiveValue"), 0.000001);
        Assert.assertEquals(-3.3, DataUtils.asDouble("negativeValue"), 0.000001);
        Assert.assertEquals(0, DataUtils.asDouble("zeroValue"), 0.000001);

        Assert.assertEquals(3, DataUtils.asDouble(null, 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asDouble("", 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asDouble("nullValue", 3), 0.000001);
        Assert.assertEquals(3, DataUtils.asDouble(NIL, 3), 0.000001);
    }

    @Test
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

        Assert.assertEquals(false, DataUtils.asBool("empty"));
        Assert.assertEquals(true, DataUtils.asBool("positiveValue"));
        Assert.assertEquals(false, DataUtils.asBool("negativeValue"));

        Assert.assertEquals(true, DataUtils.asBool(null, true));
        Assert.assertEquals(true, DataUtils.asBool("", true));
        Assert.assertEquals(true, DataUtils.asBool("nullValue", true));
        Assert.assertEquals(true, DataUtils.asBool(NIL, true));
    }

    @Test
    @DataSet(id = "asObject")
    public void testGetClass() throws Exception
    {
        TestCompoundClass testCompound = DataUtils.get(TestCompoundClass.class);

        Assert.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assert.assertEquals(null, testCompound.getNotSet());
        Assert.assertEquals(null, testCompound.getNullValue());
        Assert.assertEquals(Double.valueOf(12.34), testCompound.getNumberValue());
        Assert.assertEquals("containing strange things like spaces and äüø", testCompound.getDescription());
        Assert.assertEquals("4111111111111111", testCompound.getCreditCard().getCardNumber());
        Assert.assertEquals("123", testCompound.getCreditCard().getCcv());
        Assert.assertEquals(10, testCompound.getCreditCard().getMonth());
        Assert.assertEquals(2018, testCompound.getCreditCard().getYear());
        Assert.assertEquals(23, testCompound.getAge());
        Assert.assertEquals(3, testCompound.getNames().size());
        Assert.assertEquals("abc", testCompound.getNames().get(0));
        Assert.assertEquals("def", testCompound.getNames().get(1));
        Assert.assertEquals("ghi", testCompound.getNames().get(2));
        Assert.assertEquals(2, testCompound.getPersons().size());
        Assert.assertEquals("a", testCompound.getPersons().get(0).getFirstName());
        Assert.assertEquals("b", testCompound.getPersons().get(0).getLastName());
        Assert.assertEquals("c", testCompound.getPersons().get(1).getFirstName());
        Assert.assertEquals("d", testCompound.getPersons().get(1).getLastName());
        Assert.assertEquals("value", testCompound.getKeyValueMap().get("key"));
        Assert.assertEquals(TestCompoundClass.Level.HIGH, testCompound.getLevel());
    }

    @Test
    @DataSet(id = "asObject")
    public void testGetByPath() throws Exception
    {
        Double numberValue = DataUtils.get("$.numberValue", Double.class);
        Assert.assertEquals(Double.valueOf(12.34), numberValue);

        String description = DataUtils.get("$.description", String.class);
        Assert.assertEquals("containing strange things like spaces and äüø", description);

        TestCreditCard creditCard = DataUtils.get("$.creditCard", TestCreditCard.class);
        Assert.assertEquals("4111111111111111", creditCard.getCardNumber());
        Assert.assertEquals("123", creditCard.getCcv());
        Assert.assertEquals(10, creditCard.getMonth());
        Assert.assertEquals(2018, creditCard.getYear());

        String name = DataUtils.get("$.names[2]", String.class);
        Assert.assertEquals("ghi", name);

        String lastName = DataUtils.get("$.persons[1].lastName", String.class);
        Assert.assertEquals("d", lastName);

        TestCompoundClass.Level level = DataUtils.get("$.level", TestCompoundClass.Level.class);
        Assert.assertEquals(TestCompoundClass.Level.HIGH, level);

        @SuppressWarnings("unchecked")
        List<String> firstNames = DataUtils.get("$.persons[*].firstName", List.class);
        Assert.assertEquals("a", firstNames.get(0));
        Assert.assertEquals("c", firstNames.get(1));

        Object nullValue = DataUtils.get("$.nullValue", Object.class);
        Assert.assertEquals(null, nullValue);

        Object notSet = DataUtils.get("$.notSet", Object.class);
        Assert.assertEquals(null, notSet);
    }

    @Test
    @DataSet(id = "asObject")
    public void testGetClassByPath() throws Exception
    {
        TestCompoundClass testCompound = DataUtils.get("$", TestCompoundClass.class);

        Assert.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assert.assertEquals(null, testCompound.getNotSet());
        Assert.assertEquals(null, testCompound.getNullValue());
        Assert.assertEquals(Double.valueOf(12.34), testCompound.getNumberValue());
        Assert.assertEquals("containing strange things like spaces and äüø", testCompound.getDescription());
        Assert.assertEquals("4111111111111111", testCompound.getCreditCard().getCardNumber());
        Assert.assertEquals("123", testCompound.getCreditCard().getCcv());
        Assert.assertEquals(10, testCompound.getCreditCard().getMonth());
        Assert.assertEquals(2018, testCompound.getCreditCard().getYear());
        Assert.assertEquals(23, testCompound.getAge());
        Assert.assertEquals(3, testCompound.getNames().size());
        Assert.assertEquals("abc", testCompound.getNames().get(0));
        Assert.assertEquals("def", testCompound.getNames().get(1));
        Assert.assertEquals("ghi", testCompound.getNames().get(2));
        Assert.assertEquals(2, testCompound.getPersons().size());
        Assert.assertEquals("a", testCompound.getPersons().get(0).getFirstName());
        Assert.assertEquals("b", testCompound.getPersons().get(0).getLastName());
        Assert.assertEquals("c", testCompound.getPersons().get(1).getFirstName());
        Assert.assertEquals("d", testCompound.getPersons().get(1).getLastName());
        Assert.assertEquals("value", testCompound.getKeyValueMap().get("key"));
        Assert.assertEquals(TestCompoundClass.Level.HIGH, testCompound.getLevel());
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
            Assert.fail(MessageFormat.format("Expected exception {0} but caught {1}", expectedException.toString(), caughtExceptionName));
        }
    }

}
