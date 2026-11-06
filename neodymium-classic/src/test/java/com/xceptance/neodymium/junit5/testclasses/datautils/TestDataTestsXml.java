package com.xceptance.neodymium.junit5.testclasses.datautils;

import java.text.MessageFormat;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class TestDataTestsXml
{
    private static final String NIL = "not in list";

    @NeodymiumTest
    @DataSet(id = "asString")
    public void testExists() throws Exception
    {
        Assertions.assertTrue(Neodymium.getData().exists("value"));
        Assertions.assertFalse(Neodymium.getData().exists("notInDataSet"));
    }

    @NeodymiumTest
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

        Assertions.assertEquals("", Neodymium.getData().asString("empty"));
        Assertions.assertEquals("value", Neodymium.getData().asString("value"));
        Assertions.assertEquals("containing strange things like spaces and äüø", Neodymium.getData().asString("sentence"));

        Assertions.assertEquals(null, Neodymium.getData().asString(null, null));
        Assertions.assertEquals(null, Neodymium.getData().asString("", null));
        Assertions.assertEquals(null, Neodymium.getData().asString("nullValue", null));
        Assertions.assertEquals(null, Neodymium.getData().asString(NIL, null));
    }

    @NeodymiumTest
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

        Assertions.assertEquals(3, Neodymium.getData().asInt("positiveValue"));
        Assertions.assertEquals(-3, Neodymium.getData().asInt("negativeValue"));
        Assertions.assertEquals(0, Neodymium.getData().asInt("zeroValue"));

        Assertions.assertEquals(3, Neodymium.getData().asInt(null, 3));
        Assertions.assertEquals(3, Neodymium.getData().asInt("", 3));
        Assertions.assertEquals(3, Neodymium.getData().asInt("nullValue", 3));
        Assertions.assertEquals(3, Neodymium.getData().asInt(NIL, 3));
    }

    @NeodymiumTest
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

        Assertions.assertEquals(3, Neodymium.getData().asLong("positiveValue"));
        Assertions.assertEquals(-3, Neodymium.getData().asLong("negativeValue"));
        Assertions.assertEquals(0, Neodymium.getData().asLong("zeroValue"));

        Assertions.assertEquals(3, Neodymium.getData().asLong(null, 3));
        Assertions.assertEquals(3, Neodymium.getData().asLong("", 3));
        Assertions.assertEquals(3, Neodymium.getData().asLong("nullValue", 3));
        Assertions.assertEquals(3, Neodymium.getData().asLong(NIL, 3));
    }

    @NeodymiumTest
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

        Assertions.assertEquals(3.3, Neodymium.getData().asFloat("positiveValue"), 0.000001);
        Assertions.assertEquals(-3.3, Neodymium.getData().asFloat("negativeValue"), 0.000001);
        Assertions.assertEquals(0, Neodymium.getData().asFloat("zeroValue"), 0.000001);

        Assertions.assertEquals(3, Neodymium.getData().asFloat(null, 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asFloat("", 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asFloat("nullValue", 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asFloat(NIL, 3), 0.000001);
    }

    @NeodymiumTest
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

        Assertions.assertEquals(3.3, Neodymium.getData().asDouble("positiveValue"), 0.000001);
        Assertions.assertEquals(-3.3, Neodymium.getData().asDouble("negativeValue"), 0.000001);
        Assertions.assertEquals(0, Neodymium.getData().asDouble("zeroValue"), 0.000001);

        Assertions.assertEquals(3, Neodymium.getData().asDouble(null, 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asDouble("", 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asDouble("nullValue", 3), 0.000001);
        Assertions.assertEquals(3, Neodymium.getData().asDouble(NIL, 3), 0.000001);
    }

    @NeodymiumTest
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

        Assertions.assertEquals(false, Neodymium.getData().asBoolean("empty"));
        Assertions.assertEquals(true, Neodymium.getData().asBoolean("positiveValue"));
        Assertions.assertEquals(false, Neodymium.getData().asBoolean("negativeValue"));

        Assertions.assertEquals(true, Neodymium.getData().asBoolean(null, true));
        Assertions.assertEquals(true, Neodymium.getData().asBoolean("", true));
        Assertions.assertEquals(true, Neodymium.getData().asBoolean("nullValue", true));
        Assertions.assertEquals(true, Neodymium.getData().asBoolean(NIL, true));
    }

    @NeodymiumTest
    @DataSet(id = "asObject")
    public void testGetClass() throws Exception
    {
        TestCompoundClass testCompound = Neodymium.getData().get(TestCompoundClass.class);

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
        Double numberValue = Neodymium.getData().get("$.numberValue", Double.class);
        Assertions.assertEquals(Double.valueOf(12.34), numberValue);

        String description = Neodymium.getData().get("$.description", String.class);
        Assertions.assertEquals("containing strange things like spaces and äüø", description);

        TestCreditCard creditCard = Neodymium.getData().get("$.creditCard", TestCreditCard.class);
        Assertions.assertEquals("4111111111111111", creditCard.getCardNumber());
        Assertions.assertEquals("123", creditCard.getCcv());
        Assertions.assertEquals(10, creditCard.getMonth());
        Assertions.assertEquals(2018, creditCard.getYear());

        String name = Neodymium.getData().get("$.names[2]", String.class);
        Assertions.assertEquals("ghi", name);

        String lastName = Neodymium.getData().get("$.persons[1].lastName", String.class);
        Assertions.assertEquals("d", lastName);

        TestCompoundClass.Level level = Neodymium.getData().get("$.level", TestCompoundClass.Level.class);
        Assertions.assertEquals(TestCompoundClass.Level.HIGH, level);

        @SuppressWarnings("unchecked")
        List<String> firstNames = Neodymium.getData().get("$.persons[*].firstName", List.class);
        Assertions.assertEquals("a", firstNames.get(0));
        Assertions.assertEquals("c", firstNames.get(1));

        Object nullValue = Neodymium.getData().get("$.nullValue", Object.class);
        Assertions.assertEquals(null, nullValue);

        Object notSet = Neodymium.getData().get("$.notSet", Object.class);
        Assertions.assertEquals(null, notSet);
    }

    @NeodymiumTest
    @DataSet(id = "asObject")
    public void testGetClassByPath() throws Exception
    {
        TestCompoundClass testCompound = Neodymium.getData().get("$", TestCompoundClass.class);

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
