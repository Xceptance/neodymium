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
package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.multiplication.TwoMethods;
import com.xceptance.neodymium.junit5.testclasses.multiplication.TwoMethodsAndOneIgnored;
import com.xceptance.neodymium.junit5.testclasses.multiplication.TwoMethodsOneIgnored;
import com.xceptance.neodymium.junit5.testclasses.multiplication.dataset.OneDataSetOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.dataset.OneDataSetTwoMethods;
import com.xceptance.neodymium.junit5.testclasses.multiplication.dataset.TwoDataSetsOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.dataset.TwoDataSetsTwoMethods;
import com.xceptance.neodymium.junit5.testclasses.multiplication.pkgdata.PackageDataDoNotAffectMethodMultiplication;
import org.neodymium.util.Neodymium;

public class MultiplicationTest extends AbstractNeodymiumTest
{

    @BeforeEach
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
          "first", "second()"
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
}
