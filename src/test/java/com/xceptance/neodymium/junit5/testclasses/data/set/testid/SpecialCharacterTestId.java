package com.xceptance.neodymium.junit5.testclasses.data.set.testid;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class SpecialCharacterTestId
{
    static int dataSet = 1;

    @NeodymiumTest
    public void testParanthesis() throws Exception
    {
        switch (dataSet)
        {
            case 1:
                Assertions.assertEquals("'", Neodymium.dataValue("testId"));
                break;

            case 2:
                Assertions.assertEquals("''", Neodymium.dataValue("testId"));
                break;

            case 3:
                Assertions.assertEquals("(", Neodymium.dataValue("testId"));
                break;

            case 4:
                Assertions.assertEquals(")", Neodymium.dataValue("testId"));
                break;

            case 5:
                Assertions.assertEquals("()", Neodymium.dataValue("testId"));
                break;

            case 6:
                Assertions.assertEquals(")(", Neodymium.dataValue("testId"));
                break;

            case 7:
                Assertions.assertEquals("(a)", Neodymium.dataValue("testId"));
                break;

            default:
                Assertions.fail("uncovered test");
                break;
        }
        dataSet++;
    }
}
