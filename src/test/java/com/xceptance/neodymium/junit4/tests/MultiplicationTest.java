package com.xceptance.neodymium.junit4.tests;

import org.junit.Before;
import org.junit.Test;

import com.xceptance.neodymium.junit4.testclasses.multiplication.TwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.TwoMethodsAndOneIgnored;
import com.xceptance.neodymium.junit4.testclasses.multiplication.TwoMethodsOneIgnored;
import com.xceptance.neodymium.junit4.testclasses.multiplication.dataset.OneDataSetOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.dataset.OneDataSetTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.dataset.TwoDataSetsOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.dataset.TwoDataSetsTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameter.OneParameterSetOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameter.OneParameterSetTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameter.TwoParameterSetsOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameter.TwoParameterSetsTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.OneDataSetOneParameterSetOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.OneDataSetOneParameterSetTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.OneDataSetTwoParameterSetsOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.OneDataSetTwoParameterSetsTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.TwoDataSetsOneParameterSetOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.TwoDataSetsOneParameterSetTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.TwoDataSetsTwoParameterSetsOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset.TwoDataSetsTwoParameterSetsTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.pkgdata.PackageDataDoNotAffectMethodMultiplication;
import com.xceptance.neodymium.util.Neodymium;

public class MultiplicationTest extends NeodymiumTest
{

    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
    }

    //////////////////
    // Methods only //
    //////////////////

    @Test
    public void testTwoMethods() throws Throwable
    {
        // two test methods, no data
        String[] expected = new String[]
        {
          "first", "second"
        };
        checkDescription(TwoMethods.class, expected);
    }

    @Test
    public void testTwoMethodsAndOneIgnored() throws Throwable
    {
        // two test methods and one ignored, no data
        String[] expected = new String[]
        {
          "first", "second"
        };
        checkDescription(TwoMethodsAndOneIgnored.class, expected);
    }

    @Test
    public void testTwoMethodsOneIgnored() throws Throwable
    {
        // two methods one of them is ignored, no data
        String[] expected = new String[]
        {
          "first", "second"
        };
        checkDescription(TwoMethodsOneIgnored.class, expected);
    }

    //////////////////////////////
    // Methods and package data //
    //////////////////////////////

    @Test
    public void testPackageDataDoNotAffectMethodMultiplication() throws Throwable
    {
        // two methods, package test data do not affect multiplication
        String[] expected = new String[]
        {
          "first :: TestData", "second :: TestData"
        };
        checkDescription(PackageDataDoNotAffectMethodMultiplication.class, expected);

    }

    ///////////////////////////
    // Methods and data sets //
    ///////////////////////////

    @Test
    public void testOneDataSetOneMethod() throws Throwable
    {
        // one test method, one data set
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1"
        };
        checkDescription(OneDataSetOneMethod.class, expected);
    }

    @Test
    public void testOneDataSetTwoMethods() throws Throwable
    {
        // two test methods, one data set
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1", //
          "second :: Data set 1 / 1"
        };
        checkDescription(OneDataSetTwoMethods.class, expected);
    }

    @Test
    public void testTwoDataSetsOneMethod() throws Throwable
    {
        // one test method but two data sets
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2", //
          "first :: Data set 2 / 2"
        };
        checkDescription(TwoDataSetsOneMethod.class, expected);

    }

    @Test
    public void testTwoDataSetsTwoMethods() throws Throwable
    {
        // two methods, two data sets
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2", //
          "first :: Data set 2 / 2", //
          "second :: Data set 1 / 2", //
          "second :: Data set 2 / 2", //
        };
        checkDescription(TwoDataSetsTwoMethods.class, expected);
    }

    ///////////////////////////
    // Methods and parameter //
    ///////////////////////////

    @Test
    public void testOneParameterSetOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: [0]"
        };
        checkDescription(OneParameterSetOneMethod.class, expected);
    }

    @Test
    public void testOneParameterSetTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: [0]", //
          "second :: [0]"
        };
        checkDescription(OneParameterSetTwoMethods.class, expected);
    }

    @Test
    public void testTwoParameterSetsOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: [0]", //
          "first :: [1]"
        };
        checkDescription(TwoParameterSetsOneMethod.class, expected);
    }

    @Test
    public void testTwoParameterSetsTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: [0]", //
          "first :: [1]", //
          "second :: [0]", //
          "second :: [1]"
        };
        checkDescription(TwoParameterSetsTwoMethods.class, expected);
    }

    ////////////////////////////////////////
    // Methods and parameter and data sets//
    ////////////////////////////////////////

    @Test
    public void testOneDataSetOneParameterSetOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1 :: [0]" //
        };
        checkDescription(OneDataSetOneParameterSetOneMethod.class, expected);
    }

    @Test
    public void testOneDataSetOneParameterSetTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1 :: [0]", //
          "second :: Data set 1 / 1 :: [0]"
        };
        checkDescription(OneDataSetOneParameterSetTwoMethods.class, expected);
    }

    @Test
    public void testOneDataSetTwoParameterSetsOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1 :: [0]", //
          "first :: Data set 1 / 1 :: [1]"
        };
        checkDescription(OneDataSetTwoParameterSetsOneMethod.class, expected);
    }

    @Test
    public void testOneDataSetTwoParameterSetsTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 1 :: [0]", //
          "first :: Data set 1 / 1 :: [1]", //
          "second :: Data set 1 / 1 :: [0]", //
          "second :: Data set 1 / 1 :: [1]"
        };
        checkDescription(OneDataSetTwoParameterSetsTwoMethods.class, expected);
    }

    @Test
    public void testTwoDataSetsOneParameterSetOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2 :: [0]", //
          "first :: Data set 2 / 2 :: [0]"
        };
        checkDescription(TwoDataSetsOneParameterSetOneMethod.class, expected);
    }

    @Test
    public void testTwoDataSetsOneParameterSetTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2 :: [0]", //
          "first :: Data set 2 / 2 :: [0]", //
          "second :: Data set 1 / 2 :: [0]", //
          "second :: Data set 2 / 2 :: [0]"
        };
        checkDescription(TwoDataSetsOneParameterSetTwoMethods.class, expected);
    }

    @Test
    public void testTwoDataSetsTwoParameterSetsOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2 :: [0]", //
          "first :: Data set 1 / 2 :: [1]", //
          "first :: Data set 2 / 2 :: [0]", //
          "first :: Data set 2 / 2 :: [1]"
        };
        checkDescription(TwoDataSetsTwoParameterSetsOneMethod.class, expected);
    }

    @Test
    public void testTwoDataSetsTwoParameterSetsTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Data set 1 / 2 :: [0]", //
          "first :: Data set 1 / 2 :: [1]", //
          "first :: Data set 2 / 2 :: [0]", //
          "first :: Data set 2 / 2 :: [1]", //
          "second :: Data set 1 / 2 :: [0]", //
          "second :: Data set 1 / 2 :: [1]", //
          "second :: Data set 2 / 2 :: [0]", //
          "second :: Data set 2 / 2 :: [1]"
        };
        checkDescription(TwoDataSetsTwoParameterSetsTwoMethods.class, expected);
    }

}
