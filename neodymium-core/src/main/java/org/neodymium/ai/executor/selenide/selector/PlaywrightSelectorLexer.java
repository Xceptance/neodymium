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
import java.util.Objects;

import org.openqa.selenium.InvalidSelectorException;

/**
 * Quote-, bracket-, paren-, and escape-aware lexer for Playwright selectors.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightSelectorLexer
{
    private PlaywrightSelectorLexer()
    {
    }

    public enum TokenType
    {
        TAG,
        HASH,
        CLASS,
        LBRACKET,
        RBRACKET,
        LPAREN,
        RPAREN,
        COMMA,
        COMBINATOR,
        ATTR_OP,
        STRING,
        PSEUDO,
        REGEX,
        EOF
    }

    public record Token(TokenType type, String value, int startPos, int endPos)
    {
        @Override
        public String toString()
        {
            return this.type + "(" + this.value + "@" + this.startPos + ")";
        }
    }

    /**
     * Splits a selector string into clauses separated by {@code >>}, correctly ignoring arrows inside
     * quotes, brackets, parentheses, or regular expressions.
     *
     * @param selector raw selector string
     * @return list of trimmed clause strings
     */
    public static List<String> splitClauses(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return List.of();
        }

        final List<String> clauses = new ArrayList<>();
        final int len = selector.length();
        int clauseStart = 0;
        char inQuote = 0;
        int bracketDepth = 0;
        int parenDepth = 0;
        boolean inRegex = false;

        for (int i = 0; i < len; i++)
        {
            final char c = selector.charAt(i);

            // Handle escapes
            if (c == '\\' && i + 1 < len)
            {
                i++; // Skip escaped character
                continue;
            }

            // Handle quotes
            if (inQuote != 0)
            {
                if (c == inQuote)
                {
                    inQuote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"')
            {
                inQuote = c;
                continue;
            }

            // Handle regex (starts with / if preceded by whitespace, start, '=', or '(')
            if (inRegex)
            {
                if (c == '/')
                {
                    // Skip optional regex flags (i, s, m, u)
                    while (i + 1 < len && Character.isLetter(selector.charAt(i + 1)))
                    {
                        i++;
                    }
                    inRegex = false;
                }
                continue;
            }
            if (c == '/' && bracketDepth == 0 && parenDepth == 0)
            {
                final int prevIdx = prevNonWhitespace(selector, i - 1);
                if (prevIdx < 0 || selector.charAt(prevIdx) == '=' || selector.charAt(prevIdx) == '(' || selector.charAt(prevIdx) == ',')
                {
                    inRegex = true;
                    continue;
                }
            }

            // Track bracket depth
            if (c == '[')
            {
                bracketDepth++;
                continue;
            }
            if (c == ']')
            {
                if (bracketDepth > 0)
                {
                    bracketDepth--;
                }
                continue;
            }

            // Track paren depth
            if (c == '(')
            {
                parenDepth++;
                continue;
            }
            if (c == ')')
            {
                if (parenDepth > 0)
                {
                    parenDepth--;
                }
                continue;
            }

            // Split on >> only at depth 0
            if (bracketDepth == 0 && parenDepth == 0 && c == '>' && i + 1 < len && selector.charAt(i + 1) == '>')
            {
                final String part = selector.substring(clauseStart, i).trim();
                if (!part.isEmpty())
                {
                    clauses.add(part);
                }
                i++; // Skip second '>'
                clauseStart = i + 1;
            }
        }

        final String lastPart = selector.substring(clauseStart).trim();
        if (!lastPart.isEmpty())
        {
            clauses.add(lastPart);
        }

        return clauses;
    }

    private static int prevNonWhitespace(final String s, int from)
    {
        while (from >= 0 && Character.isWhitespace(s.charAt(from)))
        {
            from--;
        }
        return from;
    }

    /**
     * Tokenizes a CSS selector string into a list of structured tokens.
     *
     * @param css CSS selector string
     * @return list of tokens ending with {@link TokenType#EOF}
     */
    public static List<Token> tokenize(final String css)
    {
        Objects.requireNonNull(css, "css cannot be null");
        final List<Token> tokens = new ArrayList<>();
        final int len = css.length();
        int i = 0;

        while (i < len)
        {
            final char c = css.charAt(i);

            // Skip whitespace but note if it acts as a descendant combinator
            if (Character.isWhitespace(c))
            {
                final int wsStart = i;
                while (i < len && Character.isWhitespace(css.charAt(i)))
                {
                    i++;
                }
                if (i < len)
                {
                    final char next = css.charAt(i);
                    // Whitespace acts as descendant combinator if between simple selectors and not next to >, +, ~, ,
                    if (next != '>' && next != '+' && next != '~' && next != ',' && !tokens.isEmpty())
                    {
                        final TokenType prevType = tokens.get(tokens.size() - 1).type();
                        if (prevType != TokenType.COMBINATOR && prevType != TokenType.COMMA)
                        {
                            tokens.add(new Token(TokenType.COMBINATOR, " ", wsStart, i));
                        }
                    }
                }
                continue;
            }

            if (c == ',')
            {
                tokens.add(new Token(TokenType.COMMA, ",", i, i + 1));
                i++;
                continue;
            }

            // Check attribute operators first (=, ~=, |=, ^=, $=, *=) before combinators (+, ~, >)
            if (c == '=' || (i + 1 < len && css.charAt(i + 1) == '=' && (c == '~' || c == '|' || c == '^' || c == '$' || c == '*')))
            {
                final int opLen = (c == '=') ? 1 : 2;
                tokens.add(new Token(TokenType.ATTR_OP, css.substring(i, i + opLen), i, i + opLen));
                i += opLen;
                continue;
            }

            // Combinators: >, +, ~
            if (c == '>' || c == '+' || c == '~')
            {
                tokens.add(new Token(TokenType.COMBINATOR, String.valueOf(c), i, i + 1));
                i++;
                continue;
            }

            if (c == '[')
            {
                tokens.add(new Token(TokenType.LBRACKET, "[", i, i + 1));
                i++;
                continue;
            }

            if (c == ']')
            {
                tokens.add(new Token(TokenType.RBRACKET, "]", i, i + 1));
                i++;
                continue;
            }

            if (c == '(')
            {
                tokens.add(new Token(TokenType.LPAREN, "(", i, i + 1));
                i++;
                continue;
            }

            if (c == ')')
            {
                tokens.add(new Token(TokenType.RPAREN, ")", i, i + 1));
                i++;
                continue;
            }

            // Pseudo-class: :pseudo or ::pseudo
            if (c == ':')
            {
                final int start = i;
                i++;
                if (i < len && css.charAt(i) == ':')
                {
                    i++; // double colon
                }
                final int nameStart = i;
                while (i < len && isIdentChar(css.charAt(i)))
                {
                    i++;
                }
                final String pseudoName = css.substring(nameStart, i);
                tokens.add(new Token(TokenType.PSEUDO, pseudoName, start, i));
                continue;
            }

            // Class: .class
            if (c == '.')
            {
                final int start = i;
                i++;
                final int nameStart = i;
                while (i < len && isIdentChar(css.charAt(i)))
                {
                    i++;
                }
                final String className = css.substring(nameStart, i);
                tokens.add(new Token(TokenType.CLASS, className, start, i));
                continue;
            }

            // Hash/ID: #id
            if (c == '#')
            {
                final int start = i;
                i++;
                final int nameStart = i;
                while (i < len && isIdentChar(css.charAt(i)))
                {
                    i++;
                }
                final String idName = css.substring(nameStart, i);
                tokens.add(new Token(TokenType.HASH, idName, start, i));
                continue;
            }

            // Quoted string
            if (c == '"' || c == '\'')
            {
                final int start = i;
                final char quote = c;
                i++;
                final StringBuilder val = new StringBuilder();
                boolean closed = false;
                while (i < len)
                {
                    final char sc = css.charAt(i);
                    if (sc == '\\' && i + 1 < len)
                    {
                        i++;
                        val.append(css.charAt(i));
                    }
                    else if (sc == quote)
                    {
                        i++;
                        closed = true;
                        break;
                    }
                    else
                    {
                        val.append(sc);
                    }
                    i++;
                }
                if (!closed)
                {
                    throw new InvalidSelectorException("Unterminated string starting at position " + start);
                }
                tokens.add(new Token(TokenType.STRING, val.toString(), start, i));
                continue;
            }

            // Regex: /pattern/flags
            if (c == '/')
            {
                final int start = i;
                i++;
                final StringBuilder pattern = new StringBuilder();
                boolean closed = false;
                while (i < len)
                {
                    final char sc = css.charAt(i);
                    if (sc == '\\' && i + 1 < len)
                    {
                        pattern.append(sc);
                        i++;
                        pattern.append(css.charAt(i));
                    }
                    else if (sc == '/')
                    {
                        i++;
                        closed = true;
                        break;
                    }
                    else
                    {
                        pattern.append(sc);
                    }
                    i++;
                }
                if (!closed)
                {
                    throw new InvalidSelectorException("Unterminated regex starting at position " + start);
                }
                final StringBuilder flags = new StringBuilder();
                while (i < len && Character.isLetter(css.charAt(i)))
                {
                    flags.append(css.charAt(i));
                    i++;
                }
                tokens.add(new Token(TokenType.REGEX, "/" + pattern + "/" + flags, start, i));
                continue;
            }

            // Numeric literals (e.g. [level=2], nth=0)
            if (Character.isDigit(c))
            {
                final int start = i;
                while (i < len && (Character.isDigit(css.charAt(i)) || css.charAt(i) == '.'))
                {
                    i++;
                }
                tokens.add(new Token(TokenType.STRING, css.substring(start, i), start, i));
                continue;
            }

            // Universal selector: '*'
            if (c == '*' && (i + 1 >= len || css.charAt(i + 1) != '='))
            {
                tokens.add(new Token(TokenType.TAG, "*", i, i + 1));
                i++;
                continue;
            }

            // Identifier or tag name
            if (isIdentStart(c))
            {
                final int start = i;
                i++;
                while (i < len && isIdentChar(css.charAt(i)))
                {
                    i++;
                }
                final String ident = css.substring(start, i);
                tokens.add(new Token(TokenType.TAG, ident, start, i));
                continue;
            }

            throw new InvalidSelectorException("Unexpected character '" + c + "' at position " + i + " in selector: " + css);
        }

        tokens.add(new Token(TokenType.EOF, "", len, len));
        return tokens;
    }

    private static boolean isIdentStart(final char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-';
    }

    private static boolean isIdentChar(final char c)
    {
        return isIdentStart(c) || (c >= '0' && c <= '9');
    }
}
