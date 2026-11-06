package com.xceptance.neodymium.junit4.testclasses.datautils;

import java.text.MessageFormat;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class TestDataTestsXml
{
    private static final String NIL = "not in list";

    @Test
    @DataSet(id = "asString")
    public void testExists() throws Exception
    {
        Assert.assertTrue(Neodymium.getData().exists("value"));
        Assert.assertFalse(Neodymium.getData().exists("notInDataSet"));
    }

    @Test
    @DataSet(id = "asString")
    public void testAsString() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asString(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asString("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asString("");
        });
        expectIAE(() -> {
            Neodymium.getData().asString(NIL);
        });

        Assert.assertEquals("", Neodymium.getData().asString("empty"));
        Assert.assertEquals("value", Neodymium.getData().asString("value"));
        Assert.assertEquals("containing strange things like spaces and äüø", Neodymium.getData().asString("sentence"));

        Assert.assertEquals(null, Neodymium.getData().asString(null, null));
        Assert.assertEquals(null, Neodymium.getData().asString("", null));
        Assert.assertEquals(null, Neodymium.getData().asString("nullValue", null));
        Assert.assertEquals(null, Neodymium.getData().asString(NIL, null));
    }

    @Test
    @DataSet(id = "asInt")
    public void testAsInt() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asInt(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asInt("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asInt("");
        });
        expectIAE(() -> {
            Neodymium.getData().asInt(NIL);
        });
        expectNFE(() -> {
            Neodymium.getData().asInt("empty");
        });

        Assert.assertEquals(3, Neodymium.getData().asInt("positiveValue"));
        Assert.assertEquals(-3, Neodymium.getData().asInt("negativeValue"));
        Assert.assertEquals(0, Neodymium.getData().asInt("zeroValue"));

        Assert.assertEquals(3, Neodymium.getData().asInt(null, 3));
        Assert.assertEquals(3, Neodymium.getData().asInt("", 3));
        Assert.assertEquals(3, Neodymium.getData().asInt("nullValue", 3));
        Assert.assertEquals(3, Neodymium.getData().asInt(NIL, 3));
    }

    @Test
    @DataSet(id = "asLong")
    public void testAsLong() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asLong(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asLong("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asLong("");
        });
        expectIAE(() -> {
            Neodymium.getData().asLong(NIL);
        });
        expectNFE(() -> {
            Neodymium.getData().asLong("empty");
        });

        Assert.assertEquals(3, Neodymium.getData().asLong("positiveValue"));
        Assert.assertEquals(-3, Neodymium.getData().asLong("negativeValue"));
        Assert.assertEquals(0, Neodymium.getData().asLong("zeroValue"));

        Assert.assertEquals(3, Neodymium.getData().asLong(null, 3));
        Assert.assertEquals(3, Neodymium.getData().asLong("", 3));
        Assert.assertEquals(3, Neodymium.getData().asLong("nullValue", 3));
        Assert.assertEquals(3, Neodymium.getData().asLong(NIL, 3));
    }

    @Test
    @DataSet(id = "asFloat")
    public void testAsFloat() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asFloat(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asFloat("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asFloat("");
        });
        expectIAE(() -> {
            Neodymium.getData().asFloat(NIL);
        });
        expectNFE(() -> {
            Neodymium.getData().asFloat("empty");
        });

        Assert.assertEquals(3.3, Neodymium.getData().asFloat("positiveValue"), 0.000001);
        Assert.assertEquals(-3.3, Neodymium.getData().asFloat("negativeValue"), 0.000001);
        Assert.assertEquals(0, Neodymium.getData().asFloat("zeroValue"), 0.000001);

        Assert.assertEquals(3, Neodymium.getData().asFloat(null, 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asFloat("", 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asFloat("nullValue", 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asFloat(NIL, 3), 0.000001);
    }

    @Test
    @DataSet(id = "asDouble")
    public void testAsDouble() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asDouble(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asDouble("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asDouble("");
        });
        expectIAE(() -> {
            Neodymium.getData().asDouble(NIL);
        });
        expectNFE(() -> {
            Neodymium.getData().asDouble("empty");
        });

        Assert.assertEquals(3.3, Neodymium.getData().asDouble("positiveValue"), 0.000001);
        Assert.assertEquals(-3.3, Neodymium.getData().asDouble("negativeValue"), 0.000001);
        Assert.assertEquals(0, Neodymium.getData().asDouble("zeroValue"), 0.000001);

        Assert.assertEquals(3, Neodymium.getData().asDouble(null, 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asDouble("", 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asDouble("nullValue", 3), 0.000001);
        Assert.assertEquals(3, Neodymium.getData().asDouble(NIL, 3), 0.000001);
    }

    @Test
    @DataSet(id = "asBoolean")
    public void testAsBoolean() throws Exception
    {
        // expect IllegalArgumentException
        expectIAE(() -> {
            Neodymium.getData().asBoolean(null);
        });
        expectIAE(() -> {
            Neodymium.getData().asBoolean("nullValue");
        });
        expectIAE(() -> {
            Neodymium.getData().asBoolean("");
        });
        expectIAE(() -> {
            Neodymium.getData().asBoolean(NIL);
        });

        Assert.assertEquals(false, Neodymium.getData().asBoolean("empty"));
        Assert.assertEquals(true, Neodymium.getData().asBoolean("positiveValue"));
        Assert.assertEquals(false, Neodymium.getData().asBoolean("negativeValue"));

        Assert.assertEquals(true, Neodymium.getData().asBoolean(null, true));
        Assert.assertEquals(true, Neodymium.getData().asBoolean("", true));
        Assert.assertEquals(true, Neodymium.getData().asBoolean("nullValue", true));
        Assert.assertEquals(true, Neodymium.getData().asBoolean(NIL, true));
    }

    @Test
    @DataSet(id = "asObject")
    public void testGetClass() throws Exception
    {
        TestCompoundClass testCompound = Neodymium.getData().get(TestCompoundClass.class);

        Assert.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assert.assertEquals(null, testCompound.getNotSet());
        // our XML parer does not support explicit null value
        // Assert.assertEquals(null, testCompound.getNullValue());
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
        Double numberValue = Neodymium.getData().get("$.numberValue", Double.class);
        Assert.assertEquals(Double.valueOf(12.34), numberValue);

        String description = Neodymium.getData().get("$.description", String.class);
        Assert.assertEquals("containing strange things like spaces and äüø", description);

        TestCreditCard creditCard = Neodymium.getData().get("$.creditCard", TestCreditCard.class);
        Assert.assertEquals("4111111111111111", creditCard.getCardNumber());
        Assert.assertEquals("123", creditCard.getCcv());
        Assert.assertEquals(10, creditCard.getMonth());
        Assert.assertEquals(2018, creditCard.getYear());

        String name = Neodymium.getData().get("$.names[2]", String.class);
        Assert.assertEquals("ghi", name);

        String lastName = Neodymium.getData().get("$.persons[1].lastName", String.class);
        Assert.assertEquals("d", lastName);

        TestCompoundClass.Level level = Neodymium.getData().get("$.level", TestCompoundClass.Level.class);
        Assert.assertEquals(TestCompoundClass.Level.HIGH, level);

        @SuppressWarnings("unchecked")
        List<String> firstNames = Neodymium.getData().get("$.persons[*].firstName", List.class);
        Assert.assertEquals("a", firstNames.get(0));
        Assert.assertEquals("c", firstNames.get(1));

        Object nullValue = Neodymium.getData().get("$.nullValue", Object.class);
        Assert.assertEquals(null, nullValue);

        Object notSet = Neodymium.getData().get("$.notSet", Object.class);
        Assert.assertEquals(null, notSet);
    }

    @Test
    @DataSet(id = "asObject")
    public void testGetClassByPath() throws Exception
    {
        TestCompoundClass testCompound = Neodymium.getData().get("$", TestCompoundClass.class);

        Assert.assertEquals("1234567890", testCompound.getClubCardNumber());
        Assert.assertEquals(null, testCompound.getNotSet());
        // our XML parer does not support explicit null value
        // Assert.assertEquals(null, testCompound.getNullValue());
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
