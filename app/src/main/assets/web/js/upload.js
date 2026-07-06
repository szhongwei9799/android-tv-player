const uploadManager = {
    queue: [], uploading: false, maxConcurrent: 2,
    init() {
        const dz = document.getElementById('dropzone');
        if (!dz) return;
        dz.addEventListener('click', () => document.getElementById('fileInput').click());
        dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('dragover'); });
        dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));
        dz.addEventListener('drop', e => { e.preventDefault(); dz.classList.remove('dragover'); this.addFiles(e.dataTransfer.files); });
        document.getElementById('fileInput').addEventListener('change', e => { this.addFiles(e.target.files); e.target.value = ''; });
    },
    addFiles(files) { for (const f of files) this.queue.push({file:f,status:'pending',progress:0}); this.renderProgress(); this.processQueue(); },
    async processQueue() {
        if (this.uploading) return;
        const p = this.queue.filter(i => i.status === 'pending');
        if (!p.length) return;
        this.uploading = true;
        await Promise.all(p.slice(0, this.maxConcurrent).map(i => this.uploadFile(i)));
        this.uploading = false; this.processQueue();
    },
    uploadFile(item) {
        return new Promise(resolve => {
            item.status = 'uploading'; this.renderProgress();
            const fd = new FormData(); fd.append('file', item.file); fd.append('filename', item.file.name);
            const xhr = new XMLHttpRequest();
            xhr.upload.addEventListener('progress', e => { if (e.lengthComputable) { item.progress = Math.round(e.loaded/e.total*100); this.renderProgress(); } });
            xhr.onload = () => { item.status = xhr.status === 200 ? 'success' : 'error'; this.renderProgress(); if (item.status === 'success') loadMediaList(); resolve(); };
            xhr.onerror = () => { item.status = 'error'; this.renderProgress(); resolve(); };
            xhr.open('POST', '/api/media/upload'); xhr.send(fd);
        });
    },
    renderProgress() {
        const el = document.getElementById('uploadProgress');
        if (!el) return;
        el.innerHTML = this.queue.map(i => `<div class="upload-item"><span class="name">${escHtml(i.file.name)}</span><div class="track"><div class="bar" style="width:${i.progress}%"></div></div><span class="stat">${i.status==='pending'?'等待':i.status==='uploading'?i.progress+'%':i.status==='success'?'✓':'✕'}</span></div>`).join('');
    }
};
document.addEventListener('DOMContentLoaded', () => uploadManager.init());

async function loadSettings() {
    try {
        const r = await utils.request('/config');
        if (r.data) { const c = r.data; document.getElementById('imageInterval').value = c.imageInterval||5; document.getElementById('pptInterval').value = c.pptInterval||10; document.getElementById('pdfInterval').value = c.pdfInterval||10; document.getElementById('transitionDuration').value = c.transitionDuration||500; }
    } catch(_) {}
}

async function saveSettings() {
    try {
        await utils.request('/config', { method: 'PUT', body: JSON.stringify({
            imageInterval: parseInt(document.getElementById('imageInterval').value),
            pptInterval: parseInt(document.getElementById('pptInterval').value),
            pdfInterval: parseInt(document.getElementById('pdfInterval').value),
            transitionDuration: parseInt(document.getElementById('transitionDuration').value)
        })}); showToast('设置已保存');
    } catch(e) { showToast('保存失败: '+e.message,'error'); }
}

async function loadSystemInfo() {
    const el = document.getElementById('systemInfo');
    try {
        const r = await utils.request('/system/info');
        if (r.data) { const i = r.data; el.innerHTML = `<div class="info-grid">${[['设备名称',i.deviceName],['制造商',i.manufacturer],['型号',i.model],['Android版本',i.sdkVersion],['IP地址',i.ipAddress],['媒体数量',i.mediaCount],['播放列表',i.playlistCount],['定时任务',i.taskCount]].map(([k,v])=>`<div class="info-row"><span class="lbl">${k}</span><span>${v??'-'}</span></div>`).join('')}</div>`; }
        document.getElementById('serverUrl').textContent = window.location.href.replace(/\/$/,'');
    } catch(e) { el.innerHTML = `<span style="color:var(--red);font-size:13px;">加载失败: ${e.message}</span>`; }
}
