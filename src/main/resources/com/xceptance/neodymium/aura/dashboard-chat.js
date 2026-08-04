// ============================================================================
// Neodymium Aura Dashboard - Assistant Chat Side-Panel
// ============================================================================

function updateHiddenSessionId(val) {
    const hidden = document.getElementById('hiddenSessionId');
    if (hidden) {
        hidden.value = val;
    }
}
window.updateHiddenSessionId = updateHiddenSessionId;

function setChatPrompt(promptText) {
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

function clearChatInput() {
    const input = document.getElementById('chatInput');
    if (!input) return;
    const prompt = input.value.trim();
    if (!prompt) return;

    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        const userBubble = document.createElement('div');
        userBubble.className = 'chat-message user message-user';
        userBubble.innerHTML = `<div class="chat-message-header"><div class="avatar user-avatar"><i class="fa-solid fa-user"></i></div><span class="sender">You</span></div><div class="text">${escapeHtml(prompt)}</div>`;
        chatMessages.appendChild(userBubble);

        const thinkingBubble = document.createElement('div');
        thinkingBubble.className = 'chat-message ai message-ai thinking-bubble';
        thinkingBubble.innerHTML = `
            <div class="chat-message-header"><div class="avatar ai-avatar"><i class="fa-solid fa-robot"></i></div><span class="sender">Aura Assistant</span></div>
            <div class="typing-indicator"><span></span><span></span><span></span></div>
        `;
        chatMessages.appendChild(thinkingBubble);
        scrollToBottom();
    }

    setTimeout(() => {
        input.value = '';
    }, 50);
}
window.clearChatInput = clearChatInput;

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

function renameCurrentChatSession() {
    const select = document.getElementById('chatSessionSelect');
    if (!select) return;
    const currentId = select.value;
    const currentName = select.options[select.selectedIndex].text;
    const newName = prompt("Enter new name for this chat session:", currentName);
    if (newName && newName.trim() !== "") {
        htmx.ajax('POST', '/api/chat/rename?id=' + currentId + '&name=' + encodeURIComponent(newName.trim()), {
            target: '#chatContainer',
            swap: 'outerHTML'
        });
    }
}
window.renameCurrentChatSession = renameCurrentChatSession;

function deleteCurrentChatSession() {
    const select = document.getElementById('chatSessionSelect');
    if (!select) return;
    const currentId = select.value;
    if (confirm("Are you sure you want to delete this chat session?")) {
        htmx.ajax('POST', '/api/chat/delete?id=' + currentId, {
            target: '#chatContainer',
            swap: 'outerHTML'
        });
    }
}
window.deleteCurrentChatSession = deleteCurrentChatSession;
