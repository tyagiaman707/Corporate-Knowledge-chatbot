// ============================================================
//  config.js — Shared configuration and utilities
// ============================================================

const API_BASE = 'https://corporate-knowledge-chatbot-production.up.railway.app';

// ── Token helpers ────────────────────────────────────────────
const Auth = {
    getToken:    ()      => localStorage.getItem('token'),
    getUser:     ()      => JSON.parse(localStorage.getItem('user') || 'null'),
    setSession:  (data)  => {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            name:  data.name,
            email: data.email,
            role:  data.role
        }));
    },
    clearSession: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },
    isLoggedIn: () => !!localStorage.getItem('token'),
    isAdmin:    () => {
        const user = JSON.parse(localStorage.getItem('user') || 'null');
        return user?.role === 'ADMIN';
    }
};

// ── API fetch wrapper ────────────────────────────────────────
async function apiFetch(endpoint, options = {}) {
    const token = Auth.getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers
    };

    // Don't set Content-Type for FormData (file uploads)
    if (options.body instanceof FormData) {
        delete headers['Content-Type'];
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
        const message = data?.message || data?.error || 'Something went wrong';
        throw new Error(message);
    }

    return data;
}

// ── Shared UI helpers ────────────────────────────────────────
function setButtonLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    const text   = btn.querySelector('.btn-text');
    const loader = btn.querySelector('.btn-loader');
    btn.disabled = loading;
    text?.classList.toggle('hidden', loading);
    loader?.classList.toggle('hidden', !loading);
}

function formatDate(isoString) {
    if (!isoString) return '';
    return new Date(isoString).toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function formatFileSize(bytes) {
    if (bytes < 1024)       return bytes + ' B';
    if (bytes < 1024*1024)  return (bytes/1024).toFixed(1) + ' KB';
    return (bytes/1024/1024).toFixed(1) + ' MB';
}

function formatTime(isoString) {
    if (!isoString) return 'Just now';
    return new Date(isoString).toLocaleTimeString('en-IN', {
        hour: '2-digit', minute: '2-digit'
    });
}

// ── Logout ───────────────────────────────────────────────────
function logout() {
    Auth.clearSession();
    window.location.href = '/index.html';
}

// ── Guard: redirect if not logged in ─────────────────────────
function requireAuth() {
    if (!Auth.isLoggedIn()) {
        window.location.href = '/index.html';
        return false;
    }
    return true;
}

// ── Init sidebar user info ────────────────────────────────────
function initSidebar() {
    const user = Auth.getUser();
    if (!user) return;

    const nameEl   = document.getElementById('sidebarUserName');
    const avatarEl = document.getElementById('userAvatar');
    const roleEl   = document.getElementById('sidebarUserRole');

    if (nameEl)   nameEl.textContent   = user.name;
    if (avatarEl) avatarEl.textContent = user.name.charAt(0).toUpperCase();
    if (roleEl)   roleEl.textContent   = user.role === 'ADMIN' ? 'Administrator' : 'Employee';

    // Hide admin nav link for employees
    const adminLink = document.getElementById('adminNavLink');
    if (adminLink && user.role !== 'ADMIN') {
        adminLink.style.display = 'none';
    }
}
