// ============================================================================
// Neodymium Aura Dashboard - Assistant Chat Side-Panel
// ============================================================================

let pendingSubmittedPrompt = null;

function updateHiddenSessionId(val) {
    const hidden = document.getElementById('hiddenSessionId');
    if (hidden) {
        hidden.value = val;
    }
}
window.updateHiddenSessionId = updateHiddenSessionId;

function setChatPrompt(promptText) {
    syncHiddenActiveFile();
    const input = document.getElementById('chatInput');
    if (input) {
        input.value = promptText;
        const form = input.closest('form');
        if (form) {
            if (typeof htmx !== 'undefined') {
                htmx.trigger(form, 'submit');
            } else {
                form.submit();
            }
        }
    }
}
window.setChatPrompt = setChatPrompt;

function syncHiddenActiveFile() {
    const hiddenFile = document.getElementById('hiddenActiveFile');
    if (hiddenFile) {
        const editorEl = document.getElementById('editorFileName');
        const editorFile = editorEl ? editorEl.getAttribute('data-file') : '';
        hiddenFile.value = window.activeEditingFile || editorFile || '';
    }
    syncHiddenViewContext();
}
window.syncHiddenActiveFile = syncHiddenActiveFile;

function buildActiveViewContext() {
    const parts = [];
    const container = document.getElementById('mainViewContainer');
    const dashboardEl = document.getElementById('pageCustomDashboard');
    const batchOverviewEl = document.getElementById('pageBatchHistoryOverview');
    const testBaseEl = document.getElementById('pageTestBase');
    const runReportEl = container && typeof container.querySelector === 'function'
        ? container.querySelector('[data-run-id]')
        : null;

    if (dashboardEl && typeof dashboardEl.getAttribute === 'function') {
        const pageId = dashboardEl.getAttribute('data-page-id') || '';
        const pageName = dashboardEl.getAttribute('data-page-name') || '';
        parts.push('view=CustomDashboard');
        if (pageId) parts.push('pageId=' + pageId);
        if (pageName) parts.push('pageName=' + pageName);
    } else if (batchOverviewEl && typeof batchOverviewEl.getAttribute === 'function') {
        const batchName = batchOverviewEl.getAttribute('data-batch-name') || '';
        const groupType = batchOverviewEl.getAttribute('data-group-type') || '';
        const groupValue = batchOverviewEl.getAttribute('data-group-value') || '';
        const scopeTitle = batchOverviewEl.getAttribute('data-scope-title') || '';
        parts.push('view=RunsOverview');
        if (batchName) parts.push('batchName=' + batchName);
        if (groupType) parts.push('groupType=' + groupType);
        if (groupValue) parts.push('groupValue=' + groupValue);
        if (scopeTitle) parts.push('scopeTitle=' + scopeTitle);
    } else if (testBaseEl) {
        parts.push('view=TestBase');
    } else if (runReportEl && typeof runReportEl.getAttribute === 'function') {
        const runId = runReportEl.getAttribute('data-run-id') || '';
        const batchName = runReportEl.getAttribute('data-batch-name') || '';
        const env = runReportEl.getAttribute('data-environment') || '';
        parts.push('view=RunReport');
        if (runId) parts.push('runId=' + runId);
        if (batchName) parts.push('batchName=' + batchName);
        if (env) parts.push('environment=' + env);
    } else if (document.getElementById('auraTestManagerWorkspace')) {
        parts.push('view=PlaybooksAndQueue');
    }

    if (typeof window !== 'undefined' && window.location && window.location.pathname) {
        const locStr = window.location.pathname + (window.location.search || '');
        if (locStr) {
            parts.push('url=' + locStr);
        }
    }
    return parts.join('; ');
}

function syncHiddenViewContext() {
    const hiddenCtx = document.getElementById('hiddenViewContext');
    if (!hiddenCtx) return;
    hiddenCtx.value = buildActiveViewContext();
}
window.buildActiveViewContext = buildActiveViewContext;
window.syncHiddenViewContext = syncHiddenViewContext;

function getChatRequestPath(detail) {
    if (!detail) return '';
    if (detail.pathInfo && typeof detail.pathInfo.requestPath === 'string') {
        return detail.pathInfo.requestPath;
    }
    if (detail.requestConfig && typeof detail.requestConfig.path === 'string') {
        return detail.requestConfig.path;
    }
    const elt = detail.elt;
    if (elt && typeof elt.getAttribute === 'function') {
        return elt.getAttribute('hx-post') || elt.getAttribute('hx-get') || '';
    }
    return '';
}

function clearChatInput(event) {
    syncHiddenActiveFile();

    const input = document.getElementById('chatInput');
    if (!input) {
        if (event && typeof event.preventDefault === 'function') {
            event.preventDefault();
        }
        return false;
    }
    const prompt = input.value.trim();
    if (!prompt) {
        if (event && typeof event.preventDefault === 'function') {
            event.preventDefault();
        }
        if (event && typeof event.stopImmediatePropagation === 'function') {
            event.stopImmediatePropagation();
        }
        return false;
    }
    pendingSubmittedPrompt = prompt;

    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        if (typeof chatMessages.querySelectorAll === 'function') {
            chatMessages.querySelectorAll('.thinking-bubble, .optimistic-user-bubble')
                .forEach((el) => el.remove());
        }

        const userBubble = document.createElement('div');
        userBubble.className = 'chat-message user message-user optimistic-user-bubble';
        userBubble.innerHTML = `<div class="chat-message-header"><div class="avatar user-avatar"><span class="material-symbols-outlined">person</span></div><span class="sender">You</span></div><div class="text">${escapeHtml(prompt)}</div>`;
        chatMessages.appendChild(userBubble);

        const thinkingBubble = document.createElement('div');
        thinkingBubble.className = 'chat-message ai message-ai thinking-bubble';
        thinkingBubble.innerHTML = `
            <div class="chat-message-header"><div class="avatar ai-avatar"><span class="material-symbols-outlined">smart_toy</span></div><span class="sender">AI Assistant</span></div>
            <div class="typing-indicator"><span></span><span></span><span></span></div>
        `;
        chatMessages.appendChild(thinkingBubble);
        scrollToBottom();
    }

    return true;
}
window.clearChatInput = clearChatInput;

function restoreChatConfirmationButtons(root) {
    const scope = root && typeof root.querySelectorAll === 'function'
        ? root
        : (typeof document !== 'undefined' ? document : null);
    if (!scope || typeof scope.querySelectorAll !== 'function') return;
    scope.querySelectorAll('.chat-confirmation-actions button').forEach((btn) => {
        btn.disabled = false;
        const originalLabel = typeof btn.getAttribute === 'function'
            ? btn.getAttribute('data-original-label')
            : null;
        if (originalLabel !== null && originalLabel !== '') {
            btn.textContent = originalLabel;
        }
    });
}
window.restoreChatConfirmationButtons = restoreChatConfirmationButtons;

function submitChatConfirmation(buttonEl, decision) {
    if (!buttonEl || (decision !== 'confirm' && decision !== 'cancel')) return;
    const actionId = typeof buttonEl.getAttribute === 'function' ? buttonEl.getAttribute('data-action-id') : '';
    const hidden = document.getElementById('hiddenSessionId');
    const attrSessionId = typeof buttonEl.getAttribute === 'function' ? buttonEl.getAttribute('data-session-id') : '';
    const sessionId = attrSessionId || (hidden ? hidden.value : '');
    if (!actionId || typeof htmx === 'undefined') return;

    const actionsRow = typeof buttonEl.closest === 'function' ? buttonEl.closest('.chat-confirmation-actions') : null;
    if (actionsRow && typeof actionsRow.querySelectorAll === 'function') {
        actionsRow.querySelectorAll('button').forEach((btn) => {
            if (typeof btn.getAttribute === 'function' && btn.getAttribute('data-original-label') === null) {
                btn.setAttribute('data-original-label', btn.textContent || '');
            }
            btn.disabled = true;
        });
    }
    if (typeof buttonEl.getAttribute === 'function' && buttonEl.getAttribute('data-original-label') === null) {
        buttonEl.setAttribute('data-original-label', buttonEl.textContent || '');
    }
    buttonEl.disabled = true;
    buttonEl.textContent = decision === 'confirm' ? 'Confirming...' : 'Cancelling...';

    htmx.ajax('POST', '/api/chat/' + decision, {
        target: '#chatMessages',
        swap: 'innerHTML',
        values: {
            sessionId: sessionId || '',
            actionId: actionId
        }
    });
}
window.submitChatConfirmation = submitChatConfirmation;

function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}
window.scrollToBottom = scrollToBottom;

function escapeHtml(str) {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}
window.escapeHtml = escapeHtml;

function isSafeMarkdownUrl(url) {
    if (!url || typeof url !== 'string') return false;
    const trimmed = url.trim();
    if (!trimmed || /[\u0000-\u001f]/.test(trimmed)) return false;
    return /^(https?:\/\/[^\s/]|\/(?![/\\]))/i.test(trimmed);
}

function isWordChar(ch) {
    return typeof ch === 'string' && /[a-zA-Z0-9_]/.test(ch);
}

function isStarBoundaryBlockingChar(ch) {
    return typeof ch === 'string' && /[a-zA-Z0-9_/\\]/.test(ch);
}

function isUnderscoreBoundaryBlockingChar(ch) {
    return typeof ch === 'string' && /[a-zA-Z0-9_/\\*]/.test(ch);
}

function isUnderscoreDotBoundary(text, dotIndex) {
    return text[dotIndex] === '.' && isWordChar(text[dotIndex + 1]);
}

function isDotSuffixedUnderscoreToken(text, startIndex, delimiter) {
    let idx = startIndex + delimiter.length;
    while (idx < text.length && !/\s/.test(text[idx])) {
        if (text.startsWith(delimiter, idx) && isUnderscoreDotBoundary(text, idx + delimiter.length)) {
            return true;
        }
        idx++;
    }
    return false;
}

function skipInlineCodeSpan(text, backtickIdx) {
    const closeCode = text.indexOf('`', backtickIdx + 1);
    return closeCode > backtickIdx + 1 ? closeCode + 1 : backtickIdx + 1;
}

function findClosingDoubleDelimiter(text, startIndex, delimiter) {
    const delimChar = delimiter[0];
    let idx = startIndex;
    let depth = 0;
    let firstFallbackClose = -1;
    while (idx < text.length) {
        if (text[idx] === '`') {
            idx = skipInlineCodeSpan(text, idx);
            continue;
        }
        if (text.startsWith(delimiter, idx)) {
            if (text[idx + 2] === delimChar) {
                idx++;
                continue;
            }
            const prev = text[idx - 1];
            const next = text[idx + 2];
            const validClose = !/\s/.test(prev) && prev !== '/' && prev !== '\\'
                    && (delimiter !== '__'
                        || (!isUnderscoreBoundaryBlockingChar(next) && !isUnderscoreDotBoundary(text, idx + 2)));
            const validOpen = !/\s/.test(next) && next !== '/' && next !== '\\' && next !== delimChar
                    && (delimiter !== '__'
                        || (!isUnderscoreBoundaryBlockingChar(prev) && !isDotSuffixedUnderscoreToken(text, idx, '__')));
            if (validClose) {
                if (depth === 0) {
                    return idx;
                }
                if (firstFallbackClose === -1) {
                    firstFallbackClose = idx;
                }
                depth--;
                idx += 2;
                continue;
            }
            if (validOpen) {
                depth++;
            }
            idx += 2;
            continue;
        }
        idx++;
    }
    return firstFallbackClose;
}

function findClosingSingleAsterisk(text, startIndex) {
    let idx = startIndex;
    let depth = 0;
    let firstFallbackClose = -1;
    while (idx < text.length) {
        if (text[idx] === '`') {
            idx = skipInlineCodeSpan(text, idx);
            continue;
        }
        if (text[idx] === '*') {
            if (text[idx + 1] === '*') {
                idx += 2;
                continue;
            }
            const prev = text[idx - 1];
            const next = text[idx + 1];
            const validClose = !/\s/.test(prev) && prev !== '/' && prev !== '\\' && !isWordChar(next);
            const validOpen = !isStarBoundaryBlockingChar(prev) && next && !/[\s./\\]/.test(next);
            if (validClose) {
                if (depth === 0) {
                    return idx;
                }
                if (firstFallbackClose === -1) {
                    firstFallbackClose = idx;
                }
                depth--;
                idx++;
                continue;
            }
            if (validOpen) {
                depth++;
            }
        }
        idx++;
    }
    return firstFallbackClose;
}

function findClosingUnderscore(text, startIndex) {
    let idx = startIndex;
    let depth = 0;
    let firstFallbackClose = -1;
    while (idx < text.length) {
        if (text[idx] === '`') {
            idx = skipInlineCodeSpan(text, idx);
            continue;
        }
        if (text[idx] === '_') {
            if (text[idx + 1] === '_') {
                idx += 2;
                continue;
            }
            const prev = text[idx - 1];
            const next = text[idx + 1];
            const validClose = !/\s/.test(prev) && prev !== '/' && prev !== '\\' && prev !== '*'
                    && !isUnderscoreDotBoundary(text, idx + 1)
                    && !isUnderscoreBoundaryBlockingChar(next);
            const validOpen = !isUnderscoreBoundaryBlockingChar(prev)
                    && next && !/[\s/\\*]/.test(next)
                    && !isDotSuffixedUnderscoreToken(text, idx, '_');
            if (validClose) {
                if (depth === 0) {
                    return idx;
                }
                if (firstFallbackClose === -1) {
                    firstFallbackClose = idx;
                }
                depth--;
                idx++;
                continue;
            }
            if (validOpen) {
                depth++;
            }
        }
        idx++;
    }
    return firstFallbackClose;
}

function appendInlineMarkdown(parent, text) {
    const str = String(text || '');
    let pos = 0;
    let plainStart = 0;

    function flushPlain(endIdx) {
        if (endIdx > plainStart) {
            parent.appendChild(document.createTextNode(str.slice(plainStart, endIdx)));
        }
    }

    while (pos < str.length) {
        const ch = str[pos];

        if (ch === '`') {
            const endCode = str.indexOf('`', pos + 1);
            if (endCode > pos + 1) {
                flushPlain(pos);
                const code = document.createElement('code');
                code.textContent = str.slice(pos + 1, endCode);
                parent.appendChild(code);
                pos = endCode + 1;
                plainStart = pos;
                continue;
            }
        } else if (ch === '*' && str[pos + 1] === '*') {
            const endStrong = findClosingDoubleDelimiter(str, pos + 2, '**');
            if (endStrong > pos + 2) {
                flushPlain(pos);
                const strong = document.createElement('strong');
                appendInlineMarkdown(strong, str.slice(pos + 2, endStrong));
                parent.appendChild(strong);
                pos = endStrong + 2;
                plainStart = pos;
                continue;
            }
        } else if (ch === '_' && str[pos + 1] === '_' && !isUnderscoreBoundaryBlockingChar(str[pos - 1])
                && !isDotSuffixedUnderscoreToken(str, pos, '__')) {
            const endStrongUnderscore = findClosingDoubleDelimiter(str, pos + 2, '__');
            if (endStrongUnderscore > pos + 2 && !isUnderscoreBoundaryBlockingChar(str[endStrongUnderscore + 2])) {
                flushPlain(pos);
                const strong = document.createElement('strong');
                appendInlineMarkdown(strong, str.slice(pos + 2, endStrongUnderscore));
                parent.appendChild(strong);
                pos = endStrongUnderscore + 2;
                plainStart = pos;
                continue;
            }
        } else if (ch === '*' && !isStarBoundaryBlockingChar(str[pos - 1])
                && str[pos + 1] && str[pos + 1] !== '*' && !/[\s./\\]/.test(str[pos + 1])) {
            const endEm = findClosingSingleAsterisk(str, pos + 1);
            if (endEm > pos + 1) {
                flushPlain(pos);
                const em = document.createElement('em');
                appendInlineMarkdown(em, str.slice(pos + 1, endEm));
                parent.appendChild(em);
                pos = endEm + 1;
                plainStart = pos;
                continue;
            }
        } else if (ch === '_' && !isUnderscoreBoundaryBlockingChar(str[pos - 1])
                && str[pos + 1] && str[pos + 1] !== '_' && !/[\s/\\*]/.test(str[pos + 1])
                && !isDotSuffixedUnderscoreToken(str, pos, '_')) {
            const endEmUnderscore = findClosingUnderscore(str, pos + 1);
            if (endEmUnderscore > pos + 1) {
                flushPlain(pos);
                const em = document.createElement('em');
                appendInlineMarkdown(em, str.slice(pos + 1, endEmUnderscore));
                parent.appendChild(em);
                pos = endEmUnderscore + 1;
                plainStart = pos;
                continue;
            }
        } else if (ch === '[') {
            const labelClose = str.indexOf(']', pos + 1);
            if (labelClose > pos + 1 && str[labelClose + 1] === '(') {
                let depth = 1;
                let scan = labelClose + 2;
                while (scan < str.length && depth > 0) {
                    if (str[scan] === '(') {
                        depth++;
                    } else if (str[scan] === ')') {
                        depth--;
                        if (depth === 0) {
                            break;
                        }
                    } else if (str[scan] === '\n') {
                        break;
                    }
                    scan++;
                }
                if (depth === 0) {
                    const label = str.slice(pos + 1, labelClose);
                    const rawUrl = str.slice(labelClose + 2, scan);
                    flushPlain(pos);
                    if (isSafeMarkdownUrl(rawUrl)) {
                        const anchor = document.createElement('a');
                        anchor.href = rawUrl.trim();
                        anchor.textContent = label;
                        anchor.target = '_blank';
                        anchor.rel = 'noopener noreferrer';
                        parent.appendChild(anchor);
                    } else {
                        parent.appendChild(document.createTextNode(str.slice(pos, scan + 1)));
                    }
                    pos = scan + 1;
                    plainStart = pos;
                    continue;
                }
            }
        }
        pos++;
    }

    flushPlain(str.length);
}

function isHorizontalRuleLine(line) {
    return /^\s*([-*_])(?:\s*\1){2,}\s*$/.test(line);
}

function isTableSeparatorLine(line) {
    return /^\s*\|?(?:\s*:?-{3,}:?\s*\|)+(?:\s*:?-{3,}:?\s*)?\|?\s*$/.test(line);
}

function isTableStart(lines, index) {
    return index + 1 < lines.length
            && lines[index].includes('|')
            && isTableSeparatorLine(lines[index + 1]);
}

function parseTableCells(line) {
    let trimmed = line.trim();
    if (trimmed.startsWith('|')) {
        trimmed = trimmed.slice(1);
    }
    if (trimmed.endsWith('|') && !trimmed.endsWith('\\|')) {
        trimmed = trimmed.slice(0, -1);
    }

    const cells = [];
    let current = '';
    let inBackticks = false;
    for (let i = 0; i < trimmed.length; i++) {
        const ch = trimmed[i];
        if (ch === '\\' && i + 1 < trimmed.length && trimmed[i + 1] === '|') {
            current += '|';
            i++;
            continue;
        }
        if (ch === '`') {
            inBackticks = !inBackticks;
            current += ch;
            continue;
        }
        if (ch === '|' && !inBackticks) {
            cells.push(current.trim());
            current = '';
            continue;
        }
        current += ch;
    }
    cells.push(current.trim());
    return cells;
}

function parseListLine(line) {
    if (!line || isHorizontalRuleLine(line)) {
        return null;
    }
    const match = /^(\s*)(?:([-*])|(\d+)\.)\s+(.*)$/.exec(line);
    if (!match) {
        return null;
    }
    return {
        indent: match[1].replace(/\t/g, '  ').length,
        tag: match[2] ? 'ul' : 'ol',
        text: match[4]
    };
}

function appendListBlock(fragment, lines, startIndex) {
    const firstItem = parseListLine(lines[startIndex]);
    if (!firstItem) {
        return startIndex;
    }

    const rootList = document.createElement(firstItem.tag);
    fragment.appendChild(rootList);
    const stack = [{ indent: firstItem.indent, tag: firstItem.tag, listEl: rootList, lastLi: null }];
    let i = startIndex;

    while (i < lines.length) {
        const item = parseListLine(lines[i]);
        if (!item) {
            break;
        }

        while (stack.length > 1 && item.indent < stack[stack.length - 1].indent) {
            stack.pop();
        }
        let top = stack[stack.length - 1];
        if (item.indent > top.indent && top.lastLi) {
            const childList = document.createElement(item.tag);
            top.lastLi.appendChild(childList);
            stack.push({ indent: item.indent, tag: item.tag, listEl: childList, lastLi: null });
            top = stack[stack.length - 1];
        } else if (top.tag !== item.tag) {
            if (stack.length === 1) {
                break;
            }
            stack.pop();
            const parent = stack[stack.length - 1];
            const siblingList = document.createElement(item.tag);
            if (parent.lastLi) {
                parent.lastLi.appendChild(siblingList);
            } else {
                fragment.appendChild(siblingList);
            }
            stack.push({ indent: item.indent, tag: item.tag, listEl: siblingList, lastLi: null });
            top = stack[stack.length - 1];
        }

        const li = document.createElement('li');
        appendInlineMarkdown(li, item.text);
        top.listEl.appendChild(li);
        top.lastLi = li;
        i++;
    }

    return i;
}

function buildMarkdownFragment(rawText) {
    const fragment = document.createDocumentFragment();
    const lines = String(rawText || '').replace(/\r\n/g, '\n').split('\n');
    let i = 0;

    while (i < lines.length) {
        const line = lines[i];

        if (line.trimStart().startsWith('```')) {
            const lang = line.trimStart().slice(3).trim();
            const codeLines = [];
            i++;
            while (i < lines.length && !lines[i].trimStart().startsWith('```')) {
                codeLines.push(lines[i]);
                i++;
            }
            if (i < lines.length) {
                i++;
            }
            const pre = document.createElement('pre');
            const code = document.createElement('code');
            if (lang) {
                code.className = 'language-' + lang.replace(/[^a-zA-Z0-9_-]/g, '');
            }
            code.textContent = codeLines.join('\n');
            pre.appendChild(code);
            fragment.appendChild(pre);
            continue;
        }

        if (line.trim() === '') {
            i++;
            continue;
        }

        if (isHorizontalRuleLine(line)) {
            fragment.appendChild(document.createElement('hr'));
            i++;
            continue;
        }

        const headingMatch = /^(#{1,4})\s+(.+)$/.exec(line);
        if (headingMatch) {
            const heading = document.createElement('h' + headingMatch[1].length);
            appendInlineMarkdown(heading, headingMatch[2]);
            fragment.appendChild(heading);
            i++;
            continue;
        }

        if (/^\s*>\s?/.test(line)) {
            const bqLines = [];
            while (i < lines.length && /^\s*>\s?/.test(lines[i])) {
                bqLines.push(lines[i].replace(/^\s*>\s?/, ''));
                i++;
            }
            const blockquote = document.createElement('blockquote');
            blockquote.appendChild(buildMarkdownFragment(bqLines.join('\n')));
            fragment.appendChild(blockquote);
            continue;
        }

        if (isTableStart(lines, i)) {
            const headerCells = parseTableCells(lines[i]);
            i += 2;
            const table = document.createElement('table');
            const thead = document.createElement('thead');
            const headRow = document.createElement('tr');
            headerCells.forEach((cellText) => {
                const th = document.createElement('th');
                appendInlineMarkdown(th, cellText);
                headRow.appendChild(th);
            });
            thead.appendChild(headRow);
            table.appendChild(thead);

            const tbody = document.createElement('tbody');
            while (i < lines.length
                    && lines[i].trim() !== ''
                    && lines[i].includes('|')
                    && !lines[i].trimStart().startsWith('```')) {
                const rowCells = parseTableCells(lines[i]);
                const row = document.createElement('tr');
                for (let c = 0; c < headerCells.length; c++) {
                    const td = document.createElement('td');
                    appendInlineMarkdown(td, rowCells[c] || '');
                    row.appendChild(td);
                }
                tbody.appendChild(row);
                i++;
            }
            table.appendChild(tbody);
            fragment.appendChild(table);
            continue;
        }

        if (parseListLine(line)) {
            i = appendListBlock(fragment, lines, i);
            continue;
        }

        const p = document.createElement('p');
        let lineCount = 0;
        while (i < lines.length
                && lines[i].trim() !== ''
                && !lines[i].trimStart().startsWith('```')
                && !isHorizontalRuleLine(lines[i])
                && !/^(#{1,4})\s+/.test(lines[i])
                && !/^\s*>\s?/.test(lines[i])
                && !isTableStart(lines, i)
                && !/^\s*[-*]\s+/.test(lines[i])
                && !/^\s*\d+\.\s+/.test(lines[i])) {
            if (lineCount > 0) {
                p.appendChild(document.createElement('br'));
            }
            appendInlineMarkdown(p, lines[i]);
            lineCount++;
            i++;
        }
        fragment.appendChild(p);
    }

    return fragment;
}

function renderChatMarkdown(root) {
    if (typeof document === 'undefined') return;
    const scope = root && typeof root.querySelectorAll === 'function' ? root : document;
    const nodes = scope.querySelectorAll('.chat-message.message-ai .text.chat-markdown');
    nodes.forEach((el) => {
        if (el.getAttribute('data-markdown-rendered') === 'true') return;
        const rawText = el.textContent || '';
        const fragment = buildMarkdownFragment(rawText);
        el.textContent = '';
        el.appendChild(fragment);
        el.setAttribute('data-markdown-rendered', 'true');
    });
}
window.renderChatMarkdown = renderChatMarkdown;

function isChatHtmxRequest(detail) {
    if (!detail) return false;
    const reqPath = getChatRequestPath(detail);
    if (typeof reqPath === 'string' && reqPath.startsWith('/api/chat')) {
        return true;
    }
    if (detail.target && (detail.target.id === 'chatMessages' || detail.target.id === 'chatContainer')) {
        return true;
    }
    return false;
}

function shouldPreserveChatInput(xhr) {
    if (!xhr || typeof xhr.getResponseHeader !== 'function') {
        return false;
    }
    const hxTrigger = xhr.getResponseHeader('HX-Trigger');
    return typeof hxTrigger === 'string' && hxTrigger.includes('chatPreserveInput');
}

function resolveSubmittedChatPrompt(detail) {
    const params = detail && detail.requestConfig ? detail.requestConfig.parameters : null;
    if (params && typeof params.prompt === 'string') {
        return params.prompt.trim();
    }
    return null;
}

function restoreChatInputIfEmpty(detail) {
    const requestPrompt = resolveSubmittedChatPrompt(detail);
    const expectedPrompt = requestPrompt !== null ? requestPrompt : pendingSubmittedPrompt;
    const input = typeof document !== 'undefined' ? document.getElementById('chatInput') : null;
    if (input && typeof expectedPrompt === 'string' && expectedPrompt !== ''
            && typeof input.value === 'string' && input.value.trim() === '') {
        input.value = expectedPrompt;
    }
}

function handleChatRequestFailureCleanup(detail) {
    if (!isChatHtmxRequest(detail)) {
        return;
    }
    if (typeof document !== 'undefined' && typeof document.querySelectorAll === 'function') {
        document.querySelectorAll('.thinking-bubble, .optimistic-user-bubble')
            .forEach((el) => el.remove());
    }
    if (getChatRequestPath(detail) === '/api/chat') {
        restoreChatInputIfEmpty(detail);
    }
    restoreChatConfirmationButtons(detail && detail.target ? detail.target : document);
}

if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
    const scheduleChatMarkdownRender = () => renderChatMarkdown(document);
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scheduleChatMarkdownRender);
    } else {
        renderChatMarkdown(document);
    }
    document.addEventListener('htmx:afterSwap', scheduleChatMarkdownRender);
    document.addEventListener('htmx:oobAfterSwap', scheduleChatMarkdownRender);
    document.addEventListener('htmx:afterSettle', scheduleChatMarkdownRender);
    document.addEventListener('htmx:beforeRequest', (evt) => {
        const detail = evt ? evt.detail : null;
        if (getChatRequestPath(detail) === '/api/chat') {
            const promptVal = resolveSubmittedChatPrompt(detail) || '';
            if (!promptVal) {
                if (evt && typeof evt.preventDefault === 'function') {
                    evt.preventDefault();
                }
                return;
            }
            pendingSubmittedPrompt = promptVal;
            const input = document.getElementById('chatInput');
            if (input && typeof input.value === 'string' && input.value.trim() === promptVal) {
                input.value = '';
            }
        }
    });
    document.addEventListener('htmx:afterRequest', (evt) => {
        const detail = evt ? evt.detail : null;
        const path = getChatRequestPath(detail);
        if (path === '/api/chat') {
            const requestPrompt = resolveSubmittedChatPrompt(detail);
            const failed = !detail || detail.successful !== true || (detail.xhr && detail.xhr.status >= 400);
            if (failed || (detail && shouldPreserveChatInput(detail.xhr))) {
                restoreChatInputIfEmpty(detail);
            }
            if (requestPrompt === null || pendingSubmittedPrompt === requestPrompt) {
                pendingSubmittedPrompt = null;
            }
        } else if ((path === '/api/chat/confirm' || path === '/api/chat/cancel')
                && (!detail || detail.successful !== true || (detail.xhr && detail.xhr.status >= 400))) {
            handleChatRequestFailureCleanup(detail);
        }
    });
    const cleanupChatOnError = (evt) => {
        handleChatRequestFailureCleanup(evt ? evt.detail : null);
    };
    document.addEventListener('htmx:responseError', cleanupChatOnError);
    document.addEventListener('htmx:sendError', cleanupChatOnError);
}

function renameCurrentChatSession() {
    const select = document.getElementById('chatSessionSelect');
    if (!select || select.selectedIndex < 0) return;
    const currentId = select.value;
    const currentName = select.options[select.selectedIndex].text;
    const newName = prompt("Enter new name for this chat session:", currentName);
    if (newName && newName.trim() !== "") {
        htmx.ajax('POST', '/api/chat/rename?id=' + encodeURIComponent(currentId) + '&name=' + encodeURIComponent(newName.trim()), {
            target: '#chatContainer',
            swap: 'innerHTML'
        });
    }
}
window.renameCurrentChatSession = renameCurrentChatSession;

function deleteCurrentChatSession() {
    const select = document.getElementById('chatSessionSelect');
    if (!select) return;
    const currentId = select.value;
    if (confirm("Are you sure you want to delete this chat session?")) {
        htmx.ajax('POST', '/api/chat/delete?id=' + encodeURIComponent(currentId), {
            target: '#chatContainer',
            swap: 'innerHTML'
        });
    }
}
window.deleteCurrentChatSession = deleteCurrentChatSession;
