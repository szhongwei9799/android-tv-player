const API_BASE = '/api';

const utils = {
    async request(url, options = {}) {
        const config = {
            headers: { 'Content-Type': 'application/json; charset=utf-8' },
            ...options
        };
        if (!options.body) delete config.headers['Content-Type'];
        const r = await fetch(`${API_BASE}${url}`, config);
        const d = await r.json();
        if (!d.success) throw new Error(d.error || '请求失败');
        return d;
    },
    formatDate(ts) { return ts ? new Date(ts).toLocaleString('zh-CN') : '-'; },
    formatFileSize(b) {
        if (!b) return '-';
        const u = ['B','KB','MB','GB']; let s = b, i = 0;
        while (s >= 1024 && i < u.length - 1) { s /= 1024; i++; }
        return `${s.toFixed(1)} ${u[i]}`;
    },
    formatDuration(ms) {
        if (!ms) return '-';
        const s = Math.floor(ms / 1000), m = Math.floor(s / 60), h = Math.floor(m / 60);
        if (h > 0) return `${h}:${String(m%60).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
        return `${m}:${String(s%60).padStart(2,'0')}`;
    },
    mediaIcon(t) { return {VIDEO:'🎬',IMAGE:'🖼️',PPT:'📊',PDF:'📄',AUDIO:'🎵',STREAM:'📹'}[t]||'📁'; },
    mediaLabel(t) { return {VIDEO:'视频',IMAGE:'图片',PPT:'PPT',PDF:'PDF',AUDIO:'音频',STREAM:'流媒体'}[t]||t; }
};

function escHtml(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

document.querySelectorAll('.nav-item').forEach(a => {
    a.addEventListener('click', e => {
        e.preventDefault();
        const p = a.dataset.page;
        document.querySelectorAll('.nav-item').forEach(l => l.classList.remove('active'));
        a.classList.add('active');
        document.querySelectorAll('.page').forEach(x => x.classList.remove('active'));
        document.getElementById(p).classList.add('active');
        loadPageData(p);
    });
});

function loadPageData(p) {
    switch(p) {
        case 'media': loadMediaList(); break;
        case 'tags': loadTagList(); break;
        case 'playlists': loadPlaylistList(); break;
        case 'network': break;
        case 'tasks': loadTaskList(); break;
        case 'settings': loadSettings(); loadSystemInfo(); break;
    }
}

function showModal(title, content) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = content;
    document.getElementById('modal').style.display = 'flex';
}
function closeModal() { document.getElementById('modal').style.display = 'none'; }
document.getElementById('modal').addEventListener('click', e => {
    if (e.target === document.getElementById('modal')) closeModal();
});

function showToast(msg, type = 'success') {
    const c = document.getElementById('toastContainer');
    const t = document.createElement('div');
    t.className = `toast toast-${type === 'success' ? 'succ' : 'err'}`;
    t.innerHTML = `<span>${type === 'success' ? '✓' : '✕'}</span><span>${msg}</span>`;
    c.appendChild(t);
    setTimeout(() => {
        t.style.animation = 'toastOut .3s ease forwards';
        setTimeout(() => t.remove(), 300);
    }, 2500);
}

function showConfirm(msg) {
    return new Promise(resolve => {
        showModal('确认操作', `<p style="margin-bottom:18px;color:var(--text2);font-size:14px;">${msg}</p>
        <div style="display:flex;gap:8px;"><button class="btn btn-outline" onclick="closeModal();window._cr=false;">取消</button>
        <button class="btn btn-danger" onclick="closeModal();window._cr=true;">确定</button></div>`);
        const iv = setInterval(() => {
            if (window._cr !== undefined) { clearInterval(iv); const r = window._cr; window._cr = undefined; resolve(r); }
        }, 100);
    });
}

document.addEventListener('DOMContentLoaded', () => loadPageData('media'));
