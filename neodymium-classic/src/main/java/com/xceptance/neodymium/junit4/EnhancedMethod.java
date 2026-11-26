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
package com.xceptance.neodymium.junit4;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;

public class EnhancedMethod extends FrameworkMethod
{
    private List<Object> data = new LinkedList<>();

    private List<StatementBuilder<?>> builder = new LinkedList<>();

    public EnhancedMethod(Method method)
    {
        super(method);
    }

    public List<Object> getData()
    {
        return data;
    }

    public void setData(List<Object> data)
    {
        this.data = data;
    }

    public List<StatementBuilder<?>> getBuilder()
    {
        return builder;
    }

    public void setBuilder(List<StatementBuilder<?>> builder)
    {
        this.builder = builder;
    }

    @Override
    public String getName()
    {
        StringBuilder nameBuilder = new StringBuilder(250);
        nameBuilder.append(super.getName());
        for (int i = builder.size() - 1; i >= 0; i--)
        {
            StatementBuilder<?> statementBuilder = builder.get(i);
            nameBuilder.append(" :: ");
            nameBuilder.append(statementBuilder.getTestName(data.get(i)));
        }

        return nameBuilder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((builder == null) ? 0 : builder.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!super.equals(obj))
        {
            return false;
        }
        if (!(obj instanceof EnhancedMethod))
        {
            return false;
        }
        EnhancedMethod other = (EnhancedMethod) obj;
        if (builder == null)
        {
            if (other.builder != null)
            {
                return false;
            }
        }
        else if (!builder.equals(other.builder))
        {
            return false;
        }
        if (data == null)
        {
            if (other.data != null)
            {
                return false;
            }
        }
        else if (!data.equals(other.data))
        {
            return false;
        }
        return true;
    }
}
