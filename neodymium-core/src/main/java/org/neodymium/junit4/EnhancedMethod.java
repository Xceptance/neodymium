/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.junit4;

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
