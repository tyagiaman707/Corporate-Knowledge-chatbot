// ============================================================
//  admin.js — PDF Upload & Document Management
// ============================================================

requireAuth();
if (!Auth.isAdmin()) window.location.href = 'chat.html';

initSidebar();
loadDocuments();

let selectedFile = null;

// ── Drag & Drop ───────────────────────────────────────────────
function handleDragOver(e) {
    e.preventDefault();
    document.getElementById('uploadArea').classList.add('dragover');
}
function handleDragLeave(e) {
    document.getElementById('uploadArea').classList.remove('dragover');
}
function handleDrop(e) {
    e.preventDefault();
    document.getElementById('uploadArea').classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (file) setSelectedFile(file);
}

function handleFileSelect(e) {
    const file = e.target.files[0];
    if (file) setSelectedFile(file);
}

function setSelectedFile(file) {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
        alert('Please select a PDF file only.');
        return;
    }
    if (file.size > 20 * 1024 * 1024) {
        alert('File size must not exceed 20MB.');
        return;
    }

    selectedFile = file;
    document.getElementById('selectedFileName').textContent = file.name;
    document.getElementById('selectedFileSize').textContent = formatFileSize(file.size);
    document.getElementById('filePreview').classList.remove('hidden');
    document.getElementById('uploadResult').innerHTML = '';
}

function clearFile() {
    selectedFile = null;
    document.getElementById('pdfInput').value = '';
    document.getElementById('filePreview').classList.add('hidden');
    document.getElementById('uploadResult').innerHTML = '';
}

// ── Upload PDF ────────────────────────────────────────────────
async function uploadPdf() {
    if (!selectedFile) return;

    const progressEl = document.getElementById('uploadProgress');
    const resultEl   = document.getElementById('uploadResult');

    document.getElementById('filePreview').classList.add('hidden');
    progressEl.classList.remove('hidden');
    resultEl.innerHTML = '';

    try {
        const formData = new FormData();
        formData.append('file', selectedFile);

        const res = await apiFetch('/api/admin/upload', {
            method: 'POST',
            body: formData
        });

        progressEl.classList.add('hidden');
        resultEl.innerHTML = `
            <div class="upload-result-success">
                ✓ <strong>${res.data.fileName}</strong> uploaded successfully!
                ${res.data.totalChunks} chunks created.
            </div>`;

        clearFile();
        loadDocuments();

    } catch (err) {
        progressEl.classList.add('hidden');
        resultEl.innerHTML = `
            <div class="upload-result-error">
                ✕ Upload failed: ${err.message}
            </div>`;
    }
}

// ── Load Documents ────────────────────────────────────────────
async function loadDocuments() {
    const container = document.getElementById('documentsContainer');
    container.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading documents...</p>
        </div>`;

    try {
        const res = await apiFetch('/api/admin/documents');
        const docs = res.data;

        if (!docs || docs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📂</div>
                    <h3>No documents yet</h3>
                    <p>Upload a PDF above to build your knowledge base.</p>
                </div>`;
            return;
        }

        container.innerHTML = `<div class="documents-grid">${docs.map(docCard).join('')}</div>`;

    } catch (err) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">⚠️</div>
                <h3>Failed to load documents</h3>
                <p>${err.message}</p>
            </div>`;
    }
}

function docCard(doc) {
    return `
        <div class="document-card" id="doc-${doc.id}">
            <div class="doc-card-top">
                <span class="doc-icon">📄</span>
                <div class="doc-info">
                    <span class="doc-name" title="${doc.fileName}">${doc.fileName}</span>
                    <span class="doc-meta">
                        Uploaded by ${doc.uploadedBy} · ${formatDate(doc.uploadedAt)}
                    </span>
                </div>
            </div>
            <div class="doc-card-bottom">
                <span class="doc-chunks">${doc.totalChunks || '—'} chunks</span>
                <button class="btn-delete-doc" onclick="deleteDocument(${doc.id}, '${doc.fileName}')">
                    🗑 Delete
                </button>
            </div>
        </div>`;
}

// ── Delete Document ───────────────────────────────────────────
async function deleteDocument(id, name) {
    if (!confirm(`Delete "${name}" from the knowledge base?\nThis cannot be undone.`)) return;

    try {
        await apiFetch(`/api/admin/documents/${id}`, { method: 'DELETE' });
        // Remove card from UI immediately
        document.getElementById(`doc-${id}`)?.remove();
        // If no cards left, show empty state
        const grid = document.querySelector('.documents-grid');
        if (grid && grid.children.length === 0) loadDocuments();
    } catch (err) {
        alert('Delete failed: ' + err.message);
    }
}
