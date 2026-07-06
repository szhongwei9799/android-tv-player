const API_BASE = '/api';

const utils = {
    async request(url, options = {}) {
        const config = {
            headers: { 'Content-Type': 'application/json' },
            ...options
        };
        if (!options.body) delete config.headers['Content-Type'];
        const response = await fetch(`${API_BASE}${url}`, config);
        const data = await response.json();
        if (!data.success) throw new Error(data.error || '请求失败');
        return data;
    },

    formatDate(ts) {
        if (!ts) return '-';
        return new Date(ts).toLocaleString('zh-CN');
    },

    formatFileSize(bytes) {
        if (!bytes) return '-';
        const u = ['B', 'KB', 'MB', 'GB'];
        let s = bytes, i = 0;
        while (s >= 1024 && i < u.length - 1) { s /= 1024; i++; }
        return `${s.toFixed(1)} ${u[i]}`;
    },

    formatDuration(ms) {
        if (!ms) return '-';
        const s = Math.floor(ms / 1000);
        const m = Math.floor(s / 60);
        const h = Math.floor(m / 60);
        if (h > 0) return `${h}:${String(m % 60).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
        return `${m}:${String(s % 60).padStart(2, '0')}`;
    },

    getMediaTypeIcon(type) {
        return { 'VIDEO': '🎬', 'IMAGE': '🖼️', 'PPT': '📊', 'PDF': '📄', 'AUDIO': '🎵', 'STREAM': '📹' }[type] || '📁';
    },

    getMediaTypeLabel(type) {
        return { 'VIDEO': '视频', 'IMAGE': '图片', 'PPT': 'PPT', 'PDF': 'PDF', 'AUDIO': '音频', 'STREAM': '流媒体' }[type] || type;
    }
};

// Navigation
document.querySelectorAll('.nav-item').forEach(link => {
    link.addEventListener('click', e => {
        e.preventDefault();
        const page = link.dataset.page;
        document.querySelectorAll('.nav-item').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.getElementById(page).classList.add('active');
        loadPageData(page);
    });
});

function loadPageData(page) {
    switch (page) {
        case 'media': loadMediaList(); break;
        case 'tags': loadTagList(); break;
        case 'playlists': loadPlaylistList(); break;
        case 'tasks': loadTaskList(); break;
        case 'settings': loadSettings(); loadSystemInfo(); break;
    }
}

// Modal
function showModal(title, content) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = content;
    document.getElementById('modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

document.getElementById('modal').addEventListener('click', e => {
    if (e.target === document.getElementById('modal')) closeModal();
});

// Toast
function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✓' : '✕';
    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.animation = 'toastOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

// Confirm dialog
function showConfirm(message) {
    return new Promise(resolve => {
        showModal('确认操作', `
            <p style="margin-bottom:20px;color:var(--text-muted);">${message}</p>
            <div style="display:flex;gap:8px;">
                <button class="btn btn-secondary" onclick="closeModal();window._confirmRes=false;">取消</button>
                <button class="btn btn-danger" onclick="closeModal();window._confirmRes=true;">确定</button>
            </div>
        `);
        const check = setInterval(() => {
            if (window._confirmRes !== undefined) {
                clearInterval(check);
                const res = window._confirmRes;
                window._confirmRes = undefined;
                resolve(res);
            }
        }, 100);
    });
}

// Initialize
document.addEventListener('DOMContentLoaded', () => loadPageData('media'));
