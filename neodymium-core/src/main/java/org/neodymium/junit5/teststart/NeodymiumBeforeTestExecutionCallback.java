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
package org.neodymium.junit5.teststart;

import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumRandom;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.neodymium.util.NeodymiumRandom.reinitializeRandomSeed;

public class NeodymiumBeforeTestExecutionCallback implements BeforeTestExecutionCallback
{
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception
    {
        // reset the random seed so every test starts with the same values for better reproducibility
        reinitializeRandomSeed(NeodymiumRandom.SeedState.INITIALIZED);
        
        // exact unified test name calculation
        Neodymium.setTestName(context.getRequiredTestClass().getCanonicalName() + " :: " + context.getDisplayName());
    }
}
