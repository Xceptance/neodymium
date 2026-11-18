/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.tool.guard;

import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.InternetDomainName;
import java.io.ByteArrayOutputStream;
import java.net.IDN;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.openqa.selenium.WebDriver;

/**
 * Deterministic pre-execution guard that validates autonomous {@code navigate} tool calls
 * before {@code Selenide.open} executes. Prevents the AI agent from escaping to uncommanded
 * external domains or bypassing broken on-page UI interactions via guessed URLs.
 *
 * <p>Permits {@code navigate} calls when any of the following provenance rules holds:
 * <ul>
 *   <li><b>Rule A (Blank Browser / Tab):</b> WebDriver has not started or the active tab is on a blank page
 *       ({@code about:blank} or {@code data:,}).</li>
 *   <li><b>Rule B (Host Provenance):</b> Normalized TLD-preserving host or registrable domain appears in the
 *       step instruction (without downgrading an explicit subdomain) or in {@code neodymium.ai.navigation.allowedHosts}.</li>
 *   <li><b>Rule C (Relative Path / Query / Anchor Provenance):</b> Target is relative or matches the active browser host
 *       and its relative target ({@code /cart}, {@code /?a=b}, {@code ?a=b}, {@code /#section}, or quoted {@code "/"})
 *       appears in the step instruction with valid token boundaries.</li>
 * </ul>
 *
 * @author AI-generated: Claude Opus 4.6
 * @author Xceptance GmbH 2026
 */
public final class NavigationProvenanceInterceptor implements ToolInterceptor
{
    /**
     * Step-scoped {@link ToolContext} key tracking unauthorized navigation attempts within the active step.
     */
    public static final String KEY_BLOCKED_NAV_ATTEMPTS = "neodymium.navigation.blockedAttempts";

    private static final Pattern QUERY_KEY_VALUE_PATTERN = Pattern.compile("^\\?[\\p{L}\\p{N}_%-]+=.*");

    private static final Pattern NON_ROOT_PATH_PATTERN = Pattern.compile("^/[\\p{L}\\p{N}_.-].*");

    private static final String PROVENANCE_RECOMMENDATION =
            "If the step instruction explicitly specifies a URL, domain, or path to open, call 'navigate' with that exact target "
                    + "unmodified (preserving any subdomain, TLD, path, and query). Otherwise, fulfill the step using on-page UI "
                    + "elements (such as links, buttons, or menus), or invoke 'fail_step' if the required element or page state is "
                    + "missing or blocked.";

    private static final String MALFORMED_URL_RECOMMENDATION =
            "Provide a valid URL matching the target specified in the step instruction, or use on-page UI elements if no URL "
                    + "navigation was requested.";

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private final AiConfiguration explicitConfig;

    private final Supplier<String> currentUrlSupplier;

    /**
     * Constructs a default {@link NavigationProvenanceInterceptor} backed by dynamic {@link AiConfiguration#getInstance()}
     * resolution and the active Selenide {@link WebDriverRunner}.
     */
    public NavigationProvenanceInterceptor()
    {
        this(null, NavigationProvenanceInterceptor::readLiveBrowserUrl);
    }

    /**
     * Constructs a {@link NavigationProvenanceInterceptor} with explicit configuration and live browser URL lookup.
     *
     * @param config AI configuration instance
     */
    public NavigationProvenanceInterceptor(final AiConfiguration config)
    {
        this(config, NavigationProvenanceInterceptor::readLiveBrowserUrl);
    }

    /**
     * Package-private constructor for deterministic unit testing without launching a browser.
     *
     * @param config AI configuration instance (or {@code null} to dynamically resolve {@link AiConfiguration#getInstance()})
     * @param currentUrlSupplier supplier returning the active browser tab URL (or {@code null} if not started)
     */
    NavigationProvenanceInterceptor(final AiConfiguration config, final Supplier<String> currentUrlSupplier)
    {
        this.explicitConfig = config;
        this.currentUrlSupplier = currentUrlSupplier != null ? currentUrlSupplier : NavigationProvenanceInterceptor::readLiveBrowserUrl;
    }

    private AiConfiguration config()
    {
        return this.explicitConfig != null ? this.explicitConfig : AiConfiguration.getInstance();
    }

    @Override
    public InterceptionVerdict intercept(final ToolCall call, final ToolContext context)
    {
        if (call == null || call.toolName() == null)
        {
            return InterceptionVerdict.allow("Null tool call");
        }

        final String normalizedTool = normalizeToolName(call.toolName());
        if (!"navigate".equals(normalizedTool) && !"open".equals(normalizedTool) && !"goto".equals(normalizedTool))
        {
            return InterceptionVerdict.allow("Non-navigation tool is exempt from URL provenance check");
        }

        final ExecutionContext execContext = resolveExecutionContext(context);
        final SessionData sessionData = execContext != null ? execContext.getSessionData() : null;

        final String rawArg = extractRawUrl(call.arguments()).trim();
        final String resolvedTarget = (sessionData != null && rawArg.contains("${"))
                ? sessionData.resolveAvailableVariables(rawArg).trim()
                : rawArg;

        if (resolvedTarget.isEmpty())
        {
            return blockOrEscalate(call, context, sessionData,
                    "Navigation target URL is empty.",
                    MALFORMED_URL_RECOMMENDATION);
        }

        final String targetLower = resolvedTarget.toLowerCase(Locale.ROOT);

        // Always reject dangerous executable/local schemes regardless of browser state or configuration
        if (targetLower.startsWith("javascript:")
                || targetLower.startsWith("vbscript:")
                || targetLower.startsWith("data:")
                || targetLower.startsWith("file:")
                || targetLower.startsWith("/\\"))
        {
            final String safeTarget = sanitizeForMessage(resolvedTarget, sessionData);
            return InterceptionVerdict.reject(call.callId(),
                    "Navigation to unsafe scheme or path '" + safeTarget + "' is prohibited.");
        }

        final AiConfiguration activeConfig = config();
        if (!activeConfig.isNavigationGuardEnabled())
        {
            return InterceptionVerdict.allow("Navigation provenance guard disabled via configuration");
        }

        final String currentUrl = safeGetCurrentUrl();
        final boolean browserIsBlank = isBlankBrowserUrl(currentUrl);
        final String instructionLower = buildEffectiveInstructionLower(execContext);

        // Handle explicit about:blank target: prevent mid-test self-blanking unless step asked for about:blank
        if (targetLower.startsWith("about:blank"))
        {
            if (browserIsBlank || instructionLower.contains("about:blank"))
            {
                return InterceptionVerdict.allow("Blank page navigation permitted");
            }
            return blockOrEscalate(call, context, sessionData,
                    "Uncommanded navigation to 'about:blank' blocked on active page.",
                    PROVENANCE_RECOMMENDATION);
        }

        // If an unresolved placeholder literally matches a placeholder written in the step, allow so variable resolver handles it
        if (resolvedTarget.startsWith("${") && instructionLower.contains(targetLower))
        {
            return InterceptionVerdict.allow("Placeholder URL matches step instruction");
        }

        // Rule A: When browser has not started or active tab is blank (start-of-test or mid-test blank tab), allow any HTTP(S) target
        if (browserIsBlank)
        {
            return InterceptionVerdict.allow("Rule A: Browser tab is blank; initial navigation permitted");
        }

        final ParsedTarget parsed = parseCandidateTarget(resolvedTarget);
        if (parsed == null)
        {
            return blockOrEscalate(call, context, sessionData,
                    "Malformed navigation URL '" + sanitizeForMessage(resolvedTarget, sessionData) + "' blocked.",
                    MALFORMED_URL_RECOMMENDATION);
        }

        // Rule B: Explicit host / registrable domain provenance (or configured allowedHosts)
        if (parsed.host() != null)
        {
            if (isAllowedByConfig(parsed.host(), activeConfig.getNavigationAllowedHosts()))
            {
                return InterceptionVerdict.allow("Rule B: Host permitted via neodymium.ai.navigation.allowedHosts");
            }

            if (matchesInstructionHost(instructionLower, parsed))
            {
                return InterceptionVerdict.allow("Rule B: Host provenance matched step instruction");
            }
        }

        // Rule C: Relative path / query / anchor provenance (for relative URLs or same-host full URLs)
        final ParsedTarget currentParsed = parseCandidateTarget(currentUrl);
        final String currentHost = currentParsed != null ? currentParsed.host() : null;
        final boolean isRelativeOrSameHost = parsed.host() == null
                || (currentHost != null && parsed.host().equals(currentHost));

        if (isRelativeOrSameHost && matchesRelativeProvenance(instructionLower, parsed))
        {
            return InterceptionVerdict.allow("Rule C: Relative path/query provenance matched step instruction");
        }

        final String safeTarget = sanitizeForMessage(resolvedTarget, sessionData);
        return blockOrEscalate(
                call,
                context,
                sessionData,
                "Navigation to '" + safeTarget
                        + "' was blocked because that exact URL, host, or path does not match the current step instruction.",
                PROVENANCE_RECOMMENDATION);
    }

    private static InterceptionVerdict blockOrEscalate(
            final ToolCall call,
            final ToolContext context,
            final SessionData sessionData,
            final String baseReason,
            final String recommendation)
    {
        final String sanitizedReason = sanitizeForMessage(baseReason, sessionData);
        final int attempt = incrementBlockedAttempts(context);
        if (attempt >= 2)
        {
            return InterceptionVerdict.reject(call.callId(), sanitizedReason);
        }
        return InterceptionVerdict.retryWithFeedback(call.callId(), sanitizedReason, recommendation);
    }

    private static int incrementBlockedAttempts(final ToolContext context)
    {
        if (context == null)
        {
            return 1;
        }
        final int prev = context.getVariable(KEY_BLOCKED_NAV_ATTEMPTS, Integer.class).orElse(0);
        final int next = prev + 1;
        context.setVariable(KEY_BLOCKED_NAV_ATTEMPTS, next);
        return next;
    }

    private static boolean isAllowedByConfig(final String candidateHost, final List<String> allowedHosts)
    {
        if (allowedHosts == null || allowedHosts.isEmpty())
        {
            return false;
        }
        final String asciiCandidate = toAsciiHostSafe(candidateHost);
        for (final String rawAllowed : allowedHosts)
        {
            final String allowed = normalizeHost(rawAllowed);
            if (candidateHost.equals(allowed) || candidateHost.endsWith("." + allowed))
            {
                return true;
            }
            if (asciiCandidate != null && (asciiCandidate.equals(allowed) || asciiCandidate.endsWith("." + allowed)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesInstructionHost(final String instructionLower, final ParsedTarget parsed)
    {
        final String host = parsed.host();
        if (host == null || instructionLower.isEmpty())
        {
            return false;
        }

        if (containsHostWithCompatiblePath(instructionLower, host, parsed))
        {
            return true;
        }

        final String asciiHost = toAsciiHostSafe(host);
        if (asciiHost != null && !asciiHost.equals(host)
                && containsHostWithCompatiblePath(instructionLower, asciiHost, parsed))
        {
            return true;
        }

        final String registrable = extractRegistrableDomain(host);
        if (registrable != null && !registrable.equals(host))
        {
            if (containsHostWithCompatiblePath(instructionLower, registrable, parsed))
            {
                return true;
            }
            final String asciiRegistrable = toAsciiHostSafe(registrable);
            if (asciiRegistrable != null && !asciiRegistrable.equals(registrable)
                    && containsHostWithCompatiblePath(instructionLower, asciiRegistrable, parsed))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean containsHostWithCompatiblePath(
            final String text,
            final String hostToken,
            final ParsedTarget parsed)
    {
        int fromIndex = 0;
        while (fromIndex < text.length())
        {
            final int idx = text.indexOf(hostToken, fromIndex);
            if (idx < 0)
            {
                return false;
            }
            fromIndex = idx + 1;

            if (!hasValidHostLeftBoundary(text, idx))
            {
                continue;
            }

            final int end = idx + hostToken.length();
            if (!hasValidHostRightBoundary(text, end))
            {
                continue;
            }

            // Advance past optional :port following the matched host in the step instruction
            int afterHost = end;
            if (afterHost < text.length() && text.charAt(afterHost) == ':')
            {
                int portEnd = afterHost + 1;
                while (portEnd < text.length() && Character.isDigit(text.charAt(portEnd)))
                {
                    portEnd++;
                }
                if (portEnd > afterHost + 1)
                {
                    afterHost = portEnd;
                }
            }

            // If the instruction explicitly attached a non-root path, query, or fragment onto this host
            // (e.g. https://shop.de/cart or https://shop.de/?lang=en), require the candidate's path/query to match as well
            // so https://shop.de/admin cannot slip through.
            final boolean instructionHasExplicitPathAfterHost = afterHost + 1 < text.length()
                    && (text.charAt(afterHost) == '/'
                            || text.charAt(afterHost) == '?'
                            || text.charAt(afterHost) == '#')
                    && !Character.isWhitespace(text.charAt(afterHost + 1))
                    && !isClosingDelimiterOrQuote(text.charAt(afterHost + 1));

            if (!instructionHasExplicitPathAfterHost || matchesRelativeProvenance(text, parsed))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValidHostLeftBoundary(final String text, final int idx)
    {
        if (idx == 0)
        {
            return true;
        }
        if (idx >= 4 && text.startsWith("www.", idx - 4))
        {
            final int beforeWww = idx - 4;
            if (beforeWww == 0)
            {
                return true;
            }
            final char prev = text.charAt(beforeWww - 1);
            return !isHostLabelChar(prev) && prev != '.';
        }
        final char prev = text.charAt(idx - 1);
        return !isHostLabelChar(prev) && prev != '.';
    }

    private static boolean hasValidHostRightBoundary(final String text, final int end)
    {
        if (end >= text.length())
        {
            return true;
        }
        final char next = text.charAt(end);
        if (isHostLabelChar(next))
        {
            return false;
        }
        if (next == '.')
        {
            return end + 1 >= text.length() || !isHostLabelChar(text.charAt(end + 1));
        }
        return true;
    }

    private static boolean matchesRelativeProvenance(final String instructionLower, final ParsedTarget parsed)
    {
        if (instructionLower.isEmpty())
        {
            return false;
        }

        final String fullRelative = parsed.fullRelativeLower();
        final String basePath = parsed.basePathLower();

        // Case 1: Root "/" with no query or fragment requires explicit quoting ("/", '/', `/`)
        if ("/".equals(fullRelative))
        {
            return instructionLower.contains("\"/\"")
                    || instructionLower.contains("'/'")
                    || instructionLower.contains("`/`")
                    || instructionLower.contains("„/“")
                    || instructionLower.contains("“/”")
                    || instructionLower.contains("‘/’");
        }

        // Case 2: Full relative target including query/fragment (e.g. "/?a=b", "/#pricing", "/cart?promo=1")
        if (fullRelative.startsWith("/") && fullRelative.length() >= 2 && containsSlashToken(instructionLower, fullRelative))
        {
            return true;
        }

        // Support "?a=b" written without leading slash in step (e.g. "Open ?a=b" -> navigate("/?a=b") or navigate("?a=b"))
        if ("/".equals(basePath) && parsed.queryLower() != null && parsed.fragmentLower() == null)
        {
            final String queryToken = "?" + parsed.queryLower();
            if (QUERY_KEY_VALUE_PATTERN.matcher(queryToken).matches()
                    && containsQueryToken(instructionLower, queryToken))
            {
                return true;
            }
        }

        // Case 3: Normalized non-root base path (e.g. "/cart" for "/cart/" or "/cart?ts=1")
        return basePath.length() >= 2
                && NON_ROOT_PATH_PATTERN.matcher(basePath).matches()
                && containsSlashToken(instructionLower, basePath);
    }

    private static boolean containsSlashToken(final String text, final String token)
    {
        int fromIndex = 0;
        while (fromIndex < text.length())
        {
            final int idx = text.indexOf(token, fromIndex);
            if (idx < 0)
            {
                return false;
            }
            fromIndex = idx + 1;

            if (idx > 0)
            {
                final char prev = text.charAt(idx - 1);
                // Allow non-alphanumeric delimiters (whitespace, quotes, parens, colons, etc.) or host/port in expanded URLs
                if (Character.isLetterOrDigit(prev) && !isPrecededByUrlHostOrPort(text, idx))
                {
                    continue;
                }
                // Allow double slash only after http(s)://host (e.g. expanded ${baseUrl}/cart where baseUrl ends with '/')
                if (prev == '/' && !isPrecededByUrlHostOrPort(text, idx - 1))
                {
                    continue;
                }
            }

            final int end = idx + token.length();
            if (hasValidRelativeRightBoundary(text, end))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsQueryToken(final String text, final String queryToken)
    {
        int fromIndex = 0;
        while (fromIndex < text.length())
        {
            final int idx = text.indexOf(queryToken, fromIndex);
            if (idx < 0)
            {
                return false;
            }
            fromIndex = idx + 1;

            if (idx > 0)
            {
                final char prev = text.charAt(idx - 1);
                if (Character.isLetterOrDigit(prev))
                {
                    continue;
                }
            }

            final int end = idx + queryToken.length();
            if (hasValidRelativeRightBoundary(text, end))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrecededByUrlHostOrPort(final String text, final int slashIdx)
    {
        if (slashIdx <= 0)
        {
            return false;
        }
        int start = slashIdx - 1;
        while (start >= 0 && !Character.isWhitespace(text.charAt(start)))
        {
            start--;
        }
        int tokenStart = start + 1;
        while (tokenStart < slashIdx && isOpeningDelimiterOrQuote(text.charAt(tokenStart)))
        {
            tokenStart++;
        }
        final String prefix = text.substring(tokenStart, slashIdx);
        if (prefix.startsWith("http://"))
        {
            return prefix.length() > "http://".length() && prefix.charAt("http://".length()) != '/';
        }
        if (prefix.startsWith("https://"))
        {
            return prefix.length() > "https://".length() && prefix.charAt("https://".length()) != '/';
        }
        return false;
    }

    private static boolean hasValidRelativeRightBoundary(final String text, final int end)
    {
        if (end >= text.length())
        {
            return true;
        }
        final char next = text.charAt(end);
        if (Character.isWhitespace(next) || isClosingDelimiterOrQuote(next))
        {
            return true;
        }
        if (next == '.')
        {
            return end + 1 >= text.length() || !Character.isLetterOrDigit(text.charAt(end + 1));
        }
        if (next == '/' || next == '?' || next == '!')
        {
            return end + 1 >= text.length()
                    || Character.isWhitespace(text.charAt(end + 1))
                    || isClosingDelimiterOrQuote(text.charAt(end + 1));
        }
        return false;
    }

    private static boolean isOpeningDelimiterOrQuote(final char c)
    {
        return c == '"' || c == '\'' || c == '`'
                || c == '“' || c == '”' || c == '„' || c == '‘' || c == '’' || c == '«' || c == '»'
                || c == '(' || c == '[' || c == '{' || c == '<';
    }

    private static boolean isClosingDelimiterOrQuote(final char c)
    {
        return c == '"' || c == '\'' || c == '`'
                || c == '“' || c == '”' || c == '„' || c == '‘' || c == '’' || c == '«' || c == '»'
                || c == ')' || c == ']' || c == '}' || c == '>'
                || c == ',' || c == ';' || c == ':';
    }

    private static boolean isHostLabelChar(final char c)
    {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }

    private static ParsedTarget parseCandidateTarget(final String rawTarget)
    {
        if (rawTarget == null || rawTarget.isBlank())
        {
            return null;
        }
        final String trimmed = rawTarget.trim();

        // Relative path, query-only, or anchor-only target (excluding protocol-relative //host)
        final boolean isRelative = (trimmed.startsWith("/") && !trimmed.startsWith("//") && !trimmed.startsWith("/\\"))
                || trimmed.startsWith("./")
                || trimmed.startsWith("../")
                || trimmed.startsWith("?")
                || trimmed.startsWith("#");

        try
        {
            if (isRelative)
            {
                final String relSuffix = sanitizePathQueryFragment(trimmed.startsWith("/") ? trimmed : "/" + trimmed);
                final URI relUri = URI.create("https://relative.placeholder" + relSuffix);
                return buildParsedTarget(null, relUri.getRawPath(), relUri.getRawQuery(), relUri.getRawFragment());
            }

            final String uriInput;
            if (trimmed.startsWith("//"))
            {
                uriInput = "https:" + trimmed;
            }
            else if (trimmed.contains("://"))
            {
                uriInput = trimmed;
            }
            else
            {
                uriInput = "https://" + trimmed;
            }

            final String sanitizedUriInput = sanitizeFullUriForParse(uriInput);
            if (sanitizedUriInput == null)
            {
                return null;
            }

            final URI uri = URI.create(sanitizedUriInput);
            String host = uri.getHost();
            if (host == null && uri.getRawAuthority() != null)
            {
                String auth = uri.getRawAuthority();
                final int atIdx = auth.lastIndexOf('@');
                if (atIdx >= 0)
                {
                    auth = auth.substring(atIdx + 1);
                }
                final int colonIdx = auth.indexOf(':');
                host = colonIdx >= 0 ? auth.substring(0, colonIdx) : auth;
            }
            if (host == null || host.isBlank())
            {
                return null;
            }

            final String normalizedHost = normalizeHost(host);
            if (normalizedHost.isEmpty())
            {
                return null;
            }
            return buildParsedTarget(normalizedHost, uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment());
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    private static String sanitizeFullUriForParse(final String uriInput)
    {
        final int schemeSep = uriInput.indexOf("://");
        if (schemeSep <= 0)
        {
            return null;
        }
        final String scheme = uriInput.substring(0, schemeSep);
        for (int i = 0; i < scheme.length(); i++)
        {
            final char sc = scheme.charAt(i);
            final boolean valid = (i == 0)
                    ? ((sc >= 'a' && sc <= 'z') || (sc >= 'A' && sc <= 'Z'))
                    : ((sc >= 'a' && sc <= 'z') || (sc >= 'A' && sc <= 'Z')
                            || (sc >= '0' && sc <= '9') || sc == '+' || sc == '-' || sc == '.');
            if (!valid)
            {
                return null;
            }
        }

        final String afterScheme = uriInput.substring(schemeSep + 3);
        int authorityEnd = afterScheme.length();
        for (int i = 0; i < afterScheme.length(); i++)
        {
            final char c = afterScheme.charAt(i);
            if (c == '/' || c == '?' || c == '#')
            {
                authorityEnd = i;
                break;
            }
        }

        final String authority = afterScheme.substring(0, authorityEnd);
        if (authority.isEmpty())
        {
            return null;
        }

        String rest = afterScheme.substring(authorityEnd);
        if (!rest.isEmpty() && !rest.startsWith("/"))
        {
            rest = "/" + rest;
        }

        final int atIdx = authority.lastIndexOf('@');
        final String userInfoPrefix = atIdx >= 0
                ? sanitizePathQueryFragment(authority.substring(0, atIdx)) + "@"
                : "";
        final String hostPort = atIdx >= 0 ? authority.substring(atIdx + 1) : authority;

        final String rawHost;
        final String portSuffix;
        if (hostPort.startsWith("["))
        {
            final int closeBracket = hostPort.indexOf(']');
            if (closeBracket < 0)
            {
                return null;
            }
            rawHost = hostPort.substring(0, closeBracket + 1);
            portSuffix = hostPort.substring(closeBracket + 1);
        }
        else
        {
            final int colonIdx = hostPort.indexOf(':');
            rawHost = colonIdx >= 0 ? hostPort.substring(0, colonIdx) : hostPort;
            portSuffix = colonIdx >= 0 ? hostPort.substring(colonIdx) : "";
        }

        if (rawHost.isBlank())
        {
            return null;
        }

        final String asciiHost;
        if (rawHost.startsWith("["))
        {
            asciiHost = rawHost;
        }
        else
        {
            asciiHost = IDN.toASCII(rawHost, IDN.ALLOW_UNASSIGNED);
            if (asciiHost.isBlank())
            {
                return null;
            }
        }

        return scheme + "://" + userInfoPrefix + asciiHost + portSuffix + sanitizePathQueryFragment(rest);
    }

    private static String sanitizePathQueryFragment(final String input)
    {
        if (input == null || input.isEmpty())
        {
            return "";
        }
        final StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;
        while (i < input.length())
        {
            final char c = input.charAt(i);
            if (c == '%')
            {
                if (i + 2 < input.length() && isHexDigit(input.charAt(i + 1)) && isHexDigit(input.charAt(i + 2)))
                {
                    out.append(c).append(input.charAt(i + 1)).append(input.charAt(i + 2));
                    i += 3;
                }
                else
                {
                    out.append("%25");
                    i++;
                }
            }
            else if (c <= 0x20 || c >= 0x7F
                    || c == '[' || c == ']' || c == '|' || c == '{' || c == '}'
                    || c == '^' || c == '\\' || c == '"' || c == '<' || c == '>' || c == '`')
            {
                final int codePoint = input.codePointAt(i);
                final byte[] utf8 = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                for (final byte b : utf8)
                {
                    out.append('%')
                            .append(HEX_DIGITS[(b >> 4) & 0x0F])
                            .append(HEX_DIGITS[b & 0x0F]);
                }
                i += Character.charCount(codePoint);
            }
            else
            {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    static String decodePercentEncoded(final String input)
    {
        if (input == null || input.indexOf('%') < 0)
        {
            return input;
        }
        final int len = input.length();
        final StringBuilder out = new StringBuilder(len);
        final ByteArrayOutputStream byteBuf = new ByteArrayOutputStream(16);
        int i = 0;
        while (i < len)
        {
            final char c = input.charAt(i);
            if (c == '%' && i + 2 < len
                    && isHexDigit(input.charAt(i + 1))
                    && isHexDigit(input.charAt(i + 2)))
            {
                byteBuf.reset();
                while (i + 2 < len
                        && input.charAt(i) == '%'
                        && isHexDigit(input.charAt(i + 1))
                        && isHexDigit(input.charAt(i + 2)))
                {
                    final int hi = Character.digit(input.charAt(i + 1), 16);
                    final int lo = Character.digit(input.charAt(i + 2), 16);
                    byteBuf.write((hi << 4) | lo);
                    i += 3;
                }
                out.append(byteBuf.toString(StandardCharsets.UTF_8));
            }
            else
            {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isHexDigit(final char c)
    {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static ParsedTarget buildParsedTarget(
            final String normalizedHost,
            final String rawPath,
            final String rawQuery,
            final String rawFragment)
    {
        final String decodedPath = decodePercentEncoded((rawPath == null || rawPath.isEmpty()) ? "/" : rawPath);
        String path = collapseConsecutiveSlashes(decodedPath);
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        final String pathLower = path.toLowerCase(Locale.ROOT);
        String basePathLower = pathLower;
        while (basePathLower.length() > 1 && basePathLower.endsWith("/"))
        {
            basePathLower = basePathLower.substring(0, basePathLower.length() - 1);
        }

        final String queryLower = (rawQuery != null && !rawQuery.isEmpty())
                ? decodePercentEncoded(rawQuery).toLowerCase(Locale.ROOT)
                : null;
        final String fragmentLower = (rawFragment != null && !rawFragment.isEmpty())
                ? decodePercentEncoded(rawFragment).toLowerCase(Locale.ROOT)
                : null;

        final StringBuilder fullRel = new StringBuilder(basePathLower);
        if (queryLower != null)
        {
            fullRel.append('?').append(queryLower);
        }
        if (fragmentLower != null)
        {
            fullRel.append('#').append(fragmentLower);
        }

        return new ParsedTarget(normalizedHost, basePathLower, queryLower, fragmentLower, fullRel.toString());
    }

    private static String collapseConsecutiveSlashes(final String path)
    {
        if (path == null || path.indexOf("//") < 0)
        {
            return path;
        }
        final StringBuilder sb = new StringBuilder(path.length());
        boolean prevSlash = false;
        for (int i = 0; i < path.length(); i++)
        {
            final char c = path.charAt(i);
            if (c == '/')
            {
                if (!prevSlash)
                {
                    sb.append(c);
                }
                prevSlash = true;
            }
            else
            {
                sb.append(c);
                prevSlash = false;
            }
        }
        return sb.toString();
    }

    private static String normalizeHost(final String rawHost)
    {
        if (rawHost == null)
        {
            return "";
        }
        String h = rawHost.trim().toLowerCase(Locale.ROOT);
        if (h.startsWith("[") && h.endsWith("]") && h.length() > 2)
        {
            h = h.substring(1, h.length() - 1);
        }
        while (h.endsWith("."))
        {
            h = h.substring(0, h.length() - 1);
        }
        if (h.startsWith("www.") && h.length() > 4)
        {
            h = h.substring(4);
        }
        if (!h.isEmpty() && !h.contains(":"))
        {
            try
            {
                h = IDN.toUnicode(h, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
            }
            catch (final Exception ignored)
            {
            }
        }
        return h;
    }

    private static String toAsciiHostSafe(final String host)
    {
        if (host == null || host.isEmpty() || host.contains(":"))
        {
            return null;
        }
        try
        {
            return IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    private static String extractRegistrableDomain(final String normalizedHost)
    {
        try
        {
            final String candidate = InternetDomainName.isValid(normalizedHost)
                    ? normalizedHost
                    : toAsciiHostSafe(normalizedHost);
            if (candidate != null && InternetDomainName.isValid(candidate))
            {
                final InternetDomainName domain = InternetDomainName.from(candidate);
                if (domain.isUnderPublicSuffix())
                {
                    return normalizeHost(domain.topPrivateDomain().toString());
                }
            }
        }
        catch (final Exception ignored)
        {
        }
        return null;
    }

    private String safeGetCurrentUrl()
    {
        try
        {
            return this.currentUrlSupplier.get();
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    private static String readLiveBrowserUrl()
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return null;
        }
        try
        {
            final WebDriver driver = WebDriverRunner.getWebDriver();
            return driver != null ? driver.getCurrentUrl() : null;
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    private static boolean isBlankBrowserUrl(final String url)
    {
        if (url == null || url.isBlank())
        {
            return true;
        }
        final String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("about:blank") || lower.startsWith("data:");
    }

    private static ExecutionContext resolveExecutionContext(final ToolContext toolContext)
    {
        if (toolContext != null)
        {
            final ExecutionContext fromTool = toolContext.getVariable("neodymium.executionContext", ExecutionContext.class).orElse(null);
            if (fromTool != null)
            {
                return fromTool;
            }
        }
        return ExecutionContext.getActiveContext();
    }

    private static String buildEffectiveInstructionLower(final ExecutionContext execContext)
    {
        if (execContext == null)
        {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        final SessionData sessionData = execContext.getSessionData();

        appendResolved(sb, (String) execContext.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION), sessionData);
        appendResolved(sb, (String) execContext.getTransientData().get("KEY_CURRENT_STEP_RAW_INSTRUCTION"), sessionData);

        final Object stepObj = execContext.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        if (stepObj instanceof final PlaybookStep step)
        {
            appendResolved(sb, step.getInstruction(), sessionData);
            if (step.getParent() != null)
            {
                appendResolved(sb, step.getParent().getInstruction(), sessionData);
            }
        }

        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) execContext.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);
        if (milestones != null)
        {
            for (final String m : milestones)
            {
                appendResolved(sb, m, sessionData);
            }
        }

        return decodePercentEncoded(sb.toString()).toLowerCase(Locale.ROOT);
    }

    private static void appendResolved(final StringBuilder sb, final String text, final SessionData sessionData)
    {
        if (text == null || text.isBlank())
        {
            return;
        }
        sb.append(' ').append(text);
        if (sessionData != null && text.contains("${"))
        {
            sb.append(' ').append(sessionData.resolveAvailableVariables(text));
        }
    }

    private static String extractRawUrl(final JsonNode args)
    {
        if (args == null)
        {
            return "";
        }
        if (args.hasNonNull("url") && !args.path("url").asText().isBlank())
        {
            return args.path("url").asText();
        }
        if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
        {
            return args.path("target").asText();
        }
        if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
        {
            return args.path("locator").asText();
        }
        if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
        {
            return args.path("value").asText();
        }
        return "";
    }

    private static String normalizeToolName(final String rawToolName)
    {
        String name = rawToolName.trim().toLowerCase(Locale.ROOT);
        final int colonIdx = name.indexOf(':');
        if (colonIdx >= 0)
        {
            name = name.substring(colonIdx + 1);
        }
        if (name.startsWith("browser_"))
        {
            name = name.substring("browser_".length());
        }
        return name;
    }

    private static String sanitizeForMessage(final String text, final SessionData sessionData)
    {
        if (text == null || sessionData == null)
        {
            return text;
        }
        return new DefaultActionSanitizer().sanitizeText(text, sessionData);
    }

    private record ParsedTarget(
            String host,
            String basePathLower,
            String queryLower,
            String fragmentLower,
            String fullRelativeLower)
    {
    }
}
