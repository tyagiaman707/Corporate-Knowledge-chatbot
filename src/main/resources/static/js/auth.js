// ============================================================
//  auth.js — Login & Register
// ============================================================

// Redirect if already logged in
if (Auth.isLoggedIn()) {
    redirectToDashboard();
}

function redirectToDashboard() {
    const user = Auth.getUser();
    if (user?.role === 'ADMIN') {
        window.location.href = '/pages/admin.html';
    } else {
        window.location.href = '/pages/chat.html';
    }
}

// ── Tab switching ─────────────────────────────────────────────
function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach((btn, i) => {
        btn.classList.toggle('active', (i === 0 && tab === 'login') || (i === 1 && tab === 'register'));
    });
    document.getElementById('loginForm').classList.toggle('active', tab === 'login');
    document.getElementById('registerForm').classList.toggle('active', tab === 'register');
    clearErrors();
}

function clearErrors() {
    ['loginError', 'registerError', 'registerSuccess'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.classList.add('hidden'); el.textContent = ''; }
    });
}

// ── Login ─────────────────────────────────────────────────────
async function handleLogin(event) {
    event.preventDefault();
    clearErrors();

    const email    = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    setButtonLoading('loginBtn', true);

    try {
        const res = await apiFetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        Auth.setSession(res.data);
        redirectToDashboard();

    } catch (err) {
        const errorEl = document.getElementById('loginError');
        errorEl.textContent = err.message || 'Invalid email or password';
        errorEl.classList.remove('hidden');
        setButtonLoading('loginBtn', false);
    }
}

// ── Register ──────────────────────────────────────────────────
async function handleRegister(event) {
    event.preventDefault();
    clearErrors();

    const name     = document.getElementById('regName').value.trim();
    const email    = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const role     = document.getElementById('regRole').value;

    if (password.length < 6) {
        showRegisterError('Password must be at least 6 characters');
        return;
    }

    setButtonLoading('registerBtn', true);

    try {
        await apiFetch('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ name, email, password, role })
        });

        const successEl = document.getElementById('registerSuccess');
        successEl.textContent = '✓ Account created! Please sign in.';
        successEl.classList.remove('hidden');

        document.getElementById('registerForm').reset();
        setTimeout(() => switchTab('login'), 1500);

    } catch (err) {
        showRegisterError(err.message || 'Registration failed');
    } finally {
        setButtonLoading('registerBtn', false);
    }
}

function showRegisterError(msg) {
    const el = document.getElementById('registerError');
    el.textContent = msg;
    el.classList.remove('hidden');
    setButtonLoading('registerBtn', false);
}

// ── Toggle password visibility ────────────────────────────────
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    input.type = input.type === 'password' ? 'text' : 'password';
}
