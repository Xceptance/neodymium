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
package com.xceptance.neodymium.junit5.tests.utils;

import java.util.Optional;
import java.util.Set;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

public class MockedTestDescriptor implements TestDescriptor
{
    private String uniqueId;

    private String displayName;

    public MockedTestDescriptor(String uniqueId, String displayName)
    {
        this.uniqueId = uniqueId;
        this.displayName = displayName;
    }

    @Override
    public UniqueId getUniqueId()
    {
        return UniqueId.parse(uniqueId);
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public Set<TestTag> getTags()
    {
        // TODO Auto-generated method stub
        return Set.of();
    }

    @Override
    public Optional<TestSource> getSource()
    {
        // TODO Auto-generated method stub
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<TestDescriptor> getParent()
    {
        // TODO Auto-generated method stub
        return Optional.ofNullable(null);
    }

    @Override
    public void setParent(TestDescriptor parent)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<? extends TestDescriptor> getChildren()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addChild(TestDescriptor descriptor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeChild(TestDescriptor descriptor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFromHierarchy()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Type getType()
    {
        // TODO Auto-generated method stub
        return Type.TEST;
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
