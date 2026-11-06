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
package com.xceptance.aura.mcp.service;

import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Distills raw Selenium and framework stack traces to retain only high-value diagnostic failure context.
 * Filters out internal framework frames and strictly caps payload size to 1,500 characters.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class SeleniumErrorDistiller
{
    private static final int MAX_FAILURE_CHAR_BUDGET = 1500;

    private static final Pattern LOCATOR_PATTERN = Pattern.compile("(?i)(?:\"selector\"\\s*:\\s*\"([^\"]+)\"|By\\.[a-zA-Z0-9_]+:\\s*([^\\n\\r]+?)(?:\\s*\\(tried|\\s*\\(Session|$)|locator:\\s*([^\\n\\r]+)|element:\\s*<([^>]+)>)");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(https?://[^\\s\"',]+)");
    private static final Pattern DOM_SNIPPET_PATTERN = Pattern.compile(
        "(?:"
        // Explicit Selenium element prefix (captures all tags including custom Web Components):
        + "element:?\\s*<[^>]+>"
        + "|"
        // Multi-character HTML tags that cannot collide with standard single or two-letter generics:
        + "<(?:html|body|button|div|span|input|form|select|textarea|table|thead|tbody|tfoot|label|svg|section|header|nav|main|footer|dialog|img|h[1-6]|iframe|option|optgroup)\\b[^>]*>"
        + "|"
        // Short/single/two-letter tags in lowercase with any attribute, self-closing, or valueless (<a download>, <p hidden>, <i>, <i class="fa">).
        // Prevents matching uppercase Java type parameters/generics in stack traces (<A>, <P>, <TD>, <TR>, <LI>, <A extends Number>).
        + "(?-i:<(?:[apib]|t[rdh]|li|[uo]l)\\b[^>]*>)"
        + "|"
        // Legacy uppercase short tags requiring explicit attribute key-value syntax (<A HREF="...">, <A/>):
        + "<(?:[apib]|t[rdh]|li|[uo]l)(?:\\s+[^>]*=[^>]*>|/>)"
        + ")",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Distills a raw exception message, stack trace, and execution log into a compact DistilledFailureDto.
     *
     * @param testName test or playbook identifier
     * @param rawError raw exception message / stack trace
     * @param failureLogs recent execution log lines
     * @return pruned, token-bounded distilled failure representation
     */
    public DistilledFailureDto distill(final String testName, final String rawError, final List<String> failureLogs)
    {
        final DistilledFailureDto dto = new DistilledFailureDto();
        dto.setTestName(testName);

        final String safeRaw = (rawError != null) ? rawError : "";

        // 1. Extract locator if present
        final Matcher locMatcher = LOCATOR_PATTERN.matcher(safeRaw);
        if (locMatcher.find())
        {
            for (int i = 1; i <= locMatcher.groupCount(); i++)
            {
                if (locMatcher.group(i) != null)
                {
                    dto.setLocator(locMatcher.group(i).trim());
                    break;
                }
            }
        }

        // 2. Extract URL if present
        final Matcher urlMatcher = URL_PATTERN.matcher(safeRaw);
        if (urlMatcher.find())
        {
            dto.setCurrentUrl(urlMatcher.group(1).trim());
        }

        // 3. Extract failing DOM snippet if present
        final Matcher domMatcher = DOM_SNIPPET_PATTERN.matcher(safeRaw);
        if (domMatcher.find())
        {
            final String match = domMatcher.group().trim();
            final int startIdx = match.indexOf('<');
            if (startIdx >= 0)
            {
                final String tagContent = match.substring(startIdx);
                dto.setDomSnippet(tagContent.substring(0, Math.min(tagContent.length(), 200)));
            }
        }

        // 4. Clean stack trace by stripping framework noise
        final String cleanedTrace = pruneStackTrace(safeRaw);
        dto.setCleanedStackTrace(capString(cleanedTrace, MAX_FAILURE_CHAR_BUDGET));

        // 5. Categorize cluster category (incorporating locator for fine-grained grouping)
        final String baseCategory = categorizeError(safeRaw);
        if (dto.getLocator() != null && !dto.getLocator().isBlank())
        {
            dto.setClusterCategory(baseCategory + ": " + dto.getLocator());
        }
        else
        {
            dto.setClusterCategory(baseCategory);
        }

        return dto;
    }

    /**
     * Categorizes an error message into standard clustered root causes.
     *
     * @param errorMessage error string
     * @return failure category enum string
     */
    public String categorizeError(final String errorMessage)
    {
        if (errorMessage == null || errorMessage.isBlank())
        {
            return "UNKNOWN_FAILURE";
        }

        final String lower = errorMessage.toLowerCase();
        if (lower.contains("nosuchelement") || lower.contains("cannot locate element") || lower.contains("element not found"))
        {
            return "ELEMENT_NOT_FOUND";
        }
        if (lower.contains("timeoutexception") || lower.contains("timed out") || lower.contains("wait timed out"))
        {
            return "TIMEOUT";
        }
        if (lower.contains("staleelementreference") || lower.contains("stale element"))
        {
            return "STALE_ELEMENT_REFERENCE";
        }
        if (lower.contains("elementnotinteractable") || lower.contains("element click intercepted"))
        {
            return "ELEMENT_NOT_INTERACTABLE";
        }
        if (lower.contains("connection refused") || lower.contains("connection timed out") || lower.contains("sqlexception"))
        {
            return "BACKEND_CONNECTION_FAILURE";
        }
        if (lower.contains("assertionerror") || lower.contains("expected:") || lower.contains("assertion failed"))
        {
            return "ASSERTION_ERROR";
        }

        return "UNCLASSIFIED_ERROR";
    }

    private String pruneStackTrace(final String raw)
    {
        final String[] lines = raw.split("\\r?\\n");
        final List<String> filtered = new ArrayList<>();

        for (final String line : lines)
        {
            final String trimmed = line.trim();
            if (trimmed.startsWith("at org.openqa.selenium.")
                || trimmed.startsWith("at org.springframework.")
                || trimmed.startsWith("at jakarta.servlet.")
                || trimmed.startsWith("at jdk.internal.")
                || trimmed.startsWith("at java.base/jdk.internal.")
                || trimmed.startsWith("at java.base/java.lang.reflect.")
                || trimmed.startsWith("at org.apache.maven.")
                || trimmed.startsWith("at org.codehaus.plexus."))
            {
                continue;
            }
            filtered.add(line);
        }

        return String.join("\n", filtered);
    }

    private String capString(final String text, final int maxChars)
    {
        if (text == null || text.length() <= maxChars)
        {
            return text;
        }
        final String suffix = "... [TRUNCATED]";
        if (maxChars <= suffix.length())
        {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - suffix.length()) + suffix;
    }
}
