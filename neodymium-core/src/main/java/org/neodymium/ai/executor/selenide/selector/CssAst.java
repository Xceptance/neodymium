/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.executor.selenide.selector;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract syntax tree (AST) nodes for CSS selectors, compound selectors, attributes, and pseudo-classes.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class CssAst
{
    private CssAst()
    {
    }

    public enum Combinator
    {
        DESCENDANT(" "),
        CHILD(" > "),
        NEXT_SIBLING(" + "),
        SUBSEQUENT_SIBLING(" ~ ");

        private final String symbol;

        Combinator(final String symbol)
        {
            this.symbol = symbol;
        }

        public String getSymbol()
        {
            return this.symbol;
        }
    }

    public sealed interface SimpleSelector permits IdSelector, ClassSelector, AttributeSelector, PseudoClassSelector
    {
    }

    public static final class IdSelector implements SimpleSelector
    {
        private final String id;
        private final boolean isAutomationId;

        public IdSelector(final String id, final boolean isAutomationId)
        {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            this.isAutomationId = isAutomationId;
        }

        public String getId()
        {
            return this.id;
        }

        public boolean isAutomationId()
        {
            return this.isAutomationId;
        }

        @Override
        public String toString()
        {
            return "#" + this.id;
        }
    }

    public static final class ClassSelector implements SimpleSelector
    {
        private final String className;

        public ClassSelector(final String className)
        {
            this.className = Objects.requireNonNull(className, "className cannot be null");
        }

        public String getClassName()
        {
            return this.className;
        }

        @Override
        public String toString()
        {
            return "." + this.className;
        }
    }

    public static final class AttributeSelector implements SimpleSelector
    {
        private final String name;
        private final String operator;
        private final String value;
        private final boolean caseInsensitive;

        public AttributeSelector(final String name, final String operator, final String value, final boolean caseInsensitive)
        {
            this.name = Objects.requireNonNull(name, "attribute name cannot be null");
            this.operator = operator;
            this.value = value;
            this.caseInsensitive = caseInsensitive;
        }

        public String getName()
        {
            return this.name;
        }

        public String getOperator()
        {
            return this.operator;
        }

        public String getValue()
        {
            return this.value;
        }

        public boolean isCaseInsensitive()
        {
            return this.caseInsensitive;
        }

        @Override
        public String toString()
        {
            if (this.operator == null)
            {
                return "[" + this.name + "]";
            }
            return "[" + this.name + this.operator + "\"" + this.value + "\"" + (this.caseInsensitive ? " i" : "") + "]";
        }
    }

    public static final class PseudoClassSelector implements SimpleSelector
    {
        private final String name;
        private final String argument;
        private final Object parsedArgument;

        public PseudoClassSelector(final String name, final String argument, final Object parsedArgument)
        {
            this.name = Objects.requireNonNull(name, "pseudo-class name cannot be null");
            this.argument = argument;
            this.parsedArgument = parsedArgument;
        }

        public String getName()
        {
            return this.name;
        }

        public String getArgument()
        {
            return this.argument;
        }

        public Object getParsedArgument()
        {
            return this.parsedArgument;
        }

        @Override
        public String toString()
        {
            if (this.argument == null)
            {
                return ":" + this.name;
            }
            return ":" + this.name + "(" + this.argument + ")";
        }
    }

    public static final class CompoundSelector
    {
        private final String tagName;
        private final List<SimpleSelector> simpleSelectors;

        public CompoundSelector(final String tagName, final List<SimpleSelector> simpleSelectors)
        {
            this.tagName = tagName != null && !tagName.isBlank() ? tagName : null;
            this.simpleSelectors = simpleSelectors != null ? Collections.unmodifiableList(simpleSelectors) : List.of();
        }

        public String getTagName()
        {
            return this.tagName;
        }

        public List<SimpleSelector> getSimpleSelectors()
        {
            return this.simpleSelectors;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            if (this.tagName != null)
            {
                sb.append(this.tagName);
            }
            for (final SimpleSelector s : this.simpleSelectors)
            {
                sb.append(s);
            }
            return sb.toString();
        }
    }

    public static final class ComplexSelector
    {
        private final List<CompoundSelector> compounds;
        private final List<Combinator> combinators;

        public ComplexSelector(final List<CompoundSelector> compounds, final List<Combinator> combinators)
        {
            this.compounds = Collections.unmodifiableList(Objects.requireNonNull(compounds, "compounds cannot be null"));
            this.combinators = Collections.unmodifiableList(Objects.requireNonNull(combinators, "combinators cannot be null"));
            if (compounds.isEmpty())
            {
                throw new IllegalArgumentException("ComplexSelector must have at least one compound selector");
            }
            if (combinators.size() != compounds.size() - 1)
            {
                throw new IllegalArgumentException("Combinator count must equal compound count - 1");
            }
        }

        public List<CompoundSelector> getCompounds()
        {
            return this.compounds;
        }

        public List<Combinator> getCombinators()
        {
            return this.combinators;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.compounds.size(); i++)
            {
                if (i > 0)
                {
                    sb.append(this.combinators.get(i - 1).getSymbol());
                }
                sb.append(this.compounds.get(i));
            }
            return sb.toString();
        }
    }

    public static final class Root
    {
        private final List<ComplexSelector> selectors;

        public Root(final List<ComplexSelector> selectors)
        {
            this.selectors = Collections.unmodifiableList(Objects.requireNonNull(selectors, "selectors cannot be null"));
        }

        public List<ComplexSelector> getSelectors()
        {
            return this.selectors;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.selectors.size(); i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                sb.append(this.selectors.get(i));
            }
            return sb.toString();
        }
    }
}
