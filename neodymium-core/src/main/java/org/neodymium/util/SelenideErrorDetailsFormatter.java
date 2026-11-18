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
package org.neodymium.util;

import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ex.SelenideErrorFormatter;
import com.codeborne.selenide.impl.Screenshot;

public class SelenideErrorDetailsFormatter extends SelenideErrorFormatter
{
    @Override
    public String generateErrorDetails(AssertionError error, Driver driver, Screenshot screenshot, long timeoutMs)
    {

        if (Neodymium.configuration().showSelenideErrorDetails())
        {
            return super.generateErrorDetails(error, driver, screenshot, timeoutMs);
        }

        // The screenshot path is always unique. Also the caused by message is dependent on the browser, so just ignore
        // those.
        // Timeout might be interesting, but to keep the message clean we add it as a step parameter to the current step
        StringBuilder sb = new StringBuilder();
        return sb.append("(")
                 .append(timeout(timeoutMs))
                 .append(")")
                 .toString();
    }
}
