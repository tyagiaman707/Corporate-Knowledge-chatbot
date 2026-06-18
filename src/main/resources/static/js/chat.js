// ============================================================
//  chat.js — Employee Chat with Knowledge Assistant
// ============================================================

requireAuth();
initSidebar();
loadChatHistory();

let isWaiting = false;

// ── Send message ──────────────────────────────────────────────
async function sendMessage() {
    if (isWaiting) return;

    const input    = document.getElementById('questionInput');
    const question = input.value.trim();
    if (!question) return;

    // Hide suggestion chips after first message
    document.getElementById('suggestions').style.display = 'none';

    // Add user message bubble
    appendMessage('user', question);
    input.value = '';
    input.style.height = 'auto';

    // Show typing indicator
    const typingId = showTyping();
    isWaiting = true;
    document.getElementById('sendBtn').disabled = true;

    try {
        const res = await apiFetch('/api/chat/ask', {
            method: 'POST',
            body: JSON.stringify({ question })
        });

        removeTyping(typingId);
        appendAiMessage(res.data.answer, res.data.sourceDocument, res.data.createdAt);

    } catch (err) {
        removeTyping(typingId);
        appendMessage('ai', '⚠️ Sorry, I could not get an answer. Please try again.', null, null, true);
    } finally {
        isWaiting = false;
        document.getElementById('sendBtn').disabled = false;
    }
}

// ── Keyboard handler ──────────────────────────────────────────
function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
}

function autoResize(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

// ── Suggestion chips ──────────────────────────────────────────
function askSuggestion(btn) {
    document.getElementById('questionInput').value = btn.textContent;
    sendMessage();
}

// ── Clear chat ────────────────────────────────────────────────
function clearChat() {
    const messages = document.getElementById('chatMessages');
    messages.innerHTML = `
        <div class="message ai-message">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
                <div class="message-bubble">
                    <p>Chat cleared. Ask me anything about company policies!</p>
                </div>
                <span class="message-time">Just now</span>
            </div>
        </div>`;
    document.getElementById('suggestions').style.display = 'flex';
}

// ── Load chat history ─────────────────────────────────────────
async function loadChatHistory() {
    try {
        const res = await apiFetch('/api/chat/history');
        const history = res.data;

        if (!history || history.length === 0) return;

        // Show last 10 messages in reverse (oldest first)
        const recent = history.slice(0, 10).reverse();
        recent.forEach(item => {
            appendMessage('user', item.question, null, null, false, false);
            appendAiMessage(item.answer, item.sourceDocument, item.createdAt, false);
        });

    } catch (err) {
        // History load failure is non-critical, ignore silently
        console.log('Could not load history:', err.message);
    }
}

// ── Message builders ──────────────────────────────────────────
function appendMessage(type, text, sourceDoc, time, isError = false, scroll = true) {
    const messages = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = `message ${type === 'user' ? 'user-message' : 'ai-message'}`;

    const displayTime = time ? formatTime(time) : getCurrentTime();

    if (type === 'user') {
        div.innerHTML = `
            <div class="message-content">
                <div class="message-bubble">${escapeHtml(text)}</div>
                <span class="message-time">${displayTime}</span>
            </div>`;
    } else {
        div.innerHTML = `
            <div class="message-avatar">🤖</div>
            <div class="message-content">
                <div class="message-bubble ${isError ? 'error-bubble' : ''}">
                    ${escapeHtml(text)}
                </div>
                <span class="message-time">${displayTime}</span>
            </div>`;
    }

    messages.appendChild(div);
    if (scroll) scrollToBottom();
    return div;
}

function appendAiMessage(answer, sourceDoc, time, scroll = true) {
    const messages = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = 'message ai-message';
    const displayTime = time ? formatTime(time) : getCurrentTime();

    const sourceBadge = sourceDoc
        ? `<div class="source-badge">📄 Source: ${escapeHtml(sourceDoc)}</div>`
        : '';

    div.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="message-content">
            <div class="message-bubble">${formatAnswer(answer)}</div>
            ${sourceBadge}
            <span class="message-time">${displayTime}</span>
        </div>`;

    messages.appendChild(div);
    if (scroll) scrollToBottom();
}

// ── Typing indicator ──────────────────────────────────────────
function showTyping() {
    const messages = document.getElementById('chatMessages');
    const id = 'typing-' + Date.now();
    const div = document.createElement('div');
    div.id = id;
    div.className = 'message ai-message typing-indicator';
    div.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="message-content">
            <div class="message-bubble">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
            </div>
        </div>`;
    messages.appendChild(div);
    scrollToBottom();
    return id;
}

function removeTyping(id) {
    document.getElementById(id)?.remove();
}

// ── Helpers ───────────────────────────────────────────────────
function scrollToBottom() {
    const el = document.getElementById('chatMessages');
    el.scrollTop = el.scrollHeight;
}

function getCurrentTime() {
    return new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/\n/g, '<br>');
}

function formatAnswer(text) {
    // Convert **bold** markdown and newlines for nicer display
    return escapeHtml(text)
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
}
