package com.xceptance.neodymium.junit4.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPMultiplicationTest;
import com.xceptance.neodymium.util.Neodymium;

public class WIPTest extends NeodymiumTest
{
    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
        Neodymium.configuration().setProperty("neodymium.workInProgress", "true");
    }

    @Test
    public void testWIPAnnotation() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(WIPAnnotationTest.class, expected);

        Result result = JUnitCore.runClasses(WIPAnnotationTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testWIPAnnotationChild() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: ",
          "third :: "
        };
        checkDescription(WIPAnnotationChildTest.class, expected);

        Result result = JUnitCore.runClasses(WIPAnnotationChildTest.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testWIPMultiplication() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: 1 1 / 2 :: Browser Chrome_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_headless :: ",
          "first :: 1 1 / 2 :: Browser Chrome_1500x1000_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_1500x1000_headless :: ",
        };
        checkDescription(WIPMultiplicationTest.class, expected);
        Result result = JUnitCore.runClasses(WIPMultiplicationTest.class);
        checkPass(result, 4, 0);
    }
}
