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
package com.xceptance.aura.chat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Node-backed unit tests for {@code dashboard-chat.js} markdown rendering, XSS neutralization,
 * and empty form submission cancellation.
 *
 * @author AI-generated: Claude Sonnet 4.6
 * @author Xceptance GmbH 2026
 */
public class DashboardChatJsTest
{
    @Test
    public void testMarkdownRendererAndSubmitGuardsViaNode() throws Exception
    {
        Assumptions.assumeTrue(isNodeAvailable(), "Node.js is required to run dashboard-chat.js unit tests");

        final Path chatJsPath = Files.exists(Paths.get("src/main/resources/static/dashboard-chat.js"))
                ? Paths.get("src/main/resources/static/dashboard-chat.js").toAbsolutePath()
                : Paths.get("aura-manager/src/main/resources/static/dashboard-chat.js").toAbsolutePath();

        final String script = """
                const fs = require('fs');
                const vm = require('vm');
                const code = fs.readFileSync(%s, 'utf8');

                class MockNode {
                    constructor(tagName) {
                        this.tagName = tagName ? tagName.toUpperCase() : '#TEXT';
                        this.children = [];
                        this.attributes = {};
                        this._text = '';
                        this.className = '';
                        this.href = '';
                        this.target = '';
                        this.rel = '';
                        this.value = '';
                        this.scrollTop = 0;
                        this.scrollHeight = 100;
                    }
                    set textContent(v) {
                        this._text = String(v);
                        this.children = [];
                    }
                    get textContent() {
                        if (this.children.length === 0) return this._text;
                        return this.children.map(c => c.textContent).join('');
                    }
                    set innerHTML(v) {
                        this._html = String(v);
                    }
                    get innerHTML() {
                        return this._html || '';
                    }
                    appendChild(child) {
                        if (child.tagName === '#FRAGMENT') {
                            child.children.forEach(c => this.children.push(c));
                        } else {
                            this.children.push(child);
                        }
                        return child;
                    }
                    setAttribute(k, v) { this.attributes[k] = String(v); }
                    getAttribute(k) { return this.attributes[k] ?? null; }
                }

                const elements = {};
                const mockDocument = {
                    createElement: (tag) => new MockNode(tag),
                    createTextNode: (txt) => {
                        const n = new MockNode(null);
                        n.textContent = txt;
                        return n;
                    },
                    createDocumentFragment: () => new MockNode('#FRAGMENT'),
                    getElementById: (id) => elements[id] || null,
                    querySelectorAll: () => [],
                    addEventListener: () => {}
                };

                const context = {
                    window: {},
                    document: mockDocument,
                    setTimeout: (fn) => fn(),
                    htmx: { ajax: () => {}, trigger: () => {} }
                };
                context.window.document = mockDocument;
                vm.createContext(context);
                vm.runInContext(code, context);

                const md = [
                    '# Summary',
                    '> Important note with _italic_ and **bold**',
                    '',
                    '| Name | Status |',
                    '| --- | --- |',
                    '| login.yaml | PASS |',
                    '',
                    '---',
                    '',
                    'See [Wiki](https://en.wikipedia.org/wiki/Foo_(bar)), [App](/playbooks), [Proto](//evil.com/phish), [Back](/\\\\evil.com/phish), and [Bad](javascript:alert(1)).'
                ].join('\\n');

                const frag = context.buildMarkdownFragment(md);
                const tags = frag.children.map(c => c.tagName);
                if (!tags.includes('H1')) throw new Error('Expected H1 in ' + JSON.stringify(tags));
                if (!tags.includes('BLOCKQUOTE')) throw new Error('Expected BLOCKQUOTE in ' + JSON.stringify(tags));
                if (!tags.includes('TABLE')) throw new Error('Expected TABLE in ' + JSON.stringify(tags));
                if (!tags.includes('HR')) throw new Error('Expected HR in ' + JSON.stringify(tags));

                const blockquote = frag.children.find(c => c.tagName === 'BLOCKQUOTE');
                const bqEm = JSON.stringify(blockquote);
                if (!bqEm.includes('"tagName":"EM"') || !bqEm.includes('"tagName":"STRONG"')) {
                    throw new Error('Expected _italic_ EM and **bold** STRONG inside blockquote: ' + bqEm);
                }

                const table = frag.children.find(c => c.tagName === 'TABLE');
                const tableStr = JSON.stringify(table);
                if (!tableStr.includes('login.yaml') || !tableStr.includes('PASS')) {
                    throw new Error('Table cells missing: ' + tableStr);
                }

                const lastP = frag.children[frag.children.length - 1];
                const anchors = lastP.children.filter(c => c.tagName === 'A');
                if (anchors.length !== 2) {
                    throw new Error('Expected exactly 2 safe anchors (https + single-slash relative), got ' + anchors.length + ': ' + JSON.stringify(anchors));
                }
                if (anchors[0].href !== 'https://en.wikipedia.org/wiki/Foo_(bar)' || anchors[1].href !== '/playbooks') {
                    throw new Error('Unexpected safe anchor hrefs: ' + JSON.stringify(anchors));
                }

                // Empty submit must call preventDefault
                const emptyInput = new MockNode('INPUT');
                emptyInput.value = '   ';
                elements['chatInput'] = emptyInput;
                elements['hiddenActiveFile'] = new MockNode('INPUT');
                elements['chatMessages'] = new MockNode('DIV');
                let prevented = false;
                const res = context.clearChatInput({ preventDefault: () => { prevented = true; } });
                if (!prevented || res !== false) {
                    throw new Error('Expected empty clearChatInput(event) to preventDefault and return false');
                }

                // Globs and arithmetic must stay plain text; backticked * inside *...* or **...** must not break closing delimiters
                const globFrag = context.buildMarkdownFragment('Run tests/*.yaml and suites/*.yaml with 2*3*4 and Matches *.yaml and foo/*.json');
                const globJson = JSON.stringify(globFrag);
                if (globJson.includes('"tagName":"EM"')) {
                    throw new Error('Path globs or multiplication unexpectedly rendered as EM: ' + globJson);
                }

                const codeInsideEm = context.buildMarkdownFragment('*check `foo*bar` first* and **bold `a**b` end**');
                const codeInsideStr = JSON.stringify(codeInsideEm);
                if (!codeInsideStr.includes('"tagName":"EM"') || !codeInsideStr.includes('"tagName":"STRONG"')
                        || !codeInsideStr.includes('foo*bar') || !codeInsideStr.includes('a**b')) {
                    throw new Error('Code spans inside emphasis/strong broke parsing: ' + codeInsideStr);
                }

                const mixedGlobEm = context.buildMarkdownFragment('Match *.yaml and *important* with foo/__init__.py and /_tmp_/bar plus standalone __init__.py, _tmp_.log, and _ok_.');
                const mixedGlobHtml = JSON.stringify(mixedGlobEm);
                const emNodes = mixedGlobEm.children[0].children.filter(c => c.tagName === 'EM');
                if (mixedGlobHtml.includes('"tagName":"STRONG"')
                        || emNodes.length !== 2
                        || emNodes[0].textContent !== 'important'
                        || emNodes[1].textContent !== 'ok'
                        || mixedGlobEm.children[0].textContent !== 'Match *.yaml and important with foo/__init__.py and /_tmp_/bar plus standalone __init__.py, _tmp_.log, and ok.') {
                    throw new Error('Mixed glob or underscored path broke emphasis boundaries: ' + mixedGlobHtml);
                }

                const wrappedFilenameFrag = context.buildMarkdownFragment('Outer _check _tmp_.log now_ and __see __init__.py here__ stay formatted.');
                const wrappedChildren = wrappedFilenameFrag.children[0].children;
                const wrappedEm = wrappedChildren.find(c => c.tagName === 'EM');
                const wrappedStrong = wrappedChildren.find(c => c.tagName === 'STRONG');
                if (!wrappedEm || wrappedEm.textContent !== 'check _tmp_.log now'
                        || !wrappedStrong || wrappedStrong.textContent !== 'see __init__.py here') {
                    throw new Error('Outer emphasis/strong wrapping dot-suffixed underscore filenames broke: ' + JSON.stringify(wrappedFilenameFrag));
                }

                const dunderInsideEmFrag = context.buildMarkdownFragment('Outer _check __init__ now_ stays formatted.');
                const dunderOuterEm = dunderInsideEmFrag.children[0].children.find(c => c.tagName === 'EM');
                const dunderInnerStrong = dunderOuterEm
                        ? dunderOuterEm.children.find(c => c.tagName === 'STRONG')
                        : null;
                if (!dunderOuterEm || !dunderInnerStrong || dunderInnerStrong.textContent !== 'init'
                        || dunderOuterEm.textContent !== 'check init now') {
                    throw new Error('Dunder token inside outer underscore emphasis broke: ' + JSON.stringify(dunderInsideEmFrag));
                }

                const underscoreStarGlobFrag = context.buildMarkdownFragment('Run tests/*_helper.yaml and tests/_a_*.yaml cleanly.');
                const underscoreStarGlobJson = JSON.stringify(underscoreStarGlobFrag);
                if (underscoreStarGlobJson.includes('"tagName":"EM"') || underscoreStarGlobJson.includes('"tagName":"STRONG"')) {
                    throw new Error('Star+underscore glob unexpectedly rendered emphasis/strong: ' + underscoreStarGlobJson);
                }

                const tripleDelimsFrag = context.buildMarkdownFragment('Both ___hello___ and ***world*** render bold italic, and **foo ** bar** stays one bold.');
                const tripleP = tripleDelimsFrag.children[0];
                const tripleStrongs = tripleP.children.filter(c => c.tagName === 'STRONG');
                if (tripleStrongs.length !== 3
                        || !tripleStrongs[0].children.some(c => c.tagName === 'EM' && c.textContent === 'hello')
                        || !tripleStrongs[1].children.some(c => c.tagName === 'EM' && c.textContent === 'world')
                        || tripleStrongs[2].textContent !== 'foo ** bar') {
                    throw new Error('Triple delimiter or spaced ** failed: ' + JSON.stringify(tripleDelimsFrag));
                }

                const nestedSameFrag = context.buildMarkdownFragment('_check _inner_ now_ and **outer **inner** end**');
                const nestedChildren = nestedSameFrag.children[0].children;
                const outerEm = nestedChildren.find(c => c.tagName === 'EM');
                const outerStrong = nestedChildren.find(c => c.tagName === 'STRONG');
                if (!outerEm || !outerEm.children.some(c => c.tagName === 'EM' && c.textContent === 'inner')
                        || outerEm.textContent !== 'check inner now'
                        || !outerStrong || !outerStrong.children.some(c => c.tagName === 'STRONG' && c.textContent === 'inner')
                        || outerStrong.textContent !== 'outer inner end') {
                    throw new Error('Nested same-delimiter formatting broke: ' + JSON.stringify(nestedSameFrag));
                }
                console.log('OK');
                """.formatted(quoteJs(chatJsPath.toString()));

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", script);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }
        final boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "Node test timed out");
        Assertions.assertEquals(0, process.exitValue(), "Node test failed:\n" + output);
        Assertions.assertTrue(output.toString().contains("OK"));
    }

    @Test
    public void testChatInputPreservationConfirmationRecoveryAndErrorScopingViaNode() throws Exception
    {
        Assumptions.assumeTrue(isNodeAvailable(), "Node.js is required to run dashboard JS lifecycle tests");

        final Path chatJsPath = Files.exists(Paths.get("src/main/resources/static/dashboard-chat.js"))
                ? Paths.get("src/main/resources/static/dashboard-chat.js").toAbsolutePath()
                : Paths.get("aura-manager/src/main/resources/static/dashboard-chat.js").toAbsolutePath();
        final Path coreJsPath = Files.exists(Paths.get("src/main/resources/static/dashboard-core.js"))
                ? Paths.get("src/main/resources/static/dashboard-core.js").toAbsolutePath()
                : Paths.get("aura-manager/src/main/resources/static/dashboard-core.js").toAbsolutePath();

        final String script = """
                const fs = require('fs');
                const vm = require('vm');
                const chatCode = fs.readFileSync(%s, 'utf8');
                const coreCode = fs.readFileSync(%s, 'utf8');

                class MockClassList {
                    constructor(el) { this.el = el; }
                    _parts() { return new Set((this.el.className || '').split(/\\s+/).filter(Boolean)); }
                    add(...cls) { const s = this._parts(); cls.forEach(c => s.add(c)); this.el.className = Array.from(s).join(' '); }
                    remove(...cls) { const s = this._parts(); cls.forEach(c => s.delete(c)); this.el.className = Array.from(s).join(' '); }
                    contains(c) { return this._parts().has(c); }
                }

                class MockNode {
                    constructor(tagName) {
                        this.tagName = tagName ? tagName.toUpperCase() : '#TEXT';
                        this.children = [];
                        this.attributes = {};
                        this.style = {};
                        this.className = '';
                        this.classList = new MockClassList(this);
                        this.id = '';
                        this.value = '';
                        this.disabled = false;
                        this._text = '';
                        this.parentNode = null;
                        this.scrollTop = 0;
                        this.scrollHeight = 100;
                    }
                    set textContent(v) { this._text = String(v); this.children = []; }
                    get textContent() {
                        if (this.children.length === 0) return this._text;
                        return this.children.map(c => c.textContent).join('');
                    }
                    set innerHTML(v) { this._html = String(v); }
                    get innerHTML() { return this._html || ''; }
                    appendChild(child) {
                        child.parentNode = this;
                        this.children.push(child);
                        return child;
                    }
                    remove() {
                        if (this.parentNode) {
                            this.parentNode.children = this.parentNode.children.filter(c => c !== this);
                            this.parentNode = null;
                        }
                    }
                    closest(sel) {
                        if (sel.startsWith('.')) {
                            const cls = sel.slice(1);
                            let cur = this;
                            while (cur) {
                                if (cur.classList && cur.classList.contains(cls)) return cur;
                                cur = cur.parentNode;
                            }
                        }
                        return null;
                    }
                    querySelectorAll(sel) {
                        const out = [];
                        const selectors = sel.split(',').map(s => s.trim()).filter(Boolean);
                        const visit = (node) => {
                            for (const child of node.children) {
                                const matches = selectors.some(s => {
                                    if (s === '.chat-confirmation-actions button') {
                                        return child.tagName === 'BUTTON' && node.classList.contains('chat-confirmation-actions');
                                    }
                                    if (s.startsWith('.')) {
                                        return child.classList && child.classList.contains(s.slice(1));
                                    }
                                    return child.tagName === s.toUpperCase();
                                });
                                if (matches) out.push(child);
                                visit(child);
                            }
                        };
                        visit(this);
                        return out;
                    }
                    querySelector() { return null; }
                    setAttribute(k, v) { this.attributes[k] = String(v); }
                    getAttribute(k) { return this.attributes[k] ?? null; }
                    addEventListener() {}
                }

                const docListeners = {};
                const bodyListeners = {};
                const rootEl = new MockNode('HTML');
                const bodyEl = new MockNode('BODY');
                rootEl.appendChild(bodyEl);

                const elements = {};
                const mockDocument = {
                    documentElement: rootEl,
                    body: bodyEl,
                    createElement: (tag) => new MockNode(tag),
                    createTextNode: (txt) => { const n = new MockNode(null); n.textContent = txt; return n; },
                    createDocumentFragment: () => new MockNode('#FRAGMENT'),
                    getElementById: (id) => elements[id] || null,
                    querySelectorAll: (sel) => bodyEl.querySelectorAll(sel),
                    addEventListener: (name, fn) => {
                        if (!docListeners[name]) docListeners[name] = [];
                        docListeners[name].push(fn);
                    }
                };
                bodyEl.addEventListener = (name, fn) => {
                    if (!bodyListeners[name]) bodyListeners[name] = [];
                    bodyListeners[name].push(fn);
                };

                function dispatchDoc(name, detail) {
                    (docListeners[name] || []).forEach(fn => fn({ detail, target: detail && detail.target }));
                }

                const storage = {};
                const htmxAjaxCalls = [];
                const context = {
                    window: {
                        document: mockDocument,
                        location: { search: '', pathname: '/' },
                        addEventListener: () => {}
                    },
                    document: mockDocument,
                    localStorage: {
                        getItem: (k) => storage[k] || null,
                        setItem: (k, v) => { storage[k] = String(v); }
                    },
                    setTimeout: () => 1,
                    htmx: {
                        ajax: (method, url, opts) => { htmxAjaxCalls.push({ method, url, opts }); },
                        trigger: () => {}
                    }
                };
                context.window.htmx = context.htmx;
                context.window.localStorage = context.localStorage;
                vm.createContext(context);
                vm.runInContext(coreCode, context);
                vm.runInContext(chatCode, context);

                const chatInput = new MockNode('INPUT');
                chatInput.id = 'chatInput';
                chatInput.value = 'Run smoke tests';
                elements['chatInput'] = chatInput;
                elements['hiddenActiveFile'] = new MockNode('INPUT');
                elements['hiddenSessionId'] = new MockNode('INPUT');
                elements['hiddenSessionId'].value = 'sess-1';

                const warningBanner = new MockNode('DIV');
                const warningBannerText = new MockNode('SPAN');
                elements['warningBanner'] = warningBanner;
                elements['warningBannerText'] = warningBannerText;

                const chatMessages = new MockNode('DIV');
                chatMessages.id = 'chatMessages';
                bodyEl.appendChild(chatMessages);
                elements['chatMessages'] = chatMessages;

                const actionsRow = new MockNode('DIV');
                actionsRow.className = 'chat-confirmation-actions';
                const confirmBtn = new MockNode('BUTTON');
                confirmBtn.textContent = 'Confirm';
                confirmBtn.setAttribute('data-action-id', 'act-1');
                confirmBtn.setAttribute('data-session-id', 'sess-1');
                const cancelBtn = new MockNode('BUTTON');
                cancelBtn.textContent = 'Cancel';
                cancelBtn.setAttribute('data-action-id', 'act-1');
                actionsRow.appendChild(confirmBtn);
                actionsRow.appendChild(cancelBtn);
                chatMessages.appendChild(actionsRow);

                context.init();

                // 1. clearChatInput keeps prompt intact until successful /api/chat completion
                context.clearChatInput({});
                if (chatInput.value !== 'Run smoke tests') {
                    throw new Error('Expected chatInput not to be cleared prematurely: ' + chatInput.value);
                }

                // 2. Unrelated /api/status error must NOT delete active chat bubbles
                dispatchDoc('htmx:responseError', {
                    pathInfo: { requestPath: '/api/status' },
                    xhr: { status: 500, responseText: 'Poll failed' }
                });
                if (chatMessages.querySelectorAll('.thinking-bubble').length !== 1
                        || chatMessages.querySelectorAll('.optimistic-user-bubble').length !== 1) {
                    throw new Error('Unrelated /api/status error removed active chat bubbles');
                }

                // 3. Failed /api/chat response keeps chatInput text for retry, but clears bubbles
                dispatchDoc('htmx:responseError', {
                    pathInfo: { requestPath: '/api/chat' },
                    target: chatMessages,
                    xhr: { status: 500, responseText: 'LLM error' }
                });
                dispatchDoc('htmx:afterRequest', {
                    pathInfo: { requestPath: '/api/chat' },
                    successful: false,
                    xhr: { status: 500 }
                });
                if (chatInput.value !== 'Run smoke tests') {
                    throw new Error('Failed /api/chat wiped user prompt from chatInput');
                }
                if (chatMessages.querySelectorAll('.thinking-bubble').length !== 0) {
                    throw new Error('Failed /api/chat left orphaned thinking bubble');
                }

                // 4. Successful /api/chat clears chatInput only if unchanged; edited follow-up text survives
                chatInput.value = 'Run smoke tests';
                context.clearChatInput({});
                chatInput.value = 'Edited follow-up prompt';
                dispatchDoc('htmx:afterRequest', {
                    pathInfo: { requestPath: '/api/chat' },
                    successful: true,
                    xhr: { status: 200 }
                });
                if (chatInput.value !== 'Edited follow-up prompt') {
                    throw new Error('Successful /api/chat wiped newly edited follow-up prompt: ' + chatInput.value);
                }
                chatInput.value = 'Run smoke tests';
                context.clearChatInput({});
                dispatchDoc('htmx:afterRequest', {
                    pathInfo: { requestPath: '/api/chat' },
                    successful: true,
                    xhr: { status: 200 }
                });
                if (chatInput.value !== '') {
                    throw new Error('Successful /api/chat did not clear unchanged chatInput');
                }

                // 4b. Overlapping /api/chat completion must not reset pendingSubmittedPrompt or wipe new draft
                chatInput.value = 'First slow prompt';
                context.clearChatInput({});
                chatInput.value = 'Second prompt';
                context.clearChatInput({});
                chatInput.value = 'Third draft in progress';
                dispatchDoc('htmx:afterRequest', {
                    pathInfo: { requestPath: '/api/chat' },
                    requestConfig: { parameters: { prompt: 'First slow prompt' } },
                    successful: true,
                    xhr: { status: 200 }
                });
                dispatchDoc('htmx:afterRequest', {
                    pathInfo: { requestPath: '/api/chat' },
                    requestConfig: { parameters: { prompt: 'Second prompt' } },
                    successful: true,
                    xhr: { status: 200 }
                });
                if (chatInput.value !== 'Third draft in progress') {
                    throw new Error('Overlapping /api/chat completion wiped user draft: ' + chatInput.value);
                }

                // 5. Plain-text error body containing HTML markup must render via textContent (never innerHTML)
                dispatchDoc('htmx:responseError', {
                    pathInfo: { requestPath: '/api/status' },
                    xhr: { status: 400, responseText: 'Invalid ID <img src=x onerror=alert(1)>' }
                });
                if (warningBannerText.innerHTML !== '' || !warningBannerText.textContent.includes('<img')) {
                    throw new Error('Expected warning banner to use textContent instead of innerHTML: html=' + warningBannerText.innerHTML + ', text=' + warningBannerText.textContent);
                }

                // 6. Confirmation button failure (responseError + sendError) unlocks buttons and restores label
                context.submitChatConfirmation(confirmBtn, 'confirm');
                if (!confirmBtn.disabled || !cancelBtn.disabled) {
                    throw new Error('Expected confirmation buttons disabled while in flight');
                }
                dispatchDoc('htmx:sendError', {
                    pathInfo: { requestPath: '/api/chat/confirm' },
                    target: chatMessages
                });
                if (confirmBtn.disabled || cancelBtn.disabled) {
                    throw new Error('Confirmation buttons stayed permanently disabled after htmx:sendError');
                }
                if (confirmBtn.textContent !== 'Confirm') {
                    throw new Error('Confirmation button label not restored: ' + confirmBtn.textContent);
                }

                // 7. aiAction refresh_runs targets /playbooks with outerHTML swap when #queueListContainer exists
                htmxAjaxCalls.length = 0;
                const queueContainer = new MockNode('DIV');
                queueContainer.id = 'queueListContainer';
                elements['queueListContainer'] = queueContainer;
                (bodyListeners['aiAction'] || []).forEach(fn => fn({
                    detail: { action: 'refresh_runs', actions: ['refresh_runs'] }
                }));
                if (htmxAjaxCalls.length !== 1
                        || htmxAjaxCalls[0].method !== 'GET'
                        || htmxAjaxCalls[0].url !== '/playbooks'
                        || htmxAjaxCalls[0].opts.target !== '#queueListContainer'
                        || htmxAjaxCalls[0].opts.select !== '#queueListContainer'
                        || htmxAjaxCalls[0].opts.swap !== 'outerHTML') {
                    throw new Error('Unexpected aiAction refresh_runs htmx.ajax call: ' + JSON.stringify(htmxAjaxCalls));
                }

                console.log('OK_LIFECYCLE');
                """.formatted(quoteJs(chatJsPath.toString()), quoteJs(coreJsPath.toString()));

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", script);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }
        final boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "Node lifecycle test timed out");
        Assertions.assertEquals(0, process.exitValue(), "Node lifecycle test failed:\n" + output);
        Assertions.assertTrue(output.toString().contains("OK_LIFECYCLE"));
    }

    @Test
    public void testChatMarkdownReadyStateOobSwapAndNestedListsViaNode() throws Exception
    {
        Assumptions.assumeTrue(isNodeAvailable(), "Node.js is required to run dashboard-chat.js lifecycle tests");

        final Path chatJsPath = Files.exists(Paths.get("src/main/resources/static/dashboard-chat.js"))
                ? Paths.get("src/main/resources/static/dashboard-chat.js").toAbsolutePath()
                : Paths.get("aura-manager/src/main/resources/static/dashboard-chat.js").toAbsolutePath();

        final String script = """
                const fs = require('fs');
                const vm = require('vm');
                const code = fs.readFileSync(%s, 'utf8');

                class MockEl {
                    constructor(tagName) {
                        this.tagName = tagName ? tagName.toUpperCase() : '#TEXT';
                        this.children = [];
                        this.attributes = {};
                        this.className = '';
                        this.id = '';
                        this._text = '';
                    }
                    set textContent(v) {
                        this._text = String(v);
                        this.children = [];
                    }
                    get textContent() {
                        if (this.children.length === 0) return this._text;
                        return this.children.map(c => c.textContent).join('');
                    }
                    appendChild(child) {
                        if (child.tagName === '#FRAGMENT') {
                            child.children.forEach(c => this.children.push(c));
                        } else {
                            this.children.push(child);
                        }
                        return child;
                    }
                    setAttribute(k, v) { this.attributes[k] = String(v); }
                    getAttribute(k) { return this.attributes[k] ?? null; }
                    querySelectorAll(sel) {
                        if (sel !== '.chat-message.message-ai .text.chat-markdown') {
                            return [];
                        }
                        const out = [];
                        const walk = (node, insideAiMsg) => {
                            const classes = (node.className || '').split(/\\s+/).filter(Boolean);
                            const nextInsideAi = insideAiMsg
                                    || (classes.includes('chat-message') && classes.includes('message-ai'));
                            if (insideAiMsg && classes.includes('text') && classes.includes('chat-markdown')) {
                                out.push(node);
                            }
                            node.children.forEach(c => walk(c, nextInsideAi));
                        };
                        walk(this, false);
                        return out;
                    }
                }

                function makeAiBubble(markdownText) {
                    const msg = new MockEl('DIV');
                    msg.className = 'chat-message ai message-ai';
                    const textDiv = new MockEl('DIV');
                    textDiv.className = 'text chat-markdown';
                    textDiv.textContent = markdownText;
                    msg.appendChild(textDiv);
                    return { msg, textDiv };
                }

                const docListeners = {};
                const body = new MockEl('BODY');
                const initialBubble = makeAiBubble('Initial **complete** message');
                body.appendChild(initialBubble.msg);

                const mockDocument = {
                    readyState: 'complete',
                    body,
                    createElement: (tag) => new MockEl(tag),
                    createTextNode: (txt) => {
                        const n = new MockEl(null);
                        n.textContent = txt;
                        return n;
                    },
                    createDocumentFragment: () => new MockEl('#FRAGMENT'),
                    getElementById: () => null,
                    querySelectorAll: (sel) => body.querySelectorAll(sel),
                    addEventListener: (ev, fn) => {
                        if (!docListeners[ev]) docListeners[ev] = [];
                        docListeners[ev].push(fn);
                    }
                };

                const context = {
                    window: {},
                    document: mockDocument,
                    htmx: {}
                };
                context.window = context;
                vm.createContext(context);
                vm.runInContext(code, context);

                // 1. readyState === 'complete' at script execution time must still render immediately
                if (initialBubble.textDiv.getAttribute('data-markdown-rendered') !== 'true'
                        || !initialBubble.textDiv.children.some(p => p.tagName === 'P' && p.children.some(c => c.tagName === 'STRONG'))) {
                    throw new Error('Markdown did not render immediately when document.readyState === complete');
                }

                // 2. htmx:oobAfterSwap and htmx:afterSettle (and detached target on htmx:afterSwap) must still render document
                const detachedTarget = new MockEl('DIV');
                const oobBubble = makeAiBubble('Active **session** `run_1`');
                body.appendChild(oobBubble.msg);
                (docListeners['htmx:oobAfterSwap'] || []).forEach(fn => fn({ detail: { target: detachedTarget } }));
                if (oobBubble.textDiv.getAttribute('data-markdown-rendered') !== 'true') {
                    throw new Error('Markdown did not render after htmx:oobAfterSwap');
                }

                const settleBubble = makeAiBubble('After **settle** `done`');
                body.appendChild(settleBubble.msg);
                (docListeners['htmx:afterSettle'] || []).forEach(fn => fn({ detail: { target: detachedTarget } }));
                if (settleBubble.textDiv.getAttribute('data-markdown-rendered') !== 'true') {
                    throw new Error('Markdown did not render after htmx:afterSettle');
                }

                const detachedSwapBubble = makeAiBubble('After **detached** swap');
                body.appendChild(detachedSwapBubble.msg);
                (docListeners['htmx:afterSwap'] || []).forEach(fn => fn({ detail: { target: detachedTarget } }));
                if (detachedSwapBubble.textDiv.getAttribute('data-markdown-rendered') !== 'true') {
                    throw new Error('Markdown did not render when htmx:afterSwap target was detached/outerHTML');
                }

                // 3. Nested lists must preserve child <ul>/<ol> inside parent <li>
                const nestedMd = [
                    '### Execution Details',
                    '- **Results:**',
                    '  - **Passed:** 1',
                    '  - **Failed:** 0',
                    '- **Timeline:**',
                    '  1. First',
                    '  2. Second'
                ].join('\\n');
                const nestedFrag = context.buildMarkdownFragment(nestedMd);
                const ul = nestedFrag.children.find(c => c.tagName === 'UL');
                if (!ul || ul.children.length !== 2) {
                    throw new Error('Expected top-level UL with 2 LI items, got: ' + JSON.stringify(nestedFrag));
                }
                const firstSubUl = ul.children[0].children.find(c => c.tagName === 'UL');
                const secondSubOl = ul.children[1].children.find(c => c.tagName === 'OL');
                if (!firstSubUl || firstSubUl.children.length !== 2
                        || !secondSubOl || secondSubOl.children.length !== 2) {
                    throw new Error('Expected nested UL (2 items) and OL (2 items) inside parent LI: ' + JSON.stringify(ul));
                }

                // 4. Dedenting to an intermediate indent (4 -> 2) with a tag change must still nest inside parent LI
                const dedentMd = [
                    '- parent',
                    '    - deep',
                    '  1. numbered-child',
                    '- next'
                ].join('\\n');
                const dedentFrag = context.buildMarkdownFragment(dedentMd);
                if (dedentFrag.children.length !== 1 || dedentFrag.children[0].tagName !== 'UL') {
                    throw new Error('Expected single outer UL after intermediate dedent, got: ' + JSON.stringify(dedentFrag));
                }
                const dedentOuterUl = dedentFrag.children[0];
                if (dedentOuterUl.children.length !== 2) {
                    throw new Error('Expected 2 top-level LI items (parent and next), got: ' + JSON.stringify(dedentOuterUl));
                }
                const parentLi = dedentOuterUl.children[0];
                const parentSubUl = parentLi.children.find(c => c.tagName === 'UL');
                const parentSubOl = parentLi.children.find(c => c.tagName === 'OL');
                if (!parentSubUl || parentSubUl.children.length !== 1
                        || !parentSubOl || parentSubOl.children.length !== 1) {
                    throw new Error('Expected nested UL (deep) and OL (numbered-child) inside parent LI: ' + JSON.stringify(parentLi));
                }

                console.log('OK_MARKDOWN_LIFECYCLE');
                """.formatted(quoteJs(chatJsPath.toString()));

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", script);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }
        final boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "Node markdown lifecycle test timed out");
        Assertions.assertEquals(0, process.exitValue(), "Node markdown lifecycle test failed:\n" + output);
        Assertions.assertTrue(output.toString().contains("OK_MARKDOWN_LIFECYCLE"));
    }

    @Test
    public void testBuildActiveViewContextAndSyncHiddenInputsAcrossViews() throws Exception
    {
        Assertions.assertTrue(isNodeAvailable(), "Node.js is required for dashboard-chat.js context tests");
        final Path chatJsPath = Paths.get("src/main/resources/static/dashboard-chat.js").toAbsolutePath();

        final String script = """
                const fs = require('fs');
                const vm = require('vm');
                const chatCode = fs.readFileSync(%s, 'utf8');

                const elements = {};
                const hiddenFile = { value: '' };
                const hiddenViewContext = { value: '' };
                elements['hiddenActiveFile'] = hiddenFile;
                elements['hiddenViewContext'] = hiddenViewContext;

                const mainContainer = {
                    runEl: null,
                    querySelector: (sel) => sel === '[data-run-id]' ? mainContainer.runEl : null
                };
                elements['mainViewContainer'] = mainContainer;

                const context = {
                    window: { location: { pathname: '/dashboard-pages/p-1', search: '' }, activeEditingFile: '' },
                    document: {
                        getElementById: (id) => elements[id] || null
                    }
                };
                vm.createContext(context);
                vm.runInContext(chatCode, context);

                // 1. Custom dashboard context
                elements['pageCustomDashboard'] = {
                    getAttribute: (name) => name === 'data-page-id' ? 'p-1' : (name === 'data-page-name' ? 'General Overview' : '')
                };
                context.syncHiddenActiveFile();
                if (!hiddenViewContext.value.includes('view=CustomDashboard')
                        || !hiddenViewContext.value.includes('pageId=p-1')
                        || !hiddenViewContext.value.includes('pageName=General Overview')) {
                    throw new Error('Unexpected custom dashboard context: ' + hiddenViewContext.value);
                }

                // 2. Run report context
                delete elements['pageCustomDashboard'];
                context.window.location.pathname = '/run-report';
                context.window.location.search = '?runId=run_42';
                mainContainer.runEl = {
                    getAttribute: (name) => name === 'data-run-id' ? 'run_42' : (name === 'data-batch-name' ? 'Checkout' : 'staging')
                };
                context.syncHiddenViewContext();
                if (!hiddenViewContext.value.includes('view=RunReport')
                        || !hiddenViewContext.value.includes('runId=run_42')
                        || !hiddenViewContext.value.includes('batchName=Checkout')) {
                    throw new Error('Unexpected run report context: ' + hiddenViewContext.value);
                }

                // 3. Runs overview context must take precedence over child [data-run-id] rows
                elements['pageBatchHistoryOverview'] = {
                    getAttribute: (name) => name === 'data-batch-name' ? 'Checkout'
                            : (name === 'data-scope-title' ? 'All Runs' : '')
                };
                context.window.location.pathname = '/runs';
                context.window.location.search = '';
                context.syncHiddenViewContext();
                if (!hiddenViewContext.value.includes('view=RunsOverview')
                        || hiddenViewContext.value.includes('view=RunReport')
                        || !hiddenViewContext.value.includes('batchName=Checkout')
                        || !hiddenViewContext.value.includes('scopeTitle=All Runs')) {
                    throw new Error('Expected RunsOverview to win over child [data-run-id]: ' + hiddenViewContext.value);
                }
                delete elements['pageBatchHistoryOverview'];

                // 4. Playbooks workspace context and active editor file fallback
                mainContainer.runEl = null;
                context.window.location.pathname = '/playbooks';
                context.window.location.search = '';
                elements['auraTestManagerWorkspace'] = {};
                context.syncHiddenViewContext();
                if (!hiddenViewContext.value.includes('view=PlaybooksAndQueue')) {
                    throw new Error('Expected PlaybooksAndQueue when #auraTestManagerWorkspace is present without editor: ' + hiddenViewContext.value);
                }
                elements['editorFileName'] = { getAttribute: () => 'tests/Smoke.yaml' };
                context.syncHiddenActiveFile();
                if (hiddenFile.value !== 'tests/Smoke.yaml' || !hiddenViewContext.value.includes('view=PlaybooksAndQueue')) {
                    throw new Error('Unexpected playbooks context: file=' + hiddenFile.value + ', ctx=' + hiddenViewContext.value);
                }

                console.log('OK_VIEW_CONTEXT');
                """.formatted(quoteJs(chatJsPath.toString()));

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", script);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }
        final boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "Node viewContext test timed out");
        Assertions.assertEquals(0, process.exitValue(), "Node viewContext test failed:\n" + output);
        Assertions.assertTrue(output.toString().contains("OK_VIEW_CONTEXT"));
    }

    private static String quoteJs(final String value)
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static boolean isNodeAvailable()
    {
        try
        {
            final Process p = new ProcessBuilder("node", "-v").start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        }
        catch (final Exception e)
        {
            return false;
        }
    }
}
