package com.xceptance.neodymium.junit4.testclasses.data.set.testid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class SpecialCharacterTestId
{
    static int dataSet = 1;

    @Test
    public void testParanthesis() throws Exception
    {
        switch (dataSet)
        {
            case 1:
                Assert.assertEquals("'", Neodymium.dataValue("testId"));
                break;

            case 2:
                Assert.assertEquals("''", Neodymium.dataValue("testId"));
                break;

            case 3:
                Assert.assertEquals("(", Neodymium.dataValue("testId"));
                break;

            case 4:
                Assert.assertEquals(")", Neodymium.dataValue("testId"));
                break;

            case 5:
                Assert.assertEquals("()", Neodymium.dataValue("testId"));
                break;

            case 6:
                Assert.assertEquals(")(", Neodymium.dataValue("testId"));
                break;

            case 7:
                Assert.assertEquals("(a)", Neodymium.dataValue("testId"));
                break;

            default:
                Assert.fail("uncovered test");
                break;
        }
        dataSet++;
    }
}
