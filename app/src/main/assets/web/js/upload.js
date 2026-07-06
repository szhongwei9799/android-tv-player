const uploadManager = {
    queue: [],
    uploading: false,
    maxConcurrent: 2,

    init() {
        this.setupDropzone();
        this.setupFileInput();
    },

    setupDropzone() {
        const dz = document.getElementById('dropzone');
        if (!dz) return;
        dz.addEventListener('click', () => document.getElementById('fileInput').click());
        dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('dragover'); });
        dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));
        dz.addEventListener('drop', e => {
            e.preventDefault();
            dz.classList.remove('dragover');
            this.addFiles(e.dataTransfer.files);
        });
    },

    setupFileInput() {
        const fi = document.getElementById('fileInput');
        if (!fi) return;
        fi.addEventListener('change', e => {
            this.addFiles(e.target.files);
            fi.value = '';
        });
    },

    addFiles(files) {
        for (const file of files) {
            this.queue.push({ file, status: 'pending', progress: 0 });
        }
        this.renderProgress();
        this.processQueue();
    },

    async processQueue() {
        if (this.uploading) return;
        const pending = this.queue.filter(i => i.status === 'pending');
        if (pending.length === 0) return;
        this.uploading = true;
        const batch = pending.slice(0, this.maxConcurrent);
        await Promise.all(batch.map(i => this.uploadFile(i)));
        this.uploading = false;
        this.processQueue();
    },

    uploadFile(item) {
        return new Promise(resolve => {
            item.status = 'uploading';
            this.renderProgress();
            const fd = new FormData();
            fd.append('file', item.file);
            fd.append('filename', item.file.name);
            const xhr = new XMLHttpRequest();
            xhr.upload.addEventListener('progress', e => {
                if (e.lengthComputable) {
                    item.progress = Math.round((e.loaded / e.total) * 100);
                    this.renderProgress();
                }
            });
            xhr.onload = () => {
                item.status = xhr.status === 200 ? 'success' : 'error';
                this.renderProgress();
                if (item.status === 'success') loadMediaList();
                resolve();
            };
            xhr.onerror = () => {
                item.status = 'error';
                this.renderProgress();
                resolve();
            };
            xhr.open('POST', '/api/media/upload');
            xhr.send(fd);
        });
    },

    renderProgress() {
        const el = document.getElementById('uploadProgress');
        if (!el) return;
        el.innerHTML = this.queue.map(i => `
            <div class="upload-item">
                <span class="name">${escHtml(i.file.name)}</span>
                <div class="progress-track"><div class="progress-bar" style="width:${i.progress}%"></div></div>
                <span class="status">${i.status === 'pending' ? '等待' : i.status === 'uploading' ? i.progress+'%' : i.status === 'success' ? '✓' : '✕'}</span>
            </div>
        `).join('');
    }
};

document.addEventListener('DOMContentLoaded', () => uploadManager.init());

async function loadSettings() {
    try {
        const res = await utils.request('/config');
        if (res.data) {
            const c = res.data;
            document.getElementById('imageInterval').value = c.imageInterval || 5;
            document.getElementById('pptInterval').value = c.pptInterval || 10;
            document.getElementById('pdfInterval').value = c.pdfInterval || 10;
            document.getElementById('transitionDuration').value = c.transitionDuration || 500;
        }
    } catch (e) {
        console.error('加载设置失败:', e);
    }
}

async function saveSettings() {
    try {
        await utils.request('/config', { method: 'PUT', body: JSON.stringify({
            imageInterval: parseInt(document.getElementById('imageInterval').value),
            pptInterval: parseInt(document.getElementById('pptInterval').value),
            pdfInterval: parseInt(document.getElementById('pdfInterval').value),
            transitionDuration: parseInt(document.getElementById('transitionDuration').value)
        })});
        showToast('设置已保存');
    } catch (e) {
        showToast('保存失败: ' + e.message, 'error');
    }
}

async function loadSystemInfo() {
    const el = document.getElementById('systemInfo');
    try {
        const res = await utils.request('/system/info');
        if (res.data) {
            const info = res.data;
            el.innerHTML = `<div class="system-info-grid">${[
                ['设备名称', info.deviceName],
                ['制造商', info.manufacturer],
                ['型号', info.model],
                ['Android版本', info.sdkVersion],
                ['IP地址', info.ipAddress],
                ['媒体数量', info.mediaCount],
                ['播放列表', info.playlistCount],
                ['定时任务', info.taskCount]
            ].map(([k, v]) => `<div class="info-row"><span class="info-label">${k}</span><span>${v ?? '-'}</span></div>`).join('')}</div>`;
        }
        document.getElementById('serverUrl').textContent = window.location.href.replace(/\/$/, '');
    } catch (e) {
        el.innerHTML = `<span style="color:var(--danger)">加载失败: ${e.message}</span>`;
    }
}
