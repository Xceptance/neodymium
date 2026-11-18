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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.InvalidSelectorException;

/**
 * Strict parser for Playwright selectors and Neodymium locator extensions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightSelectorParser
{
    private static final Set<String> SUPPORTED_PSEUDOS = Set.of(
        // Standard CSS pseudo-classes
        "not", "is", "where", "has", "first-child", "last-child", "nth-child", "nth-last-child",
        "nth-of-type", "nth-last-of-type", "first-of-type", "last-of-type", "only-child", "only-of-type",
        "empty", "root", "enabled", "disabled", "checked", "selected", "required", "read-only", "read-write",
        "focus", "active", "hover",
        // Playwright text pseudo-classes
        "has-text", "text", "text-is", "text-matches", "contains", "exact-text", "has-text-is", "has-text*", "text*",
        // Playwright state & positional pseudo-classes
        "visible", "nth-match",
        // Playwright layout pseudo-classes
        "left-of", "right-of", "above", "below", "near"
    );

    private static final Pattern ENGINE_PREFIX_PATTERN = Pattern.compile("^(\\*?)([a-zA-Z0-9_\\-]+(?:\\:[a-zA-Z0-9_\\-]+)?\\*?)=(.*)$", Pattern.DOTALL);

    private PlaywrightSelectorParser()
    {
    }

    /**
     * Parses a raw selector into a validated {@link SelectorChain}.
     *
     * @param rawSelector raw selector string
     * @return parsed {@link SelectorChain}
     * @throws InvalidSelectorException if the selector is invalid or uses unsupported syntax
     */
    public static SelectorChain parse(final String rawSelector)
    {
        if (rawSelector == null || rawSelector.isBlank())
        {
            throw new InvalidSelectorException("Selector cannot be null or blank");
        }

        final List<String> clauses = PlaywrightSelectorLexer.splitClauses(rawSelector);
        if (clauses.isEmpty())
        {
            throw new InvalidSelectorException("Selector cannot be empty: " + rawSelector);
        }

        final List<SelectorStep> steps = new ArrayList<>(clauses.size());
        for (final String clause : clauses)
        {
            steps.add(parseStep(clause));
        }

        return new SelectorChain(steps);
    }

    private static SelectorStep parseStep(final String rawClause)
    {
        final String trimmed = rawClause.trim();
        boolean captured = false;
        String withoutCapture = trimmed;
        if (trimmed.startsWith("*"))
        {
            captured = true;
            withoutCapture = trimmed.substring(1).trim();
        }

        // Check explicit engine prefix: engine=body
        final Matcher matcher = ENGINE_PREFIX_PATTERN.matcher(withoutCapture);
        if (matcher.matches())
        {
            final String prefixCaptured = matcher.group(1);
            if (!prefixCaptured.isEmpty())
            {
                captured = true;
            }
            final String engineStr = matcher.group(2).trim();
            final String body = matcher.group(3);

            if (engineStr.toLowerCase(Locale.ROOT).startsWith("internal:control"))
            {
                if (body.contains("enter-frame"))
                {
                    throw new InvalidSelectorException("internal:control=enter-frame is not supported; frame switching must be done explicitly");
                }
            }

            final SelectorEngine engine = resolveEngine(engineStr);
            if (engine != null)
            {
                final Object parsed = parseBody(engine, body);
                return new SelectorStep(captured, engine, engineStr, body, parsed);
            }
        }

        // Shorthands
        final String lowerWithoutCapture = withoutCapture.toLowerCase(Locale.ROOT);
        if (lowerWithoutCapture.startsWith("text:")
                || lowerWithoutCapture.startsWith("text*:")
                || lowerWithoutCapture.startsWith("has-text:")
                || lowerWithoutCapture.startsWith("has-text*:"))
        {
            final int colonIdx = withoutCapture.indexOf(':');
            final String prefix = withoutCapture.substring(0, colonIdx);
            final String body = withoutCapture.substring(colonIdx + 1);
            return new SelectorStep(captured, SelectorEngine.TEXT, prefix, body, body);
        }
        if (withoutCapture.startsWith("//") || withoutCapture.startsWith("..") || withoutCapture.startsWith("("))
        {
            return new SelectorStep(captured, SelectorEngine.XPATH, "xpath", withoutCapture, withoutCapture);
        }
        if (withoutCapture.startsWith("\"") || withoutCapture.startsWith("'"))
        {
            return new SelectorStep(captured, SelectorEngine.TEXT, "text", withoutCapture, withoutCapture);
        }
        if (withoutCapture.matches("^xc[a-zA-Z0-9_\\-]+$"))
        {
            return new SelectorStep(captured, SelectorEngine.AUTOMATION_ID, "data-ai", withoutCapture, withoutCapture);
        }

        // Default to CSS
        final Object parsed = parseCss(withoutCapture);
        return new SelectorStep(captured, SelectorEngine.CSS, "css", withoutCapture, parsed);
    }

    private static SelectorEngine resolveEngine(final String engineStr)
    {
        final String lower = engineStr.toLowerCase(Locale.ROOT);
        if (lower.startsWith("internal:"))
        {
            return SelectorEngine.INTERNAL;
        }
        return switch (lower)
        {
            case "css" -> SelectorEngine.CSS;
            case "xpath" -> SelectorEngine.XPATH;
            case "text", "text*", "has-text", "has-text*" -> SelectorEngine.TEXT;
            case "role" -> SelectorEngine.ROLE;
            case "id" -> SelectorEngine.ID;
            case "data-testid", "testid" -> SelectorEngine.DATA_TESTID;
            case "data-test-id" -> SelectorEngine.DATA_TEST_ID;
            case "data-test" -> SelectorEngine.DATA_TEST;
            case "data-qa" -> SelectorEngine.DATA_QA;
            case "data-cy" -> SelectorEngine.DATA_CY;
            case "nth" -> SelectorEngine.NTH;
            case "visible" -> SelectorEngine.VISIBLE;
            case "label" -> SelectorEngine.LABEL;
            case "placeholder" -> SelectorEngine.PLACEHOLDER;
            case "alt" -> SelectorEngine.ALT;
            case "title" -> SelectorEngine.TITLE;
            case "data-ai" -> SelectorEngine.AUTOMATION_ID;
            default -> null;
        };
    }

    private static Object parseBody(final SelectorEngine engine, final String body)
    {
        return switch (engine)
        {
            case CSS -> parseCss(body);
            case NTH -> parseNth(body);
            case VISIBLE -> parseVisible(body);
            default -> body;
        };
    }

    private static Integer parseNth(final String body)
    {
        try
        {
            return Integer.parseInt(body.trim());
        }
        catch (final NumberFormatException e)
        {
            throw new InvalidSelectorException("Invalid nth= parameter: '" + body + "' must be an integer");
        }
    }

    private static Boolean parseVisible(final String body)
    {
        final String clean = body.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(clean))
        {
            return Boolean.TRUE;
        }
        if ("false".equals(clean))
        {
            return Boolean.FALSE;
        }
        throw new InvalidSelectorException("Invalid visible= parameter: '" + body + "' must be true or false");
    }

    private record ParsedCompound(CssAst.CompoundSelector compound, int nextIdx)
    {
    }

    /**
     * Parses a CSS selector string into a structured {@link CssAst.Root}.
     */
    public static CssAst.Root parseCss(final String css)
    {
        final List<PlaywrightSelectorLexer.Token> tokens = PlaywrightSelectorLexer.tokenize(css);
        final List<CssAst.ComplexSelector> complexSelectors = new ArrayList<>();

        int cursor = 0;
        final int tokenCount = tokens.size();

        while (cursor < tokenCount && tokens.get(cursor).type() != PlaywrightSelectorLexer.TokenType.EOF)
        {
            final List<CssAst.CompoundSelector> compounds = new ArrayList<>();
            final List<CssAst.Combinator> combinators = new ArrayList<>();

            // Parse first compound selector
            final ParsedCompound first = parseCompound(tokens, cursor);
            compounds.add(first.compound());
            cursor = first.nextIdx();

            // While next token is combinator or another compound
            while (cursor < tokenCount && tokens.get(cursor).type() != PlaywrightSelectorLexer.TokenType.COMMA
                   && tokens.get(cursor).type() != PlaywrightSelectorLexer.TokenType.EOF)
            {
                CssAst.Combinator comb = CssAst.Combinator.DESCENDANT;
                if (tokens.get(cursor).type() == PlaywrightSelectorLexer.TokenType.COMBINATOR)
                {
                    final String sym = tokens.get(cursor).value().trim();
                    comb = switch (sym)
                    {
                        case ">" -> CssAst.Combinator.CHILD;
                        case "+" -> CssAst.Combinator.NEXT_SIBLING;
                        case "~" -> CssAst.Combinator.SUBSEQUENT_SIBLING;
                        default -> CssAst.Combinator.DESCENDANT;
                    };
                    cursor++;
                }
                combinators.add(comb);
                final ParsedCompound next = parseCompound(tokens, cursor);
                compounds.add(next.compound());
                cursor = next.nextIdx();
            }

            complexSelectors.add(new CssAst.ComplexSelector(compounds, combinators));

            if (cursor < tokenCount && tokens.get(cursor).type() == PlaywrightSelectorLexer.TokenType.COMMA)
            {
                cursor++; // skip comma
            }
        }

        return new CssAst.Root(complexSelectors);
    }

    private static ParsedCompound parseCompound(final List<PlaywrightSelectorLexer.Token> tokens, final int startIdx)
    {
        int idx = startIdx;
        String tagName = null;
        final List<CssAst.SimpleSelector> simpleSelectors = new ArrayList<>();

        if (idx < tokens.size() && tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.TAG)
        {
            tagName = tokens.get(idx).value();
            idx++;
        }

        while (idx < tokens.size())
        {
            final PlaywrightSelectorLexer.Token t = tokens.get(idx);
            if (t.type() == PlaywrightSelectorLexer.TokenType.CLASS)
            {
                simpleSelectors.add(new CssAst.ClassSelector(t.value()));
                idx++;
            }
            else if (t.type() == PlaywrightSelectorLexer.TokenType.HASH)
            {
                final boolean isAutoId = t.value().startsWith("xc");
                simpleSelectors.add(new CssAst.IdSelector(t.value(), isAutoId));
                idx++;
            }
            else if (t.type() == PlaywrightSelectorLexer.TokenType.LBRACKET)
            {
                // Parse attribute: [name op value i/s]
                idx++;
                if (idx >= tokens.size() || tokens.get(idx).type() != PlaywrightSelectorLexer.TokenType.TAG)
                {
                    throw new InvalidSelectorException("Expected attribute name inside brackets at pos " + t.startPos());
                }
                final String attrName = tokens.get(idx).value();
                idx++;
                String op = null;
                String val = null;
                boolean caseInsensitive = false;

                if (idx < tokens.size() && tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.ATTR_OP)
                {
                    op = tokens.get(idx).value();
                    idx++;
                    if (idx < tokens.size() && (tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.STRING
                                                || tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.TAG))
                    {
                        val = tokens.get(idx).value();
                        idx++;
                    }
                    else
                    {
                        throw new InvalidSelectorException("Expected attribute value after operator '" + op + "'");
                    }
                    // Optional case flag 'i' or 's'
                    if (idx < tokens.size() && tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.TAG)
                    {
                        final String flag = tokens.get(idx).value();
                        if ("i".equalsIgnoreCase(flag))
                        {
                            caseInsensitive = true;
                            idx++;
                        }
                        else if ("s".equalsIgnoreCase(flag))
                        {
                            caseInsensitive = false;
                            idx++;
                        }
                    }
                }

                if (idx >= tokens.size() || tokens.get(idx).type() != PlaywrightSelectorLexer.TokenType.RBRACKET)
                {
                    throw new InvalidSelectorException("Unclosed attribute bracket");
                }
                idx++; // skip RBRACKET
                simpleSelectors.add(new CssAst.AttributeSelector(attrName, op, val, caseInsensitive));
            }
            else if (t.type() == PlaywrightSelectorLexer.TokenType.PSEUDO)
            {
                final String pseudoName = t.value().toLowerCase(Locale.ROOT);
                if (!SUPPORTED_PSEUDOS.contains(pseudoName))
                {
                    throw new InvalidSelectorException("Unknown pseudo-class ':" + t.value() + "' at position " + t.startPos());
                }
                idx++;
                String arg = null;
                if (idx < tokens.size() && tokens.get(idx).type() == PlaywrightSelectorLexer.TokenType.LPAREN)
                {
                    idx++;
                    final StringBuilder sb = new StringBuilder();
                    int parenDepth = 1;
                    while (idx < tokens.size())
                    {
                        final PlaywrightSelectorLexer.Token argToken = tokens.get(idx);
                        if (argToken.type() == PlaywrightSelectorLexer.TokenType.LPAREN)
                        {
                            parenDepth++;
                            sb.append("(");
                        }
                        else if (argToken.type() == PlaywrightSelectorLexer.TokenType.RPAREN)
                        {
                            parenDepth--;
                            if (parenDepth == 0)
                            {
                                idx++;
                                break;
                            }
                            sb.append(")");
                        }
                        else
                        {
                            sb.append(argToken.value());
                        }
                        idx++;
                    }
                    arg = sb.toString();
                }
                simpleSelectors.add(new CssAst.PseudoClassSelector(pseudoName, arg, null));
            }
            else
            {
                break;
            }
        }

        if (tagName == null && simpleSelectors.isEmpty())
        {
            throw new InvalidSelectorException("Empty compound selector near position " + (startIdx < tokens.size() ? tokens.get(startIdx).startPos() : "end"));
        }

        return new ParsedCompound(new CssAst.CompoundSelector(tagName, simpleSelectors), idx);
    }
}
