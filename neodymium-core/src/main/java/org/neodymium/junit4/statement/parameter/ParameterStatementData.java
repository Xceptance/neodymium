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
package org.neodymium.junit4.statement.parameter;

import java.util.List;

import org.junit.runners.model.FrameworkField;

public class ParameterStatementData
{
    private int parameterIndex;

    private Object[] parameter;

    private List<FrameworkField> parameterFrameworkFields;

    public ParameterStatementData(int parameterIndex, Object[] parameter, List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterIndex = parameterIndex;
        this.parameter = parameter;
        this.parameterFrameworkFields = parameterFrameworkFields;
    }

    public int getParameterIndex()
    {
        return parameterIndex;
    }

    public Object[] getParameter()
    {
        return parameter;
    }

    public List<FrameworkField> getParameterFrameworkFields()
    {
        return parameterFrameworkFields;
    }

    public void setParameterFrameworkFields(List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterFrameworkFields = parameterFrameworkFields;
    }
}
